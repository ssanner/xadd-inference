package hgm.poly.integral;

import hgm.poly.*;
import hgm.sampling.gibbs.integral.Interval;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 20/01/14
 * Time: 1:33 PM
 * <p/>
 * Integration of a single polytope
 */
//todo test the class...
public class OneDimPolynomialIntegral {
    boolean DEBUG = false;

    /**
     * @param cp                        integrand
     * @param integrationVar            integration variable
     * @param usableContinuousVarAssign a variable assignment according to which all variables expect the integration var are instantiated
     * @return The integration of a uni-dimensional function where except the 'integration var', all variables are instantiated due to the given value in the assignment:
     */
    @Deprecated
    public OneDimFunction integrate(ConstrainedExpression cp, String integrationVar, Double[] usableContinuousVarAssign) {
//        if (DEBUG) {
//            System.out.println("cp.evaluate(" + Arrays.toString(usableContinuousVarAssign) + "=" + cp.evaluate(usableContinuousVarAssign));  [by be incomplete]
//        }

        if (DEBUG) {
//            FunctionVisualizer.visualize(cp, -50, 50, 0.1, "cp (in integrate...)");

            if (usableContinuousVarAssign[cp.getPolynomialFactory().getVarIndex(integrationVar)] != null) {
                System.out.println("cp.evaluate(usableContinuousVarAssign) = " + cp.evaluate(usableContinuousVarAssign));
            }
        }
        //1. instantiate all variables except one that should be integrated on:
        //Exclude the integration var from the assignment by making it NULL
        int integrationVarIndex = cp.getPolynomialFactory().getVarIndex(integrationVar);
        return integrate(cp, integrationVarIndex, usableContinuousVarAssign);
    }

    public OneDimFunction integrate(ConstrainedExpression cp, int integrationVarIndex, Double[] usableContinuousVarAssign) {
        //excluding the integration var:
        usableContinuousVarAssign[integrationVarIndex] = null;
        ConstrainedExpression substitutedCP = cp.substitute(usableContinuousVarAssign);
        if (DEBUG) {
            System.out.println("substitutedCP = " + substitutedCP);
//            FunctionVisualizer.visualize(substitutedCP, -50, 50, 0.1, "substitutedCP");
        }

        //2. one dimensional integral on the produced CP:
        List<Interval> positiveIntervals = fetchSortedIntervalsInWhichConstraintsAreSatisfied(substitutedCP); //Note: intervals should be sorted in the fetch method
        if (positiveIntervals.isEmpty()) {
            if (DEBUG) {
                System.err.println("empty polytope interval! \t" + positiveIntervals + " for CP: " + substitutedCP);
//                FunctionVisualizer.visualize(substitutedCP, -50, 50, 0.1, "non positive curve:");
            }
            return OneDimFunction.ZERO_1D_FUNCTION;
        }

        //calc. indefinite integral:
        Polynomial indefIntegral = (Polynomial) substitutedCP.getFruit(); //here I should cast.... (?)
        indefIntegral.replaceThisWithIndefiniteIntegral(integrationVarIndex);
        PiecewiseOffset1DPolynomial result = new PiecewiseOffset1DPolynomial(indefIntegral, integrationVarIndex);

        double runningSum = 0.0;
        for (Interval posInterval : positiveIntervals) {
            usableContinuousVarAssign[integrationVarIndex] = posInterval.getLowerBound();
            double l = indefIntegral.evaluate(usableContinuousVarAssign);
            usableContinuousVarAssign[integrationVarIndex] = posInterval.getUpperBound();
            double h = indefIntegral.evaluate(usableContinuousVarAssign);
            double intervalVol = h - l;
            result.addIntervalAndOffset(posInterval, runningSum - l);
            runningSum += intervalVol;
        }

//        if (DEBUG) {
//            FunctionVisualizer.visualize(result, -50, 50, 0.1, "integration result");
//        }

        return result;

        // now lower:
//        usableContinuousVarAssign[integrationVarIndex] = interval.getLowBound();
//        double lo = indefIntegral.evaluate(usableContinuousVarAssign);
//        indefIntegral.addScalarToThis(-lo);
//        return new OneDimIntegralFunc(indefIntegral, interval, integrationVar);
    }

    //assumptions: 1. constraints are linear or quadratic, 2. The polytope is univariate. Note: The returned list is sorted.
    public List<Interval> fetchSortedIntervalsInWhichConstraintsAreSatisfied(ConstrainedExpression<Polynomial> uniVarConstrainedPoly) {    //todo I do not like this casting here...

        List<Interval> intervals = new ArrayList<Interval>(); //note: more than one interval is used to cover non linear conditions as well...
        intervals.add(new Interval(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));

        for (Polynomial constraint : uniVarConstrainedPoly.getConstraints()) {

            int constraintDegree = constraint.degree();
            double[] coeffs = sortCoefficientsOfUnivariatePolynomial(constraint, constraintDegree);
            switch (constraintDegree) {
                case 0: //e.g. if a constraint is 'y (>0)' and we are integrating over 'x'
                    if (coeffs[0] < 0) {
                        if (DEBUG)
                            System.err.println("unexpected constraint: " + constraint + "(unsatisfiable)"); //note that all constraints are positive '>0'
                        return Collections.<Interval>emptyList();  //since infeasible
                    }
                    break;
                case 1: // a*x + b (>0)

                    double a = coeffs[1];
                    double b = coeffs[0];

                    double r = -b / a;
                    if (a > 0) {
                        updateConstraintsWithConstraints(intervals, r, null);
                    } else {
                        updateConstraintsWithConstraints(intervals, null, r);
                    }
                    break;
                case 2: //a*x^2 + b*x + c (>0)

                    double aa = coeffs[2];
                    double bb = coeffs[1];
                    double cc = coeffs[0];

                    double delta = bb * bb - 4 * aa * cc;

                    if (delta > 0) {
                        double deltaRoot = Math.sqrt(delta);
                        double xx1 = (-bb - deltaRoot) / (2 * aa);
                        double xx2 = (-bb + deltaRoot) / (2 * aa);

                        if (aa > 0) {
                            //(-infinity, xx1] and [xx2, infinity)
                            updateConstrainsWithEither(intervals, null, xx1, xx2, null);
                        } else {
                            //(xx2, xx1)
                            updateConstraintsWithConstraints(intervals, xx2, xx1);
                        }
                    }
                    //todo: what if delta is negative????
                    break;
                default:
                    throw new PolynomialException("degree: " + constraint.degree() + " is not supported for positive constraint " + constraint);
            }
        }

        return intervals;
    }

    private double[] sortCoefficientsOfUnivariatePolynomial(Polynomial polynomial, int degree) {
        double[] coeffs = new double[degree + 1]; //since number of coeffs is 1 more than degree i.e. ax+b is of degree 1 with coeffs [a,b]
        for (List<Double> pow : polynomial.getAllPowers()) {
            int d = 0;
            for (double p : pow) {
                int pInt = (int) p;
                if (pInt != p) throw new RuntimeException("integer degree expected");
                d += pInt;
            }
//            System.out.println("degree = " + degree);
//            System.out.println("d = " + d);
            coeffs[d] = polynomial.getCoefficient(pow);
        }
        return coeffs;
    }

//    private boolean isFilledWithZeros(List<Double> list) {

//    }
//        return true;
//
//        }
//            if (d != 0d) return false;
//        for (Double d : list) {

    private void updateConstraintsWithConstraints(List<Interval> intervals, Double low, Double high) {
        for (Iterator<Interval> iterator = intervals.iterator(); iterator.hasNext(); ) {
            Interval interval = iterator.next();
            interval.imposeMoreRestriction(low, high);
            if (!interval.isFeasible()) {
                iterator.remove();
            }
        }
    }

    //NOTE: there should be no intersection between the interval (low1, high1) and (low2, high2)
    private void updateConstrainsWithEither(List<Interval> intervals, Double low1, Double high1, /* OR */ Double low2, Double high2) {
        if (low2 < high1) throw new PolynomialException("intervals are not sorted");

        List<Interval> clonedIntervals = deepClone(intervals);
        updateConstraintsWithConstraints(intervals, low1, high1);
        updateConstraintsWithConstraints(clonedIntervals, low2, high2);
        intervals.addAll(clonedIntervals);
    }

    private List<Interval> deepClone(List<Interval> intervals) {

        List<Interval> cloned = new ArrayList<Interval>(intervals.size());
        for (Interval interval : intervals) {
            cloned.add(interval.clone());
        }

        return cloned;
    }

}

