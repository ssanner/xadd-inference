package sveApprox;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Hadi M Afshar
 * Date: 10/09/13
 * Time: 2:33 PM
 */
@Deprecated
public class FactorSet {
    private Set<Old_Factor> factors;

    public FactorSet(Old_Factor... factors) {
        this.factors = new HashSet<Old_Factor>(factors.length);
        for (Old_Factor factor : factors) {
            this.factors.add(factor);
        }
    }

    public String[] getScope() {
        Set<String> scope = new HashSet<String>();
        for (Old_Factor factor : factors) {
            scope.addAll(factor.getScope());
        }

        return scope.toArray(new String[]{});
    }

    public Old_Factor marginalize(String[] varsToEliminate) {
        Set<Old_Factor> phi = new HashSet<Old_Factor>(this.factors);

        for (int i = 0; i < varsToEliminate.length; i++) {
            String z = varsToEliminate[i];
            Set<Old_Factor> factorsInvolvingZ = chooseFactorsIncluding(phi, z);
            phi.removeAll(factorsInvolvingZ); // phi = phi \ factors that involve z
            Old_Factor psi = product(factorsInvolvingZ);
            Old_Factor tau = summation(psi, z);
            phi.add(tau);
        }

        if (phi.size() == 1)
            return new Old_Factor(phi.iterator().next().getValue());
        else
            return new Old_Factor(product(phi).getValue());

    }

    private Old_Factor summation(Old_Factor psi, String variable) {
        String varLowBound = variable + "_l";
        String varHighBound = variable + "_h";
        return new Old_Factor("Sum_{" + variable + "in (" + varLowBound + ", " + varHighBound + ")}\n<" + psi.getValue() + ">\n");
    }

    private Old_Factor product(Set<Old_Factor> factorsWithZinScope) {
        String productFactorValue = "[";
        for (Old_Factor f : factorsWithZinScope) {
            productFactorValue += "(" + f.getValue() + ").";
        }
        productFactorValue = productFactorValue.substring(0, productFactorValue.length() - 1) + "]";
        return new Old_Factor(productFactorValue);
    }

    private Set<Old_Factor> chooseFactorsIncluding(Set<Old_Factor> factorsToBeFiltered, String filteringVariable) {
        Set<Old_Factor> filteredFactors = new HashSet<Old_Factor>();

        for (Old_Factor factor : factorsToBeFiltered) {
            if (factor.getScope().contains(filteringVariable)) {
                filteredFactors.add(factor);
            }
        }
        return filteredFactors;
    }
}
