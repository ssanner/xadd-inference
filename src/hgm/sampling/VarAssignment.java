package hgm.sampling;

import hgm.asve.Pair;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 18/12/13
 * Time: 11:48 PM
 */
public class VarAssignment {
    private HashMap<String, Double> continuousVarAssign;
    private HashMap<String, Boolean> booleanVarAssign;

    public VarAssignment(HashMap<String, Boolean> booleanVarAssign, HashMap<String, Double> continuousVarAssign) {
        this.booleanVarAssign = booleanVarAssign;
        this.continuousVarAssign = continuousVarAssign;
    }

    public HashMap<String, Double> getContinuousVarAssign() {
        return continuousVarAssign;
    }

    public HashMap<String, Boolean> getBooleanVarAssign() {
        return booleanVarAssign;
    }

    @Override
    public String toString() {
        return "{" +
                "continuousAssign=" + continuousVarAssign +
                "\n, booleanAssign=" + booleanVarAssign +
                '}';
    }

    public int numberOfVars() {
        return booleanVarAssign.size() + continuousVarAssign.size();
    }

    public void assignExistingContinuousVariable(String var, Double value) {
        if (!continuousVarAssign.containsKey(var))
            throw new RuntimeException("no such continuous variable exists: " + var);
        continuousVarAssign.put(var, value);
    }

    public void assignOrCreateContinuousVariable(String var, Double value) {
        continuousVarAssign.put(var, value);
    }

    public double getContinuousVar(String var) {
        return continuousVarAssign.get(var);
    }

    /**
     * sorts variables in a Lexicographical order
     */
    public Double[] getContinuousVarAssignAsArray(String varNameBase) {
        Double[] result = new Double[continuousVarAssign.size()];
        for (int i = 0; i < continuousVarAssign.size(); i++) {
            Double value = continuousVarAssign.get(varNameBase + "_" + i);
            if (value == null) throw new RuntimeException("cannot take the value of " + varNameBase + "_" + i);
            result[i] = value;
        }

        return result;
    }
}
