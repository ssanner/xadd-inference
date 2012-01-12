// Package definition
package camdp;

// Packages to import
import graph.Graph;

import java.io.*;
import java.util.*;

import java.math.BigDecimal;
import java.text.DecimalFormat;

import util.IntTriple;
import xadd.XADD;
import xadd.XADD.*;
import cmdp.HierarchicalParser;

/**
 * Main Continuous State and Action MDP (CAMDP) dynamic programming solution class
 * 
 * @version 1.0
 * @author Zahra
 * @language Java (JDK 1.5)
 **/
public class CAMDP {

	/* Constants */
	public final static boolean DISPLAY_Q = false;
	public final static boolean DISPLAY_V = true;
	public final static boolean DISPLAY_2D = true;
	public final static boolean DISPLAY_MAX = false;

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
	public String _problemFile = null;
	public XADD _context = null;
	public HashSet<String> _hsBoolSVars;
	public HashSet<String> _hsContSVars;
	public HashSet<String> _hsContAVars;

	public ArrayList<String> _alBoolSVars; // Retain order given in MDP file
	public ArrayList<String> _alContSVars; // Retain order given in MDP file
	public ArrayList<String> _alContAVars; // Retain order given in MDP file
	public ArrayList<String> _alContAllVars; // Retain order given in MDP file
	
	public Integer _valueDD; // The resulting value function once this CMDP has
	public Integer _maxDD;
	public Integer _prevDD;
	public BigDecimal _bdDiscount; // Discount (gamma) for CMDP
	public Integer    _nIter;      // Number of iterations for CMDP

	public HashMap<String,ArithExpr>  _hmPrimeSubs;
	public HashMap<String,CAction>    _hmName2Action;
	public HashMap<IntTriple,Integer> _hmContRegrCache;
	public ArrayList<Integer>         _alConstraints;
	
	public ComputeQFunction _qfunHelper = null;

	////////////////////////////////////////////////////////////////////////////
	// Constructors
	////////////////////////////////////////////////////////////////////////////

	/**
	 * Constructor - filename
	 **/
	public CAMDP(String filename) {
		this(filename, HierarchicalParser.ParseFile(filename));
	}

	/**
	 * Constructor - pre-parsed file
	 **/
	private CAMDP(String file_source, ArrayList input) {
		
		// Basic initializations
		_problemFile = file_source;
		_context = new XADD();
		_prevDD = _maxDD = _valueDD = null;
		_bdDiscount = new BigDecimal("" + (-1));
		_nIter = null;
		_hmName2Action = new HashMap<String,CAction>();

		// Setup CAMDP according to parsed file contents
		ParseCAMDP parser = new ParseCAMDP(this);
		parser.buildCAMDP(input);
		_context._hmMaxVal = parser._maxCVal;
		_context._hmMinVal = parser._minCVal;
		_context._alBooleanVars = parser.getBVars();
		_alConstraints = parser.getConstraints();
		_nIter = parser.getIterations();
		_bdDiscount = parser.getDiscount();
		_hmName2Action = parser.getHashmap();
		
		// Setup variable sets and lists
		_hsBoolSVars = new HashSet<String>(parser.getBVars());
		_hsContSVars = new HashSet<String>(parser.getCVars());
		_hsContAVars = new HashSet<String>(parser.getAVars());
		_alBoolSVars = new ArrayList<String>(parser.getBVars()); // Retain order given in MDP file
		_alContSVars = new ArrayList<String>(parser.getCVars()); // Retain order given in MDP file
		_alContAVars = new ArrayList<String>(parser.getAVars()); // Retain order given in MDP file
		_alContAllVars = new ArrayList<String>(_alContSVars);
		_alContAllVars.addAll(_alContAVars);
		_context._alContinuousVars = _alContAllVars;
		
		// Build cur-state var -> next-state var map
		_hmPrimeSubs = new HashMap<String,ArithExpr>();
		for (String var : _hsContSVars) 
			_hmPrimeSubs.put(var, new XADD.VarExpr(var + "'"));
		
		// This helper class performs the regression
		_qfunHelper = new ComputeQFunction(_context, this);
		
		// Setup a logger
		try {
			_logStream = new PrintStream(new FileOutputStream(file_source + ".log"));
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
		int iter = 0;
		long[] time = new long[max_iter + 1];
		int[] num_nodes = new int[max_iter + 1]; 
		int[] num_branches = new int[max_iter + 1]; 
		//////////////////////////////////////////////////////////////////////////
		
		if (max_iter < 0)
			max_iter = _nIter;

		// Initialize value function to zero
		_valueDD = _context.getTermNode(XADD.ZERO);

		// Perform value iteration for specified number of iterations, or until convergence detected
		while (++iter <= max_iter) 
		{
			ResetTimer();
			_logStream.println("Iteration #" + iter + ", " + MemDisplay() + " bytes, " + GetElapsedTime() + " ms");
			_logStream.flush();
			
			// Prime diagram
			_prevDD = _valueDD;

			// Iterate over each action
			_maxDD = null;
			for (Map.Entry<String,CAction> me : _hmName2Action.entrySet()) {

				// Regress the current value function through each action (finite number of continuous actions)
				int regr = _qfunHelper.regress(_valueDD, me.getValue());
				if (DISPLAY_Q)
					displayGraph(regr, "Q^" + iter + "(" + me.getKey() + ")");
	
				// Take the max over this action and the previous action 
				//(can have continuous parameters which represents many discrete actions)
				if (_maxDD == null)
					_maxDD = regr;
				else {
					_maxDD = _context.apply(_maxDD, regr, XADD.MAX);
					_maxDD = _context.reduceLP(_maxDD, _alContAllVars);
				}
				if(DISPLAY_MAX)
					displayGraph(_maxDD, "max-" + iter);

				flushCaches();
			}

			_valueDD = _maxDD;
			time[iter] = GetElapsedTime();
			num_nodes[iter] = _context.getNodeCount(_valueDD);
			num_branches[iter] = _context.getBranchCount(_valueDD);
			_logStream.println("Value function size @ end of iteration " + iter + 
					": " + num_nodes[iter] + " nodes = " + 
					num_branches[iter] + " cases" + " in " + time[iter] + " ms");

			if (DISPLAY_V) 
				displayGraph(_valueDD, "V_"+iter+".dot");
			
//			if (DISPLAY_2D) {
//				TestXADDDist plot = new TestXADDDist();
//				HashMap<String,Boolean> bvars = new HashMap<String, Boolean>();
//				HashMap<String,Double> dvars = new HashMap<String, Double>();
//				if (_context._alBooleanVars.size()>0) bvars.put(_context._alBooleanVars.get(0), false);
//				plot.PlotXADD(_context, _valueDD, _context._hmMinVal.get("x1"), 1, _context._hmMaxVal.get("x1"), bvars,dvars, "x1", "V^"+iter);
//			}
		}

		flushCaches();	
		
		_logStream.println("\nValue iteration complete!");
		_logStream.println(max_iter + " iterations took " + GetElapsedTime() + " ms");
		_logStream.println("Canonical / non-canonical: " + XADD.OperExpr.ALREADY_CANONICAL + " / " + XADD.OperExpr.NON_CANONICAL);

		_logStream.println("\nIteration Results summary");
		for (int i = 1; i <= max_iter; i++) {
			String branch_count = num_branches[i] >= 0 
			? "" + num_branches[i] : " > " + XADD.MAX_BRANCH_COUNT; 
			_logStream.println("Iter " + i + ": nodes = " + num_nodes[i] + "\tbranches = " + branch_count + "\ttime = " + time[i] + " ms");
		}

		return iter;
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

		for (CAction a : _hmName2Action.values()) {
			_context.addSpecialNode(a._reward);
			for (Integer xadd : a._hmVar2DD.values())
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
		sb.append("CVars:       " + _context._alContinuousVars + "\n");
		sb.append("Min-values:  " + _context._hmMinVal + "\n");
		sb.append("Max-values:  " + _context._hmMaxVal + "\n");
		sb.append("BVars:       " + _context._alBooleanVars + "\n");
		sb.append("Order:       " + _context._alOrder + "\n");
		sb.append("Iterations:  " + _nIter + "\n");
		sb.append("Constraints (" + _alConstraints.size() + "):\n");
		for (Integer cons : _alConstraints) {
			sb.append("- " + _context.getString(cons) + "\n");
		}
		sb.append("Actions (" + _hmName2Action.size() + "):\n");
		for (CAction a : _hmName2Action.values()) {
			sb.append("\n==> " + a);
		}
		sb.append("\n");

		if (display_value) {
			Graph g = _context.getGraph(_valueDD);
			//g.launchViewer(1300, 770);
		}

		return sb.toString();
	}

	public void displayGraph(int xadd_id, String label) {
		Graph g = _context.getGraph(xadd_id);
		g.addNode("_temp_");
		g.addNodeLabel("_temp_", label);
		g.addNodeShape("_temp_", "square");
		g.addNodeStyle("_temp_", "filled");
		g.addNodeColor("_temp_", "lightblue");
		g.launchViewer(1300, 770);
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
	
	public static void usage() {
		System.out.println("\nMust enter: MDP-filename");
		System.out.println("\nMust enter: number of iterations");
		System.exit(1);
	}

	public static void main(String args[]) {
		if (args.length != 2) {
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
			System.exit(1);
		}
		catch(ArrayIndexOutOfBoundsException aiobe){
			System.out.println("\nIMissing argument\n");
			usage();
		}

		// Build a CAMDP, display, solve
		CAMDP mdp = new CAMDP(filename);
		System.out.println(mdp.toString(false, false));

		int iter_used = mdp.solve(iter);
		System.out.println("\nSolution complete, required " + 
				iter_used + " / " + iter + " iterations.");
	}
}
