package camdp.solver;
import graph.Graph;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.Map.Entry;

import util.IntPair;
import util.IntTriple;
import util.Pair;
import xadd.ExprLib;
import xadd.XADD;
import xadd.ExprLib.ArithExpr;
import xadd.ExprLib.DoubleExpr;
import xadd.LinearXADDMethod.NamedOptimResult;
import xadd.XADD.XADDNode;

import camdp.CAMDP;
import camdp.State;
import camdp.CAction;

public class CRTDPFH extends CAMDPsolver {
    
    public Integer curTrial = null;
    public Integer curDepth = null;
    private Integer regressionMask = null;
    private Integer maxDD = null;
    public Integer nTrials = null;
    Integer[] valueDDList;
    private Integer finalTrial = null;
    
    
    /////////////////////////
    /* Solution Parameters */
    /////////////////////////
    public static final int DEFAULT_NTRIALS = 100;
    private static final boolean REVERSE_TRIAL = true;
    private static final boolean SECOND_MASK = true;
    private static final boolean IMMEDIATEREWARD = true;
    boolean APPROXIMATION = false;
    private static final int TRIAL_PER_APPROX = 3;
    
    /* Debugging Flags */    
    //Local Debug flags    
    private static final boolean GREEDY_ACTION_DEBUG = false;
    private static final boolean COMPLETE_RESULTS = false;
    private static final boolean DEBUG_RETRIEVE_MASK = false;
    private static final boolean WARN_BACKUP_FAIL = false;

    
    //Result Keeping
    public long[][] updateTimes = null;
    public int[][] updateNodes = null;
    public double[][] updateIniVals = null;
    public PrintStream _resultStreamUp = null;
    //////////////////Methods /////////////////////////////////
        
    public CRTDPFH(CAMDP camdp, int nTrials, int iter){
        this(camdp, nTrials, iter,0d);
    }
    
    public CRTDPFH(CAMDP camdp, int nt, int ni, double approx){
        mdp = camdp;
        context = camdp._context;
        nTrials = nt;
        nIter = ni; 
        dApproxError = approx;
        if (approx > 0) APPROXIMATION = true;
        valueDDList = new Integer[nIter+1];
        _logStream = camdp._logStream;
        solveMethod = APPROXIMATION? "ARTSDP": "RTSDP";
        makeResultStream();
        setupResults();
    }
    
    private int getValueDD(){
        return valueDDList[nIter];
    }

    ////////Main Solver Class ///////////////
    public int solve(){
        int RUN_DEPTH = 1;
        if (DEBUG_DEPTH > RUN_DEPTH) debugOutput.println("Starting "+solveMethod+" solution, Horizon = "+nIter+", nTrial = " + nTrials);
        if (mdp._initialS == null){
            System.err.println("Impossible to solve Unknown Initial State MDP with "+solveMethod);
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
            CAMDP.resetTimer(RUN_DEPTH);
            if (DEBUG_DEPTH > RUN_DEPTH){
                debugOutput.println("Starting Trial# " + curTrial);
            }
            
            int []prevDDList = new int[nIter+1];
            for(int i=0;i<=nIter;i++) prevDDList[i] = valueDDList[i]; 
                        
            //Perform Trial
            makeTrial(mdp._initialS);
            solConverge = earlyConverge(prevDDList);
            flushCaches();
            //////////////////////////////////////////////////////////////////////////
            solutionDDList[curTrial] = getValueDD();
            solutionTimeList[curTrial] = CAMDP.getElapsedTime(RUN_DEPTH) + (curTrial >1? solutionTimeList[curTrial-1]:0);
            solutionNodeList[curTrial] = getAllNodes();
            solutionInitialSValueList[curTrial] = getStateVal(mdp._initialS, nIter);            
            if (mdp.LINEAR_PROBLEM) solutionMaxValueList[curTrial] = context.linMaxVal(getValueDD());
            
            if (DEBUG_DEPTH > RUN_DEPTH){
                debugOutput.println("Trial:" + curTrial+" Complete");
                debugOutput.println("Value DD:"+getValueDD()+" Nodes= "+solutionNodeList[curTrial]+"("+context.getNodeCount(getValueDD())+") Time ="+solutionTimeList[curTrial]);
                debugOutput.println("Initial State Value = "+solutionInitialSValueList[curTrial]);
                debugShow(getValueDD(), "Value after Trial "+curTrial, true);
                debugOutput.println();
            }
        }
        finalTrial = curTrial;
        return finalTrial;
    }

    public ParametrizedAction getGreedyAction(int valDD, State s){
      int RUN_DEPTH = 4;
      double maxVal = Double.NEGATIVE_INFINITY;
      CAction greedyAction = null;
      HashMap<String, Double> greedyParams = null;
      
      for (Map.Entry<String,CAction> me : mdp._hmName2Action.entrySet()) {
          // Regress the current value function through each action (finite number of continuous actions)
          CAction a = me.getValue();
          int q = regressRegionRetrieveMask(valDD, a, s, false);
          int contQbool = context.substituteBoolVars(q, s._hmBoolVars);
          HashMap<String, ArithExpr> subs = new HashMap<String, ArithExpr>();
          for (Entry<String, Double> pair: s._hmContVars.entrySet()){
              subs.put(pair.getKey(), new DoubleExpr(pair.getValue()) );
          }
          int localQ = context.substitute(contQbool, subs);
          if (DEBUG_DEPTH > RUN_DEPTH){
              debugShow(localQ, "Totally substituted Q GA"+me.getValue()._sName+"T"+curTrial+"H"+curDepth, false);
          }
//          if (GREEDY_ACTION_DEBUG) mdp.displayGraph(q, "Original Q");
//          if (GREEDY_ACTION_DEBUG) mdp.displayGraph(contQbool, "Only Cont Q");
//          if (GREEDY_ACTION_DEBUG) mdp.displayGraph(localQ, "LocalQ");
          for (int i=0; i < a._actionParams.size(); i++) 
          {
              String avar = a._actionParams.get(i);
              double lb   = a._hmAVar2LB.get(avar);
              double ub   = a._hmAVar2UB.get(avar);
              context.addContinuousVarBounds(avar, lb, ub);
          }
          localQ = mdp.standardizeDD(localQ);
          NamedOptimResult solution = context.linMaxArg(localQ);
          if (solution.sol_value > maxVal){
              maxVal = solution.sol_value;
              greedyAction = a;
              greedyParams = solution.assignment;
          }
      }
      // If an action variable is indeterminate by the maximization, use the lower bound value. (TODO: Is there a better heuristic for this?)
      for (String param: greedyAction._actionParams){
          if ( !greedyParams.containsKey(param))
              greedyParams.put(param, greedyAction._hmAVar2LB.get(param));
      }
      if (DEBUG_DEPTH > RUN_DEPTH){
          debugOutput.println("Greedy Action is: "+greedyAction._sName +" with "+greedyParams);
      }
      return new ParametrizedAction(greedyAction, greedyParams);
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
            debugOutput.println("Converged to solution early, in Trial "+curTrial);
            return true;
        }
    }        
    
    private void makeTrial(State currentS){
        int RUN_DEPTH = 2;
        State []stateList = new State[nIter+1];
        for(curDepth= nIter; curDepth > 0; curDepth--){
            stateList[curDepth] = currentS;
            CAMDP.resetTimer(RUN_DEPTH);
            if (DEBUG_DEPTH > RUN_DEPTH){
                debugOutput.println("Start Backup at T#"+curTrial+", d ="+curDepth+", "+ currentS.toString());    
                debugOutput.println("State Value = "+ getStateVal(currentS, curDepth) );
            }
            ParametrizedAction greedyAction = regionBellmanBackup(currentS);
            
            updateTimes[curTrial][curDepth] = CAMDP.getElapsedTime(RUN_DEPTH);
            updateNodes[curTrial][curDepth] = context.getNodeCount(valueDDList[curDepth]);
            updateIniVals[curTrial][curDepth] = getStateVal(currentS, curDepth);
            
            //Using greedy action, sample next state
            CAMDP.resetTimer(RUN_DEPTH);
            State nextS = sample(currentS, greedyAction);
            if (DEBUG_DEPTH > RUN_DEPTH) {
                debugOutput.println("Backup "+curDepth+" took = "+updateTimes[curTrial][curDepth]);
                debugOutput.println("Greedy Action = "+greedyAction+"State Value After Update= "+ getStateVal(currentS, curDepth) );
                debugOutput.println("State After Sample = "+nextS+" Value= "+ getStateVal(nextS,curDepth-1)+"\n");
                debugShow(valueDDList[curDepth],"V"+curDepth +" After Update T"+curTrial,true);
            }
            currentS = nextS;
            flushCaches();
        }
        //reverse Trial
        if (REVERSE_TRIAL){ //no need to repeat update on last state, revert from 2, no need to reverse update initialS, first update of next trial will take care of it.
            for(curDepth = 2; curDepth < nIter; curDepth++){
                currentS = stateList[curDepth];
                if (DEBUG_DEPTH > RUN_DEPTH){
                    debugOutput.println("Start Reverse Backup at T#"+curTrial+", d ="+curDepth+", "+ currentS.toString());    
                    debugOutput.println("State Value = "+ getStateVal(currentS, curDepth) );
                }
                ParametrizedAction greedyAction = regionBellmanBackup(currentS);
                if (DEBUG_DEPTH > RUN_DEPTH) {
                    debugOutput.println("Greedy Action = "+greedyAction+"State Value After Update= "+ getStateVal(currentS, curDepth) );
                    debugShow(valueDDList[curDepth],"V"+curDepth +" After Update Reverse T"+curTrial,true);
                }
            }
        }
        checkLinearApprox();
    }

    private Double getStateVal(State currentS, int remainHorizon) {
        return mdp.evaluateState(valueDDList[remainHorizon], currentS);
    }
    
    public ParametrizedAction  regionBellmanBackup(State currS){
        int RUN_DEPTH=3; 
        
        //Iterate over each action
        maxDD = null;
        CAction maxAction = null;
        Double currSValue = null;
        HashMap<String, Double> maxParam = new HashMap<String, Double>();
                
        for (Map.Entry<String,CAction> me : mdp._hmName2Action.entrySet()) {
                // Regress the current value function through each action (finite number of continuous actions)
                
                CAMDP.resetTimer(RUN_DEPTH);
                //int regr = regressRegion(valueDDList[curDepth-1], me.getValue(), currS, true);
                int regr = regressRegionRetrieveMask(valueDDList[curDepth-1], me.getValue(), currS, true);
                
                double value = mdp.evaluateState(regr, currS);
                if (value == Double.POSITIVE_INFINITY || value == Double.NEGATIVE_INFINITY || value == Double.NaN){
                    System.err.println("Regression Fail: Initial State value is Pos INF, Neg INF or NaN in Q value function:");
                    System.err.println("State is "+currS);
                    if (!CAMDP.SILENCE_ERRORS_PLOTS) {
                        mdp.displayGraph(regr, makeXADDLabel("Inf Q: Regr "+me.getValue()._sName+" DD", curTrial, curDepth));
                        mdp.evaluateState(regr, currS);
                        DEBUG_DEPTH = 6;
                        regressRegion(valueDDList[curDepth-1], me.getValue(), currS, true);                    
                    }
                }
                
                if (DEBUG_DEPTH > RUN_DEPTH){
                    debugOutput.println("Regressed Action: " + me.getKey() + " IniS Value:" + value+"Regr Time = "+CAMDP.getElapsedTime(RUN_DEPTH));
                    debugShow(regr, "Final Q"+me.getValue()._sName+"T"+curTrial+"H"+curDepth, false);
                }
             
                //Maintain running max over different actions
                maxDD = (maxDD == null) ? regr : context.apply(maxDD, regr, XADD.MAX);
                maxDD = mdp.standardizeDD(maxDD);
                
                if (currSValue == null || value > currSValue){
                    maxAction = me.getValue();
                    currSValue =value; 
                }
                if ( Math.abs(currSValue - mdp.evaluateState(maxDD, currS) ) > STATE_PRECISION){
                    System.err.println("Maxim fail, greedy value "+currSValue+" different from value "+mdp.evaluateState(maxDD, currS)+" for "+currS);
                    if (!CAMDP.SILENCE_ERRORS_PLOTS) {
                        mdp.displayGraph(regr, makeXADDLabel("Regr"+me.getValue()._sName+" DD", curTrial, curDepth));
                        mdp.displayGraph(maxDD, makeXADDLabel("BB Max_DD", curTrial, curDepth));
                    }
                }
                if (DEBUG_DEPTH > RUN_DEPTH){
                    debugOutput.println("Current Max after Regress Action: " + me.getValue()._sName + " Value:" + currSValue);
                    debugShow(maxDD, makeXADDLabel("MaxDD-"+me.getValue()._sName, curTrial, curDepth), true);
                }
    
        }
        
        //Min out Ilegal +Inf values, these will be non update regions
        valueDDList[curDepth] = context.apply(maxDD, valueDDList[curDepth], XADD.MIN);        
        valueDDList[curDepth] = mdp.standardizeDD(valueDDList[curDepth]); 
        if ( Math.abs(currSValue - mdp.evaluateState(valueDDList[curDepth], currS)) > STATE_PRECISION
        		&& WARN_BACKUP_FAIL){
            System.err.println("Backup fail, greedy value "+ currSValue+" different from value "+mdp.evaluateState(valueDDList[curDepth], currS)+" for "+currS);
            System.err.println("Max DD Current State value is " + mdp.evaluateState(maxDD, currS));
            if (!CAMDP.SILENCE_ERRORS_PLOTS) mdp.displayDifError(maxDD, valueDDList[curDepth]);
            valueDDList[curDepth] = context.apply(maxDD, valueDDList[curDepth], XADD.MIN);
        }
        
        ParametrizedAction pA = getGreedyAction(valueDDList[curDepth-1], currS);
        return pA;
    }

    /**
     * Regress a DD through an action
     **/
    private IntTriple _contRegrKey = new IntTriple(-1,-1,-1);

    public int regressRegion(int valDD, CAction a, State currS, boolean regressParams) {
        int RUN_DEPTH = 4;
        // Prime the value function 
        int q = context.substitute(valDD, mdp._hmPrimeSubs); 
        
        if (DEBUG_DEPTH > RUN_DEPTH){
            debugOutput.println("Regressing: "+valDD+" at " +currS+" with "+a._sName);
            debugShow(q, makeXADDLabel("Qstart RR"+a._sName, curTrial, curDepth), false);            
        }
        
        // Discount
        if (mdp._bdDiscount.doubleValue() != 1){
            debugOutput.println("Using discount on Finite Horizon: ");
            q = context.scalarOp(q, mdp._bdDiscount.doubleValue(), XADD.PROD);
        }
        
        // Add reward *if* it contains primed vars that need to be regressed
        boolean i_and_ns_vars_in_reward = ! filterIandNSVars(context.collectVars(a._reward), true, true).isEmpty();
        boolean avars_in_reward = ! filterAVars(context.collectVars(a._reward)).isEmpty();
        
        if (i_and_ns_vars_in_reward) {
            // Create a mask for reward function
            int maskedReward = createMaskedReward(a, currS);
            q = context.apply(maskedReward, q, XADD.SUM); // Add reward to already discounted primed value function
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
        
        if (DEBUG_DEPTH > RUN_DEPTH){
            debugShow(q, makeXADDLabel("Q RR after Regress State "+a._sName, curTrial, curDepth), false);
        }
        
        if ( (!i_and_ns_vars_in_reward) && (avars_in_reward) ) {
            // Create a mask for reward function
            int maskedReward = createMaskedReward(a, currS);
            q = context.apply(maskedReward, q, XADD.SUM);
        }
                        
        //Remove Noise Vars
        q = regressNoise(q, a);        
        
        if (DEBUG_DEPTH > RUN_DEPTH){
            debugShow(q, makeXADDLabel("Q RR before Regress Action "+a._sName, curTrial, curDepth), false);
        }
        
        //Remove Action Vars
        if (regressParams){
            q = regressAction(q, a);
        }
        if (DEBUG_DEPTH > RUN_DEPTH && regressParams) {
            debugShow(q, "Q RR After Regress Action" + a._sName + "-" + a._actionParams + "T" + curTrial +"Rem"+curDepth, false);
        }
        
        if ( (!i_and_ns_vars_in_reward) && (!avars_in_reward) ) {
            // Create a mask for reward function
            int maskReward = context.createMask(a._reward,currS._hmBoolVars, currS._hmContVars, context.NAN);
            if (DEBUG_DEPTH > RUN_DEPTH){
                debugShow(maskReward, makeXADDLabel("MaskRew RR"+a._sName, curTrial, curDepth), false);
            }
            q = context.apply(maskReward, q, XADD.SUM);
        }
        
        // Final NaN to Inf Conversion
        q = context.substituteNode(q, context.NAN, context.POS_INF);
        
        if (DEBUG_DEPTH > RUN_DEPTH && regressParams) {
            debugShow(q, "Q RR After Remove NaN" + a._sName + "-" + a._actionParams + "T" + curTrial +"Rem"+curDepth, false);
        }
        return mdp.standardizeDD(q);
    }

    public int regressRegionRetrieveMask(int valDD, CAction a, State currS, boolean regressParams) {
        int RUN_DEPTH = 4;
        regressionMask = context.ZERO;
        // Prime the value function 
        int q = context.substitute(valDD, mdp._hmPrimeSubs); 
       
        if (DEBUG_DEPTH > RUN_DEPTH){
            debugOutput.println("RM Regressing: "+valDD+" at " +currS+" with "+a._sName + (regressParams? "": "without params") );
            debugShow(q, makeXADDLabel("Q RM start "+a._sName, curTrial, curDepth), false);            
        }
        
        // Discount
        if (mdp._bdDiscount.doubleValue() != 1){
            debugOutput.println("Using discount on Finite Horizon: ");
            q = context.scalarOp(q, mdp._bdDiscount.doubleValue(), XADD.PROD);
        }
        
        // Add reward *if* it contains primed vars that need to be regressed
        boolean i_and_ns_vars_in_reward = ! filterIandNSVars(context.collectVars(a._reward), true, true).isEmpty();
        boolean avars_in_reward = ! filterAVars(context.collectVars(a._reward)).isEmpty();
        
        if (i_and_ns_vars_in_reward) {
            int cleanReward = retrieveMaskReward(a,currS); // Mask reward function
            q = context.apply(cleanReward, q, XADD.SUM);
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
                q = regressBVarsRetrieveMask(q, a, var_to_elim, currS);
            } else if (mdp._hsContIVars.contains(var_to_elim) || mdp._hsContNSVars.contains(var_to_elim)) {
                q = regressCVarsRetrieveMask(q, a, var_to_elim, currS);
            } else {
                // The topological sort will also add in current state and action variables since they were parents in the network
                _logStream.println("- Ignoring current state or action variable " + var_to_elim + " during elimination");
            }
        }
        
        if (DEBUG_DEPTH > RUN_DEPTH){
            debugShow(q, makeXADDLabel("QClean RM after Regress State "+a._sName, curTrial, curDepth), false);
        }
        
        if ( (!i_and_ns_vars_in_reward) && (avars_in_reward) ) {
            int cleanReward = retrieveMaskReward(a,currS); // Mask reward function
            q = context.apply(cleanReward, q, XADD.SUM);
        }
                        
        //Remove Noise Vars
        q = regressNoise(q, a);        
        
        if (DEBUG_DEPTH > RUN_DEPTH){
            debugShow(q, makeXADDLabel("Q RM before Regress Action "+a._sName, curTrial, curDepth), false);
        }
        
        //Remove Action Vars
        if (regressParams){
            q = regressAction(q, a);
        
            if (DEBUG_DEPTH > RUN_DEPTH) {
                debugShow(q, "Q RM After Regress Action" + a._sName + "-" + a._actionParams + "T" + curTrial +"Rem"+curDepth, false);
            }
        }
        if ( (!i_and_ns_vars_in_reward) && (!avars_in_reward) ) {
            int cleanReward = retrieveMaskReward(a,currS); // Mask reward function
            q = context.apply(cleanReward, q, XADD.SUM);
        }
                
        if (regressParams){
            //Apply Mask back to Q
            if (DEBUG_DEPTH > RUN_DEPTH) {
                debugShow(regressionMask, "Final RegressionMask " + a._sName + "-" + a._actionParams + "T" + curTrial +"Rem"+curDepth, false);
                debugShow(q, "Q RM Before Reapply" + a._sName + "-" + a._actionParams + "T" + curTrial +"Rem"+curDepth, false);
            }
            q = context.combine(q, regressionMask);
            if (DEBUG_DEPTH > RUN_DEPTH) {
                debugShow(q, "Q RM After Reapply Mask" + a._sName + "T" + curTrial + " Rem"+curDepth, false);
            }
            // Final NaN to Inf Conversion
            q = context.substituteNode(q, context.NAN, context.POS_INF);
        }
        if (DEBUG_DEPTH > RUN_DEPTH) {
            debugShow(q, "Final Q RM Regress " + a._sName + "T" + curTrial + " Rem"+curDepth, false);
        }
        return mdp.standardizeDD(q);
    }

    private int createMaskedReward(CAction a, State currS) {
        int maskedReward = context.createMask(a._reward,currS._hmBoolVars, currS._hmContVars, context.NAN);
        return maskedReward;
    }
    
    private int retrieveMaskReward(CAction a, State currS) {
        int RUN_DEPTH = 6;
        IntPair rewardMaskPair = context.retrieveMask(a._reward,currS._hmBoolVars, currS._hmContVars, context.NAN);
        int cleanReward = rewardMaskPair._i1;
        regressionMask = context.combine(regressionMask, rewardMaskPair._i2);
        if (DEBUG_DEPTH > RUN_DEPTH){
            debugShow(cleanReward, makeXADDLabel("Clean Reward "+a._sName, curTrial, curDepth), false);
            debugShow(regressionMask, makeXADDLabel("RM after Reward "+a._sName, curTrial, curDepth), false);
        }
        return cleanReward;
    }
    
    public int regressCVarsMask(int q, CAction a, String var, State currS) {
        int RUN_DEPTH = 5;
        if (DEBUG_DEPTH > RUN_DEPTH) debugOutput.println("Regressing Continuous " + var);

        // Get cpf for continuous var'
        int var_id = context._cvar2ID.get(var);
        Integer dd_conditional_sub = a._hmVar2DD.get(var);

        if (DEBUG_DEPTH > RUN_DEPTH){
                debugShow(dd_conditional_sub, "Initial DD_sub" + a._sName + "-" + a._actionParams + "T", false);
        }
        //MaskTransitions not from current state
        dd_conditional_sub = context.createMask(dd_conditional_sub, currS._hmBoolVars, currS._hmContVars, context.NAN);        
        
        if (DEBUG_DEPTH > RUN_DEPTH) debugShow(dd_conditional_sub, "NAN DD_sub "+ a._sName + "-" + a._actionParams + "T", false);        
        if (DEBUG_DEPTH > RUN_DEPTH) debugShow(q, "q before regress "+ a._sName + "-" + a._actionParams + "T", false);
        // Perform regression via delta function substitution
        q = context.reduceProcessXADDLeaf(dd_conditional_sub, context.new DeltaFunctionSubstitution(var, q), true);
        if (DEBUG_DEPTH > RUN_DEPTH) debugShow(q, "q after regress before Mask2 "+ a._sName + "-" + a._actionParams + "T", false);
        //Mask result not from current state
        if (SECOND_MASK) q = context.createMask(q, currS._hmBoolVars, currS._hmContVars, context.NAN);
        if (DEBUG_DEPTH > RUN_DEPTH) debugShow(q, "Final Q "+ a._sName + "-" + a._actionParams + "T", false);
        return q;        
    }

    public int regressCVarsRetrieveMask(int q, CAction a, String var, State currS) {
        int RUN_DEPTH = 5;
        if (DEBUG_DEPTH > RUN_DEPTH) debugOutput.println("Regressing RM Cont " + var +" At "+ a._sName + "-" + a._actionParams);

        // Get cpf for continuous var'
        Integer dd_conditional_sub = a._hmVar2DD.get(var);
        if (DEBUG_DEPTH > RUN_DEPTH){
                debugShow(dd_conditional_sub, "CVar Initial DD_sub " + var, false);
        }
        //MaskTransitions not from current state
        IntPair subPair = context.retrieveMask(dd_conditional_sub, currS._hmBoolVars, currS._hmContVars, context.NAN);
        dd_conditional_sub = subPair._i1;
        regressionMask = context.combine(regressionMask, subPair._i2);
        
        if (DEBUG_DEPTH > RUN_DEPTH) debugShow(dd_conditional_sub, "Cvar clean DD_sub "+ var, false);        
        if (DEBUG_DEPTH > RUN_DEPTH) debugShow(regressionMask, "Cvar Current RM "+ var, false);        
        if (DEBUG_DEPTH > RUN_DEPTH) debugShow(q, "CVar q before regress "+ var, false);

        // Perform regression via delta function substitution
        q = context.reduceProcessXADDLeaf(dd_conditional_sub, context.new DeltaFunctionSubstitution(var, q), true);
       
        //Second Mask!
        //MaskQ not from current state
        IntPair cleanqPair = context.retrieveMask(q, currS._hmBoolVars, currS._hmContVars, context.NAN);
        q = cleanqPair._i1;
        regressionMask = context.combine(regressionMask, cleanqPair._i2);
        if (DEBUG_DEPTH > RUN_DEPTH) debugShow(regressionMask, "Cvar Current RM "+ var, false);        
        
        if (DEBUG_DEPTH > RUN_DEPTH) debugShow(q, "CVar Final Q "+var, false);
        return q;        
    }

    public int regressBVarsMask(int q, CAction a, String var, State currS) {
        int RUN_DEPTH = 5;
        // Get cpf for boolean var'
        if (DEBUG_DEPTH > RUN_DEPTH) debugOutput.println("Regressing Boolean " + var);
        int var_id = context.getVarIndex( context.new BoolDec(var), false);
        Integer dd_cpf = a._hmVar2DD.get(var);
        if (DEBUG_DEPTH > RUN_DEPTH) debugOutput.println("Initial DD_cpf -->: " + context.getString(dd_cpf));
        //MaskTransitions not from current state
        dd_cpf = context.createMask(dd_cpf, currS._hmBoolVars, currS._hmContVars, context.NAN);
        if (DEBUG_DEPTH > RUN_DEPTH) debugOutput.println("NAN DD_cpf -->: " + context.getString(dd_cpf));
        q = context.apply(q, dd_cpf, XADD.PROD);
        if (DEBUG_DEPTH > RUN_DEPTH) debugOutput.println("q after PROD with CPF -->: " + context.getString(q));
        
        // Following is a safer way to marginalize (instead of using opOut
        // based on apply) in the event that two branches of a boolean variable 
        // had equal probability and were collapsed.
        int restrict_high = context.opOut(q, var_id, XADD.RESTRICT_HIGH);
        if (DEBUG_DEPTH > RUN_DEPTH) debugOutput.println(" q High -->: " + context.getString(restrict_high));
        int restrict_low  = context.opOut(q, var_id, XADD.RESTRICT_LOW);
        if (DEBUG_DEPTH > RUN_DEPTH) debugOutput.println(" q Low -->: " + context.getString(restrict_low));
        q = context.apply(restrict_high, restrict_low, XADD.SUM);
        if (DEBUG_DEPTH > RUN_DEPTH) debugOutput.println("q after regress before Mask2 -->: " + context.getString(q));
        if (SECOND_MASK) q = context.createMask(q, currS._hmBoolVars, currS._hmContVars, context.NAN);
        if (DEBUG_DEPTH > RUN_DEPTH) debugOutput.println("Final q after mask2 -->: " + context.getString(q));
        return q;
    }

    public int regressBVarsRetrieveMask(int q, CAction a, String var, State currS) {
        int RUN_DEPTH = 5;
        if (DEBUG_DEPTH > RUN_DEPTH) debugOutput.println("Regressing RM Boolean " + var);
        int var_id = context.getVarIndex( context.new BoolDec(var), false);
        Integer dd_cpf = a._hmVar2DD.get(var);
        if (DEBUG_DEPTH > RUN_DEPTH) debugShow(dd_cpf, "BVar Complete DD_cpf "+ var, false);

        //MaskTransitions not from current state
        IntPair cpfMask = context.retrieveMask(dd_cpf, currS._hmBoolVars, currS._hmContVars, context.NAN);
        dd_cpf = cpfMask._i1;
        regressionMask = context.combine(regressionMask, cpfMask._i2);
        
        if (DEBUG_DEPTH > RUN_DEPTH) debugShow(dd_cpf, "BVar Clean DD_cpf "+ var, false);
        if (DEBUG_DEPTH > RUN_DEPTH) debugShow(regressionMask, "BVar current RM "+ var, false);
        q = context.apply(q, dd_cpf, XADD.PROD);
        if (DEBUG_DEPTH > RUN_DEPTH) debugShow(q, "BVar After DD_cpf prod "+ var, false);
        
        // Safe marginalization.
        int restrict_high = context.opOut(q, var_id, XADD.RESTRICT_HIGH);
        if (DEBUG_DEPTH > RUN_DEPTH) debugShow(restrict_high, "BVar High_q "+ var, false);
        int restrict_low  = context.opOut(q, var_id, XADD.RESTRICT_LOW);
        if (DEBUG_DEPTH > RUN_DEPTH) debugShow(restrict_low, "BVar Low_q "+ var, false);
        q = context.apply(restrict_high, restrict_low, XADD.SUM);
        
        //No need for a second mask in Boolean Variables, there is no substitution to generate decidable decisions.
        if (DEBUG_DEPTH > RUN_DEPTH) debugShow(q, "BVar Final_Q "+ var, false);
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
        if (regressionMask != null) moreSpecialNodes.add(regressionMask);
        if (maxDD != null) moreSpecialNodes.add(maxDD);
        mdp.flushCaches(moreSpecialNodes);
    }
    
    //Plotting
    public String makeXADDLabel(String xadd, int trial, int remH)
    {
        return  xadd+" Trial"+trial+" remainH "+remH+(dApproxError > 0? "-approx"+String.format("%03d",Math.round(1000*dApproxError)): "");
    }

    public String makeXADDLabel(String xadd, int trial)
    {
        return  xadd+" Trial"+trial+(dApproxError > 0? "-approx"+String.format("%03d",Math.round(1000*dApproxError)): "");
    }
    
    private void checkLinearApprox() {
        int RUN_DEPTH=2;
        if (mdp.LINEAR_PROBLEM && APPROXIMATION) {
            for(int i=0; i <nIter;i++) {
            	CAMDP.resetTimer(RUN_DEPTH);
            	int VDD = 0, AppVDD = 0;
            	if (DEBUG_DEPTH >= RUN_DEPTH){
            		VDD = context.getNodeCount(valueDDList[i]);
            		debugShow(valueDDList[i],"Value Before Approx T"+curTrial+" I"+i,true);
            	}
            	valueDDList[i] = context.linUpperPruneRel(valueDDList[i], dApproxError);
            	if (DEBUG_DEPTH >= RUN_DEPTH){
                	AppVDD = context.getNodeCount(valueDDList[i]);
                	debugOutput.println("Approx Trial"+ curTrial+ "iter "+i+" pruning time = " + 
                			CAMDP.getElapsedTime(RUN_DEPTH)	+ " Size reduction = " +
                			(1 - AppVDD*1.0/VDD) + " ( "+VDD+" -> "+AppVDD+" )");
                	debugShow(valueDDList[i],"Value After Approx T"+curTrial+" I"+i,true);
                }
            }
        }
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
        for(int i=0; i<=nIter; i++){
            if (mdp.DISPLAY_3D) save3D(valueDDList[i], String.format(solveMethod+"-Value%d", i) );
            if (mdp.DISPLAY_2D) save2D(valueDDList[i], String.format(solveMethod+"-Value%d", i) );
            saveGraph(valueDDList[i], String.format(solveMethod+"-Value%d", i) );
        }
    }
    public void printResults() {
        debugOutput.println("Results for "+solveMethod+": "+finalTrial + " trials of Depth "+nIter);

        if (COMPLETE_RESULTS){
            debugOutput.print("Time:"); long sumTime = 0;
            for(int i=1;i<=finalTrial;i++){ 
                debugOutput.println();
                for(int j=nIter; j>0; j--) {
                    debugOutput.print(updateTimes[i][j]+" ");
                }
            }     debugOutput.println(";");
            debugOutput.print("Nodes:");
            for(int i=1;i<=finalTrial;i++){ 
                debugOutput.println();
                for(int j=nIter; j>0; j--) debugOutput.print(updateNodes[i][j]+" ");
            }     debugOutput.println(";");
            debugOutput.print("Current State Value:");
            for(int i=1;i<=finalTrial;i++){ 
                debugOutput.println();
                for(int j=nIter; j>0; j--) debugOutput.print(updateIniVals[i][j]+" ");
            }     debugOutput.println(";");
        }
        debugOutput.print("Time per Trial:"); for(int i=1; i<=finalTrial; i++) debugOutput.print(solutionTimeList[i]+" ");debugOutput.println(";");
        debugOutput.print("Nodes per Trial:"); for(int i=1; i<=finalTrial; i++) debugOutput.print(solutionNodeList[i]+" ");debugOutput.println(";");
        debugOutput.print("Initial S Value:"); for(int i=1; i<=finalTrial; i++) debugOutput.print(solutionInitialSValueList[i]+" ");debugOutput.println(";");
    }

{
// unclearUsability test RegionBellmanBackup
//if (TEST_DETERMINISTIC_REGRESSION){
//      State nextS = sample(currS, new ParametrizedAction(me.getValue(), maxParam));
//      Double rewValue = mdp.evaluateState(me.getValue()._reward, currS);
//      Double newValue = getStateVal(nextS, curDepth-1);                    
//      if (Math.abs(value - (rewValue + newValue) ) > XADD.PRECISION){
//          debugOutput.println("Regress Action: " + me.getValue()._sName + " RValue:" + rewValue +" NValue = "+newValue+" Tvalue = "+ (rewValue+newValue) +" RegrValue"+value);
//          System.err.println("Regression value failure!");
//          mdp.evaluateState(regr, currS);
//          PRINT_DD = true;
//          PLOT_DD = true;
//          REGRESS_DEBUG = true;
//          if (PLOT_DD) mdp.doDisplay(regr, "Q"+me.getValue()._sName+"T"+curTrial+"H"+curDepth);
//          regressRegion(valueDDList[curDepth-1],me.getValue(), currS, true);
//      }
//  }
  }

}