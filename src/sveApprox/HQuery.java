package sveApprox;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Hadi M Afshar
 * Date: 11/09/13
 * Time: 8:42 PM
 */
public class HQuery {
    private Set<VariableValue> evidence;
    private List<String> queries;

    public HQuery(Set<VariableValue> evidence, List<String> queries) {
        this.evidence = evidence;
        this.queries = queries;
    }

    public Set<VariableValue> getEvidence() {
        return evidence;
    }

    public Set<String> getEvidenceVariables() {
        Set<String> evidenceVars = new HashSet<String>(evidence.size());
        for (VariableValue variableValue : evidence) {
            evidenceVars.add(variableValue.getVariable());
        }
        return evidenceVars;
    }

    public List<String> getQueries() {
        return queries;
    }

    @Override
    public String toString() {
        return "HQuery{" +
                "evidence=" + evidence +
                ", queries=" + queries +
                '}';
    }
}
