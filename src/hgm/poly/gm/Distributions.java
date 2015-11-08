package hgm.poly.gm;

import hgm.poly.*;

import java.util.Arrays;

/**
 * Created by Hadi Afshar.
 * Date: 7/07/14
 * Time: 8:04 AM
 */
public class Distributions {
    private PolynomialFactory factory;

    public Distributions(PolynomialFactory factory) {
        this.factory = factory;
    }
    /*new PriorHandler(varBaseName, numVars) {
        @Override
        public PiecewisePolynomial makePrior() {
            String[] vars = factory.getAllVars();
            int numVars = vars.length;//factory.numberOfVars();
            //1. prior: pr(W)
            String[] constraints = new String[numVars * 2];
            for (int i = 0; i < factory.numberOfVars(); i++) {
                String w_i = vars[i];//valueVectorName + "_" + i;
                constraints[2 * i] = w_i + "^(1) + " + bound + ">0";
                constraints[2 * i + 1] = "-1*" + w_i + "^(1) + " + bound + ">0";
            }

            ConstrainedPolynomial singeCase = factory.makeConstrainedPolynomial("1", constraints);//only one case...
            return new PiecewisePolynomial(false, singeCase); //otherwise 0
        }

        @Override
        protected Pair<double[], double[]> surroundingLowerUpperBounds() {
            double[] mins = new double[factory.numberOfVars()];
            double[] maxes = new double[factory.numberOfVars()];
            for (int i = 0; i < factory.numberOfVars(); i++) {
                mins[i] = -bound;
                maxes[i] = bound;
            }
            return new Pair<double[], double[]>(mins, maxes);
        }

        @Override
        protected double functionUpperBound() {
            return 1.0;
        }
    };*/

    public PiecewiseExpression createUniformDistribution(String var, String leftBoundStr, String rightBoundStr) {
        return createUniformDistribution(var, factory.makePolynomial(leftBoundStr), factory.makePolynomial(rightBoundStr));
    }

    /**
     * @return U(var; leftBound, rightBound)
     */
    public PiecewiseExpression createUniformDistribution(String var, Polynomial leftBound, Polynomial rightBound) {
        Polynomial negLB = leftBound.clone();
        negLB.multiplyScalarInThis(-1.0);
        String[] constraints = new String[]{
                var + "^(1) + " + negLB + ">0",
                "-1*" + var + "^(1) + " + rightBound + ">0"
        };
        ConstrainedExpression singleCase = factory.makeConstrainedPolynomial("1", constraints);
        return new PiecewiseExpression(false, singleCase);
    }

    public PiecewiseExpression<Fraction> createUniformDistributionFraction(String var, String leftBoundStr, String rightBoundStr) {
        return createUniformDistributionFraction(var, factory.makeFraction(leftBoundStr), factory.makeFraction(rightBoundStr));
    }

    public PiecewiseExpression<Fraction> createUniformDistributionFraction(String var, Fraction leftBound, Fraction rightBound) {
        Fraction varF = factory.makeFraction(var + "^(1)");
        Fraction f1 = varF.returnSubtraction(leftBound);       //x - a >0
        Fraction f2 = rightBound.returnSubtraction(varF);      //b - x >0
        Fraction d = factory.makeFraction("1").divide(rightBound.returnSubtraction(leftBound));                                     // 1 / b - a
        ConstrainedExpression<Fraction> singleCase = new ConstrainedExpression<Fraction>(d, Arrays.asList(f1, f2));
        return new PiecewiseExpression<Fraction>(false, singleCase);
    }

    public PiecewiseExpression<Fraction> createExhaustiveUniformDistributionFraction(String var, String leftBoundStr, String rightBoundStr) {
        return createExhaustiveUniformDistributionFraction(var, factory.makeFraction(leftBoundStr), factory.makeFraction(rightBoundStr));
    }
    public PiecewiseExpression<Fraction> createExhaustiveUniformDistributionFraction(String var, Fraction leftBound, Fraction rightBound) {
        Fraction varF = factory.makeFraction(var + "^(1)");
        Fraction greaterThanLB = varF.returnSubtraction(leftBound);       //x - a >0
        Fraction lessThanLB = greaterThanLB.returnScalarMultiplication(-1); //x - a <0
        Fraction lessThanUB = rightBound.returnSubtraction(varF);      //b - x >0
        Fraction greaterThanUB = lessThanUB.returnScalarMultiplication(-1); // x - b >0

        Fraction d = factory.makeFraction("1").divide(rightBound.returnSubtraction(leftBound));                                     // 1 / b - a

        ConstrainedExpression<Fraction> case1 = new ConstrainedExpression<Fraction>(factory.makeFraction("0"), Arrays.asList(lessThanLB));
        ConstrainedExpression<Fraction> case2 = new ConstrainedExpression<Fraction>(d, Arrays.asList(greaterThanLB, lessThanUB));
        ConstrainedExpression<Fraction> case3 = new ConstrainedExpression<Fraction>(factory.makeFraction("0"), Arrays.asList(greaterThanUB));
        return new PiecewiseExpression<Fraction>(true, case1, case2, case3);
    }

    //creates a truncated bell shaped distribution that is not normalized but its max (which occurs in the point (rightB - leftB)/2) is 1.
    public PiecewiseExpression<Fraction> createNonNormalizedBellShapedDistribution(String var, String leftBoundStr, String rightBoundStr) {
        return createNonNormalizedBellShapedDistribution(var, factory.makeFraction(leftBoundStr), factory.makeFraction(rightBoundStr));
    }

    //creates a truncated bell shaped distribution that is not normalized but its max (which occurs in the point (rightB - leftB)/2) is 1.
    public PiecewiseExpression<Fraction> createNonNormalizedBellShapedDistribution(String var, Fraction a, Fraction b) {
        Fraction epsilon = factory.makeFraction("0.0001");     //to avoid being negative
        Fraction x = factory.makeFraction(var + "^(1)");
        Fraction f1 = x.returnSubtraction(a);       //x - a >0
        Fraction f2 = b.returnSubtraction(x);      //b - x >0
        Fraction d1 = f1.returnMultiplication(f2); //-(x-a)(x-b) is a bell shaped function
        Fraction d2 = ((a.returnSubtraction(b)).returnMultiplication(a.returnSubtraction(b))).returnReciprocal();
        d2.multiplyScalarInThis(4);  // [4]/[(a - b)^2] is the normalizer
        Fraction d = d1.returnMultiplication(d2);
        ConstrainedExpression<Fraction> singleCase = new ConstrainedExpression<Fraction>(d.returnAddition(epsilon), Arrays.asList(f1, f2));

        return new PiecewiseExpression<Fraction>(false, singleCase);
    }

    public PiecewiseExpression<Fraction> irwinHall(String variable) {

        Fraction xPositive = factory.makePositiveConstraint(variable + "^(1) > 0").cloneCastToFraction(); //x>0
        Fraction xNegative = factory.makePositiveConstraint(variable + "^(1) < 0").cloneCastToFraction(); //x<0

        Fraction xGreaterThan1 = factory.makePositiveConstraint(variable + "^(1) + -1 > 0").cloneCastToFraction(); //x>1
        Fraction xLessThan1 = factory.makePositiveConstraint(variable + "^(1) + -1 < 0").cloneCastToFraction(); //x<1

         Fraction xGreaterThan2 = factory.makePositiveConstraint(variable + "^(1) + -2 > 0").cloneCastToFraction(); //x>2
        Fraction xLessThan2 = factory.makePositiveConstraint(variable + "^(1) + -2 < 0").cloneCastToFraction(); //x<2

        Fraction xGreaterThan3 = factory.makePositiveConstraint(variable + "^(1) + -3 > 0").cloneCastToFraction(); //x>3
        Fraction xLessThan3 = factory.makePositiveConstraint(variable + "^(1) + -3 < 0").cloneCastToFraction(); //x<3


        //Pure Irwin-Hall roughly Normal with mean 1.5 and sigma 0.5

        //if (x<0) return 0;
        ConstrainedExpression<Fraction> case1 = new ConstrainedExpression<Fraction>(factory.makeFraction("[0]/[1]"), Arrays.asList(xNegative));

        //if (x >= 0 && x < 1) return 0.5 * x * x;
        ConstrainedExpression<Fraction> case2 = new ConstrainedExpression<Fraction>(factory.makeFraction("[0.5 * " + variable + "^(2)]/[1]"), Arrays.asList(xPositive, xLessThan1));

        //if (x >= 1 && x < 2) return 0.5 * (-2.0 * x * x + 6.0 * x - 3.0);
        ConstrainedExpression<Fraction> case3 =
                new ConstrainedExpression<Fraction>(factory.makeFraction("[-1.0 * " + variable + "^(2) + 3.0 * " + variable + "^(1) + -1.5]/[1]"),
                        Arrays.asList(xGreaterThan1, xLessThan2));


        //if (x >= 2 && x <= 3) return 0.5 * (x * x - 6.0 * x + 9);
        ConstrainedExpression<Fraction> case4 =
                new ConstrainedExpression<Fraction>(factory.makeFraction("[0.5 * " + variable + "^(2) + -3.0 * " + variable + "^(1) + 4.5]/[1]"),
                        Arrays.asList(xGreaterThan2, xLessThan3));

        //if (x > 3) return 0;
        ConstrainedExpression<Fraction> case5 =
                new ConstrainedExpression<Fraction>(factory.makeFraction("[0]/[1]"),
                        Arrays.asList(xGreaterThan3));

        return new PiecewiseExpression<Fraction>(true, case1, case2, case3, case4, case5);
    }

    public PiecewiseExpression<Fraction> createNormalDistributionIrwinHallApprox(String x, String mean, String standardDeviation /*note: it is sigma not sigma^2*/) {
        return createIrwinHallApproxNormalDistribution(x, factory.makeFraction(mean), factory.makeFraction(standardDeviation));
    }

    public PiecewiseExpression<Fraction> createIrwinHallApproxNormalDistribution(String x, Fraction mean, Fraction standardDeviation /*note: it is sigma not sigma^2*/) {

        Double innerSigma = 0.5;
        Double innerMean = 1.5;

        Fraction oldToNewStdDev = new Fraction(
                standardDeviation.getDenominator().scalarMultiplication(innerSigma),
                standardDeviation.getNumerator());// innerSigma / normalStdDev;

        String auxiliaryVar = x;
        PiecewiseExpression<Fraction> irwinHall = irwinHall(auxiliaryVar);

        // oldToNewStdDev * irwinHall(oldToNewStdDev * (v - normalMean) + innerMean):
        Fraction f = factory.makeFraction(x + "^(1)"); //v
        f = f.returnSubtraction(mean); // v - normalMean
        f = oldToNewStdDev.returnMultiplication(f); // oldToNewStdDev * (v - normalMean)
        f = f.returnAddition(factory.makeFraction(innerMean.toString())); // oldToNewStdDev * (v - normalMean) + innerMean

        PiecewiseExpression<Fraction> result = irwinHall.substitute(auxiliaryVar, f);
        return result.multiplyExpression(oldToNewStdDev);


        /*double innerSigma = 0.5;
        double innerMean = 1.5;
        double oldToNewStdDev = innerSigma / normalStdDev;

        String auxiliaryVar = x;
        PiecewiseExpression<Fraction> irwinHall = irwinHall(auxiliaryVar); //auxiliary var

        // oldToNewStdDev * irwinHall(oldToNewStdDev * (v - normalMean) + innerMean);
        StringBuilder sb = new StringBuilder();
        sb.append(oldToNewStdDev).append(" * ").append(x).append("^(1)").append(" + ").append(-oldToNewStdDev * normalMean + innerMean);
        Fraction a = factory.makeFraction(sb.toString());
        PiecewiseExpression<Fraction> result = irwinHall.substitute(auxiliaryVar, a);
        result.multiplyScalarInThis(oldToNewStdDev);

        return result;*/
    }

    public PiecewiseExpression<Fraction> createBowlDistributionFraction(String[] vars, double radius, int halfPow) {
        // if (x^2 + y^2 + ... ) < radius : (x + y + ... )^(2*halfPow)
        StringBuilder conditionSb = new StringBuilder();
        StringBuilder fruitSb = new StringBuilder();

        for (String var : vars) {
            conditionSb.append("-1*" + var + "^(2) +");
            fruitSb.append(var + "^(1) +");
        }
        String hyperSphere = conditionSb.substring(0, conditionSb.length() - 1) + " + " + radius * radius;
        String fruit = fruitSb.substring(0, fruitSb.length() - 1);
        Polynomial fruitP = factory.makePolynomial(fruit);
        fruitP = fruitP.toPower(2 * halfPow);
        fruitP.multiplyScalarInThis(1 / (Math.pow(radius, 2 * halfPow) * Math.pow(vars.length, halfPow)));  //to guarantee that all values are less than 1 (although it is not normalized)
        Fraction fruitF = new Fraction(fruitP, factory.one());
        ConstrainedExpression<Fraction> singleCase = new ConstrainedExpression<Fraction>(fruitF, Arrays.asList(factory.makeFraction(hyperSphere)));
        return new PiecewiseExpression<Fraction>(false, singleCase);
    }

    /**
     * @param var var in the inverse uniform distribution
     * @param a   lower bound of the UNIFORM distribution that we look for its inverse (lower bound of the inverse.distribution is 1/b assuming 0< a < b)
     * @param b   upper bound of the UNIFORM distribution that we look for its inverse (upper bound of the inverse.distribution is 1/a assuming 0< a < b)
     * @return inverse uniform distribution
     */
    public PiecewiseExpression<Fraction> createInverseUniformDistributionFraction(String var, double a, double b) {
        if (a < 0 || b < 0 || b <= a) throw new RuntimeException("at the moment only 0<a<b is allowed");
        Fraction fruit = factory.makePolynomial("" + (1.0 / (b - a)) + " * " + var + "^(-2.0)").cloneCastToFraction();// 1/((b-a)*y^2)
        // support: [1/b, 1/a]
        Fraction lowSupport = factory.makePolynomial(var + "^(1) +-" + (1.0 / b)).cloneCastToFraction();// 1/b < v
        Fraction highSupport = factory.makePolynomial("-1.0 * " + var + "^(1) +" + (1.0 / a)).cloneCastToFraction();// 1/a > v
        ConstrainedExpression<Fraction> singleCase = new ConstrainedExpression<Fraction>(fruit, Arrays.asList(lowSupport, highSupport));
        return new PiecewiseExpression<Fraction>(false, singleCase);
    }

    public PiecewiseExpression<Fraction> createExhaustiveTriangular(String variable, String mu, String left, String right) {
        return createExhaustiveTriangular(factory.makeFraction(variable+"^(1)"), factory.makeFraction(mu), factory.makeFraction(left), factory.makeFraction(right));
    }

    public PiecewiseExpression<Fraction> createExhaustiveTriangular(Fraction expr, Fraction mu, Fraction left, Fraction right) {

        //Note: both left and right widths would be positive otherwise wrong function is generated  //todo generate error if not positive number...

//        if (dwidthl < 0 || dwidthr < 0) {
//            System.out.println("Negative widths (" + dwidthl + "," + dwidthr + ") not allowed.");
//            System.exit(1);
//        }

//        double H = 2d / (dwidthr + dwidthl);
        Fraction h = factory.makeFraction("2").divide(left.returnAddition(right));


        //if (x >= 2 && x <= 3) return 0.5 * (x * x - 6.0 * x + 9);
//        ConstrainedExpression<Fraction> case4 =
//                new ConstrainedExpression<Fraction>(factory.makeFraction("[0.5 * " + variable + "^(2) + -3.0 * " + variable + "^(1) + 4.5]/[1]"),
//                        Arrays.asList(xGreaterThan2, xLessThan3));


//        Fraction xGreaterThan3 = factory.makePositiveConstraint(variable + "^(1) + -3 > 0").cloneCastToFraction(); //x>3
//        Fraction xLessThan3 = factory.makePositiveConstraint(variable + "^(1) + -3 < 0").cloneCastToFraction(); //x<3




        // Handle cases where left- or right-hand sides are empty

        if (right.isZero()) {
            // expr > mu - left::        expr - mu + left  (>0)
            Fraction vGreaterThanMuMinusLeft = expr.returnSubtraction(mu).returnAddition(left);
            Fraction vLessThanMuMinusLeft = vGreaterThanMuMinusLeft.returnScalarMultiplication(-1);

            // expr > mu
            Fraction vGreaterThanMu = expr.returnSubtraction(mu);
            Fraction vLessThanMu = vGreaterThanMu.returnScalarMultiplication(-1.0);

//            "( [" + _df.format(H/dwidthl) + " * " + "(" + expr + " - " + mu + " + " + widthl + ")] )"
            Fraction fruit = h.divide(left).returnMultiplication(vGreaterThanMuMinusLeft);
            ConstrainedExpression<Fraction> case1 = new ConstrainedExpression<Fraction>(fruit, Arrays.asList(vGreaterThanMuMinusLeft, vLessThanMu));
            ConstrainedExpression<Fraction> case2 = new ConstrainedExpression<Fraction>(factory.makeFraction("0"), Arrays.asList(vGreaterThanMuMinusLeft, vGreaterThanMu));
            ConstrainedExpression<Fraction> case3 = new ConstrainedExpression<Fraction>(factory.makeFraction("0"), Arrays.asList(vLessThanMuMinusLeft));


            return new PiecewiseExpression<Fraction>(true, case1, case2, case3);
            //            s = "([" + expr + " >= " + mu + " - " + dwidthl + "] "
//                    + "([" + expr + " <= " + mu + "] "
//                    + "( [" + _df.format(H/dwidthl) + " * " + "(" + expr + " - " + mu + " + " + widthl + ")] )"
//                    + "( [0.0] ) ) ( [0.0] ) )";

        } else if(left.isZero()) {
            // expr > mu ::         expr - mu (>0)
            Fraction vGreaterThanMu = expr.returnSubtraction(mu);
            Fraction vLessThanMu = vGreaterThanMu.returnScalarMultiplication(-1);

            //expr < mu + right  ::     mu + right - expr (>0)
            Fraction vLessThanMuPlusRight = mu.returnAddition(right).returnSubtraction(expr);

            // expr - mu -right
            Fraction vGreaterThanMuPlusRight = vLessThanMuPlusRight.returnScalarMultiplication(-1);

            //"( [" + _df.format(-H/dwidthr) + " * " + "(" + expr + " - " + mu + " - " + widthr + ")] )"
            Fraction fruit = vGreaterThanMuPlusRight.returnMultiplication(h.returnScalarMultiplication(-1).divide(right));

            ConstrainedExpression<Fraction> case1 = new ConstrainedExpression<Fraction>(fruit, Arrays.asList(vGreaterThanMu, vLessThanMuPlusRight));
            ConstrainedExpression<Fraction> case2 = new ConstrainedExpression<Fraction>(factory.makeFraction("0"), Arrays.asList(vLessThanMu));
            ConstrainedExpression<Fraction> case3 = new ConstrainedExpression<Fraction>(factory.makeFraction("0"), Arrays.asList(vGreaterThanMu, vLessThanMuPlusRight));

            return new PiecewiseExpression<Fraction>(true, case1, case2, case3);
//            s = "([" + expr + " >= " + mu + "] "
//                    + "([" + expr + " <= " + mu + " + " + dwidthr + "] "
//                    + "( [" + _df.format(-H/dwidthr) + " * " + "(" + expr + " - " + mu + " - " + widthr + ")] )"
//                    + "( [0.0] ) ) ( [0.0] ) )";
        }
        else {
            // expr > mu - left::        expr - mu + left  (>0)
            Fraction vGreaterThanMuMinusLeft = expr.returnSubtraction(mu).returnAddition(left);
            Fraction vLessThanMuMinusLeft = vGreaterThanMuMinusLeft.returnScalarMultiplication(-1);

            //expr < mu + right  ::     mu + right - expr (>0)
            Fraction vLessThanMuPlusRight = mu.returnAddition(right).returnSubtraction(expr);
            // expr - mu -right
            Fraction vGreaterThanMuPlusRight = vLessThanMuPlusRight.returnScalarMultiplication(-1);

            // expr > mu ::         expr - mu (>0)
            Fraction vGreaterThanMu = expr.returnSubtraction(mu);
            Fraction vLessThanMu = vGreaterThanMu.returnScalarMultiplication(-1);

            //            "( [" + _df.format(H/dwidthl) + " * " + "(" + expr + " - " + mu + " + " + widthl + ")] )"
            Fraction fruit1 = h.divide(left).returnMultiplication(vGreaterThanMuMinusLeft);

            //"( [" + _df.format(-H/dwidthr) + " * " + "(" + expr + " - " + mu + " - " + widthr + ")] )"
            Fraction fruit2 = vGreaterThanMuPlusRight.returnMultiplication(h.returnScalarMultiplication(-1).divide(right));

            ConstrainedExpression<Fraction> case1 = new ConstrainedExpression<Fraction>(factory.makeFraction("0"), Arrays.asList(vLessThanMuMinusLeft));
            ConstrainedExpression<Fraction> case2 = new ConstrainedExpression<Fraction>(factory.makeFraction("0"), Arrays.asList(vGreaterThanMuMinusLeft, vGreaterThanMuPlusRight));
            ConstrainedExpression<Fraction> case3 = new ConstrainedExpression<Fraction>(fruit1, Arrays.asList(vGreaterThanMuMinusLeft, vLessThanMuPlusRight, vLessThanMu));
            ConstrainedExpression<Fraction> case4 = new ConstrainedExpression<Fraction>(fruit2, Arrays.asList(vGreaterThanMuMinusLeft, vLessThanMuPlusRight, vGreaterThanMu));

            return new PiecewiseExpression<Fraction>(true, case1, case2, case3, case4);

            /*
        else {
            s = "([" + expr + " >= " + mu + " - " + dwidthl + "] "
                    + "([" + expr + " <= " + mu + " + " + dwidthr + "] "
                    + "([" + expr + " <= " + mu + "] "
                    + "( [" + _df.format(H/dwidthl) + " * " + "(" + expr + " - " + mu + " + " + widthl + ")] )"
                    + "( [" + _df.format(-H/dwidthr) + " * " + "(" + expr + " - " + mu + " - " + widthr + ")] ))"
                    + "( [0.0] ) ) ( [0.0] ) )";
        }
*/
        }





    }
}
