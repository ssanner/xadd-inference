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

	public Integer curIter;   // Current iteration for Value Iteration
	public Integer finalIter;   // Last Iteration in case of early Convergence
	public Integer _maxDD;
		
	/* Inner DEBUG Variables */
	private static final boolean REGRESS_DEBUG = false;

	
	//////////////////Methods /////////////////////////////////
	
	public VI(CAMDP camdp, int iter){
		mdp = camdp;
		context = camdp._context;
		valueDD = context.NEG_INF;
		_logStream = camdp._logStream;
		solveMethod = "VI";
		makeResultStream();
		nIter = iter;
		setupResults();
	}
	
    ////////Main Solver Class ///////////////
	public int solve(){		
		if (MAIN_DEBUG) debugOutput.println("Starting VI solution, Max #Iterations = " + nIter+"\n");
		Integer _prevDD = null;
		//Iteration counter
		curIter = 0;		

		//Initialize value function to zero
		valueDD = context.ZERO;

		// Perform value iteration for specified number of iterations, or until convergence detected
		while (curIter < nIter) 
		{
			++curIter;
			resetTimer();
			
			// Prime diagram
			_prevDD = valueDD;

			long iniT = getElapsedTime();
			bellmanBackup();
			long endT = getElapsedTime();
			if (MAIN_DEBUG) debugOutput.println("VI Backup Took:"+(endT - iniT) );
			
			checkLinearAndApprox(valueDD);

			if (MAIN_DEBUG){
				debugOutput.println("Iter:" + curIter+" Complete");
				if (PRINT_DD) debugOutput.println("ValueDD = "+context.getExistNode(valueDD).toString());
				if (PLOT_DD) mdp.doDisplay(valueDD,makeXADDLabel("V",curIter, APPROX_ERROR));
			}
			
			solutionDDList[curIter] = valueDD;
			solutionTimeList[curIter] = getElapsedTime() + (curIter >1? solutionTimeList[curIter-1]:0);
			solutionNodeList[curIter] = context.getNodeCount(valueDD);
			if (mdp.LINEAR_PROBLEM) solutionMaxValueList[curIter] = context.linMaxVal(valueDD);
			if( mdp._initialS != null) solutionInitialSValueList[curIter] = context.evaluate(valueDD, mdp._initialS._hmBoolVars, mdp._initialS._hmContVars);			

			if (ENABLE_EARLY_CONVERGENCE && _prevDD.equals(valueDD) ) {
				if (MAIN_DEBUG) debugOutput.println("\nVI: Converged to solution early,  at iteration "+curIter);
				break;
			}

		}
		flushCaches();	
		finalIter = curIter;
		if (MAIN_DEBUG) debugOutput.println("\nVI: complete at iteration "+finalIter+"\n");
		return finalIter;
	}

	private void checkLinearApprox() {
		if (mdp.LINEAR_PROBLEM && APPROX_PRUNING) {
			long appTime = getElapsedTime();
			valueDD = context.linPruneRel(valueDD, APPROX_ERROR);
			long endTime = getElapsedTime() - appTime;
			debugOutput.println("Approx Finish"+ curIter+ " Iter took: "+appTime+ " pruning: "+endTime);
			//displayGraph(_valueDD, "valPruned-" + _nCurIter+" e"+APPROX_ERROR);
		}
	}

	private void bellmanBackup() {
		// Iterate over each action
		_maxDD = null;
		for (Map.Entry<String,CAction> me : mdp._hmName2Action.entrySet()) {

			// Regress the current value function through each action (finite number of continuous actions)
			int regr = regress(valueDD, me.getValue());
			regr  = context.reduceRound(regr); // Round!
			// Maintain running max over different actions
			_maxDD = (_maxDD == null) ? regr : context.apply(_maxDD, regr, XADD.MAX);
			_maxDD = context.reduceRound(_maxDD); // Round!
			_maxDD = context.reduceLP(_maxDD); // Rely on flag XADD.CHECK_REDUNDANCY

			//Optional post-max approximation 
			// Could be used safely if overall error is being monitored 
			if (mdp.LINEAR_PROBLEM && APPROX_ALWAYS)
				_maxDD = context.linPruneRel(_maxDD, APPROX_ERROR);

			// Error checking and logging
			int canon_max_dd = context.makeCanonical(_maxDD);
			if (_maxDD != canon_max_dd) {
				System.err.println("CAMDP VI ERROR: encountered non-canonical node that should have been canonical... could be rounding, continuing.");
				_maxDD = context.makeCanonical(_maxDD);
			}
			
			flushCaches(Arrays.asList(_maxDD));
			if (REGRESS_DEBUG){
				debugOutput.println("Iter "+curIter+" Action "+me.getValue()._sName+" Maximization Complete.");
				if (PRINT_DD){ 
					debugOutput.println("MaxDD = "+ context.getExistNode(_maxDD));
				}
			}
		}

		valueDD = context.reduceLP(_maxDD);
	}
	
	private IntTriple _contRegrKey = new IntTriple(-1,-1,-1);
	
	/**
	 * Regress a DD through an action
	 **/
	public int regress(int vfun, CAction a) {
		if (REGRESS_DEBUG){
			debugOutput.println("REGRESSING ACTION " + a._sName + " Iter "+ curIter );
		}
		_logStream.println("\n>>> REGRESSING '" + a._sName + "'\n");
		
		// Prime the value function 
		int q = context.substitute(vfun, mdp._hmPrimeSubs); 
		_logStream.println("- Primed value function:\n" + context.getString(q));
		
		// Discount
		q = context.scalarOp(q, mdp._bdDiscount.doubleValue(), XADD.PROD);
		
		if (REGRESS_DEBUG){
			debugOutput.println("Q After discount Prod:");
			if (PRINT_DD) debugOutput.println(context.getExistNode(q).toString());
			if (PLOT_DD) context.showGraph(q, "Q after discount Prod");
		}
		// Add reward *if* it contains primed vars that need to be regressed
		HashSet<String> i_and_ns_vars_in_reward = filterIandNSVars(context.collectVars(a._reward), true, true);
		if (!i_and_ns_vars_in_reward.isEmpty()) {
			q = context.apply(a._reward, q, XADD.SUM); // Add reward to already discounted primed value function
			_logStream.println("- Added in reward pre-marginalization with interm/next state vars: " + i_and_ns_vars_in_reward);
		}
			
		// Derive a variable elimination order for the DBN w.r.t. the reward that puts children before parents
		HashSet<String> vars_to_regress = filterIandNSVars(context.collectVars(q), true, true);
		Graph g = buildDBNDependencyDAG(a, vars_to_regress);
		if (g.hasCycle()) 
			displayCyclesAndExit(g, a);
		
		// Get a valid elimination order (does not minimize tree width, could be optimized)
		List var_order = g.topologicalSort(true);
		_logStream.println("- Elimination order for regression: " + var_order);
		
		// Regress each variable in the topological order
		for (Object o : var_order) {
			String var_to_elim = (String)o;
			if (mdp._hsBoolIVars.contains(var_to_elim) || mdp._hsBoolNSVars.contains(var_to_elim)) {
				q = regressBVars(q, a, var_to_elim);
			} else if (mdp._hsContIVars.contains(var_to_elim) || mdp._hsContNSVars.contains(var_to_elim)) {
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
		// if (mdp.MAINTAIN_POLICY) { 
		//      ... 
		// }
			
		// NOTE: if reward was not added in prior to regression, it must be 
		// added in now...
		if (i_and_ns_vars_in_reward.isEmpty()) {
			q = context.apply(a._reward, q, XADD.SUM);
			_logStream.println("- Added in reward post-marginalization with no interm/next state vars.");
		}
		
		// Optional Display
		if (REGRESS_DEBUG){
			debugOutput.println("Q before Max:");
			if (PLOT_DD){
				mdp.displayGraph(q, "Q-" + a._sName + "-" + a._actionParams + "^" + curIter + "-" + Math.round(1000*APPROX_ERROR));
			}
		}

		// Noise handling
		if (a._noiseVars.size() == 0) {
			// No action params to maximize over
			_logStream.println("- Q^" + curIter + "(" + a._sName + " ):\n" + " No noise parameters to max over, skipping this step.");
		} else {
			// Max in noise constraints and min out each noise parameter in turn
			// NOTE: we can do this because noise parameters can only reference state variables 
			//       (not currently allowing them to condition on intermediate or other noise vars)
			//       hence legal values of noise var determined solely by the factor for that var
			HashSet<String> q_vars = context.collectVars(q);
			for (String nvar : a._noiseVars) {
	
				if (!q_vars.contains(nvar)) {
					_logStream.println("- Skipping noise var '" + nvar + "', which does not occur in q: " + context.collectVars(q));
					continue;
				}
				
				_logStream.println("- Minimizing over noise param '" + nvar + "'");
				int noise_factor = a._hmNoise2DD.get(nvar);
				q = context.apply(noise_factor, q, XADD.MAX); // Max in the noise so illegal states get replace by +inf, otherwise q replaces -inf
				q = minOutVar(q, nvar, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
				_logStream.println("-->: " + context.getString(q));
				
				// Can be computational expensive (max-out) so flush caches if needed
				flushCaches(_maxDD == null? Arrays.asList(q): Arrays.asList(q, _maxDD));
			}
			_logStream.println("- Done noise parameter minimization");
			_logStream.println("- Q^" + curIter + "(" + a._sName + " )" + context.collectVars(q) + "\n" + context.getString(q));
		}		
		
		// Continuous action parameter maximization
		if (a._actionParams.size() == 0) {
			// No action params to maximize over
			_logStream.println("- Q^" + curIter + "(" + a._sName + " ):\n" + " No action parameters to max over, skipping this step.");
		} else 
		{
		
				// Max out each action param in turn
			HashSet<String> q_vars = context.collectVars(q);
			for (int i=0; i < a._actionParams.size(); i++) 
			{
				String avar = a._actionParams.get(i);
				double lb   = a._hmAVar2LB.get(avar);
				double ub   = a._hmAVar2UB.get(avar);
	
				if (!q_vars.contains(avar)) {
					_logStream.println("- Skipping var '" + avar + "': [" + lb + "," + ub + "], which does not occur in q: " + context.collectVars(q));
					continue;
				}
				 
				_logStream.println("- Maxing out action param '" + avar + "': [" + lb + "," + ub + "]");
				q = maxOutVar(q, avar, lb, ub);
				_logStream.println("-->: " + context.getString(q));
				
				// Can be computational expensive (max-out) so flush caches if needed
				flushCaches( (_maxDD == null? Arrays.asList(q): Arrays.asList(q, _maxDD)) /* additional node to save */);
				if (REGRESS_DEBUG){
					if (PLOT_DD){
					mdp.displayGraph(q, "Q-" + a._sName + "-" + a._actionParams + "-End^" + curIter + "-" + Math.round(1000*APPROX_ERROR));
					}
				}
			  }
		
			_logStream.println("- Done action parameter maximization");
			_logStream.println("- Q^" + curIter + "(" + a._sName + " )\n" + context.getString(q));
		}

		if (REGRESS_DEBUG) debugOutput.println("Regress End");
		return q;
	}
	
	public int regressCVars(int q, CAction a, String var) {
		
		// Get cpf for continuous var'
		int var_id = context._cvar2ID.get(var);
		Integer dd_conditional_sub = a._hmVar2DD.get(var);

		_logStream.println("- Integrating out: " + var + "/" + var_id /* + " in\n" + context.getString(dd_conditional_sub)*/);

		// Check cache
		_contRegrKey.set(var_id, dd_conditional_sub, q);
		Integer result = null;
		if ((result = mdp._hmContRegrCache.get(_contRegrKey)) != null)
			return result;
		
		// Perform regression via delta function substitution
		q = context.reduceProcessXADDLeaf(dd_conditional_sub, 
				context.new DeltaFunctionSubstitution(var, q), true);
		
		// Cache result
		_logStream.println("-->: " + context.getString(q));
		mdp._hmContRegrCache.put(new IntTriple(_contRegrKey), q);
		
		return q;		
	}

	public int regressBVars(int q, CAction a, String var) {
		
		// Get cpf for boolean var'
		int var_id = context.getVarIndex( context.new BoolDec(var), false);
		Integer dd_cpf = a._hmVar2DD.get(var);
		
		_logStream.println("- Summing out: " + var + "/" + var_id /*+ " in\n" + context.getString(dd_cpf)*/);
		q = context.apply(q, dd_cpf, XADD.PROD);
		
		// Following is a safer way to marginalize (instead of using opOut
		// based on apply) in the event that two branches of a boolean variable 
		// had equal probability and were collapsed.
		int restrict_high = context.opOut(q, var_id, XADD.RESTRICT_HIGH);
		int restrict_low  = context.opOut(q, var_id, XADD.RESTRICT_LOW);
		q = context.apply(restrict_high, restrict_low, XADD.SUM);

		_logStream.println("-->: " + context.getString(q));

		return q;
	}

	//Memory Management
	public void flushCaches(List<Integer> specialNodes){
		ArrayList<Integer> moreSpecialNodes = new ArrayList<Integer>();
		moreSpecialNodes.addAll(specialNodes);
		moreSpecialNodes.add(valueDD);
		for(int i=1;i<curIter;i++) moreSpecialNodes.add(solutionDDList[i]);
		mdp.flushCaches(moreSpecialNodes);
	}

	//Plot
	public String makeXADDLabel(String xadd, int iter, double approx)
	{
		return  xadd + " Iter"+iter+ (approx > 0? "-approx"+String.format("%03d",Math.round(1000*approx)): "");
	}

	//Results
	public void setupResults(){
		solutionDDList = new int[nIter+1];
		solutionTimeList = new long[nIter+1];
		solutionNodeList = new int[nIter+1];
		solutionInitialSValueList = new double[nIter+1];
		solutionMaxValueList = new double[nIter+1];
	}

	public void saveResults(){
		//Results: NIter, Time, Nodes, InitialS Value.
		for(int i=1; i<=nIter; i++){
		_resultStream.format("%d %d %d %f\n", i, solutionTimeList[i], solutionNodeList[i], (mdp._initialS != null) ? solutionInitialSValueList[i]: "0");
		}
		if (mdp.DISPLAY_3D){
			for(int i=1; i<=nIter; i++){
				save3D(solutionDDList[i], String.format("VI-Value%d", i) );
				saveGraph(solutionDDList[i], String.format("VI-Value%d", i) );
			}
		}
}

	public void printResults() {
		debugOutput.println("Results for Value Iteration: " + finalIter + " iterations:");
		debugOutput.print("Time:"); for(int i=1; i<=finalIter; i++) debugOutput.print(solutionTimeList[i]+" ");debugOutput.println(";");
		debugOutput.print("Nodes:"); for(int i=1; i<=finalIter; i++) debugOutput.print(solutionNodeList[i]+" ");debugOutput.println(";");
		debugOutput.print("Initial S Value:"); for(int i=1; i<=finalIter; i++) debugOutput.print(solutionInitialSValueList[i]+" ");debugOutput.println(";");
	}
}


