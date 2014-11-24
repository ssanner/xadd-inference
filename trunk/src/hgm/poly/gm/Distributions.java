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
        Fraction f1 = varF.subtract(leftBound);       //x - a >0
        Fraction f2 = rightBound.subtract(varF);      //b - x >0
        Fraction d = factory.makeFraction("1").divide(rightBound.subtract(leftBound));                                     // 1 / b - a
        ConstrainedExpression<Fraction> singleCase = new ConstrainedExpression<Fraction>(d, Arrays.asList(f1, f2));
        return new PiecewiseExpression<Fraction>(false, singleCase);
    }

    //creates a truncated bell shaped distribution that is not normalized but its max (which occurs in the point (rightB - leftB)/2) is 1.
    public PiecewiseExpression<Fraction> createNonNormalizedBellShapedDistribution(String var, String leftBoundStr, String rightBoundStr) {
        return createNonNormalizedBellShapedDistribution(var, factory.makeFraction(leftBoundStr), factory.makeFraction(rightBoundStr));
    }

    //creates a truncated bell shaped distribution that is not normalized but its max (which occurs in the point (rightB - leftB)/2) is 1.
    public PiecewiseExpression<Fraction> createNonNormalizedBellShapedDistribution(String var, Fraction a, Fraction b) {
        Fraction epsilon = factory.makeFraction("0.0001");     //to avoid being negative
        Fraction x = factory.makeFraction(var + "^(1)");
        Fraction f1 = x.subtract(a);       //x - a >0
        Fraction f2 = b.subtract(x);      //b - x >0
        Fraction d1 = f1.returnMultiplication(f2); //-(x-a)(x-b) is a bell shaped function
        Fraction d2 = ((a.subtract(b)).returnMultiplication(a.subtract(b))).returnReciprocal();
        d2.multiplyScalarInThis(4);  // [4]/[(a - b)^2] is the normalizer
        Fraction d = d1.returnMultiplication(d2);
        ConstrainedExpression<Fraction> singleCase = new ConstrainedExpression<Fraction>(d.returnAddition(epsilon), Arrays.asList(f1, f2));
        return new PiecewiseExpression<Fraction>(false, singleCase);
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
}
