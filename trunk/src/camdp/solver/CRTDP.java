package camdp.solver;
public abstract class CRTDP {
	CRTDP(){
	}
}

//Not using Infinite Horizon CRTDP for now

//package camdp.solver;
//
//import graph.Graph;
//
//import java.io.PrintStream;
//import java.util.*;
//import java.util.Map.Entry;
//
//import util.IntTriple;
//import util.Pair;
//import xadd.ExprLib;
//import xadd.XADD;
//
//import camdp.CAMDP;
//import camdp.State;
//import camdp.CAction;
//
//public class CRTDP extends CAMDPsolver {
//
//	//Trial Keeping Variables
//	public Integer curDepth = null;
//	public Integer curTrial = null;
//	public Integer nTrials = null;
//	public Integer finalTrial = null;
//	
//	/////////////////////////
//	/* Solution Parameters */
//	/////////////////////////
//	public final int DEFAULT_NTRIALS = 100;
//	private static final int DEFAULT_DEPTH = 5;
//	/* Debugging Flags */
//	
//	//Local flags:
//	private static final boolean BELLMAN_DEBUG = false;
//	private static final boolean REGRESS_DEBUG = false;
//	
//	//////////////////Methods /////////////////////////////////
//	
//	public CRTDP(CAMDP camdp){
//		initCRTDP(camdp, DEFAULT_NTRIALS, DEFAULT_DEPTH);
//	}
//	
//	public CRTDP(CAMDP camdp, int nt, int ni){
//		initCRTDP(camdp, nt, ni);
//	}
//	
//	private void initCRTDP(CAMDP camdp, int nt, int ni){
//		mdp = camdp;
//		context = camdp._context;
//		nTrials = nt;
//		nIter = ni;
//		solveMethod = "CRTDP";
//		makeResultStream();
//		setupResults();
//	}
//	
//	////////Main Solver Class ///////////////
//	public int solve(){
//		if (MAIN_DEBUG) debugOutput.println("Starting CRTDP solution, Ntrials "+ nTrials +" , Horizon = " + nIter);
//		if (mdp._initialS == null){
//			System.err.println("Impossible to solve Unknown Initial State MDP with RTDP!");
//			return -1;
//		}
//		
//		resetTimer(1); //total solution time
//		
//		// Initialize Solution
//		boolean solConverge = false;
//		Integer prevDD = null;
//		valueDD = context.ZERO;		//TODO: Heuristic Function
//		curTrial = 0;
//		
//		// Repeat Trials until convergence
//		while (curTrial < nTrials && !solConverge) 
//		{
//			resetTimer(); 
//			++curTrial;			
//			if (MAIN_DEBUG){
//				debugOutput.println("Starting Trial# " + curTrial +" with max depth = " + nIter);
//				if (VALIDATION_TEST){
//					if ( (!DEEP_DEBUG) && PRINT_DD)  debugOutput.println("Initial Value DD = "+valueDD+" DD:"+context.getString(valueDD));
//					if ( (!DEEP_DEBUG) && PLOT_DD)   mdp.doDisplay(valueDD,makeXADDLabel("V Start",curTrial, APPROX_ERROR));
//				}
//			}
//	
//			// Save Previous diagram
//			prevDD = valueDD;
//			
//			//Perform Trial
//			makeTrial(mdp._initialS);
//			
//			//Optional post-max approximation - Could be used safely if overall error is being monitored 
//			valueDD = approximateDD(valueDD);
//			//approxValueDD();
//			flushCaches();
//
//			storeResults();
//			
//
//			if (MAIN_DEBUG){
//				debugOutput.println("Trial:" + curTrial+" Complete");
//				if (PERFORMANCE_TEST) debugOutput.println("Time = "+getElapsedTime());
//				if ( (!DEEP_DEBUG) && PRINT_DD)  debugOutput.println("Value after Trial = "+valueDD+" DD:" + context.getString(valueDD));
//				if ( (!DEEP_DEBUG) && PLOT_DD) mdp.doDisplay(valueDD,makeXADDLabel("V",curTrial, APPROX_ERROR));
//			}
//			
//			// Verify Convergence
//			if (ENABLE_EARLY_CONVERGENCE && prevDD.equals(valueDD) ) {
//				if (MAIN_DEBUG) debugOutput.println("! CRTDP: Converged to solution early, in Trial "+curTrial);
//				solConverge = true;
//			}
//		}
//		flushCaches();	
//		finalTrial = curTrial;
//		if (PERFORMANCE_TEST){
//			debugOutput.println("Total CRTDP Time = "+getElapsedTime(1));
//		}
//		return finalTrial;
//	}
//
//	private void storeResults() {
//		// Results 
//		solutionDDList[curTrial] = valueDD;
//		solutionTimeList[curTrial] = getElapsedTime() + (curTrial >1? solutionTimeList[curTrial-1]:0);
//		solutionNodeList[curTrial] = context.getNodeCount(valueDD);
//		solutionInitialSValueList[curTrial] = context.evaluate(valueDD, mdp._initialS._hmBoolVars, mdp._initialS._hmContVars);
//		if (mdp.LINEAR_PROBLEM) solutionMaxValueList[curTrial] = context.linMaxVal(valueDD);
//	}
//	
//	private void approxValueDD() {
//		if (APPROX_PRUNING) {
//			long appTime = getElapsedTime();
//			valueDD = context.linPruneRel(valueDD, APPROX_ERROR);
//			long pruneTime = getElapsedTime() - appTime;
//			debugOutput.println("ApproxResult: Trial "+ curTrial+ " Solve time = "+appTime+ ", Prune time = "+pruneTime);
//		}
//	}
//
//	private Integer checkCanon(Integer maxDD) {
//		// Error checking and logging
//		int canon_max_dd = context.makeCanonical(maxDD);
//		if (maxDD != canon_max_dd) {
//			System.err.println("CAMDP VI ERROR: encountered non-canonical node that should have been canonical... could be rounding, continuing.");
//			context.exportXADDToFile(maxDD, "ERRORdiagram1OriginalXADD.xadd");
//			context.exportXADDToFile(canon_max_dd, "ERRORdiagram2makeCanonical.xadd");
//			context.getGraph(maxDD).launchViewer("ERROR diagram 1: original maxDD");
//			context.getGraph(canon_max_dd).launchViewer("ERROR diagram 2: makeCanonical(maxDD)");
//		}
//		return maxDD;
//	}
//
//
//	private void makeTrial(State currentS){
//		int remainH = nIter;
//		while (remainH > 0){	
//			resetTimer(3); //One full Backup
//			curDepth = nIter-remainH;
//			int prevDD = valueDD;
//			State prevS = currentS;
//			
//			if (DEEP_DEBUG){
//				debugOutput.println("BB Start, Trial "+curTrial+", remH "+remainH);
//				if (VALIDATION_TEST){ 
//					debugOutput.println("Current State = "+currentS.toString());
//					debugOutput.println("State Value = "+ context.evaluate(valueDD, currentS._hmBoolVars, currentS._hmContVars) );
//					if (PRINT_DD) debugOutput.println("Initial Value = "+valueDD+" DD:\n"+ context.getExistNode(valueDD) +"\n");
//					if (PLOT_DD) mdp.doDisplay(valueDD, makeXADDLabel("V StartBB", curTrial, remainH, APPROX_ERROR));
//				}
//			}
//			ParametrizedAction greedyAction = regionBellmanBackup(currentS);
//			long bbTime = getElapsedTime(3);
//			resetTimer(3);
//			//Using greedy action, sample next state
//			State nextS = sample(currentS, greedyAction);
//			
//			if (DEEP_DEBUG){
//				debugOutput.println("BB Finish, Trial "+curTrial+", remH "+remainH);
//				if (VALIDATION_TEST){
//					debugOutput.println("State After Sample = "+nextS);
//					debugOutput.println("New State Value = "+ context.evaluate(valueDD, currentS._hmBoolVars, currentS._hmContVars) +"\n" );
//					if (PRINT_DD) debugOutput.println("Value After Backup = "+ valueDD +" DD:\n"+ context.getExistNode(valueDD) +"\n");
//					
//				}
//				if ( (!BELLMAN_DEBUG) && PLOT_DD) mdp.doDisplay(valueDD, makeXADDLabel("V After BB ", curTrial, remainH, APPROX_ERROR));
//				if (PERFORMANCE_TEST) debugOutput.println("Trial "+curTrial+" Step "+(curDepth)+" BBTime = "+bbTime+" Sample = "+getElapsedTime(3));
//			}
////			if (prevDD == valueDD && prevS.equals(nextS) ){
////				// TODO: Can we stop trials early if back upis not changing
////				debugOutput.println("Trial #"+curTrial+" converged early!, finishing it");
////				break;
////			}
//			
//			currentS = nextS;
//			remainH = remainH - 1;
//		}
//	}
//	
//	
//	public ParametrizedAction  regionBellmanBackup(State currS){
//		//		 Iterate over each action
//		Integer maxDD = null;
//		CAction maxAction = null;
//		Double currSValue = null;
//		HashMap<String, Double> maxParam = new HashMap<String, Double>();
//		
//		if (BELLMAN_DEBUG){
//			debugOutput.println("Bellman Backup Start: T"+curTrial+" BB"+curDepth);
//			if (VALIDATION_TEST){
//				debugOutput.println("BB State "+currS);
//				if (PRINT_DD) debugOutput.println("Original ValueDD = "+valueDD+"DD:\n"+ context.getExistNode(valueDD));
//				if (PLOT_DD) mdp.doDisplay(valueDD,makeXADDLabel("Original V", curTrial, curDepth, APPROX_ERROR));
//			}
//		}
//		
//		for (Map.Entry<String,CAction> me : mdp._hmName2Action.entrySet()) {
//				resetTimer(4);
//				// Regress the current value function through each action (finite number of continuous actions)
//				int regr = regressRegion(valueDD, me.getValue(), currS);
//				double value = context.evaluate(regr, currS._hmBoolVars, currS._hmContVars);
//				
//				long regressTime = getElapsedTime(4);
//				resetTimer(4);
//				
//				// Maintain running max over different actions
//				maxDD = (maxDD == null) ? regr : context.apply(maxDD, regr, XADD.MAX);
//				maxDD = standardizeDD(maxDD); 
//
//				if (currSValue == null || value > currSValue){
//					maxAction = me.getValue();
//					currSValue =value; 
//				}
//				
//				if (BELLMAN_DEBUG){
//					debugOutput.println("Regress and Max Action: " + me.getValue()._sName +" complete.");
//					if (VALIDATION_TEST) {
//						debugOutput.println("Action Value:" + value+ "Max Value ="+currSValue+"MaxAction ="+maxAction._sName);
//						if (PRINT_DD) debugOutput.println("Q Regress = "+regr+" DD:\n" + context.getExistNode(regr));
//						if (PLOT_DD) mdp.doDisplay(regr, makeXADDLabel("Regr "+me.getValue()._sName, curTrial, curDepth, APPROX_ERROR));
//						if (PLOT_DD) mdp.doDisplay(maxDD, makeXADDLabel("Current Max "+me.getValue()._sName, curTrial, curDepth, APPROX_ERROR));
//					}
//					if (PERFORMANCE_TEST) debugOutput.println("Regress Time = "+(regressTime)+" Maxim Time = "+getElapsedTime(4)); 
//				}
//		}
//
//		resetTimer(4); //Final Minimization
//		
//		//Min out Illegal +Inf values, these will be non update regions
//		valueDD = context.apply(maxDD, valueDD, XADD.MIN);
//		valueDD = standardizeDD(valueDD);		
//		
//		ParametrizedAction pA = new ParametrizedAction(maxAction, maxParam);
//		if (BELLMAN_DEBUG){
//			if (VALIDATION_TEST){
//				debugOutput.println("\nBackup End, Greedy Action: " + pA + " Greedy Value:" + currSValue +"\n");
//				if (PRINT_DD) debugOutput.println("MAX of Regr = "+maxDD+" DD:\n" + context.getExistNode(maxDD));
//				if (PRINT_DD) debugOutput.println("\n New Vfun = "+valueDD+" DD:\n" + context.getExistNode(valueDD));
//				if (PLOT_DD) mdp.doDisplay(valueDD, makeXADDLabel("Regression End ", curTrial, curDepth, APPROX_ERROR));
//			}
//			if (PERFORMANCE_TEST) debugOutput.println("BB Final Minim Time = "+getElapsedTime(4));
//		}
//		return pA;
//	}
//	
//	private IntTriple _contRegrKey = new IntTriple(-1,-1,-1);
//
//	public int regressRegion(int vfun, CAction a, State currS) {
//		if (REGRESS_DEBUG){
//			debugOutput.println("Regressing: "+a._sName+" Trial "+curTrial+" Step "+curDepth);
//			if (VALIDATION_TEST) if (PLOT_DD) mdp.doDisplay(vfun, makeXADDLabel("Qstart-"+a._sName, curTrial, 0, APPROX_ERROR));
//		}
//		// Prime the value function 
//		resetTimer(5); //substitute
//		int q = context.substitute(vfun, mdp._hmPrimeSubs); 
//		long primeTime = getElapsedTime(5);
//		resetTimer(5);
//		
//		// Discount
//		q = context.scalarOp(q, mdp._bdDiscount.doubleValue(), XADD.PROD);
//		int maskReward = context.createMask(a._reward,currS._hmBoolVars, currS._hmContVars, context.NAN);
//		long maskRewTime = getElapsedTime(5);
//		resetTimer(5);
//
//		// Add reward *if* it contains primed vars that need to be regressed
//		HashSet<String> i_and_ns_vars_in_reward = filterIandNSVars(context.collectVars(a._reward), true, true);
//		if (!i_and_ns_vars_in_reward.isEmpty()) {
//			q = context.apply(maskReward, q, XADD.SUM); // Add reward to already discounted primed value function
//			_logStream.println("- Added in reward pre-marginalization with interm/next state vars: " + i_and_ns_vars_in_reward);
//		}
//		
//		long addRewTime = getElapsedTime(5);
//		resetTimer(5);
//		
//		// Derive a variable elimination order for the DBN w.r.t. the reward that puts children before parents
//		HashSet<String> vars_to_regress = filterIandNSVars(context.collectVars(q), true, true);
//		Graph g = buildDBNDependencyDAG(a, vars_to_regress);
//		if (g.hasCycle()) 
//			displayCyclesAndExit(g, a);
//		
//		// Get a valid elimination order (does not minimize tree width, could be optimized)
//		List var_order = g.topologicalSort(true);
//		
//		// Regress each variable in the topological order
//		for (Object o : var_order) {
//			String var_to_elim = (String)o;
//			if (mdp._hsBoolIVars.contains(var_to_elim) || mdp._hsBoolNSVars.contains(var_to_elim)) {
//				q = regressBVarsMask(q, a, var_to_elim, currS);
//			} else if (mdp._hsContIVars.contains(var_to_elim) || mdp._hsContNSVars.contains(var_to_elim)) {
//				q = regressCVarsMask(q, a, var_to_elim, currS);
//			} else {
//				//Ignore Var
//			}
//		}
//		
//		if (i_and_ns_vars_in_reward.isEmpty()) {
//			q = context.apply(maskReward, q, XADD.SUM);
//		}
//		
//
//		long regStateTime = getElapsedTime(5);
//		resetTimer(5);
//		
//		// Optional Display With just State, Action and Noise Vars
//		if (REGRESS_DEBUG){
//			if (VALIDATION_TEST){
//				debugOutput.println("Qfunction after removing NS = "+q+"DD:\n"+context.getString(q));
//				_logStream.println("- Q^" + "(" + a._sName + ", " + a._actionParams + " )\n" + context.getString(q));
//				if (PLOT_DD) mdp.displayGraph(q, "Q-" + a._sName + "-" + a._actionParams + "^" + curTrial + "-" + Math.round(1000*APPROX_ERROR));
//			}
//		}
//		
//		//Remove Noise Vars
//		q = regressNoise(q, a);		
//		long regNoiseTime = getElapsedTime(5);
//		resetTimer(5);
//		
//		//Remove Action Vars
//		q = regressAction(q, a);
//		long regActionTime = getElapsedTime(5);
//		resetTimer(5);
//
//		if (REGRESS_DEBUG){
//			debugOutput.println("Finished Regressing: "+a._sName);
//			if (VALIDATION_TEST) {
//				if (PLOT_DD) mdp.doDisplay(q, makeXADDLabel("Qend-"+a._sName, curTrial, 0, APPROX_ERROR));
//			}
//			if (PERFORMANCE_TEST) {
//				debugOutput.print("regressActVar times: Prime = "+primeTime+", MaskTime = "+maskRewTime+", addRewTime = "+addRewTime);
//				debugOutput.println("regressState = "+regStateTime+", regressNoiseTime = "+regNoiseTime+", regressAction = "+regActionTime);
//			}
//		}
//		return standardizeDD(q);
//	}
//	
//	public int regressCVarsMask(int q, CAction a, String var, State currS) {
//		
//		// Get cpf for continuous var'
//		int var_id = context._cvar2ID.get(var);
//		Integer dd_conditional_sub = a._hmVar2DD.get(var);
//
//		//MaskTransitions not from current state
//		dd_conditional_sub = context.createMask(dd_conditional_sub, currS._hmBoolVars, currS._hmContVars, context.POS_INF);		
//		
//		// Check cache
//		_contRegrKey.set(var_id, dd_conditional_sub, q);
//		Integer result = null;
//		if ((result = mdp._hmContRegrCache.get(_contRegrKey)) != null)
//			return result;
//		
//		// Perform regression via delta function substitution
//		q = context.reduceProcessXADDLeaf(dd_conditional_sub, 
//				context.new DeltaFunctionSubstitution(var, q), true);
//		
//		// Cache result
//		mdp._hmContRegrCache.put(new IntTriple(_contRegrKey), q);
//		
//		return q;		
//	}
//
//	public int regressBVarsMask(int q, CAction a, String var, State currS) {
//		
//		// Get cpf for boolean var'
//		int var_id = context.getVarIndex( context.new BoolDec(var), false);
//		Integer dd_cpf = a._hmVar2DD.get(var);
//		
//		//MaskTransitions not from current state
//		dd_cpf = context.createMask(dd_cpf, currS._hmBoolVars, currS._hmContVars, context.ZERO);
//		_logStream.println("- Summing out: " + var + "/" + var_id /*+ " in\n" + _context.getString(dd_cpf)*/);
//		q = context.apply(q, dd_cpf, XADD.PROD);
//		
//		// Following is a safer way to marginalize (instead of using opOut
//		// based on apply) in the event that two branches of a boolean variable 
//		// had equal probability and were collapsed.
//		int restrict_high = context.opOut(q, var_id, XADD.RESTRICT_HIGH);
//		int restrict_low  = context.opOut(q, var_id, XADD.RESTRICT_LOW);
//		q = context.apply(restrict_high, restrict_low, XADD.SUM);
//		q = context.createMask(q, currS._hmBoolVars, currS._hmContVars, context.POS_INF);
//		_logStream.println("-->: " + context.getString(q));
//
//		return q;
//	}
//
//	
//	// Sampling
//	public State sample(State currentS, ParametrizedAction parA){
//		HashSet<String> AllNSVars = new HashSet<String>();
//		AllNSVars.addAll(mdp._hsBoolNSVars);
//		AllNSVars.addAll(mdp._hsContNSVars);
//		Graph g = buildDBNDependencyDAG(parA._action, AllNSVars);
//		if (g.hasCycle()) 
//			displayCyclesAndExit(g, parA._action);
//		
//		List var_order = g.topologicalSort(true);
//		
//		// Sample each variable in the topological order
//		State nextS = new State();
//		for (Object o : var_order) {
//			String var = (String)o;
//			Integer dd_cpf = parA._action._hmVar2DD.get(var);
//			
//			if (mdp._hsBoolNSVars.contains(var)) {
//				Boolean val = sampleBVar(currentS, nextS, var, dd_cpf, parA._params);
//				nextS._hmBoolVars.put(var, val);
//			} else if (mdp._hsContNSVars.contains(var)) {
//				Double val = sampleCVar(currentS,nextS,dd_cpf, parA._params);
//				nextS._hmContVars.put(var, val);
//			} else {
//				// The topological sort will also add in next state and action variables since they were parents in the network
//				//debugOutput.println("- Ignoring Non-NextState variable " + var + " during sampling");
//				//_logStream.println("- Ignoring Non-NextState variable " + var + " during sampling");
//			}
//		}
//		return unprime(nextS);
//	}
//	
//	public Boolean sampleBVar(State prevS, State nextS, String bVar, Integer bVarDD, HashMap<String, Double> aParams){
//		HashMap<String, Boolean> bool_assign = makeBoolAssign(prevS, nextS);
//		bool_assign.put(bVar, true); //bVar is the next state boolean we are sampling
//		double trueProb = context.evaluate(bVarDD, bool_assign, makeContAssign(prevS,nextS, aParams) );
//		return (Math.random() < trueProb) ? true: false;
//	}
//	
//	public Double sampleCVar(State prevS, State nextS, Integer cVarDD, HashMap<String, Double> aParams){
//		return context.evaluate(cVarDD, makeBoolAssign(prevS,nextS), makeContAssign(prevS,nextS, aParams));
//	}
//
//	public HashMap<String, Boolean> makeBoolAssign(State currentS, State nextS){
//		HashMap<String, Boolean> bool_assign = new HashMap<String, Boolean>();
//		bool_assign.putAll(currentS._hmBoolVars);
//		bool_assign.putAll(nextS._hmBoolVars);
//		return bool_assign;
//	}
//	
//	public HashMap<String, Double> makeContAssign(State currentS,State nextS,HashMap<String, Double> aParams){
//		HashMap<String, Double> cont_assign = new HashMap<String, Double>();
//		cont_assign.putAll(currentS._hmContVars);
//		cont_assign.putAll(nextS._hmContVars);
//		cont_assign.putAll(aParams);
//		return cont_assign;
//	}
//	
//	private State unprime(State nextS) {
//		State newS = new State();
//		for(Entry<String, Boolean> e: nextS._hmBoolVars.entrySet()){
//			String var = e.getKey();
//			if (!var.endsWith("'")) debugOutput.println("Invalid (unprimed) Next State Var: "+ var);
//			newS._hmBoolVars.put(var.substring(0, var.length()-1), e.getValue());
//		}
//		for(Entry<String, Double> e: nextS._hmContVars.entrySet()){
//			String var = e.getKey();
//			if (!var.endsWith("'")) debugOutput.println("Invalid (unprimed) Next State Var: "+ var);
//			newS._hmContVars.put(var.substring(0, var.length()-1), e.getValue());
//		}
//		return newS;
//	}
//	
//    
//	//Memory Management
//	public void flushCaches(List<Integer> specialNodes){
//		ArrayList<Integer> moreSpecialNodes = new ArrayList<Integer>();
//		moreSpecialNodes.addAll(specialNodes);
//		moreSpecialNodes.add(valueDD);
//		for(int i=1;i<curTrial;i++) moreSpecialNodes.add(solutionDDList[i]);
//		mdp.flushCaches(moreSpecialNodes);
//	}
//	
//	//Plot
//	public String makeXADDLabel(String xadd, int trial, double approx){
//		return  xadd+" Trial"+trial+(approx > 0?"-approx"+String.format("%03d",Math.round(1000*approx)): "");
//	}
//	public String makeXADDLabel(String xadd, int trial, int remH, double approx){
//		return  xadd+" Trial"+trial+" RemH "+remH+(approx > 0? "-approx"+String.format("%03d",Math.round(1000*approx)): "");
//	}
//
//    ///////////// Results //////////////////////////////////
//	public void setupResults(){
//		solutionDDList = new int[nTrials+1];
//		solutionTimeList = new long[nTrials+1];
//		solutionNodeList = new int[nTrials+1];
//		solutionInitialSValueList = new double[nTrials+1];
//		solutionMaxValueList = new double[nTrials+1];
//	}
//
//	public void saveResults(){
//		//Results: NIter, Time, Nodes, InitialS Value.
//		for(int i=1; i<=finalTrial; i++){
//		_resultStream.format("%d %d %d %f\n", i, solutionTimeList[i], solutionNodeList[i], (mdp._initialS != null) ? solutionInitialSValueList[i]: "0");
//	}
//}
//    
//    public void printResults() {
//		debugOutput.println("Results for CRTDP: " + (finalTrial) + " trials:");
//		debugOutput.print("Time:"); for(int i=1; i<=finalTrial; i++) debugOutput.print(solutionTimeList[i]+" ");debugOutput.println(";");
//		debugOutput.print("Nodes:"); for(int i=1; i<=finalTrial; i++) debugOutput.print(solutionNodeList[i]+" ");debugOutput.println(";");
//		debugOutput.print("Initial S Value:"); for(int i=1; i<=finalTrial; i++) debugOutput.print(solutionInitialSValueList[i]+" ");debugOutput.println(";");
//	}
//    
//}