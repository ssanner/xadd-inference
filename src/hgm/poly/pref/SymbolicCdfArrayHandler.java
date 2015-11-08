package hgm.poly.pref;

import hgm.poly.Expression;
import hgm.poly.Fraction;
import hgm.poly.PiecewiseExpression;
import hgm.poly.integral.OneDimFunction;
import hgm.poly.integral.SymbolicOneDimFunctionGenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SymbolicCdfArrayHandler {
    private final List<PiecewiseExpression<Fraction>> factorsNotInvolvingIntegrand; //just should be multiplied in the final result

    SymbolicOneDimFunctionGenerator[] generators;
    final OneDimFunction[] reusableInstantiatedFunctions;

    public SymbolicCdfArrayHandler(SymbolicOneDimFunctionGenerator[] generators) {
          this(generators, new ArrayList<PiecewiseExpression<Fraction>>());
    }

    public SymbolicCdfArrayHandler(SymbolicOneDimFunctionGenerator[] generators,
                                   List<PiecewiseExpression<Fraction>> factorsNotInvolvingIntegrand) {
        this.generators = generators;
        reusableInstantiatedFunctions = new OneDimFunction[generators.length];
        this.factorsNotInvolvingIntegrand = factorsNotInvolvingIntegrand;
    }

    @Override
    public String toString() {
        return "Generators: " + Arrays.toString(generators);
    }

    //note that each instantiation changes the result of the former due to reused object
    public OneDimFunction instantiate(Double[] varAssign) {
        for (int i = 0; i < generators.length; i++) {
            SymbolicOneDimFunctionGenerator generator = generators[i];
            OneDimFunction segmentCdf = generator.makeFunction(varAssign);
//            if (!segmentCdf.equals(OneDimFunction.ZERO_1D_FUNCTION)) {
            reusableInstantiatedFunctions[i] = segmentCdf;
        }


       /* for (int i = 0, reusableInstantiatedFunctionsLength = reusableInstantiatedFunctions.length; i < reusableInstantiatedFunctionsLength; i++) {
            OneDimFunction reusableInstantiatedFunction = reusableInstantiatedFunctions[i];
            FunctionVisualizer.visualize(reusableInstantiatedFunction, -20d, 20d, 0.01, "sub func. #" + i);
        }*/

        double c=1.0;
        for (PiecewiseExpression mult : factorsNotInvolvingIntegrand) {
             c = c*mult.evaluate(varAssign);
        }

        final double finalC = c;
        return new OneDimFunction() {

            @Override
            public double eval(double var) {
                double result = 0d;
                for (OneDimFunction polyCDF : reusableInstantiatedFunctions) {
                    result += polyCDF.eval(var);
                }

                //now multiply in the factors not involving the integrand:

                return finalC *result;
            }
        };
    }
}
