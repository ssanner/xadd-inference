package hgm.asve.model;

import hgm.Variable;
import hgm.asve.factor.OLD_IFactor;

import java.util.List;
import java.util.Set;

/**
 * Created by Hadi Afshar.
 * Date: 19/09/13
 * Time: 2:32 PM
 */
public interface BayesianGraphicalModel<F extends OLD_IFactor>{
    Set<F> getParents(F factor);
    int calcMaxDistanceFromLeaf(F factor);

    /**
     * @return a list of factors due to the order by which they should preferably be eliminated
     */
    @Deprecated
    List<F> getSortedFactors();

    /**
     * @return a shallow clone
     */
//    BayesianGraphicalModel clone();

    F getAssociatedFactor(Variable variable);

    List<F> getAssociatedFactors(List<Variable> vars);

}
