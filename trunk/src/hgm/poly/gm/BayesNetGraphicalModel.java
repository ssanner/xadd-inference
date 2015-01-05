package hgm.poly.gm;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 23/07/14
 * Time: 10:12 PM
 */
public class BayesNetGraphicalModel implements GraphicalModel {
    private Map<String, Factor> varToFactorMap = new HashMap<String, Factor>();
    private Map<String, Set<String>> varToParents = new HashMap<String, Set<String>>();

    private List<String> deterministicVars = new ArrayList<String>();


    public void addFactor(VarAssociatedFactor f) {
        String associatedVar = f.getAssociatedVar();
        if (varToFactorMap.put(associatedVar, f) != null) throw new RuntimeException("factor associated with " + associatedVar + " already exists");

        if (f instanceof DeterministicFactor) {
            deterministicVars.add(associatedVar);
        }

        Set<String> parents = f.getParentVars();

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

    /**
     * In BNs it is guaranteed that ALL parents are returned before children
     */
    @Override
    public List<Factor> allInferenceRelevantFactors(Collection<String> vars) {
        List<Factor> results = new ArrayList<Factor>();
        for (String var : vars) {
            Set<String> parents = varToParents.get(var);
            if (parents==null) throw new RuntimeException("variable '" + var + "' is not in the BN");
            List<Factor> parentsResults = allInferenceRelevantFactors(parents);
            for (Factor parentRelevantF : parentsResults) {
                if (!results.contains(parentRelevantF)) {
                    results.add(parentRelevantF);
                }
            }

            Factor currentF = varToFactorMap.get(var);
            if (!results.contains(currentF)) {
                results.add(currentF);
            }
        }
        return results;
    }

    public Factor getAssociatedFactor(String var) {
        return varToFactorMap.get(var);
    }

    @Override
    public List<String> allDeterministicVars() {
        return deterministicVars;
    }


    //The first entry of the list should be the factor associated with the var itself
    public List<Factor> fetchAssociatedFactorAndAncestorFactors(String var) {
        Set<String> ancestors = fetchAncestors(var);
        List<Factor> fs = new ArrayList<Factor>(ancestors.size() + 1);
        fs.add(getAssociatedFactor(var)); //first self
        for (String ancestor : ancestors) {
            fs.add(getAssociatedFactor(ancestor));
        }
        return fs;
    }

    public Set<String> getParents(String var){
        return varToParents.get(var);
    }

    //Note: the set of ancestors does not include the 'var' itself
    private Set<String> fetchAncestors(String var) {
        Set<String> ancestors = new HashSet<String>();
        for (String par:getParents(var)){
            ancestors.addAll(fetchAncestors(par));
            ancestors.add(par);
        }
        return ancestors;
    }


}
