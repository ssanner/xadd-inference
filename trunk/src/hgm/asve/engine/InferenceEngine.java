package hgm.asve.engine;

import hgm.IQuery;
import hgm.Variable;
import hgm.asve.factor.OLD_IFactor;

import java.util.List;

/**
 * Created by Hadi Afshar.
 * Date: 19/09/13
 * Time: 6:55 PM
 */

@Deprecated
public interface InferenceEngine<F extends OLD_IFactor> {
    public F infer(IQuery q);
    public F infer(IQuery q, List<Variable> varOrdering);
}
