package hgm.sampling.gibbs.integral;

import hgm.sampling.SamplingFailureException;
import xadd.ExprLib;
import xadd.XADD;
import xadd.XADD.*;
import xadd.ExprLib.*;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 20/01/14
 * Time: 1:33 PM
 */
public class OneDimIntegral {
    private XADD context;

    public OneDimIntegral(XADD context) {
        this.context = context;
    }

    public Piecewise1DPolynomialUsingArithExpr integrate(XADD.XADDNode node, String integrationVar) {
        return integrate(node, integrationVar, new HashMap<String, Double>());
    }

    /**
     * @param node                integrand
     * @param integrationVar      integration variable
     * @param continuousVarAssign a variable assignment according to which all variables expect the integration var are instantiated
     * @return The integration of a uni-dimensional function where except the 'integration var', all variables are instantiated due to the given value in the assignment:
     */
    public Piecewise1DPolynomialUsingArithExpr integrate(XADD.XADDNode node, String integrationVar, HashMap<String, Double> continuousVarAssign) {
        //Exclude the integration var from the assignment and replace doubles with expressions
        HashMap<String, ExprLib.ArithExpr> substitution = new HashMap<String, ExprLib.ArithExpr>(Math.max(0, continuousVarAssign.size() - 1)); //since the int. var. is not added to it
        for (Map.Entry<String, Double> cVarValue : continuousVarAssign.entrySet()) {
            String cVar = cVarValue.getKey();
            if (!cVar.equals(integrationVar)) { //var should be excluded!
                Double cAssign = cVarValue.getValue();
                substitution.put(cVar, new ExprLib.DoubleExpr(cAssign));
            }
        }

        List<PolynomialInAnInterval> polynomials = substituteAndConvertToPiecewisePolynomial(node, substitution,
                new Interval(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));

//        System.out.println("polynomials = " + polynomials);

        return integrate(polynomials, integrationVar);
    }

    //no assumption...
    public List<PolynomialInAnInterval> substituteAndConvertToPiecewisePolynomial(XADD.XADDNode node, HashMap<String, ArithExpr> subst,
                                                                                   Interval inheritedInterval) {

        if (node instanceof XADD.XADDTNode) {
            List<PolynomialInAnInterval> results = new ArrayList<PolynomialInAnInterval>();
            results.add(new PolynomialInAnInterval(inheritedInterval.getLowerBound(), inheritedInterval.getUpperBound(), ((XADD.XADDTNode) node)._expr.substitute(subst)));
            return results;
        }

        XADD.XADDINode iNode = (XADD.XADDINode) node;
        XADD.Decision decision = context._alOrder.get(iNode._var);

        Interval thisDecisionBounds;

        //substitution:
        if (!(decision instanceof ExprDec)) {
            throw new SamplingFailureException("is not implemented for boolean case YET. Error in substitution of decision: " + decision);
        }
        ExprLib.CompExpr comparableExpression = ((ExprDec) decision)._expr;
        comparableExpression = comparableExpression.substitute(subst);
        Expr canonExpr = comparableExpression.makeCanonical();
//        System.out.println(comparableExpression + " ==canon=> " + canonExpr);
        if (canonExpr instanceof CompExpr) {
            thisDecisionBounds = fetchComparableExprBounds((CompExpr) canonExpr);
        } else throw new SamplingFailureException("Expression: " + canonExpr + "cannot be parsed...");
//            d = new ExprDec(comp);
//            var = getVarIndex(d, true);
//        if (d instanceof BoolDec) {
        // This part is not tested hence commented...
/*
            // System.out.println(((BoolDec)d)._sVarName + ": " + subst);
            ExprLib.VarExpr sub = (ExprLib.VarExpr) subst.get(((BoolDec) d)._sVarName);
            if (sub != null) {
                // There is a substitution for this BoolDec... get new var index
                var = getVarIndex(new BoolDec(sub._sVarName), false);
            }
*/

        Interval lowInterval = inheritedInterval.clone();
        lowInterval.imposeMoreRestriction(thisDecisionBounds.highBound, thisDecisionBounds.lowBound); // they are swapped for the low child, note that one bound is NULL...

        List<PolynomialInAnInterval> lowDataList = null;
        if (lowInterval.isFeasible()) {
            lowDataList = substituteAndConvertToPiecewisePolynomial(iNode.getLowChild(), subst, lowInterval);
//            results.addAll(lowDataList);
        }
//        List<PolynomialInAnInterval> lowDataList = computePiecewisePolynomial(iNode.getLowChild());
//        for (PolynomialInAnInterval lowData : lowDataList) {
//            lowData.imposeMoreRestriction(thisDecisionBounds.getHighBound(), thisDecisionBounds.getLowBound()); //low and high bounds are swapped for the low child...
//        }

        inheritedInterval.imposeMoreRestriction(thisDecisionBounds.getLowerBound(), thisDecisionBounds.highBound); //to be passed to the high child...
        List<PolynomialInAnInterval> highDataList = null;
        if (inheritedInterval.isFeasible()) {
            highDataList = substituteAndConvertToPiecewisePolynomial(iNode.getHighChild(), subst, inheritedInterval);
//            results.addAll(highDataList);
        }
//        for (PolynomialInAnInterval highData : highDataList) {
//            highData.imposeMoreRestriction(thisDecisionBounds.getLowBound(), thisDecisionBounds.getHighBound()); //low and high bounds are swapped for the low child...
//        }

        if (lowDataList == null) return highDataList;
        if (highDataList == null) return lowDataList;

        //sorting:
        if (lowDataList.get(0).getLowerBound() < highDataList.get(0).getLowerBound()) {
            lowDataList.addAll(highDataList);
            return lowDataList;
        } else {
            highDataList.addAll(lowDataList);
            return highDataList;
        }
    }



    public Piecewise1DPolynomialUsingArithExpr integrate(List<PolynomialInAnInterval> piecewisePolynomial, String var) {
        //todo only works for functions with no boolean variable...
        Piecewise1DPolynomialUsingArithExpr result = new Piecewise1DPolynomialUsingArithExpr(var);
        HashMap<String, Double> assign = new HashMap<String, Double>(1);
        double runningSum = 0.0d;
        for (PolynomialInAnInterval intervalPoly : piecewisePolynomial) {
            ExprLib.ArithExpr indefIntegral = intervalPoly.getPolynomial().integrateExpr(var);

            assign.put(var, intervalPoly.getLowerBound());
            Double l = indefIntegral.evaluate(assign);
            if (l.isNaN()) l = 0d; //this happens in the fist interval with lower bound -infty...

            assign.put(var, intervalPoly.getUpperBound());
            Double h = indefIntegral.evaluate(assign);

//            System.out.println("h = " + h);
//            System.out.println("l = " + l);
            Double intervalVol = h - l;
//            System.out.println("IntervalVol = " + intervalVol + "; for " + indefIntegral + " with max: " + assign);


            result.put(intervalPoly.getLowerBound(), ExprLib.ArithExpr.op(indefIntegral, (runningSum - l), ExprLib.ArithOperation.SUM));

            runningSum += intervalVol; // mass of this interval will be added to the offset of the next one...
//            System.out.println("runningSum = " + runningSum);
        }
        return result;
    }


    private Interval fetchComparableExprBounds(CompExpr comparableExpression) {
        Interval interval = new Interval(null, null);


        //todo Only for debug...
        if (!comparableExpression._rhs.equals(ExprLib.ZERO)) {
            throw new RuntimeException("processXADDLeaf: Expected RHS = 0 for '" + comparableExpression + "'");
        }

         if (comparableExpression._type != ExprLib.CompOperation.GT && comparableExpression._type != ExprLib.CompOperation.GT_EQ)
            throw new RuntimeException("Not implemented for Comparable operation '" + comparableExpression._type + "' in " + comparableExpression);

        // I expect patterns like "(1 + (-0.2 * x))" or "(-0.2 * x)" in the LHS:
        ExprLib.ArithExpr lhs = comparableExpression._lhs;
        if (lhs instanceof DoubleExpr) {
            double c = ((DoubleExpr) lhs)._dConstVal;
            if (c>0 || (c>=0 && comparableExpression._type == ExprLib.CompOperation.GT_EQ)) {
                return new Interval(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY); //always true
            } else {
                return new Interval(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY); //always false
            }
        } else if (!(lhs instanceof ExprLib.OperExpr)) {
            throw new RuntimeException(lhs + " is not an operation...");
        }

        double a, b = 0.0d; // to have (b + a * x)
        ExprLib.OperExpr op = (ExprLib.OperExpr) lhs;
        ArrayList<ExprLib.ArithExpr> terms;
        switch (op._type) {
            case SUM:
                //like: (1 + (-0.2 * x)) [bias + coeff * var]
                terms = op._terms;
                if (terms.size() != 2) throw new RuntimeException("cannot parse: " + terms);
                ExprLib.DoubleExpr bias = (ExprLib.DoubleExpr) terms.get(0);
                b = bias._dConstVal;
                if (!(terms.get(1) instanceof ExprLib.OperExpr)) {
                    throw new RuntimeException("Operation expression was expected in the second argument of: " + op);
                }

                op = ((ExprLib.OperExpr) terms.get(1));
                // after this the PROD CASE should be run as well...
            case PROD:
                //like: (-0.2 * x)  [coeff * var]
                terms = op._terms;
                if (terms.size() != 2) throw new RuntimeException("cannot parse: " + terms);
                ExprLib.DoubleExpr coefficient = (ExprLib.DoubleExpr) terms.get(0);
                a = coefficient._dConstVal;
                if (!(terms.get(1) instanceof ExprLib.VarExpr)) {
                    throw new RuntimeException("Variable was expected as the second param. of: " + op);
                }
                break;

            default:
                throw new RuntimeException("cannot parse Operation " + op);
        }

//        System.out.println("a = " + a);
//        System.out.println("b = " + b);

        double bound = -b / a;

        boolean varIsGreaterThanBound = (a > 0);

        if (varIsGreaterThanBound) interval.setLowBound(bound);
        else interval.setHighBound(bound);

        return interval;
    }


}
