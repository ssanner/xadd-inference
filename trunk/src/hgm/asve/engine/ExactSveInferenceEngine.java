package hgm.asve.engine;

import hgm.*;
import hgm.asve.factor.IFactor;
import hgm.asve.factory.FactorFactory;

import java.util.*;

/**
 * Created by Hadi M Afshar
 * Date: 11/09/13
 * Time: 5:04 PM
 */
public class ExactSveInferenceEngine <T extends IFactor> implements FactorSetBasedInferenceEngine<T> {
    //    private XADDFactory factory = null;
    private List<T> _factors = null;
    private FactorFactory<T> _factory;
    private Set<Variable> _scopeVariables = new HashSet<Variable>();

    public ExactSveInferenceEngine(FactorFactory<T> factory, Collection<T> factorsToInferOn) {
        _factory = factory;

        takeFactors(factorsToInferOn);

    }

    @Override
    public void takeFactors(Collection<T> factors) {
        _factors = new ArrayList(factors);

        _scopeVariables.clear();
        for (IFactor f : factors) {
            _scopeVariables.addAll(f.getScopeVars());
        }

    }

    @Override
    public T infer(IQuery q) {
        return infer(q, bestVariableOrder());
    }

    private ArrayList<Variable> bestVariableOrder() {
        System.err.println("not implemented yet");
        return null;  //TODO implement...
    }

    @Override
    public T infer(IQuery q, List<Variable> varOrdering) {

        // 0. Make sure all scope variables are included in varOrdering:
        if (!varOrdering.containsAll(_scopeVariables)) {
            throw new RuntimeException("some variables not included in the varOrdering");
        }

        // 1. instantiation by evidence //todo re-implement this part
        List<T> instantiatedFactors = _factors;//new ArrayList<HFactor>();//(this.factors.size());
//        for (int i = 0; i < factors.size(); i++) {
//            instantiatedFactors.add(factors.get(i).instantiate(q.getEvidence()));      //todo instantiation should go to factory
//        }
//        if (XADDFactory.DEBUG) {
//            for (int i = 0; i < instantiatedFactors.size(); i++) {
//                System.out.println("instantiatedFactors.get(" + i + ").getXADDNodeString() = " + instantiatedFactors.get(i).getXADDNodeString());
//
//            }
//        }

        // 2. marginalize out variables that are not in query and are not already instantiated
        List<Variable> orderedMarginalizingVariables = new ArrayList<Variable>();
        for (int i = 0; i < varOrdering.size(); i++) {
            Variable var = varOrdering.get(i);
            if (!q.getQueries().contains(var) & !q.getNonInstantiatedEvidenceVariables().contains(var)) {
                orderedMarginalizingVariables.add(var);
            }
        }

        return inferenceWithVE(instantiatedFactors, orderedMarginalizingVariables);
    }

    private T inferenceWithVE(List<T> instantiatedFactors, List<Variable> varsToEliminate) {
        Set<T> phi = new HashSet<T>(instantiatedFactors); // initialized with all factors

        for (int i = 0; i < varsToEliminate.size(); i++) {
            Variable z = varsToEliminate.get(i);
            Set<T> factorsInvolvingZ = chooseFactorsIncluding(phi, z);
            phi.removeAll(factorsInvolvingZ); // phi = phi \ factors that involve z
            T psi = _factory.multiply(factorsInvolvingZ);
            T tau = _factory.marginalize(psi, z);
            phi.add(tau);
        }

        if (phi.size() == 1)
            return phi.iterator().next();
        else
            return _factory.multiply(phi);

    }

    private Set<T> chooseFactorsIncluding(Set<T> factors, Variable var) {
        Set<T> factorsWithVar = new HashSet<T>();
        for (T f : factors) {
            if (f.getScopeVars().contains(var)) {
                factorsWithVar.add(f);
            }
        }
        return factorsWithVar;
    }
}
