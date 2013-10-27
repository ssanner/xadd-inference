package hgm.asve.cnsrv.gm;

import hgm.IQuery;
import hgm.InstantiatedVariable;
import hgm.Variable;
import sve.Query;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 1/10/13
 * Time: 3:33 PM
 */
public class FBQuery {
    private Query q;

    public FBQuery(String queryFileName) {
        q = new Query(queryFileName);
    }

    public Map<String, Double> getContinuousInstantiatedEvidence() {
        return q._hmCVarAssign;
    }
    public Map<String, Boolean> getBooleanInstantiatedEvidence() {
        return q._hmBVarAssign;
    }

    public Set<String> getNonInstantiatedEvidenceVariables() {
        System.err.println("Warning! <getNonInstantiatedEvidenceVariables> in FileBasedQuery is not implemented");
        return new HashSet<String>();
    }

    public List<String> getQueryVariables() {
        return q._alQueryVars;
    }

    public HashMap<String,ArrayList<Integer>> getVar2Expansion(){
         return q._hmVar2Expansion;
    }

    public Set<String> fetchInstantiatedEvidenceVariables() {
        Set<String> instantiatedVars = new HashSet<String>(q._hmBVarAssign.keySet());
        instantiatedVars.addAll(q._hmCVarAssign.keySet());
        return instantiatedVars;
    }
}
