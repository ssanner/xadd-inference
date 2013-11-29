package hgm.asve.cnsrv.factory;

import hgm.asve.cnsrv.approxator.*;
import hgm.asve.cnsrv.gm.FBGraphicalModel;
import hgm.asve.cnsrv.gm.FBQuery;
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

    public static ModelBasedXaddFactorFactory newInstance(String modelFileName, FBQuery query, Approximator approximator) {
        FBGraphicalModel gm = new FBGraphicalModel(modelFileName);

        gm.instantiateGMTemplate(query.getVar2Expansion());
        XADD context = gm._context;

        //now context is made and can be passed to the approximator:
        approximator.setupWithContext(context);

        //and to the (parent) constructor of this class:
        return new ModelBasedXaddFactorFactory(gm, query, approximator, context);
    }

    private ModelBasedXaddFactorFactory(FBGraphicalModel gm, FBQuery instantiatingQuery, Approximator approximator, XADD context) {
        super(context, approximator);
        this.gm = gm;
        this.query = instantiatingQuery;

        // instantiating factory with query
        instantiatedVarFactorMap = gm.instantiateByVariableAssignment(gm._varFactorMap, query.getContinuousInstantiatedEvidence(), query.getBooleanInstantiatedEvidence());
        instantiatedFactorVarMap = invertMap(instantiatedVarFactorMap);
        varParentsMap = populateVarParentMap(instantiatedVarFactorMap);

        //the factors associated with variables will never be flushed away.
        makePermanent(instantiatedFactorVarMap.keySet());
    }

    public FBQuery getQuery() {
        return query;
    }


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

    public Collection<? extends String> getAllVariables() {
        return this.gm.getAllVariables();
    }

}
