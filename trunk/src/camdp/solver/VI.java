package camdp.solver;

import graph.Graph;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import util.IntTriple;
import xadd.ExprLib.ArithExpr;
import xadd.ExprLib.DoubleExpr;
import xadd.ExprLib.OperExpr;
import xadd.LinearXADDMethod.NamedOptimResult;
import xadd.XADD;
import xadd.XADD.XADDLeafMinOrMax;
import camdp.CAMDP;
import camdp.CAction;
import camdp.State;

public class VI extends CAMDPsolver {

    public Integer curIter;   // Current iteration for Value Iteration
    public Integer finalIter;   // Last Iteration in case of early Convergence
    public Integer maxDD; //Current Max during a Bellman Backup
        
    
    //////////////////Methods /////////////////////////////////
    
    public VI(CAMDP camdp, int iter){
    	this(camdp,iter, 0d);
    }

    public VI(CAMDP camdp, int iter, double approxError){
        mdp = camdp;
        context = camdp._context;
        valueDD = context.NEG_INF;
        _logStream = camdp._logStream;
        if (approxError == 0d)
        	APPROXIMATION = false;
        solveMethod = APPROXIMATION? "BASDP":"SDP";
        makeResultStream();
        nIter = iter;
        dApproxError = approxError;
        setupResults();
    }   
    
    
    ////////Main Solver Class ///////////////
    public int solve(){       
        int RUN_DEPTH = 1;
        if (DEBUG_DEPTH > RUN_DEPTH) debugOutput.println("Starting "+solveMethod+ " solution, Max #Iterations = " + nIter+"\n");
        Integer _prevDD = null;
        //Iteration counter
        curIter = 0;        

        //Initialize value function to zero
        valueDD = context.ZERO;

        // Perform value iteration for specified number of iterations, or until convergence detected
        while (curIter < nIter) 
        {
            ++curIter;
            CAMDP.resetTimer(RUN_DEPTH);
            // Prime diagram
            _prevDD = valueDD;
            bellmanBackup();
            
            checkLinearApprox(); //Approximation at the end of Iter

            solutionDDList[curIter] = valueDD;
            solutionTimeList[curIter] = CAMDP.getElapsedTime(RUN_DEPTH) + (curIter >1? solutionTimeList[curIter-1]:0);
            solutionNodeList[curIter] = context.getNodeCount(valueDD);
            //if (mdp.LINEAR_PROBLEM) solutionMaxValueList[curIter] = context.linMaxVal(valueDD);
            if( mdp._initialS != null) solutionInitialSValueList[curIter] = mdp.evaluateInitialS(valueDD);            

            if (DEBUG_DEPTH > RUN_DEPTH){
                debugOutput.println("Iter:" + curIter+" Complete");
                debugOutput.println("Value DD:"+valueDD+" Nodes= "+solutionNodeList[curIter]+" Time ="+solutionTimeList[curIter]);
                if (CALCULATE_GREEDY_ACTION) debugOutput.println("Greedy Action ="+getGreedyAction(valueDD, mdp._initialS));
                if( mdp._initialS != null) debugOutput.println("Initial State Value = "+solutionInitialSValueList[curIter]);
                debugShow(valueDD, makeXADDLabel("V",curIter, dApproxError), true);
                debugOutput.println();
            }
            
            if (ENABLE_EARLY_CONVERGENCE && _prevDD.equals(valueDD) ) {
                if (DEBUG_DEPTH > RUN_DEPTH) debugOutput.println("\nVI: Converged to solution early,  at iteration "+curIter);
                break;
            }
        }
        flushCaches();    
        finalIter = curIter;
        return finalIter;
    }
    
    public ParametrizedAction getGreedyAction(int valDD, State s){
        
//        if (DEEP_DEBUG) debugOutput.println("\nVI: get Policy Start\n");
        double maxVal = Double.NEGATIVE_INFINITY;
        CAction greedyAction = null;
        HashMap<String, Double> greedyParams = null;
        for (Map.Entry<String,CAction> me : mdp._hmName2Action.entrySet()) {
//             if (MAIN_DEBUG) debugOutput.println("Regressing Action:"+me.getKey());
            // Regress the current value function through each action (finite number of continuous actions)
            CAction a = me.getValue();
            int q = regress(valDD, a, false);
            int contQbool = context.substituteBoolVars(q, s._hmBoolVars);
            HashMap<String, ArithExpr> subs = new HashMap<String, ArithExpr>();
            for (Entry<String, Double> pair: s._hmContVars.entrySet()){
                subs.put(pair.getKey(), new DoubleExpr(pair.getValue()) );
            }
            int localQ = context.substitute(contQbool, subs);
//            mdp.displayGraph(q, "Original Q");
//            mdp.displayGraph(contQbool, "Only Cont Q");
//            mdp.displayGraph(localQ, "LocalQ");
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
        return new ParametrizedAction(greedyAction, greedyParams);    }
    
    private void checkLinearApprox() {
        int RUN_DEPTH=2;
        if (mdp.LINEAR_PROBLEM && APPROXIMATION) {
            CAMDP.resetTimer(RUN_DEPTH);
            int VDD = 0, AppVDD = 0;
            if (DEBUG_DEPTH >= RUN_DEPTH){
            	 VDD = context.getNodeCount(valueDD);
            	 debugShow(valueDD,"Value Before Approx Iter "+curIter,true);
            }
            valueDD = context.linPruneRel(valueDD, dApproxError);
            if (DEBUG_DEPTH >= RUN_DEPTH){
            	AppVDD = context.getNodeCount(valueDD);
            	debugOutput.println("Approx Finish"+ curIter+ " pruning time = " + 
            			CAMDP.getElapsedTime(RUN_DEPTH)	+ " Size reduction = " +
            			(1 - AppVDD*1.0/VDD) + " ( "+VDD+" -> "+AppVDD+" )");
            	debugShow(valueDD,"Value After Approx Iter "+curIter, true);
            }
            //displayGraph(_valueDD, "valPruned-" + _nCurIter+" e"+APPROX_ERROR);
        }
    }

    private void bellmanBackup() {
        int RUN_DEPTH=2;
        
        maxDD = null;
        // Iterate over each action
        for (Map.Entry<String,CAction> me : mdp._hmName2Action.entrySet()) {
            CAMDP.resetTimer(RUN_DEPTH);

            // Regress the current value function through each action (finite number of continuous actions)
            int regr = regress(valueDD, me.getValue(), true);
            
            if (DEBUG_DEPTH > RUN_DEPTH){
                debugOutput.println("Bellman Backup "+curIter+" Action "+me.getKey()+" Regr Time = "+CAMDP.getElapsedTime(RUN_DEPTH));
                debugShow(regr, "DD of regressing "+me.getKey()+"^"+curIter, true);
            }
            // Maintain running max over different actions
            maxDD = (maxDD == null) ? regr : context.apply(maxDD, regr, XADD.MAX);
            maxDD = mdp.standardizeDD(maxDD); // MakeCanonical and ReduceLP pruning.

            if (DEBUG_DEPTH > RUN_DEPTH){
                debugOutput.println("Bellman Backup "+curIter+" Action "+me.getKey()+" Regr Time = "+CAMDP.getElapsedTime(RUN_DEPTH));
                debugShow(maxDD, "Max DD After "+me.getKey()+"^"+curIter, true);
                if (mdp._initialS != null){
                    double iniSVal = mdp.evaluateInitialS(regr);
                    debugOutput.println("InitialSValue for Action "+me.getValue()._sName+" = "+iniSVal);
                }
            }
            //Optional post-max approximation, can be used if overall error is being monitored 
            if (APPROX_ALWAYS) maxDD = mdp.approximateDD(maxDD);
            flushCaches();
        }
        valueDD = maxDD;
    }
        
    /**
     * Regress a DD through an action
     **/
    public int regress(int vfun, CAction a, boolean regressCont) {
        int RUN_DEPTH=3;
        // Prime the value function 
        int q = context.substitute(vfun, mdp._hmPrimeSubs); 
        if (DEBUG_DEPTH > RUN_DEPTH){
            debugOutput.println("REGRESSING ACTION " + a._sName + " Iter "+ curIter );
            debugShow(q, "Q Start "+a._sName+"^"+curIter, false);
        }
        
        // Discount
        if (mdp._bdDiscount.doubleValue() != 1){
            debugOutput.println("Warning: Using discount on Finite Horizon");
            q = context.scalarOp(q, mdp._bdDiscount.doubleValue(), XADD.PROD);
        }
        
        // Add reward *if* it contains primed vars that need to be regressed
        HashSet<String> i_and_ns_vars_in_reward = filterIandNSVars(context.collectVars(a._reward), true, true);
        if (!i_and_ns_vars_in_reward.isEmpty()) {
            q = context.apply(a._reward, q, XADD.SUM); // Add reward to already discounted primed value function
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
                q = regressBVars(q, a, var_to_elim);
            } else if (mdp._hsContIVars.contains(var_to_elim) || mdp._hsContNSVars.contains(var_to_elim)) {
                q = regressCVars(q, a, var_to_elim);
            }
        }

        if (DEBUG_DEPTH > RUN_DEPTH){
            debugShow(q, "Q after Regressing "+a._sName+"^"+curIter, false);
        }
        if (i_and_ns_vars_in_reward.isEmpty()) {
            q = context.apply(a._reward, q, XADD.SUM);
        }
        
        //Continuous Noise
        q = regressNoise(q,a);
        //Continuous Parameter
        if (regressCont){
            q = regressAction(q, a);
        }
        //Final Display
        if (DEBUG_DEPTH > RUN_DEPTH){
            debugShow(q, "Before Standard Q "+a._sName+"^"+curIter, false);
        }
        q = mdp.standardizeDD(q);

        //Final Display
        if (DEBUG_DEPTH > RUN_DEPTH){
            debugShow(q, "Final Q "+a._sName+"^"+curIter, false);
        }
        return q;
    }
    
    
    private IntTriple _contRegrKey = new IntTriple(-1,-1,-1); //Temporary variable for key
    public int regressCVars(int q, CAction a, String var) {
        int RUN_DEPTH = 4;
        // Get cpf for continuous var'
        int var_id = context._cvar2ID.get(var);
        Integer dd_conditional_sub = a._hmVar2DD.get(var);

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
        
        if (DEBUG_DEPTH > RUN_DEPTH) {
            debugOutput.println("Integrating out: "+var+" at regression of "+a._sName+"^"+curIter);
            debugShow(q, "Q after integrating out "+var, false);
        }
        return q;        
    }

    public int regressBVars(int q, CAction a, String var) {
        int RUN_DEPTH = 4;
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
        if (maxDD != null)  moreSpecialNodes.add(maxDD);
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
//        solutionMaxValueList = new double[nIter+1];
    }

    public void saveResults(){
        //Results: NIter, Time, Nodes, InitialS Value.
        for(int i=1; i<=nIter; i++){
        _resultStream.format("%d %f %d %f\n", i, solutionTimeList[i]/1000.0, solutionNodeList[i], (mdp._initialS != null) ? solutionInitialSValueList[i]: 0d);
        }
        for(int i=1; i<=nIter; i++){
            if (mdp.DISPLAY_3D) save3D(solutionDDList[i], String.format(solveMethod+"-Value%d", i) );
            if (mdp.DISPLAY_2D) save2D(solutionDDList[i], String.format(solveMethod+"-Value%d", i) );
            saveGraph(solutionDDList[i], String.format(solveMethod+"-Value%d", i) );
        }
    }

    public void printResults() {
        debugOutput.println("Results for Value Iteration: " + finalIter + " iterations:");
        debugOutput.print("Time:"); for(int i=1; i<=finalIter; i++) debugOutput.print(solutionTimeList[i]+" ");debugOutput.println(";");
        debugOutput.print("Nodes:"); for(int i=1; i<=finalIter; i++) debugOutput.print(solutionNodeList[i]+" ");debugOutput.println(";");
        debugOutput.print("Initial S Value:"); for(int i=1; i<=finalIter; i++) debugOutput.print(solutionInitialSValueList[i]+" ");debugOutput.println(";");
    }

    public void exportSolutionToFile() {
        for(int i=1; i<=finalIter; i++) context.exportXADDToFile(solutionNodeList[i], makeResultFile(i));
    }
}


