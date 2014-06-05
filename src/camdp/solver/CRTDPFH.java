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
	public static final int DEFAULT_NTRIALS = 100;
	private static final boolean REVERSE_TRIAL = true;
	private static final boolean SECOND_MASK = true;
	private static final boolean THIRD_MASK = false;
	private static final boolean IMMEDIATEREWARD = true;
	/* Debugging Flags */
	
	//Local Debug flags	
	private static final boolean BELLMAN_DEBUG = false;
	private static boolean REGRESS_DEBUG = false;
	private static boolean REGRESS_VAR_DEBUG = false;
	private static final boolean DD_BEFORE_TRIAL = false;
	private static final boolean USE_CONT_REGR_CACHE = false;
	

	
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
		solveMethod = "CRTDP";
		makeResultStream();
		setupResults();
	}
	
	private int getValueDD(){
		return valueDDList[nIter];
	}

	////////Main Solver Class ///////////////
	public int solve(){
		if (MAIN_DEBUG) debugOutput.println("Starting "+solveMethod+" solution, Horizon = "+nIter+", nTrial = " + nTrials);
		if (mdp._initialS == null){
			System.err.println("Impossible to solve Unknown Initial State MDP with RTDP!");
			return -1;
		}
		
		//Initialize value function to zero assumes ZERO is admissible, need a heuristic evaluation
		//for(int i=0; i<=nIter; i++) valueDDList[i] = context.ZERO;
		initializeDDList();
		
		curTrial = 0;
		boolean solConverge = false;
		
		// Repeat Trials until convergence
		while (curTrial < nTrials && !solConverge) 
		{
			++curTrial;
			resetTimer();
			if (MAIN_DEBUG){
				debugOutput.println("Starting Trial# " + curTrial);
				//if (PRINT_DD) debugOutput.println("Initial Value DD = "+getValueDD()+" DD:"+context.getString(getValueDD()));
				//if (!DEEP_DEBUG && PLOT_DD) mdp.doDisplay(getValueDD(), makeXADDLabel("VBeforeT",curTrial));
			}
			
			int []prevDDList = new int[nIter+1];
			for(int i=0;i<=nIter;i++) prevDDList[i] = valueDDList[i]; 
						
			//Perform Trial
			makeTrial(mdp._initialS);

			solConverge = earlyConverge(prevDDList);
			flushCaches();
			//////////////////////////////////////////////////////////////////////////
			solutionDDList[curTrial] = getValueDD();
			solutionTimeList[curTrial] = getElapsedTime() + (curTrial >1? solutionTimeList[curTrial-1]:0);
			solutionNodeList[curTrial] = getAllNodes();
			solutionInitialSValueList[curTrial] = getStateVal(mdp._initialS, nIter);			
			if (mdp.LINEAR_PROBLEM) solutionMaxValueList[curTrial] = context.linMaxVal(getValueDD());
			
			if (MAIN_DEBUG){
				debugOutput.println("Trial:" + curTrial+" Complete");
				debugOutput.println("Value DD:"+getValueDD()+" Nodes= "+solutionNodeList[curTrial]+" Time ="+solutionTimeList[curTrial]);
				debugOutput.println("Initial State Value = "+solutionInitialSValueList[curTrial]);
				if (PRINT_DD) debugOutput.println("Value after Trial = "+getValueDD()+" DD:" + context.getString(getValueDD()));
				if (!DEEP_DEBUG && PLOT_DD) mdp.doDisplay(getValueDD(),makeXADDLabel("VAfterT",curTrial));
				debugOutput.println();
			}
			
		}
		finalTrial = curTrial;
		return finalTrial;
	}

	private void initializeDDList(){
		int maxRewDD = context.createDoubleNode(mdp.maxImediateReward);
		valueDDList[0] = context.ZERO;
		if (IMMEDIATEREWARD){
			for(int i=1; i<=nIter; i++) valueDDList[i] = context.createDoubleNode(i*mdp.maxImediateReward);
		}
		else{
			for(int i=1; i<=nIter; i++) valueDDList[i] = maxRewDD;
		}
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
			if (DEEP_DEBUG){
				debugOutput.println("Trial #"+curTrial+", depth ="+remainHorizon+", "+ currentS.toString());	
				debugOutput.println("State Value = "+ getStateVal(currentS, remainHorizon) );
				if (PRINT_DD && DD_BEFORE_TRIAL) debugOutput.println("Initial Value = "+valueDDList[remainHorizon]+" DD:\n"+ context.getExistNode(valueDDList[remainHorizon]) +"\n");
				if (PLOT_DD && DD_BEFORE_TRIAL) mdp.doDisplay(valueDDList[remainHorizon], makeXADDLabel("VBeforeBB",curTrial, remainHorizon));
			}
			ParametrizedAction greedyAction = regionBellmanBackup(currentS, remainHorizon);
			
			long backupT = getElapsedTime();
			if (DEEP_DEBUG) {
				debugOutput.println("Trial #"+curTrial+" backup "+remainHorizon+" took = "+(backupT-iniT));
				debugOutput.println("Greedy Action = "+greedyAction+"State Value After Update= "+ getStateVal(currentS, remainHorizon) );
				if (PRINT_DD) debugOutput.println("Value Trial After Backup = "+ valueDDList[remainHorizon] +" DD:\n"+ context.getExistNode(valueDDList[remainHorizon]) +"\n");
				if (PLOT_DD) mdp.doDisplay(valueDDList[remainHorizon], makeXADDLabel("VAfterBB",curTrial, remainHorizon));
			}
			
			updateTimes[curTrial][remainHorizon] = (backupT-iniT);
			updateNodes[curTrial][remainHorizon] = context.getNodeCount(valueDDList[remainHorizon]);
			updateIniVals[curTrial][remainHorizon] = getStateVal(currentS, remainHorizon);
			
			//Using greedy action, sample next state
			State nextS = sample(currentS, greedyAction);
			long sampleT = getElapsedTime();
			if (DEEP_DEBUG){
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
		if (REVERSE_TRIAL){ //no need to repeat update on last state, revert from 2
			for(int remainHorizon = 2; remainHorizon <= nIter; remainHorizon++){
				currentS = stateList[remainHorizon];
				if (DEEP_DEBUG){
					debugOutput.println("Reverse Trial #"+curTrial+", depth ="+remainHorizon+", "+ currentS.toString());	
				}
				ParametrizedAction greedyAction = regionBellmanBackup(currentS, remainHorizon);
				if (DEEP_DEBUG){
					debugOutput.println("Greedy Action = "+greedyAction+" New State Value = "+ getStateVal(currentS, remainHorizon) );
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
			if (PLOT_DD) mdp.doDisplay(valueDDList[h], makeXADDLabel("IniVDD-", curTrial, h));
		}
		
		for (Map.Entry<String,CAction> me : mdp._hmName2Action.entrySet()) {
				// Regress the current value function through each action (finite number of continuous actions)
				
				int regr = regressRegion(me.getValue(), currS, h);

				double value = context.evaluate(regr, currS._hmBoolVars, currS._hmContVars);
				if (value == Double.POSITIVE_INFINITY || value == Double.NEGATIVE_INFINITY || value == Double.NaN){
					System.err.println("Regression Fail: Initial State value is Pos INF, Neg INF or NaN in Q value function:");
					System.err.println("State is "+currS);
					if (!CAMDP.SILENCE_ERRORS_PLOTS) mdp.displayGraph(regr, makeXADDLabel("Inf Q: Regr"+me.getValue()._sName+" DD", curTrial, h));
					context.evaluate(regr, currS._hmBoolVars, currS._hmContVars);
					REGRESS_DEBUG = true;
					REGRESS_VAR_DEBUG = true;
					regressRegion(me.getValue(), currS, h);
				}
				if (BELLMAN_DEBUG){
					debugOutput.println("Regress Action: " + me.getValue()._sName + " Value:" + value);
					if (PRINT_DD) debugOutput.println("Q_a = "+regr+" DD:\n" + context.getExistNode(regr));
					if (PLOT_DD) mdp.doDisplay(regr, "Final Q"+me.getValue()._sName+"T"+curTrial+"H"+h);
				}
				
				// Maintain running max over different actions
				maxDD = (maxDD == null) ? regr : context.apply(maxDD, regr, XADD.MAX);
				maxDD = mdp.standardizeDD(maxDD); // Round!
				
				if (currSValue == null || value > currSValue){
					maxAction = me.getValue();
					currSValue =value; 
				}
				if ( Math.abs(currSValue - context.evaluate(maxDD, currS._hmBoolVars, currS._hmContVars)) > STATE_PRECISION){
					System.err.println("Maxim fail, greedy value "+ currSValue+" different from value "+context.evaluate(maxDD, currS._hmBoolVars, currS._hmContVars)+" for "+currS);
					if (!CAMDP.SILENCE_ERRORS_PLOTS) {
						mdp.displayGraph(regr, makeXADDLabel("Regr"+me.getValue()._sName+" DD", curTrial, h));
						mdp.displayGraph(maxDD, makeXADDLabel("BB Max_DD", curTrial, h));
					}
				}
				if (BELLMAN_DEBUG){
					debugOutput.println("Current Max after Regress Action: " + me.getValue()._sName + " Value:" + currSValue);
					if (PRINT_DD) debugOutput.println("Max of Qs = "+maxDD+" DD: " + context.getExistNode(maxDD));
					if (PLOT_DD) mdp.doDisplay(maxDD, makeXADDLabel("MaxDD-"+me.getValue()._sName, curTrial, h));
				}
	
		}
		
		if (BELLMAN_DEBUG){
			if (PRINT_DD) debugOutput.println("MAX of Regr = "+maxDD+" DD:\n" + context.getExistNode(maxDD));
			if (PRINT_DD) debugOutput.println("Previous Value = "+valueDDList[h]+" DD:\n" + context.getExistNode(valueDDList[h]));
		}
		
		//Min out Ilegal +Inf values, these will be non update regions
		valueDDList[h] = context.apply(maxDD, valueDDList[h], XADD.MIN);		
		valueDDList[h] = mdp.standardizeDD(valueDDList[h]); 
		
		ParametrizedAction pA = new ParametrizedAction(maxAction, maxParam);
		
		if ( Math.abs(currSValue - context.evaluate(valueDDList[h], currS._hmBoolVars, currS._hmContVars)) > STATE_PRECISION){
			System.err.println("Backup fail, greedy value "+ currSValue+" different from value "+context.evaluate(valueDDList[h], currS._hmBoolVars, currS._hmContVars)+" for "+currS);
			System.err.println("Max DD value is " + context.evaluate(maxDD, currS._hmBoolVars, currS._hmContVars));
			if (!CAMDP.SILENCE_ERRORS_PLOTS) {
				mdp.doDisplay(maxDD, makeXADDLabel("BB Bug! MaxDD", curTrial, h));
				mdp.doDisplay(valueDDList[h], makeXADDLabel("BB Bug! V_DD", curTrial, h));
			}
			
			if (PRINT_DD) debugOutput.println("MAX DD = "+maxDD+" DD:\n" + context.getExistNode(maxDD));
			if (PRINT_DD) debugOutput.println("Value DD= "+valueDDList[h]+" DD:\n" + context.getExistNode(valueDDList[h]));
			valueDDList[h] = context.apply(maxDD, valueDDList[h], XADD.MIN);
		}
		if (BELLMAN_DEBUG){
			debugOutput.println("Backup End, Greedy Action: " + pA + " Greedy Value:" + currSValue +"\n");			
			if (PRINT_DD) debugOutput.println("New vfun ="+valueDDList[h] +" DD:\n" + context.getExistNode(valueDDList[h]));
			if (PLOT_DD) mdp.doDisplay(valueDDList[h], makeXADDLabel("NewVDD-", curTrial, h));
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
//			if (PRINT_DD) debugOutput.println("Next State DD = "+q+":\n"+context.getExistNode(q));
//			if (PLOT_DD) mdp.displayGraph(valueDDList[depth-1], makeXADDLabel("Vh-1 "+a._sName, curTrial, depth));
			if (PLOT_DD) mdp.displayGraph(q, makeXADDLabel("Qstart-"+a._sName, curTrial, depth));			
		}
		
		// Discount
		if (mdp._bdDiscount.doubleValue() != 1){
			debugOutput.println("Using discount on Finite Horizon: ");
			q = context.scalarOp(q, mdp._bdDiscount.doubleValue(), XADD.PROD);
		}
		
		// Create a mask for reward function
		int maskReward = context.createMask(a._reward,currS._hmBoolVars, currS._hmContVars, context.NAN);

		if (REGRESS_DEBUG){
			if (PRINT_DD) debugOutput.println("masked Reward DD = "+maskReward+":\n"+context.getExistNode(maskReward));
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
			if (PRINT_DD) debugOutput.println("Q after regression before summing reward DD = "+q+":\n"+context.getExistNode(q));
			if (PLOT_DD) mdp.displayGraph(q, makeXADDLabel("Q after Regress-"+a._sName, curTrial, depth));
		}
		
		if (i_and_ns_vars_in_reward.isEmpty()) {
			q = context.apply(maskReward, q, XADD.SUM);
		}
				
		// Optional Display With just State, Action and Noise Vars
//		if (REGRESS_DEBUG){
//			if (PRINT_DD) debugOutput.println("Qfunction after summing reward = "+q+" DD:\n"+context.getString(q));
//			if (PLOT_DD) mdp.doDisplay(q, "QAfterSumR-" + a._sName + "-" + a._actionParams + "T" + curTrial +"Rem"+depth);
//		}
		
		//Remove Noise Vars
		q = regressNoise(q, a);		
		
		//Remove Action Vars
		q = regressAction(q, a);

		if (THIRD_MASK) q = context.createMask(q, currS._hmBoolVars, currS._hmContVars, context.NAN);
		// Final NaN to Inf Conversion
		q = context.substituteNode(q, context.NAN, context.POS_INF);
		
		if (REGRESS_DEBUG){
			if (PRINT_DD) debugOutput.println("Qfunction after removing NaN = "+q+" DD:\n"+context.getString(q));
			if (PLOT_DD) mdp.doDisplay(q, "QAfterRemoveNaN" + a._sName + "-" + a._actionParams + "T" + curTrial +"Rem"+depth);
		}
		//Constraints Verification
//		if (REGRESS_DEBUG){
//			debugOutput.println("Finished Regressing: "+a._sName);
//			debugOutput.print(context.getExistNode(q).toString());
//		}
		return mdp.standardizeDD(q);
	}
	
	public int regressCVarsMask(int q, CAction a, String var, State currS) {
		
		if (REGRESS_VAR_DEBUG) debugOutput.println("Regressing Continuous " + var);
		// Get cpf for continuous var'
		int var_id = context._cvar2ID.get(var);
		Integer dd_conditional_sub = a._hmVar2DD.get(var);

		if (REGRESS_VAR_DEBUG){
				if (PRINT_DD) debugOutput.println("Initial DD_sub = "+dd_conditional_sub+" DD:\n"+context.getString(dd_conditional_sub));
				if (PLOT_DD) mdp.displayGraph(dd_conditional_sub, "Initial DD_sub" + a._sName + "-" + a._actionParams + "T");
		}
		//MaskTransitions not from current state
		dd_conditional_sub = context.createMask(dd_conditional_sub, currS._hmBoolVars, currS._hmContVars, context.NAN);		
		
		if (REGRESS_VAR_DEBUG){
			String name = "NAN DD_sub";
			int dd =  dd_conditional_sub;
			if (PRINT_DD) debugOutput.println(name+" "+dd+" DD:\n"+context.getString(dd));
			if (PLOT_DD) mdp.displayGraph(dd, name+" "+ a._sName + "-" + a._actionParams + "T");
		}
		
		if (REGRESS_VAR_DEBUG) debugOutput.println("Checking Cache: -->: VAR_ID" +var_id+" DD_SUB"+dd_conditional_sub+" Q"+q);
		// Check cache
		if (!REGRESS_VAR_DEBUG && USE_CONT_REGR_CACHE){
		_contRegrKey.set(var_id, dd_conditional_sub, q);
		Integer result = mdp._hmContRegrCache.get(_contRegrKey);
		if (result != null)
			return result;
		}
		if (REGRESS_VAR_DEBUG){
			String name = "q before regress";
			int dd = q;
			if (PRINT_DD) debugOutput.println(name+" "+dd+" DD:\n"+context.getString(dd));
			if (PLOT_DD) mdp.displayGraph(dd, name+" "+ a._sName + "-" + a._actionParams + "T");
		}
		// Perform regression via delta function substitution
		q = context.reduceProcessXADDLeaf(dd_conditional_sub, 
				context.new DeltaFunctionSubstitution(var, q), true);
		
		if (REGRESS_VAR_DEBUG){
			String name = "q after regress before Mask2";
			int dd = q;
			if (PRINT_DD) debugOutput.println(name+" "+dd+" DD:\n"+context.getString(dd));
			if (PLOT_DD) mdp.displayGraph(dd, name+" "+ a._sName + "-" + a._actionParams + "T");
		}
		
		//Mask result not from current state
		if (SECOND_MASK) q = context.createMask(q, currS._hmBoolVars, currS._hmContVars, context.NAN);
		if (REGRESS_VAR_DEBUG){
			String name = "final q after Mask2";
			int dd = q;
			if (PRINT_DD) debugOutput.println(name+" "+dd+" DD:\n"+context.getString(dd));
			if (PLOT_DD) mdp.displayGraph(dd, name+" "+ a._sName + "-" + a._actionParams + "T");
		}

		// Cache result
		mdp._hmContRegrCache.put(new IntTriple(_contRegrKey), q);
		return q;		
	}

	public int regressBVarsMask(int q, CAction a, String var, State currS) {
		
		// Get cpf for boolean var'
		if (REGRESS_VAR_DEBUG) debugOutput.println("Regressing Boolean " + var);
		int var_id = context.getVarIndex( context.new BoolDec(var), false);
		Integer dd_cpf = a._hmVar2DD.get(var);
		if (REGRESS_VAR_DEBUG) debugOutput.println("Initial DD_cpf -->: " + context.getString(dd_cpf));
		//MaskTransitions not from current state
		dd_cpf = context.createMask(dd_cpf, currS._hmBoolVars, currS._hmContVars, context.NAN);
		if (REGRESS_VAR_DEBUG) debugOutput.println("NAN DD_cpf -->: " + context.getString(dd_cpf));
		q = context.apply(q, dd_cpf, XADD.PROD);
		if (REGRESS_VAR_DEBUG) debugOutput.println("q after PROD with CPF -->: " + context.getString(q));
		
		// Following is a safer way to marginalize (instead of using opOut
		// based on apply) in the event that two branches of a boolean variable 
		// had equal probability and were collapsed.
		int restrict_high = context.opOut(q, var_id, XADD.RESTRICT_HIGH);
		if (REGRESS_VAR_DEBUG) debugOutput.println(" q High -->: " + context.getString(restrict_high));
		int restrict_low  = context.opOut(q, var_id, XADD.RESTRICT_LOW);
		if (REGRESS_VAR_DEBUG) debugOutput.println(" q Low -->: " + context.getString(restrict_low));
		q = context.apply(restrict_high, restrict_low, XADD.SUM);
		if (REGRESS_VAR_DEBUG) debugOutput.println("q after regress before Mask2 -->: " + context.getString(q));
		if (SECOND_MASK) q = context.createMask(q, currS._hmBoolVars, currS._hmContVars, context.NAN);
		if (REGRESS_VAR_DEBUG) debugOutput.println("Final q after mask2 -->: " + context.getString(q));
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
    	try{
    		_resultStreamUp = new PrintStream(new FileOutputStream(OUTPUT_DIR+"/"+solveMethod+".up"));
    	}
    	catch (FileNotFoundException e){
    		System.err.println("Couldn't create result Stream for: "+OUTPUT_DIR + "/" + solveMethod + " update\nException:"+e);
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
			_resultStream.format("%d %f %d %f\n", i, solutionTimeList[i]/1000.0, solutionNodeList[i], (mdp._initialS != null) ? solutionInitialSValueList[i]: "0");
		}
		//Results per Update
		for(int i=0; i<= finalTrial; i++){
			for(int j=nIter; j>0; j--)
				_resultStreamUp.format("%d %d %d %d %f\n", i, j, updateTimes[i][j], updateNodes[i][j], updateIniVals[i][j]);
		}
		if (mdp.DISPLAY_3D){
			for(int i=0; i<=nIter; i++){
				save3D(valueDDList[i], String.format(solveMethod+"-Value%d", i) );
				saveGraph(valueDDList[i], String.format(solveMethod+"-Value%d", i) );
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
