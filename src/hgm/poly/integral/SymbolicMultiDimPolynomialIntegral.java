package hgm.poly.integral;

import hgm.poly.*;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 20/01/14
 * Time: 1:33 PM
 * <p/>
 * Integration of a single polytope
 */
public class SymbolicMultiDimPolynomialIntegral {
    boolean DEBUG = false;

    public SymbolicOneDimFunctionGenerator integrate(ConstrainedExpression<Polynomial> cp, final String integrationVar) {
        final int integrationVarIndex = cp.getPolynomialFactory().getVarIndex(integrationVar);
        //excluding the integration var:
//        usableContinuousVarAssign[integrationVarIndex] = null;
//        ConstrainedPolynomial substitutedCP = cp.substitute(usableContinuousVarAssign);
        final List<Polynomial> independentConstraints = new ArrayList<Polynomial>();
        final List<Polynomial> maxLB = new ArrayList<Polynomial>();
        final List<Polynomial> minUB = new ArrayList<Polynomial>();

        for (Polynomial positiveConstraint : cp.getConstraints() ) {
            int degree = positiveConstraint.degree(integrationVarIndex);
            if (degree == 0) {
                independentConstraints.add(positiveConstraint);
            } else if (degree == 1){
                Polynomial[] sortedWrtVar = positiveConstraint.sortWithRespectTo(integrationVarIndex);
//                System.out.println("sortedWrtVar = " + Arrays.toString(sortedWrtVar) + " \t...\t" + integrationVar);
                if (sortedWrtVar.length != 2) throw new RuntimeException("how can it be possible!");
                //ax + b >0
                Polynomial bPoly = sortedWrtVar[0];
                Polynomial aPoly = sortedWrtVar[1];
                Set<List<Double>> aPowers = aPoly.getAllPowers();
                if (aPowers.size() != 1) throw new RuntimeException("how? should be linear.. " + aPoly); //debug
                if (aPoly.degree() != 0) throw new RuntimeException("how? a constant was expected " + aPoly);
                List<Double> aPow = aPowers.iterator().next();
                double a = aPoly.getCoefficient(aPow);//it should be the only non-zero coefficient since a constant is expected
//                System.out.println("a = " + a);
                //making -b/a out of b:
                bPoly.multiplyScalarInThis(-1.0/a);
                //now bPoly is -b/a.
                if (a>0) { //x > -b/a
                    maxLB.add(bPoly);
                } else { //x < -b/a
                    minUB.add(bPoly);
                }
            } else {
                throw new RuntimeException("at present time, only linear constraints are supported");
            }
        }

        //now that bounds are sorted out indefinite integral:
        final Polynomial indefIntegral = cp.getFruit().clone();
        indefIntegral.replaceThisWithIndefiniteIntegral(integrationVar);

        //having 3 lists and indefinite integral we generate a class that when all X\x are instantiated, calculates integral F dx.
        return new SymbolicOneDimFunctionGenerator(){

            @Override
            public String toString() {
                return "value:\t" + indefIntegral +
                        "\nIndependents:\t" + independentConstraints +
                        "\nminUB:\t\t\t" + minUB +
                        "\nmaxLB:\t\t\t" + maxLB + "\n";
            }

            @Override
            public OneDimFunction makeFunction(final Double[] reusableVarAssign) { //NOTE: be very careful about the reusable array
//                if (reusableVarAssign[integrationVarIndex] != null)
//                    throw new RuntimeException("although does not matter, for debug sake please make it NULL"); //to exclude integration var from being instantiated

                //independent constraints and lower/higher bounds do not contain the integration var so we do not need to 'null' it.
                for (Polynomial independent : independentConstraints) {

                    //todo just test:
//                    Double[] debugArr = new Double[]{-8d, -2d};
//                    System.out.println("DEBUG = " + independent.evaluate(debugArr));



                    double v = independent.evaluate(reusableVarAssign);
                    if (v <= 0) {
//                        System.out.println("Returning 0");
                        return OneDimFunction.ZERO_1D_FUNCTION; //what should I do with '='?
                    }
                    //since independent "positive" constraints are not satisfied.
                }

                final double lb = max(maxLB, reusableVarAssign);
                final double ub = min(minUB, reusableVarAssign);

//                System.out.println("lb = " + lb);
//                System.out.println("ub = " + ub);
                if (lb >=ub) return OneDimFunction.ZERO_1D_FUNCTION;

                reusableVarAssign[integrationVarIndex]=lb;
                final double offset1 = indefIntegral.evaluate(reusableVarAssign);
                reusableVarAssign[integrationVarIndex]=ub;
                final double u = indefIntegral.evaluate(reusableVarAssign) - offset1;

                return new OneDimFunction() {
                    @Override
                    public double eval(double var) {
                        if (var < lb) return 0.0;
                        if (var > ub) return u;

                        reusableVarAssign[integrationVarIndex]=var;
                        double f = indefIntegral.evaluate(reusableVarAssign);
                        return f - offset1;
                    }
                };
            }

            private double max(List<Polynomial> polynomials, Double[] reusableVarAssign) {
                double max = Double.NEGATIVE_INFINITY;
                for (Polynomial polynomial : polynomials) {
                       max = Math.max(max, polynomial.evaluate(reusableVarAssign));
                }
                return max;
            }

            private double min(List<Polynomial> polynomials, Double[] reusableVarAssign) {
                double min = Double.POSITIVE_INFINITY;
                for (Polynomial polynomial : polynomials) {
                       min = Math.min(min, polynomial.evaluate(reusableVarAssign));
                }
                return min;
            }
        };
    }
}

