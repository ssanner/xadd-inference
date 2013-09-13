package sveApprox;

import java.util.*;

/**
 * Created by Hadi M Afshar
 * Date: 11/09/13
 * Time: 5:04 PM
 */
public class FactorList {
    private XADDFactory factory = null;
    private List<HFactor> factors = new ArrayList<HFactor>();
    private Set<String> scopeVars = new HashSet<String>();

    public FactorList(HFactor... factors) {
        for (int i = 1; i < factors.length; i++) {
            HFactor f = factors[i];
            if (factory == null) {
                factory = f.getFactory();
            } else if (f.getFactory() != factory) {
                throw new RuntimeException("all factors should correspond a same factory");
            }

            this.factors.add(f);
            this.scopeVars.addAll(f.getScopeVars());
        }
    }

    public HFactor infer(HQuery q) {
        return infer(q, bestVariableOrder());
    }

    private ArrayList<String> bestVariableOrder() {
    return null;  //TODO implement...
    }

    public HFactor infer(HQuery q, List<String> varOrdering) {

        // 1. instantiation by evidence
        List<HFactor> instantiatedFactors = new ArrayList<HFactor>();//(this.factors.size());
        for (int i = 0; i < factors.size(); i++) {
            instantiatedFactors.add(factors.get(i).instantiate(q.getEvidence()));
        }

        // 2. marginalize out variables that are not in query and are not already instantiated
        List<String> orderedMarginalizingVariables = new ArrayList<String>();
        for (int i = 0; i < varOrdering.size(); i++) {
            String var = varOrdering.get(i);
            if (!q.getQueries().contains(var) & !q.getEvidenceVariables().contains(var)) {
                orderedMarginalizingVariables.add(var);
            }
        }

        return marginalizeWithVE(instantiatedFactors, orderedMarginalizingVariables);
    }

    private HFactor marginalizeWithVE(List<HFactor> instantiatedFactors, List<String> varsToEliminate) {
        Set<HFactor> phi = new HashSet<HFactor>(instantiatedFactors); // initialized with all factors

        for (int i = 0; i < varsToEliminate.size(); i++) {
            String z = varsToEliminate.get(i);
            Set<HFactor> factorsInvolvingZ = chooseFactorsIncluding(phi, z);
            phi.removeAll(factorsInvolvingZ); // phi = phi \ factors that involve z
            HFactor psi = factory.multiply(factorsInvolvingZ);
            HFactor tau = factory.definiteIntegral(psi, z);
            phi.add(tau);
        }

        if (phi.size() == 1)
            return phi.iterator().next();
        else
            return factory.multiply(phi);

    }

    private Set<HFactor> chooseFactorsIncluding(Set<HFactor> factors, String var) {
        Set<HFactor> factorsWithVar = new HashSet<HFactor>();
        for (HFactor f : factors) {
            if (f.getScopeVars().contains(var)) {
                factorsWithVar.add(f);
            }
        }
        return factorsWithVar;
    }
}
