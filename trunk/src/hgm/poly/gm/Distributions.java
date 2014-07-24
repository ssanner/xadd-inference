package hgm.poly.gm;

import hgm.poly.ConstrainedPolynomial;
import hgm.poly.PiecewisePolynomial;
import hgm.poly.Polynomial;
import hgm.poly.PolynomialFactory;

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

    public PiecewisePolynomial createUniformDistribution(String var, String leftBoundStr, String rightBoundStr) {
        return createUniformDistribution(var, factory.makePolynomial(leftBoundStr), factory.makePolynomial(rightBoundStr));
    }

    /**
     *
     * @return U(var; leftBound, rightBound)
     */
    public PiecewisePolynomial createUniformDistribution(String var, Polynomial leftBound, Polynomial rightBound){
        Polynomial negLB = leftBound.clone();
        negLB.multiplyScalarInThis(-1.0);
                String[] constraints = new String[]{
                var + "^(1) + " + negLB + ">0",
         "-1*" + var + "^(1) + " + rightBound + ">0"
        };
        ConstrainedPolynomial singleCase = factory.makeConstrainedPolynomial("1", constraints);
        return new PiecewisePolynomial(false, singleCase);
    }
}
