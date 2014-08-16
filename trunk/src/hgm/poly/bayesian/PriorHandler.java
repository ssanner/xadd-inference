package hgm.poly.bayesian;

import hgm.asve.Pair;
import hgm.poly.ConstrainedExpression;
import hgm.poly.PiecewiseExpression;
import hgm.poly.Polynomial;
import hgm.poly.PolynomialFactory;

import java.util.Arrays;

/**
 * Created by Hadi Afshar.
 * Date: 28/04/14
 * Time: 11:03 AM
 */
public abstract class PriorHandler {
    protected PolynomialFactory factory;
    protected String varBaseName;
    protected int numVars;
    private PiecewiseExpression priorPiecewisePolynomial;
    private double[] lowerBoundsPerDim;
    private double[] upperBoundsPerDim;
    private double functionUpperBound;

    protected PriorHandler(String varBaseName, int numVars) {
        this.varBaseName = varBaseName;
        this.numVars = numVars;

        String[] vars = new String[numVars];
        for (int i = 0; i < vars.length; i++) {
            String v_i = varBaseName + "_" + i;
            vars[i] = v_i;
        }

        factory = new PolynomialFactory(vars);
        priorPiecewisePolynomial = makePrior();
        Pair<double[], double[]> boundsPair = surroundingLowerUpperBounds();
        lowerBoundsPerDim = boundsPair.getFirstEntry();
        upperBoundsPerDim = boundsPair.getSecondEntry();
        functionUpperBound = functionUpperBound();
    }

    public PiecewiseExpression getPrior() {
        return priorPiecewisePolynomial;
    }

    public double[] getLowerBoundsPerDim() {
        return lowerBoundsPerDim;
    }

    public double[] getUpperBoundsPerDim() {
        return upperBoundsPerDim;
    }

    public double getFunctionUpperBound() {
        return functionUpperBound;
    }

    protected abstract PiecewiseExpression/*ConstrainedPolynomial*/ makePrior();

    // the bounds are not necessarily tight
    protected abstract Pair<double[] /*mins*/, double[] /*maxes*/> surroundingLowerUpperBounds();

    // not necessarily tight
    protected abstract double functionUpperBound();

    public static PriorHandler uniformInHypercube(String varBaseName, int numVars, final double bound) {
        return new PriorHandler(varBaseName, numVars) {
            @Override
            public PiecewiseExpression makePrior() {
                String[] vars = factory.getAllVars();
                int numVars = vars.length;//factory.numberOfVars();
                //1. prior: pr(W)
                String[] constraints = new String[numVars * 2];
                for (int i = 0; i < factory.numberOfVars(); i++) {
                    String w_i = vars[i];//valueVectorName + "_" + i;
                    constraints[2 * i] = w_i + "^(1) + " + bound + ">0";
                    constraints[2 * i + 1] = "-1*" + w_i + "^(1) + " + bound + ">0";
                }

                ConstrainedExpression singeCase = factory.makeConstrainedPolynomial("1", constraints);//only one case...
                return new PiecewiseExpression(false, singeCase); //otherwise 0
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
        };
    }

    public static PriorHandler uniformInEllipse(String varBaseName, /*final double x0, final double y0, */final double a, final double b) {
        return new PriorHandler(varBaseName, 2 /*only for 2D*/) {
            @Override
            public PiecewiseExpression makePrior() {
                String[] vars = factory.getAllVars();
                if (vars.length != 2) throw new RuntimeException("only for two vars...");

                double theta = -Math.PI / 4.0; //static
                double d = Math.cos(theta); //static
                double e = Math.sin(theta); //static

                String x = vars[0];
                String y = vars[1];
                String constraintStr =
                        (d * d / (a * a) + e * e / (b * b)) + " * " + x + "^(2) + " +
                                (e * e / (a * a) + d * d / (b * b)) + " * " + y + "^(2) + " +
                                (-2.0 * d * e / (a * a) + 2.0 * d * e / (b * b) + " * " + x + "^(1) * " + y + "^(1) + -1");
                //this constraint <0, so:
                Polynomial constraint = factory.makePolynomial(constraintStr);
                constraint.multiplyScalarInThis(-1.0);

                return new PiecewiseExpression(false, new ConstrainedExpression(factory.makePolynomial("1"), Arrays.asList(constraint))); //otherwise 0
            }

            @Override
            protected Pair<double[], double[]> surroundingLowerUpperBounds() {
                if (factory.getAllVars().length != 2) throw new RuntimeException("dims=2 expected");
                double min = Math.min(-a, -b);
                double max = Math.max(a, b);
                return new Pair<double[], double[]>(new double[]{min, min}, new double[]{max, max});
            }

            @Override
            protected double functionUpperBound() {
                return 1.0;
            }
        };
    }

    public static PriorHandler quadraticInEllipse(String varBaseName, /*final double x0, final double y0, */final double a, final double b) {
        return new PriorHandler(varBaseName, 2 /*only for 2D*/) {
            @Override
            public PiecewiseExpression makePrior() {
                String[] vars = factory.getAllVars();
                if (vars.length != 2) throw new RuntimeException("only for two vars...");

                double theta = -Math.PI / 4.0; //static
                double d = Math.cos(theta); //static
                double e = Math.sin(theta); //static

                String x = vars[0];
                String y = vars[1];
                String constraintStr =
                        (d * d / (a * a) + e * e / (b * b)) + " * " + x + "^(2) + " +
                                (e * e / (a * a) + d * d / (b * b)) + " * " + y + "^(2) + " +
                                (-2.0 * d * e / (a * a) + 2.0 * d * e / (b * b) + " * " + x + "^(1) * " + y + "^(1) + -1");
                //this constraint <0, so:
                Polynomial constraint = factory.makePolynomial(constraintStr);
                constraint.multiplyScalarInThis(-1.0);

                return new PiecewiseExpression(false, new ConstrainedExpression(factory.makePolynomial("-1*" + x + "^(2) + -1*" + y + "^(2)" + " + 100"), Arrays.asList(constraint))); //otherwise 0
            }

            @Override
            protected Pair<double[], double[]> surroundingLowerUpperBounds() {
                if (factory.getAllVars().length != 2) throw new RuntimeException("dims=2 expected");
                double min = Math.min(-a, -b);
                double max = Math.max(a, b);
                return new Pair<double[], double[]>(new double[]{min, min}, new double[]{max, max});
            }

            @Override
            protected double functionUpperBound() {
                return 1.0;
            }
        };
    }

    // pr(x_0)          ~ U(-bound0, bound0)
    // pr(x_i|x_{i-1})  ~ U(x_{i-1} - conditionalBound, x_{i-1} + conditionalBound)
    public static PriorHandler serialDependent(String varBaseName, int numVars, final double firstBound, final double conditionalBound) {
        if (firstBound < 0 || conditionalBound < 0) throw new RuntimeException("positive values expected");
        return new PriorHandler(varBaseName, numVars) {
            @Override
            public PiecewiseExpression makePrior() {
                String[] vars = factory.getAllVars();
                int numVars = vars.length;
                String[] constraints = new String[numVars * 2];
                String w_0 = vars[0];
                constraints[0] = w_0 + "^(1) + " + firstBound + ">0";     // w_0 > -first.bound
                constraints[1] = "-1*" + w_0 + "^(1) + " + firstBound + ">0"; // w_0 < first.bound
                for (int i = 1; i < numVars; i++) {
                    String w_i = vars[i];
                    String w_i_1 = vars[i - 1];
                    constraints[2 * i] = w_i + "^(1) + -1*" + w_i_1 + "^(1) + " + conditionalBound + ">0";  // w_i > w_{i-1} - c
                    constraints[2 * i + 1] = "-1*" + w_i + "^(1) + " + w_i_1 + "^(1) + " + conditionalBound + ">0";  // w_i < w_{i-1} + c
                }
                return new PiecewiseExpression(false, factory.makeConstrainedPolynomial("1", constraints)); //otherwise 0
            }

            @Override
            protected Pair<double[], double[]> surroundingLowerUpperBounds() {
                int numVars = factory.numberOfVars();
                double[] mins = new double[numVars];
                double[] maxes = new double[numVars];
                mins[0] = -firstBound;
                maxes[0] = firstBound;
                if (numVars > 1) {
                    for (int i = 1; i < numVars; i++) {
                        mins[i] = mins[i-1] - conditionalBound;
                        maxes[i] = maxes[i-1] + conditionalBound;
                    }
                }

                return new Pair<double[], double[]>(mins, maxes);
            }

            @Override
            protected double functionUpperBound() {
                return 1.0;
            }
        };
    }

    public PolynomialFactory getFactory() {
        return factory;
    }

//    public String getVarBaseName() {
//        return varBaseName;
//    }

//    public int getNumVars() {
//        return numVars;
//    }

}
