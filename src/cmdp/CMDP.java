//////////////////////////////////////////////////////////////////////////
//
// File:     MDP.java
// Author:   Scott Sanner, University of Toronto (ssanner@cs.toronto.edu)
// Date:     9/1/2003
//
// Description:
//
//   An MDP inference package that uses both Tables, ADDs, AADDs as the
//   underlying computational mechanism.  (All via the logic.add.FBR
//   interface.)  See SPUDD (Hoey et al, UAI 1999) for more details on the
//   algorithm.
//
//////////////////////////////////////////////////////////////////////////

// Package definition
package cmdp;

// Packages to import
import graph.Graph;

import java.io.*;
import java.math.*;
import java.text.*;
import java.util.regex.*;
import java.util.*;

import xadd.XADD;
import xadd.XADD.ArithExpr;
import xadd.XADD.XADDINode;

// DD & FBR interfaces

/**
 * Main CMDP inference class
 * 
 * @version 1.0
 * @author Scott Sanner
 * @language Java (JDK 1.5)
 **/
public class CMDP {

	// TODO: Handle reduce variable ordering correctly on substitution... write a 
	// general ordering correction algorithm (also debug checking routine)
		
	// TODO: Canonical form for expressions
	// -- convert MINUS to SUM of negated products
	// -- convert sum of products of constants and vars
	// -- for now products don't need to be exponentiated, but should later
	
	// TODO: Correct min/max evaluation for canonical form... if negated, reverse?

	// TODO: Use subtraction < 0 for introduced min/max comparison decisions

	// TODO: Allow (1) proper reading of MDPs with boolean variables and handling
	//       of boolean variables during regression... (2) any diagram with a boolean
	//       bvariable at the head has to be split into a dual action diagram
	//       and summed out, (3) any ivariable simply has to be summed out. 
	
	// TODO: Integrate LP-Solve
	
	/* Constants */
	public final static boolean DISPLAY_Q = true;
	public final static boolean DISPLAY_V = true;
	public final static boolean ALWAYS_FLUSH = false; // Always flush DD caches?
	public final static double FLUSH_PERCENT_MINIMUM = 0.3d; // Won't flush until < amt

	/* For printing */
	public static DecimalFormat _df = new DecimalFormat("#.###");

	/* Static variables */
	public static long _lTime; // For timing purposes
	public static Runtime RUNTIME = Runtime.getRuntime();

	/* Local vars */
	public XADD _context = null;
	public ArrayList<String> _alCVars; 
	public ArrayList<String> _alBVars; 
	public ArrayList<String> _alIVars; 

	public Integer _rewardDD; // The reward for this CMDP
	public Integer _valueDD; // The resulting value function once this CMDP has
							// been solved
	public Integer _maxDD;
	public Integer _prevDD;
	public BigDecimal _bdDiscount;   // Discount (gamma) for CMDP
	public Integer    _nIter; // Tolerance (gamma) for CMDP
	
	public HashMap<String,Action> _hmName2Action;

	////////////////////////////////////////////////////////////////////////////
	// Constructors
	////////////////////////////////////////////////////////////////////////////

	/**
	 * Constructor - filename
	 **/
	public CMDP(String filename) {
		this(HierarchicalParser.ParseFile(filename));
	}

	/**
	 * Constructor - pre-parsed file
	 **/
	public CMDP(ArrayList input) {
		_context = new XADD();
		_prevDD = _maxDD = _rewardDD = _valueDD = null;
		_bdDiscount = new BigDecimal("" + (-1));
		_nIter = null;
		_hmName2Action = new HashMap<String,Action>();

		buildCMDP(input);
	}

	////////////////////////////////////////////////////////////////////////////
	// Generic CMDP Inference Methods
	////////////////////////////////////////////////////////////////////////////

	/**
	 * CMDP inference methods
	 **/
	public int solve(int max_iter) {

		// ////////////////////////////////////////////////////////////
		// Set value function equal to reward
		// ////////////////////////////////////////////////////////////
		_valueDD = _rewardDD;
		
		// Other initialization
		int iter = 0;

		// ////////////////////////////////////////////////////////////
		// Iterate until convergence (or max iterations)
		// ////////////////////////////////////////////////////////////
		ResetTimer();
		
		// Only use file number of iterations if not overridden by command line
		if (max_iter < 0)
			max_iter = _nIter;
		
		while (iter++ < max_iter) {

			// Error decreasing?
			System.out.println("Iteration #" + iter + ", " + MemDisplay()
					+ " bytes, " + GetElapsedTime() + " ms");

			// Prime diagram
			_prevDD = _valueDD;
			
			// TODO: Handle boolean variables
			//_valueDD = _context.remapGIDsInt(_valueDD, _hmPrimeRemap);

			//////////////////////////////////////////////////////////////
			// Iterate over each action
			//////////////////////////////////////////////////////////////
			for (Map.Entry<String,Action> me : _hmName2Action.entrySet()) {

				//////////////////////////////////////////////////////////////
				// Regress the current value function through each action
				//////////////////////////////////////////////////////////////
				int regr = regress(_valueDD, me.getValue());

				if (DISPLAY_Q) {
					Graph g = _context.getGraph(regr);
					g.addNode("_temp_");
					g.addNodeLabel("_temp_", "Q^" + iter + "(" + me.getKey() + ")");
					g.addNodeShape("_temp_", "square");
					g.addNodeStyle("_temp_", "filled");
					g.addNodeColor("_temp_", "lightblue");

					// g.genDotFile(type + "value.dot");
					g.launchViewer(1300, 770);
				}

				// ////////////////////////////////////////////////////////////
				// Take the max over this action and the previous action
				// ////////////////////////////////////////////////////////////
				_maxDD = ((_maxDD == null) ? regr : 
					_context.apply(_maxDD, regr, XADD.MAX));
			}

			// ////////////////////////////////////////////////////////////
			// Discount the max'ed value function backup and add in reward
			// ////////////////////////////////////////////////////////////
			_valueDD = _context.apply(_rewardDD, 
							_context.scalarOp(_maxDD, _bdDiscount.doubleValue(), XADD.PROD), 
							XADD.SUM);

			if (DISPLAY_V) {
				Graph g = _context.getGraph(_valueDD);
				g.addNode("_temp_");
				g.addNodeLabel("_temp_", "V^" + iter);
				g.addNodeShape("_temp_", "square");
				g.addNodeStyle("_temp_", "filled");
				g.addNodeColor("_temp_", "lightblue");

				// g.genDotFile(type + "value.dot");
				g.launchViewer(1300, 770);
			}
		}

		// Flush caches and return number of iterations
		return iter;
	}

	/**
	 * Regress a DD through an action
	 **/
	public int regress(int vfun, Action a) {

		XADD.XADDNode n = _context.getNode(vfun);
		HashSet<String> vars = n.collectVars();
		ArrayList<String> var_names = new ArrayList<String>();
		ArrayList<XADD.XADDNode> node_list = new ArrayList<XADD.XADDNode>();
		ArrayList<XADD.ArithExpr> subst = new ArrayList<XADD.ArithExpr>();
		
		// This relies on the fact all necessary variables have been properly
		// defined and that there are no name clashes between continuous vars
		// and boolean vars.
		for (String var : vars) {
			String var_name = var + "'";
			Integer dd = a._hmVar2DD.get(var_name);
			if (dd != null) {
				var_names.add(var);
				node_list.add(_context.getNode(dd));
				subst.add(null);
			}
		}

		System.out.println("\n" + a._sName);
		System.out.println("Vars:  " + var_names);
		System.out.println("Node:  " + node_list);
		System.out.println("Subst: " + subst);
		
		// TODO: Deal with non-canonical XADD result (call reduce)
		return regress(node_list, var_names, subst, 0, vfun);//regress(_valueDD, a);

		/*
		// For every next-state var in Action, multiply by DD and sumOut var
		long max = -1;
		Iterator i = a._tmID2DD.entrySet().iterator();
		Object dd_ret = vfun;

		// Find what gids are currently in vfun (probs cannot introduce new
		// primed gids)
		Set gids = _context.getGIDs(vfun);
		if (VERBOSE_LEVEL >= 1) {
			System.out.println("Regressing action: " + a._sName + "\nGIDs: "
					+ gids);
		}

		// ////////////////////////////////////////////////////////////
		// For each next state variable in DBN for action 'a'
		// ////////////////////////////////////////////////////////////
		while (i.hasNext()) {

			Map.Entry me = (Map.Entry) i.next();
			Integer head_id = (Integer) me.getKey();

			// No use in multiplying by a gid that does not exist (and will sum
			// to 1)
			if (!gids.contains(head_id)) {
				if (VERBOSE_LEVEL >= 1) {
					System.out.println("Skipping " + head_id);
				}
				continue;
			}

			// Get the dd for this action
			Object dd = me.getValue();

			// Screen output
			if (VERBOSE_LEVEL >= 2) {
				System.out.println("  - Summing out: " + head_id);
			}

			// /////////////////////////////////////////////////////////////////
			// Multiply next state variable DBN into current value function
			// /////////////////////////////////////////////////////////////////
			dd_ret = _context.applyInt(dd_ret, dd, DD.ARITH_PROD);
			int regr_sz = _context.getGIDs(dd_ret).size();
			if (regr_sz > _nMaxRegrSz) {
				_nMaxRegrSz = regr_sz;
			}

			// /////////////////////////////////////////////////////////////////
			// Sum out next state variable
			// /////////////////////////////////////////////////////////////////
			dd_ret = _context.opOut(dd_ret, head_id.intValue(), DD.ARITH_SUM);
			// Cache maintenance
			if (flush_caches) {
				clearSaveNodes();
				saveNode(dd_ret);
				flushCaches();
			}
		}

		// Return regressed value function (which is now in terms of prev state
		// vars)
		return dd_ret;
		*/
	}
	
	// TODO: Deal with non-canonical XADD result (call reduce)
	public int regress(ArrayList<XADD.XADDNode> node_list, 
		ArrayList<String> var_names, ArrayList<XADD.ArithExpr> subst, 
		int index, int vfun) {
		
		// Check if at terminal
		if (index >= node_list.size()) {
		
			HashMap<String,ArithExpr> leaf_subs = new HashMap<String,ArithExpr>();
			for (int i = 0; i < var_names.size(); i++) 
				leaf_subs.put(var_names.get(i), subst.get(i));
			System.out.println("Substituting: " + leaf_subs + 
					"\ninto:" + _context.getString(vfun));
			return _context.substitute(vfun, leaf_subs);
		}
		
		// Must be nonterminal so continue to recurse
		return regress(node_list.get(index), node_list, var_names, subst, index, vfun);
	}
	
	// TODO: Deal with non-canonical XADD result (call reduce)
	// Recurses to leaves and returns substituted XADD
	public int regress(XADD.XADDNode cur, ArrayList<XADD.XADDNode> node_list, 
			ArrayList<String> var_names, ArrayList<XADD.ArithExpr> subst, 
			int index, int vfun) {
		
		if (cur instanceof XADD.XADDINode) {
			// Branch each way and recombine in new result
			XADD.XADDINode inode = (XADD.XADDINode)cur;
			XADD.XADDNode low = _context.getNode(inode._low);
			int new_low = regress(low, node_list, var_names, subst, index, vfun);
			XADD.XADDNode high = _context.getNode(inode._high);
			int new_high = regress(high, node_list, var_names, subst, index, vfun);
			
			// TODO: Deal with non-canonical XADD (ordering could change due to subst)
			return _context.getINode(inode._var, new_low, new_high);
			
		} else { 
			// Terminal, so identify substition and continue to next index
			XADD.XADDTNode tnode = (XADD.XADDTNode)cur;
			subst.set(index, tnode._expr);
			// This substitution uncovered, advance to next index
			int ret_node = regress(node_list, var_names, subst, index + 1, vfun);
			subst.set(index, null);
			return ret_node;
		}
	}
	
	////////////////////////////////////////////////////////////////////////////
	// DD Cache Maintenance
	////////////////////////////////////////////////////////////////////////////

//	/**
//	 * Clear nodes on save list
//	 **/
//	public void clearSaveNodes() {
//		_alSaveNodes.clear();
//	}
//
//	/**
//	 * Add node to save list
//	 **/
//	public void saveNode(Object dd) {
//		_alSaveNodes.add(dd);
//	}
//
//	/**
//	 * Frees up memory... only do this if near limit?
//	 **/
//	public void flushCaches() {
//		if (!ALWAYS_FLUSH
//				&& ((double) RUNTIME.freeMemory() / (double) RUNTIME
//						.totalMemory()) > FLUSH_PERCENT_MINIMUM) {
//			return; // Still enough free mem to exceed minimum requirements
//		}
//
//		_context.clearSpecialNodes();
//		Iterator i = _hmName2Action.values().iterator();
//		while (i.hasNext()) {
//			Action a = (Action) i.next();
//			Iterator j = a._hsTransDDs.iterator();
//			while (j.hasNext()) {
//				_context.addSpecialNode(j.next());
//			}
//		}
//		_context.addSpecialNode(_rewardDD);
//		_context.addSpecialNode(_valueDD);
//		if (_maxDD != null)
//			_context.addSpecialNode(_maxDD);
//		if (_prevDD != null)
//			_context.addSpecialNode(_prevDD);
//
//		Iterator j = _alSaveNodes.iterator();
//		while (j.hasNext()) {
//			_context.addSpecialNode(j.next());
//		}
//		_context.flushCaches(false);
//	}

//	public double getRewardRange() {
//		return _context.getMaxValue(_rewardDD)
//				- _context.getMinValue(_rewardDD);
//	}

	////////////////////////////////////////////////////////////////////////////
	// CMDP Construction Methods
	////////////////////////////////////////////////////////////////////////////

	/**
	 * CMDP construction methods
	 **/
	public void buildCMDP(ArrayList input) {

		if (input == null) {
			System.out.println("Empty input file!");
			System.exit(1);
		}

		Iterator i = input.iterator();
		Object o;

		// Set up variables
		o = i.next();
		if (!(o instanceof String) || !((String) o).equalsIgnoreCase("cvariables")) {
			exit("Missing cvariable declarations: " + o);
		}
		o = i.next();
		_alCVars = (ArrayList<String>) ((ArrayList) o).clone();		
		o = i.next();
		if (!(o instanceof String) || !((String) o).equalsIgnoreCase("min-values")) {
			exit("Missing min-values declarations: " + o);
		}
		o = i.next();
		for (int index = 0; index < _alCVars.size(); index++) {
			String var = _alCVars.get(index);
			String val = ((ArrayList) o).get(index).toString();
			if (!val.trim().equalsIgnoreCase("x")) try {
				double min_val = Double.parseDouble(val);
				_context._hmMinVal.put(var, min_val);
			} catch (NumberFormatException nfe) {
				System.out.println("\nIllegal min-value: " + var + " = " + val + " @ index " + index);
				System.exit(1);
			}
		}
		o = i.next();
		if (!(o instanceof String) || !((String) o).equalsIgnoreCase("max-values")) {
			exit("Missing max-values declarations: " + o);
		}
		o = i.next();
		for (int index = 0; index < _alCVars.size(); index++) {
			String var = _alCVars.get(index);
			String val = ((ArrayList) o).get(index).toString();
			if (!val.trim().equalsIgnoreCase("x")) try {
				double max_val = Double.parseDouble(val);
				_context._hmMaxVal.put(var, max_val);
			} catch (NumberFormatException nfe) {
				System.out.println("\nIllegal max-value: " + var + " = " + val + " @ index " + index);
				System.exit(1);
			}
		}
		o = i.next();
		if (!(o instanceof String) || !((String) o).equalsIgnoreCase("bvariables")) {
			exit("Missing bvariable declarations: " + o);
		}
		// TODO: Add all boolean vars to XADD
		o = i.next();
		//System.out.println(o);
		_alBVars = (ArrayList<String>) ((ArrayList) o).clone();
		for (String var : _alBVars) {
			_context.getVarIndex(_context.new BoolDec(var), true);
			_context.getVarIndex(_context.new BoolDec(var + "'"), true);
		}
		// TODO: Add all intermediate boolean vars to XADD
		o = i.next();
		if (!(o instanceof String) || !((String) o).equalsIgnoreCase("ivariables")) {
			exit("Missing ivariable declarations: " + o);
		}
		o = i.next();
		_alIVars = (ArrayList<String>) ((ArrayList) o).clone();

		// Set up actions
		while (true) {
			o = i.next();
			if (!(o instanceof String)
					|| !((String) o).equalsIgnoreCase("action")) {
				break;
			}

			// o == "action"
			String aname = (String) i.next();
			HashMap<String,ArrayList> cpt_map = new HashMap<String,ArrayList>();

			o = i.next();
			while (!((String) o).equalsIgnoreCase("endaction")) {
				cpt_map.put((String) o, (ArrayList) i.next());
				o = i.next();
			}

			_hmName2Action.put(aname, new Action(this, aname, cpt_map));
		}

		// Set up reward
		if (!(o instanceof String) || !((String) o).equalsIgnoreCase("reward")) {
			System.out.println("Missing reward declaration: " + o);
			System.exit(1);
		}
		ArrayList reward = (ArrayList) i.next();

		_rewardDD = _context.buildCanonicalXADD(reward);

		// Read discount and tolerance
		o = i.next();
		if (!(o instanceof String)
				|| !((String) o).equalsIgnoreCase("discount")) {
			System.out.println("Missing discount declaration: " + o);
			System.exit(1);
		}
		_bdDiscount = ((BigDecimal) i.next());

		o = i.next();
		if (!(o instanceof String)
				|| !((String) o).equalsIgnoreCase("iterations")) {
			System.out.println("Missing iterations declaration: " + o);
			System.exit(1);
		}
		_nIter = new Integer((String)i.next());
	}

	public void exit(String msg) {
		System.out.println(msg);
		System.exit(1);
	}
	
	////////////////////////////////////////////////////////////////////////////
	// Miscellaneous
	////////////////////////////////////////////////////////////////////////////

	public String toString() {
		return toString(false, false);
	}

	public String toString(boolean display_reward, boolean display_value) {
		StringBuffer sb = new StringBuffer();
		sb.append("\nCMDP Definition:\n===============\n");
		sb.append("CVars:       " + _alCVars + "\n");
		sb.append("Min-values:  " + _context._hmMinVal + "\n");
		sb.append("Max-values:  " + _context._hmMaxVal + "\n");
		sb.append("BVars:       " + _alBVars + "\n");
		sb.append("IVars:       " + _alIVars + "\n");
		sb.append("Order:       " + _context._alOrder + "\n");
		sb.append("Discount:    " + _bdDiscount + "\n");
		sb.append("Iterations:  " + _nIter + "\n");
		sb.append("Actions (" + _hmName2Action.size() + "):\n");
		for (Action a : _hmName2Action.values()) {
			sb.append("\n==> " + a);
		}
		sb.append("\n");

		if (display_reward) {
			Graph g = _context.getGraph(_rewardDD);
			g.launchViewer(1300, 770);
		}

		if (display_value) {
			Graph g = _context.getGraph(_valueDD);
			g.launchViewer(1300, 770);
		}

		return sb.toString();
	}

	// Reset elapsed time
	public void ResetTimer() {
		_lTime = System.currentTimeMillis(); 
	}

	// Get the elapsed time since resetting the timer
	public long GetElapsedTime() {
		return System.currentTimeMillis() - _lTime;
	}

	public static String MemDisplay() {
		long total = RUNTIME.totalMemory();
		long free = RUNTIME.freeMemory();
		return total - free + ":" + total;
	}

	////////////////////////////////////////////////////////////////////////////
	// Testing Interface
	////////////////////////////////////////////////////////////////////////////

	/**
	 * Basic testing interface.
	 **/
	public static void main(String args[]) {
		if (args.length < 1 || args.length > 2) {
			System.out.println("\nMust enter: MDP-filename [iterations]");
			System.exit(1);
		}

		// Parse problem filename
		String filename = args[0];

		// Parse iterations
		int iter = -1;
		if (args.length == 2) try {
			iter = Integer.parseInt(args[1]);
		} catch (NumberFormatException nfe) {
			System.out.println("\nIllegal iteration value\n");
			System.exit(1);
		}

		// Build a CMDP, display, solve
		CMDP mdp1 = new CMDP(filename);
		System.out.println(mdp1.toString(false, false));
		//System.exit(1);
		
		int  iter1 = mdp1.solve(iter);
		long time1 = mdp1.GetElapsedTime();
	}
}
