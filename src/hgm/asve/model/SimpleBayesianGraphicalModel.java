package hgm.asve.model;

import hgm.Variable;
import hgm.asve.FactorParentsTuple;
import hgm.asve.factor.OLD_IFactor;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 19/09/13
 * Time: 2:38 PM
 */
@Deprecated
public class SimpleBayesianGraphicalModel<F extends OLD_IFactor> implements BayesianGraphicalModel<F> {
    private Map<F, Set<F>> _childParentsMap;
    private List<F> _allFactorsSorted;

    protected SimpleBayesianGraphicalModel(Map<F, Set<F>> childParentsMap,
                                           List<F> allFactors) {
        _childParentsMap = childParentsMap;
        _allFactorsSorted = allFactors;
    }

    public SimpleBayesianGraphicalModel(FactorParentsTuple<F>... informationTuples) {
        _childParentsMap = new HashMap<F, Set<F>>();
        _allFactorsSorted = new ArrayList<F>();

        /*auxiliary data structure to capture redundancies*/
        Set<F> factorSet = new HashSet<F>();

        for (FactorParentsTuple<F> tuple : informationTuples) {
            if (tuple == null) throw new RuntimeException("NULL tuple");

            F child = tuple.getFactor();

            Set<F> parents = _childParentsMap.get(child);
            if (parents == null) {
                parents = new HashSet<F>();
                _childParentsMap.put(child, parents);

            }

            parents.addAll(Arrays.asList(tuple.getParents()));

            //all variables (without duplication):
            factorSet.addAll(Arrays.asList(tuple.getParents()));
            factorSet.add(tuple.getFactor());
        }

        //sort variables:
        _allFactorsSorted = new ArrayList<F>(factorSet);
        sortVariables(_allFactorsSorted);

    }

    private void sortVariables(List<F> factors) {
        Collections.sort(factors, new Comparator<F>() {
            @Override
            public int compare(F f1, F f2) {
                int d1 = calcMaxDistanceFromLeaf(f1);
                int d2 = calcMaxDistanceFromLeaf(f2);

                if (d1 < d2) return -1;
                if (d1 > d2) return 1;

                if (f1.getAssociatedVar() == null || f2.getAssociatedVar() == null) {
                    if (f1.hashCode() < f2.hashCode()) return -1;
                    if (f1.hashCode() > f2.hashCode()) return 1;
                    return 0;
                }

                return f1.getAssociatedVar().getName().compareTo(f2.getAssociatedVar().getName());
            }
        });
    }

    @Override
    public Set<F> getParents(F factor) {
        return _childParentsMap.get(factor);
    }

    @Override
    public int calcMaxDistanceFromLeaf(F factor) {
        Set<F> parents = _childParentsMap.get(factor);
        if (parents == null) throw new RuntimeException(factor + "not in list");
        if (parents.isEmpty()) return 0;

        int maxParentDist = 0;
        for (F parent : parents) {
            int parentDist = calcMaxDistanceFromLeaf(parent);
            if (parentDist > maxParentDist) {
                maxParentDist = parentDist;
            }
        }

        return maxParentDist + 1;
    }

    @Override
    public List<F> getSortedFactors() {
        return _allFactorsSorted;
    }

    @Override
    public F getAssociatedFactor(Variable variable) {
        for (F f : _allFactorsSorted) {
            if (f.getAssociatedVar() == variable) return f;
        }
        return null; //not found
    }

    @Override
    public List<F> getAssociatedFactors(List<Variable> vars) {
        List<F> factors = new ArrayList<F>(vars.size());
        for (int i = 0; i < vars.size(); i++) {
            factors.add(getAssociatedFactor(vars.get(i)));
        }
        return factors;
    }

    /*@Override
    public BayesianGraphicalModel<F> clone() {
        //only shallowly...
        return new SimpleBayesianGraphicalModel<F>(
                new HashMap<F, Set<F>>(_childParentsMap),
                new ArrayList<F>(_allFactorsSorted)
        );
    }*/
}
