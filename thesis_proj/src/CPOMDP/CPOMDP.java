package CPOMDP;

import graph.Graph;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import DPOMDP.GammaSet;

import camdp.CAMDP;
import camdp.CAction;
import camdp.ComputeQFunction;
import camdp.ParseCAMDP;
import camdp.CAMDP.FileOptions;
import cmdp.HierarchicalParser;
import util.IntTriple;
import xadd.TestXADDDist;
import xadd.XADD;
import xadd.XADD.ArithExpr;

public class CPOMDP {

	/* Constants */
	public final static String RESULTS_DIR = "results"; // Diagnostic output destination
	public final static boolean DISPLAY_V = true;
	public final static boolean DISPLAY_G = true;

	/* Maintain an explicit policy? */
	public final static boolean MAINTAIN_POLICY = false;

	/* Cache maintenance */
	public final static boolean ALWAYS_FLUSH = false; // Always flush DD caches?
	public final static double FLUSH_PERCENT_MINIMUM = 0.3d; // Won't flush until < amt

	/* For printing */
	public static DecimalFormat _df = new DecimalFormat("#.###");
	public static PrintStream _logStream = null;

	/* Static variables */
	public static long _lTime; // For timing purposes
	public static Runtime RUNTIME = Runtime.getRuntime();

	/* Local vars */
	public boolean DISPLAY_2D = false;
	public boolean DISPLAY_3D = false;

	public String _problemFile = null;
	public String _logFileRoot = null;
	public XADD _context = null;
	public HashSet<String> _hsBoolSVars;
	public HashSet<String> _hsBoolOVars;
	public HashSet<String> _hsContSVars;
	public HashSet<String> _hsContOVars;

	public ArrayList<String> _alBoolSVars; // Retain order given in MDP file
	public ArrayList<String> _alBoolOVars; // Retain order given in MDP file
	public ArrayList<String> _alContSVars; // Retain order given in MDP file
	public ArrayList<String> _alContOVars; // Retain order given in MDP file
	public ArrayList<String> _alContAllVars; //state and observation

	public Integer _valueDD; // The resulting value function once this CMDP has
	public Integer _maxDD;
	public Integer _prevDD;
	public BigDecimal _bdDiscount; // Discount (gamma) for CMDP
	public Integer    _nMaxIter;   // Number of iterations for CMDP
	public Integer    _nCurIter;   // Number of iterations for CMDP

	public HashMap<String,ArithExpr>  _hmPrimeSubs;
	public HashMap<String,COAction>    _hmName2Action;
	public HashMap<IntTriple,Integer> _hmContRegrCache;
	public ArrayList<Integer>         _alConstraints;
	
	public ComputeGamma _gammaHelper = null;
	public HashMap<Integer,Integer> _currentgammaSet_h;
	public HashMap<Integer,Integer> _previousgammaSet_h;
	
	public CPOMDP(String filename) {
		this(filename, HierarchicalParser.ParseFile(filename));
	}

	public CPOMDP(String file_source, ArrayList input) {

		// Basic initializations
		_problemFile = file_source;
		_logFileRoot = InsertDirectory(_problemFile, RESULTS_DIR).replace("-", "_");
		_context = new XADD();
		_prevDD = _maxDD = _valueDD = null;
		_bdDiscount = new BigDecimal("" + (-1));
		_nMaxIter = null;
		// Setup CAMDP according to parsed file contents
		ParsePOMDP parser = new ParsePOMDP(this);
		parser.buildPOMDP(input);
		_context._hmMaxVal = parser._maxCVal;
		_context._hmMinVal = parser._minCVal;
		_context._alBooleanVars = parser.getBVars();

		_alConstraints = parser.getConstraints();
		_nMaxIter = parser.getIterations();
		_bdDiscount = parser.getDiscount();
		_hmName2Action = parser.getHashmap();
		_hmContRegrCache = new HashMap<IntTriple,Integer>();

		// Setup variable sets and lists
		_hsBoolSVars = new HashSet<String>(Intern(parser.getBVars()));
		_hsBoolOVars = new HashSet<String>(Intern(parser.getBOVars()));
		_hsContSVars = new HashSet<String>(Intern(parser.getCVars()));
		_hsContOVars = new HashSet<String>(Intern(parser.getOVars()));
		_alBoolSVars = new ArrayList<String>(Intern(parser.getBVars())); // Retain order given in MDP file
		_alBoolOVars = new ArrayList<String>(Intern(parser.getBOVars())); // Retain order given in MDP file
		_alContSVars = new ArrayList<String>(Intern(parser.getCVars())); // Retain order given in MDP file
		_alContOVars = new ArrayList<String>(Intern(parser.getOVars())); // Retain order given in MDP file
		_alContAllVars = new ArrayList<String>(_alContSVars);
		_alContAllVars.addAll(_alContOVars);

		
		
		_gammaHelper = new ComputeGamma(_context, this);
		_previousgammaSet_h = new HashMap<Integer, Integer>();
		_currentgammaSet_h = new HashMap<Integer, Integer>();
		// Build cur-state var -> next-state var map
		_hmPrimeSubs = new HashMap<String,ArithExpr>();
		for (String var : _hsContSVars) 
			_hmPrimeSubs.put(var, new XADD.VarExpr(var + "'"));
		for (String var : _hsBoolSVars) 
			_hmPrimeSubs.put(var, new XADD.VarExpr(var + "'"));

		// Perform progression and regression?

		// Setup a logger
		try {
			_logStream = new PrintStream(new FileOutputStream(_logFileRoot + ".log"));
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
	 * CMDP inference methods
	 **/
	public int solve(int max_iter)
	{
		//////////////////////////////////////////////////////////////////////////
		// Value iteration statistics
		_nCurIter = 0;
		if (max_iter < 0)
			max_iter = _nMaxIter;
		long[] time = new long[max_iter + 1];
		int[] num_nodes = new int[max_iter + 1]; 
		int[] num_branches = new int[max_iter + 1]; 
		//////////////////////////////////////////////////////////////////////////
		
		// Initialize value function to zero
		_currentgammaSet_h.put(0,_context.getTermNode(XADD.ZERO));
		_valueDD = _context.getTermNode(XADD.ZERO);
		// Perform value iteration for specified number of iterations, or until convergence detected
		while (_nCurIter < max_iter) 
		{
			++_nCurIter;
			ResetTimer();
			_logStream.println("Iteration #" + _nCurIter + ", " + MemDisplay() + " bytes, " + GetElapsedTime() + " ms");
			_logStream.flush();
			
			// Prime diagram
			_prevDD = _valueDD;
			_previousgammaSet_h = _currentgammaSet_h;
			_currentgammaSet_h = new HashMap<Integer, Integer>();

			int counter=0;
			// Iterate over each action
			_maxDD = null;
			for (Map.Entry<String,COAction> me : _hmName2Action.entrySet()) {

				counter++;
				int regr = _gammaHelper.computeGamma(me.getValue(),_previousgammaSet_h);
				//the result is Gamma^h_a, add this to the current set of Gamma^h_a
				regr = _context.reduceLinearize(regr);
				regr = _context.reduceLP(regr);
				
				_currentgammaSet_h.put(counter, regr);
				
				flushCaches();
			}

			//pruning the _alpha vectors
			//_currentgammaSet_h = dominanceTest(_currentgammaSet_h);
			//TODO: taking a max to display the value function????
			_logStream.println("- V^" + _nCurIter + _context.getString(_valueDD));
			doDisplay(_valueDD, _logFileRoot + ": V^"+_nCurIter);
			
			//////////////////////////////////////////////////////////////////////////
			// Value iteration statistics
			time[_nCurIter] = GetElapsedTime();
			num_nodes[_nCurIter] = _context.getNodeCount(_valueDD);
			num_branches[_nCurIter] = _context.getBranchCount(_valueDD);
			_logStream.println("Value function size @ end of iteration " + _nCurIter + 
					": " + num_nodes[_nCurIter] + " nodes = " + 
					num_branches[_nCurIter] + " cases" + " in " + time[_nCurIter] + " ms");
			//////////////////////////////////////////////////////////////////////////
		}

		flushCaches();	
		
		//////////////////////////////////////////////////////////////////////////
		// Performance Logging
		_logStream.println("\nValue iteration complete!");
		_logStream.println(max_iter + " iterations took " + GetElapsedTime() + " ms");
		_logStream.println("Canonical / non-canonical: " + XADD.OperExpr.ALREADY_CANONICAL + " / " + XADD.OperExpr.NON_CANONICAL);

		_logStream.println("\nIteration Results summary");
		for (int i = 1; i <= max_iter; i++) {
			String branch_count = num_branches[i] >= 0 
			? "" + num_branches[i] : " > " + XADD.MAX_BRANCH_COUNT; 
			_logStream.println("Iter " + i + ": nodes = " + num_nodes[i] + "\tbranches = " + branch_count + "\ttime = " + time[i] + " ms");
		}
		//////////////////////////////////////////////////////////////////////////

		return _nCurIter;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	// Miscellaneous
	////////////////////////////////////////////////////////////////////////////

	public void flushCaches() {
		flushCaches(new ArrayList<Integer>());
	}

	public void flushCaches(List<Integer> special_nodes) {
		if (((double)RUNTIME.freeMemory() / 
				(double)RUNTIME.totalMemory()) > FLUSH_PERCENT_MINIMUM) {
			System.out.println("No need to flush caches.");
			return; // Still enough free mem to exceed minimum requirements
		}

		// Commence cache flushing
		_logStream.println("Before flush: " + _context._hmInt2Node.size() + " XADD nodes in use, " + "freeMemory: " + 
				_df.format(RUNTIME.freeMemory()/10e6d) + " MB = " + 
				_df.format(100d*RUNTIME.freeMemory()/(double)RUNTIME.totalMemory()) + "% available memory");

		// TODO: Maybe keeping these is worthwhile?
		_hmContRegrCache.clear();

		_context.clearSpecialNodes();
		for (Integer node : special_nodes)
			_context.addSpecialNode(node);

		for (COAction a : _hmName2Action.values()) {
			_context.addSpecialNode(a._reward);
			for (Integer xadd : a._hmVar2DD.values())
				_context.addSpecialNode(xadd);
			for (Integer xadd : a._hmObs2DD.values())
				_context.addSpecialNode(xadd);
		}
		if (_prevDD!=null){
			_context.addSpecialNode(_prevDD);
		}
		if (_maxDD!=null){
			_context.addSpecialNode(_maxDD);
		}
		if (_valueDD!=null){
			_context.addSpecialNode(_valueDD); 
		}
		_context.flushCaches();

		_logStream.println("After flush: " + _context._hmInt2Node.size() + " XADD nodes in use, " + "freeMemory: " + 
				_df.format(RUNTIME.freeMemory()/10e6d) + " MB = " + 
				_df.format(100d*RUNTIME.freeMemory()/(double)RUNTIME.totalMemory()) + "% available memory");
	}

	public String toString() {
		return toString(false, false);
	}

	public String toString(boolean display_reward, boolean display_value) {
		StringBuffer sb = new StringBuffer();
		sb.append("\nCMDP Definition:\n===============\n");
		sb.append("CVars:       " +	_alContAllVars + " = " + _hsContSVars + " + " + _hsContOVars + "\n");
		sb.append("Min-values:  " + _context._hmMinVal + "\n");
		sb.append("Max-values:  " + _context._hmMaxVal + "\n");
		sb.append("BVars:       " + _context._alBooleanVars + "/" + _hsBoolSVars + "\n");
		sb.append("BOVars:       " + _hsBoolOVars + "\n");
		sb.append("OVars:       " + _hsContOVars + "\n");
		sb.append("Order:       " + _context._alOrder + "\n");
		sb.append("Iterations:  " + _nMaxIter + "\n");
		sb.append("Constraints (" + _alConstraints.size() + "):\n");
		for (Integer cons : _alConstraints) {
			sb.append("- " + _context.getString(cons) + "\n");
		}
		sb.append("Actions (" + _hmName2Action.size() + "):\n");
		for (COAction a : _hmName2Action.values()) {
			sb.append("\n==> " + a);
		}
		sb.append("\n");

		if (display_value) {
			Graph g = _context.getGraph(_valueDD);
			//g.launchViewer(1300, 770);
		}

		return sb.toString();
	}

	public void doDisplay(int xadd_id, String label) {
		if (DISPLAY_V) 
			displayGraph(xadd_id, label);
		if (DISPLAY_2D)
			display2D(xadd_id, label);
		if (DISPLAY_3D) 
			display3D(xadd_id, label);
	}

	public void displayGraph(int xadd_id, String label) {
		Graph g = _context.getGraph(xadd_id);
		String[] split = label.split("[\\\\/]");
		label = split[split.length - 1];
		label = label.replace(".camdp", "").replace(".cmdp", "");
		g.addNode("_temp_");
		g.addNodeLabel("_temp_", label);
		g.addNodeShape("_temp_", "square");
		g.addNodeStyle("_temp_", "filled");
		g.addNodeColor("_temp_", "gold1");
		g.launchViewer(1300, 770);
	}

	public void display2D(int xadd_id, String label) {

		// If DISPLAY_2D is enabled, it is expected that necessary parameters 
		// have been placed in a _problemFile + ".2d"
		FileOptions opt = new FileOptions(_problemFile + ".2d");

		System.out.println("Plotting 2D...");
		System.out.println("var: " + opt._var.get(0) + ", [" + opt._varLB.get(0) + ", " + 
				opt._varInc.get(0) + ", " + opt._varUB.get(0) + "]");
		System.out.println("bassign: " + opt._bassign);
		System.out.println("dassign: " + opt._dassign);

		TestXADDDist.PlotXADD(_context, xadd_id, 
				opt._varLB.get(0), opt._varInc.get(0), opt._varUB.get(0), 
				opt._bassign, opt._dassign, opt._var.get(0), label);
	}

	public void display3D(int xadd_id, String label) {

		// If DISPLAY_3D is enabled, it is expected that necessary parameters 
		// have been placed in a _problemFile + ".2d"
		FileOptions opt = new FileOptions(_problemFile + ".3d");

		System.out.println("Plotting 3D...");
		System.out.println("var: " + opt._var.get(1) + ", [" + opt._varLB.get(1) + ", " + 
				opt._varInc.get(1) + ", " + opt._varUB.get(1) + "]");
		System.out.println("bassign: " + opt._bassign);
		System.out.println("dassign: " + opt._dassign);

		TestXADDDist.Plot3DXADD(_context, xadd_id, 
				opt._varLB.get(0), opt._varInc.get(0), opt._varUB.get(0), 
				opt._varLB.get(1), opt._varInc.get(1), opt._varUB.get(1), 
				opt._bassign, opt._dassign, opt._var.get(0), opt._var.get(1), label);
	}

	// A helper class to load options for 2D and 3D plotting for specific problems
	public class FileOptions {
		public ArrayList<String> _var = new ArrayList<String>();
		public ArrayList<Double> _varLB = new ArrayList<Double>();
		public ArrayList<Double> _varInc = new ArrayList<Double>();
		public ArrayList<Double> _varUB = new ArrayList<Double>();
		public HashMap<String,Boolean> _bassign = new HashMap<String, Boolean>();
		public HashMap<String,Double>  _dassign = new HashMap<String, Double>();
		public FileOptions(String filename) {
			String line = null;
			try {
				BufferedReader br = new BufferedReader(new FileReader(filename));
				while ((line = br.readLine()) != null) {
					line = line.trim();
					if (line.length() == 0)
						continue;
					String[] split = line.split("\t");
					String label = split[0].trim();
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
						throw new Exception("ERROR: Unexpected line label '" + label + "', not {var, bassign, dassign}");
					}
				}
			} catch (Exception e) {
				System.err.println(e + "\nContent at current line: '" + line + "'");
				System.err.println("ERROR: could not read 2d file: " + filename + ", exiting.");
			}		
		}
	}

	// Reset elapsed time
	public static void ResetTimer() {
		_lTime = System.currentTimeMillis(); 
	}

	// Get the elapsed time since resetting the timer
	public static long GetElapsedTime() {
		return System.currentTimeMillis() - _lTime;
	}

	public static String MemDisplay() {
		long total = RUNTIME.totalMemory();
		long free  = RUNTIME.freeMemory();
		return total - free + ":" + total;
	}

	////////////////////////////////////////////////////////////////////////////
	// Testing Interface
	////////////////////////////////////////////////////////////////////////////

	public static String InsertDirectory(String filename, String add_dir) {
		try {
			File f = new File(filename);
			String parent = f.getParent();
			return (parent == null ? "" : parent) + File.separator + add_dir + 
					File.separator + f.getName();
		} catch (Exception e) {
			System.err.println("Could not insert directory '" + add_dir + "' into '" + filename + "' to produce output files.");
			System.exit(1);
		}
		return null;
	}

	public ArrayList<String> Intern(ArrayList<String> l) {
		ArrayList<String> ret = new ArrayList<String>();
		for (String s : l)
			ret.add(s.intern());
		return ret;
	}

	public static void Usage() {
		System.out.println("\nUsage: MDP-filename #iter display-2D? display-3D?");
		System.exit(1);
	}

	public static void main(String args[]) {
		if (args.length != 4) {
			Usage();
		}

		// Parse problem filename
		String filename = args[0];

		// Parse iterations
		int iter = -1;
		try {
			iter = Integer.parseInt(args[1]);
		} catch (NumberFormatException nfe) {
			System.out.println("\nIllegal iteration value\n");
			Usage();
		}

		// Build a CAMDP, display, solve
		CPOMDP pomdp = new CPOMDP(filename);
		pomdp.DISPLAY_2D = Boolean.parseBoolean(args[2]);
		pomdp.DISPLAY_3D = Boolean.parseBoolean(args[3]);
		System.out.println(pomdp.toString(false, false));
		//System.in.read();

		int iter_used = pomdp.solve(iter);
		System.out.println("\nSolution complete, required " + 
				iter_used + " / " + iter + " iterations.");
	}

}
