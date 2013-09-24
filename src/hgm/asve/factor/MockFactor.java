package hgm.asve.factor;

import hgm.Variable;
import hgm.asve.factor.IFactor;

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
public class MockFactor implements IFactor {
    private Set<Variable> scope;
    public static final Variable A = new Variable("[A]");
    public static final Variable B = new Variable("[B]");
    public static final Variable C = new Variable("[C]");
    public static final Variable D = new Variable("[D]");
    public static final Variable E = new Variable("[E]");
    public static final Variable F = new Variable("[F]");
    public static final Variable G = new Variable("[G]");

    public static final Variable T = new Variable("[T]");
    public static final Variable U = new Variable("[U]");
    public static final Variable V = new Variable("[V]");
    public static final Variable W = new Variable("[W]");
    public static final Variable X = new Variable("[X]");
    public static final Variable Y = new Variable("[Y]");
    public static final Variable Z = new Variable("[Z]");

    //TODO with regex catch [capital letter]
    private static final Variable[] possibleVariableNames = {A, B, C, D, E, F, G, T, U, V, W, X, Y, Z};

    private String factorText;
    private Variable var;

    public MockFactor(String factorText) {
        this(null, factorText);
    }

    public MockFactor(Variable var, String factorText) {
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
//        +"{" + "scope=" + scope + '}';
    }

    @Override
    public Variable getAssociatedVar() {
        return var;
    }
}
