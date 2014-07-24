package hgm.poly.gm;

import hgm.poly.PiecewisePolynomial;
import hgm.poly.Polynomial;
import hgm.sampling.VarAssignment;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 23/07/14
 * Time: 5:05 PM
 */
public class SymbolicGraphicalModelHandler {
    PiecewisePolynomial makeJoint(GraphicalModel gm, List<String> queryVars, Map<String, Double> evidence) {
        //step 0.
        Set<String> queryAndEvidenceVars = new HashSet<String>(queryVars.size() + evidence.size());
        queryAndEvidenceVars.addAll(queryVars);
        queryAndEvidenceVars.addAll(evidence.keySet());

        List<Factor> originalFactors = gm.allInferenceRelevantFactors(queryAndEvidenceVars); //in BNs this is the factors of query, evidence and their ancestors.

        //step 1.
        List<Factor> factors = instantiateObservedFactors(originalFactors, evidence);

    /*    //step 2.
        isolateDeterministicFactors(factors);

        //step 3.
        reduceDimension(factors);

        //step 4.
        return makeStochasticFactorsJoint(factors);*/
        return null;
    }

    List<Factor> instantiateObservedFactors(List<Factor> factors, Map<String, Double> evidence) {
        List<Factor> results = new ArrayList<Factor>(factors.size());
        for (Factor factor : factors) {
            results.add(factor.substitute(evidence));
        }
        return results;
    }

    /*void isolateDeterministicFactors(List<Factor> factors) {
        for (Factor factor : factors) {
            if (factor instanceof DeterministicFactor) {
                Polynomial factorG = ((DeterministicFactor) factor).getEqualityExpression(); //todo in future this may become a piecewise polynomial...
                String factorV = factor.getAssociatedVariable();
                for (Factor otherFactor : factors) {
                    if (!otherFactor.equals(factor)) {
                        otherFactor.substitute(factorV, factorG);
                    }
                }
            }
        }
    }

    private void reduceDimension(List<Factor> factors, PartialVarAssignment evidence, GraphicalModel originalGM) {
        List<DeterministicFactor> deterministicEvidence = new ArrayList<DeterministicFactor>();
        for (String evidenceVar : evidence.keySet()) {
            Factor f = originalGM.getAssociatedFactor(evidenceVar);
            if (f instanceof DeterministicFactor) {
                deterministicEvidence.add((DeterministicFactor) f);
            }

        }
        for (Factor factor : factors) {
            if (factor instanceof DeterministicFactor) {
                Polynomial factorG = ((DeterministicFactor) factor).getEqualityExpression(); //todo in future this may become a piecewise polynomial...
                String factorV = factor.getAssociatedVariable();
                for (Factor otherFactor : factors) {
                    if (!otherFactor.equals(factor)) {
                        otherFactor.substitute(factorV, factorG);
                    }
                }
            }
        }
    }*/
}
