package hgm.sampling;

import hgm.asve.Pair;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Hadi Afshar.
 * Date: 18/12/13
 * Time: 11:48 PM
 */
public class VarAssignment{
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
}
