package camdp.solver;

import graph.Graph;

import java.util.*;
import java.util.Map.Entry;

import util.IntTriple;
import util.Pair;
import xadd.ExprLib;
import xadd.XADD;

import camdp.CAMDP;
import camdp.State;
import camdp.CAction;

public class CRTDP extends CAMDPsolver {

	public Integer _curTrial = null;
	public Integer _nTrials = null;
	/* Static variables */
	public static long _lTime; // For timing purposes
	public static Runtime RUNTIME = Runtime.getRuntime();
	
	/////////////////////////
	/* Solution Parameters */
	/////////////////////////
	
	public final int MAXIMUM_ITER = 10000;
	public final int MAXIMUM_TRIAL = 1000;
	public final int DEFAULT_TRIAL = 100;
	public final boolean MAINTAIN_POLICY = false;
	public final boolean DISCRETIZE_PROBLEM = false;
	
	/* Optimal solution maintenance */
	public ArrayList<Integer> optimalDD = new ArrayList<Integer>();
	public double []optimalMaxValues = new double[MAXIMUM_ITER];
	
	/* Discretization Parameters */
	public int DISCRETE_NUMBER = 11;
	public int GLOBAL_LB=-9;
	public int GLOBAL_UB=9;
	
	/* Debugging Flags */
	
	private static final boolean MAIN_DEBUG = false;
	private static final boolean DISPLAYS_STATS = false;
	private static final boolean DEBUG_TRIAL = false;
	private static final boolean BELLMAN_DEBUG = false;
	private static final boolean REGRESS_DEBUG = false;
	private static final boolean SAMPLING_DEBUG = false;
	private static final boolean PLOT_DD = false;
	private static final boolean PRINT_DD = false;	
	//////////////////Methods /////////////////////////////////
	
	public CRTDP(CAMDP camdp){
		initCRTDP(camdp, DEFAULT_TRIAL);
	}
	
	public CRTDP(CAMDP camdp, int nTrials){
		initCRTDP(camdp, nTrials);
	}
	
	private void initCRTDP(CAMDP camdp, int nTrials){
		_mdp = camdp;
		_context = camdp._context;
		_valueDD = _context.NEG_INF;
		_logStream = camdp._logStream;
		_nTrials = nTrials;
		_solveMethod = "CRTDP";
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
	public int solve(int max_depth){
		
		if (_mdp._initialS == null){
			System.err.println("Impossible to solve Unknown Initial State MDP with RTDP!");
			return -1;
		}
		Integer prevDD = null;
		boolean solConverge = false;
		
		//Trial Counter counter
		if (max_depth < 0)
			max_depth = _mdp._nMaxIter;
		
		// Trial statistics
		int totalTime=0;
		long[] time = new long[_nTrials + 1];
		int[] num_nodes = new int[_nTrials + 1];
		int[] num_leaves = new int[_nTrials + 1];
		int[] num_branches = new int[_nTrials + 1];

		//Initialize value function to zero assumes ZERO is admissible, need a heuristic evaluation
		//TODO: Heuristic Function
		_valueDD = _context.ZERO;
		_curTrial = 0;
		
		// Repeat Trials until convergence
		while (_curTrial < _nTrials && !solConverge) 
		{
			++_curTrial;
			ResetTimer();
			if (MAIN_DEBUG){
				System.out.println("\n"+ASCII_BAR+"\nStarting Trial# " + _curTrial +" with max depth = " + max_depth);
				if (PRINT_DD) System.out.println("Initial Value DD = "+_valueDD+" DD:"+_context.getString(_valueDD));
				_logStream.println(ASCII_BAR + "\nTRIAL #" + _curTrial + ", " + 
						MemDisplay() + " bytes, " + GetElapsedTime() + " ms\n" + ASCII_BAR);
						_logStream.flush();
			}
			

			// Save Previous diagram
			prevDD = _valueDD;
			
			//Perform Trial
			_valueDD = makeTrial(_valueDD, _mdp._initialS, max_depth);

			if (MAIN_DEBUG){
				System.out.println("Trial:" + _curTrial+" Complete");
				if (PRINT_DD) System.out.println("Value after Trial = "+_valueDD+" DD:" + _context.getString(_valueDD));
				_logStream.println("Trial complete:" + _curTrial + _context.getString(_valueDD));
				//if (PLOT_DD) 
				_mdp.doDisplay(_valueDD,makeXADDLabel("V",_curTrial, max_depth, APPROX_ERROR));
			}
			//Optional post-max approximation - Could be used safely if overall error is being monitored 
			_valueDD = checkLinearAndApprox(_valueDD);
			checkCanon(_valueDD);
			approxValueDD();
			flushCaches();
			
			if (MAIN_DEBUG){
				if (PRINT_DD) System.out.println("Value after CheckLinear, Canon & Prune = "+_valueDD+" DD:" + _context.getString(_valueDD));
			}
			//////////////////////////////////////////////////////////////////////////
			// Value iteration statistics
			time[_curTrial] = GetElapsedTime();
			totalTime += time[_curTrial];
			num_nodes[_curTrial] = _context.getNodeCount(_valueDD);
			num_leaves[_curTrial] = _context.getLeafCount(_valueDD);
			num_branches[_curTrial] = _context.getBranchCount(_valueDD);

			double maxVal = 0d;
			double maxRelErr = 0d;
			if (_mdp.LINEAR_PROBLEM) {
				maxVal = _context.linMaxVal(_valueDD);
				optimalMaxValues[_curTrial-1] = maxVal;
				if (COMPARE_OPTIMAL){
					if(APPROX_ERROR == 0d){ //Exact solution
						if (optimalDD.size() != _curTrial-1)
							System.err.println("Incorrect optimalDD:"+optimalDD+" "+_curTrial);
						optimalDD.add(_valueDD);
					}
					if (optimalDD.size() > _curTrial-1){
						maxRelErr = (_context.linMaxDiff(optimalDD.get(_curTrial-1), _valueDD))/optimalMaxValues[_curTrial-1];
					}
					else maxRelErr = -1;
				}
			}
			_logStream.println("Value function size @ end of iteration " + _curTrial + 
					": " + num_nodes[_curTrial] + " nodes = " + 
					num_branches[_curTrial] + " cases" + " in " + time[_curTrial] + " ms");

			//APPROX_TEST LOG, outputs: iter, #node, #branches, #UsedMem(MB), IterTime, TotTime, MaxVal, RelErr
			_resultStream.format("%d %d %d %d %d %d %d %f %f\n", _curTrial, num_nodes[_curTrial], 
					num_leaves[_curTrial], num_branches[_curTrial], usedMem(), 
					time[_curTrial], totalTime,
					_context.linMaxVal(_valueDD), maxRelErr );
			}
//////////////////////////////////////////////////////////////////////
			if (prevDD.equals(_valueDD) ) {
				System.out.println("! CRTDP: Converged to solution early,  at iteration "+_curTrial);
				int it = _curTrial;
				while (++it < _nTrials){
					optimalMaxValues[it] = optimalMaxValues[_curTrial];
					optimalDD.add(_valueDD);
					_logStream.format("%d %d %d %d %d %d %d %f %f\n", it, num_nodes[_curTrial], num_leaves[_curTrial], 
							num_branches[_curTrial], usedMem(),
							time[_curTrial],totalTime,
							_context.linMaxVal(_valueDD));
				}
				solConverge = true;
			}		
		flushCaches();	

		//////////////////////////////////////////////////////////////////////////
		// Performance Logging
		_logStream.println("\n CRTDP complete!");
		_logStream.println(_nTrials + " trials took " + GetElapsedTime() + " ms");
		_logStream.println("Canonical / non-canonical: " + ExprLib.OperExpr.ALREADY_CANONICAL + " / " + ExprLib.OperExpr.NON_CANONICAL);
		_logStream.println("\nTrials Results summary");
		for (int i = 1; i <= _nTrials; i++) {
		String branch_count = num_branches[i] >= 0 
		? "" + num_branches[i] : " > " + XADD.MAX_BRANCH_COUNT; 
		_logStream.println("Trial " + i + ": nodes = " + num_nodes[i] + "\tbranches = " + branch_count + "\ttime = " + time[i] + " ms");
		}
		//
		////////////////////////////////////////////////////////////////////////

		return _curTrial;
	}

	private void approxValueDD() {
		if (APPROX_PRUNING) {
			long appTime = GetElapsedTime();
			_valueDD = _context.linPruneRel(_valueDD, APPROX_ERROR);
			long pruneTime = GetElapsedTime() - appTime;
			System.out.println("ApproxResult: Trial "+ _curTrial+ " Solve time = "+appTime+ ", Prune time = "+pruneTime);
		}
	}
	private Integer checkLinearAndApprox(Integer maxDD) {
		if ( _mdp.LINEAR_PROBLEM && APPROX_ALWAYS)
			maxDD = _context.linPruneRel(maxDD, APPROX_ERROR);
		return maxDD;
	}

	private Integer checkCanon(Integer maxDD) {
		// Error checking and logging
		int canon_max_dd = _context.makeCanonical(maxDD);
		if (maxDD != canon_max_dd) {
			System.err.println("CAMDP VI ERROR: encountered non-canonical node that should have been canonical... could be rounding, continuing.");
			_context.exportXADDToFile(maxDD, "ERRORdiagram1OriginalXADD.xadd");
			_context.exportXADDToFile(canon_max_dd, "ERRORdiagram2makeCanonical.xadd");
			_context.getGraph(maxDD).launchViewer("ERROR diagram 1: original maxDD");
			_context.getGraph(canon_max_dd).launchViewer("ERROR diagram 2: makeCanonical(maxDD)");
		}
		return maxDD;
	}

	private int makeTrial(Integer valueDD, State currentS, int depth){
		
		while (depth > 0){
			if (DEBUG_TRIAL){
				System.out.println("Trial #"+_curTrial+", depth ="+depth+", "+ currentS.toString());	
				System.out.println("State Value = "+ _context.evaluate(valueDD, currentS._hmBoolVars, currentS._hmContVars) );
				if (PRINT_DD) System.out.println("Initial Value = "+valueDD+" DD:\n"+ _context.getExistNode(valueDD) +"\n");
			}
			Pair<Integer, ParametrizedAction> bellmanResult = regionBellmanBackup(valueDD, currentS);
			valueDD = bellmanResult._o1;
			ParametrizedAction greedyAction = bellmanResult._o2;
			//Using greedy action, sample next state
			State nextS = sample(currentS, greedyAction);
		
			if (DEBUG_TRIAL){
				System.out.println("State After Sample = "+nextS);
				System.out.println("New State Value = "+ _context.evaluate(valueDD, currentS._hmBoolVars, currentS._hmContVars) +"\n" );
				if (PRINT_DD) System.out.println("Value After Backup = "+ valueDD +" DD:\n"+ _context.getExistNode(valueDD) +"\n");
				if (PLOT_DD) _mdp.doDisplay(valueDD,makeXADDLabel("V", _curTrial, depth, APPROX_ERROR));
			}
			currentS = nextS;
			depth = depth - 1;
		}		
		return valueDD;
	}

	{ //Commenter makeTrialRec
//	private int makeTrial(Integer valueDD, State currentS, int depth){
//		
//		if (depth <= 0){
//			return valueDD;
//		}
//		
//		if (DEBUG_TRIAL){
//			System.out.println("Trial #"+_curTrial+", depth ="+depth+", "+ currentS.toString());	
//			System.out.println("State Value = "+ _context.evaluate(valueDD, currentS._hmBoolVars, currentS._hmContVars) );
//			System.out.println("Initial Value = "+ _context.getExistNode(valueDD) +"\n");
//		}
//		Pair<Integer, ParametrizedAction> bellmanResult = regionBellmanBackup(valueDD, currentS);
//		valueDD = bellmanResult._o1;
//		ParametrizedAction greedyAction = bellmanResult._o2;
//		//Using greedy action, sample next state
//		State nextS = sample(currentS, greedyAction);
//		
//		if (DEBUG_TRIAL){
//			System.out.println("State After Sample = "+nextS);
//			System.out.println("New State Value = "+ _context.evaluate(valueDD, currentS._hmBoolVars, currentS._hmContVars) );
//			System.out.println("Value After Backup = "+ _context.getExistNode(valueDD) +"\n");
//			if (PLOT_DD) _mdp.doDisplay(valueDD,makeXADDLabel("V", _curTrial, depth, APPROX_ERROR));
//		}
//		
//		return makeTrial(valueDD, nextS, depth -1);
//
//		
////		for (Map.Entry<String,CAction> me : _mdp._hmName2Action.entrySet()) {
////			CAction a = me.getValue();
////			HashMap<String, Double> aParams = new HashMap<String,Double>();
////			for(String aVar : a._actionParams){
////				double lb = a._hmAVar2LB.get(aVar);
////				double ub = a._hmAVar2UB.get(aVar);
////				aParams.put(aVar, (lb + ub)/2.0);
////			}
////			ParametrizedAction pA = new ParametrizedAction(a, aParams);
////			nextS = sample(currentS, pA);
////			System.out.println(" Sampling action: "+ a._sName + ", with params: "+ aParams.toString() +
////					" from " + currentS.toString() + " led to " + nextS.toString());
////		}	
//	}
	}
	
	public Pair<Integer, ParametrizedAction>  regionBellmanBackup(int vfun, State currS){
		//		 Iterate over each action
		Integer maxDD = null;
		CAction maxAction = null;
		Double currSValue = null;
		HashMap<String, Double> maxParam = new HashMap<String, Double>();
		
		if (BELLMAN_DEBUG){
			System.out.println("Bellman Backup Start: "+currS);
			if (PRINT_DD) System.out.println("Original ValueDD = "+vfun+"DD:\n"+ _context.getExistNode(vfun));
		}
		
		for (Map.Entry<String,CAction> me : _mdp._hmName2Action.entrySet()) {
				// Regress the current value function through each action (finite number of continuous actions)
				
				int regr = regressRegion(vfun, me.getValue(), currS);
				if (regr != _context.reduceRound(regr) ) System.out.println("Still Rouding issues on Regress!"); // Round!
				regr = _context.reduceLP(regr);
				
				// Maintain running max over different actions
				maxDD = (maxDD == null) ? regr : _context.apply(maxDD, regr, XADD.MAX);
				maxDD = _context.reduceRound(maxDD); // Round!
				maxDD = _context.reduceLP(maxDD); // Rely on flag XADD.CHECK_REDUNDANCY

				double value = _context.evaluate(regr, currS._hmBoolVars, currS._hmContVars);
				
				if (BELLMAN_DEBUG){
					System.out.println("Regress Action: " + me.getValue()._sName + " Value:" + value);
					if (PRINT_DD) System.out.println("Q Regress = "+regr+" DD:\n" + _context.getExistNode(regr));
				}
				if (currSValue == null || value > currSValue){
					maxAction = me.getValue();
					currSValue =value; 
				}
		}
		
		if (BELLMAN_DEBUG){
			if (PRINT_DD) System.out.println("MAX of Regr = "+maxDD+" DD:\n" + _context.getExistNode(maxDD));
		}

		//Min out Ilegal +Inf values, these will be non update regions
		vfun = _context.apply(maxDD, vfun, XADD.MIN);
		if (BELLMAN_DEBUG){
			if (PRINT_DD) System.out.println("\n New Vfun Before Reduce ="+vfun+" DD:\n" + _context.getExistNode(vfun));
		}
		vfun = _context.reduceLP(vfun); // Rely on flag XADD.CHECK_REDUNDANCY
		ParametrizedAction pA = new ParametrizedAction(maxAction, maxParam); 
		
		if (BELLMAN_DEBUG){
			System.out.println("\nBackup End, Greedy Action: " + pA + " Greedy Value:" + currSValue +"\n");
			if (PRINT_DD) System.out.println("New vfun ="+vfun+" DD:\n" + _context.getExistNode(vfun));
		}
		return new Pair<Integer, ParametrizedAction> (vfun, pA);
	}
	
	/**
	 * Regress a DD through an action
	 **/
	private IntTriple _contRegrKey = new IntTriple(-1,-1,-1);

	public int regressRegion(int vfun, CAction a, State currS) {
		_logStream.println("\n>>> REGRESSING '" + a._sName);
		
		// Prime the value function 
		int q = _context.substitute(vfun, _mdp._hmPrimeSubs); 
		_logStream.println("- Primed value function:\n" + _context.getString(q));
		
		if (REGRESS_DEBUG){
			System.out.println("Regressing: "+vfun+" at " +currS+" with "+a._sName);
			if (PLOT_DD) _mdp.doDisplay(vfun, makeXADDLabel("Qstart-"+a._sName, _curTrial, 0, APPROX_ERROR));
		}
		
		// Discount
		q = _context.scalarOp(q, _mdp._bdDiscount.doubleValue(), XADD.PROD);
		
		int maskReward = _context.createPosInfMask(a._reward,currS._hmBoolVars, currS._hmContVars);

		// Add reward *if* it contains primed vars that need to be regressed
		HashSet<String> i_and_ns_vars_in_reward = filterIandNSVars(_context.collectVars(a._reward), true, true);
		if (!i_and_ns_vars_in_reward.isEmpty()) {
			q = _context.apply(maskReward, q, XADD.SUM); // Add reward to already discounted primed value function
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
				q = regressBVarsMask(q, a, var_to_elim, currS);
			} else if (_mdp._hsContIVars.contains(var_to_elim) || _mdp._hsContNSVars.contains(var_to_elim)) {
				q = regressCVarsMask(q, a, var_to_elim, currS);
			} else {
				// The topological sort will also add in current state and action variables since they were parents in the network
				_logStream.println("- Ignoring current state or action variable " + var_to_elim + " during elimination");
			}
		}
		
		if (i_and_ns_vars_in_reward.isEmpty()) {
			q = _context.apply(maskReward, q, XADD.SUM);
			_logStream.println("- Added in reward post-marginalization with no interm/next state vars.");
		}
				
		// Optional Display With just State, Action and Noise Vars
		if (REGRESS_DEBUG){
			System.out.println("Qfunction after removing NS = "+q+"DD:\n"+_context.getString(q));
			_logStream.println("- Q^" + "(" + a._sName + ", " + a._actionParams + " )\n" + _context.getString(q));
			if (PLOT_DD) _mdp.displayGraph(q, "Q-" + a._sName + "-" + a._actionParams + "^" + _curTrial + "-" + Math.round(1000*APPROX_ERROR));
		}
		
		//Remove Noise Vars
		q = regressNoise(q, a);		
		
		//Remove Action Vars
		q = regressAction(q, a);

		//Constraints Verification
		if (REGRESS_DEBUG){
			System.out.println("Finished Regressing: "+a._sName);
			if (PLOT_DD) _mdp.doDisplay(q, makeXADDLabel("Qend-"+a._sName, _curTrial, 0, APPROX_ERROR));
		}
		return q;
	}

	private int regressAction(int q, CAction a) {

		if (a._actionParams.size() == 0) {// No action params to maximize over			
			_logStream.println("- Q^" + _curTrial + "(" + a._sName + " ):\n" + " No action parameters to max over, skipping this step.");
			return q;
		}
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
				q = linearizedRegression(q, avar);
			else {	//not discretizing! continuous version
				_logStream.println("- Maxing out action param '" + avar + "': [" + lb + "," + ub + "]");
				q = maxOutVar(q, avar, lb, ub);
				_logStream.println("-->: " + _context.getString(q));
				// Can be computational expensive (max-out) so flush caches if needed
				flushCaches(Arrays.asList(q) /* additional node to save */);
			}
		}
		
		_logStream.println("- Done action parameter maximization");
		_logStream.println("- Q^" + _curTrial + "(" + a._sName + " )\n" + _context.getString(q));
		return q;
	}

	private int linearizedRegression(int q, String avar) {
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
			ExprLib.ArithExpr range_a = new ExprLib.DoubleExpr(range);
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
		return q;
	}

	private int regressNoise(int q, CAction a) {
		//If no Noise vars
		if (a._noiseVars.size() == 0) {
			_logStream.println("- Q^" + _curTrial + "(" + a._sName + " ):\n" + " No noise parameters to max over, skipping this step.");
			return q;
		}
		
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
		_logStream.println("- Q^" + _curTrial + "(" + a._sName + " )" + _context.collectVars(q) + "\n" + _context.getString(q));
		return q;
	}
	
	public int regressCVarsMask(int q, CAction a, String var, State currS) {
		
		// Get cpf for continuous var'
		int var_id = _context._cvar2ID.get(var);
		Integer dd_conditional_sub = a._hmVar2DD.get(var);

		//MaskTransitions not from current state
		dd_conditional_sub = _context.createPosInfMask(dd_conditional_sub, currS._hmBoolVars, currS._hmContVars);		
		
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

	public int regressBVarsMask(int q, CAction a, String var, State currS) {
		
		// Get cpf for boolean var'
		int var_id = _context.getVarIndex( _context.new BoolDec(var), false);
		Integer dd_cpf = a._hmVar2DD.get(var);
		
		//MaskTransitions not from current state
		dd_cpf = _context.createPosInfMask(dd_cpf, currS._hmBoolVars, currS._hmContVars);
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
		XADD.XADDLeafMinOrMax max = _context.new XADDLeafMinOrMax(var, lb, ub, true /* is_max */, _logStream);
		ixadd  = _context.reduceProcessXADDLeaf(ixadd, max, false);
		return max._runningResult;
	}
	
	public int minOutVar(int ixadd, String var, double lb, double ub) {
		XADD.XADDLeafMinOrMax min = _context.new XADDLeafMinOrMax(var, lb, ub, false /* is_max */, _logStream);
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
	
	public State sample(State currentS, ParametrizedAction parA){
		HashSet<String> AllNSVars = new HashSet<String>();
		AllNSVars.addAll(_mdp._hsBoolNSVars);
		AllNSVars.addAll(_mdp._hsContNSVars);
		Graph g = buildDBNDependencyDAG(parA._action, AllNSVars);
		if (g.hasCycle()) 
			displayCyclesAndExit(g, parA._action);
		
		List var_order = g.topologicalSort(true);
		
		// Sample each variable in the topological order
		State nextS = new State();
		for (Object o : var_order) {
			String var = (String)o;
			Integer dd_cpf = parA._action._hmVar2DD.get(var);
			
			if (_mdp._hsBoolNSVars.contains(var)) {
				Boolean val = sampleBVar(currentS, nextS, var, dd_cpf, parA._params);
				nextS._hmBoolVars.put(var, val);
			} else if (_mdp._hsContNSVars.contains(var)) {
				Double val = sampleCVar(currentS,nextS,dd_cpf, parA._params);
				nextS._hmContVars.put(var, val);
			} else {
				// The topological sort will also add in next state and action variables since they were parents in the network
				//System.out.println("- Ignoring Non-NextState variable " + var + " during sampling");
				//_logStream.println("- Ignoring Non-NextState variable " + var + " during sampling");
			}
		}
		return unprime(nextS);
	}
	
	public Boolean sampleBVar(State prevS, State nextS, String bVar, Integer bVarDD, HashMap<String, Double> aParams){
		HashMap<String, Boolean> bool_assign = makeBoolAssign(prevS, nextS);
		bool_assign.put(bVar, true); //bVar is the next state boolean we are sampling
		double trueProb = _context.evaluate(bVarDD, bool_assign, makeContAssign(prevS,nextS, aParams) );
		return (Math.random() < trueProb) ? true: false;
	}
	
	public Double sampleCVar(State prevS, State nextS, Integer cVarDD, HashMap<String, Double> aParams){
		return _context.evaluate(cVarDD, makeBoolAssign(prevS,nextS), makeContAssign(prevS,nextS, aParams));
	}

	public HashMap<String, Boolean> makeBoolAssign(State currentS, State nextS){
		HashMap<String, Boolean> bool_assign = new HashMap<String, Boolean>();
		bool_assign.putAll(currentS._hmBoolVars);
		bool_assign.putAll(nextS._hmBoolVars);
		return bool_assign;
	}
	
	public HashMap<String, Double> makeContAssign(State currentS,State nextS,HashMap<String, Double> aParams){
		HashMap<String, Double> cont_assign = new HashMap<String, Double>();
		cont_assign.putAll(currentS._hmContVars);
		cont_assign.putAll(nextS._hmContVars);
		cont_assign.putAll(aParams);
		return cont_assign;
	}
	
	private State unprime(State nextS) {
		State newS = new State();
		for(Entry<String, Boolean> e: nextS._hmBoolVars.entrySet()){
			String var = e.getKey();
			if (!var.endsWith("'")) System.out.println("Invalid (unprimed) Next State Var: "+ var);
			newS._hmBoolVars.put(var.substring(0, var.length()-1), e.getValue());
		}
		for(Entry<String, Double> e: nextS._hmContVars.entrySet()){
			String var = e.getKey();
			if (!var.endsWith("'")) System.out.println("Invalid (unprimed) Next State Var: "+ var);
			newS._hmContVars.put(var.substring(0, var.length()-1), e.getValue());
		}
		return newS;
	}
	
}