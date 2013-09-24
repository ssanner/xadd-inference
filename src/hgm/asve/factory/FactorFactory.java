package hgm.asve.factory;

import hgm.Variable;
import hgm.asve.factor.IFactor;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Created by Hadi Afshar.
 * Date: 20/09/13
 * Time: 8:52 PM
 */
public interface FactorFactory<E extends IFactor> {
    E multiply(Collection<E> factors);
    E marginalize(E factor, Variable v);
    E approximate(E factor);
}
