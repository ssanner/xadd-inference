package camdp;

import graph.Graph;

import java.util.*;

import util.IntTriple;
import xadd.XADD;
import xadd.XADD.*;

public class ComputeQFunction {

	public XADD _context = null;
	public CAMDP _camdp  = null;

	public ComputeQFunction(XADD context ,CAMDP camdp)
	{
		_context  = context;
		_camdp = camdp;
	}
	
	private IntTriple _contRegrKey = new IntTriple(-1,-1,-1);
	
	/**
	 * Regress a DD through an action
	 **/
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
			String var_to_elim = (String)o;
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

    	// Ensure Q-function is properly constrained and minimal (e.g., subject to constraints)
		if (_camdp._alConstraints.size() > 0) {
			System.err.println("WARNING: constraint application currently not verified");
			for (Integer constraint : _camdp._alConstraints)
				q = _context.apply(q, constraint, XADD.PROD); // TODO: Examine application of constraints
			q = _context.reduceLP(q);
		}
		
		// Optional Display
		_camdp._logStream.println("- Q^" + _camdp._nCurIter + "(" + a._sName + ", " + a._actionVars + " )\n" + _context.getString(q));
		if (CAMDP.DISPLAY_PREMAX_Q)
			_camdp.doDisplay(q, "Q-" + a._sName + "-" + a._actionVars + "^" + _camdp._nCurIter + "-" + Math.round(100*_camdp.APPROX_ERROR));
		
		// Continuous action parameter maximization
		if (a._actionVars.size() == 0) {
			// No action params to maximize over
			_camdp._logStream.println("- Q^" + _camdp._nCurIter + "(" + a._sName + " ):\n" + " No action parameters to max over, skipping.");
		} else {
			// Max out each action param in turn
			for (int i=0; i < a._actionVars.size(); i++) {
				String avar = a._actionVars.get(i);
				double lb   = a._hmAVar2LB.get(avar);
				double ub   = a._hmAVar2UB.get(avar);
	
				_camdp._logStream.println("- Maxing out var '" + avar + "': [" + lb + "," + ub + "]");
				q = maxOutVar(q, avar, lb, ub);
				_camdp._logStream.println("-->: " + _context.getString(q));
				
				// Can be computational expensive (max-out) so flush caches if needed
				_camdp.flushCaches(Arrays.asList(q) /* additional node to save */);
			}
			_camdp._logStream.println("- Q^" + _camdp._nCurIter + "(" + a._sName + " )\n" + _context.getString(q));
		}

		return q;
	}
	
	public int regressCVars(int q, CAction a, String var) {
		
		// Get cpf for continuous var'
		int var_id = _context.getVarIndex( _context.new BoolDec(var), false);
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
		int var_id = _context.getVarIndex( _context.new BoolDec(var), false);
		Integer dd_cpf = a._hmVar2DD.get(var);
		
		_camdp._logStream.println("- Summing out: " + var + "/" + var_id /*+ " in\n" + _context.getString(dd_cpf)*/);
		q = _context.apply(q, dd_cpf, XADD.PROD);
		
		// Following is a safer way to marginalize (instead of using opOut
		// based on apply) in the event that two branches of a boolean variable 
		// had equal probability and were collapsed.
		int restrict_high = _context.opOut(q, var_id, XADD.RESTRICT_HIGH);
		int restrict_low  = _context.opOut(q, var_id, XADD.RESTRICT_LOW);
		q = _context.apply(restrict_high, restrict_low, XADD.SUM);

		_camdp._logStream.println("-->: " + _context.getString(q));

		return q;
	}

	// Works backward from this root factor 
	public Graph buildDBNDependencyDAG(CAction a, HashSet<String> vars) {
		Graph g = new Graph(true, false, true, false);
		HashSet<String> already_seen = new HashSet<String>();
		
		// We don't want to generate parents for the following "base" variables
		already_seen.addAll( _camdp._hsContSVars );
		already_seen.addAll( _camdp._hsBoolSVars );
		already_seen.addAll( _camdp._hsContAVars ); 
		
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
		XADDLeafMax max = _context.new XADDLeafMax(var, lb, ub, _camdp._logStream);
		ixadd  = _context.reduceProcessXADDLeaf(ixadd, max, false);
		return max._runningMax;
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
		try { System.in.read(); } catch (Exception e) {}
		System.exit(1);
	}
}
