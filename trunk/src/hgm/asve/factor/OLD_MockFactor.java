package hgm.asve.factor;

import hgm.ContinuousVariable;
import hgm.Variable;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Hadi M Afshar
 * Date: 9/09/13
 * Time: 7:58 PM
 */

/**
 * A dummy class used for test and debugging
 */
@Deprecated
public class OLD_MockFactor implements OLD_IFactor {
    private Set<Variable> scope;
    public static final Variable A = new ContinuousVariable("[A]", -10d, 20d);
    public static final Variable B = new ContinuousVariable("[B]", -10d, 20d);
    public static final Variable C = new ContinuousVariable("[C]", -10d, 20d);
    public static final Variable D = new ContinuousVariable("[D]", -10d, 20d);
    public static final Variable E = new ContinuousVariable("[E]", -10d, 20d);
    public static final Variable F = new ContinuousVariable("[F]", -10d, 20d);
    public static final Variable G = new ContinuousVariable("[G]", -10d, 20d);

    public static final Variable T = new ContinuousVariable("[T]", -10d, 20d);
    public static final Variable U = new ContinuousVariable("[U]", -10d, 20d);
    public static final Variable V = new ContinuousVariable("[V]", -10d, 20d);
    public static final Variable W = new ContinuousVariable("[W]", -10d, 20d);
    public static final Variable X = new ContinuousVariable("[X]", -10d, 20d);
    public static final Variable Y = new ContinuousVariable("[Y]", -10d, 20d);
    public static final Variable Z = new ContinuousVariable("[Z]", -10d, 20d);

    //TODO with regex catch [capital letter]
    private static final Variable[] possibleVariableNames = {A, B, C, D, E, F, G, T, U, V, W, X, Y, Z};

    private String factorText;
    private Variable var;

    public OLD_MockFactor(String factorText) {
        this(null, factorText);
    }

    public OLD_MockFactor(Variable var, String factorText) {
        this.factorText = factorText;
        scope = new HashSet<Variable>();
        this.var = var;

        for (Variable possibleVariable : possibleVariableNames) {
            if (factorText.contains(possibleVariable.getName())) {
                scope.add(possibleVariable);
            }
        }

        if (var != null && !scope.contains(var)) throw new RuntimeException("variable not if factor text");
    }

    @Override
    public Set<Variable> getScopeVars() {
        return scope;
    }

    public String getText() {
        return factorText;
    }

    @Override
    public String toString() {
        return factorText;
    }

    @Override
    public Variable getAssociatedVar() {
        return var;
    }
}
