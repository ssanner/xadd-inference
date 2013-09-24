package hgm;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Hadi M Afshar
 * Date: 11/09/13
 * Time: 8:42 PM
 */
public class QueryImpl implements IQuery {
    private Set<InstantiatedVariable> _evidence;
    private List<Variable> _queries;

    public QueryImpl(Set<InstantiatedVariable> evidence, List<Variable> queries) {
        this._evidence = evidence;
        this._queries = queries;
    }

    @Override
    public Set<InstantiatedVariable> getEvidence() {
        return _evidence;
    }

    @Override
    public Set<Variable> getNonInstantiatedEvidenceVariables() {
        Set<Variable> evidenceVars = new HashSet<Variable>(_evidence.size());
        for (InstantiatedVariable instantiatedVariable : _evidence) {
            evidenceVars.add(instantiatedVariable.getVariable());
        }
        return evidenceVars;
    }

    @Override
    public List<Variable> getQueries() {
        return _queries;
    }

    @Override
    public String toString() {
        return "HQuery{" +
                "evidence=" + _evidence +
                ", queries=" + _queries +
                '}';
    }
}
