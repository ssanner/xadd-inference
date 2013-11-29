package hgm.utils;

import java.util.List;
import java.util.Map;

/**
 * Created by Hadi Afshar.
 * Date: 22/10/13
 * Time: 11:23 AM
 */
public abstract class Function {
    protected int inputArgumentDimension;
    protected String[] effectiveVarNames;

    public Function(List<String> effectiveVarNames) {
        this(effectiveVarNames.toArray(new String[0]));
    }

    public Function(String[] effectiveVarNames) {
        this.effectiveVarNames = effectiveVarNames;
        inputArgumentDimension = effectiveVarNames.length;
    }

    public int getInputArgumentDimension() {
        return inputArgumentDimension;
    }

    public String[] getEffectiveVarNames() {
        return effectiveVarNames;
    }

    public double calcTarget(Map<String, Double> varAssignment) {
        double[] effectiveVars = new double[inputArgumentDimension];
        for (int i = 0; i < effectiveVarNames.length; i++) {
            effectiveVars[i] = varAssignment.get(effectiveVarNames[i]);
        }
        return func(effectiveVars);
    }

    public abstract double func(double... effectiveVars);
}
