package camdp.solver;

import graph.Graph;

import java.io.PrintStream;
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

	//Trial Keeping Variables
	public Integer curTrial = null;
	public Integer nTrials = null;
	public Integer finalTrial = null;
	
	/////////////////////////
	/* Solution Parameters */
	/////////////////////////
	public final int DEFAULT_NTRIALS = 100;
	private static final int DEFAULT_DEPTH = 5;
	/* Debugging Flags */
	
	//Local flags:
	private static final boolean MAIN_DEBUG = true;
	private static final boolean DEBUG_TRIAL = false;
	private static final boolean BELLMAN_DEBUG = false;
	private static final boolean REGRESS_DEBUG = false;
	private static final boolean SAMPLING_DEBUG = false;
	
	//Debug Format flags
	private static final boolean PLOT_DD = false;
	private static final boolean PRINT_DD = false;
	
	//////////////////Methods /////////////////////////////////
	
	public CRTDP(CAMDP camdp){
		initCRTDP(camdp, DEFAULT_NTRIALS, DEFAULT_DEPTH);
	}
	
	public CRTDP(CAMDP camdp, int nt, int ni){
		initCRTDP(camdp, nt, ni);
	}
	
	private void initCRTDP(CAMDP camdp, int nt, int ni){
		mdp = camdp;
		context = camdp._context;
		nTrials = nt;
		nIter = ni;
		solveMethod = "CRTDP";
		makeResultStream();
		setupResults();
	}
	
	////////Main Solver Class ///////////////
	public int solve(){
		if (MAIN_DEBUG) System.out.println("Starting CRTDP solution, Ntrials "+ nTrials +" , Horizon = " + nIter);
		if (mdp._initialS == null){
			System.err.println("Impossible to solve Unknown Initial State MDP with RTDP!");
			return -1;
		}
		
		// Initialize Solution
		boolean solConverge = false;
		Integer prevDD = null;
		valueDD = context.ZERO;		//TODO: Heuristic Function
		curTrial = 0;
		
		// Repeat Trials until convergence
		while (curTrial < nTrials && !solConverge) 
		{
			++curTrial;
			resetTimer();
			if (MAIN_DEBUG){
				System.out.println("Starting Trial# " + curTrial +" with max depth = " + nIter);
				if (PRINT_DD) System.out.println("Initial Value DD = "+valueDD+" DD:"+context.getString(valueDD));
			}
	
			// Save Previous diagram
			prevDD = valueDD;
			
			//Perform Trial
			makeTrial(mdp._initialS);

			if (MAIN_DEBUG){
				System.out.println("Trial:" + curTrial+" Complete");
				if (PRINT_DD) System.out.println("Value after Trial = "+valueDD+" DD:" + context.getString(valueDD));
				if (PLOT_DD) mdp.doDisplay(valueDD,makeXADDLabel("V",curTrial, nIter, APPROX_ERROR));
			}
			
			//Optional post-max approximation - Could be used safely if overall error is being monitored 
			valueDD = checkLinearAndApprox(valueDD);
			checkCanon(valueDD);
			approxValueDD();
			flushCaches();

			// Results 
			solutionDDList[curTrial] = valueDD;
			solutionTimeList[curTrial] = getElapsedTime();
			solutionNodeList[curTrial] = context.getNodeCount(valueDD);
			solutionInitialSValueList[curTrial] = context.evaluate(valueDD, mdp._initialS._hmBoolVars, mdp._initialS._hmContVars);
			if (mdp.LINEAR_PROBLEM) solutionMaxValueList[curTrial] = context.linMaxVal(valueDD);
			
			// Verify Convergence
			if (prevDD.equals(valueDD) ) {
				if (MAIN_DEBUG) System.out.println("! CRTDP: Converged to solution early, in Trial "+curTrial);
				finalTrial = curTrial;
				solConverge = true;
			}		
		}
		flushCaches();	
		finalTrial = curTrial;
		return finalTrial;
	}
	
	private void approxValueDD() {
		if (APPROX_PRUNING) {
			long appTime = getElapsedTime();
			valueDD = context.linPruneRel(valueDD, APPROX_ERROR);
			long pruneTime = getElapsedTime() - appTime;
			System.out.println("ApproxResult: Trial "+ curTrial+ " Solve time = "+appTime+ ", Prune time = "+pruneTime);
		}
	}

	private Integer checkCanon(Integer maxDD) {
		// Error checking and logging
		int canon_max_dd = context.makeCanonical(maxDD);
		if (maxDD != canon_max_dd) {
			System.err.println("CAMDP VI ERROR: encountered non-canonical node that should have been canonical... could be rounding, continuing.");
			context.exportXADDToFile(maxDD, "ERRORdiagram1OriginalXADD.xadd");
			context.exportXADDToFile(canon_max_dd, "ERRORdiagram2makeCanonical.xadd");
			context.getGraph(maxDD).launchViewer("ERROR diagram 1: original maxDD");
			context.getGraph(canon_max_dd).launchViewer("ERROR diagram 2: makeCanonical(maxDD)");
		}
		return maxDD;
	}


	private void makeTrial(State currentS){
		int depth = nIter;
		while (depth > 0){			
			long trialStart = getElapsedTime();
			if (DEBUG_TRIAL){
				System.out.println("Trial #"+curTrial+", depth ="+depth+", "+ currentS.toString());	
				System.out.println("State Value = "+ context.evaluate(valueDD, currentS._hmBoolVars, currentS._hmContVars) );
				if (PRINT_DD) System.out.println("Initial Value = "+valueDD+" DD:\n"+ context.getExistNode(valueDD) +"\n");
			}
			ParametrizedAction greedyAction = regionBellmanBackup(currentS);
			long trialMiddle = getElapsedTime();
							
			//Using greedy action, sample next state
			State nextS = sample(currentS, greedyAction);
			long trialEnd = getElapsedTime();
			if (DEBUG_TRIAL){
				System.out.println("Trial #"+curTrial+" Backup took "+(trialMiddle-trialStart)+" Sample took "+(trialEnd-trialMiddle));
				System.out.println("State After Sample = "+nextS);
				System.out.println("New State Value = "+ context.evaluate(valueDD, currentS._hmBoolVars, currentS._hmContVars) +"\n" );
				if (PRINT_DD) System.out.println("Value After Backup = "+ valueDD +" DD:\n"+ context.getExistNode(valueDD) +"\n");
				if (PLOT_DD) mdp.doDisplay(valueDD,makeXADDLabel("V", curTrial, depth, APPROX_ERROR));
			}
			currentS = nextS;
			depth = depth - 1;
		}		
	}
	
	
	public ParametrizedAction  regionBellmanBackup(State currS){
		//		 Iterate over each action
		Integer maxDD = null;
		CAction maxAction = null;
		Double currSValue = null;
		HashMap<String, Double> maxParam = new HashMap<String, Double>();
		
		if (BELLMAN_DEBUG){
			System.out.println("Bellman Backup Start: "+currS);
			if (PRINT_DD) System.out.println("Original ValueDD = "+valueDD+"DD:\n"+ context.getExistNode(valueDD));
		}
		
		for (Map.Entry<String,CAction> me : mdp._hmName2Action.entrySet()) {
				long regrIni = getElapsedTime();
				// Regress the current value function through each action (finite number of continuous actions)
				int regr = regressRegion(valueDD, me.getValue(), currS);
				if (regr != context.reduceRound(regr) ) System.out.println("Still Rouding issues on Regress!"); // Round!
				regr = context.reduceLP(regr);
				
				long regrMiddle = getElapsedTime();
				// Maintain running max over different actions
				maxDD = (maxDD == null) ? regr : context.apply(maxDD, regr, XADD.MAX);
				maxDD = context.reduceRound(maxDD); // Round!
				maxDD = context.reduceLP(maxDD); // Rely on flag XADD.CHECK_REDUNDANCY
				double value = context.evaluate(regr, currS._hmBoolVars, currS._hmContVars);
				
				long regrEnd = getElapsedTime();
				if (BELLMAN_DEBUG){
					System.out.println("Regress Action: " + me.getValue()._sName + " Value:" + value);
					System.out.println("Regress took "+(regrMiddle-regrIni)+"s Maxim took "+(regrEnd-regrMiddle));
					if (PRINT_DD) System.out.println("Q Regress = "+regr+" DD:\n" + context.getExistNode(regr));
				}
				if (currSValue == null || value > currSValue){
					maxAction = me.getValue();
					currSValue =value; 
				}
		}

		//Min out Illegal +Inf values, these will be non update regions
		long regrMinS = getElapsedTime();
		valueDD = context.apply(maxDD, valueDD, XADD.MIN);
		long regrMinE = getElapsedTime();
		if (BELLMAN_DEBUG){
			System.out.println("Final minimization took"+(regrMinE-regrMinS));
			if (PRINT_DD) System.out.println("MAX of Regr = "+maxDD+" DD:\n" + context.getExistNode(maxDD));
			if (PRINT_DD) System.out.println("\n New Vfun Before Reduce ="+valueDD+" DD:\n" + context.getExistNode(valueDD));
		}
		
		valueDD = context.reduceLP(valueDD); // Rely on flag XADD.CHECK_REDUNDANCY 
		
		ParametrizedAction pA = new ParametrizedAction(maxAction, maxParam);
		if (BELLMAN_DEBUG){
			System.out.println("\nBackup End, Greedy Action: " + pA + " Greedy Value:" + currSValue +"\n");
			if (PRINT_DD) System.out.println("New vfun ="+valueDD +" DD:\n" + context.getExistNode(valueDD));
		}
		return pA;
	}
	
	private IntTriple _contRegrKey = new IntTriple(-1,-1,-1);

	public int regressRegion(int vfun, CAction a, State currS) {
		// Prime the value function 
		int q = context.substitute(vfun, mdp._hmPrimeSubs); 
		
		if (REGRESS_DEBUG){
			System.out.println("Regressing: "+vfun+" at " +currS+" with "+a._sName);
			if (PLOT_DD) mdp.doDisplay(vfun, makeXADDLabel("Qstart-"+a._sName, curTrial, 0, APPROX_ERROR));
		}
		
		// Discount
		q = context.scalarOp(q, mdp._bdDiscount.doubleValue(), XADD.PROD);
		
		int maskReward = context.createPosInfMask(a._reward,currS._hmBoolVars, currS._hmContVars);

		// Add reward *if* it contains primed vars that need to be regressed
		HashSet<String> i_and_ns_vars_in_reward = filterIandNSVars(context.collectVars(a._reward), true, true);
		if (!i_and_ns_vars_in_reward.isEmpty()) {
			q = context.apply(maskReward, q, XADD.SUM); // Add reward to already discounted primed value function
			_logStream.println("- Added in reward pre-marginalization with interm/next state vars: " + i_and_ns_vars_in_reward);
		}
			
		// Derive a variable elimination order for the DBN w.r.t. the reward that puts children before parents
		HashSet<String> vars_to_regress = filterIandNSVars(context.collectVars(q), true, true);
		Graph g = buildDBNDependencyDAG(a, vars_to_regress);
		if (g.hasCycle()) 
			displayCyclesAndExit(g, a);
		
		// Get a valid elimination order (does not minimize tree width, could be optimized)
		List var_order = g.topologicalSort(true);
		
		// Regress each variable in the topological order
		for (Object o : var_order) {
			String var_to_elim = (String)o;
			if (mdp._hsBoolIVars.contains(var_to_elim) || mdp._hsBoolNSVars.contains(var_to_elim)) {
				q = regressBVarsMask(q, a, var_to_elim, currS);
			} else if (mdp._hsContIVars.contains(var_to_elim) || mdp._hsContNSVars.contains(var_to_elim)) {
				q = regressCVarsMask(q, a, var_to_elim, currS);
			} else {
				//Ignore Var
			}
		}
		
		if (i_and_ns_vars_in_reward.isEmpty()) {
			q = context.apply(maskReward, q, XADD.SUM);
		}
				
		// Optional Display With just State, Action and Noise Vars
		if (REGRESS_DEBUG){
			System.out.println("Qfunction after removing NS = "+q+"DD:\n"+context.getString(q));
			_logStream.println("- Q^" + "(" + a._sName + ", " + a._actionParams + " )\n" + context.getString(q));
			if (PLOT_DD) mdp.displayGraph(q, "Q-" + a._sName + "-" + a._actionParams + "^" + curTrial + "-" + Math.round(1000*APPROX_ERROR));
		}
		
		//Remove Noise Vars
		q = regressNoise(q, a);		
		
		//Remove Action Vars
		q = regressAction(q, a);

		//Constraints Verification
		if (REGRESS_DEBUG){
			System.out.println("Finished Regressing: "+a._sName);
			if (PLOT_DD) mdp.doDisplay(q, makeXADDLabel("Qend-"+a._sName, curTrial, 0, APPROX_ERROR));
		}
		return q;
	}


	
	public int regressCVarsMask(int q, CAction a, String var, State currS) {
		
		// Get cpf for continuous var'
		int var_id = context._cvar2ID.get(var);
		Integer dd_conditional_sub = a._hmVar2DD.get(var);

		//MaskTransitions not from current state
		dd_conditional_sub = context.createPosInfMask(dd_conditional_sub, currS._hmBoolVars, currS._hmContVars);		
		
		// Check cache
		_contRegrKey.set(var_id, dd_conditional_sub, q);
		Integer result = null;
		if ((result = mdp._hmContRegrCache.get(_contRegrKey)) != null)
			return result;
		
		// Perform regression via delta function substitution
		q = context.reduceProcessXADDLeaf(dd_conditional_sub, 
				context.new DeltaFunctionSubstitution(var, q), true);
		
		// Cache result
		mdp._hmContRegrCache.put(new IntTriple(_contRegrKey), q);
		
		return q;		
	}

	public int regressBVarsMask(int q, CAction a, String var, State currS) {
		
		// Get cpf for boolean var'
		int var_id = context.getVarIndex( context.new BoolDec(var), false);
		Integer dd_cpf = a._hmVar2DD.get(var);
		
		//MaskTransitions not from current state
		dd_cpf = context.createPosInfMask(dd_cpf, currS._hmBoolVars, currS._hmContVars);
		_logStream.println("- Summing out: " + var + "/" + var_id /*+ " in\n" + _context.getString(dd_cpf)*/);
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

	
	// Sampling
	
	public State sample(State currentS, ParametrizedAction parA){
		HashSet<String> AllNSVars = new HashSet<String>();
		AllNSVars.addAll(mdp._hsBoolNSVars);
		AllNSVars.addAll(mdp._hsContNSVars);
		Graph g = buildDBNDependencyDAG(parA._action, AllNSVars);
		if (g.hasCycle()) 
			displayCyclesAndExit(g, parA._action);
		
		List var_order = g.topologicalSort(true);
		
		// Sample each variable in the topological order
		State nextS = new State();
		for (Object o : var_order) {
			String var = (String)o;
			Integer dd_cpf = parA._action._hmVar2DD.get(var);
			
			if (mdp._hsBoolNSVars.contains(var)) {
				Boolean val = sampleBVar(currentS, nextS, var, dd_cpf, parA._params);
				nextS._hmBoolVars.put(var, val);
			} else if (mdp._hsContNSVars.contains(var)) {
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
		double trueProb = context.evaluate(bVarDD, bool_assign, makeContAssign(prevS,nextS, aParams) );
		return (Math.random() < trueProb) ? true: false;
	}
	
	public Double sampleCVar(State prevS, State nextS, Integer cVarDD, HashMap<String, Double> aParams){
		return context.evaluate(cVarDD, makeBoolAssign(prevS,nextS), makeContAssign(prevS,nextS, aParams));
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
	
    
	//Memory Management
	public void flushCaches(List<Integer> specialNodes){
		ArrayList<Integer> moreSpecialNodes = new ArrayList<Integer>();
		moreSpecialNodes.addAll(specialNodes);
		moreSpecialNodes.add(valueDD);
		for(int i=1;i<curTrial;i++) moreSpecialNodes.add(solutionDDList[i]);
		mdp.flushCaches(moreSpecialNodes);
	}
	
	//Plot
	public String makeXADDLabel(String xadd, int trial, int depth, double approx)
	{
		return  xadd+" Trial"+trial+" Depth "+depth+(approx > 0? "-approx"+String.format("%03d",Math.round(1000*approx)): "");
	}

    ///////////// Results //////////////////////////////////
	public void setupResults(){
		solutionDDList = new int[nTrials+1];
		solutionTimeList = new long[nTrials+1];
		solutionNodeList = new int[nTrials+1];
		solutionInitialSValueList = new double[nTrials+1];
		solutionMaxValueList = new double[nTrials+1];
	}

	public void saveResults(){
		//Results: NIter, Time, Nodes, InitialS Value.
		for(int i=1; i<=finalTrial; i++){
		_resultStream.format("%d %d %d %f\n", i, solutionTimeList[i], solutionNodeList[i], (mdp._initialS != null) ? solutionInitialSValueList[i]: "0");
	}
}
    
    public void printResults() {
		System.out.println("Results for CRTDP: " + (finalTrial) + " trials:");
		System.out.print("Time:"); for(int i=1; i<=finalTrial; i++) System.out.print(solutionTimeList[i]+" ");System.out.println(";");
		System.out.print("Nodes:"); for(int i=1; i<=finalTrial; i++) System.out.print(solutionNodeList[i]+" ");System.out.println(";");
		System.out.print("Initial S Value:"); for(int i=1; i<=finalTrial; i++) System.out.print(solutionInitialSValueList[i]+" ");System.out.println(";");
		System.out.print("Total Time: "); long t=0; for(int i=1; i<=finalTrial; i++) t += solutionTimeList[i]; System.out.println(t+";");
	}
    
}