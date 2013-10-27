package hgm.asve.cnsrv.factory;

import hgm.asve.cnsrv.approxator.*;
import hgm.asve.cnsrv.gm.FBGraphicalModel;
import hgm.asve.cnsrv.gm.FBQuery;
import hgm.asve.cnsrv.factor.FactorVisualizer;
import hgm.asve.cnsrv.factor.Factor;
import xadd.XADD;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 1/10/13
 * Time: 2:23 PM
 */
public class ModelBasedXaddFactorFactory extends BaselineXaddFactorFactory {
    protected FBGraphicalModel gm;
    protected Map<String, Factor> instantiatedVarFactorMap; //association map after initialization
    protected Map<Factor, String> instantiatedFactorVarMap;
    protected Map<String, Set<String>> varParentsMap;
    protected FBQuery query;

    public ModelBasedXaddFactorFactory(String modelFileName, FBQuery instantiatingQuery) {
        super(null);
        gm = new FBGraphicalModel(modelFileName);
        query = instantiatingQuery;
        instantiateFactoryWithQuery(instantiatingQuery);

        //the factors associated with variables will never be flushed away.
        permanentFactors = new HashSet<Factor>(instantiatedFactorVarMap.keySet());
    }

    private void instantiateFactoryWithQuery(FBQuery q) {
        // instantiateGMTemplate:
        gm.instantiateGMTemplate(q.getVar2Expansion());
        this.context = gm._context;

        instantiatedVarFactorMap = gm.instantiateByVariableAssignment(gm._varFactorMap, q.getContinuousInstantiatedEvidence(), q.getBooleanInstantiatedEvidence());
        instantiatedFactorVarMap = invertMap(instantiatedVarFactorMap);
        varParentsMap = populateVarParentMap(instantiatedVarFactorMap);
    }

    public FBQuery getQuery() {
        return query;
    }


    //    @Deprecated
//    public Factor approximate(Factor factor, double massThreshold, double volumeThreshold) {
//        int approximatedNodeId = context.approximateXADD(factor._xadd, massThreshold, volumeThreshold);
//        return new Factor(approximatedNodeId, context, "~" + factor.getHelpingText());
//    }


    public String getAssociatedVariable(Factor factor) {
        return instantiatedFactorVarMap.get(factor);
    }


    public Factor getAssociatedInstantiatedFactor(String v) {
        return instantiatedVarFactorMap.get(v);
    }

    // Functionality of graphical model:
    public List<Factor> getAssociatedInstantiatedFactors(Collection<String> vars) {
        List<Factor> associatedFactors = new ArrayList<Factor>(vars.size());
        for (String v : vars) {
            associatedFactors.add(getAssociatedInstantiatedFactor(v));

        }
        return associatedFactors;
    }

    public Set<String> getParents(String variable) {
        return varParentsMap.get(variable);
    }

    private Map<Factor, String> invertMap(Map<String, Factor> varFactorMap) {
        Map<Factor, String> invertedMap = new HashMap<Factor, String>(varFactorMap.size());

        for (Map.Entry<String, Factor> entry : varFactorMap.entrySet()) {
            invertedMap.put(entry.getValue(), entry.getKey());
        }

        return invertedMap;
    }


    private Map<String, Set<String>> populateVarParentMap(Map<String, Factor> varFactorMap) {
        Map<String, Set<String>> varParents = new HashMap<String, Set<String>>();
        for (String var : varFactorMap.keySet()) {
            Set<String> parents = varParents.get(var);
            if (parents == null) {
                parents = new HashSet<String>();
                varParents.put(var, parents);
            }

            Factor f = varFactorMap.get(var);
            for (String scopeVar : f.getScopeVars()) {
                if (!scopeVar.equals(var)) {
                    //every variable occurred in the factor associated with var except var itself is its parent:
                    parents.add(scopeVar);
                }
            }
        }

        return varParents;
    }


    public int countLeafCount(Factor approxResultF) {
        return context.getLeafCount(approxResultF._xadd);
    }

    public Collection<? extends String> getAllVariables() {
        return this.gm.getAllVariables();
    }

}
