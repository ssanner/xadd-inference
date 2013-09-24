package hgm.asve.model;

import hgm.Variable;
import hgm.asve.factor.IFactor;

import java.util.List;
import java.util.Set;

/**
 * Created by Hadi Afshar.
 * Date: 19/09/13
 * Time: 2:32 PM
 */
public interface BayesianGraphicalModel<F extends IFactor> extends Cloneable{
    Set<F> getParents(F factor);
    int calcMaxDistanceFromLeaf(F factor);

    /**
     * @return a list of factors due to the order by which they should preferably be eliminated
     */
    List<F> getSortedFactors();

    /**
     * @return a shallow clone
     */
    BayesianGraphicalModel clone();

    F getFactor(Variable variable);

    List<F> getFactors(List<Variable> vars);

}
