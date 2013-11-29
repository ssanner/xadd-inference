package hgm.asve.engine;

import hgm.asve.factor.OLD_IFactor;

import java.util.Collection;

/**
 * Created by Hadi Afshar.
 * Date: 20/09/13
 * Time: 4:54 PM
 */

/**
 * an inference engine that exclusively needs a collection of factors
 *
 * @param <E> factor
 */
@Deprecated
public interface OLD_FactorSetBasedInferenceEngine<E extends OLD_IFactor> extends InferenceEngine<E> {
    /**
     * This method should be called before inference
     *
     * @param factors A collection of factors is enough for inference and can be changed...
     */
    public void takeFactors(Collection<E> factors);
}
