package camdp;

import graph.Graph;

import java.util.*;

import util.IntTriple;
import xadd.ExprLib;
import xadd.XADD;
import xadd.ExprLib.ArithExpr;
import xadd.ExprLib.DoubleExpr;
import xadd.XADD.*;

public class ComputeQFunction {

    public XADD _context = null;
    public CAMDP _camdp = null;

    public ComputeQFunction(XADD context, CAMDP camdp) {
        _context = context;
        _camdp = camdp;
    }

    private IntTriple _contRegrKey = new IntTriple(-1, -1, -1);

    /**
     * Regress a DD through an action
     */
    public int regress(int vfun, CAction a) {

        _camdp._logStream.println("\n>>> REGRESSING '" + a._sName + "'\n");

        // Prime the value function
        int q = _context.substitute(vfun, _camdp._hmPrimeSubs);
        _camdp._logStream.println("- Primed value function:\n" + _context.getString(q));

        // Discount
        q = _context.scalarOp(q, _camdp._bdDiscount.doubleValue(), XADD.PROD);

        // Add reward *if* it contains primed vars that need to be regressed
        HashSet<String> i_and_ns_vars_in_reward = filterIandNSVars(_context.collectVars(a._reward), true, true);
        if (!i_and_ns_vars_in_reward.isEmpty()) {
            q = _context.apply(a._reward, q, XADD.SUM); // Add reward to already discounted primed value function
            _camdp._logStream.println("- Added in reward pre-marginalization with interm/next state vars: " + i_and_ns_vars_in_reward);
        }

        // Derive a variable elimination order for the DBN w.r.t. the reward that puts children before parents
        HashSet<String> vars_to_regress = filterIandNSVars(_context.collectVars(q), true, true);
        Graph g = buildDBNDependencyDAG(a, vars_to_regress);
        if (g.hasCycle())
            displayCyclesAndExit(g, a);

        // Get a valid elimination order (does not minimize tree width, could be optimized)
        List var_order = g.topologicalSort(true);
        _camdp._logStream.println("- Elimination order for regression: " + var_order);

        // Regress each variable in the topological order
        for (Object o : var_order) {
            String var_to_elim = (String) o;
            if (_camdp._hsBoolIVars.contains(var_to_elim) || _camdp._hsBoolNSVars.contains(var_to_elim)) {
                q = regressBVars(q, a, var_to_elim);
            } else if (_camdp._hsContIVars.contains(var_to_elim) || _camdp._hsContNSVars.contains(var_to_elim)) {
                q = regressCVars(q, a, var_to_elim);
            } else {
                // The topological sort will also add in next state and action variables since they were parents in the network
                _camdp._logStream.println("- Ignoring current state or action variable " + var_to_elim + " during elimination");
            }
        }

        // TODO: Policy maintenance: currently unfinished in this version
        // - if no action variables, can just annotate each Q-function with action
        // - if action variables then need to maintain action name along with
        //   the substitutions made at the leaves (which can occur recursively for
        //   multivariable problems)
        // if (_camdp.MAINTAIN_POLICY) {
        //      ...
        // }

        // NOTE: if reward was not added in prior to regression, it must be
        // added in now...
        if (i_and_ns_vars_in_reward.isEmpty()) {
            q = _context.apply(a._reward, q, XADD.SUM);
            _camdp._logStream.println("- Added in reward post-marginalization with no interm/next state vars.");
        }

        // Optional Display
        _camdp._logStream.println("- Q^" + _camdp._nCurIter + "(" + a._sName + ", " + a._actionParams + " )\n" + _context.getString(q));
        if (CAMDP.DISPLAY_PREMAX_Q) {
            _camdp.displayGraph(q, "Q-" + a._sName + "-" + a._actionParams + "^" + _camdp._nCurIter + _camdp.makeApproxLabel() );
        }
        // Noise handling
        if (a._noiseVars.size() == 0) {
            // No action params to maximize over
            _camdp._logStream.println("- Q^" + _camdp._nCurIter + "(" + a._sName + " ):\n" + " No noise parameters to max over, skipping this step.");
        } else {
            // Max in noise constraints and min out each noise parameter in turn
            // NOTE: we can do this because noise parameters can only reference state variables
            //       (not currently allowing them to condition on intermediate or other noise vars)
            //       hence legal values of noise var determined solely by the factor for that var
            HashSet<String> q_vars = _context.collectVars(q);
            for (String nvar : a._noiseVars) {

                if (!q_vars.contains(nvar)) {
                    _camdp._logStream.println("- Skipping noise var '" + nvar + "', which does not occur in q: " + _context.collectVars(q));
                    continue;
                }

                _camdp._logStream.println("- Minimizing over noise param '" + nvar + "'");
                int noise_factor = a._hmNoise2DD.get(nvar);
                q = _context.apply(noise_factor, q, XADD.MAX); // Max in the noise so illegal states get replace by +inf, otherwise q replaces -inf
                q = minOutVar(q, nvar, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
                _camdp._logStream.println("-->: " + _context.getString(q));

                // Can be computational expensive (max-out) so flush caches if needed
                _camdp.flushCaches(Arrays.asList(q) /* additional node to save */);
            }
            _camdp._logStream.println("- Done noise parameter minimization");
            _camdp._logStream.println("- Q^" + _camdp._nCurIter + "(" + a._sName + " )" + _context.collectVars(q) + "\n" + _context.getString(q));
        }

        // Continuous action parameter maximization
        if (a._actionParams.size() == 0) {
            // No action params to maximize over
            _camdp._logStream.println("- Q^" + _camdp._nCurIter + "(" + a._sName + " ):\n" + " No action parameters to max over, skipping this step.");
        } else {

            // Max out each action param in turn
            HashSet<String> q_vars = _context.collectVars(q);
            for (int i = 0; i < a._actionParams.size(); i++) {
                String avar = a._actionParams.get(i);
                double lb = a._hmAVar2LB.get(avar);
                double ub = a._hmAVar2UB.get(avar);

                if (!q_vars.contains(avar)) {
                    _camdp._logStream.println("- Skipping var '" + avar + "': [" + lb + "," + ub + "], which does not occur in q: " + _context.collectVars(q));
                    continue;
                }
                //discretizing for continuous domains ( C is the step size)
                if (_camdp.DISCRETIZE_PROBLEM) {
                    _camdp._logStream.println("- DISCRETIZING '" + avar + " into " + _camdp.DISCRETE_NUMBER + " discrete actions");
                    int actionTree = q;
                    Integer maximizedTree = null;
                    double range = _camdp.GLOBAL_LB;
                    double stepsize = (_camdp.GLOBAL_UB - _camdp.GLOBAL_LB) / (_camdp.DISCRETE_NUMBER - 1);
                    int var_id = _context._cvar2ID.get(avar);
                    int actionReplace = -1;
                    //int interval = (ub - lb) / 10;
                    while (range <= _camdp.GLOBAL_UB) {
                        ArithExpr range_a = new DoubleExpr(range);
                        Integer actionValue = _context.getTermNode(range_a);
                        // Check cache
                        _contRegrKey.set(var_id, actionValue, actionTree);
                        Integer result = null;
                        if ((result = _camdp._hmContRegrCache.get(_contRegrKey)) != null)
                            actionReplace = result;
                        else {
                            // Perform regression via delta function substitution
                            actionReplace = _context.reduceProcessXADDLeaf(actionValue,
                                    _context.new DeltaFunctionSubstitution(avar, actionTree), true);

                            // Cache result
                            _camdp._hmContRegrCache.put(new IntTriple(_contRegrKey), actionReplace);
                        }

//                        System.out.println("Warning using Continuous Action discretezation;");
                        maximizedTree = (maximizedTree == null) ? actionReplace :
                                _context.apply(maximizedTree, actionReplace, XADD.MAX);
                        maximizedTree = _context.reduceRound(maximizedTree); // Round!
                        maximizedTree = _context.reduceLP(maximizedTree); // Rely on flag XADD.CHECK_REDUNDANCY

                        range = range + stepsize;
                        //_camdp.flushCaches(Arrays.asList(maximizedTree,actionReplace) /* additional node to save */);

                    }
                    q = maximizedTree;
                } else //not discretizing! continuous version
                {
                    _camdp._logStream.println("- Maxing out action param '" + avar + "': [" + lb + "," + ub + "]");
                    q = maxOutVar(q, avar, lb, ub);
                    _camdp._logStream.println("-->: " + _context.getString(q));
                    // Can be computational expensive (max-out) so flush caches if needed
                    _camdp.flushCaches(Arrays.asList(q) /* additional node to save */);
                }

                if (CAMDP.DISPLAY_PREMAX_Q) {
                    _camdp.displayGraph(q, "Q-" + a._sName + "-" + a._actionParams + "-End^" + _camdp._nCurIter + _camdp.makeApproxLabel());
                }
            }

            _camdp._logStream.println("- Done action parameter maximization");
            _camdp._logStream.println("- Q^" + _camdp._nCurIter + "(" + a._sName + " )\n" + _context.getString(q));
        }

        // Constraints not currently allowed, should be applied to the reward as -Infinity
        //if (_camdp._alConstraints.size() > 0) {
        //	System.err.println("WARNING: constraint application currently not verified");
        //	System.exit(1);
        //	for (Integer constraint : _camdp._alConstraints)
        //		q = _context.apply(q, constraint, XADD.PROD);
        //	q = _context.reduceLP(q);
        //}

        //Returning non-Standard DD (to be standarized in CAMDP)
        return q;
    }

    public int regressCVars(int q, CAction a, String var) {

        // Get cpf for continuous var'
        int var_id = _context._cvar2ID.get(var);
        Integer dd_conditional_sub = a._hmVar2DD.get(var);

        _camdp._logStream.println("- Integrating out: " + var + "/" + var_id /* + " in\n" + _context.getString(dd_conditional_sub)*/);

        // Check cache
        _contRegrKey.set(var_id, dd_conditional_sub, q);
        Integer result = null;
        if ((result = _camdp._hmContRegrCache.get(_contRegrKey)) != null)
            return result;

        // Perform regression via delta function substitution
        q = _context.reduceProcessXADDLeaf(dd_conditional_sub,
                _context.new DeltaFunctionSubstitution(var, q), true);

        // Cache result
        _camdp._logStream.println("-->: " + _context.getString(q));
        _camdp._hmContRegrCache.put(new IntTriple(_contRegrKey), q);

        return q;
    }

    public int regressBVars(int q, CAction a, String var) {

        // Get cpf for boolean var'
        int var_id = _context.getVarIndex(_context.new BoolDec(var), false);
        Integer dd_cpf = a._hmVar2DD.get(var);

        _camdp._logStream.println("- Summing out: " + var + "/" + var_id /*+ " in\n" + _context.getString(dd_cpf)*/);
        q = _context.apply(q, dd_cpf, XADD.PROD);

        // Following is a safer way to marginalize (instead of using opOut
        // based on apply) in the event that two branches of a boolean variable
        // had equal probability and were collapsed.
        int restrict_high = _context.opOut(q, var_id, XADD.RESTRICT_HIGH);
        int restrict_low = _context.opOut(q, var_id, XADD.RESTRICT_LOW);
        q = _context.apply(restrict_high, restrict_low, XADD.SUM);

        _camdp._logStream.println("-->: " + _context.getString(q));

        return q;
    }

    // Works backward from this root factor
    public Graph buildDBNDependencyDAG(CAction a, HashSet<String> vars) {
        Graph g = new Graph(true, false, true, false);
        HashSet<String> already_seen = new HashSet<String>();

        // We don't want to generate parents for the following "base" variables
        already_seen.addAll(_camdp._hsContSVars);
        already_seen.addAll(_camdp._hsBoolSVars);
        already_seen.addAll(_camdp._hsContAVars);
        already_seen.addAll(_camdp._hsNoiseVars);

        for (String var : vars)
            buildDBNDependencyDAGInt(a, var, g, already_seen);

        return g;
    }

    // Consider that vars belong to a parent factor, recursively call
    // for every child factor and link child to parent
    //
    // have R(x1i,b1i,x2'), DAG has (b1i -> x1i -> R), (b1i -> R), (x2' -> R)... {x1i, b1i, x2'}
    // recursively add in parents for each of x2', xli, bli
    public void buildDBNDependencyDAGInt(CAction a, String parent_var, Graph g, HashSet<String> already_seen) {
        if (already_seen.contains(parent_var))
            return;
        already_seen.add(parent_var);
        Integer dd_cpf = a._hmVar2DD.get(parent_var);
        if (dd_cpf == null) {
            System.err.println("Could not find CPF definition for variable '" + parent_var +
                    "' while regressing action '" + a._sName + "'");
            System.exit(1);
        }
        HashSet<String> children = _context.collectVars(dd_cpf);
        for (String child_var : children) {
            // In the case of boolean variables, the dual action diagram contains the parent,
            // because this is not a substitution, it is a distribution over the parent.
            // Hence we need to explicitly prevent boolean variable self-loops -- this is not
            // an error.
            if (!child_var.equals(parent_var) || _camdp._hsContIVars.contains(parent_var) || _camdp._hsContNSVars.contains(parent_var)) {
                g.addUniLink(child_var, parent_var);
                //System.out.println("Adding link " + child_var + " --> " + parent_var);
            } else if (child_var.equals(parent_var)) {
                // SUSPICIOUS CODE :p (avoid removing variables that dont have dependencies
                g.addNode(parent_var);
            }
            buildDBNDependencyDAGInt(a, child_var, g, already_seen);
        }
    }

    public HashSet<String> filterIandNSVars(HashSet<String> vars, boolean allow_cont, boolean allow_bool) {
        HashSet<String> filter_vars = new HashSet<String>();
        for (String var : vars)
            if (allow_cont &&
                    (_camdp._hsContIVars.contains(var) ||
                            _camdp._hsContNSVars.contains(var)))
                filter_vars.add(var);
            else if (allow_bool &&
                    (_camdp._hsBoolIVars.contains(var) ||
                            _camdp._hsBoolNSVars.contains(var)))
                filter_vars.add(var);
        return filter_vars;
    }

    public int maxOutVar(int ixadd, String var, double lb, double ub) {
        XADDLeafMinOrMax max = _context.new XADDLeafMinOrMax(var, lb, ub, true /* is_max */, _camdp._logStream);
        ixadd = _context.reduceProcessXADDLeaf(ixadd, max, false);
        return max._runningResult;
    }

    public int minOutVar(int ixadd, String var, double lb, double ub) {
        XADDLeafMinOrMax min = _context.new XADDLeafMinOrMax(var, lb, ub, false /* is_max */, _camdp._logStream);
        ixadd = _context.reduceProcessXADDLeaf(ixadd, min, false);
        return min._runningResult;
    }

    private void displayCyclesAndExit(Graph g, CAction a) {
        // Error display -- find the cycles and display them
        System.err.println("ERROR: in action '" + a._sName + "' the DBN dependency graph contains one or more cycles as follows:");
        HashSet<HashSet<Object>> sccs = g.getStronglyConnectedComponents();
        for (HashSet<Object> connected_component : sccs)
            if (connected_component.size() > 1)
                System.err.println("- Cycle: " + connected_component);
        HashSet<Object> self_cycles = g.getSelfCycles();
        for (Object v : self_cycles)
            System.err.println("- Self-cycle: [" + v + "]");
        g.launchViewer("DBN Dependency Graph for '" + a._sName + "'");
        try {
            System.in.read();
        } catch (Exception e) {
        }
        System.exit(1);
    }
}
