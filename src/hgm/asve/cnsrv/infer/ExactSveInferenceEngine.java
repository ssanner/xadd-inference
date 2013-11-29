package hgm.asve.cnsrv.infer;

import graph.Graph;
import hgm.asve.cnsrv.factory.ModelBasedXaddFactorFactory;
import hgm.asve.cnsrv.gm.FBQuery;
import hgm.asve.cnsrv.factor.Factor;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 19/09/13
 * Time: 4:02 PM
 */
public class ExactSveInferenceEngine {

    private ModelBasedXaddFactorFactory _factory;
    private Records _records;

    public ExactSveInferenceEngine(ModelBasedXaddFactorFactory factory) {
        _factory = factory;
        _records = new Records("exact.SVE");
    }

    public Factor infer(boolean exclusivelyUseAncestorsOfQueryAndEvidenceFactors) {
        return infer(null, exclusivelyUseAncestorsOfQueryAndEvidenceFactors);
    }

    public Factor infer(List<String> varEliminationOrder,
                        boolean exclusivelyUseAncestorsOfQueryAndEvidenceFactors) {

//        List<Factor> specialFactors = new ArrayList<Factor>();

        FBQuery query = _factory.getQuery();
        _records.set("query.variables", query.getQueryVariables().toString());
        _records.set("query.continuous.instantiated.evidence", query.getContinuousInstantiatedEvidence().toString());
        _records.set("query.boolean.instantiated.evidence", query.getBooleanInstantiatedEvidence().toString());

        //topologically score the graph:
        List<String> nonRemovableVariables = new ArrayList<String>();

        if (exclusivelyUseAncestorsOfQueryAndEvidenceFactors) {
            nonRemovableVariables.addAll(query.getQueryVariables());
            nonRemovableVariables.addAll(query.getNonInstantiatedEvidenceVariables());
            nonRemovableVariables.addAll(query.fetchInstantiatedEvidenceVariables()); //although instantiated variables are omitted, there corresponding factor is not.
        } else {
            //All variables of the of the graphical model should non-removable:
            nonRemovableVariables.addAll(_factory.getAllVariables());
        }
        _records.set("non.removable.variables", nonRemovableVariables.toString());

        Set<String> varSet = getSelfAndAncestorVariables(nonRemovableVariables);
        _records.set("all.variables.taken.into.account", varSet.toString());

        List<Factor> factors = _factory.getAssociatedInstantiatedFactors(varSet);

        List<String> sortedVariables;
        if (varEliminationOrder == null) {
            sortedVariables = chooseBestVariableOrdering(varSet);
        } else {
            sortedVariables = varEliminationOrder;
            if (!sortedVariables.containsAll(varSet))
                throw new RuntimeException(sortedVariables + " does not contain all elements of " + varSet);
        }
        System.out.println("sortedVariables (var elimination order) = " + sortedVariables);

        //****

        // Decide which variables to eliminate
        ArrayList<String> vars_to_eliminate = new ArrayList<String>();
        for (String var : sortedVariables)
            if (!query.getQueryVariables().contains(var) && !query.fetchInstantiatedEvidenceVariables().contains(var)) {     //todo what about uninstantiated evidence?
                vars_to_eliminate.add(var);
            }

        // Do variable elimination
        ArrayList<Factor> factors_with_var = new ArrayList<Factor>();
        ArrayList<Factor> factors_without_var = new ArrayList<Factor>();
        for (String var : vars_to_eliminate) {
            System.out.println("Eliminating: " + var);
            _records.desiredVariableEliminationOrder.add(var);
            _records.variablesActuallyMarginalized.add(var);

            // Split factors into sets that contain and do not contain the variable
            splitFactors(var, factors, factors_with_var, factors_without_var);

            // Multiply factors that contain variable and marginalize out variable,
            // adding this new factor and all without the variable to the factors list
            Factor xadd_with_var = _factory.multiply(factors_with_var);
//            System.out.println("Marginalizing out: " + _factory.getContext().getString(xadd_with_var._xadd));
            _records.recordFactor(xadd_with_var);

            Factor xadd_marginal = _factory.marginalize(xadd_with_var, var);

            factors = new ArrayList<Factor>(factors_without_var);
            factors.add(xadd_marginal);
            _records.recordFactor(xadd_marginal);

            _factory.flushFactorsExcept(factors); //note that factors associated with variables will not be eliminated anyway...
        }

        // Done variable elimination, have a set of factors just over query vars,
        // need to compute normalizer
        Factor result = _factory.multiply(factors);
        _records.recordFactor(result);
        Factor norm_result = _factory.normalize(result);
        _records.set("remaining.variables", norm_result.getScopeVars().toString());

        //test reduction:
//        int reducedId = _factory.context.reduceLP(norm_result._xadd, true);
//        norm_result = new Factor(reducedId, _factory.context, "reduced(" + norm_result.getHelpingText() + ")");

        _records.recordFinalResult(norm_result);

        _factory.makePermanent(Arrays.asList(norm_result));
        _factory.flushFactorsExcept(Collections.EMPTY_LIST);
        return norm_result;
    }

    private void splitFactors(String split_var, List<Factor> factor_source, ArrayList<Factor> factors_with_var,
                              ArrayList<Factor> factors_without_var) {

        factors_with_var.clear();
        factors_without_var.clear();
        for (Factor f : factor_source)
            if (f.getScopeVars().contains(split_var))
                factors_with_var.add(f);
            else
                factors_without_var.add(f);
    }

    private List<String> chooseBestVariableOrdering(Iterable<String> variables) {
        // Find best variable order -- build map from each variable to CPTs
        Graph g = new Graph(/* directed */true, false, true, false);
        g.setBottomToTop(false);
        g.setMultiEdges(false);
        for (String v : variables) {
            Factor f = _factory.getAssociatedInstantiatedFactor(v);
            g.addAllUniLinks(f.getScopeVars(), f.getScopeVars());
        }
        // g.launchViewer();

        // Use a topological sort to find a good variable ordering
        List<String> sortedVariables = g.computeBestOrder();
        return sortedVariables;
//        return _factory.getAssociatedInstantiatedFactors(sortedVariables);
    }


    private Set<String> getSelfAndAncestorVariables(Iterable<String> variables) {
        Set<String> result = new HashSet<String>();
        for (String v : variables) {
            recursivelyPopulateAncestorSet(v, result);
        }
        return result;
    }

    private void recursivelyPopulateAncestorSet(String var, Set<String> ancestorSet) {
        Set<String> parents = _factory.getParents(var);
        if (parents == null) {
            System.err.println("unknown variable " + var + "!!!");
            return;
        }

        for (String parent : parents) {
            recursivelyPopulateAncestorSet(parent, ancestorSet);
        }
        ancestorSet.add(var);
    }

    public Records getRecords() {
        return _records;
    }
}
