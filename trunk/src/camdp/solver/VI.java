package camdp.solver;

import graph.Graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import util.IntTriple;
import xadd.ExprLib.ArithExpr;
import xadd.ExprLib.DoubleExpr;
import xadd.ExprLib.OperExpr;
import xadd.XADD;
import xadd.XADD.XADDLeafMinOrMax;
import camdp.CAMDP;
import camdp.CAction;

public class VI extends CAMDPsolver {

	public Integer _maxDD;
	public Integer _prevDD;
	public Integer _nCurIter;   // Current iteration for Value Iteration	
	
	/* Static variables */
	public static long _lTime; // For timing purposes
	public static Runtime RUNTIME = Runtime.getRuntime();
	
	/////////////////////////
	/* Solution Parameters */
	/////////////////////////
	
	public final int MAXIMUM_ITER = 10000;
	public final boolean MAINTAIN_POLICY = false;
	public final boolean DISCRETIZE_PROBLEM = false;
	
	/* Optimal solution maintenance */
	public ArrayList<Integer> optimalDD = new ArrayList<Integer>();
	public double []optimalMaxValues = new double[MAXIMUM_ITER];
	
	/* Discretization Parameters */
	public int DISCRETE_NUMBER = 11;
	public int GLOBAL_LB=-9;
	public int GLOBAL_UB=9;
	
	
	//////////////////Methods /////////////////////////////////
	
	public VI(CAMDP camdp){
		_mdp = camdp;
		_context = camdp._context;
		_valueDD = _context.NEG_INF;
		_logStream = camdp._logStream;
		_solveMethod = "VI";
		makeResultStream();
	}
	
	
	/////// Time Management utilities ////////////////////// 

	// Reset elapsed time
	public static void ResetTimer() {
		_lTime = System.currentTimeMillis(); 
	}

	// Get the elapsed time since resetting the timer
	public static long GetElapsedTime() {
		return System.currentTimeMillis() - _lTime;
	}

	//////////Memory Management Utilities/////////////////
	
	public static String MemDisplay() {
		long total = RUNTIME.totalMemory();
		long free  = RUNTIME.freeMemory();
		return total - free + ":" + total;
	}

	public static int usedMem() {
		long total = RUNTIME.totalMemory();
		long free  = RUNTIME.freeMemory();
		return (int) ((total - free)/1000000);
	}

	public void flushCaches() {
		flushCaches(new ArrayList<Integer>());
	}
	public void flushCaches(boolean forceFlush) {
		flushCaches(new ArrayList<Integer>(), forceFlush);
	}
    public void flushCaches(List<Integer> special_nodes) {
		flushCaches(special_nodes,false);
	}
    public void flushCaches(List<Integer> special_nodes, boolean forceFlush) {
		
		if (((double)RUNTIME.freeMemory() / 
				(double)RUNTIME.totalMemory()) > FLUSH_PERCENT_MINIMUM && !forceFlush) {
			//System.out.println("No need to flush caches.");
			return; // Still enough free mem to exceed minimum requirements
		}
		
		// Commence cache flushing
		_logStream.println("Before flush: " + _context._hmInt2Node.size() + " XADD nodes in use, " + "freeMemory: " + 
				_df.format(RUNTIME.freeMemory()/10e6d) + " MB = " + 
				_df.format(100d*RUNTIME.freeMemory()/(double)RUNTIME.totalMemory()) + "% available memory");

		// TODO: Maybe keeping these is worthwhile?
		_mdp._hmContRegrCache.clear();
		
		_context.clearSpecialNodes();
		for (Integer node : special_nodes)
			_context.addSpecialNode(node);

		
		for (CAction a : _mdp._hmName2Action.values()) {
			_context.addSpecialNode(a._reward);
			for (Integer xadd : a._hmVar2DD.values())
				_context.addSpecialNode(xadd);
			for (Integer xadd : a._hmNoise2DD.values())
				_context.addSpecialNode(xadd);
		}
		if (_valueDD!=null && !forceFlush){
			_context.addSpecialNode(_valueDD); 
		}
		if ( optimalDD !=null) //keep even at forceFlush, because we want to measure the error
			_context._hsSpecialNodes.addAll(optimalDD);
		_context.flushCaches();

		_logStream.println("After flush: " + _context._hmInt2Node.size() + " XADD nodes in use, " + "freeMemory: " + 
				_df.format(RUNTIME.freeMemory()/10e6d) + " MB = " + 
				_df.format(100d*RUNTIME.freeMemory()/(double)RUNTIME.totalMemory()) + "% available memory");
	}
    
	////////Main Solver Class ///////////////
	public int solve(int max_iter){
		
		_prevDD = _maxDD = null;
		//Iteration counter
		_nCurIter = 0;
		if (max_iter < 0)
			max_iter = _mdp._nMaxIter;
		
		// Value iteration statistics
		int totalTime=0;
		long[] time = new long[max_iter + 1];
		int[] num_nodes = new int[max_iter + 1];
		int[] num_leaves = new int[max_iter + 1];
		int[] num_branches = new int[max_iter + 1];

		//Initialize value function to zero
		_valueDD = _context.ZERO;

		// Perform value iteration for specified number of iterations, or until convergence detected
		while (_nCurIter < max_iter) 
		{
			++_nCurIter;
			ResetTimer();
			_logStream.println(ASCII_BAR + "\nITERATION #" + _nCurIter + ", " + 
			MemDisplay() + " bytes, " + GetElapsedTime() + " ms\n" + ASCII_BAR);
			_logStream.flush();

			// Prime diagram
			_prevDD = _valueDD;

			// Iterate over each action
			_maxDD = null;
			for (Map.Entry<String,CAction> me : _mdp._hmName2Action.entrySet()) {

			// Regress the current value function through each action (finite number of continuous actions)
			int regr = regress(_valueDD, me.getValue());
			regr  = _context.reduceRound(regr); // Round!
			if (DISPLAY_POSTMAX_Q)
				_mdp.doDisplay(regr, makeXADDLabel("Q-"+me.getKey(), _nCurIter, APPROX_ERROR));

			// Maintain running max over different actions
			_maxDD = (_maxDD == null) ? regr : _context.apply(_maxDD, regr, XADD.MAX);
			_maxDD = _context.reduceRound(_maxDD); // Round!
			_maxDD = _context.reduceLP(_maxDD); // Rely on flag XADD.CHECK_REDUNDANCY

			//Optional post-max approximation 
			// Could be used safely if overall error is being monitored 
			if (APPROX_ALWAYS)
				_maxDD = _context.linPruneRel(_maxDD, APPROX_ERROR);

			// Error checking and logging
			int canon_max_dd = _context.makeCanonical(_maxDD);
			if (_maxDD != canon_max_dd) {
				System.err.println("CAMDP VI ERROR: encountered non-canonical node that should have been canonical... could be rounding, continuing.");
				_context.exportXADDToFile(_maxDD, "ERRORdiagram1OriginalXADD.xadd");
				_context.exportXADDToFile(canon_max_dd, "ERRORdiagram2makeCanonical.xadd");
				_context.getGraph(_maxDD).launchViewer("ERROR diagram 1: original maxDD");
				_context.getGraph(canon_max_dd).launchViewer("ERROR diagram 2: makeCanonical(maxDD)");
				_maxDD = _context.makeCanonical(_maxDD);

				//ExitOnError("CAMDP VI ERROR: encountered non-canonical node that should have been canonical:\n" + 
				//			_context.getString(_maxDD) + "\nvs.\n" + _context.getString(_maxDD));
			}
			if(DISPLAY_MAX)
				_mdp.doDisplay(_maxDD, makeXADDLabel("QMax", _nCurIter, APPROX_ERROR));
			_logStream.println("Running max in iter " + _nCurIter + ":" + _context.getString(_maxDD));

			flushCaches();
			}


			// This error shouldn't happen anymore, new reduceLPv2 version is "bottom up", so no repeated reducing.
			//if (_maxDD != _context.reduceLP(_maxDD))
			//	ExitOnError("CAMDP VI ERROR: encountered non-reduced value function");

			_valueDD = _context.reduceLP(_maxDD);
			if (APPROX_PRUNING) {
				long appTime = GetElapsedTime();
				_valueDD = _context.linPruneRel(_valueDD, APPROX_ERROR);
				long endTime = GetElapsedTime() - appTime;
				System.out.println("Approx Finish"+ _nCurIter+ " Iter took: "+appTime+ " pruning: "+endTime);
				//displayGraph(_valueDD, "valPruned-" + _nCurIter+" e"+APPROX_ERROR);
			}

			System.out.println("Iter:" + _nCurIter+" Complete");
			_logStream.println("Iter complete:" + _nCurIter + _context.getString(_valueDD));
			_mdp.doDisplay(_valueDD,makeXADDLabel("V",_nCurIter, APPROX_ERROR));

			//////////////////////////////////////////////////////////////////////////
			// Value iteration statistics
			time[_nCurIter] = GetElapsedTime();
			totalTime += time[_nCurIter];
			num_nodes[_nCurIter] = _context.getNodeCount(_valueDD);
			num_leaves[_nCurIter] = _context.getLeafCount(_valueDD);
			num_branches[_nCurIter] = _context.getBranchCount(_valueDD);

			double maxVal = 0d;
			double maxRelErr = 0d;
			if (_mdp.LINEAR_PROBLEM) {
				maxVal = _context.linMaxVal(_valueDD);
				optimalMaxValues[_nCurIter-1] = maxVal;
				if (COMPARE_OPTIMAL){
					if(APPROX_ERROR == 0d){ //Exact solution
						if (optimalDD.size() != _nCurIter-1)
							System.err.println("Incorrect optimalDD:"+optimalDD+" "+_nCurIter);
						optimalDD.add(_valueDD);
					}
					if (optimalDD.size() > _nCurIter-1){
						maxRelErr = (_context.linMaxDiff(optimalDD.get(_nCurIter-1), _valueDD))/optimalMaxValues[_nCurIter-1];
					}
					else maxRelErr = -1;
				}
			}
			_logStream.println("Value function size @ end of iteration " + _nCurIter + 
					": " + num_nodes[_nCurIter] + " nodes = " + 
					num_branches[_nCurIter] + " cases" + " in " + time[_nCurIter] + " ms");

			//APPROX_TEST LOG, outputs: iter, #node, #branches, #UsedMem(MB), IterTime, TotTime, MaxVal, RelErr

			if (_mdp.LINEAR_PROBLEM && APPROX_PRUNING) {
				_resultStream.format("%d %d %d %d %d %d %d %f %f\n", _nCurIter, num_nodes[_nCurIter], 
						num_leaves[_nCurIter], num_branches[_nCurIter], usedMem(), 
						time[_nCurIter], totalTime,
						_context.linMaxVal(_valueDD), maxRelErr );
			}
//////////////////////////////////////////////////////////////////////
			if (_prevDD.equals(_valueDD) ) {
				System.out.println("CAMDP: Converged to solution early,  at iteration "+_nCurIter);
				int it = _nCurIter;
				while (++it < max_iter){
					optimalMaxValues[it] = optimalMaxValues[_nCurIter];
					optimalDD.add(_valueDD);
					_resultStream.format("%d %d %d %d %d %d %d %f %f\n", it, num_nodes[_nCurIter], num_leaves[_nCurIter], 
							num_branches[_nCurIter], usedMem(),
							time[_nCurIter],totalTime,
							_context.linMaxVal(_valueDD), maxRelErr );
				}
				break;
			}

		}

		flushCaches();	

		//////////////////////////////////////////////////////////////////////////
		// Performance Logging
		_logStream.println("\nValue iteration complete!");
		_logStream.println(max_iter + " iterations took " + GetElapsedTime() + " ms");
		_logStream.println("Canonical / non-canonical: " + OperExpr.ALREADY_CANONICAL + " / " + OperExpr.NON_CANONICAL);
		
		_logStream.println("\nIteration Results summary");
		for (int i = 1; i <= max_iter; i++) {
		String branch_count = num_branches[i] >= 0 
		? "" + num_branches[i] : " > " + XADD.MAX_BRANCH_COUNT; 
		_logStream.println("Iter " + i + ": nodes = " + num_nodes[i] + "\tbranches = " + branch_count + "\ttime = " + time[i] + " ms");
		}
//////////////////////////////////////////////////////////////////////////

		return _nCurIter;
	}

	private IntTriple _contRegrKey = new IntTriple(-1,-1,-1);
	
	/**
	 * Regress a DD through an action
	 **/
	public int regress(int vfun, CAction a) {

		_logStream.println("\n>>> REGRESSING '" + a._sName + "'\n");
		
		// Prime the value function 
		int q = _context.substitute(vfun, _mdp._hmPrimeSubs); 
		_logStream.println("- Primed value function:\n" + _context.getString(q));
		
		// Discount
		q = _context.scalarOp(q, _mdp._bdDiscount.doubleValue(), XADD.PROD);
		
		// Add reward *if* it contains primed vars that need to be regressed
		HashSet<String> i_and_ns_vars_in_reward = filterIandNSVars(_context.collectVars(a._reward), true, true);
		if (!i_and_ns_vars_in_reward.isEmpty()) {
			q = _context.apply(a._reward, q, XADD.SUM); // Add reward to already discounted primed value function
			_logStream.println("- Added in reward pre-marginalization with interm/next state vars: " + i_and_ns_vars_in_reward);
		}
			
		// Derive a variable elimination order for the DBN w.r.t. the reward that puts children before parents
		HashSet<String> vars_to_regress = filterIandNSVars(_context.collectVars(q), true, true);
		Graph g = buildDBNDependencyDAG(a, vars_to_regress);
		if (g.hasCycle()) 
			displayCyclesAndExit(g, a);
		
		// Get a valid elimination order (does not minimize tree width, could be optimized)
		List var_order = g.topologicalSort(true);
		_logStream.println("- Elimination order for regression: " + var_order);
		
		// Regress each variable in the topological order
		for (Object o : var_order) {
			String var_to_elim = (String)o;
			if (_mdp._hsBoolIVars.contains(var_to_elim) || _mdp._hsBoolNSVars.contains(var_to_elim)) {
				q = regressBVars(q, a, var_to_elim);
			} else if (_mdp._hsContIVars.contains(var_to_elim) || _mdp._hsContNSVars.contains(var_to_elim)) {
				q = regressCVars(q, a, var_to_elim);
			} else {
				// The topological sort will also add in next state and action variables since they were parents in the network
				_logStream.println("- Ignoring current state or action variable " + var_to_elim + " during elimination");
			}
		}
		
		// TODO: Policy maintenance: currently unfinished in this version
		// - if no action variables, can just annotate each Q-function with action
		// - if action variables then need to maintain action name along with
		//   the substitutions made at the leaves (which can occur recursively for
		//   multivariable problems)
		// if (_mdp.MAINTAIN_POLICY) { 
		//      ... 
		// }
			
		// NOTE: if reward was not added in prior to regression, it must be 
		// added in now...
		if (i_and_ns_vars_in_reward.isEmpty()) {
			q = _context.apply(a._reward, q, XADD.SUM);
			_logStream.println("- Added in reward post-marginalization with no interm/next state vars.");
		}
		
		// Optional Display
		_logStream.println("- Q^" + _nCurIter + "(" + a._sName + ", " + a._actionParams + " )\n" + _context.getString(q));
		if (DISPLAY_PREMAX_Q){
			_mdp.displayGraph(q, "Q-" + a._sName + "-" + a._actionParams + "^" + _nCurIter + "-" + Math.round(1000*APPROX_ERROR));
		}
		// Noise handling
		if (a._noiseVars.size() == 0) {
			// No action params to maximize over
			_logStream.println("- Q^" + _nCurIter + "(" + a._sName + " ):\n" + " No noise parameters to max over, skipping this step.");
		} else {
			// Max in noise constraints and min out each noise parameter in turn
			// NOTE: we can do this because noise parameters can only reference state variables 
			//       (not currently allowing them to condition on intermediate or other noise vars)
			//       hence legal values of noise var determined solely by the factor for that var
			HashSet<String> q_vars = _context.collectVars(q);
			for (String nvar : a._noiseVars) {
	
				if (!q_vars.contains(nvar)) {
					_logStream.println("- Skipping noise var '" + nvar + "', which does not occur in q: " + _context.collectVars(q));
					continue;
				}
				
				_logStream.println("- Minimizing over noise param '" + nvar + "'");
				int noise_factor = a._hmNoise2DD.get(nvar);
				q = _context.apply(noise_factor, q, XADD.MAX); // Max in the noise so illegal states get replace by +inf, otherwise q replaces -inf
				q = minOutVar(q, nvar, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
				_logStream.println("-->: " + _context.getString(q));
				
				// Can be computational expensive (max-out) so flush caches if needed
				flushCaches(Arrays.asList(q) /* additional node to save */);
			}
			_logStream.println("- Done noise parameter minimization");
			_logStream.println("- Q^" + _nCurIter + "(" + a._sName + " )" + _context.collectVars(q) + "\n" + _context.getString(q));
		}		
		
		// Continuous action parameter maximization
		if (a._actionParams.size() == 0) {
			// No action params to maximize over
			_logStream.println("- Q^" + _nCurIter + "(" + a._sName + " ):\n" + " No action parameters to max over, skipping this step.");
		} else 
		{
		
				// Max out each action param in turn
			HashSet<String> q_vars = _context.collectVars(q);
			for (int i=0; i < a._actionParams.size(); i++) 
			{
				String avar = a._actionParams.get(i);
				double lb   = a._hmAVar2LB.get(avar);
				double ub   = a._hmAVar2UB.get(avar);
	
				if (!q_vars.contains(avar)) {
					_logStream.println("- Skipping var '" + avar + "': [" + lb + "," + ub + "], which does not occur in q: " + _context.collectVars(q));
					continue;
				}
				 //discretizing for continuous domains ( C is the step size) 
				if (DISCRETIZE_PROBLEM)
				{
					_logStream.println("- DISCRETIZING '" + avar + " into "+ DISCRETE_NUMBER + " discrete actions");
					int actionTree = q;
					Integer maximizedTree = null;
					double range= GLOBAL_LB;
					double stepsize =  (GLOBAL_UB - GLOBAL_LB)/(DISCRETE_NUMBER-1);
					int var_id = _context._cvar2ID.get(avar);
					int actionReplace = -1;
					//int interval = (ub - lb) / 10;
					while (range<= GLOBAL_UB)
					{
						ArithExpr range_a = new DoubleExpr(range);
						Integer actionValue =_context.getTermNode(range_a);
						// Check cache
						_contRegrKey.set(var_id, actionValue, actionTree);
						Integer result = null;
						if ((result = _mdp._hmContRegrCache.get(_contRegrKey)) != null)
							actionReplace= result;
						else
						{
						// Perform regression via delta function substitution
							actionReplace = _context.reduceProcessXADDLeaf(actionValue, 
								_context.new DeltaFunctionSubstitution(avar, actionTree), true);
						
						// Cache result
						_mdp._hmContRegrCache.put(new IntTriple(_contRegrKey), actionReplace);
						}
						
						maximizedTree= (maximizedTree == null) ? actionReplace :
											_context.apply(maximizedTree, actionReplace, XADD.MAX);
						maximizedTree = _context.reduceRound(maximizedTree); // Round!
						maximizedTree = _context.reduceLP(maximizedTree); // Rely on flag XADD.CHECK_REDUNDANCY

						range = range+ stepsize;
					    //_mdp.flushCaches(Arrays.asList(maximizedTree,actionReplace) /* additional node to save */);

					}
					q = maximizedTree;
				}
				else //not discretizing! continuous version
				{
					_logStream.println("- Maxing out action param '" + avar + "': [" + lb + "," + ub + "]");
					q = maxOutVar(q, avar, lb, ub);
					_logStream.println("-->: " + _context.getString(q));
					// Can be computational expensive (max-out) so flush caches if needed
					flushCaches(Arrays.asList(q) /* additional node to save */);
				}
				
				if (DISPLAY_PREMAX_Q){
					_mdp.displayGraph(q, "Q-" + a._sName + "-" + a._actionParams + "-End^" + _nCurIter + "-" + Math.round(1000*APPROX_ERROR));
				}
			  }
		
			_logStream.println("- Done action parameter maximization");
			_logStream.println("- Q^" + _nCurIter + "(" + a._sName + " )\n" + _context.getString(q));
		}

		// Constraints not currently allowed, should be applied to the reward as -Infinity
		//if (_mdp._alConstraints.size() > 0) {
		//	System.err.println("WARNING: constraint application currently not verified");
		//	System.exit(1);
		//	for (Integer constraint : _mdp._alConstraints)
		//		q = _context.apply(q, constraint, XADD.PROD); 
		//	q = _context.reduceLP(q);
		//}

		return q;
	}
	
	public int regressCVars(int q, CAction a, String var) {
		
		// Get cpf for continuous var'
		int var_id = _context._cvar2ID.get(var);
		Integer dd_conditional_sub = a._hmVar2DD.get(var);

		_logStream.println("- Integrating out: " + var + "/" + var_id /* + " in\n" + _context.getString(dd_conditional_sub)*/);

		// Check cache
		_contRegrKey.set(var_id, dd_conditional_sub, q);
		Integer result = null;
		if ((result = _mdp._hmContRegrCache.get(_contRegrKey)) != null)
			return result;
		
		// Perform regression via delta function substitution
		q = _context.reduceProcessXADDLeaf(dd_conditional_sub, 
				_context.new DeltaFunctionSubstitution(var, q), true);
		
		// Cache result
		_logStream.println("-->: " + _context.getString(q));
		_mdp._hmContRegrCache.put(new IntTriple(_contRegrKey), q);
		
		return q;		
	}

	public int regressBVars(int q, CAction a, String var) {
		
		// Get cpf for boolean var'
		int var_id = _context.getVarIndex( _context.new BoolDec(var), false);
		Integer dd_cpf = a._hmVar2DD.get(var);
		
		_logStream.println("- Summing out: " + var + "/" + var_id /*+ " in\n" + _context.getString(dd_cpf)*/);
		q = _context.apply(q, dd_cpf, XADD.PROD);
		
		// Following is a safer way to marginalize (instead of using opOut
		// based on apply) in the event that two branches of a boolean variable 
		// had equal probability and were collapsed.
		int restrict_high = _context.opOut(q, var_id, XADD.RESTRICT_HIGH);
		int restrict_low  = _context.opOut(q, var_id, XADD.RESTRICT_LOW);
		q = _context.apply(restrict_high, restrict_low, XADD.SUM);

		_logStream.println("-->: " + _context.getString(q));

		return q;
	}

	// Works backward from this root factor 
	public Graph buildDBNDependencyDAG(CAction a, HashSet<String> vars) {
		Graph g = new Graph(true, false, true, false);
		HashSet<String> already_seen = new HashSet<String>();
		
		// We don't want to generate parents for the following "base" variables
		already_seen.addAll( _mdp._hsContSVars );
		already_seen.addAll( _mdp._hsBoolSVars );
		already_seen.addAll( _mdp._hsContAVars ); 
		already_seen.addAll( _mdp._hsNoiseVars ); 
		
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
			if (!child_var.equals(parent_var) || _mdp._hsContIVars.contains(parent_var) || _mdp._hsContNSVars.contains(parent_var)) {
				g.addUniLink(child_var, parent_var);
				//System.out.println("Adding link " + child_var + " --> " + parent_var);
			} else if(child_var.equals(parent_var)){ 
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
				(_mdp._hsContIVars.contains(var) ||
				 _mdp._hsContNSVars.contains(var)))
				filter_vars.add(var);
			else if (allow_bool &&
				(_mdp._hsBoolIVars.contains(var) ||
				 _mdp._hsBoolNSVars.contains(var)))
				filter_vars.add(var);
		return filter_vars;
	}
		
	public int maxOutVar(int ixadd, String var, double lb, double ub) {
		XADDLeafMinOrMax max = _context.new XADDLeafMinOrMax(var, lb, ub, true /* is_max */, _logStream);
		ixadd  = _context.reduceProcessXADDLeaf(ixadd, max, false);
		return max._runningResult;
	}
	
	public int minOutVar(int ixadd, String var, double lb, double ub) {
		XADDLeafMinOrMax min = _context.new XADDLeafMinOrMax(var, lb, ub, false /* is_max */, _logStream);
		ixadd  = _context.reduceProcessXADDLeaf(ixadd, min, false);
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
		try { System.in.read(); } catch (Exception e) {}
		System.exit(1);
	}
	
}

