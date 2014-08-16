package hgm.poly.gm;

import java.util.Collection;
import java.util.List;

/**
 * Created by Hadi Afshar.
 * Date: 3/07/14
 * Time: 4:02 AM
 *
 * Graphical model used for symbolic integral
 */
public interface GraphicalModel {
    /**
     * It should be guaranteed that [at least in the case of deterministic factors] parents come before children
     */
    List<Factor> allInferenceRelevantFactors(Collection<String> vars);

//    Factor getAssociatedFactor(String var);

    List<String> allDeterministicVars();


   /* PolynomialFactory factory;
    List<Factor> factors;

    public GraphicalModel(List<Factor> factors) {
        this.factors = factors;

        factory = factors.get(0).getPiecewisePolynomial().getFactory();
        for (Factor factor : factors) {
            if (factor.getPiecewisePolynomial().getFactory() != factory) {
                throw new RuntimeException();
            }
        }
    }

    //todo later on only parents of evidence and query nodes should be multiplied...
    Factor jointOfAllFactors() {
        Polynomial result = factory.one().clone();
        for (Factor factor : factors) {
            PiecewisePolynomial p = factor.getPiecewisePolynomial();
            result = result.multiply(p);

        }

    }*/
}
