package hgm.asve.factory;

import hgm.Variable;
import hgm.asve.factor.OLD_IFactor;

import java.util.Collection;

/**
 * Created by Hadi Afshar.
 * Date: 20/09/13
 * Time: 8:52 PM
 */
@Deprecated
public interface OLD_FactorFactory<E extends OLD_IFactor> {
    E multiply(Collection<E> factors);
    E marginalize(E factor, Variable v);
    E approximate(E factor);
}
