package hgm.poly.gm;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 23/07/14
 * Time: 10:12 PM
 */
public class BayesianNetworkGraphicalModel implements GraphicalModel {
    private Map<String, Factor> varToFactorMap = new HashMap<String, Factor>();
    private Map<String, Set<String>> varToParents = new HashMap<String, Set<String>>();


    public void addFactor(Factor f) {
        String associatedVar = f.getAssociatedVar();
        if (varToFactorMap.put(associatedVar, f) != null) throw new RuntimeException("factor associated with " + associatedVar + " already exists");

        Set<String> parents = new HashSet<String>(f.getPiecewisePolynomial().getScopeVars());
        parents.remove(associatedVar); //associated var is not a parent of itself

//        List<Factor> parentFactors = new ArrayList<Factor>(parents.size());

        //just for test:
        for (String parent : parents) {
            Factor parentF = varToFactorMap.get(parent);
            if (parentF == null) {
                throw new RuntimeException("unknown parent " + parent);
            }
//            parentFactors.add(parentF);
        }

        varToParents.put(associatedVar, parents);
    }


    //todo test
    /**
     * In BNs it is guaranteed that ALL parents are returned before children
     */
    @Override
    public List<Factor> allInferenceRelevantFactors(Collection<String> vars) {
        List<Factor> results = new ArrayList<Factor>();
        for (String var : vars) {
            Set<String> parents = varToParents.get(var);
            List<Factor> parentsResults = allInferenceRelevantFactors(parents);
            results.addAll(parentsResults);
            results.add(varToFactorMap.get(var));
        }
        return results;
    }

    @Override
    public Factor getAssociatedFactor(String var) {
        return varToFactorMap.get(var);
    }
}
