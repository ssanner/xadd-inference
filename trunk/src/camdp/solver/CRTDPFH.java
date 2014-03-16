package camdp.solver;
import graph.Graph;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.Map.Entry;

import util.IntTriple;
import util.Pair;
import xadd.ExprLib;
import xadd.XADD;
import xadd.XADD.XADDNode;

import camdp.CAMDP;
import camdp.State;
import camdp.CAction;

public class CRTDPFH extends CAMDPsolver {

	public Integer curTrial = null;
	public Integer nTrials = null;
	Integer[] valueDDList;
	private Integer finalTrial = null;
	
	
	/////////////////////////
	/* Solution Parameters */
	/////////////////////////
	public final int DEFAULT_NTRIALS = 100;
	public final boolean REVERSE_TRIAL = true;
	/* Debugging Flags */
	
	//Local flags	
	private static final boolean TRIAL_DEBUG = true;
	private static final boolean BELLMAN_DEBUG = false;
	private static final boolean REGRESS_DEBUG = false;	
		
	//Result Keeping
	public long[][] updateTimes = null;
	public int[][] updateNodes = null;
	public double[][] updateIniVals = null;
	public PrintStream _resultStreamUp = null;
	//////////////////Methods /////////////////////////////////
	
	public CRTDPFH(CAMDP camdp, int iter){
		initCRTDP(camdp, DEFAULT_NTRIALS, iter);
	}
	
	public CRTDPFH(CAMDP camdp, int nTrials, int iter){
		initCRTDP(camdp, nTrials, iter);
	}
	
	private void initCRTDP(CAMDP camdp, int nt, int ni){
		mdp = camdp;
		context = camdp._context;
		nTrials = nt;
		nIter = ni; 
		valueDDList = new Integer[nIter+1];
		_logStream = camdp._logStream;
		solveMethod = "CRTDPFH";
		makeResultStream();
		setupResults();
	}
	
	private int getValueDD(){
		return valueDDList[nIter];
	}

	////////Main Solver Class ///////////////
	public int solve(){
		if (MAIN_DEBUG) debugOutput.println("Starting CRTDP-FH solution, Horizon = "+nIter+", nTrial = " + nTrials);
		if (mdp._initialS == null){
			System.err.println("Impossible to solve Unknown Initial State MDP with RTDP!");
			return -1;
		}
		
		//Initialize value function to zero assumes ZERO is admissible, need a heuristic evaluation
		for(int i=0; i<=nIter; i++) valueDDList[i] = context.ZERO;
		curTrial = 0;
		boolean solConverge = false;
		
		// Repeat Trials until convergence
		while (curTrial < nTrials && !solConverge) 
		{
			++curTrial;
			resetTimer();
			if (MAIN_DEBUG){
				debugOutput.println("Starting Trial# " + curTrial);
				if (PRINT_DD) debugOutput.println("Initial Value DD = "+getValueDD()+" DD:"+context.getString(getValueDD()));
				if (!TRIAL_DEBUG && PLOT_DD) mdp.doDisplay(getValueDD(), makeXADDLabel("VBeforeT",curTrial));
			}
			
			int []prevDDList = new int[nIter+1];
			for(int i=0;i<=nIter;i++) prevDDList[i] = valueDDList[i]; 
						
			//Perform Trial
			makeTrial(mdp._initialS);

			if (MAIN_DEBUG){
				debugOutput.println("Trial:" + curTrial+" Complete");
				if (PRINT_DD) debugOutput.println("Value after Trial = "+getValueDD()+" DD:" + context.getString(getValueDD()));
				if (!TRIAL_DEBUG && PLOT_DD) mdp.doDisplay(getValueDD(),makeXADDLabel("VAfterT",curTrial));
				debugOutput.println();
			}

			solConverge = earlyConverge(prevDDList);
			flushCaches();
			//////////////////////////////////////////////////////////////////////////
			solutionDDList[curTrial] = getValueDD();
			solutionTimeList[curTrial] = getElapsedTime() + (curTrial >1? solutionTimeList[curTrial-1]:0);
			solutionNodeList[curTrial] = getAllNodes();
			solutionInitialSValueList[curTrial] = getStateVal(mdp._initialS, nIter);			
			if (mdp.LINEAR_PROBLEM) solutionMaxValueList[curTrial] = context.linMaxVal(getValueDD());
		}
		finalTrial = curTrial;
		return finalTrial;
	}
	
	private int getAllNodes(){
		HashSet<XADDNode> nodes = new HashSet<XADDNode>();
		for(int i=0; i <= nIter;i++) context.getExistNode(valueDDList[i]).collectNodes(nodes);
		return nodes.size();
	}
	
	private boolean earlyConverge(int []prevDDList){
		if (!ENABLE_EARLY_CONVERGENCE) return false;
		else{
			for(int i=0; i<= nIter; i++) {
				if (prevDDList[i] != valueDDList[i]) return false; 
			}
			if (MAIN_DEBUG) debugOutput.println("!CRTDP: Converged to solution early, in Trial "+curTrial);
			return true;
		}
	}		
	
	{
//	private void approxValueDD() {
//		if (APPROX_PRUNING) {
//			long appTime = getElapsedTime();
//			_valueDD = _context.linPruneRel(_valueDD, APPROX_ERROR);
//			long pruneTime = getElapsedTime() - appTime;
//			debugOutput.println("ApproxResult: Trial "+ _curTrial+ " Solve time = "+appTime+ ", Prune time = "+pruneTime);
//		}
//	}
	
//	private Integer checkLinearAndApprox(Integer maxDD) {
//		if ( _mdp.LINEAR_PROBLEM && APPROX_ALWAYS)
//			maxDD = _context.linPruneRel(maxDD, APPROX_ERROR);
//		return maxDD;
//	} 
	}
	
	private void makeTrial(State currentS){	
		State []stateList = new State[nIter+1];
		for(int remainHorizon= nIter; remainHorizon > 0; remainHorizon--){
			stateList[remainHorizon] = currentS;
			long iniT = getElapsedTime();
			if (TRIAL_DEBUG){
				debugOutput.println("Trial #"+curTrial+", depth ="+remainHorizon+", "+ currentS.toString());	
				debugOutput.println("State Value = "+ getStateVal(currentS, remainHorizon) );
				//if (PRINT_DD) debugOutput.println("Initial Value = "+valueDDList[remainHorizon]+" DD:\n"+ context.getExistNode(valueDDList[remainHorizon]) +"\n");
				//if (PLOT_DD) mdp.doDisplay(valueDDList[remainHorizon], makeXADDLabel("VBeforeBB",curTrial, remainHorizon));
			}
			ParametrizedAction greedyAction = regionBellmanBackup(currentS, remainHorizon);
			checkDD(valueDDList[remainHorizon]);
			
			long backupT = getElapsedTime();
			if (TRIAL_DEBUG) {
				debugOutput.println("Trial #"+curTrial+" backup "+remainHorizon+" took = "+(backupT-iniT));
				debugOutput.println("State Value After Update= "+ getStateVal(currentS, remainHorizon) );
				if (PRINT_DD) debugOutput.println("Value Trial After Backup = "+ valueDDList[remainHorizon] +" DD:\n"+ context.getExistNode(valueDDList[remainHorizon]) +"\n");
				if (PLOT_DD) mdp.doDisplay(valueDDList[remainHorizon], makeXADDLabel("VAfterBB",curTrial, remainHorizon));
			}
			
			updateTimes[curTrial][remainHorizon] = (backupT-iniT);
			updateNodes[curTrial][remainHorizon] = context.getNodeCount(valueDDList[remainHorizon]);
			updateIniVals[curTrial][remainHorizon] = getStateVal(currentS, remainHorizon);
			
			//Using greedy action, sample next state
			State nextS = sample(currentS, greedyAction);
			long sampleT = getElapsedTime();
			if (TRIAL_DEBUG){
				if (VALIDATION_TEST){
					debugOutput.println("Trial #"+curTrial+" sample took = "+(sampleT-backupT));
					debugOutput.println("State After Sample = "+nextS);
					debugOutput.println("New State Value = "+ getStateVal(currentS,remainHorizon-1) +"\n" );
				}
			}
			currentS = nextS;
			flushCaches();
		}
		//reverse Trial
		if (REVERSE_TRIAL){
			for(int remainHorizon = 1; remainHorizon <= nIter; remainHorizon++){
				currentS = stateList[remainHorizon];
				if (TRIAL_DEBUG){
					debugOutput.println("Reverse Trial #"+curTrial+", depth ="+remainHorizon+", "+ currentS.toString());	
				}
				ParametrizedAction greedyAction = regionBellmanBackup(currentS, remainHorizon);
				checkDD(valueDDList[remainHorizon]);
				if (TRIAL_DEBUG){
					debugOutput.println("New State Value = "+ getStateVal(currentS, remainHorizon) );
					if (PLOT_DD) mdp.doDisplay(valueDDList[remainHorizon], makeXADDLabel("VReverse",curTrial, remainHorizon));
				}
			}
		}
	}

	private Double getStateVal(State currentS, int remainHorizon) {
		return context.evaluate(valueDDList[remainHorizon], currentS._hmBoolVars, currentS._hmContVars);
	}
	
	public ParametrizedAction  regionBellmanBackup(State currS, int h){
		//		 Iterate over each action
		Integer maxDD = null;
		CAction maxAction = null;
		Double currSValue = null;
		HashMap<String, Double> maxParam = new HashMap<String, Double>();
		
		if (BELLMAN_DEBUG){
			debugOutput.println("\nBellman Backup Start Trial "+curTrial+"depth "+h+" State: "+currS);
			debugOutput.println("State Value ="+ getStateVal(currS,h));
		}
		
		for (Map.Entry<String,CAction> me : mdp._hmName2Action.entrySet()) {
				// Regress the current value function through each action (finite number of continuous actions)
				
				int regr = regressRegion(me.getValue(), currS, h);
				regr = context.reduceLP(regr);
				checkDD(regr);
				double value = context.evaluate(regr, currS._hmBoolVars, currS._hmContVars);
				if (BELLMAN_DEBUG){
					debugOutput.println("Regress Action: " + me.getValue()._sName + " Value:" + value);
					if (PRINT_DD) debugOutput.println("Q Regress = "+regr+" DD:\n" + context.getExistNode(regr));
				}
				
				// Maintain running max over different actions
				maxDD = (maxDD == null) ? regr : context.apply(maxDD, regr, XADD.MAX);
				maxDD = context.reduceRound(maxDD); // Round!
				maxDD = context.reduceLP(maxDD); // Rely on flag XADD.CHECK_REDUNDANCY
				checkDD(maxDD);
				if (currSValue == null || value > currSValue){
					maxAction = me.getValue();
					currSValue =value; 
				}
				if (BELLMAN_DEBUG){
					debugOutput.println("Current Max after Regress Action: " + me.getValue()._sName + " Value:" + currSValue);
					if (PLOT_DD) mdp.doDisplay(maxDD, makeXADDLabel("MaxDD-"+me.getValue()._sName, curTrial, h));
				}
		}
		
		if (BELLMAN_DEBUG){
			if (PRINT_DD) debugOutput.println("MAX of Regr = "+maxDD+" DD:\n" + context.getExistNode(maxDD));
		}

		//Min out Ilegal +Inf values, these will be non update regions
		valueDDList[h] = context.apply(maxDD, valueDDList[h], XADD.MIN);		
		valueDDList[h] = context.reduceLP(valueDDList[h]); // Rely on flag XADD.CHECK_REDUNDANCY 
		checkDD(valueDDList[h]);
		
		ParametrizedAction pA = new ParametrizedAction(maxAction, maxParam);
		if (BELLMAN_DEBUG){
			debugOutput.println("Backup End, Greedy Action: " + pA + " Greedy Value:" + currSValue +"\n");
			if (PRINT_DD) debugOutput.println("New vfun ="+valueDDList[h] +" DD:\n" + context.getExistNode(valueDDList[h]));
		}
		return pA;
	}

	/**
	 * Regress a DD through an action
	 **/
	private IntTriple _contRegrKey = new IntTriple(-1,-1,-1);

	public int regressRegion(CAction a, State currS, int depth) {
		// Prime the value function 
		int q = context.substitute(valueDDList[depth-1], mdp._hmPrimeSubs); 
		
		if (REGRESS_DEBUG){
			debugOutput.println("Regressing: "+valueDDList[depth-1]+" at " +currS+" with "+a._sName);
			if (PRINT_DD) debugOutput.println("Next State DD = "+q+":\n"+context.getExistNode(q));
			if (PLOT_DD) mdp.displayGraph(q, makeXADDLabel("Qstart-"+a._sName, curTrial, depth));
		}
		
		// Discount
		q = context.scalarOp(q, mdp._bdDiscount.doubleValue(), XADD.PROD);
		
		// Create a mask for reward function
		int maskReward = context.createPosInfMask(a._reward,currS._hmBoolVars, currS._hmContVars);

		if (REGRESS_DEBUG){
			if (PLOT_DD) mdp.displayGraph(maskReward, makeXADDLabel("MaskRew-"+a._sName, curTrial, depth));
		}
		// Add reward *if* it contains primed vars that need to be regressed
		HashSet<String> i_and_ns_vars_in_reward = filterIandNSVars(context.collectVars(a._reward), true, true);
		
		if (!i_and_ns_vars_in_reward.isEmpty()) {
			q = context.apply(maskReward, q, XADD.SUM); // Add reward to already discounted primed value function
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
				// The topological sort will also add in current state and action variables since they were parents in the network
				_logStream.println("- Ignoring current state or action variable " + var_to_elim + " during elimination");
			}
		}
		
		if (REGRESS_DEBUG){
			if (PLOT_DD) mdp.displayGraph(q, makeXADDLabel("Q after Regree-"+a._sName, curTrial, depth));
		}
		
		if (i_and_ns_vars_in_reward.isEmpty()) {
			q = context.apply(maskReward, q, XADD.SUM);
		}
				
		// Optional Display With just State, Action and Noise Vars
		if (REGRESS_DEBUG){
			if (PRINT_DD) debugOutput.println("Qfunction after removing NS = "+q+" DD:\n"+context.getString(q));
			if (PLOT_DD) mdp.doDisplay(q, "Q-" + a._sName + "-" + a._actionParams + "T" + curTrial +"Rem"+depth);
		}
		
		//Remove Noise Vars
		q = regressNoise(q, a);		
		
		//Remove Action Vars
		q = regressAction(q, a);

		//Constraints Verification
		if (REGRESS_DEBUG){
			debugOutput.println("Finished Regressing: "+a._sName);
		}
		return q;
	}
	
	public int regressCVarsMask(int q, CAction a, String var, State currS) {
		
		// Get cpf for continuous var'
		int var_id = context._cvar2ID.get(var);
		Integer dd_conditional_sub = a._hmVar2DD.get(var);

		//MaskTransitions not from current state
		dd_conditional_sub = context.createPosInfMask(dd_conditional_sub, currS._hmBoolVars, currS._hmContVars);		
		
		_logStream.println("- Integrating out: " + var + "/" + var_id /* + " in\n" + _context.getString(dd_conditional_sub)*/);

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

	public State sample(State currentS, ParametrizedAction parA){
		HashSet<String> AllNSVars = new HashSet<String>();
		AllNSVars.addAll(mdp._hsBoolNSVars);
		AllNSVars.addAll(mdp._hsContNSVars);
		Graph g = buildDBNDependencyDAG(parA._action, AllNSVars);
		if (g.hasCycle()) 
			displayCyclesAndExit(g, parA._action);
		
		List var_order = g.topologicalSort(false);
		
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
				//debugOutput.println("- Ignoring Non-NextState variable " + var + " during sampling");
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
			if (!var.endsWith("'")) debugOutput.println("Invalid (unprimed) Next State Var: "+ var);
			newS._hmBoolVars.put(var.substring(0, var.length()-1), e.getValue());
		}
		for(Entry<String, Double> e: nextS._hmContVars.entrySet()){
			String var = e.getKey();
			if (!var.endsWith("'")) debugOutput.println("Invalid (unprimed) Next State Var: "+ var);
			newS._hmContVars.put(var.substring(0, var.length()-1), e.getValue());
		}
		return newS;
	}
	
	// Memory management///
	public void flushCaches(List<Integer> specialNodes){
		ArrayList<Integer> moreSpecialNodes = new ArrayList<Integer>();
		moreSpecialNodes.addAll(specialNodes);
		for(int i=1;i<curTrial;i++) moreSpecialNodes.add(solutionDDList[i]);
		for(int i=0;i<=nIter;i++) moreSpecialNodes.add(valueDDList[i]);
		mdp.flushCaches(moreSpecialNodes);
	}
	
	//Plotting
    public String makeXADDLabel(String xadd, int trial, int remH)
	{
		return  xadd+" Trial"+trial+" remainH "+remH+(APPROX_ERROR > 0? "-approx"+String.format("%03d",Math.round(1000*APPROX_ERROR)): "");
	}

    public String makeXADDLabel(String xadd, int trial)
	{
		return  xadd+" Trial"+trial+(APPROX_ERROR > 0? "-approx"+String.format("%03d",Math.round(1000*APPROX_ERROR)): "");
	}
    
    //Results
    public void makeResultStream(){
    	super.makeResultStream();
		int filenamestart = mdp._problemFile.lastIndexOf('/');
		String filename = mdp._problemFile.substring(filenamestart,mdp._problemFile.length()-5);
		String problemType = mdp.CONTINUOUS_ACTIONS? "/contact":"/discact"; 
		String dir = RESULTS_DIR + problemType + filename;
		
		//System.out.println("testing filename:" + dir + "/" + _solveMethod + ".rslt");
    	try{
    		_resultStreamUp = new PrintStream(new FileOutputStream(dir+"/"+solveMethod+".up"));
    	}
    	catch (FileNotFoundException e){
    		System.err.println("Couldn't create result Stream for: "+dir + "/" + solveMethod + " update\nException:"+e);
    	}
    }
	public void setupResults(){
		solutionDDList = new int[nTrials+1];
		solutionTimeList = new long[nTrials+1];
		solutionNodeList = new int[nTrials+1];
		solutionInitialSValueList = new double[nTrials+1];
		solutionMaxValueList = new double[nTrials+1];
		updateTimes = new long [nTrials+1][nIter+1];
		updateNodes = new int [nTrials+1][nIter+1];
		updateIniVals = new double [nTrials+1][nIter+1];
	}
	public void saveResults(){
		//Results per Trial: NIter, Time, Nodes, InitialS Value.
		for(int i=1; i<=finalTrial; i++){
			_resultStream.format("%d %d %d %f\n", i, solutionTimeList[i], solutionNodeList[i], (mdp._initialS != null) ? solutionInitialSValueList[i]: "0");
		}
		//Results per Update
		for(int i=1; i<= finalTrial; i++){
			for(int j=nIter; j>0; j--)
				_resultStreamUp.format("%d %d %d %d %f\n", i, j, updateTimes[i][j], updateNodes[i][j], updateIniVals[i][j]);
		}
		if (mdp.DISPLAY_3D){
			for(int i=1; i<=nIter; i++){
				save3D(valueDDList[i], String.format("CRTDPFH-Value%d", i) );
				saveGraph(valueDDList[i], String.format("CRTDPFH-Value%d", i) );
			}
		}
	}
	public void printResults() {
		debugOutput.println("Results for CRTDP-FH: " + finalTrial + " trials of Depth "+nIter);
		debugOutput.print("Time:"); long sumTime = 0;
		for(int i=1;i<=finalTrial;i++){ 
			debugOutput.println();
			for(int j=nIter; j>0; j--) {
				debugOutput.print(updateTimes[i][j]+" ");
			}
		} 	debugOutput.println(";");
		debugOutput.print("Nodes:");
		for(int i=1;i<=finalTrial;i++){ 
			debugOutput.println();
			for(int j=nIter; j>0; j--) debugOutput.print(updateNodes[i][j]+" ");
		} 	debugOutput.println(";");
		debugOutput.print("Current State Value:");
		for(int i=1;i<=finalTrial;i++){ 
			debugOutput.println();
			for(int j=nIter; j>0; j--) debugOutput.print(updateIniVals[i][j]+" ");
		} 	debugOutput.println(";");
		debugOutput.print("Time per Trial:"); for(int i=1; i<=finalTrial; i++) debugOutput.print(solutionTimeList[i]+" ");debugOutput.println(";");
		debugOutput.print("Nodes per Trial:"); for(int i=1; i<=finalTrial; i++) debugOutput.print(solutionNodeList[i]+" ");debugOutput.println(";");
		debugOutput.print("Initial S Value:"); for(int i=1; i<=finalTrial; i++) debugOutput.print(solutionInitialSValueList[i]+" ");debugOutput.println(";");
	}
}
