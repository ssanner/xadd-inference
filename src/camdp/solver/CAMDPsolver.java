package camdp.solver;

import graph.Graph;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import camdp.CAMDP;
import camdp.CAction;
import camdp.CAMDP.FileOptions;
import camdp.State;
import util.DevNullPrintStream;
import xadd.XADD;
import xadd.XADDUtils;
import xadd.XADD.XADDLeafMinOrMax;

public abstract class CAMDPsolver {
    
    public static CAMDP mdp;
    public static XADD context;
    public Integer valueDD;
    public int nIter;
    public Integer curIter;
    public String solveMethod = null;
    public double dApproxError = 0.0d;
    
    /*General Solution Parameter*/
    public final boolean ENABLE_EARLY_CONVERGENCE = false;
    public final boolean CALCULATE_GREEDY_ACTION = false;
    public final double STATE_PRECISION = 1e-12;
    
    /* Approximation Parameters */
    public boolean APPROXIMATION = true;
    public boolean APPROX_ALWAYS = false;
    public boolean COMPARE_OPTIMAL = false;
    
    /* DEBUG PARAMETER */
    protected static int DEBUG_DEPTH = 0;
    protected static PrintStream debugOutput = System.out;
    private static boolean PLOT_DD = false;
    private static boolean PRINT_DD = false;
    protected static boolean PERFORMANCE_DEBUG = false;
    
    /* For printing */
    public final static String RESULTS_DIR = "./results"; // Diagnostic output destination
    public String OUTPUT_DIR = null; // complete output destination
    public static DecimalFormat _df = new DecimalFormat("#.########");
    public PrintStream _logStream = null;
    public PrintStream _resultStream = null;    
    public final static String ASCII_BAR = "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%"; // Display shortcut
    private static final int MAX_GRAPH_NODES = 700;
    private static final boolean SHOW_ABSENT_PARAMS = false;
    
    /* Time & Memory Management  Moved to CAMDP*/ 
//    public final static int nTimers = 8;
//    public long[] _lTime = new long[nTimers];
    //public Runtime RUNTIME = Runtime.getRuntime();
    
    /*Solution maintenance */
    public int[] solutionDDList = null;
    public long[] solutionTimeList = null;
    public int[] solutionNodeList = null;
    public double[] solutionInitialSValueList = null;
    public double[] solutionMaxValueList = null;
    
    /* *********************** Methods *********************** */
    public void setApproxTest(double eps, PrintStream log, boolean always) {
        dApproxError = eps;
        _resultStream = log;
        APPROX_ALWAYS = always;
        COMPARE_OPTIMAL = true;
    }
    
    //    Common Solution methods
    public abstract int solve();
    public abstract ParametrizedAction getGreedyAction(int valueDD, State s);

    //Regression functions 
    public Graph buildDBNDependencyDAG(CAction a, HashSet<String> vars) {
        // Works backward from this root factor
        Graph g = new Graph(true, false, true, false);
        HashSet<String> already_seen = new HashSet<String>();
        
        // We don't want to generate parents for the following "base" variables
        already_seen.addAll( mdp._hsContSVars );
        already_seen.addAll( mdp._hsBoolSVars );
        already_seen.addAll( mdp._hsContAVars ); 
        already_seen.addAll( mdp._hsNoiseVars ); 
        
        for (String var : vars)
            buildDBNDependencyDAGInt(a, var, g, already_seen);
        
        return g;
    }
    public void buildDBNDependencyDAGInt(CAction a, String parent_var, Graph g, HashSet<String> already_seen) {
        // Consider that vars belong to a parent factor, recursively call
        // for every child factor and link child to parent
        // 
        // have R(x1i,b1i,x2'), DAG has (b1i -> x1i -> R), (b1i -> R), (x2' -> R)... {x1i, b1i, x2'}
        // recursively add in parents for each of x2', xli, bli

        if (already_seen.contains(parent_var))
            return;
        already_seen.add(parent_var);
        g.addNode(parent_var);
        Integer dd_cpf = a._hmVar2DD.get(parent_var);
        if (dd_cpf == null) {
            System.err.println("Could not find CPF definition for variable '" + parent_var + 
                    "' while regressing action '" + a._sName + "'");
            System.exit(1);
        }
        HashSet<String> children = context.collectVars(dd_cpf);
        for (String child_var : children) {
            // In the case of boolean variables, the dual action diagram contains the parent,
            // because this is not a substitution, it is a distribution over the parent.
            // Hence we need to explicitly prevent boolean variable self-loops -- this is not
            // an error.
            if (!child_var.equals(parent_var) || mdp._hsContIVars.contains(parent_var) || mdp._hsContNSVars.contains(parent_var)) {
                g.addUniLink(child_var, parent_var);
                //System.out.println("Adding link " + child_var + " --> " + parent_var);
            } 
            buildDBNDependencyDAGInt(a, child_var, g, already_seen);
        }
    }
    protected void displayCyclesAndExit(Graph g, CAction a) {
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
    
    protected int regressNoise(int q, CAction a) {
        //If no Noise vars do nothing
        if (a._noiseVars.size() == 0) {
            return q;
        }
        
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
            flushCaches(Arrays.asList(q) /* additional node to save */);
        }
        _logStream.println("- Done noise parameter minimization");
        _logStream.println("- Q^" + curIter + "(" + a._sName + " )" + context.collectVars(q) + "\n" + context.getString(q));
        return q;
    }
    protected int regressAction(int q, CAction a) {
        int RUN_DEPTH = 5;
        if (a._actionParams.size() == 0) {// No action params to maximize over            
            return q;
        }
        // Max out each action param in turn
        HashSet<String> q_vars = context.collectVars(q);
        for (int i=0; i < a._actionParams.size(); i++) 
        {
            String avar = a._actionParams.get(i);
            double lb   = a._hmAVar2LB.get(avar);
            double ub   = a._hmAVar2UB.get(avar);

            if (!q_vars.contains(avar)){
                if (DEBUG_DEPTH > RUN_DEPTH && SHOW_ABSENT_PARAMS) 
                    debugOutput.println("- Skipping var '" + avar + "': [" + lb + "," + ub + "], which does not occur in q: " + context.collectVars(q));
                continue;
            }
            if (DEBUG_DEPTH > RUN_DEPTH){
                debugOutput.println("- Maxing out action param '" + avar + "': [" + lb + "," + ub + "]");
                debugShow(q, "Q BeforeMaxParam "+avar+" "+a._sName, false);
            }
            q = maxOutVar(q, avar, lb, ub);
            if (DEBUG_DEPTH > RUN_DEPTH){
                debugOutput.println("- Maxing out action param '" + avar + "': [" + lb + "," + ub + "]");
                debugShow(q, "Q AfterMaxParam "+avar+" "+a._sName, false);
            }
            // Can be computational expensive (max-out) so flush caches if needed
            flushCaches(Arrays.asList(q) /* additional node to save */);
        }
        return q;
    }
    public HashSet<String> filterIandNSVars(HashSet<String> vars, boolean allow_cont, boolean allow_bool) {
        HashSet<String> filter_vars = new HashSet<String>();
        for (String var : vars)
            if (allow_cont && 
                (mdp._hsContIVars.contains(var) ||
                 mdp._hsContNSVars.contains(var)))
                filter_vars.add(var);
            else if (allow_bool &&
                (mdp._hsBoolIVars.contains(var) ||
                 mdp._hsBoolNSVars.contains(var)))
                filter_vars.add(var);
        return filter_vars;
    }
    public HashSet<String> filterAVars(HashSet<String> vars) {
        HashSet<String> filter_vars = new HashSet<String>();
        for (String var : vars)
                if (mdp._hsContAVars.contains(var)) filter_vars.add(var);
        return filter_vars;
    }
    public int maxOutVar(int ixadd, String var, double lb, double ub) {
        XADD.XADDLeafMinOrMax max = context.new XADDLeafMinOrMax(var, lb, ub, true /* is_max */, _logStream);
        ixadd  = context.reduceProcessXADDLeaf(ixadd, max, false);
        return max._runningResult;
    }
    public int minOutVar(int ixadd, String var, double lb, double ub) {
        XADD.XADDLeafMinOrMax min = context.new XADDLeafMinOrMax(var, lb, ub, false /* is_max */, _logStream);
        ixadd  = context.reduceProcessXADDLeaf(ixadd, min, false);
        return min._runningResult;
    }

//    protected Integer checkLinearAndApprox(Integer dd) {
//        if ( mdp.LINEAR_PROBLEM && APPROX_ALWAYS)
//            dd = context.linPruneRel(dd, dApproxError);
//        return dd;
//    }

    ////////// Space Management ///////////////////////////
    public void flushCaches(){
        flushCaches(new ArrayList<Integer>());
    }
    public abstract void flushCaches(List<Integer> specialNodes);
    
    // Debugging Management
    public static void debugSetUp(int verb, boolean plot){
        DEBUG_DEPTH = verb;
        PLOT_DD = plot;
    }
    public static void debugShow(int dd, String name, boolean plotValue){
        if (PLOT_DD) {
            if (plotValue) mdp.doDisplay(dd, name);
            else mdp.displayGraph(dd, name);
        }
        if (PRINT_DD) debugOutput.println(name+" DD_ID="+dd+":\n"+context.getExistNode(dd));
    }

    
    // Results
    public void makeResultStream(){

        String fullFile = mdp._problemFile;
        String[] filenameList = fullFile.split("/");
        int nwords = filenameList.length;
        String problemName = filenameList[nwords-1].substring(0,(filenameList[nwords-1]).length() -5);
        String className = filenameList[nwords-2];
        OUTPUT_DIR = RESULTS_DIR +"/"+ className +"/"+problemName;
        
        //System.out.println("testing filename:" + OUTPUT_DIR + "/" + _solveMethod + ".rslt");
        try{
            new File(OUTPUT_DIR).mkdirs();
            _resultStream = new PrintStream(new FileOutputStream(OUTPUT_DIR+"/"+solveMethod+".log"));
        }
        catch (FileNotFoundException e){
            System.err.println("Couldn't create result Stream for: "+OUTPUT_DIR+"/"+solveMethod+"\nException:"+e);
        }
    }
    public String makeResultFile(int iter){
        return OUTPUT_DIR+"/"+solveMethod+iter+".xadd";
    }

    public abstract void setupResults();
    public abstract void printResults();
    public abstract void saveResults();
        
    ////////////Result Storage Methods /////////////////////
    public void save3D(int xadd_id, String label) {
        // If DISPLAY_3D is enabled, it is expected that necessary parameters
        // have been placed in a _problemFile + ".3d"
        FileOptions opt = new FileOptions(mdp._problemFile + ".3d");
        if ( opt == null){
            System.err.println("ERROR in display 3D:Couldn't open"+mdp._problemFile + ".3d");
            return;
        }
        double low_x = opt._varLB.get(0);
        double inc_x = opt._varInc.get(0);
        double high_x = opt._varUB.get(0);
        double low_y = opt._varLB.get(1);
        double inc_y = opt._varInc.get(1);
        double high_y = opt._varUB.get(1);
        int nSamples = (int) Math.ceil( (high_x - low_x)/inc_x);
        
        HashMap<String,Boolean> static_bvars = opt._bassign;
        HashMap<String, Double> static_dvars = opt._dassign;
        
        String xVar = opt._var.get(0);
        String yVar = opt._var.get(1);
        
        ArrayList<Float> alX = new ArrayList<Float>(nSamples);
        float temp_x = (float) low_x;
        for (int i = 0; i < nSamples; i++) {
            temp_x += inc_x;
            alX.add(temp_x);
        }
        ArrayList<Float> alY = new ArrayList<Float>(nSamples);
        float temp_y = (float) low_y;
        for (int i = 0; i < nSamples; i++) {
            temp_y += inc_y;
            alY.add(temp_y);
        }
        if (alX.size() != alY.size()) {
            System.err.println("ERROR: Surface plotting requires the same number of samples along the x and y axes");
            return;
        }

        PrintStream ps = null;
        String filename = OUTPUT_DIR+"/"+ label +".txt";
        try {
            ps = new PrintStream(new FileOutputStream(filename));
        } catch (Exception e) {
            System.err.println("Could not open " + filename + " for data export.");
            ps = new DevNullPrintStream();
        }

        static_dvars = new HashMap<String, Double>(static_dvars);
        for (int i = 0; i < alX.size(); i++) {
            for (int j = 0; j < alY.size(); j++) {

                float x = alX.get(i);
                float y = alY.get(j);
                static_dvars.put(xVar, (double) x);
                static_dvars.put(yVar, (double) y);
                //System.out.println("DD: "+xadd_id+ " Vars"+static_bvars+"d"+static_dvars);
                //context.evaluate(xadd_id, static_bvars, static_dvars);
                float z = context.evaluate(xadd_id, static_bvars, static_dvars).floatValue();
                if (Float.isInfinite(z)) z = Float.NaN;
                static_dvars.remove(xVar);
                static_dvars.remove(yVar);

                //if (z > 0.1d)
                //    System.out.println("f(" + x + "," + y + ") = " + z);
                ps.println(x + "\t" + y + "\t" + z);
            }
            ps.println();
        }
        ps.close();
    }
    public void save2D(int xadd_id, String label) {
        // If DISPLAY_2D is enabled, it is expected that necessary parameters
        // have been placed in a _problemFile + ".2d"
        FileOptions opt = new FileOptions(mdp._problemFile + ".2d");
        if ( opt == null){
            System.err.println("ERROR in display 2D:Couldn't open"+mdp._problemFile + ".2d");
            return;
        }
        double low_x = opt._varLB.get(0);
        double inc_x = opt._varInc.get(0);
        double high_x = opt._varUB.get(0);
        int nSamples = (int) Math.ceil( (high_x - low_x)/inc_x);        
        HashMap<String,Boolean> static_bvars = opt._bassign;
        HashMap<String, Double> static_dvars = opt._dassign;
        String xVar = opt._var.get(0);
        
        ArrayList<Float> alX = new ArrayList<Float>(nSamples);
        float temp_x = (float) low_x;
        for (int i = 0; i < nSamples; i++) {
            temp_x += inc_x;
            alX.add(temp_x);
        }
        PrintStream ps = null;
        String filename = OUTPUT_DIR+"/"+ label +".txt";
        try {
            ps = new PrintStream(new FileOutputStream(filename));
        } catch (Exception e) {
            System.err.println("Could not open " + filename + " for data export.");
            ps = new DevNullPrintStream();
        }

        static_dvars = new HashMap<String, Double>(static_dvars);
        for (int i = 0; i < alX.size(); i++) {
                float x = alX.get(i);
                static_dvars.put(xVar, (double) x);
                float z = context.evaluate(xadd_id, static_bvars, static_dvars).floatValue();
                if (Float.isInfinite(z)) z = Float.NaN;
                static_dvars.remove(xVar);
                ps.println(x + "\t" + z);
        }
        ps.close();
    }
    public void saveGraph(int xadd_id, String label) {
        Graph g;
        int nNodes = context.getNodeCount(xadd_id);
        if (nNodes < MAX_GRAPH_NODES)
            g = context.getGraph(xadd_id);
        else {
            g = new Graph();
            g.addNode("_warn_");
            g.addNodeLabel("_warn_", nNodes+" Nodes: Too many to plot.");
            g.addNodeShape("_warn_", "box");
            g.addNodeStyle("_warn_", "filled");
            g.addNodeColor("_warn_", "red1");
        }
        g.addNode("_temp_");
        g.addNodeLabel("_temp_", label);
        g.addNodeShape("_temp_", "square");
        g.addNodeStyle("_temp_", "filled");
        g.addNodeColor("_temp_", "gold1");       
        String filename = OUTPUT_DIR+"/"+ label +".dot";
        g.genDotFile(filename);
    }
}