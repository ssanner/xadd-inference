package hgm.asve.cnsrv.factory;


import hgm.asve.cnsrv.factor.IFactor;

import java.util.Collection;
import java.util.Map;

/**
 * Created by Hadi Afshar.
 * Date: 20/09/13
 * Time: 8:52 PM
 */
public interface FactorFactory<F extends IFactor> extends ElementaryFactorFactory<F> {
    F multiply(Collection<F> factors);

    F marginalize(F factor, String variable);

    //todo should accept parameters as a class
    F approximate(F factor, double param1, double param2);

}
