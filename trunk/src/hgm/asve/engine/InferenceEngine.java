package hgm.asve.engine;

import hgm.IQuery;
import hgm.Variable;
import hgm.asve.factor.IFactor;

import java.util.List;

/**
 * Created by Hadi Afshar.
 * Date: 19/09/13
 * Time: 6:55 PM
 */

public interface InferenceEngine<F extends IFactor> {
    public F infer(IQuery q);
    public F infer(IQuery q, List<Variable> varOrdering);
}
