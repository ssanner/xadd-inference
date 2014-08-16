package hgm.poly.pref;

import hgm.poly.integral.OneDimFunction;
import hgm.poly.integral.SymbolicOneDimFunctionGenerator;

import java.util.Arrays;

public class SymbolicCdfArrayHandler {
    SymbolicOneDimFunctionGenerator[] generators;
    final OneDimFunction[] reusableInstantiatedFunctions;

    public SymbolicCdfArrayHandler(SymbolicOneDimFunctionGenerator[] generators) {
        this.generators = generators;
        reusableInstantiatedFunctions = new OneDimFunction[generators.length];
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

        return new OneDimFunction() {
            @Override
            public double eval(double var) {
                double result = 0d;
                for (OneDimFunction polyCDF : reusableInstantiatedFunctions) {
                    result += polyCDF.eval(var);
                }

                return result;
            }
        };
    }
}
