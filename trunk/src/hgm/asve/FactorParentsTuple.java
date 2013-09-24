package hgm.asve;

import hgm.Variable;
import hgm.asve.factor.IFactor;

/**
 * Created by Hadi Afshar.
 * Date: 19/09/13
 * Time: 10:42 PM
 */

/**
 * An intermediate simple wrapper
 */
public class FactorParentsTuple<F extends IFactor> {
    private F[] _parents;
    private F _factor;

    public FactorParentsTuple(F factor, F[] parents) {
        _parents = parents;
        _factor = factor;
    }

    public F[] getParents() {
        return _parents;
    }

    public F getFactor() {
        return _factor;
    }
}
