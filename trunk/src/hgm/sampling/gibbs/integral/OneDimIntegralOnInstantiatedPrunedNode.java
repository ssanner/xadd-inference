package hgm.sampling.gibbs.integral;

import xadd.ExprLib;
import xadd.XADD;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Created by Hadi Afshar.
 * Date: 20/01/14
 * Time: 1:33 PM
 */
@Deprecated
// Just for test.
public class OneDimIntegralOnInstantiatedPrunedNode {
    private XADD context;

    public OneDimIntegralOnInstantiatedPrunedNode(XADD context) {
        this.context = context;
    }

    public Piecewise1DPolynomialUsingArithExpr integrate(XADD.XADDNode node) {
        HashSet<String> vars = node.collectVars();
        if (vars.size() != 1) throw new RuntimeException("For now only one var.... for node: " + node);
        String var = vars.iterator().next();

        List<IntervalPolynomial> piecewisePolynomial = computePiecewisePolynomial(node);
        return integral(piecewisePolynomial, var);
    }

    class IntervalPolynomial {
        private Double lowBound;
        private Double highBound;

        private ExprLib.ArithExpr polynomial;

        public IntervalPolynomial(Double lowBound, Double highBound, ExprLib.ArithExpr polynomial) {
            this.lowBound = lowBound;
            this.highBound = highBound;
            this.polynomial = polynomial;
        }

        void setLowBound(Double lowBound) {
            this.lowBound = lowBound;
        }

        void setHighBound(Double highBound) {
            this.highBound = highBound;
        }

        void setPolynomial(ExprLib.ArithExpr polynomial) {
            this.polynomial = polynomial;
        }

        Double getLowBound() {
            return lowBound;
        }

        Double getHighBound() {
            return highBound;
        }

        ExprLib.ArithExpr getPolynomial() {
            return polynomial;
        }

        @Override
        public String toString() {
            return polynomial + " : [" + lowBound + ", " + highBound + "]";
        }
        public void imposeMoreRestriction(Double lowBound, Double highBound) {
            if (lowBound != null && lowBound> this.lowBound) {
                this.lowBound = lowBound;
            }

            if (highBound != null && highBound < this.highBound) {
                this.highBound = highBound;
            }
        }

    }

    private Piecewise1DPolynomialUsingArithExpr integral(List<IntervalPolynomial> piecewisePolynomial, String var) {
//        System.out.println("piecewisePolynomial = " + piecewisePolynomial);
        Piecewise1DPolynomialUsingArithExpr result = new Piecewise1DPolynomialUsingArithExpr(var);
        HashMap<String, Double> assign = new HashMap<String, Double>(1);
        double runningSum = 0.0d;
        for (IntervalPolynomial intervalPoly : piecewisePolynomial) {
            ExprLib.ArithExpr indefIntegral = intervalPoly.getPolynomial().integrateExpr(var);

            assign.put(var, intervalPoly.getLowBound());
            Double l = indefIntegral.evaluate(assign);
            if (l.isNaN()) l=0d; //this happens in the fist interval with lower bound -infty...

            assign.put(var, intervalPoly.getHighBound());
            Double h = indefIntegral.evaluate(assign);

//            System.out.println("h = " + h);
//            System.out.println("l = " + l);
            Double intervalVol = h-l;
//            System.out.println("IntervalVol = " + intervalVol + "; for " + indefIntegral + " with max: " + assign);


            result.put(intervalPoly.getLowBound(), ExprLib.ArithExpr.op(indefIntegral, (runningSum - l), ExprLib.ArithOperation.SUM));

            runningSum += intervalVol; // mass of this interval will be added to the offset of the next one...
//            System.out.println("runningSum = " + runningSum);
        }
        return result;
    }


    public List<IntervalPolynomial> computePiecewisePolynomial(XADD.XADDNode node) {

        if (node instanceof XADD.XADDTNode) {
            List<IntervalPolynomial> results = new ArrayList<IntervalPolynomial>();
            results.add(new IntervalPolynomial(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, ((XADD.XADDTNode) node)._expr));
            return results;
        }

        XADD.XADDINode iNode = (XADD.XADDINode) node;
        XADD.Decision decision = context._alOrder.get(iNode._var);
        IntervalPolynomial thisDecisionBounds = fetchDecisionBounds(decision);

        List<IntervalPolynomial> lowDataList = computePiecewisePolynomial(iNode.getLowChild());
        for (IntervalPolynomial lowData : lowDataList) {
            lowData.imposeMoreRestriction(thisDecisionBounds.getHighBound(), thisDecisionBounds.getLowBound()); //low and high bounds are swapped for the low child...
        }

        List<IntervalPolynomial> highDataList = computePiecewisePolynomial(iNode.getHighChild());
        for (IntervalPolynomial highData : highDataList) {
            highData.imposeMoreRestriction(thisDecisionBounds.getLowBound(), thisDecisionBounds.getHighBound()); //low and high bounds are swapped for the low child...
        }

        //todo sort out (=)/null in the fetch...

        //sorting out which path goes first [i.e. sorting]
        Double lowDataLowBound = lowDataList.get(0).getLowBound();
        if (lowDataLowBound == null || lowDataLowBound < highDataList.get(0).getLowBound()) {
            lowDataList.addAll(highDataList);
            return lowDataList;
        } else {
            highDataList.addAll(lowDataList);
            return highDataList;
        }
    }


    private IntervalPolynomial fetchDecisionBounds(XADD.Decision decision) {
        IntervalPolynomial interval = new IntervalPolynomial(null, null, null);

        ExprLib.CompExpr comparableExpression;
        if (decision instanceof XADD.ExprDec) {
            comparableExpression = ((XADD.ExprDec) decision)._expr;
        } else {
            throw new RuntimeException("processXADDLeaf: Unsupported decision type '" + decision + "'");
        }

        //todo Only for debug...
        if (!comparableExpression._rhs.equals(ExprLib.ZERO)) {
            throw new RuntimeException("processXADDLeaf: Expected RHS = 0 for '" + comparableExpression + "'");
        }

//        System.out.println("comp = " + comparableExpression);
//        System.out.println("comp._type = " + comparableExpression._type);

        /*if (comparableExpression._type == ExprLib.CompOperation.EQ) {
            System.err.println("(=)!!!");
            return null; // I do not care about equality since it does not affect integration (assuming there is no delta...) I consider that region as INFEASIBLE
        }*/

        if (comparableExpression._type != ExprLib.CompOperation.GT && comparableExpression._type != ExprLib.CompOperation.GT_EQ)
            throw new RuntimeException("Not implemented for Comparable operation '" + comparableExpression._type + "' in " + comparableExpression);

        // I expect patterns like "(1 + (-0.2 * x))" or "(-0.2 * x)" in the LHS:
        ExprLib.ArithExpr lhs = comparableExpression._lhs;
        if (!(lhs instanceof ExprLib.OperExpr)) {
            throw new RuntimeException(lhs + " is not an operation...");
//                continue; // if it is not an operation containing the integration variable I do not care what it is...(assuming that FEASIBILITY is checked...)
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
