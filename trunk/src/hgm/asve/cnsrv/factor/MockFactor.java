package hgm.asve.cnsrv.factor;


/**
 * Created by Hadi M Afshar
 * Date: 9/09/13
 * Time: 7:58 PM
 */

import java.util.HashSet;
import java.util.Set;

/**
 * A dummy class used for test and debugging
 */
public class MockFactor implements IFactor {
    private Set<String> scope;

    public static final String A = "[A]";
    public static final String B = "[B]";
    public static final String C = "[C]";
    public static final String D = "[D]";
    public static final String E = "[E]";
    public static final String F = "[F]";
    public static final String G = "[G]";
    public static final String T = "[T]";
    public static final String U = "[U]";
    public static final String V = "[V]";
    public static final String W = "[W]";
    public static final String X = "[X]";
    public static final String Y = "[Y]";
    public static final String Z = "[Z]";


    //todo use regular expressions instead...
    private static final String[] possibleVariableNames = {A, B, C, D, E, F, G, T, U, V, W, X, Y, Z};

    private String factorText;

    public MockFactor(String factorText) {
        this.factorText = factorText;
        scope = new HashSet<String>();

        for (String possibleVariable : possibleVariableNames) {
            if (factorText.contains(possibleVariable)) {
                scope.add(possibleVariable);
            }
        }
    }

    @Override
    public Set<String> getScopeVars() {
        return scope;
    }

    @Override
    public String getHelpingText() {
        return factorText;
    }

    @Override
    public String toString() {
        return factorText;
    }

/*    private boolean isMulitiplicationOfVars = false;

    public boolean isMulitiplicationOfVars() {
        return isMulitiplicationOfVars;
    }

    public void setMulitiplicationOfVars(boolean mulitiplicationOfVars) {
        isMulitiplicationOfVars = mulitiplicationOfVars;
    }*/
}
