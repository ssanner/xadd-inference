package sveApprox;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Hadi M Afshar
 * Date: 9/09/13
 * Time: 7:58 PM
 */
@Deprecated
public class Old_Factor {
    private Set<String> scope;
    private static final String[] possibleVariableNames = {"[T]", "[U]", "[V]", "[W]", "[X]", "[Y]", "[Z]"};
    private String value;

    public Old_Factor(String value) {
        this.value = value;
        scope = new HashSet<String>();

        for (String possibleVariable : possibleVariableNames) {
            if (value.contains(possibleVariable)) {
                 scope.add(possibleVariable);
            }
        }
//        String[] vars = scope.split(" ");
//        this.scope = new HashSet<String>(Arrays.asList(vars));
    }

    public Set<String> getScope() {
        return scope;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value + "{" +
                "scope=" + scope +
                '}';
    }
}
