package camdp;

// Packages to import

import graph.Graph;

import java.io.*;
import java.util.*;

import java.math.BigDecimal;
import java.text.DecimalFormat;

import util.IntTriple;
import xadd.XADD;
import xadd.XADDUtils;
import xadd.ExprLib.*;

/**
 * Main Continuous State and Action MDP (CAMDP) dynamic programming solution class
 * (handles discrete actions, continuous actions, and continuous noise)
 *
 * @author Zahra Zamani
 * @author Scott Sanner
 * @version 1.0
 * @language Java (JDK 1.5)
 * <p/>
 * TODO: Reintroduce policy annotation
 * TODO: Allow alternate initialization of V^0 in input file
 * TODO: Action and next-state dependent reward expectations can be pre-computed and added
 * in after value function regression but before continuous parameter maximization.
 */
public class CAMDP {

    /* Constants */
    public final static int MAXIMUM_ITER = 10000;
    public final static String RESULTS_DIR = "results"; // Diagnostic output destination

    // Display Flags
    public final static boolean DISPLAY_PREMAX_Q = false;
    public final static boolean DISPLAY_POSTMAX_Q = false;
    public final static boolean DISPLAY_V = true;
    public final static boolean DISPLAY_MAX = false;
    private static final boolean SILENT_PLOT = true;
    private static final boolean DONT_SHOW_HUGE_GRAPHS = true;
    private static final int MAXIMUM_XADD_DISPLAY_SIZE = 500;
    public static final boolean SILENCE_ERRORS_PLOTS = false;
    
    //Prune and Linear Flags
    public double maxImediateReward;
    public boolean LINEAR_PROBLEM = true;
    public boolean CONTINUOUS_ACTIONS = true;
    public boolean APPROX_PRUNING = false;
    public double APPROX_ERROR = 0.0d;
    public boolean APPROX_ALWAYS = false;
    public boolean COMPARE_OPTIMAL = false;
    public boolean DISCRETIZE_PROBLEM = false;
    public int DISCRETE_NUMBER = 11;
    public int GLOBAL_LB = -9;
    public int GLOBAL_UB = 9;

    //Optimal solution maintenance
    public Integer optimalHorizon;
    public ArrayList<Integer> optimalDDList = new ArrayList<Integer>();
    public ArrayList<Double> optimalMaxValueList = new ArrayList<Double>();

    /* Maintain an explicit policy? */
    public final static boolean MAINTAIN_POLICY = false;

    /* Cache maintenance */
    // Unused FLAG? public final static boolean ALWAYS_FLUSH = false; // Always flush DD caches?
    public final static double FLUSH_PERCENT_MINIMUM = 0.3d; // Won't flush until < amt

    /* For printing */
    public static DecimalFormat _df = new DecimalFormat("#.########");
    public PrintStream _logStream = null;
    public PrintStream _testLogStream = null;

    /* Static variables */
    public static long _lTime; // For timing purposes
    public static Runtime RUNTIME = Runtime.getRuntime();
    public final static int nTimers = 8;
    public static long[] _lTimers = new long[nTimers];
    private static final boolean EFFICIENCY_DEBUG = false;

    /* Local vars */
    public boolean DISPLAY_2D = false;
    public boolean DISPLAY_3D = true;

    public String _problemFile = null;
    public String _logFileRoot = null;
    public XADD _context = null;
    public HashSet<String> _hsBoolSVars;
    public HashSet<String> _hsContSVars;
    public HashSet<String> _hsBoolIVars;
    public HashSet<String> _hsContIVars;
    public HashSet<String> _hsContAVars;

    public HashSet<String> _hsNoiseVars;

    public HashSet<String> _hsBoolNSVars; // Next state vars
    public HashSet<String> _hsContNSVars; // Next state vars

    public HashSet<String> _hsBoolAllVars; // Retain order given in MDP file
    public HashSet<String> _hsContAllVars; // Retain order given in MDP file

    public Integer _valueDD; // The resulting value function once this CMDP has
    public Integer _maxDD;
    public Integer _prevDD;
    public BigDecimal _bdDiscount; // Discount (gamma) for CMDP
    public Integer _nMaxIter;   // Number of iterations for CMDP
    public Integer _nCurIter;   // Number of iterations for CMDP

    public HashMap<String, ArithExpr> _hmPrimeSubs;
    public HashMap<String, CAction> _hmName2Action;
    public HashMap<IntTriple, Integer> _hmContRegrCache;

    // Constraints not currently allowed, should be applied to the reward as -Infinity
    //public ArrayList<Integer>         _alConstraints;

    public ComputeQFunction _qfunHelper = null;

    public State _initialS = null;

    ////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Constructor - filename
     */
    public CAMDP(String filename) {
        this(filename, HierarchicalParser.ParseFile(filename));
    }

    /**
     * Constructor - pre-parsed file
     */
    private CAMDP(String file_source, ArrayList input) {

        // Basic initializations
        _problemFile = file_source;
        _logFileRoot = insertDirectory(_problemFile, RESULTS_DIR);
        _context = new XADD();
        _prevDD = _maxDD = _valueDD = null;
        _bdDiscount = new BigDecimal("" + (-1));
        _nMaxIter = null;
        _hmName2Action = new HashMap<String, CAction>();

        // Setup CAMDP according to parsed file contents
        ParseCAMDP parser = new ParseCAMDP(this);
        parser.buildCAMDP(input);
        _context.addContinuousVarsBounds(parser._minCVal, parser._maxCVal);
        //_alConstraints = parser.getConstraints();
        _nMaxIter = parser.getIterations();
        _bdDiscount = parser.getDiscount();
        _hmName2Action = parser.getHashmap();
        _hmContRegrCache = new HashMap<IntTriple, Integer>();


        // Setup variable sets and lists
        _hsBoolSVars = new HashSet<String>(intern(parser.getBVars()));
        _hsContSVars = new HashSet<String>(intern(parser.getCVars()));
        _hsBoolIVars = new HashSet<String>(intern(parser.getIBVars()));
        _hsContIVars = new HashSet<String>(intern(parser.getICVars()));
        _hsContAVars = new HashSet<String>(intern(parser.getAVars()));
        _hsNoiseVars = new HashSet<String>(intern(parser.getNoiseVars()));
        _hsBoolAllVars = new HashSet<String>(_hsBoolSVars);
        _hsBoolAllVars.addAll(_hsBoolIVars);
        _hsContAllVars = new HashSet<String>(_hsContSVars);
        _hsContAllVars.addAll(_hsContAVars);
        _hsContAllVars.addAll(_hsContIVars);
        //_context._hmContinuousVars = _alContAllVars;
        // Build cur-state var -> next-state var map
        _hsBoolNSVars = new HashSet<String>();
        _hsContNSVars = new HashSet<String>();
        _hmPrimeSubs = new HashMap<String, ArithExpr>();
        for (String var : _hsContSVars) {
            String prime_var = var + "'";
            _hmPrimeSubs.put(var, new VarExpr(prime_var));
            _hsContNSVars.add(prime_var);
        }
        for (String var : _hsBoolSVars) {
            String prime_var = var + "'";
            _hmPrimeSubs.put(var, new VarExpr(prime_var));
            _hsBoolNSVars.add(prime_var);
        }

        CONTINUOUS_ACTIONS = _hsContAVars.isEmpty()? false: true;
        LINEAR_PROBLEM = parser.LINEARITY;
        maxImediateReward = parser.MAXREWARD;
        
        // This helper class performs the regression
        _qfunHelper = new ComputeQFunction(_context, this);
        if ( !parser.get_initBVal().isEmpty() || !parser.get_initCVal().isEmpty() )_initialS = new State(parser.get_initCVal(), parser.get_initBVal());

        // Setup a logger
        try {
            _logStream = new PrintStream(new FileOutputStream(/*"timeSpace.txt"));*/_logFileRoot + ".log"));
            _logStream.println(this.toString());
            //Default log to stdout
            _testLogStream = System.out;
        } catch (FileNotFoundException e) {
            System.err.println(e);
            e.printStackTrace();
            System.exit(1);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Main value iteration routine
    ////////////////////////////////////////////////////////////////////////////

    /**
     * CAMDP inference methods
     */
    public int solve(int max_iter) {
        //////////////////////////////////////////////////////////////////////////
        // Value iteration statistics
        _nCurIter = 0;
        if (max_iter < 0)
            max_iter = _nMaxIter;

        int totalTime = 0;
        long[] time = new long[max_iter + 1];
        int[] num_nodes = new int[max_iter + 1];
        int[] num_leaves = new int[max_iter + 1];
        int[] num_branches = new int[max_iter + 1];

        //////////////////////////////////////////////////////////////////////////

        // Initialize value function to zero
        _valueDD = _context.ZERO;

        // Perform value iteration for specified number of iterations, or until convergence detected
        while (_nCurIter < max_iter) {
            ++_nCurIter;
            resetTimer();
            _logStream.println(ASCII_BAR + "\nITERATION #" + _nCurIter + ", " +
                    memDisplay() + " bytes, " + getElapsedTime() + " ms\n" + ASCII_BAR);
            _logStream.flush();

            // Prime diagram
            _prevDD = _valueDD;

            // Iterate over each action
            _maxDD = null;
            for (Map.Entry<String, CAction> me : _hmName2Action.entrySet()) {

                // Regress the current value function through each action (finite number of continuous actions)
                resetTimer(0);
                int regr = _qfunHelper.regress(_valueDD, me.getValue());
                if (EFFICIENCY_DEBUG) System.out.println("Regression Time for "+me.getKey()+" in iter "+_nCurIter+" = "+getElapsedTime(0));
                regr = standardizeDD(regr); 
                if (DISPLAY_POSTMAX_Q)
                    doDisplay(regr, "Q-" + me.getKey() + "^" + _nCurIter + makeApproxLabel());

                // Maintain running max over different actions
                resetTimer(0);
                _maxDD = (_maxDD == null) ? regr : _context.apply(_maxDD, regr, XADD.MAX);
                _maxDD = standardizeDD(_maxDD);
                if (EFFICIENCY_DEBUG) System.out.println("Standardize MaxDD Time for "+me.getKey()+" in iter "+_nCurIter+" = "+getElapsedTime(0));

                // Optional post-max approximation
                if (APPROX_ALWAYS){
                    resetTimer(0);
                    _maxDD = approximateDD(_maxDD);
                    _maxDD = standardizeDD(_maxDD);
                    if (EFFICIENCY_DEBUG) System.out.println("Approx Always & Standardize Time for "+me.getKey()+" in iter "+_nCurIter+" = "+getElapsedTime(0));
                }
                
                if (DISPLAY_MAX)
                    doDisplay(_maxDD, "QMax^" + _nCurIter + makeApproxLabel() );
                _logStream.println("Running max in iter " + _nCurIter + ":" + _context.getString(_maxDD));
                flushCaches();
            }
            // _maxDD should already be Canonical/LPpruned, check
            _valueDD = _maxDD;
            checkStandardDD(_valueDD);
            
            resetTimer(0);
            _valueDD = approximateDD(_valueDD);
            if (EFFICIENCY_DEBUG && APPROX_PRUNING) System.out.println("Approximation Finish on iter " + _nCurIter +"  pruning took: " + getElapsedTime(0));

            //////////////////////////////////////////////////////////////////////////
            // Value iteration statistics
            time[_nCurIter] = getElapsedTime();
            totalTime += time[_nCurIter];
            num_nodes[_nCurIter] = _context.getNodeCount(_valueDD);
            num_leaves[_nCurIter] = _context.getLeafCount(_valueDD);
            num_branches[_nCurIter] = _context.getBranchCount(_valueDD);

            System.out.println("Iter:" + _nCurIter + " Complete. Took: "+time[_nCurIter]+"ms, Nodes = "+num_nodes[_nCurIter]+", Memory = "+memDisplay() +" bytes.");
            _logStream.println("Iter complete:" + _nCurIter + _context.getString(_valueDD));
            doDisplay(_valueDD, "V^" + _nCurIter + makeApproxLabel());

            if (LINEAR_PROBLEM && APPROX_PRUNING){
                double maxVal = _context.linMaxVal(_valueDD);;
                double maxRelErr = Double.NaN;
                if (COMPARE_OPTIMAL) {
                    if (APPROX_ERROR == 0d) { //Exact solution
                        optimalMaxValueList.add(maxVal);
                        if (optimalDDList.size() != _nCurIter - 1)
                            System.err.println("Incorrect optimalDD:" + optimalDDList + " " + _nCurIter);
                        optimalDDList.add(_valueDD);
                    }
                    if (optimalDDList.size() > _nCurIter - 1) {
                        maxRelErr = (_context.linMaxDiff(optimalDDList.get(_nCurIter - 1), _valueDD)) / optimalMaxValueList.get(_nCurIter);
                    } else maxRelErr = -1;
                }
              //APPROX_TEST LOG, outputs: iter, #node, #branches, #UsedMem(MB), IterTime, TotTime, MaxVal, RelErr
                _testLogStream.format("%d %d %d %d %d %d %d %f %f\n", _nCurIter, num_nodes[_nCurIter],
                        num_leaves[_nCurIter], num_branches[_nCurIter], usedMem(),
                        time[_nCurIter], totalTime,
                        _context.linMaxVal(_valueDD), maxRelErr);
            }
            _logStream.println("Value function size @ end of iteration " + _nCurIter +
                    ": " + num_nodes[_nCurIter] + " nodes = " +
                    num_branches[_nCurIter] + " cases" + " in " + time[_nCurIter] + " ms");
            
            //////////////////////////////////////////////////////////////////////////
            //Verify Early Convergence
            if (_prevDD.equals(_valueDD)) {
                System.out.println("CAMDP: Converged to solution early,  at iteration " + _nCurIter);
                // Store Optimal solution for all horizons for comparison
                if (LINEAR_PROBLEM && APPROX_PRUNING && COMPARE_OPTIMAL){
                    int it = _nCurIter;
                    while (++it < max_iter) {
                        optimalMaxValueList.add(optimalMaxValueList.get(_nCurIter));
                        optimalDDList.add(_valueDD);
                        //APPROX_TEST LOG, outputs: iter, #node, #branches, #UsedMem(MB), IterTime, TotTime, MaxVal, RelErr
                        _testLogStream.format("%d %d %d %d %d %d %d %f %f\n", it, num_nodes[_nCurIter], num_leaves[_nCurIter],
                            num_branches[_nCurIter], usedMem(),
                            time[_nCurIter], totalTime,
                            optimalMaxValueList.get(_nCurIter), 0.0);
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
        for (int i = 1; i <= max_iter; i++) {
            String branch_count = num_branches[i] >= 0
                    ? "" + num_branches[i] : " > " + XADD.MAX_BRANCH_COUNT;
            _logStream.println("Iter " + i + ": nodes = " + num_nodes[i] + "\tbranches = " + branch_count + "\ttime = " + time[i] + " ms");
        }
        //////////////////////////////////////////////////////////////////////////

        return _nCurIter;
    }

    public static void ExitOnError(String msg) {
        System.err.println(msg);
        System.exit(1);
    }

    ////////// DD Property Tests /////////////////////////
    public int standardizeDD(int dd){
        if (XADD.ROUND_PRECISION!= null) {dd = _context.reduceRound(dd); checkRound(dd);}
        dd = _context.makeCanonical(dd); checkCanon(dd);//Always use Canonization
        if (LINEAR_PROBLEM) {dd = _context.reduceLP(dd); while (!checkReduceLP(dd)) dd = _context.reduceLP(dd);}
        checkStandardDD(dd);
        return dd;
    }
    public boolean checkStandardDD(int dd){
        boolean standard = true;
        if (XADD.ROUND_PRECISION!= null && !checkRound(dd)) standard = false;
        if (!checkCanon(dd)) standard = false;//Always use Canonization
        if (LINEAR_PROBLEM && !checkReduceLP(dd)) standard = false;
        return standard;
    }
    private boolean checkRound(int dd) {
        int roundDD = _context.reduceRound(dd);
        if (roundDD != dd){
            System.err.println("Check Round fail");
            if (!SILENCE_ERRORS_PLOTS){
                _context.getGraph(dd).launchViewer("ERROR diagram 1: original DD");
                _context.getGraph(roundDD).launchViewer("ERROR diagram 2: reduceRound(DD)");
            }
            return false;
        }
        return true;
    }
    private boolean checkCanon(int dd) {
        //Error checking and logging
        int canonDD = _context.makeCanonical(dd);
        if (dd != canonDD) {
            System.err.println("Check Canon fail: OriDD: "+dd+" size = "+_context.getNodeCount(dd)+", Canon DD Size="+_context.getNodeCount(canonDD));
            displayDifError(dd, canonDD);
            dd = _context.makeCanonical(dd); //repeat command for Debugging
            canonDD = _context.makeCanonical(dd);
            return false;
        }
        return true;
    }
    public void displayDifError(int dd, int newDD) {
        if (!SILENCE_ERRORS_PLOTS){
            doDisplay(dd,"ERROR plot 1: original");
            doDisplay(newDD,"ERROR plot 2:resulting");
            int dif = _context.apply(dd, newDD, XADD.MINUS);
            doDisplay(dif,"ERROR plot 3: difference");
        }
    }
    private boolean checkReduceLP(int dd) {
        //Error checking and logging
        int reduLPDD = _context.reduceLP(dd);
        if (dd != reduLPDD) {
            System.err.println("Check ReduceLP fail: OriDD: "+dd+" size = "+_context.getNodeCount(dd)+", Pruned DD Size="+_context.getNodeCount(reduLPDD));
            displayDifError(dd, reduLPDD);
            dd = _context.reduceLP(dd); //repeat command for Debugging
            reduLPDD = _context.reduceLP(dd);
            return false;
        }
        return true;
    }
    public int approximateDD(int dd){
        if (LINEAR_PROBLEM && APPROX_PRUNING && APPROX_ERROR > 0)
            dd = _context.linPruneRel(dd, APPROX_ERROR);
        return dd;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Miscellaneous
    ////////////////////////////////////////////////////////////////////////////

    public Double evaluateInitialS(int valueDD){
        if (_initialS == null){
            System.err.println("Trying to Evaluate initial State on a CAMDP without it.");
            return Double.NaN;
        }
        return evaluateState(valueDD, _initialS);
    }
    public Double evaluateState(int valueDD, State s){
        return _context.evaluate(valueDD, s._hmBoolVars, s._hmContVars);
    }    
    public void flushCaches() {
        flushCaches(new ArrayList<Integer>());
    }

    public void flushCaches(boolean forceFlush) {
        flushCaches(new ArrayList<Integer>(), forceFlush);
    }

    public void flushCaches(List<Integer> special_nodes) {
        flushCaches(special_nodes, false);
    }

    public void flushCaches(List<Integer> special_nodes, boolean forceFlush) {

        if (((double) RUNTIME.freeMemory() /
                (double) RUNTIME.totalMemory()) > FLUSH_PERCENT_MINIMUM && !forceFlush) {
            //System.out.println("No need to flush caches.");
            return; // Still enough free mem to exceed minimum requirements
        }

        // Commence cache flushing
        _logStream.println("Before flush: " + _context._hmInt2Node.size() + " XADD nodes in use, " + "freeMemory: " +
                _df.format(RUNTIME.freeMemory() / 10e6d) + " MB = " +
                _df.format(100d * RUNTIME.freeMemory() / (double) RUNTIME.totalMemory()) + "% available memory");

        // TODO: Maybe keeping these is worthwhile?
        _hmContRegrCache.clear();

        _context.clearSpecialNodes();
        for (Integer node : special_nodes)
            _context.addSpecialNode(node);


        for (CAction a : _hmName2Action.values()) {
            _context.addSpecialNode(a._reward);
            for (Integer xadd : a._hmVar2DD.values())
                _context.addSpecialNode(xadd);
            for (Integer xadd : a._hmNoise2DD.values())
                _context.addSpecialNode(xadd);
        }
        if (_prevDD != null && !forceFlush) {
            _context.addSpecialNode(_prevDD);
        }
        if (_maxDD != null && !forceFlush) {
            _context.addSpecialNode(_maxDD);
        }
        if (_valueDD != null && !forceFlush) {
            _context.addSpecialNode(_valueDD);
        }
        if (optimalDDList != null && optimalDDList.size()>1) //keep even at forceFlush, because we want to measure the error
            _context._hsSpecialNodes.addAll(optimalDDList.subList(1, optimalDDList.size()-1));
        _context.flushCaches();

        _logStream.println("After flush: " + _context._hmInt2Node.size() + " XADD nodes in use, " + "freeMemory: " +
                _df.format(RUNTIME.freeMemory() / 10e6d) + " MB = " +
                _df.format(100d * RUNTIME.freeMemory() / (double) RUNTIME.totalMemory()) + "% available memory");
    }


    public String toString() {
        return toString(true, false);
    }

    public String toString(boolean display_reward, boolean display_value) {
        StringBuffer sb = new StringBuffer();
        sb.append("\nCMDP Definition:\n===============\n");
        sb.append("BVars:       " + _hsBoolAllVars + " = S:" + _hsBoolSVars + " + I:" + _hsBoolIVars + " = XADD (all vars): " + _context._alBooleanVars + "\n");
        sb.append("NS BVars:    " + _hsBoolNSVars + "\n");
        sb.append("CVars:       " + _hsContAllVars + " = S:" + _hsContSVars + " + A:" + _hsContAVars + " + I:" + _hsContIVars + "\n");
        sb.append("NS CVars:    " + _hsContNSVars + "\n");
        sb.append("Noise vars:  " + _hsNoiseVars + "\n");
        sb.append("Min-values:  " + _context._hmMinVal + "\n");
        sb.append("Max-values:  " + _context._hmMaxVal + "\n");
        sb.append("Order:       " + _context._alOrder + "\n");
        sb.append("Iterations:  " + _nMaxIter + "\n");
        sb.append("Linearity:  " + LINEAR_PROBLEM + "\n");
        //sb.append("Constraints (" + _alConstraints.size() + "):\n");
        //for (Integer cons : _alConstraints) {
        //    sb.append("- " + _context.getString(cons) + "\n");
        //}
        if (_initialS != null) {
            sb.append("Initial State: " + _initialS + "\n");    
        }
        sb.append("Actions (" + _hmName2Action.size() + "):\n");
        for (CAction a : _hmName2Action.values()) {
            sb.append("\n==> " + a);
        }
        sb.append("\n");

        if (display_value) {
            Graph g = _context.getGraph(_valueDD);
            g.launchViewer(1300, 770);
        }

        return sb.toString();
    }

    public String makeApproxLabel(){
        return APPROX_PRUNING?"":"-" + String.format("%03d", Math.round(1000 * APPROX_ERROR));
    }
    public void doDisplay(int xadd_id, String label) {
        exportXADD(xadd_id, label); // Exports DAG, can read in later and view using XADDViewer
        if (DISPLAY_V)
            displayGraph(xadd_id, label);
        if (DISPLAY_2D)
            display2D(xadd_id, label);
        if (DISPLAY_3D)
            display3D(xadd_id, label);
    }

    public void exportXADD(int xadd_id, String label) {
        label = label.replace(".csamdp", "").replace(".camdp", "").replace(".cmdp", "")
                .replace('^', '_').replace("(", "").replace(")", "").replace(":", "_").replace(" ", "");
        String xadd_filename = _logFileRoot + "." + label + ".xadd";
        _context.exportXADDToFile(xadd_id, xadd_filename);

        // Copy over plotting options if they exist
        File file2D = new File(_problemFile + ".2d");
        if (file2D.exists()) {
            FileOptions opt = new FileOptions(_problemFile + ".2d");
            opt.exportToFile(xadd_filename + ".2d");
        }
        File file3D = new File(_problemFile + ".3d");
        if (file3D.exists()) {
            FileOptions opt = new FileOptions(_problemFile + ".3d");
            opt.exportToFile(xadd_filename + ".3d");
        }
    }

    public void displayGraph(int xadd_id, String label) {
        String[] split = label.split("[\\\\/]");
        label = split[split.length - 1];
        label = label.replace(".csamdp", "").replace(".camdp", "").replace(".cmdp", "");
    
        Graph g;
        int count;
        if (DONT_SHOW_HUGE_GRAPHS && (count = _context.getNodeCount(xadd_id)) > MAXIMUM_XADD_DISPLAY_SIZE){
            g = new Graph();
            g.addNode("_count_");
            g.addNodeLabel("_count_", "Too Large to Print: "+count+" Nodes");
            g.addNodeShape("_count_", "square");
            g.addNodeStyle("_count_", "filled");
            g.addNodeColor("_count_", "red1");
        }
        else{
            g = _context.getGraph(xadd_id);
        }
        g.addNode("_temp_");
        g.addNodeLabel("_temp_", label);
        g.addNodeShape("_temp_", "square");
        g.addNodeStyle("_temp_", "filled");
        g.addNodeColor("_temp_", "gold1");
        String safe_filename = label.replace('^', '_').replace("(", "").replace(")", "").replace(":", "_").replace(" ", "");
        g.genDotFile(_logFileRoot + "." + safe_filename + ".dot");

        g.launchViewer(label);
    }

    public void display2D(int xadd_id, String label) {

        // If DISPLAY_2D is enabled, it is expected that necessary parameters
        // have been placed in a _problemFile + ".2d"
        FileOptions opt = new FileOptions(_problemFile + ".2d");

        if (!SILENT_PLOT) {
            System.out.println("Plotting 2D...");
            System.out.println("var: " + opt._var.get(0) + ", [" + opt._varLB.get(0) + ", " +
                    opt._varInc.get(0) + ", " + opt._varUB.get(0) + "]");
            System.out.println("bassign: " + opt._bassign);
            System.out.println("dassign: " + opt._dassign);
        }

        XADDUtils.PlotXADD(_context, xadd_id,
                opt._varLB.get(0), opt._varInc.get(0), opt._varUB.get(0),
                opt._bassign, opt._dassign, opt._var.get(0), _logFileRoot + "." + label);
    }

    public void display3D(int xadd_id, String label) {

        // If DISPLAY_3D is enabled, it is expected that necessary parameters
        // have been placed in a _problemFile + ".3d"
        FileOptions opt = new FileOptions(_problemFile + ".3d");

        if (!SILENT_PLOT) {
            System.out.println("Plotting 3D...");
            System.out.println("var: " + opt._var.get(1) + ", [" + opt._varLB.get(1) + ", " +
                    opt._varInc.get(1) + ", " + opt._varUB.get(1) + "]");
            System.out.println("bassign: " + opt._bassign);
            System.out.println("dassign: " + opt._dassign);
        }

        XADDUtils.Plot3DSurfXADD(_context, xadd_id,
                opt._varLB.get(0), opt._varInc.get(0), opt._varUB.get(0),
                opt._varLB.get(1), opt._varInc.get(1), opt._varUB.get(1),
                opt._bassign, opt._dassign, opt._var.get(0), opt._var.get(1), _logFileRoot + "." + label);
    }

    // A helper class to load options for 2D and 3D plotting for specific problems
    public static class FileOptions {
        public ArrayList<String> _var = new ArrayList<String>();
        public ArrayList<Double> _varLB = new ArrayList<Double>();
        public ArrayList<Double> _varInc = new ArrayList<Double>();
        public ArrayList<Double> _varUB = new ArrayList<Double>();
        public HashMap<String, Boolean> _bassign = new HashMap<String, Boolean>();
        public HashMap<String, Double> _dassign = new HashMap<String, Double>();

        public FileOptions() {
        }

        public FileOptions(String filename) {
            String line = null;
            try {
                BufferedReader br = new BufferedReader(new FileReader(filename));
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.length() == 0)
                        continue;
                    String[] split = line.split("\\s+"); // Luis: need general whitespace regex since previous files use \t
                    String label = split[0].trim();
                    //System.out.println("Label: '" + label + "'");
                    if (label.equalsIgnoreCase("var")) {
                        // Line format: var name lb inc ub
                        _var.add(split[1].trim());
                        _varLB.add(Double.parseDouble(split[2]));
                        _varInc.add(Double.parseDouble(split[3]));
                        _varUB.add(Double.parseDouble(split[4]));
                    } else if (label.equalsIgnoreCase("bassign")) {
                        // Line format: bassign name {true,false}
                        _bassign.put(split[1].trim(), Boolean.parseBoolean(split[2]));
                    } else if (label.equalsIgnoreCase("cassign")) {
                        // Line format: cassign name double
                        _dassign.put(split[1].trim(), Double.parseDouble(split[2]));
                    } else {
                        throw new Exception("ERROR: Unexpected line label '" + label + "', not {var, bassign, cassign}");
                    }
                }
            } catch (Exception e) {
                System.err.println(e + "\nContent at current line: '" + line + "'");
                System.err.println("ERROR: could not read file: " + filename + ", exiting.");
            }
        }

        public void exportToFile(String outfile) {
            try {
                PrintStream ps = new PrintStream(new FileOutputStream(outfile));
                for (int i = 0; i < _var.size(); i++) {
                    String var = _var.get(i);
                    double lb = _varLB.get(i);
                    double inc = _varInc.get(i);
                    double ub = _varUB.get(i);
                    ps.println("var\t" + var + "\t" + lb + "\t" + inc + "\t" + ub);
                }
                for (Map.Entry<String, Boolean> me : _bassign.entrySet())
                    ps.println("bassign\t" + me.getKey() + "\t" + me.getValue());
                for (Map.Entry<String, Double> me : _dassign.entrySet())
                    ps.println("dassign\t" + me.getKey() + "\t" + me.getValue());
                ps.close();
            } catch (Exception e) {
                System.err.println("WARNING: could not export " + outfile);
            }
        }
    }

    // Reset elapsed time
    public static void resetTimer() {
        _lTime = System.currentTimeMillis();
    }

    // Get the elapsed time since resetting the timer
    public static long getElapsedTime() {
        return System.currentTimeMillis() - _lTime;
    }

    // Reset elapsed time
    public static void resetTimer(int n) {
        _lTimers[n] = System.currentTimeMillis();
    }

    // Get the elapsed time since resetting the timer
    public static long getElapsedTime(int n) {
        return System.currentTimeMillis() - _lTimers[n];
    }
    
    
    public static String memDisplay() {
        long total = RUNTIME.totalMemory();
        long free = RUNTIME.freeMemory();
        return total - free + ":" + total;
    }

    public static int usedMem() {
        long total = RUNTIME.totalMemory();
        long free = RUNTIME.freeMemory();
        return (int) ((total - free) / 1000000);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Testing Interface
    ////////////////////////////////////////////////////////////////////////////

    public static String insertDirectory(String filename, String add_dir) {
        try {
            File f = new File(filename);
            String parent = f.getParent();
            String dir_path = (parent == null ? "" : parent) + File.separator + add_dir;
            File dir = new File(dir_path);
            if (dir.exists() && !dir.isDirectory())
                throw new Exception("'" + dir + "' is a file, cannot change it to a directory for logging.");
            if (!dir.exists())
                dir.mkdir();
            return dir_path + File.separator + f.getName();
        } catch (Exception e) {
            System.err.println("Could not insert directory '" + add_dir + "' into '" + filename + "' to produce output files.");
            System.exit(1);
        }
        return null;
    }

    public ArrayList<String> intern(ArrayList<String> l) {
        ArrayList<String> ret = new ArrayList<String>();
        for (String s : l)
            ret.add(s.intern());
        return ret;
    }

    public static void usage() {
        System.out.println("\nUsage: MDP-filename #iter display-2D? display-3D? [dApproxPrune]");
        System.exit(1);
    }

    public void setApproxTest(double eps, PrintStream log, boolean always) {
        APPROX_ERROR = eps;
        _testLogStream = log;
        APPROX_ALWAYS = always;
        COMPARE_OPTIMAL = true;
    }

    public static void main(String args[]) {
        if (args.length < 4 || args.length > 5) {
            usage();
        }

        // Parse problem filename
        String filename = args[0];

        // Parse iterations
        int iter = -1;
        try {
            iter = Integer.parseInt(args[1]);
        } catch (NumberFormatException nfe) {
            System.out.println("\nIllegal iteration value\n");
            usage();
        }

        // Build a CAMDP, display, solve
        CAMDP mdp = new CAMDP(filename);
        mdp.DISPLAY_2D = Boolean.parseBoolean(args[2]);
        mdp.DISPLAY_3D = Boolean.parseBoolean(args[3]);

        //aditional argument modifies
        if (args.length == 5) {
            mdp.APPROX_ERROR = Double.parseDouble(args[4]);
        }
        //System.out.println(mdp.toString(false, false));
        System.out.println(mdp.toString(false, false));
        //System.in.read();

        int iter_used = mdp.solve(iter);
//        System.out.println("\nSolution complete, required " + 
//                iter_used + " / " + iter + " iterations.");
        //mdp._context.showCacheSize();
//        mdp.flushCaches(true);
//        mdp._context.showCacheSize();
        System.out.println("CAMDP-FINISH");
    }

    public final static String ASCII_BAR = "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%"; // Display shortcut
}
