package camdp.solver;

import graph.Graph;

import java.io.PrintStream;
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



	public Integer _CurIter;   // Current iteration for Value Iteration	
	public Integer _maxDD;
	
	/* Static variables */
	public static long _lTime; // For timing purposes
	public static Runtime RUNTIME = Runtime.getRuntime();
	
	/* DEBUG Variables */
	private static final boolean MAIN_DEBUG = true;
	private static final boolean REGRESS_DEBUG = false;
	
	//Display Flags
	public final static boolean DISPLAY_PREMAX_Q = false;
	public final static boolean DISPLAY_POSTMAX_Q = false;

	private static final boolean DISPLAY_DD = true;

	private static final boolean PRINT_DD = false;
	
	//////////////////Methods /////////////////////////////////
	
	public VI(CAMDP camdp){
		_mdp = camdp;
		_context = camdp._context;
		_valueDD = _context.NEG_INF;
		_logStream = camdp._logStream;
		_solveMethod = "VI";
		makeResultStream();
		setupSolution();
	}
	
	public void setupSolution(){
		solutionDDList.add(null);
		solutionTimeList.add(null);
		solutionNodeList.add(null);
		solutionInitialSValueList.add(null);
		solutionMaxValueList.add(null);
		
		_mdp.optimalDDList.add(null);
		_mdp.optimalMaxValueList.add(null);
	}
	
    ////////Main Solver Class ///////////////
	public int solve(int max_iter){		
		if (MAIN_DEBUG) System.out.println("Starting VI solution, Max #Iterations = " + max_iter);
		Integer _prevDD = null;
		//Iteration counter
		_CurIter = 0;
		if (max_iter < 0) max_iter = _mdp._nMaxIter;
		
		//Initialize value function to zero
		_valueDD = _context.ZERO;

		// Perform value iteration for specified number of iterations, or until convergence detected
		while (_CurIter < max_iter) 
		{
			++_CurIter;
			resetTimer();
			
			_logStream.println(ASCII_BAR + "\nITERATION #" + _CurIter + ", " + 
			CAMDP.MemDisplay() + " bytes, " + getElapsedTime() + " ms\n" + ASCII_BAR);
			_logStream.flush();

			// Prime diagram
			_prevDD = _valueDD;

			long iniT = getElapsedTime();
			bellmanBackup();
			long endT = getElapsedTime();
			if (MAIN_DEBUG) System.out.println("VI Backup Took:"+(endT - iniT) );
			
			if (_mdp.LINEAR_PROBLEM && APPROX_PRUNING) {
				long appTime = getElapsedTime();
				_valueDD = _context.linPruneRel(_valueDD, APPROX_ERROR);
				long endTime = getElapsedTime() - appTime;
				System.out.println("Approx Finish"+ _CurIter+ " Iter took: "+appTime+ " pruning: "+endTime);
				//displayGraph(_valueDD, "valPruned-" + _nCurIter+" e"+APPROX_ERROR);
			}

			if (MAIN_DEBUG){
				System.out.println("Iter:" + _CurIter+" Complete");
				_logStream.println("Iter complete:" + _CurIter + _context.getString(_valueDD));
				if (DISPLAY_DD) _mdp.doDisplay(_valueDD,makeXADDLabel("V",_CurIter, APPROX_ERROR));
			}
			
			if (solutionDDList.size() != _CurIter){
				System.err.println("Wrong Result Storage");
			}
			solutionDDList.add(_valueDD);
			_mdp.optimalDDList.add(_valueDD);
			solutionTimeList.add(getElapsedTime());
			solutionNodeList.add(_context.getNodeCount(_valueDD));
			if (_mdp.LINEAR_PROBLEM) {
				solutionMaxValueList.add(_context.linMaxVal(_valueDD));
				_mdp.optimalMaxValueList.add(_context.linMaxVal(_valueDD));
			}
			if( _mdp._initialS != null){
				solutionInitialSValueList.add(_context.evaluate(_valueDD, _mdp._initialS._hmBoolVars, _mdp._initialS._hmContVars));
			}

			
			if (_prevDD.equals(_valueDD) ) {
				if (MAIN_DEBUG) System.out.println("VI: Converged to solution early,  at iteration "+_CurIter);
				int it = _CurIter;
				_mdp.optimalHorizon = _CurIter;
				while (++it <= max_iter){					
					_mdp.optimalDDList.add(_valueDD);
					if (_mdp.LINEAR_PROBLEM) {
						_mdp.optimalMaxValueList.add(_mdp.optimalMaxValueList.get(_CurIter));
					}
				}
				break;
			}

		}

		flushCaches();	

		//////////////////////////////////////////////////////////////////////////
		// Performance Logging
		_logStream.println("\nValue iteration complete!");
		_logStream.println(max_iter + " iterations took " + getElapsedTime() + " ms");
		_logStream.println("Canonical / non-canonical: " + OperExpr.ALREADY_CANONICAL + " / " + OperExpr.NON_CANONICAL);
		_logStream.println("\nIteration Results summary");
		for (int i = 1; i <= max_iter && (_mdp.optimalHorizon == null || i <=_mdp.optimalHorizon); i++) _logStream.println("Iter " + i + ": nodes = " + solutionNodeList.get(i) + "\ttime = " + solutionTimeList.get(i) + " ms");

		return _CurIter;
	}

	private void bellmanBackup() {
		// Iterate over each action
		_maxDD = null;
		for (Map.Entry<String,CAction> me : _mdp._hmName2Action.entrySet()) {

			// Regress the current value function through each action (finite number of continuous actions)
			int regr = regress(_valueDD, me.getValue());
			regr  = _context.reduceRound(regr); // Round!
			if (DISPLAY_POSTMAX_Q)
				_mdp.doDisplay(regr, makeXADDLabel("Q-"+me.getKey(), _CurIter, APPROX_ERROR));

			// Maintain running max over different actions
			_maxDD = (_maxDD == null) ? regr : _context.apply(_maxDD, regr, XADD.MAX);
			_maxDD = _context.reduceRound(_maxDD); // Round!
			_maxDD = _context.reduceLP(_maxDD); // Rely on flag XADD.CHECK_REDUNDANCY

			//Optional post-max approximation 
			// Could be used safely if overall error is being monitored 
			if (_mdp.LINEAR_PROBLEM && APPROX_ALWAYS)
				_maxDD = _context.linPruneRel(_maxDD, APPROX_ERROR);

			// Error checking and logging
			int canon_max_dd = _context.makeCanonical(_maxDD);
			if (_maxDD != canon_max_dd) {
				System.err.println("CAMDP VI ERROR: encountered non-canonical node that should have been canonical... could be rounding, continuing.");
				_maxDD = _context.makeCanonical(_maxDD);
			}
			
			flushCaches(Arrays.asList(_maxDD));
			if (REGRESS_DEBUG){
				System.out.println("Iter "+_CurIter+" Action "+me.getValue()._sName+" Maximization Complete.");
				if (PRINT_DD){ 
					System.out.println("MaxDD = "+ _context.getExistNode(_maxDD));
				}
			}
		}

		_valueDD = _context.reduceLP(_maxDD);
	}

	public void printResults() {
		System.out.println("Results for Value Iteration: " + (solutionTimeList.size()-1) + " iterations:");
		System.out.print("Time:"); printList( solutionTimeList, System.out); System.out.println(";");
		System.out.print("Nodes:"); printList( solutionNodeList, System.out); System.out.println(";");
		System.out.print("Initial S Value:"); printList( solutionInitialSValueList, System.out); System.out.println(";");
	}
	
	private void printList(List l, PrintStream out){
		for(int i=1; i<l.size(); i++) out.print(" "+l.get(i));
	}
	
	private IntTriple _contRegrKey = new IntTriple(-1,-1,-1);
	
	/**
	 * Regress a DD through an action
	 **/
	public int regress(int vfun, CAction a) {
		if (REGRESS_DEBUG){
			System.out.println("REGRESSING ACTION " + a._sName + " Iter "+ _CurIter );
		}
		_logStream.println("\n>>> REGRESSING '" + a._sName + "'\n");
		
		// Prime the value function 
		int q = _context.substitute(vfun, _mdp._hmPrimeSubs); 
		_logStream.println("- Primed value function:\n" + _context.getString(q));
		
		// Discount
		q = _context.scalarOp(q, _mdp._bdDiscount.doubleValue(), XADD.PROD);
		
		if (REGRESS_DEBUG){
			System.out.println("Q After dicount Prod:");
			if (PRINT_DD) System.out.println(_context.getExistNode(q).toString());
			if (DISPLAY_DD) _context.showGraph(q, "Q after disc Prod");
		}
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
		_logStream.println("- Q^" + _CurIter + "(" + a._sName + ", " + a._actionParams + " )\n" + _context.getString(q));
		if (REGRESS_DEBUG){
			System.out.println("Q before Max:");
			if (DISPLAY_PREMAX_Q){
				_mdp.displayGraph(q, "Q-" + a._sName + "-" + a._actionParams + "^" + _CurIter + "-" + Math.round(1000*APPROX_ERROR));
			}
		}

		// Noise handling
		if (a._noiseVars.size() == 0) {
			// No action params to maximize over
			_logStream.println("- Q^" + _CurIter + "(" + a._sName + " ):\n" + " No noise parameters to max over, skipping this step.");
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
				flushCaches(_maxDD == null? Arrays.asList(q): Arrays.asList(q, _maxDD));
			}
			_logStream.println("- Done noise parameter minimization");
			_logStream.println("- Q^" + _CurIter + "(" + a._sName + " )" + _context.collectVars(q) + "\n" + _context.getString(q));
		}		
		
		// Continuous action parameter maximization
		if (a._actionParams.size() == 0) {
			// No action params to maximize over
			_logStream.println("- Q^" + _CurIter + "(" + a._sName + " ):\n" + " No action parameters to max over, skipping this step.");
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
				 
				_logStream.println("- Maxing out action param '" + avar + "': [" + lb + "," + ub + "]");
				q = maxOutVar(q, avar, lb, ub);
				_logStream.println("-->: " + _context.getString(q));
				
				// Can be computational expensive (max-out) so flush caches if needed
				flushCaches( (_maxDD == null? Arrays.asList(q): Arrays.asList(q, _maxDD)) /* additional node to save */);
				if (REGRESS_DEBUG){
					if (DISPLAY_PREMAX_Q){
					_mdp.displayGraph(q, "Q-" + a._sName + "-" + a._actionParams + "-End^" + _CurIter + "-" + Math.round(1000*APPROX_ERROR));
					}
				}
			  }
		
			_logStream.println("- Done action parameter maximization");
			_logStream.println("- Q^" + _CurIter + "(" + a._sName + " )\n" + _context.getString(q));
		}

		if (REGRESS_DEBUG) System.out.println("Regress End");
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

	public String makeXADDLabel(String xadd, int iter, double approx)
	{
		return  xadd + " Iter"+iter+ (approx > 0? "-approx"+String.format("%03d",Math.round(1000*approx)): "");
	}
}


