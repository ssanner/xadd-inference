//////////////////////////////////////////////////////////////////////
//
// Extended Algebraic Decision Diagrams
//
// Decision tests have labels -- either boolean variables or arithmetic
//                               expressions (could be relational expressions
//                               as well -- same as FOADD, but not used here)
//                               Need a list of decisions (type: var, expr) 
//                               leaves are expressions (can be constants)
//
// TODO: Need a don't care node to handle unreachable values and need
//       to modify computeTermNode to handle these different values.
//
// TODO: Do we need to prune dead branches after case-max for correctness?  
//       Don't think so.  Do we need to prune dead branches after max_x?
//       Potentially... this is currently done, but may be wasteful.
//
//////////////////////////////////////////////////////////////////////

package xadd;

import graph.Graph;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import logic.kb.fol.FOPC;
import lpsolve.LP;
import lpsolve.LpSolve;
import util.IntPair;
import util.IntTriple;
import util.Pair;
import cmdp.HierarchicalParser;

/**
 * General class for implementation of ADD data structure
 **/
public class XADD {

	// Visualization
	public final static boolean GRAPH_USE_COLOR = true;

	// Flags
	public final static boolean USE_CANONICAL_NODES = true; // Store nodes in canonical format?
	public final static boolean CHECK_MIN_MAX = false; // Will be buggy if min/max of expr
	// not at extrema of domain

	public final static boolean USE_MINUS_COMP = false; // Error, all max/min comps reduce to false!
	public final static int MAX_BRANCH_COUNT = 1000000;

	public final static double TOL = 1e-10d;
	
	// Debug
	public final static boolean CHECK_LOCAL_ORDERING = true;
	public final static boolean SHOW_DECISION_EVAL = false;
	public final static boolean DEBUG_EVAL_RANGE = false;
	public final static boolean DEBUG_CONSTRAINTS = false;
	public final static boolean REDUCE_LP = true;

	// Operators
	public final static int UND = 0;
	public final static int SUM = 1;
	public final static int MINUS = 2;
	public final static int PROD = 3;
	public final static int DIV = 4;
	public final static int MAX = 5;
	public final static int MIN = 6;
	public final static int RESTRICT_LOW = 7;
	public final static int RESTRICT_HIGH = 8;
	public final static int EQ = 9;
	public final static int NEQ = 10;
	public final static int GT = 11;
	public final static int GT_EQ = 12;
	public final static int LT = 13;
	public final static int LT_EQ = 14;
	public final static int LINEARIZE = 15;
	public final static int ERROR = 16;

	public final static String[] _aOpNames = {/* 0 */"UND",
		/* 1 */"+", "-", "*", "/", "max", "min", "|l", "|h",
		/* 9 */"=", "!=", ">", ">=", "<", "<=", 
		/* 15 */ "LINEARIZE", "ERROR" };
		
	// Constants
	public final static String STRING_TAB = "   ";
	public final static DecimalFormat _df = new DecimalFormat("#.########################");
	public final static DecimalFormat _df_unformatted = new DecimalFormat("#.########################");

	public final static ArithExpr ZERO = new DoubleExpr(0d);
	public final static ArithExpr ONE = new DoubleExpr(1d);
	public final static ArithExpr NEG_ONE = new DoubleExpr(-1d);

	// Variable Maintenance
	public ArrayList<Decision> _alOrder = new ArrayList<Decision>();

	// Node Maintenance
	public int _markCounter = 1;
	public int _nodeCounter = 1;
	public HashMap<XADDNode, Integer> _hmNode2Int = new HashMap<XADDNode, Integer>();
	public HashMap<Integer, XADDNode> _hmInt2Node = new HashMap<Integer, XADDNode>();

	// Reduce & Apply Caches
	public HashMap<IntTriple, Integer> _hmReduceCache = new HashMap<IntTriple, Integer>();
	public HashMap<IntPair, Integer>   _hmReduceLeafOpCache = new HashMap<IntPair, Integer>();
	public HashMap<IntPair, Integer>   _hmReduceAnnotateCache = new HashMap<IntPair, Integer>();
	public HashMap<IntTriple, Integer> _hmApplyCache = new HashMap<IntTriple, Integer>();
	public HashMap<XADDINode, HashSet<String>> _hmINode2Vars = new HashMap<XADDINode, HashSet<String>>();

	public ArrayList<String> _alBooleanVars = new ArrayList<String>();
	public ArrayList<String> _alContinuousVars;

	public HashMap<String, Double> _hmMinVal = new HashMap<String, Double>();
	public HashMap<String, Double> _hmMaxVal = new HashMap<String, Double>();

	// Flush
	public HashSet<Integer> _hsSpecialNodes = new HashSet<Integer>();
	public HashMap<XADDNode, Integer> _hmNode2IntNew = new HashMap<XADDNode, Integer>();
	public HashMap<Integer, XADDNode> _hmInt2NodeNew = new HashMap<Integer, XADDNode>();


	// Methods
	public XADD() {
		// Ensure that the 0th decision ID is invalid
		_alOrder.add(new NullDec());
		_alContinuousVars = new ArrayList<String>();
	}

	public ArrayList<String> getContinuousVarList() {
		return _alContinuousVars;
	}

	public void setContinuousVarList(ArrayList<String> continuousVarList) {
		_alContinuousVars = continuousVarList;
	}

	public int getBoolVarIndex(String bool_name) {
		return getVarIndex(new BoolDec(bool_name), false);
	}

	public int getVarIndex(Decision d) {
		return getVarIndex(d, false);
	}

	// Note: var index can never be 0, negative var index now means negated decision
	public int getVarIndex(Decision d, boolean create) {

		if (USE_CANONICAL_NODES) {
			//System.out.println(">> Before canonical: " + d);

			d = d.makeCanonical();
			//TODO
			//System.out.println(">> After canonical: " + d);
		}

		int index = _alOrder.indexOf(d);

		// If not found, try negating d
		if (index < 0 && d instanceof ExprDec) {
			CompExpr comp = ((ExprDec)d)._expr;
			CompExpr neg_comp = new CompExpr( CompExpr.flipCompOper(comp._type), comp._lhs, comp._rhs );
			Decision neg_d = new ExprDec(neg_comp).makeCanonical();

			index = _alOrder.indexOf(neg_d);
			if (index > 0)
				index = -index;
			else
				index = 0; // No valid decision has index 0
		} else if (index < 0) {
			index = 0; // No valid decision has index 0
		}

		if (index != 0 || !create) // Valid index found
			return index;
		else {
			_alOrder.add(d);
			return _alOrder.size() - 1;
		}
	}

	public int getTermNode(XADDTNode n) {
		return getTermNode(n._expr, n._annotate); 
	}

	public int getTermNode(ArithExpr e) {
		return getTermNode(e, null);
	}

	// This version can handle annotations
	public XADDTNode _tempTNode = new XADDTNode(null,null);
	
	public int getTermNode(ArithExpr e, Object annotation) {
		
		if (USE_CANONICAL_NODES)
			e = (ArithExpr) e.makeCanonical();

		_tempTNode.set(e, annotation);
		Integer id = _hmNode2Int.get(_tempTNode);
		if (id == null) {
			// Not in cache so create
			id = _nodeCounter;
			XADDTNode node = new XADDTNode(e, annotation);
			_hmNode2Int.put(node, id);
			_hmInt2Node.put(id, node);
			_nodeCounter++;
		}
		return id;
	}

	public int getVarNode(Decision d, double low_val, double high_val) {
		int var = getVarIndex(d, true);
		int low = getTermNode(new DoubleExpr(low_val));
		int high = getTermNode(new DoubleExpr(high_val));
		return getINode(var, low, high);
	}

	public XADDNode getNode(int node_id) {
		return _hmInt2Node.get(node_id);
	}

	public XADDINode _tempINode = new XADDINode(-1, -1, -1);

	public int getINode(int var, int low, int high) {

		if (var < 0) {
			int temp = low;
			low = high;
			high = temp;
			var = -var;
		}

		if (low < 0 || high < 0) {
			System.err.println("Invalid node (low,high)=(" + low + "," + high + ") for var: " + var);
			new Exception().printStackTrace();
			System.exit(1);
		}

		// First check if low == high... in this case, just perform the
		// obvious equivalent reduction (this saves headaches later)
		if (low == high) {
			return low;
		}

		// Handle tautological variables
		Decision d = _alOrder.get(var);
		if (d instanceof TautDec) {
			if (((TautDec) d)._bTautology)
				return high;
			else
				return low;
		}

		// Retrieve inode (and create if it does not exist)
		_tempINode.set(var, low, high);
		Integer id = _hmNode2Int.get(_tempINode);
		if (id == null) {
			// Not in cache so create
			id = _nodeCounter;
			XADDINode node = new XADDINode(var, low, high);
			_hmNode2Int.put(node, id);
			_hmInt2Node.put(id, node);
			_nodeCounter++;
		}
		return id;
	}

	// Returns all variables in this XADD
	public HashSet<String> collectVars(int id) {
		XADDNode n = _hmInt2Node.get(id);
		if (n == null) {
			System.out.println("ERROR: " + id + " expected in node cache, but not found!");
			new Exception().printStackTrace();
			System.exit(1);
		}
		return n.collectVars();
	}

	// //////////////////////////////////////////////////////////////

	public int buildCanonicalXADD(ArrayList l) {
		if (l.size() == 1) {
			// Terminal node
			//System.out.println("Parsing: '" + l.get(0) + "'");
			if (!(l.get(0) instanceof String)) {
				System.err.println("Cannot parse terminal: '" + l.get(0) + "'\n... make sure terminals are surrounded in [ ]");
				System.exit(1);
			}
			String s = (String) l.get(0);
			// Work around a parser issue with singleton vars in brackets
			if (s.startsWith("["))
				s = s.substring(1, s.length() - 1);
			int n = ArithExpr.parseIntoXADD(this, s);
			if (n < 0) {
				System.err.println("Failed to parse: '" + s + "'");
				System.exit(1);
			}
			return n;
		} else if (l.size() == 3) {
			// Internal node
			String expr = ((String) l.get(0));
			Decision d = null;
			// System.out.println("Expr: " + expr);
			if (expr.startsWith("[")) {
				CompExpr c = CompExpr.parse(expr);
				if (c != null) {
					// System.out.println("CompExpr: " + c);
					d = new ExprDec(c);
				}
			} else {
				d = new BoolDec(expr);
			}

			if (d == null) {
				System.out.println("Could not buildNonCanonicalXADD for terminal '" + l + "'");
				return -1;
			} else {
				// System.out.println("Var expr: " + d);
				int var = getVarIndex(d, true);
				int high = buildCanonicalXADD((ArrayList) l.get(1));
				int low = buildCanonicalXADD((ArrayList) l.get(2));

				int T_ZERO = getTermNode(ZERO);
				int T_ONE = getTermNode(ONE);
				int ind_true = getINode(var, /* low */T_ZERO, /* high */T_ONE);
				int ind_false = getINode(var, /* low */T_ONE, /* high */T_ZERO);
				int true_half = applyInt(ind_true, high, PROD); // Note: this enforces canonicity so
				int false_half = applyInt(ind_false, low, PROD); // can use applyInt rather than apply

				// System.out.println("New inode: " + index + " - " + low + ", " + high);
				// System.out.println("Var: " + _alOrder.get(index));
				return applyInt(true_half, false_half, SUM);
			}
		} else {
			// Unknown
			System.out.println("Could not buildNonCanonicalXADD for " + l.size() + " args '" + l + "'");
			return -1;
		}
	}

	public int buildNonCanonicalXADD(ArrayList l) {
		if (l.size() == 1) {
			// Terminal node
			String s = (String) l.get(0);
			// Work around a parser issue with singleton vars in brackets
			if (s.startsWith("["))
				s = s.substring(1, s.length() - 1);
			ArithExpr a = ArithExpr.parse(s);
			if (a == null) {
				// System.out.println("Could not buildNonCanonicalXADD for terminal '" + (String)l.get(0) + "'");
				return -1;
			} else
				return getTermNode(a);
		} else if (l.size() == 3) {
			// Internal node
			String expr = ((String) l.get(0));
			Decision d = null;
			// System.out.println("Expr: " + expr);
			if (expr.startsWith("[")) {
				CompExpr c = CompExpr.parse(expr);
				if (c != null) {
					// System.out.println("CompExpr: " + c);
					d = new ExprDec(c);
				}
			} else {
				d = new BoolDec(expr);
			}

			if (d == null) {
				System.out.println("Could not buildNonCanonicalXADD for terminal '" + l + "'");
				return -1;
			} else {
				// System.out.println("Var expr: " + d);
				int index = getVarIndex(d, true);
				int high = buildNonCanonicalXADD((ArrayList) l.get(1));
				int low = buildNonCanonicalXADD((ArrayList) l.get(2));
				// System.out.println("New inode: " + index + " - " + low + ", " + high);
				// System.out.println("Var: " + _alOrder.get(index));
				return getINode(index, low, high);
			}
		} else {
			// Unknown
			System.out.println("Could not buildNonCanonicalXADD for " + l.size() + " args '" + l + "'");
			return -1;
		}
	}

	public int getBranchCount(int id) {
		XADDNode root = _hmInt2Node.get(id);
		return root.countBranches();
	}

	public int getNodeCount(int id) {
		XADDNode root = _hmInt2Node.get(id);
		return root.collectNodes().size();
	}

	// ////////////////////////////////////////////////////////////////////

	public Graph getGraph(int id) {
		Graph g = new Graph(true /* directed */, false /* bottom-to-top */, false /* left-to-right */, true /* multi-links */);
		XADDNode root = _hmInt2Node.get(id);
		root.toGraph(g, id);
		return g;
	}

	public String getString(int id) {
		return getString(id, true);
	}

	public String getString(int id, boolean format) {
		XADDNode root = _hmInt2Node.get(id);
		int num_nodes = getNodeCount(id);
		if (num_nodes > 30)
			return "[XADD " + id + " contains " + num_nodes + " nodes... too large to print]";
		else
			return root.toString(format);
	}

	// ////////////////////////////////////////////////////////////////////

	// TODO: Implement garbage collection with counter increment trick
	//       and marking.

	public IntTriple _tempReduceKey = new IntTriple(-1, -1, -1);

	public int reduce(int node) {
		return reduceOp(node, -1, -1);
	}

	public int opOut(int node_id, int var_id, int op) {
		int ret = reduceOp(node_id, var_id, op);

		// operations like sum and product may get decisions out of order... really?
		// (reduce low / high should not do this)
		// TODO: is this right?
		if (op == SUM || op == PROD)
			return makeCanonical(ret);
		else
			return ret;
	}

	private int reduceOp(int node_id, int var_id, int op) {

		if (var_id < 0 && op >= 0) {
			System.err.println("Illegal op-code '" + op + "' for var id: " + var_id);
			new Exception().printStackTrace();
			System.exit(1);
		}

		Integer ret = null;
		XADDNode n = _hmInt2Node.get(node_id);
		if (n == null) {
			System.out.println("ERROR: " + node_id + " expected in node cache, but not found!");
			new Exception().printStackTrace();
			System.exit(1);
		}

		// A terminal node should be reduced (and cannot be restricted)
		// by default if hashing and equalitay testing are working in getTNode
		if (n instanceof XADDTNode) {
			return node_id; // Assuming that to have a node id means canonical
		}

		// If its an internal node, check the reduce cache
		_tempReduceKey.set(node_id, var_id, op);
		if ((ret = _hmReduceCache.get(_tempReduceKey)) != null) {
			// System.out.println("In cache, returning: " + qret);
			return ret;
		}

		XADDINode inode = (XADDINode) n;

		int low = -1;
		int high = -1;
		if (op != RESTRICT_HIGH || var_id != inode._var) {
			low = reduceOp(inode._low, var_id, op);
		}
		if (op != RESTRICT_LOW || var_id != inode._var) {
			high = reduceOp(inode._high, var_id, op);
		}

		if (op != -1 && var_id != -1 && var_id == inode._var) {
			// ReduceOp
			if (op == RESTRICT_LOW) {
				ret = low;
			} else if (op == RESTRICT_HIGH) {
				ret = high;
			} else if (op == SUM || op == PROD) { // op \in {MINUS, DIV} not commutative
				// not obvious if low or high comes first
				ret = apply(low, high, op); // may not be canonical (why?), but will be fixed by makeCanonical
			} else {
				System.out.println("ERROR: id:" + op + "/ name:" + _aOpNames[op]
						+ " expected in node cache, but not found!  (Or illegal op.)");
				new Exception().printStackTrace();
				System.exit(1);
			}
		} else {
			// Standard Reduce: getInode will handle the case of low == high
			ret = getINode(inode._var, low, high);
		}

		// Put return value in cache and return
		_hmReduceCache.put(new IntTriple(node_id, var_id, op), ret);
		return ret;
	}

	// NOTE: this linearizes one decision variable at a time, need to iterate
	//       in case additional variables in decisions can be linearized;
	//       iteration terminates when the node_id has not changed
	public int reduceLinearize(int node_id) {
		int prev_node_id = -1;
		while (node_id != prev_node_id) {
			System.out.println("Reduce linearize... " + node_id + " / " + prev_node_id);
			prev_node_id = node_id;
			node_id = reduceLinearizeInt(node_id);
			node_id = makeCanonical(node_id); // Nodes may have gotten out of order
		}
		return node_id;
	}

	private int reduceLinearizeInt(int node_id) {

		Integer ret = null;
		XADDNode n = _hmInt2Node.get(node_id);
		if (n == null) {
			System.out.println("ERROR: " + node_id + " expected in node cache, but not found!");
					new Exception().printStackTrace();
			System.exit(1);
		}

		// A terminal node should be reduced (and cannot be restricted)
		//by default if hashing and equality testing are working in getTNode
		if (n instanceof XADDTNode) {
			return node_id; // Assuming that to have a node id means canonical
		}

		// If its an internal node, check the reduce cache
		_tempReduceKey.set(node_id, -1, LINEARIZE);
		if ((ret = _hmReduceCache.get(_tempReduceKey)) != null) {
			//System.out.println("In cache, returning: " + qret);
			return ret;
		}

		XADDINode inode = (XADDINode) n;

		int low  = reduceLinearizeInt(inode._low);
		int high = reduceLinearizeInt(inode._high);

		Decision d = _alOrder.get(inode._var);

		// For now we'll only do linearization of quadratic decisions
		Integer xadd_dec_true = linearizeQuadDecision(d);
		if (xadd_dec_true == null)
			ret = getINode(inode._var, low, high);
		else {
			int xadd_dec_false = applyInt(getTermNode(ONE), xadd_dec_true, MINUS); // can use applyInt rather than apply
			int true_half = applyInt(xadd_dec_true, high, PROD); // Note: this enforces canonicity so
			int false_half = applyInt(xadd_dec_false, low, PROD); // can use applyInt rather than apply
			ret = applyInt(true_half, false_half, SUM);
		}

		// Put return value in cache and return
		_hmReduceCache.put(new IntTriple(node_id,-1,LINEARIZE), ret);
		return ret;
	}

	// Returns XADD for true branch indicator of decision, otherwise null
	public Integer linearizeQuadDecision(Decision d) {

		if (!(d instanceof ExprDec))
			return null;

		ExprDec e = (ExprDec)d;
		OperExpr lhs = (OperExpr)e._expr._lhs;
		if (lhs._type != SUM)
			return null;

		HashSet<String> cont_vars = new HashSet<String>();
		d.collectVars(cont_vars);

		for (String var : cont_vars) {

			// Bookkeeping variables
			VarExpr varExpr = new VarExpr(var);
			boolean all_var = true;
			boolean higher_order = false;
			Double quad_coef = null;
			double linear_coef = 0d;
			double const_coef = 0d;

			// Set coefficients
			for (ArithExpr a : lhs._terms) {

				if (a instanceof OperExpr && ((OperExpr)a)._type == PROD) {

					// Count order of polynomial in var
					int count_var = 0;
					Double coef = null;
					for (ArithExpr a_sub : ((OperExpr)a)._terms) {
						if (a_sub.equals(varExpr))
							count_var++;
						else if (a_sub instanceof DoubleExpr)
							coef = ((coef == null) ? ((DoubleExpr)a_sub)._dConstVal
									: coef * ((DoubleExpr)a_sub)._dConstVal);
						else {
							all_var = false;
							break;
						}
					}

					// Set appropriate coefficients
					if (count_var > 2) {
						higher_order = true;
						break;
					} else if (count_var == 2)
						quad_coef = quad_coef == null ? coef : quad_coef + coef;
					else if (count_var == 1)
						linear_coef += coef;
					else if (count_var == 0)
						const_coef += coef;

				} else if (a instanceof VarExpr) {
					linear_coef += 1d;
				} else if (a instanceof DoubleExpr) {
					const_coef += ((DoubleExpr)a)._dConstVal;
				}
			}

			// If this is a quadratic expression in one variable, we can
			// linearize and return the true indicator XADD for it
			if (all_var && !higher_order && quad_coef != null) {

				//Build XADD for new decision true indicator and return
				// ax^2 + bx + c COMP 0
				// => x^2 + b/a*x COMP -c/a
				//      let d = b/a and e = -c/a
				//      if (a < 0) COMP = flip(COMP)
				// => x^2 + d*x + (d/2)^2 COMP (d/2)^2 + e
				// => (x + d/2)^2 COMP (d/2)^2 + e
				// => [if ((d/2)^2 + e) >= 0]
				//      (x + d/2) COMP SQRT((d/2)^2 + e)
				//      OR (x + d/2) flip(COMP) -SQRT((d/2)^2 + e)
				//    [else]
				//      substitute x/0 then makeCanonical 
				//      (to find out whether true or false since for any x, always on same side)
				double var_d = linear_coef / quad_coef;
				double var_e = -const_coef / quad_coef;

				//Build expressions for first and second inequality
				int comp_oper = e._expr._type;
				if (quad_coef < 0)
					comp_oper = CompExpr.flipCompOper(comp_oper);
				int flip_comp_oper = CompExpr.flipCompOper(comp_oper);
				OperExpr lhs_expr = new OperExpr(SUM, new VarExpr(var), new DoubleExpr(var_d / 2d));
				double rhs_pre_sqrt = var_e + var_d * var_d / 4d;
				if (rhs_pre_sqrt < 0) {
					// Check for imaginary roots... quadratic never crosses zero
					HashMap<String,ArithExpr> subst = new HashMap<String,ArithExpr>();
					subst.put(var, ZERO);
					TautDec new_dec = (TautDec)(new ExprDec(e._expr.substitute(subst)).makeCanonical());
					return getTermNode(new_dec._bTautology ? ONE : ZERO); // will be multiplied by true branch
				}
				double rhs_const = Math.sqrt(rhs_pre_sqrt);
				int var1_id = getVarIndex(new ExprDec(new CompExpr(comp_oper, lhs_expr, new 
						DoubleExpr(rhs_const))), true);
				int var2_id = getVarIndex(new ExprDec(new CompExpr(flip_comp_oper, lhs_expr, new 
						DoubleExpr(-rhs_const))), true);

				// Build XADD indicator from expressions
				// Note: disjunction a v b = 1 - (1 - a)*(1 - b)
				int T_ZERO = getTermNode(ZERO);
				int T_ONE = getTermNode(ONE);
				int expr1_xadd = getINode(var1_id, /* low */T_ONE, /* high */T_ZERO);
				int expr2_xadd = getINode(var2_id, /* low */T_ONE, /* high */T_ZERO);
				int ret_xadd = apply(T_ONE, apply(expr1_xadd, expr2_xadd, PROD), MINUS);
				System.out.println("LINEARIZE -- started with: " + e + "... returning\n" + 
						getString(ret_xadd));
				//System.exit(1);
				return ret_xadd;
			}
		} // Done processing 'var'

		return null; // We didn't find a quadratic before this
	}

	IntPair _tempReduceAnnotateKey = new IntPair(-1, -1);
	public int annotateXADD(int node_id, Object annotation)
	{
		Integer ret = null;
		XADDNode node = _hmInt2Node.get(node_id);
		if (node == null) {
			System.out.println("ERROR: " + node_id + " expected in node cache, but not found!");
			new Exception().printStackTrace();
			System.exit(1);
		}

		// Directly annotate terminal nodes only 
		if ((node instanceof XADDTNode)) {

			XADDTNode tnode = (XADDTNode)node;
			return getTermNode(tnode._expr, annotation);
		}

		// If its an internal node, first check the annotate cache
		_tempReduceAnnotateKey.set(node_id, annotation.hashCode());
		if 	((ret = _hmReduceAnnotateCache.get(_tempReduceAnnotateKey)) != null)
			return ret;

		// Not annotated before so recursively annotate
		XADDINode inode = (XADDINode) node;
		int low  = annotateXADD(inode._low, annotation);
		int high = annotateXADD(inode._high, annotation);
		ret = getINode(inode._var, low, high);
		_hmReduceAnnotateCache.put(new IntPair(node_id, annotation.hashCode()), ret);
		return ret;
	}

	public IntTriple _tempReduceLPKey = new IntTriple(-1, -1, -1);
	public HashMap<String, Integer> _cvar2ID = null;

	public int reduceLP(int node_id, List<String> cvars) {
		if (true /*_cvar2ID == null*/) {
			_cvar2ID = new HashMap<String, Integer>();
			int id = 0;
			for (String cvar : cvars)
				_cvar2ID.put(cvar, id++);
		}
		ArrayList<Integer> test_var = new ArrayList<Integer>();
		ArrayList<Boolean> test_dec = new ArrayList<Boolean>();
		for (int i = 0; i < _alOrder.size(); i++) {
			test_dec.add(null);
		}
		return reduceLP(node_id, test_var, test_dec);
	}

	private int reduceLP(int node_id, ArrayList<Integer> test_var, ArrayList<Boolean> test_dec) {

		Integer ret = null;
		XADDNode n = _hmInt2Node.get(node_id);
		if (n == null) {
			System.out.println("ERROR: " + node_id + " expected in node cache, but not found!");
			new Exception().printStackTrace();
			System.exit(1);
		}

		// A terminal node should be reduced (and cannot be restricted)
		// by default if hashing and equality testing are working in getTNode
		if (n instanceof XADDTNode) {
			return node_id; // Assuming that to have a node id means canonical
		}

		// TODO: Checking reduce cache means checking to see which constraints
		//       were used in pruning a branch
		//_tempReduceLPKey.set(node_id, test_var.hashCode(), test_dec.hashCode());
		//if (false && (ret = _hmReduceLPCache.get(_tempReduceLPKey)) != null) {
		//	// System.out.println("In cache, returning: " + qret);
		//	return ret;
		//}

		XADDINode inode = (XADDINode) n;

		if (isTestImplied(test_var, test_dec, inode._var, true)) {

			ret = reduceLP(inode._high, test_var, test_dec);
		} else if (isTestImplied(test_var, test_dec, inode._var, false)) {

			ret = reduceLP(inode._low, test_var, test_dec);
		} else {

			test_var.add(inode._var);
			test_dec.set(inode._var, false);
			int low = reduceLP(inode._low, test_var, test_dec);
			test_dec.set(inode._var, true);
			int high = reduceLP(inode._high, test_var, test_dec);
			test_dec.set(inode._var, null);
			test_var.remove(test_var.size() - 1);

			// Standard Reduce: getInode will handle the case of low == high
			ret = getINode(inode._var, low, high);
		}

		// Put return value in cache and return
		//_hmReduceLPCache.put(new IntTriple(node_id, test_var.hashCode(), test_dec.hashCode()), ret);
		return ret;
	}

	private boolean isTestImplied(ArrayList<Integer> test_var, ArrayList<Boolean> test_dec, int var_id, boolean dec) {

		if (test_var.size() == 0)
			return false;

		if (DEBUG_CONSTRAINTS) {
			System.out.println("===================\nisTestImplied: " + _alOrder.get(var_id) + " = " + dec);
			for (int cons : test_var) {
				System.out.println("- cons :: " + _alOrder.get(cons) + " = " + test_dec.get(cons));
			}
		}

		// if (a > b + c) == true makes system infeasible then it must be implied
		// that (a > b + c) == false (prove A => B by seeing if A^~B is infeasible)

		// Setup LP
		int nvars = _cvar2ID.size();
		double[] obj_coef = new double[nvars]; // default all zeros, which is what we want
		double[] lb = new double[nvars];
		double[] ub = new double[nvars];
		for (Map.Entry<String, Integer> me : _cvar2ID.entrySet()) {
			String cvar = me.getKey();
			Integer cvar_id = me.getValue();
			Double d_lb = this._hmMinVal.get(cvar);
			lb[cvar_id] = d_lb != null ? d_lb : -1e10d;
			Double d_ub = this._hmMaxVal.get(cvar);
			ub[cvar_id] = d_ub != null ? d_ub : 1e10d;
			obj_coef[cvar_id] = 1d;
		}

		LP lp = new LP(nvars, lb, ub, obj_coef, LP.MAXIMIZE);

		// Now add all constraints
		for (Integer constraint_id : test_var) {
			addConstraint(lp, constraint_id, test_dec.get(constraint_id));
		}

		// Finally add the negated decision to test
		addConstraint(lp, var_id, !dec);

		// Solve and get decision
		double[] soln = lp.solve();
		boolean implied = (lp._status == LpSolve.INFEASIBLE);
		//System.out.println(lp._status + " == (infeas) " + LpSolve.INFEASIBLE + " :: " + implied);
		if (DEBUG_CONSTRAINTS)
			System.out.println("Solution: " + LP.PrintVector(lp._x));
		lp.free();

		// Since dec inverted, infeasibility implies that the implication is true
		// System.out.println("- RESULT: " + implied);
		return implied;
	}

	private void addConstraint(LP lp, int constraint_id, boolean dec) {

		if (DEBUG_CONSTRAINTS)
			System.out.println("Adding constraint id [" + constraint_id + "] = " + dec);

		Decision d = _alOrder.get(constraint_id);
		if (d instanceof ExprDec) {
			ExprDec e = (ExprDec) d;
			if (!(e._expr._rhs instanceof DoubleExpr) || ((DoubleExpr) e._expr._rhs)._dConstVal != 0d) {
				System.out.println("WARNING: Unexpected RHS constraint value: " + e._expr._rhs);
				//take these out so it does not terminal upon bilinear decisions
				//new Exception().printStackTrace(System.out);
				//System.exit(1);
			}
			// From here we just need convert LHS to coefficients and construct
			// correct constraint from CompExpr type
			double[] coefs = new double[_cvar2ID.size()];

			try {
				double const_coef = setCoefficients(e._expr._lhs, coefs); // move to RHS => -

				int type = dec ? e._expr._type : invertType(e._expr._type);

				if (DEBUG_CONSTRAINTS)
					System.out.println("- adding cons: " + const_coef + " + " + LP.PrintVector(coefs) + " <=> "
							+ (dec ? "" : "!") + e._expr);

				switch (type) {
				case GT:
					lp.addGTConstraint(coefs, -const_coef);
					break;
				case GT_EQ:
					lp.addGeqConstraint(coefs, -const_coef);
					break;
				case LT:
					lp.addLTConstraint(coefs, -const_coef);
					break;
				case LT_EQ:
					lp.addLeqConstraint(coefs, -const_coef);
					break;
				case EQ:
					lp.addEqConstraint(coefs, -const_coef);
					break;
				case NEQ:
					break; // Can't add an NEQ constraint
				default:
					break; // Unknown constraint type
				}
			} catch (Exception e2) {
				System.err.println("Could not add constraint: " + e2 + "\n... skipping");
			}
		}
	}

	private int invertType(int type) {
		switch (type) {
		case GT:
			return LT_EQ;
		case GT_EQ:
			return LT;
		case LT:
			return GT_EQ;
		case LT_EQ:
			return GT;
		case EQ:
			return NEQ;
		case NEQ:
			return EQ;
		default:
			return -1; // Unknown constraint type
		}
	}

	// Convert an expression to an array of coefficients and a constant
	private double setCoefficients(ArithExpr e, double[] coefs) throws Exception {

		int error = 0;
		int index=0;
		double accum = 0d;
		if (e instanceof OperExpr) {
			OperExpr o = ((OperExpr) e);
			if (o._type == PROD) {
				if (o._terms.size() != 2)
					error = 1;
				else {
					index = _cvar2ID.get(((VarExpr) o._terms.get(1))._sVarName);
					coefs[index] = ((DoubleExpr) o._terms.get(0))._dConstVal;
				}
			} else if (o._type == SUM) {
				for (ArithExpr e2 : o._terms)
					accum += setCoefficients(e2, coefs);
			} else
				error = 2;
		} else if (e instanceof DoubleExpr) {
			accum += ((DoubleExpr) e)._dConstVal;
		} else
			error = 3;

		if (error > 0) {
			System.out.println("[" + error + "] Unexpected LHS constraint term: " + e);
			throw new Exception("[" + error + "] Unexpected LHS constraint term: " + e); //.printStackTrace(System.out);
			//System.exit(1);
		}

		return accum;
	}

	public int makeCanonical(int node_id) {
		// Graph g = getGraph(node_id);
		// g.addNode("_temp_");
		// g.addNodeLabel("_temp_", "Before make canonical");
		// g.addNodeShape("_temp_", "square");
		// g.addNodeStyle("_temp_", "filled");
		// g.addNodeColor("_temp_", "lightblue");
		// g.launchViewer(1300, 770);
		return reduceSub(node_id, new HashMap<String, ArithExpr>(), new HashMap<Integer, Integer>());
	}

	// Not worrying about reusing cache across class
	// TODO: substitution has to enforce order if it is violated
	public int substitute(int node_id, HashMap<String, ArithExpr> subst) {
		return reduceSub(node_id, subst, new HashMap<Integer, Integer>());
	}

	// TODO: substitution has to enforce order if it is violated
	// could possibly do with a pairwise reordering...
	public int reduceSub(int node_id, HashMap<String, ArithExpr> subst, HashMap<Integer, Integer> subst_cache) {

		Integer ret = null;
		XADDNode n = _hmInt2Node.get(node_id);
		if (n == null) {
			System.out.println("ERROR: " + node_id + " expected in node cache, but not found!");
			new Exception().printStackTrace();
			System.exit(1);
		}

		// A terminal node should be reduced (and cannot be restricted)
		// by default if hashing and equality testing are working in getTNode
		if (n instanceof XADDTNode) {
			ArithExpr expr = ((XADDTNode)n)._expr.substitute(subst);
			Object annotation = ((XADDTNode)n)._annotate;
			if (annotation != null && annotation instanceof ArithExpr) {
				annotation = ((ArithExpr)annotation).substitute(subst);
			}
			return getTermNode(expr, annotation);
		}

		// If its an internal node, check the reduce cache
		if ((ret = subst_cache.get(node_id)) != null) {
			// System.out.println("In cache, returning: " + qret);
			return ret;
		}

		// Handle an internal node
		XADDINode inode = (XADDINode) n;

		int low = reduceSub(inode._low, subst, subst_cache);
		int high = reduceSub(inode._high, subst, subst_cache);

		int var = inode._var;
		Decision d = _alOrder.get(var);
		if (d instanceof ExprDec) {
			// Only need to substitute if a var expression
			CompExpr comp = ((ExprDec) d)._expr;
			comp = comp.substitute(subst);
			d = new ExprDec(comp);
			var = getVarIndex(d, true);
		} else if (d instanceof BoolDec) {
			// System.out.println(((BoolDec)d)._sVarName + ": " + subst);
			VarExpr sub = (VarExpr) subst.get(((BoolDec) d)._sVarName);
			if (sub != null) {
				// There is a substitution for this BoolDec... get new var index
				var = getVarIndex(new BoolDec(sub._sVarName), false);
			}
		}

		// Getting an Inode directly can be unsafe due to the fact that a
		// substitution could have affected a variable reordering...
		// uglier and more computationally costly, but the better solution
		// is via apply.
		// ret = getINode(var, low, high);
		int T_ZERO = getTermNode(ZERO);
		int T_ONE  = getTermNode(ONE);
		int ind_true  = getINode(var, /* low */T_ZERO, /* high */T_ONE);
		int ind_false = getINode(var, /* low */T_ONE, /* high */T_ZERO);
		int true_half = applyInt(ind_true, high, PROD); // Note: this enforces canonicity so
		int false_half = applyInt(ind_false, low, PROD); // can use applyInt rather than apply

		ret = applyInt(true_half, false_half, SUM);

		if (CHECK_LOCAL_ORDERING) {
			// Check ordering
			XADDNode new_node = _hmInt2Node.get(ret);
			if (new_node instanceof XADDINode) {
				XADDINode new_inode = (XADDINode) new_node;
				int var_id = new_inode._var;
				XADDNode low_n = _hmInt2Node.get(new_inode._low);
				if (low_n instanceof XADDINode) {
					XADDINode low_ni = (XADDINode) low_n;
					if (var_id > low_ni._var) {
						System.out.println("Reordering problem: " + var_id + " > " + low_ni._var);
						System.out.println(var_id + ": " + _alOrder.get(var_id));
						System.out.println(low_ni._var + ": " + _alOrder.get(low_ni._var));
						new Exception().printStackTrace(System.out);
						// System.exit(1);
					}
				}
				XADDNode high_n = _hmInt2Node.get(new_inode._high);
				if (high_n instanceof XADDINode) {
					XADDINode high_ni = (XADDINode) high_n;
					if (var_id > high_ni._var) {
						System.out.println("Reordering problem: " + var_id + " > " + high_ni._var);
						System.out.println(var_id + ": " + _alOrder.get(var_id));
						System.out.println(high_ni._var + ": " + _alOrder.get(high_ni._var));
						new Exception().printStackTrace(System.out);
						// System.exit(1);
					}
				}
			}
		}

		// Put return value in cache and return
		subst_cache.put(node_id, ret);
		return ret;
	}

	// Convert an ArithExpr to an XADD while performing (optional) substitution of
	// an XADD for a var (set var = null if no substitution is needed)
	public int substituteXADDforVarInArithExpr(ArithExpr leaf_val, String var, int xadd) {
		if (leaf_val instanceof OperExpr) {
			OperExpr oper_expr = (OperExpr) leaf_val;
			int running_xadd = substituteXADDforVarInArithExpr(oper_expr._terms.get(0), var, xadd);
			for (int i = 1; i < oper_expr._terms.size(); i++) {
				int next_operand = substituteXADDforVarInArithExpr(oper_expr._terms.get(i), var, xadd);
				running_xadd = apply(running_xadd, next_operand, oper_expr._type /* SUM, PROD, ... */);
			}
			return running_xadd;
		} else if (leaf_val instanceof VarExpr) {
			VarExpr var_expr = (VarExpr) leaf_val;
			if (var_expr._sVarName == var) // assume interned
				return xadd;
			else
				return getTermNode(leaf_val);
		} else if (leaf_val instanceof DoubleExpr) {
			return getTermNode(leaf_val);
		} else {
			System.out.println("substituteXADDforVar: Unsupported expression '" + leaf_val + "'");
			System.exit(1);
			return -1;
		}
	}

	public Double evaluate(int node_id, HashMap<String, Boolean> bool_assign, HashMap<String, Double> cont_assign) {

		// Get root
		XADDNode n = _hmInt2Node.get(node_id);

		// Traverse decision diagram until terminal found
		while (n instanceof XADDINode) {
			XADDINode inode = (XADDINode) n;
			Decision d = _alOrder.get(inode._var);
			Boolean branch_high = null;
			if (d instanceof TautDec)
				branch_high = ((TautDec) d)._bTautology;
			else if (d instanceof BoolDec)
				branch_high = bool_assign.get(((BoolDec) d)._sVarName);
			else if (d instanceof ExprDec) {
				branch_high = ((ExprDec) d)._expr.evaluate(cont_assign);
				if (SHOW_DECISION_EVAL) {
					System.out.println(" - " + ((ExprDec) d)._expr + ": " + branch_high);
				}
			}

			// Not all required variables were assigned
			if (branch_high == null)
				return null;

			// Advance down to next node
			n = _hmInt2Node.get(branch_high ? inode._high : inode._low);
		}

		// Now at a terminal node so evaluate expression
		XADDTNode t = (XADDTNode) n;
		return t._expr.evaluate(cont_assign);
	}

	// TODO: could an op other than max/min lead to a non-canonical diagram???
	// only ops that can change inodes can affect ordering and the only
	// ops that can do this should be max/min.
	public int scalarOp(int dd, double val, int op) {
		int dd_val = getTermNode(new DoubleExpr(val));
		return apply(dd, dd_val, op); // could make non-canonical so have to use apply
	}

	public IntTriple _tempApplyKey = new IntTriple(-1, -1, -1);

	// TODO: could an op other than max/min lead to a non-canonical diagram???
	// only ops that can change inodes can affect ordering and the only
	// ops that can do this should be max/min.
	public int apply(int a1, int a2, int op) {
		int ret = applyInt(a1, a2, op);
		if ((op == MAX) || (op == MIN))
			return makeCanonical(ret);
		else
			return ret;
	}
	
	public int applyInt(int a1, int a2, int op) {

		_tempApplyKey.set(a1, a2, op);
		Integer ret = _hmApplyCache.get(_tempApplyKey);
		if (ret != null) {
			return ret;
		}

		// Can we create a terminal node here?
		XADDNode n1 = _hmInt2Node.get(a1);
		XADDNode n2 = _hmInt2Node.get(a2);
		ret = computeTermNode(a1, n1, a2, n2, op);
		if (ret == null) {

			int v1low, v1high, v2low, v2high, var;

			// Find node with min id (or only internal node)
			if (n1 instanceof XADDINode) {
				if (n2 instanceof XADDINode) {
					if (((XADDINode) n1)._var < ((XADDINode) n2)._var) {
						var = ((XADDINode) n1)._var;
					} else {
						var = ((XADDINode) n2)._var;
					}
				} else {
					var = ((XADDINode) n1)._var;
				}
			} else {
				var = ((XADDINode) n2)._var;
			}

			// Determine next recursion for n1
			if ((n1 instanceof XADDINode) && (((XADDINode) n1)._var == var)) {
				XADDINode n1i = (XADDINode) n1;
				v1low = n1i._low;
				v1high = n1i._high;
			} else {
				v1low = a1;
				v1high = a1;
			}

			// Determine next recursion for n2
			if ((n2 instanceof XADDINode) && (((XADDINode) n2)._var == var)) {
				XADDINode n2i = (XADDINode) n2;
				v2low = n2i._low;
				v2high = n2i._high;
			} else {
				v2low = a2;
				v2high = a2;
			}

			// Perform in-line reduction and set min/max for subnodes if needed
			int low = applyInt(v1low, v2low, op);
			int high = applyInt(v1high, v2high, op);

			// getINode will take care of 'low==high'
			ret = getINode(var, low, high);
		}

		_hmApplyCache.put(new IntTriple(a1, a2, op), ret);
		return ret;
	}

	// Computes a terminal node value if possible, assume
	// terms of same type (otherwise incompatible!)
	public Integer computeTermNode(int a1, XADDNode n1, int a2, XADDNode n2, int op) {

		//changed_var_index = -1;
		if ((n1 instanceof XADDTNode) && (n2 instanceof XADDTNode)) {
			XADDTNode xa1 = (XADDTNode) n1;
			XADDTNode xa2 = (XADDTNode) n2;

			if ((op != MAX) && (op != MIN))
				return getTermNode(new OperExpr(op, xa1._expr, xa2._expr));

			CompExpr comp = null;
			if (USE_MINUS_COMP) {
				ArithExpr diff = new OperExpr(MINUS, xa1._expr, xa2._expr);
				comp = new CompExpr(LT, diff, new DoubleExpr(0d));
			} else
				comp = new CompExpr(LT_EQ, xa1._expr, xa2._expr);

			// TODO: Remove
			//System.out.println("**ON MAX: " + xa1._expr + " <= " + xa2._expr);
			//System.out.println("**ON MAX, After simplification: " + comp.makeCanonical());

			Decision d = new ExprDec(comp);

			// Do a range check here
			// if (CHECK_MIN_MAX) {
			//   TautDec tdec = checkRangeImpliedDecision(d);
			//   if (tdec != null)
			//     d = tdec; // Change to a tautology... getInode will handle this
			// }

			int var_index = getVarIndex(d, true);
			int node1 = getTermNode(xa1); // getTermNode(XADDTNode)
			int node2 = getTermNode(xa2); // getTermNode(XADDTNode)
			
			return getINode(var_index, op == MAX ? node1 : node2, op == MAX ? node2 : node1);
		}

		if (n1 instanceof XADDTNode) {
			// If we get here a2 must be XADDINode
			XADDTNode xa1 = (XADDTNode) n1;

			// Check for identity operation value for n1
			if ((op == SUM && xa1._expr.equals(ZERO)) || (op == PROD && xa1._expr.equals(ONE))) {
				return a2;
			}
		}

		if (n2 instanceof XADDTNode) {
			// If we get here a1 must be XADDINode
			XADDTNode xa2 = (XADDTNode) n2;

			// Check for identity operation value for n2
			if ((op == SUM && xa2._expr.equals(ZERO)) || (op == PROD && xa2._expr.equals(ONE))
					|| (op == MINUS && xa2._expr.equals(ZERO)) || (op == DIV && xa2._expr.equals(ONE))) {
				return a1;
			}
		}

		return null;
	}

	// Quick cache snapshot
	 public void showCacheSize() {
		 System.out.println("APPLY CACHE:    " + _hmApplyCache.size());
		 System.out.println("REDUCE CACHE:   " + _hmReduceCache.size());
		 System.out.println("REDUCE CACHE L: " + _hmReduceLeafOpCache.size());
		 System.out.println("REDUCE CACHE A: " + _hmReduceAnnotateCache.size());
		 System.out.println("INODE CACHE 1:  " + _hmNode2Int.size() + "\n");		 
		 System.out.println("INODE CACHE 2:  " + _hmInt2Node.size() + "\n");
		 System.out.println("VAR CACHE 2:    " + _hmINode2Vars.size() + "\n");
	 }

	public void clearMarks() {
		_markCounter++;
	}

	/////////////////////////////////////////////////////////////////////////////////////
	//                               XADD Leaf Operations
	//
	// - maximization of a variable (max_v F(v))  
	// - integration (int_v F(v)... indefinite and definite) 
	// - differentiation (partial_v / partial_F F(v))
	// - XADD substitution for a variable (int_v delta(v - G)*F = F{v/G})
	//   ... for now, v can only occur a leaves of F, but can generalize to any F
	//
	/////////////////////////////////////////////////////////////////////////////////////
	
	// This internal class is intended to be used to specify inline operations to perform at the
	// leaves of an XADD... the operation returns a new XADD for the leaf.
	//
	// 'decisions' and 'decision_values' are parallel arrays indicating the path taken to 'leaf_val'
	// 'xadd' is the XADD being subsituted for 'var'
	public abstract class XADDLeafOperation {
		public abstract int processXADDLeaf(ArrayList<Decision> decisions, ArrayList<Boolean> decision_values,
				ArithExpr leaf_val);
		public abstract boolean isCacheable();
	}

	////////////////////////////////////////////////////////////////////////////////
	// XADDLeafMax added by Zahra 
	// Class of XADDLeafMax that preforms max over a variable on the leaves subject
	// to the constraints encountered to reach those leaves.  The leaf max is over
	// the LB,UB or any root of the polynomial.
	////////////////////////////////////////////////////////////////////////////////

	public class XADDLeafMax extends XADDLeafOperation {

		public int _runningMax; // XADD for the running max of all leaf substitutions
		double _lowerBound, _upperBound;
		String _maxVar;
		ArrayList<String> _contVars;
		
		public XADDLeafMax(String max_var, double lower_bound, double upper_bound) {
			_maxVar = max_var.intern(); 
			_lowerBound = lower_bound;
			_upperBound = upper_bound;
			_runningMax = -1;
		}

		// TODO: revisit whether caching is possible, or in what circumstances
		public boolean isCacheable() {
			return false;
		}

        public int processXADDLeaf(ArrayList<Decision> decisions, ArrayList<Boolean> decision_values, ArithExpr leaf_val) 
        {
            System.out.println("=============================");
            System.out.println("Current node: " + leaf_val);
            System.out.println("Decisions to get to get here: " + decisions + " = " + decision_values + "\n===\n");

            // Bound management
            ArrayList<ArithExpr> lower_bound = new ArrayList<ArithExpr>();
            ArrayList<ArithExpr> upper_bound = new ArrayList<ArithExpr>();
        	lower_bound.add(new DoubleExpr(_lowerBound));
			upper_bound.add(new DoubleExpr(_upperBound));

        	// Multiply these in later
            HashMap<Decision, Boolean> max_var_indep_decisions = new HashMap<Decision, Boolean>();

            // First compute the upper and lower bounds and var-independent constraints
			// from the decisions
			for (int i = 0; i < decisions.size(); i++) {
                Decision d = decisions.get(i);
                Boolean is_true = decision_values.get(i);
                CompExpr comp = null;
                if (d instanceof BoolDec) {
                        max_var_indep_decisions.put(d, is_true);
                        continue;
                } else if (d instanceof ExprDec) {
                        ExprDec ed = (ExprDec) d;
                        comp = ed._expr;
                } else {
                        System.out.println("processXADDLeaf: Unsupported decision type '" + d + "'");
                        System.exit(1);
                }

                // Check that comparison expression is normalized
                if (!comp._rhs.equals(ZERO)) {
                        System.out.println("processXADDLeaf: Expected RHS = 0 for '" + comp + "'");
                        System.exit(1);
                }

                // Takes ArithExpr expr1 linear in var, returns (coef,expr2) where expr1 = coef*x + expr2
				CoefExprPair p = comp._lhs.removeVarFromExpr(_maxVar); 
				ArithExpr lhs_isolated = p._expr;
				double    var_coef     = p._coef;
				System.out.println("Pre: " + comp + " == " + is_true + ", int var [" + _maxVar + "]"
						+ "\nLHS isolated: " + lhs_isolated + "\n ==>  " + var_coef + " * " + _maxVar);

				if (var_coef == 0d) {
					max_var_indep_decisions.put(d, is_true);
					continue;
				}

				// We have var_coef*x + lhs_isolated {<,<=,>,>=} 0
				// ... need to get x {<,<=,>,>=} 1/var_coef * lhs_isolated
				//     (and inequality gets flipped if var_coef is negative)
				boolean flip_comparison = (var_coef < 0d) && (comp._type != EQ) && (comp._type != NEQ);
				ArithExpr new_rhs = (ArithExpr) new OperExpr(MINUS, ZERO, new OperExpr(PROD, new DoubleExpr(
						1d / var_coef), lhs_isolated)).makeCanonical();

				// Divide through by coef (pos or neg)
				// - if coef neg, flip expression
				// - if decision neg, flip expression
				// - if both, don't flip
				int comp_oper = comp._type;
				if ((/* negated */!is_true && !flip_comparison) || (/* not negated */is_true && flip_comparison)) {
					comp_oper = CompExpr.flipCompOper(comp_oper);
				}

				// Now we have (x {<,<=,>,>=} expr)... mark each as lower/upper bound:
				// - lower bounds: x > f(y), x >= f(y)
				// - upper bounds: x < f(z), x <= f(z)
				if (comp_oper == GT || comp_oper == GT_EQ)
					lower_bound.add(new_rhs);
				else if (comp_oper == LT || comp_oper == LT_EQ)
					upper_bound.add(new_rhs);
				else {
					System.out.println("Cannot currently handle: "
							+ new CompExpr(comp_oper, new VarExpr(_maxVar), new_rhs));
					System.out.println("Note: = triggers substitution, not sure how to handle ~=");
					new Exception().printStackTrace();
					System.exit(1);
				}
			}

			// Now explicitly compute lower and upper bounds as XADDs
			//
			// If these are polynomials, must go to +/- infinity at limits so cannot
			// be used to approximate cdfs. Hence we must assume that there will always
			// be limits on the polynomial functions implicit in the bounds.
			System.out.println("Lower bounds: " + lower_bound);
			int xadd_lower_bound = -1;
			for (ArithExpr e : lower_bound) // Lower bound is max of all lower bounds
				xadd_lower_bound = (xadd_lower_bound == -1) ? getTermNode(e)
						: apply(xadd_lower_bound, getTermNode(e), MAX);

			System.out.println("Upper bounds: " + upper_bound);
			int xadd_upper_bound = -1;
			for (ArithExpr e : upper_bound) // Upper bound is min of all upper bounds
				xadd_upper_bound = (xadd_upper_bound == -1) ? getTermNode(e)
						: apply(xadd_upper_bound, getTermNode(e), MIN);

			// Build all constraints for the maximization
			for (ArithExpr e1 : upper_bound) {
				for (ArithExpr e2 : lower_bound) {
					CompExpr ce = new CompExpr(GT, e1, e2);
					ExprDec ed = new ExprDec(ce);
					max_var_indep_decisions.put(ed, Boolean.TRUE);
				}
			}

			// Display lower and upper bounds
			System.out.println("Lower bound:\n" + getString(xadd_lower_bound));
			System.out.println("Upper bound:\n" + getString(xadd_upper_bound));

			// Determine whether we need to handle the quadratic case, if so,
			// root will be set to a non-null evaluation
			ArithExpr root = null;
			int highest_order = leaf_val.determineHighestOrderOfVar(_maxVar);
			if (highest_order > 2) {
				System.out.println("XADDLeafMax: Cannot currently handle expressions higher than order 2 in " + _maxVar + ": " + leaf_val);
				System.exit(1);					
			} else if (highest_order == 2) {
				ArithExpr first_derivative = leaf_val.differentiateExpr(_maxVar);

                // Takes ArithExpr expr1 linear in var, returns (coef,expr2) where expr1 = coef*x + expr2
				// setting expr1 = coef*x + expr2 = 0 then x = -expr2/coef
				CoefExprPair p2 = first_derivative.removeVarFromExpr(_maxVar);
				
				root = (ArithExpr)(new OperExpr(MINUS, ZERO, new OperExpr(PROD, new DoubleExpr(
									           1d / p2._coef), p2._expr)).makeCanonical());
			}
            
			// Substitute lower and upper bounds into leaf
            int max_eval_lower = substituteXADDforVarInArithExpr(leaf_val, _maxVar, xadd_lower_bound);
            int max_eval_upper = substituteXADDforVarInArithExpr(leaf_val, _maxVar, xadd_upper_bound);

            // Display lower and upper bound substitution
            System.out.println("Substitute in: " + leaf_val);
            System.out.println("Lower bound sub:\n" + getString(max_eval_lower));
            System.out.println("Upper bound sub:\n" + getString(max_eval_upper));
            
            // We don't know which of UB/LB substitution is maximal so we take the "case"-max
            // ... if there were nonlinearities in leaf, then substitution leads to nonlinear
            //     function which ends up in decisions... need linear decisions so
            //     call reduceLinearize to complete-the-square for quadratic variables
            //     and linearize the decisions (linearize includes makeCanonical)
            // ??? need to avoid case where max leads to an illegal pruning -- occurs???
            //     e.g., could an unreachable constant prune out another reachable node?
            //     (don't think this can happen... still in context of unreachable constraints)
            int max_eval = apply(max_eval_upper, max_eval_lower, MAX);
            max_eval = reduceLinearize(max_eval); 
            max_eval = reduceLP(max_eval, _alContinuousVars); // Result should be canonical
            System.out.println("max of LB and UB (reduce/linearize): " + getString(max_eval));

            // NOTE: Constraints on root have to be multiplied in here, not at end.  -Scott
            if (root != null) {
            	
                int max_eval_root = substituteXADDforVarInArithExpr(leaf_val, _maxVar, getTermNode(root));
                System.out.println("root substitute: " + getString(max_eval_root));
                
                // Now multiply in constraints into int_eval, make result canonical
                for (ArithExpr ub : upper_bound) {
                    CompExpr ce = new CompExpr(LT_EQ, root, ub);
                    int ub_xadd = getVarNode(new ExprDec(ce), 0d, 1d);
                    max_eval_root = apply(max_eval_root, ub_xadd, PROD);
                }
                for (ArithExpr lb : lower_bound) {
                    CompExpr ce = new CompExpr(GT_EQ, root, lb);
                    int lb_xadd = getVarNode(new ExprDec(ce), 0d, 1d);
                    max_eval_root = apply(max_eval_root, lb_xadd, PROD);
                }
                max_eval_root = reduceLP(max_eval_root, _alContinuousVars); // Result should be canonical
                
                System.out.println("constrained root substitute: " + getString(max_eval_root));
                max_eval = apply(max_eval, max_eval_root, MAX);
                System.out.println("max of constrained root sub and int_eval(LB/UB): " + getString(max_eval));
            }

            System.out.println("max_eval before decisions (after sub root): " + getString(max_eval));

            // Finally, multiply in boolean decisions and irrelevant comparison expressions
            // to the XADD and add it to the running sum
            for (Map.Entry<Decision, Boolean> me : max_var_indep_decisions.entrySet()) {
                    double high_val = me.getValue() ? 1d : 0d;
                    double low_val = me.getValue() ? 0d : 1d;
                    System.out.println("max_eval with decisions: " + me.getKey());
                    max_eval = apply(max_eval, getVarNode(me.getKey(), low_val, high_val), PROD);
            }
            System.out.println("max_eval with decisions: " + getString(max_eval));
            
            System.out.println("Before linearize: " + getString(max_eval));
            max_eval = reduceLinearize(max_eval); 
            max_eval = reduceLP(max_eval, _alContinuousVars); // Result should be canonical
            System.out.println("After linearize: " + getString(max_eval));
                            
            if (_runningMax == -1) 
            	_runningMax = max_eval;
            else 
            	_runningMax = apply(_runningMax, max_eval, MAX);

            _runningMax = reduceLinearize(_runningMax);
            _runningMax = reduceLP(_runningMax, _alContinuousVars);
            if (_runningMax != makeCanonical(_runningMax)) {
            	System.err.println("processXADDMax ERROR: encountered non-canonical node that should have been canonical");
            	System.exit(1);
            }
            System.out.println("running max result: " + getString(_runningMax));

			// All return information is stored in _runningMax so no need to return
			// any information here... just keep diagram as is
            return getTermNode(leaf_val);
        }
	}

	// Given argument var v, the XADD on which this is called contains substitutions
	// for v at its leaves that are to be substituted into argument XADD xadd and returned
	// at the leaves (because these branches are the conditions under which the substitutions
	// are made).
	//
	// Need to ensure make_canonical is true when call reduceProcessXADD
	//
	// (int_v delta(v - G)*F = F{v/G})
	// ... here v is subvar, F is xadd_sub_at_leaves, G is passed to reduceProcessXADD
	//     (G is passed because substitution works by recursing to its leaves to
	//      to determine conditions under which each substitution should be made in F
	public class DeltaFunctionSubstitution extends XADDLeafOperation {
		String _subVar;
		int _xaddSubAtLeaves = -1;
		HashMap<String, ArithExpr> _leafSubs = new HashMap<String, ArithExpr>();

		public DeltaFunctionSubstitution(String subvar, int xadd_sub_at_leaves) {
			_subVar = subvar;
			_xaddSubAtLeaves = xadd_sub_at_leaves;
		}

		// TODO: revisit whether caching is possible, or in what circumstances
		public boolean isCacheable() {
			return false;
		}

		public int processXADDLeaf(ArrayList<Decision> decisions, ArrayList<Boolean> decision_values, ArithExpr leaf_val) {

			// Return an XADD for the resulting expression
			_leafSubs.clear();
			_leafSubs.put(_subVar, leaf_val);
			return substitute(_xaddSubAtLeaves, _leafSubs);
		}

		public int hashCode() {
			return _subVar.hashCode() + _xaddSubAtLeaves;
		}
	}

	public class XADDLeafDerivative extends XADDLeafOperation {
		String _derivativeVar;

		public XADDLeafDerivative(String deriv_var) {
			_derivativeVar = deriv_var.intern();
		}

		// TODO: verify this is cacheable... should be, just returns modified subdiagrams
		public boolean isCacheable() {
			return true;
		}

		public int processXADDLeaf(ArrayList<Decision> decisions, ArrayList<Boolean> decision_values, ArithExpr leaf_val) {

			// Return an XADD for the resulting expression
			ArithExpr ret_expr = leaf_val.differentiateExpr(_derivativeVar);
			return getTermNode(ret_expr);
		}
	}

	public class XADDLeafIndefIntegral extends XADDLeafOperation {
		String _integrationVar;

		public XADDLeafIndefIntegral(String integration_var) {
			_integrationVar = integration_var.intern();
		}

		// TODO: revisit whether caching is possible... should be, just returning integrated subdiagrams
		public boolean isCacheable() {
			return true;
		}

		public int processXADDLeaf(ArrayList<Decision> decisions, ArrayList<Boolean> decision_values, ArithExpr leaf_val) {

			// Return an XADD for the resulting expression
			ArithExpr ret_expr = leaf_val.integrateExpr(_integrationVar);
			return getTermNode(ret_expr);
		}
	}


	public int computeDefiniteIntegral(int xadd, String int_var) {
		XADDLeafDefIntegral integrator = new XADDLeafDefIntegral(int_var);
		reduceProcessXADDLeaf(xadd, integrator, /* canonical_reorder */false);
		return integrator._runningSum;
	}

	public class XADDLeafDefIntegral extends XADDLeafIndefIntegral {
		int _runningSum; // XADD for the running sum of all leaf substitutions

		public final static boolean DEBUG_XADD_DEF_INTEGRAL = false;

		public XADDLeafDefIntegral(String integration_var) {
			super(integration_var);

			// Start with the zero XADD
			_runningSum = getTermNode(ZERO);
		}

		// TODO: revisit whether caching is possible, or in what circumstances (i.e., constraints
		//       irrelevant to the integration variable)
		public boolean isCacheable() {
			return false;
		}

		public int processXADDLeaf(ArrayList<Decision> decisions, ArrayList<Boolean> decision_values, ArithExpr leaf_val) {

			// Multiply these in later
			HashMap<Decision, Boolean> int_var_indep_decisions = new HashMap<Decision, Boolean>();

			//upper and lower bounds based on the decisions
			if (DEBUG_XADD_DEF_INTEGRAL) {
				System.out.println("=============================");
				System.out.println("Current node: " + leaf_val);
				System.out.println("Decisions to get to get here: " + decisions + " = " + decision_values + "\n===\n");
			}
			ArrayList<ArithExpr> lower_bound = new ArrayList<ArithExpr>();
			ArrayList<ArithExpr> upper_bound = new ArrayList<ArithExpr>();
			for (int i = 0; i < decisions.size(); i++) {
				Decision d = decisions.get(i);
				Boolean is_true = decision_values.get(i);
				CompExpr comp = null;
				if (d instanceof BoolDec) {
					int_var_indep_decisions.put(d, is_true);
					continue;
				} else if (d instanceof ExprDec) {
					ExprDec ed = (ExprDec) d;
					comp = ed._expr;
				} else {
					System.out.println("processXADDLeaf: Unsupported decision type '" + d + "'");
					System.exit(1);
				}

				// Check that comparison expression is normalized
				if (!comp._rhs.equals(ZERO)) {
					System.out.println("processXADDLeaf: Expected RHS = 0 for '" + comp + "'");
					System.exit(1);
				}

				// Takes ArithExpr expr1 linear in var, returns (coef,expr2) where expr1 = coef*x + expr2
				CoefExprPair p = comp._lhs.removeVarFromExpr(_integrationVar); 
				ArithExpr lhs_isolated = p._expr;
				double    var_coef     = p._coef;
				if (DEBUG_XADD_DEF_INTEGRAL) {
					System.out.println("Pre: " + comp + " == " + is_true + ", int var [" + _integrationVar + "]"
							+ "\nLHS isolated: " + lhs_isolated + "\n ==>  " + var_coef + " * "
							+ _integrationVar);
				}

				if (var_coef == 0d) {
					int_var_indep_decisions.put(d, is_true);
					continue;
				}

				// We have coef*x + expr COMP_OPER 0
				boolean flip_comparison = (var_coef < 0d) && (comp._type != EQ) && (comp._type != NEQ);
				ArithExpr new_rhs = (ArithExpr) new OperExpr(MINUS, ZERO, new OperExpr(PROD, new DoubleExpr(
						1d / var_coef), lhs_isolated)).makeCanonical();

				// Divide through by coef (pos or neg)
				// - if coef neg, flip expression
				// - if decision neg, flip expression
				// - if both, don't flip
				int comp_oper = comp._type;
				if ((/* negated */!is_true && !flip_comparison) || (/* not negated */is_true && flip_comparison)) {
					comp_oper = CompExpr.flipCompOper(comp_oper);
				}

				// Now we have x COMP_OPER expr
				// System.out.println("After arrange: " + new CompExpr(comp_oper, new VarExpr(_integrationVar),
				// new_rhs));

				// Now mark this as a lower or upper bound
				// - lower bounds: x > f(y), x >= f(y)
				// - upper bounds: x < f(z), x <= f(z)
				if (comp_oper == GT || comp_oper == GT_EQ)
					lower_bound.add(new_rhs);
				else if (comp_oper == LT || comp_oper == LT_EQ)
					upper_bound.add(new_rhs);
				else {
					System.out.println("Cannot currently handle: "
							+ new CompExpr(comp_oper, new VarExpr(_integrationVar), new_rhs));
					System.out.println("Note: = triggers substitution, not sure how to handle ~=");
					new Exception().printStackTrace();
					System.exit(1);
				}
			}

			// Now explicitly compute lower and upper bounds as XADDs
			//
			// If these are polynomials, must go to +/- infinity at limits so cannot
			// be used to approximate cdfs. Hence we must assume that there will always
			// be limits on the polynomial functions implicit in the bounds.
			int xadd_lower_bound = -1;
			if (lower_bound.isEmpty()) {
				if (DEBUG_XADD_DEF_INTEGRAL) {
					System.err.println("No explicit lower bound given for '" + _integrationVar + "', using -1e6");
					System.err.println("Constraints: " + decisions);
					System.err.println("Assignments: " + decision_values);
				}
				xadd_lower_bound = getTermNode(new DoubleExpr(-1e6));
			} else {
				if (DEBUG_XADD_DEF_INTEGRAL) 
					System.out.println("Lower bounds: " + lower_bound);
				for (ArithExpr e : lower_bound) {
					if (xadd_lower_bound == -1) {
						xadd_lower_bound = getTermNode(e);
					} else {
						// Lower bound is max of all lower bounds
						xadd_lower_bound = apply(xadd_lower_bound, getTermNode(e), MAX);
					}
				}
			}

			int xadd_upper_bound = -1;
			if (upper_bound.isEmpty()) {
				if (DEBUG_XADD_DEF_INTEGRAL) {
					System.err.println("No explicit upper bound given for '" + _integrationVar + "', using +1e6");
					System.err.println("Constraints: " + decisions);
					System.err.println("Assignments: " + decision_values);
				}
				xadd_upper_bound = getTermNode(new DoubleExpr(1e6));
			} else {
				if (DEBUG_XADD_DEF_INTEGRAL) 
					System.out.println("Upper bounds: " + upper_bound);
				for (ArithExpr e : upper_bound) {
					if (xadd_upper_bound == -1) {
						xadd_upper_bound = getTermNode(e);
					} else {
						// Upper bound is min of all upper bounds
						xadd_upper_bound = apply(xadd_upper_bound, getTermNode(e), MIN);
					}
				}
			}

			// Build all constraints for the integration
			for (ArithExpr e1 : upper_bound) {
				for (ArithExpr e2 : lower_bound) {
					CompExpr ce = new CompExpr(GT, e1, e2);
					ExprDec ed = new ExprDec(ce);
					int_var_indep_decisions.put(ed, Boolean.TRUE);
				}
			}

			// Display lower and upper bounds
			if (DEBUG_XADD_DEF_INTEGRAL) {
				System.out.println("Lower bound:\n" + getString(xadd_lower_bound));
				System.out.println("Upper bound:\n" + getString(xadd_upper_bound));
			}

			// Compute the integral of this leaf w.r.t. the integration variable
			ArithExpr leaf_integral = leaf_val.integrateExpr(_integrationVar);
			if (DEBUG_XADD_DEF_INTEGRAL)
				System.out.println("Integral: " + leaf_integral);

			// Now compute:
			// leaf_integral{int_var = xadd_upper_bound} - leaf_integral{int_var = xadd_lower_bound}
			int int_eval_lower = substituteXADDforVarInArithExpr(leaf_integral, _integrationVar, xadd_lower_bound);
			int int_eval_upper = substituteXADDforVarInArithExpr(leaf_integral, _integrationVar, xadd_upper_bound);
			int int_eval = apply(int_eval_upper, int_eval_lower, MINUS);
			//System.out.println("Result: " + getString(int_eval));

			// Finally, multiply in boolean decisions and irrelevant comparison expressions
			// to the XADD and add it to the running sum
			//
			// - HashMap<Decision,Boolean> int_var_indep_decisions
			for (Map.Entry<Decision, Boolean> me : int_var_indep_decisions.entrySet()) {
				double high_val = me.getValue() ? 1d : 0d;
				double low_val = me.getValue() ? 0d : 1d;
				int_eval = apply(int_eval, getVarNode(me.getKey(), low_val, high_val), PROD);
			}
			_runningSum = apply(_runningSum, int_eval, SUM);

			// All return information is stored in _runningSum so no need to return
			// any information here... just keep diagram as is
			//System.out.println("Running sum: " + getString(_runningSum));
			return getTermNode(leaf_val);
		}
	}

	// Note: use canonical_reorder=false for integration since internal nodes *cannot* get
	// out of order, but use canonical_reorder=true for substitution
	public int reduceProcessXADDLeaf(int id, XADDLeafOperation leaf_op, boolean canonical_reorder) {
		int ret_node = reduceProcessXADDLeaf(id, leaf_op, new ArrayList<Decision>(), new ArrayList<Boolean>());
		if (canonical_reorder)
			return makeCanonical(ret_node);
		else
			return ret_node;
	}

	public IntPair _tempReduceLeafOpKey = new IntPair(-1, -1);

	private int reduceProcessXADDLeaf(int id, XADDLeafOperation leaf_op, 
			ArrayList<Decision> decisions, ArrayList<Boolean> decision_values) {

		//System.out.println("** reduceProcessXADDLeaf: decisions " + decisions + " -> " + decision_values);

		Integer ret = null;
		XADDNode n = _hmInt2Node.get(id);
		if (n == null) {
			System.out.println("ERROR: " + id + " expected in node cache, but not found!");
			new Exception().printStackTrace();
			System.exit(1);
		}

		// A terminal node should be reduced (and cannot be restricted)
		// by default if hashing and equality testing are working in getTNode
		if (n instanceof XADDTNode) {
			return leaf_op.processXADDLeaf(decisions, decision_values, ((XADDTNode) n)._expr); // Assuming that to have
		}

		// If its an internal node, check the reduce cache		
		if (leaf_op.isCacheable()) {
			_tempReduceLeafOpKey.set(id, leaf_op.hashCode());
			// System.out.println("** In cache, returning: " + getString(ret));
			if ((ret = _hmReduceLeafOpCache.get(_tempReduceLeafOpKey)) != null)
				return ret;
		}

		XADDINode inode = (XADDINode) n;
		Decision d = _alOrder.get(inode._var);

		decisions.add(d);
		decision_values.add(Boolean.FALSE);
		int low = reduceProcessXADDLeaf(inode._low, leaf_op, decisions, decision_values);

		decision_values.set(decision_values.size() - 1, Boolean.TRUE);
		int high = reduceProcessXADDLeaf(inode._high, leaf_op, decisions, decision_values);

		decisions.remove(decisions.size() - 1);
		decision_values.remove(decision_values.size() - 1);

		// Standard Reduce: getInode will handle the case of low == high
		ret = getINode(inode._var, low, high);

		// Put return value in cache and return
		if (leaf_op.isCacheable())
			_hmReduceLeafOpCache.put(new IntPair(id, leaf_op.hashCode()), ret);
		
		return ret;
	}

	///////////////////////////////////////////////////////////////////////////////
	//                           Helper Internal Classes
	///////////////////////////////////////////////////////////////////////////////
	
	public static class CoefExprPair {
		public ArithExpr _expr;
		public double    _coef;
		public CoefExprPair(ArithExpr expr, double coef) {
			_expr = expr;
			_coef = coef;
		}
	}
	
	public abstract class XADDNode {
		public int _mark;

		public void mark() {
			_mark = _markCounter;
		}

		public void unmark() {
			_mark = 0;
		}

		public boolean isMarked() {
			return _mark == _markCounter;
		}

		public String toString() {
			return toString(0);
		}

		public HashSet<XADDNode> collectNodes() {
			HashSet<XADDNode> nodes = new HashSet<XADDNode>();
			collectNodes(nodes);
			return nodes;
		}

		public HashSet<String> collectVars() {
			HashSet<String> vars = new HashSet<String>();
			collectVars(vars);
			return vars;
		}

		public abstract int countBranches();

		public abstract String toString(int depth);

		public abstract String toString(boolean format);

		public abstract void toGraph(Graph g, int id);

		public abstract void collectVars(HashSet<String> vars);

		public abstract void collectNodes(HashSet<XADDNode> nodes);
	}

	public class XADDTNode extends XADDNode {
		public ArithExpr _expr;
		public Object    _annotate;

		public XADDTNode(ArithExpr e) {
			if (e instanceof OperExpr && ((OperExpr) e)._terms.size() == 1)
				_expr = ((OperExpr) e)._terms.get(0);
			else
				_expr = e;
			_annotate = null;
		}
		
		public void set(ArithExpr e, Object annotation) {
			_expr = e;
			_annotate = annotation;
		}

		public XADDTNode(ArithExpr e, Object annotation) {
			if (e instanceof OperExpr && ((OperExpr) e)._terms.size() == 1)
				_expr = ((OperExpr) e)._terms.get(0);
			else
				_expr = e;
			_annotate = annotation;
		}

		public int hashCode() {
			if (_annotate == null) 
				return _expr.hashCode();
			else
				return _expr.hashCode() + _annotate.hashCode();
		}

		public boolean equals(Object o) {
			if (o instanceof XADDTNode) {
				XADDTNode t = (XADDTNode)o;
				if (_annotate == null)
					return t._annotate == null && t._expr.equals(_expr);
				else 
					return t._annotate.equals(_annotate) && t._expr.equals(_expr);
			} else 
				return false;
		}

		public void collectVars(HashSet<String> vars) {
			_expr.collectVars(vars);
		}

		public void collectNodes(HashSet<XADDNode> nodes) {
			nodes.add(this);
		}

		public void toGraph(Graph g, int id) {
			String this_node = Integer.toString(id);
			g.addNode(this_node);
			if (_annotate == null)
				g.addNodeLabel(this_node, _expr.toString());
			else
				g.addNodeLabel(this_node, _expr.toString() + "\n[ " + _annotate + " ]");

			if (GRAPH_USE_COLOR)
				g.addNodeColor(this_node, "lightsalmon"); // red, darkred, lightsalmon
			g.addNodeShape(this_node, "box");
			g.addNodeStyle(this_node, "filled");
		}

		public String toString(int depth) {
			return "( ["/* "#" + _hmNode2Int.get(this) + ": " */+ _expr.toString() + "] )";
		}

		public String toString(boolean format) {
			return "( ["/* "#" + _hmNode2Int.get(this) + ": " */+ _expr.toString(format) + "] )";
		}

		@Override
		public int countBranches() {
			return 1;
		}

	}

	public class XADDINode extends XADDNode {
		public int _var;
		public int _low;
		public int _high;

		public XADDINode(int var, int low, int high) {
			_var = var;
			_low = low;
			_high = high;
		}

		public void set(int var, int low, int high) {
			_var = var;
			_low = low;
			_high = high;
		}

		public int hashCode() {
			return (_var) + (_low << 10) - (_high << 20) + (_high >>> 20) - (_low >>> 10);
		}

		public boolean equals(Object o) {
			if (o instanceof XADDINode) {
				XADDINode n = (XADDINode) o;
				return this._var == n._var && this._low == n._low && this._high == n._high;
			} else
				return false;
		}

		public void collectNodes(HashSet<XADDNode> nodes) {
			if (nodes.contains(this))
				return;

			nodes.add(this);
			_hmInt2Node.get(_low).collectNodes(nodes);
			_hmInt2Node.get(_high).collectNodes(nodes);
		}

		public void collectVars(HashSet<String> vars) {

			// Check cache
			HashSet<String> vars2 = _hmINode2Vars.get(this);
			if (vars2 != null) {
				vars.addAll(vars2);
				return;
			}

			XADDNode low = _hmInt2Node.get(_low);
			XADDNode high = _hmInt2Node.get(_high);
			Decision d = _alOrder.get(_var);
			d.collectVars(vars);
			low.collectVars(vars);
			high.collectVars(vars);

			_hmINode2Vars.put(this, (HashSet<String>) vars.clone());
		}

		public void toGraph(Graph g, int id) {

			// Main node
			String this_node = Integer.toString(id);
			// System.out.println("var: " + _var);
			// System.out.println(_alOrder);
			g.addNode(this_node);
			g.addNodeLabel(this_node, _alOrder.get(_var).toString()/* +" id: "+getVarIndex(_alOrder.get(_var),true) */);
			if (GRAPH_USE_COLOR)
				g.addNodeColor(this_node, "lightblue"); // green, lightblue
			g.addNodeShape(this_node, "ellipse");
			g.addNodeStyle(this_node, "filled");

			// Children
			XADDNode low = _hmInt2Node.get(_low);
			if (low != null) {
				String low_node = Integer.toString(_low);
				g.addUniLink(this_node, low_node, "black", "dashed", Graph.EMPTY_STR);
				low.toGraph(g, _low);
			}

			XADDNode high = _hmInt2Node.get(_high);
			if (high != null) {
				String high_node = Integer.toString(_high);
				g.addUniLink(this_node, high_node, "black", "solid", Graph.EMPTY_STR);
				high.toGraph(g, _high);
			}
		}

		public String toString(int depth) {
			return toString(depth, true);
		}

		public String toString(int depth, boolean format) {
			StringBuffer sb = new StringBuffer();
			sb.append("( "/*
			 * "#" + _hmNode2Int.get(this) + " v" + _var + ": "
			 */+ "[" + _alOrder.get(_var).toString(format) + "]");

			// Node level cache
			XADDNode n2 = _hmInt2Node.get(_high);
			if (n2 != null) {
				sb.append("\n" + indent(depth) + " " + n2.toString(depth + 1) + " ");
			} else {
				sb.append("h:[null] ");
			}
			XADDNode n1 = _hmInt2Node.get(_low);
			if (n1 != null) {
				sb.append("\n" + indent(depth) + " " + n1.toString(depth + 1) + " ");
			} else {
				sb.append("l:[null] ");
			}
			sb.append(") ");

			return sb.toString();
		}

		public int countBranches() {
			int low_count = _hmInt2Node.get(_low).countBranches();
			if (low_count > MAX_BRANCH_COUNT || low_count == -1)
				return -1;

			int high_count = _hmInt2Node.get(_high).countBranches();
			if (high_count > MAX_BRANCH_COUNT || high_count == -1)
				return -1;

			int total = low_count + high_count;
			if (total > MAX_BRANCH_COUNT)
				return -1;

			return total;
		}

		public String toString(boolean format) {
			return toString(0, format);
		}
	}

	// //////////////////////////////////////////////////////////////

	public abstract class Decision {
		public abstract void collectVars(HashSet<String> vars);

		public abstract String toString(boolean format);

		public abstract Decision makeCanonical();
	}

	public class NullDec extends Decision {

		public void collectVars(HashSet<String> vars) { }

		public Decision makeCanonical() {
			return this;
		}

		public String toString(boolean format) {
			return "[NULL DECISION]";
		}

		public int hashCode() {
			return 0;
		}

		public boolean equals(Object o) {
			return (o instanceof NullDec);
		}

	}

	// Represents a tautology or its negation (inconsistency)
	public class TautDec extends Decision {
		public boolean _bTautology;

		public TautDec(boolean isTaut) {
			_bTautology = isTaut;
		}

		public int hashCode() {
			return _bTautology ? 1 : 0;
		}

		public boolean equals(Object o) {
			if (o instanceof TautDec)
				return ((TautDec) o)._bTautology == _bTautology;
			else
				return false;
		}

		public void collectVars(HashSet<String> vars) {
		}

		public String toString() {
			return Boolean.toString(_bTautology);
		}

		public Decision makeCanonical() {
			return this;
		}

		public String toString(boolean format) {
			return Boolean.toString(_bTautology);
		}
	}

	public class BoolDec extends Decision {
		public String _sVarName = null;

		public BoolDec(String var_name) {
			_sVarName = var_name.intern();
			_alBooleanVars.add(_sVarName);
		}

		public int hashCode() {
			return _sVarName.hashCode();
		}

		public boolean equals(Object o) {
			if (o instanceof BoolDec)
				return ((BoolDec) o)._sVarName == _sVarName;
			else
				return false;
		}

		public void collectVars(HashSet<String> vars) {
			vars.add(_sVarName);
		}

		public String toString() {
			return _sVarName;
		}

		public Decision makeCanonical() {
			return this;
		}

		public String toString(boolean format) {
			return toString();
		}
	}

	public class ExprDec extends Decision {
		public CompExpr _expr = null;

		public ExprDec(CompExpr expr) {
			_expr = expr;
		}

		public int hashCode() {
			return _expr.hashCode();
		}

		public boolean equals(Object o) {
			if (o instanceof ExprDec)
				return ((ExprDec) o)._expr.equals(_expr);
			else
				return false;
		}

		public void collectVars(HashSet<String> vars) {
			_expr._lhs.collectVars(vars);
			_expr._rhs.collectVars(vars);
		}

		public String toString() {
			return _expr.toString();
		}

		public String toString(boolean format) {
			return _expr.toString(format);
		}

		public Decision makeCanonical() {

			// Make canonical
			Decision d = this;
			if (d instanceof ExprDec) {
				ExprDec e = (ExprDec) d;
				//have to add the decisions from the divisor node to the decisions
				// if the decision of the divisor is positive, the decision d's sign does not change, else flip the decision type. 
				CompExpr new_comp = (CompExpr) e._expr.makeCanonical();
				d = new ExprDec(new_comp);
				//if there were divisors, add the decisions based on the expressions

			}

			// Check for tautology (or inconsistency)
			if (d instanceof ExprDec && ((ExprDec) d)._expr._lhs instanceof DoubleExpr
					&& ((ExprDec) d)._expr._rhs instanceof DoubleExpr) {
				// Directly evaluate the inequality
				double dval_lhs = ((DoubleExpr) ((ExprDec) d)._expr._lhs)._dConstVal;
				double dval_rhs = ((DoubleExpr) ((ExprDec) d)._expr._rhs)._dConstVal;
				TautDec tdec = null;
				switch (((ExprDec) d)._expr._type) {
				case EQ:
					tdec = new TautDec(dval_lhs == dval_rhs);
					break;
				case NEQ:
					tdec = new TautDec(dval_lhs != dval_rhs);
					break;
				case GT:
					tdec = new TautDec(dval_lhs > dval_rhs);
					break;
				case GT_EQ:
					tdec = new TautDec(dval_lhs >= dval_rhs);
					break;
				case LT:
					tdec = new TautDec(dval_lhs < dval_rhs);
					break;
				case LT_EQ:
					tdec = new TautDec(dval_lhs <= dval_rhs);
					break;
				}
				if (tdec != null)
					d = tdec;
			} else if (d instanceof ExprDec && CHECK_MIN_MAX) {
				// Check for evaluations based on minimum and maximum values
				// ExprDec d1 = (ExprDec)d;
				TautDec tdec = checkRangeImpliedDecision((ExprDec) d);
				if (tdec != null) {
					d = tdec;
				}
			}

			return d;
		}

	}

	public TautDec checkRangeImpliedDecision(ExprDec d) {
		TautDec tdec = null;
		Double lhs_max = d._expr._lhs.evaluateRange(_hmMinVal, _hmMaxVal, false);
		Double lhs_min = d._expr._lhs.evaluateRange(_hmMinVal, _hmMaxVal, true);
		Double rhs_max = d._expr._rhs.evaluateRange(_hmMinVal, _hmMaxVal, false);
		Double rhs_min = d._expr._rhs.evaluateRange(_hmMinVal, _hmMaxVal, true);
		if (lhs_max == null)
			lhs_max = Double.MAX_VALUE;
		if (rhs_max == null)
			rhs_max = Double.MAX_VALUE;
		if (lhs_min == null)
			lhs_min = -Double.MAX_VALUE;
		if (rhs_min == null)
			rhs_min = -Double.MAX_VALUE;
		switch (d._expr._type) {
		case EQ:
		case NEQ:
			if ((lhs_min > rhs_max || rhs_min > lhs_max)
					|| ((lhs_max == lhs_min) && (rhs_max == rhs_min) && (lhs_min != rhs_min))) {
				// Indicates they cannot possibly be equal
				tdec = new TautDec((d._expr._type == EQ) ? false : true);
			}
			break;
		case GT:
			// lhs > rhs
			if (lhs_min > rhs_max)
				tdec = new TautDec(true);
			else if (lhs_max <= rhs_min)
				tdec = new TautDec(false);
			break;
		case GT_EQ:
			// lhs >= rhs
			if (lhs_min >= rhs_max)
				tdec = new TautDec(true);
			else if (lhs_max < rhs_min)
				tdec = new TautDec(false);
			break;
		case LT:
			// lhs < rhs
			if (lhs_max < rhs_min)
				tdec = new TautDec(true);
			else if (lhs_min >= rhs_max)
				tdec = new TautDec(false);
			break;
		case LT_EQ:
			// lhs <= rhs
			if (lhs_max <= rhs_min)
				tdec = new TautDec(true);
			else if (lhs_min > rhs_max)
				tdec = new TautDec(false);
			break;
		}
		if (tdec != null) {
			System.out
			.println("*** " + (tdec == null ? "not" : "") + " pruning " + d + " with " + tdec + " because...");
			System.out.println("- [" + lhs_min + "," + lhs_max + "] " + _aOpNames[d._expr._type] + " [" + rhs_min + ","
					+ rhs_max + "]");
		}

		return tdec;
	}

	// ///////////////////////////////////////////////////////////////////

	public static abstract class Expr implements Comparable<Expr> {

		public static final Class DOUBLE_CLASS = DoubleExpr.class;
		public static final Class VAR_CLASS = VarExpr.class;
		public static final Class ARITH_CLASS = ArithExpr.class;
		public static final Class OPER_CLASS = OperExpr.class;
		public static final Class COMP_CLASS = CompExpr.class;

		public static HashMap<Class, Integer> _class2order = new HashMap<Class, Integer>();
		static {
			_class2order.put(DOUBLE_CLASS, 0);
			_class2order.put(VAR_CLASS, 1);
			_class2order.put(ARITH_CLASS, 2);
			_class2order.put(OPER_CLASS, 3);
			_class2order.put(COMP_CLASS, 4);
		}

		public abstract Expr makeCanonical();

		public int compareTo(Expr o) {
			// Var, Double, Arith, Oper, Comp
			Class this_class = this.getClass();
			Class other_class = o.getClass();

			if (!this_class.equals(other_class)) {
				Integer rank_this = _class2order.get(this_class);
				Integer rank_other = _class2order.get(other_class);
				return rank_this - rank_other;
			} else
				return this.hashCode() - o.hashCode();
		}
	}

	public static class CompExpr extends Expr {

		public int _type = UND;
		public ArithExpr _lhs = null;
		public ArithExpr _rhs = null;

		public CompExpr(int type, ArithExpr lhs, ArithExpr rhs) {
			_type = type;
			_lhs = lhs;
			_rhs = rhs;
		}

		public String toString(boolean format) {
			return _lhs.toString(format) + " " + _aOpNames[_type] + " " + _rhs.toString(format);
		}

		public static int flipCompOper(int comp_oper) {
			switch (comp_oper) {
			case GT:
				return LT_EQ;
			case GT_EQ:
				return LT;
			case LT:
				return GT_EQ;
			case LT_EQ:
				return GT;
			case EQ:
				return NEQ;
			case NEQ:
				return EQ;
			default:
				return -1;
			}
		}

		public Expr makeCanonical() {

			// 1. Expressions all zero on RHS of comparisons and restrict symbols:
			// a > b : a <= b and swap branches
			// a < b : a >= b and swap branches
			// a != b : a == b and swap branches
			CompExpr new_expr = new CompExpr(_type, _lhs, _rhs);
			switch (new_expr._type) {
			case GT:
				new_expr._type = LT_EQ;
				// Swap lhs and rhs
				new_expr._lhs = _rhs;
				new_expr._rhs = _lhs;
				break;
			case LT:
				new_expr._type = GT_EQ;
				// Swap lhs and rhs
				new_expr._lhs = _rhs;
				new_expr._rhs = _lhs;
				break;
			case NEQ:
				new_expr._type = EQ;
				// Swap lhs and rhs
				new_expr._lhs = _rhs;
				new_expr._rhs = _lhs;
				break;
			}
			
			// System.out.println(">> CompExpr: makeCanonical: " + _lhs + " - " + _rhs);
			ArithExpr new_lhs = ArithExpr.op(new_expr._lhs, new_expr._rhs, MINUS);
			new_lhs = (ArithExpr) new_lhs.makeCanonical();
			new_expr = new CompExpr(new_expr._type, new_lhs, ZERO);
			// System.out.println(">> CompExpr: makeCanonical: " + new_expr);
			return new_expr;
		}

		public boolean equals(Object o) {
			if (o instanceof CompExpr) {
				CompExpr c = (CompExpr) o;
				return this._type == c._type && this._lhs.equals(c._lhs) && this._rhs.equals(c._rhs);
			} else
				return false;
		}

		public int hashCode() {
			int i2 = _lhs.hashCode();
			int i3 = _rhs.hashCode();
			return (_type) + (i2 << 10) - (i3 << 20) + (i3 >>> 20) - (i2 >>> 10);
		}

		public static CompExpr parse(String s) {
			try {
				FOPC.Node res = FOPC.parse(s);
				return Convert2CompExpr((FOPC.PNode) res);
			} catch (Exception f) {
				return null;
			}
		}

		public static CompExpr Convert2CompExpr(FOPC.PNode res) {
			int type = UND;
			if (res._nPredID != FOPC.PNode.INVALID) {
				switch (res._nPredID) {
				case FOPC.PNode.EQUALS: {
					type = res._bIsNegated ? NEQ : EQ;
				}
				break;
				case FOPC.PNode.LESS: {
					type = res._bIsNegated ? GT_EQ : LT;
				}
				break;
				case FOPC.PNode.LESSEQ: {
					type = res._bIsNegated ? GT : LT_EQ;
				}
				break;
				}
			}
			ArithExpr lhs = ArithExpr.Convert2ArithExpr((FOPC.Term) res.getBinding(0));
			ArithExpr rhs = ArithExpr.Convert2ArithExpr((FOPC.Term) res.getBinding(1));
			if (lhs == null || rhs == null || type == UND)
				return null;
			else
				return new CompExpr(type, lhs, rhs);
		}

		public String toString() {
			return _lhs + " " + _aOpNames[_type] + " " + _rhs;
		}

		public CompExpr substitute(HashMap<String, ArithExpr> subst) {
			ArithExpr lhs = _lhs.substitute(subst);
			ArithExpr rhs = _rhs.substitute(subst);
			return new CompExpr(_type, lhs, rhs);
		}

		public Boolean evaluate(HashMap<String, Double> cont_assign) {

			Double dval_lhs = _lhs.evaluate(cont_assign);
			Double dval_rhs = _rhs.evaluate(cont_assign);

			if (dval_lhs == null || dval_rhs == null)
				return null;

			switch (_type) {
			case EQ:
				return (dval_lhs == dval_rhs);
			case NEQ:
				return (dval_lhs != dval_rhs);
			case GT:
				return (dval_lhs > dval_rhs);
			case GT_EQ:
				return (dval_lhs >= dval_rhs);
			case LT:
				return (dval_lhs < dval_rhs);
			case LT_EQ:
				return (dval_lhs <= dval_rhs);
			default:
				return null;
			}
		}

		public void collectVars(HashSet<String> vars) {
			_lhs.collectVars(vars);
			_rhs.collectVars(vars);
		}
	}

	public abstract static class ArithExpr extends Expr {

		public static ArithExpr parse(String s) {
			try {
				FOPC.Node res = FOPC.parse(s + " = 0");
				// if (res != null)
				// System.out.println("==> " + res.toFOLString());
				return Convert2ArithExpr(((FOPC.PNode) res).getBinding(0));
			} catch (Exception e) {
				return null;
			}
		}

		public static int parseIntoXADD(XADD context, String s) {
			try {
				FOPC.Node res = FOPC.parse(s + " = 0");
				//if (res != null)
				//	System.out.println("==> " + res.toFOLString());
				return Convert2XADD(context, ((FOPC.PNode) res).getBinding(0));
			} catch (Exception e) {
				System.err.println("Could not convert: " + s + "\n" + e);
				e.printStackTrace(System.err);
				System.exit(1); return -1;
			}
		}

		public abstract String toString(boolean format);

		public static ArithExpr Convert2ArithExpr(FOPC.Term t) {
			// System.out.println("Convert2ArithExpr: " + t.toFOLString());
			if (t instanceof FOPC.TVar) {
				return new VarExpr(((FOPC.TVar) t)._sName);
			} else if (t instanceof FOPC.TScalar) {
				return new DoubleExpr(((FOPC.TScalar) t)._dVal);
			} else if (t instanceof FOPC.TInteger) {
				return new DoubleExpr(((FOPC.TInteger) t)._nVal);
			} else if (t instanceof FOPC.TFunction) {
				return OperExpr.Convert2OperExpr((FOPC.TFunction) t);
			} else
				return null;
		}

		public static int Convert2XADD(XADD context, FOPC.Term t) {
			// System.out.println("Convert2ArithExpr: " + t.toFOLString());
			if (t instanceof FOPC.TVar) {
				return context.getTermNode(new VarExpr(((FOPC.TVar) t)._sName));
			} else if (t instanceof FOPC.TScalar) {
				return context.getTermNode(new DoubleExpr(((FOPC.TScalar) t)._dVal));
			} else if (t instanceof FOPC.TInteger) {
				return context.getTermNode(new DoubleExpr(((FOPC.TInteger) t)._nVal));
			} else if (t instanceof FOPC.TFunction) {
				return OperExpr.Convert2XADD(context, (FOPC.TFunction) t);
			} else
				return -1;
		}

		public static ArithExpr op(ArithExpr f1, ArithExpr f2, int op) {
			if (f1 instanceof DoubleExpr && (op == SUM || op == PROD)) {
				// operands reordered
				return op(f2, ((DoubleExpr) f1)._dConstVal, op);
			} else if (f2 instanceof DoubleExpr) {
				// Can handle MINUS and DIV here
				return op(f1, ((DoubleExpr) f2)._dConstVal, op);
			} else if (f1 instanceof OperExpr && f2 instanceof OperExpr
					&& ((OperExpr) f1)._type == ((OperExpr) f2)._type && ((OperExpr) f1)._type == op
					&& (op == SUM || op == PROD)) {
				// Exploit associativity
				ArrayList<ArithExpr> terms = new ArrayList<ArithExpr>(((OperExpr) f1)._terms);
				terms.addAll(((OperExpr) f2)._terms);
				return new OperExpr(op, terms);
			} else {
				ArrayList<ArithExpr> terms = new ArrayList<ArithExpr>();
				terms.add(f1);
				terms.add(f2);
				return new OperExpr(op, terms);
			}

		}

		public static ArithExpr op(ArithExpr f1, double d, int op) {
			if (f1 instanceof DoubleExpr) {
				if (op == SUM)
					return new DoubleExpr(((DoubleExpr) f1)._dConstVal + d);
				else if (op == PROD)
					return new DoubleExpr(((DoubleExpr) f1)._dConstVal * d);
				else if (op == MINUS)
					return new DoubleExpr(((DoubleExpr) f1)._dConstVal - d);
				else if (op == DIV)
					return new DoubleExpr(((DoubleExpr) f1)._dConstVal / d);
				else {
					ArrayList<ArithExpr> terms = new ArrayList<ArithExpr>();
					terms.add(new DoubleExpr(d));
					terms.add(f1);
					return new OperExpr(op, terms);
				}
			} else if (f1 instanceof OperExpr && ((OperExpr) f1)._type == op && (op == SUM || op == PROD)) {
				ArrayList<ArithExpr> terms = new ArrayList<ArithExpr>(((OperExpr) f1)._terms);
				terms.add(new DoubleExpr(d));
				return new OperExpr(op, terms);
			} else {
				ArrayList<ArithExpr> terms = new ArrayList<ArithExpr>();
				terms.add(f1);
				terms.add(new DoubleExpr(d));
				return new OperExpr(op, terms);
			}
		}

		public abstract ArithExpr substitute(HashMap<String, ArithExpr> subst);

		public abstract Double evaluate(HashMap<String, Double> cont_assign);

		public abstract Double evaluateRange(HashMap<String, Double> low_assign, HashMap<String, Double> high_assign,
				boolean use_low);

		public abstract void collectVars(HashSet<String> vars);
		
		// Assume expression is canonical, hence in sum of products form (could be a single term)
		public ArithExpr differentiateExpr(String diff_var) {
			diff_var = diff_var.intern();
			ArithExpr ret_expr = null;
			if (this instanceof OperExpr && ((OperExpr) this)._type == SUM) {
				ret_expr = ((OperExpr) this).differentiateMultipleTerms(diff_var);
			} else if (this instanceof OperExpr && ((OperExpr) this)._type == PROD) {
				ret_expr = ((OperExpr) this).differentiateTerm(diff_var);
			} else if ((this instanceof VarExpr) || (this instanceof DoubleExpr)) {
				OperExpr temp = new OperExpr(PROD, Arrays.asList(this));
				ret_expr = temp.differentiateTerm(diff_var);
			} else {
				System.out.println("differentiateLeaf: Unsupported expression '" + this + "'");
				System.exit(1);
			}
			return ret_expr;
		}
		
		// Assume expression is canonical, hence in sum of products form (could be a single term)
		public ArithExpr integrateExpr(String integration_var) {
			integration_var = integration_var.intern();
			ArithExpr ret_expr = null;
			if (this instanceof OperExpr && ((OperExpr) this)._type == SUM) {
				ret_expr = ((OperExpr) this).integrateMultipleTerms(integration_var);
			} else if (this instanceof OperExpr && ((OperExpr) this)._type == PROD) {
				ret_expr = ((OperExpr) this).integrateTerm(integration_var);
			} else if ((this instanceof VarExpr) || (this instanceof DoubleExpr)) {
				OperExpr temp = new OperExpr(PROD, Arrays.asList(this));
				ret_expr = temp.integrateTerm(integration_var);
			} else {
				System.out.println("processXADDLeaf: Unsupported expression '" + this + "'");
				System.exit(1);
			}
			return ret_expr;
		}
		
		// Takes ArithExpr expr1 linear in var, returns (coef,expr2) where expr1 = coef*x + expr2
		// Assume expression is canonical, hence in sum of products form (could be a single term)
		public CoefExprPair removeVarFromExpr(String remove_var) {
			remove_var = remove_var.intern();
			CoefExprPair ret = null;
			if (this instanceof OperExpr && ((OperExpr) this)._type == SUM) {
				ret = ((OperExpr) this).removeVarFromExprMultipleTerms(remove_var);
			} else if (this instanceof OperExpr && ((OperExpr) this)._type == PROD) {
				ret = ((OperExpr) this).removeVarFromExprTerm(remove_var);
			} else if ((this instanceof VarExpr) || (this instanceof DoubleExpr)) {
				OperExpr temp = new OperExpr(PROD, Arrays.asList(this));
				ret = temp.removeVarFromExprTerm(remove_var);
			} else {
				System.out.println("removeVarFromExpr: Unsupported expression '" + this + "'");
				System.exit(1);
			}
			return ret;
		}
		
		// Assume expression is canonical, hence in sum of products form (could be a single term)
		public int determineHighestOrderOfVar(String var) {
			var = var.intern();
			CoefExprPair ret = null;
			if (this instanceof OperExpr && ((OperExpr) this)._type == SUM) {
				return ((OperExpr) this).determineHighestOrderOfVarMultipleTerms(var);
			} else if (this instanceof OperExpr && ((OperExpr) this)._type == PROD) {
				return ((OperExpr) this).determineHighestOrderOfVarTerm(var);
			} else if ((this instanceof VarExpr) || (this instanceof DoubleExpr)) {
				OperExpr temp = new OperExpr(PROD, Arrays.asList(this));
				return temp.determineHighestOrderOfVarTerm(var);
			} else {
				System.out.println("removeVarFromExpr: Unsupported expression '" + this + "'");
				System.exit(1);
				return 0;
			}
		}
	}

	public static class OperExpr extends ArithExpr {

		public int _type = UND;
		public ArrayList<ArithExpr> _terms = null;

		public OperExpr(int type, ArithExpr t1, ArithExpr t2) {
			// this(type, Arrays.asList(new ArithExpr[] {t1, t2}));
			this(type, Arrays.asList(t1, t2));
		}

		public OperExpr(int type, List<ArithExpr> terms) {
			_type = type;

			// Ensure subtraction and division are binary operators
			if ((_type == MINUS || _type == DIV) && terms.size() != 2) {
				_type = ERROR;
				return;
			}

			_terms = new ArrayList<ArithExpr>(terms);
			if (_type == SUM || _type == PROD)
				Collections.sort(_terms);


		}

		public boolean equals(Object o) {
			if (o instanceof OperExpr) {
				OperExpr e = (OperExpr) o;
				return this._type == e._type && this._terms.equals(e._terms);
			} else
				return false;
		}

		public int hashCode() {
			return _terms.toString().hashCode() - _type;
		}

		public static int Convert2XADD(XADD context, FOPC.TFunction t) {

			if (t._nArity == 0)
				return context.getTermNode(new VarExpr(t._sFunName));

			if (t._sFunName.equals("N") && t._nArity == 4) { // Normal approximation

				ArithExpr expr  = ArithExpr.Convert2ArithExpr(t.getBinding(0));
				ArithExpr mu    = ArithExpr.Convert2ArithExpr(t.getBinding(1));
				ArithExpr var   = ArithExpr.Convert2ArithExpr(t.getBinding(2));
				ArithExpr width = ArithExpr.Convert2ArithExpr(t.getBinding(3)); // truncated outside of +/- width

				if (!(var instanceof DoubleExpr)) {
					System.out.println("Currently cannot handle non-constant variance: " + var.toString());
					System.exit(1);
				}
				double dvar = ((DoubleExpr)var)._dConstVal;

				if (!(width instanceof DoubleExpr)) {
					System.out.println("Currently cannot handle non-constant width: " + width.toString());
					System.exit(1);
				}
				double dwidth = ((DoubleExpr)width)._dConstVal;

				double Z = 3d / (4d * dwidth * dwidth * dwidth);
				double DW2 = dwidth * dwidth;
				String s = "([" + expr + " >= " + mu + " - " + dwidth + "] "
						+ "([" + expr + " <= " + mu + " + " + dwidth + "] " 
						+ "( [" + _df.format(Z) + " * " + "(-((" + expr + " - " + mu + ") * (" + expr + " - " + mu + ")) + " + DW2 + ")] )"
						+ "( [0.0] ) ) ( [0.0] ) )";

				//System.out.println("Produced: " + s);

				ArrayList l = HierarchicalParser.ParseString(s);
				//System.out.println("Parsed: " + l);
				int dd = context.buildCanonicalXADD((ArrayList) l.get(0));
				return dd;

			} else if (t._sFunName.equals("U") && t._nArity == 4) { // Triangular 

				ArithExpr expr  = ArithExpr.Convert2ArithExpr(t.getBinding(0));
				ArithExpr mu    = ArithExpr.Convert2ArithExpr(t.getBinding(1));
				ArithExpr widthl = ArithExpr.Convert2ArithExpr(t.getBinding(2)); // width left
				ArithExpr widthr = ArithExpr.Convert2ArithExpr(t.getBinding(3)); // width right

				if (!(widthl instanceof DoubleExpr)) {
					System.out.println("Currently cannot handle non-constant variance: " + widthl.toString());
					System.exit(1);
				}
				double dwidthl = ((DoubleExpr)widthl)._dConstVal;

				if (!(widthr instanceof DoubleExpr)) {
					System.out.println("Currently cannot handle non-constant width: " + widthr.toString());
					System.exit(1);
				}
				double dwidthr = ((DoubleExpr)widthr)._dConstVal;

				if (dwidthl < 0 || dwidthr < 0) {
					System.out.println("Negative widths (" + dwidthl + "," + dwidthr + ") not allowed.");
					System.exit(1);				
				}

				double Z = 1d / (dwidthl + dwidthr);
				String s = "([" + expr + " >= " + mu + " - " + dwidthl + "] "
						+ "([" + expr + " <= " + mu + " + " + dwidthr + "] " 
						+ "( [" + _df.format(Z) + "] )"
						+ "( [0.0] ) ) ( [0.0] ) )";
				ArrayList l = HierarchicalParser.ParseString(s);
				int dd = context.buildCanonicalXADD((ArrayList) l.get(0));
				return dd;

			} else if (t._sFunName.equals("T") && t._nArity == 4) { // Uniform

				ArithExpr expr  = ArithExpr.Convert2ArithExpr(t.getBinding(0));
				ArithExpr mu    = ArithExpr.Convert2ArithExpr(t.getBinding(1));
				ArithExpr widthl = ArithExpr.Convert2ArithExpr(t.getBinding(2)); // width left
				ArithExpr widthr = ArithExpr.Convert2ArithExpr(t.getBinding(3)); // width right

				if (!(widthl instanceof DoubleExpr)) {
					System.out.println("Currently cannot handle non-constant variance: " + widthl.toString());
					System.exit(1);
				}
				double dwidthl = ((DoubleExpr)widthl)._dConstVal;

				if (!(widthr instanceof DoubleExpr)) {
					System.out.println("Currently cannot handle non-constant width: " + widthr.toString());
					System.exit(1);
				}
				double dwidthr = ((DoubleExpr)widthr)._dConstVal;

				if (dwidthl < 0 || dwidthr < 0) {
					System.out.println("Negative widths (" + dwidthl + "," + dwidthr + ") not allowed.");
					System.exit(1);				
				}

				double H = 2d / (dwidthr + dwidthl);
				String s = null;

				// Handle cases where left- or right-hand sides are empty
				if (dwidthl == 0d) {
					s = "([" + expr + " >= " + mu + "] "
							+ "([" + expr + " <= " + mu + " + " + dwidthr + "] " 
							+ "( [" + _df.format(-H/dwidthr) + " * " + "(" + expr + " - " + mu + " - " + widthr + ")] )"
							+ "( [0.0] ) ) ( [0.0] ) )";		
				} else if (dwidthr == 0d) {
					s = "([" + expr + " >= " + mu + " - " + dwidthl + "] "
							+ "([" + expr + " <= " + mu + "] "
							+ "( [" + _df.format(H/dwidthl) + " * " + "(" + expr + " - " + mu + " + " + widthl + ")] )"
							+ "( [0.0] ) ) ( [0.0] ) )";					
				} else {
					s = "([" + expr + " >= " + mu + " - " + dwidthl + "] "
							+ "([" + expr + " <= " + mu + " + " + dwidthr + "] " 
							+ "([" + expr + " <= " + mu + "] "
							+ "( [" + _df.format(H/dwidthl) + " * " + "(" + expr + " - " + mu + " + " + widthl + ")] )"
							+ "( [" + _df.format(-H/dwidthr) + " * " + "(" + expr + " - " + mu + " - " + widthr + ")] ))"
							+ "( [0.0] ) ) ( [0.0] ) )";
				}

				ArrayList l = HierarchicalParser.ParseString(s);
				int dd = context.buildCanonicalXADD((ArrayList) l.get(0));
				return dd;

			} else if (t._nArity == 2) {

				// A standard operator expression convertible to a terminal node
				// The following is a bit ugly but easy to write & debug and
				// only called once when files are read (so inefficiency is OK)
				int xadd1 = Convert2XADD(context, t.getBinding(0));
				int xadd2 = Convert2XADD(context, t.getBinding(1));
				int op = UND;
				if (t._sFunName.equals("f_add")) {
					op = SUM;
				} else if (t._sFunName.equals("f_sub")) {
					op = MINUS;
				} else if (t._sFunName.equals("f_mul")) {
					op = PROD;
				} else if (t._sFunName.equals("f_div")) {
					op = DIV;
				} else {
					System.err.println("Convert2XADD: Could not process binary function: " + t.toFOLString());
					System.exit(1);					
				}
				return context.apply(xadd1, xadd2, op);
			} else {
				System.err.println("Convert2XADD: Could not process: " + t.toFOLString());
				System.exit(1); return -1;
			}
		}

		public static ArithExpr Convert2OperExpr(FOPC.TFunction t) {
			// System.out.println("Convert2OperExpr: [" + t._nArity + "] "
			// + t.toFOLString());
			if (t._nArity == 0)
				return new VarExpr(t._sFunName);

			// The following is a bit ugly but easy to write & debug and
			// only called once when files are read (so inefficiency is OK)
			ArithExpr term1 = ArithExpr.Convert2ArithExpr(t.getBinding(0));
			ArithExpr term2 = ArithExpr.Convert2ArithExpr(t.getBinding(1));
			ArrayList<ArithExpr> terms = new ArrayList<ArithExpr>();
			int type = UND;
			if (t._sFunName.equals("f_add")) {
				type = SUM;
			} else if (t._sFunName.equals("f_sub")) {
				type = MINUS;
				if (term1 instanceof DoubleExpr && term2 instanceof DoubleExpr && ((DoubleExpr) term1)._dConstVal == 0d)
					return new DoubleExpr(-((DoubleExpr) term2)._dConstVal);
			} else if (t._sFunName.equals("f_mul")) {
				type = PROD;
			} else if (t._sFunName.equals("f_div")) {
				type = DIV;
			}
			if (type == UND)
				return null;
			if ((type == SUM || type == PROD) && (term1 instanceof OperExpr) && ((OperExpr) term1)._type == type) {
				terms.addAll(((OperExpr) term1)._terms);
				term1 = null;
			}
			if ((type == SUM || type == PROD) && (term2 instanceof OperExpr) && ((OperExpr) term2)._type == type) {
				terms.addAll(((OperExpr) term2)._terms);
				term2 = null;
			}
			if (term1 != null)
				terms.add(term1);
			if (term2 != null)
				terms.add(term2);
			return new OperExpr(type, terms);
		}

		public String toString(boolean format) {
			StringBuilder sb = new StringBuilder("(");
			for (int i = 0; i < _terms.size(); i++) {
				if (i != 0)
					sb.append(" " + _aOpNames[_type] + " ");
				sb.append(_terms.get(i).toString(format));
			}
			sb.append(")");
			return sb.toString();
		}

		public void collectVars(HashSet<String> vars) {
			for (ArithExpr e : _terms)
				e.collectVars(vars);
		}

		public ArithExpr substitute(HashMap<String, ArithExpr> subst) {
			ArrayList<ArithExpr> terms2 = new ArrayList<ArithExpr>();
			for (ArithExpr expr : _terms)
				terms2.add(expr.substitute(subst));
			OperExpr expr = new OperExpr(_type, terms2);
			if (expr._terms.size() == 1)
				return expr._terms.get(0);
			else
				return expr;
		}

		public Double evaluate(HashMap<String, Double> cont_assign) {
			Double accum = _terms.get(0).evaluate(cont_assign);
			for (int i = 1; i < _terms.size() && accum != null; i++) {
				Double term_eval = _terms.get(i).evaluate(cont_assign);
				if (term_eval == null)
					accum = null;
				else
					switch (_type) {
					case SUM:
						accum = accum + term_eval;
						break;
					case MINUS:
						accum = accum - term_eval;
						break;
					case PROD:
						accum = accum * term_eval;
						break;
					case DIV:
						accum = accum / term_eval;
						break;
					default:
						accum = null;
					}
			}
			return accum;
		}

		@Override
		public Double evaluateRange(HashMap<String, Double> low_assign, HashMap<String, Double> high_assign,
				boolean use_low) {

			// Must use canonical nodes here... assumes products are binary
			// with the first term being a coefficient
			if (!USE_CANONICAL_NODES) {
				System.err.println("Must use canonical nodes if using evaluateRange");
				System.exit(1);
			}

			if (DEBUG_EVAL_RANGE)
				System.out.println("Evaluating " + (use_low ? "min" : "max") + " range: " + this);

			HashMap<String, Double> assign = use_low ? low_assign : high_assign;
			Double accum = _terms.get(0).evaluateRange(low_assign, high_assign, use_low);
			if (DEBUG_EVAL_RANGE)
				System.out.println("- Term eval [" + 0 + "] = " + _terms.get(0) + " = " + accum + " -- " + use_low);

			boolean subterm_use_low = (_type == MINUS || _type == DIV || (_type == PROD && accum < 0d)) ? !use_low
					: use_low;

			for (int i = 1; i < _terms.size() && accum != null; i++) {

				Double term_eval = _terms.get(i).evaluateRange(low_assign, high_assign, subterm_use_low);
				if (DEBUG_EVAL_RANGE)
					System.out.println("- Term eval [" + i + "] = " + _terms.get(i) + " = " + term_eval + " -- "
							+ subterm_use_low);
				if (term_eval == null)
					accum = null;
				else
					switch (_type) {
					case SUM:
						accum += term_eval;
						break;
					case MINUS:
						accum -= term_eval;
						break;
					case PROD:
						accum *= term_eval;
						break;
					case DIV:
						accum /= term_eval;
						break;
					default:
						accum = null;
					}
				if (DEBUG_EVAL_RANGE)
					System.out.println("- accum: " + accum);
			}
			if (DEBUG_EVAL_RANGE)
				System.out.println("* Result " + (use_low ? "min" : "max") + " range: " + accum);
			return accum;
		}

		public boolean checkCanonical() {

			// This node is canonical if it is term canonical
			// or it is a sum of terms
			if (checkTermCanonical())
				return true;

			if (_type == SUM) {
				// Ensure all subterms are canonical
				for (int i = 0; i < _terms.size(); i++) {

					// First term can be a constant so long as more than one term
					if (i == 0 && (_terms.get(0) instanceof DoubleExpr)) {
						if (Math.abs(((DoubleExpr) _terms.get(0))._dConstVal) <= 1e-6d)
							return false;
						else
							continue;
					}

					if (!(_terms.get(i) instanceof OperExpr) || !((OperExpr) _terms.get(i)).checkTermCanonical()) {
						// System.out.println("-- not canonical because [" + i + "]" + _terms.get(i) + " : " +
						// _terms.get(i).getClass());
						return false;
					}
				}

				return true;
			} else {
				// System.out.println("-- not canonical because not SUM: " + _aOpNames[_type]);
				return false;
			}
		}

		public boolean checkTermCanonical() {
			// This is term canonical if it is a product of a constant followed by variables
			if (_type == PROD) {
				if (!(_terms.get(0) instanceof DoubleExpr))
					return false;

				for (int i = 1; i < _terms.size(); i++) {
					if (!(_terms.get(i) instanceof VarExpr))
						return false;
				}

				return true;
			} else
				return false;
		}

		// Canonicity for arithmetic expressions:
		//
		// 1. Expressions all zero on RHS of comparisons and restrict symbols:
		// a > b : a <= b and swap branches
		// a < b : a >= b and swap branches
		// a != b : a == b and swap branches
		// 2. Multiple layers of + / * collapsed: (X + Y) + Z -> X + Y + Z
		// 3. Distribute * over +: X * (A + B) -> X * A + X * B
		// 4. All subtraction: X - Y -> X + -Y
		// ??? 5. Division -> multiplication
		// ??? 6. Multiple multipled divisions -> numerator and denominator
		// 7. Sorting of OperExpr terms with TreeSet?
		// 8. Make all products start with a single Double coefficient
		// 9. Compress / remove common polynomial terms
		// 10. Prevent singleton operator expressions
		public static int ALREADY_CANONICAL = 0;
		public static int NON_CANONICAL = 0;

		public Expr makeCanonical() {

			// A simple non-canonical case is OperExpr - 0, so catch this
			if (_type == MINUS && _terms.get(1) instanceof DoubleExpr && ((DoubleExpr) _terms.get(1))._dConstVal == 0d) {
				return _terms.get(0).makeCanonical();
			}

			// If already canonical, no need to modify
			if (checkCanonical()) {
				// System.out.println("** Already canonical: " + this);
				ALREADY_CANONICAL++;
				return this;
			} else {
				// System.out.println("** Not canonical: " + this);
				NON_CANONICAL++;
			}

			int new_type = _type;
			ArrayList<ArithExpr> new_terms = new ArrayList<ArithExpr>(_terms);

			// 4. All subtraction: X - Y -> X + -Y
			if (new_type == MINUS) {
				ArithExpr term2 = new_terms.get(1);
				term2 = ArithExpr.op(term2, NEG_ONE, PROD);
				new_terms.set(1, term2);
				new_type = SUM;
			}

			// Recursively ensure all subterms in canonical form, and then
			// 2. Multiple layers of + / * collapsed: (X + Y) + Z -> X + Y + Z
			ArrayList<ArithExpr> reduced_terms = new ArrayList<ArithExpr>();
			for (ArithExpr e : new_terms) {
				e = (ArithExpr) e.makeCanonical();
				// If same type, add all subterms directly to reduced terms
				if ((e instanceof OperExpr) && ((OperExpr) e)._type == new_type
						&& (new_type == SUM || new_type == PROD))
					reduced_terms.addAll(((OperExpr) e)._terms);
				else
					reduced_terms.add(e);
			}
			new_terms = reduced_terms;
			// System.out.println(">> Flattened terms: " + reduced_terms);

			// 3. Distribute * over +: X * (A + B) -> X * A + X * B
			// X * (1/Y) * (W + Z) * (U + V)
			// Maintain sum list...
			// if division, multiply in 1/x
			if (new_type == PROD) {

				ArrayList<ArithExpr> sum_terms = new ArrayList<ArithExpr>();
				ArithExpr first_term = new_terms.get(0);
				if ((first_term instanceof OperExpr) && ((OperExpr) first_term)._type == SUM)
					sum_terms.addAll(((OperExpr) first_term)._terms);
				else
					sum_terms.add(first_term);

				for (int i = 1; i < new_terms.size(); i++) {
					ArithExpr e = new_terms.get(i);
					if ((e instanceof OperExpr) && ((OperExpr) e)._type == SUM) {
						// e2 : {A + B} * e3 : {C + D}
						// System.out.println(">>>> Mult 1 " + e + " * " + sum_terms);
						ArrayList<ArithExpr> new_sum_terms = new ArrayList<ArithExpr>();
						for (ArithExpr e2 : sum_terms) {
							for (ArithExpr e3 : ((OperExpr) e)._terms) {
								// System.out.println(">>>> Multiplying " + e2 + " * " + e3);
								new_sum_terms.add(ArithExpr.op(e2, e3, PROD));
							}
						}
						// System.out.println(">>>> Mult 1 Out " + new_sum_terms);
						sum_terms = new_sum_terms;
					} else {
						// e2 : {A + B} * e
						// System.out.println(">>>> Mult 2 " + e + " * " + sum_terms);
						for (int j = 0; j < sum_terms.size(); j++) {
							ArithExpr e2 = sum_terms.get(j);
							sum_terms.set(j, new OperExpr(PROD, e, e2));
						}
					}
				}

				// If sum_terms are singular no need to modify, otherwise
				if (sum_terms.size() > 1) {

					new_type = SUM;

					// Again make canonical and collapse terms where possible
					new_terms.clear();
					for (ArithExpr e : sum_terms) {
						e = (ArithExpr) e.makeCanonical();
						// If same type, add all subterms directly to reduced terms
						if ((e instanceof OperExpr) && ((OperExpr) e)._type == SUM)
							new_terms.addAll(((OperExpr) e)._terms);
						else
							new_terms.add(e);
					}
				}
			}

			// 9. Merge (and remove) all polynomial terms in a sum
			if (new_type == SUM) {
				ArrayList<ArithExpr> non_terms = new ArrayList<ArithExpr>();
				double const_sum = 0d;

				// Hash all terms to a coefficient
				HashMap<ArrayList<ArithExpr>, Double> term2coef = new HashMap<ArrayList<ArithExpr>, Double>();
				for (ArithExpr e : new_terms) {
					if ((e instanceof OperExpr && ((OperExpr) e)._type == PROD) || (e instanceof VarExpr)) {

						// Determine the terms and coefficient
						ArrayList<ArithExpr> index = new ArrayList<ArithExpr>();
						DoubleExpr d = null;
						if (e instanceof VarExpr) {
							index.add(e);
							d = new DoubleExpr(1d);
						} else {
							OperExpr o = (OperExpr) e;
							d = (DoubleExpr) o._terms.get(0);
							for (int j = 1; j < o._terms.size(); j++)
								index.add(o._terms.get(j));
						}

						// Hash to the correct coefficient
						Double dval = null;
						if ((dval = term2coef.get(index)) != null)
							dval += d._dConstVal;
						else
							dval = d._dConstVal;
						term2coef.put(index, dval);

					} else if (e instanceof DoubleExpr) {
						const_sum += ((DoubleExpr) e)._dConstVal;
					} else
						non_terms.add(e);
				}

				// Convert back to an ArrayList
				new_terms = non_terms;
				if (const_sum != 0d)
					new_terms.add(0, new DoubleExpr(const_sum));

				for (Map.Entry<ArrayList<ArithExpr>, Double> t : term2coef.entrySet()) {
					double val = t.getValue();
					if (val == 0d)
						continue;

					ArrayList<ArithExpr> term = t.getKey();
					DoubleExpr dcoef = new DoubleExpr(val);
					term.add(0, dcoef);
					new_terms.add(new OperExpr(PROD, term));
				}

				// An empty sum is zero
				if (new_terms.size() == 0)
					return new DoubleExpr(0d);
			}

			// 8. Make all products start with a single Double coefficient
			if (new_type == PROD) {
				double coef = 1d;
				ArrayList<ArithExpr> factors = new ArrayList<ArithExpr>();
				for (ArithExpr e : new_terms) {
					if (e instanceof DoubleExpr)
						coef *= ((DoubleExpr) e)._dConstVal;
					else
						factors.add(e);
				}
				if (coef != 0d) {
					factors.add(0, new DoubleExpr(coef));
					new_terms = factors; // Will be sorted on new OperExpr
				} else {
					return new DoubleExpr(0d);
				}
			}
			
			// Handle division of two constants
			if (new_type == DIV && new_terms.get(0) instanceof DoubleExpr && new_terms.get(1) instanceof DoubleExpr) {
				return new DoubleExpr(((DoubleExpr)new_terms.get(0))._dConstVal / ((DoubleExpr) new_terms.get(1))._dConstVal);
			}

			// 10. Prevent singleton operator expressions
			if (new_terms.size() == 1) {
				return new_terms.get(0);
			}

			// Ensure canonical order
			OperExpr canonical_expr = new OperExpr(new_type, new_terms);
			// System.out.println("- now canonical: " + canonical_expr);
			return canonical_expr;
		}

		@Override
		public String toString() {
			return toString(true);
		}
		
		// Must be a SUM of terms to get here
		public OperExpr differentiateMultipleTerms(String diff_var) {
			if (this._type != SUM) {
				System.out.println("differentiateMultipleTerms: Expected SUM, got '" + this + "'");
				System.exit(1);
			}
			ArrayList<ArithExpr> differentiated_terms = new ArrayList<ArithExpr>();
			for (ArithExpr e : this._terms) {
				if (e instanceof OperExpr) {
					differentiated_terms.add(((OperExpr) e).differentiateTerm(diff_var));
				} else if ((e instanceof VarExpr) || (e instanceof DoubleExpr)) {
					OperExpr temp = new OperExpr(PROD, Arrays.asList(e));
					differentiated_terms.add(temp.differentiateTerm(diff_var));
				} else {
					System.out.println("differentiateMultipleTerms: Unsupported expression '" + e + "'");
					System.exit(1);
				}
			}
			return new OperExpr(SUM, differentiated_terms);
		}

		// A single term (PROD)
		public ArithExpr differentiateTerm(String diff_var) {
			if (this._type != PROD) {
				System.out.println("differentiateTerm: Expected PROD, got '" + this + "'");
				System.exit(1);
			}

			// Process all terms (optional double followed by vars)
			int derivative_var_count = 0;
			int last_var_added_at = -1;
			DoubleExpr d = new DoubleExpr(1d);
			ArrayList<ArithExpr> factors = new ArrayList<ArithExpr>();
			for (ArithExpr e : this._terms) {
				if (e instanceof DoubleExpr) {
					DoubleExpr d2 = (DoubleExpr) e;
					d = new DoubleExpr(d._dConstVal * d2._dConstVal);
				} else if (e instanceof VarExpr) {
					factors.add(e);
					// Both interned so we can test direct equality
					if (((VarExpr) e)._sVarName == diff_var) {
						derivative_var_count++;
						last_var_added_at = factors.size() - 1;
					}
				} else {
					System.out.println("differentiateTerm: Unsupported expression '" + e + "'");
					System.exit(1);
				}
			}

			// Perform differentiation
			if (derivative_var_count == 0) {
				return ZERO; // Derivative of a constant is 0
			} else {
				// x*x*...*x = x^n
				// d/dx x^n = n*x^{n-1}
				factors.remove(last_var_added_at);
				d = new DoubleExpr(((double) derivative_var_count) * d._dConstVal);
				factors.add(0, d);

				return new OperExpr(PROD, factors);
			}
		}
		
		// Must be a SUM of terms to get here
		public OperExpr integrateMultipleTerms(String integration_var) {
			if (this._type != SUM) {
				System.out.println("integrateMultipleTerms: Expected SUM, got '" + this + "'");
				System.exit(1);
			}
			ArrayList<ArithExpr> integrated_terms = new ArrayList<ArithExpr>();
			for (ArithExpr e : this._terms) {
				if (e instanceof OperExpr) {
					integrated_terms.add(((OperExpr) e).integrateTerm(integration_var));
				} else if ((e instanceof VarExpr) || (e instanceof DoubleExpr)) {
					OperExpr temp = new OperExpr(PROD, Arrays.asList(e));
					integrated_terms.add(temp.integrateTerm(integration_var));
				} else {
					System.out.println("integrateMultipleTerms: Unsupported expression '" + e + "'");
					System.exit(1);
				}
			}
			return new OperExpr(SUM, integrated_terms);
		}

		// A single term (PROD)
		public ArithExpr integrateTerm(String integration_var) {
			if (this._type != PROD) {
				System.out.println("integrateTerm: Expected PROD, got '" + this + "'");
				System.exit(1);
			}

			// Process all terms (optional double followed by vars)
			int integration_var_count = 0;
			DoubleExpr d = new DoubleExpr(1d);
			ArrayList<ArithExpr> factors = new ArrayList<ArithExpr>();
			for (ArithExpr e : this._terms) {
				// while (e instanceof OperExpr) {
				// OperExpr oe = (OperExpr) e;
				// e = e.op(oe._terms.get(0), oe._terms.get(1), oe._type);
				// }
				if (e instanceof DoubleExpr) {
					DoubleExpr d2 = (DoubleExpr) e;
					d = new DoubleExpr(d._dConstVal * d2._dConstVal);
				} else if (e instanceof VarExpr) {
					factors.add(e);
					// Both interned so we can test direct equality
					if (((VarExpr) e)._sVarName == integration_var)
						integration_var_count++;
				} else {
					System.out.println("integrateTerm: Unsupported expression '" + e + "'");
					System.exit(1);
				}
			}

			// Perform integration
			factors.add(new VarExpr(integration_var));
			d = new DoubleExpr(d._dConstVal / (double) (integration_var_count + 1));
			factors.add(0, d);

			return new OperExpr(PROD, factors);
		}
		
		// Must be a SUM of terms to get here
		public CoefExprPair removeVarFromExprMultipleTerms(String remove_var) {
			if (this._type != SUM) {
				System.out.println("removeVarFromExprMultipleTerms: Expected SUM, got '" + this + "'");
				System.exit(1);
			}
			double var_coef = 0d;
			ArrayList<ArithExpr> remaining_terms = new ArrayList<ArithExpr>();
			for (ArithExpr e : this._terms) {
				if (e instanceof OperExpr) {
					CoefExprPair p = ((OperExpr) e).removeVarFromExprTerm(remove_var);
					var_coef += p._coef;
					remaining_terms.add(p._expr);
				} else if (e instanceof VarExpr) {
					OperExpr temp = new OperExpr(PROD, Arrays.asList(e));
					CoefExprPair p = temp.removeVarFromExprTerm(remove_var);
					var_coef += p._coef;
					remaining_terms.add(p._expr);
				} else if (e instanceof DoubleExpr) {
					remaining_terms.add(e);
				} else {
					System.out.println("removeVarFromExprMultipleTerms: Unsupported expression '" + e + "'");
					System.exit(1);
				}
			}
			return new CoefExprPair(new OperExpr(SUM, remaining_terms), var_coef);
		}

		// A single term (PROD)
		public CoefExprPair removeVarFromExprTerm(String remove_var) {
			double var_coef = 0d;
			if (this._type != PROD) {
				System.out.println("removeVarFromExprTerm: Expected PROD, got '" + this + "'");
				System.exit(1);
			}

			// Process all terms (optional double followed by vars)
			int remove_var_count = 0;
			double coef = 1d;
			ArrayList<ArithExpr> factors = new ArrayList<ArithExpr>();
			for (ArithExpr e : this._terms) {
				if (e instanceof DoubleExpr) {
					coef *= ((DoubleExpr) e)._dConstVal;
				} else if (e instanceof VarExpr) {
					// Both interned so we can test direct equality
					if (((VarExpr) e)._sVarName == remove_var)
						remove_var_count++;
					else
						factors.add(e);
				} else {
					System.out.println("removeVarFromExprTerm: Unsupported expression '" + e + "'");
					System.exit(1);
				}
			}

			// Check for proper form
			if (remove_var_count > 0) {
				if (remove_var_count > 1 || factors.size() > 0) {
					System.out.println("removeVarFromExprTerm: var '" + remove_var
							+ "' must appear linearly in expression '" + this + "'");
					System.exit(1);
				}
				// If get here only coef*_integrationVar
				var_coef += coef;
				return new CoefExprPair(ZERO, var_coef); // Just add a zero term in this place
			} else {
				factors.add(0, new DoubleExpr(coef));
				return new CoefExprPair(new OperExpr(PROD, factors), var_coef);
			}
		}
		
		// Must be a SUM of terms to get here
		public int determineHighestOrderOfVarMultipleTerms(String var) {
			if (this._type != SUM) {
				System.out.println("determineHighestOrderOfVarMultipleTerms: Expected SUM, got '" + this + "'");
				System.exit(1);
			}
			int max_order = 0;
			for (ArithExpr e : this._terms) {
				if (e instanceof OperExpr) {
					max_order = Math.max(max_order, ((OperExpr) e).determineHighestOrderOfVarTerm(var));
				} else if (e instanceof VarExpr) {
					OperExpr temp = new OperExpr(PROD, Arrays.asList(e));
					max_order = Math.max(max_order, temp.determineHighestOrderOfVarTerm(var));
				} else if (!(e instanceof DoubleExpr)) {
					System.out.println("determineHighestOrderOfVarMultipleTerms: Unsupported expression '" + e + "'");
					System.exit(1);
				}
			}
			return max_order;
		}

		// A single term (PROD)
		public int  determineHighestOrderOfVarTerm(String var) {
			double var_coef = 0d;
			if (this._type != PROD) {
				System.out.println("determineHighestOrderOfVarTerm: Expected PROD, got '" + this + "'");
				System.exit(1);
			}

			// Process all terms (optional double followed by vars)
			int integration_var_count = 0;
			double coef = 1d;
			ArrayList<ArithExpr> factors = new ArrayList<ArithExpr>();
			for (ArithExpr e : this._terms) {
				if (e instanceof DoubleExpr) {
					coef *= ((DoubleExpr) e)._dConstVal;
				} else if (e instanceof VarExpr) {
					// Both interned so we can test direct equality
					if (((VarExpr) e)._sVarName == var)
						integration_var_count++;
					else
						factors.add(e);
				} else {
					System.out.println("determineHighestOrderOfVarTerm: Unsupported expression '" + e + "'");
					System.exit(1);
				}
			}
			return integration_var_count;
		}
	}

	public static class DoubleExpr extends ArithExpr {
		public double _dConstVal = Double.NaN;

		public DoubleExpr(double val) {
			_dConstVal = val;
		}

		public String toString() {
			return _df.format(_dConstVal);
		}

		@Override
		public String toString(boolean format) {
			return _df_unformatted.format(_dConstVal);
		}

		public boolean equals(Object o) {
			if (o instanceof DoubleExpr) {
				DoubleExpr d = (DoubleExpr) o;
				//return Math.abs(this._dConstVal - d._dConstVal) <= TOL;
				return this._dConstVal == d._dConstVal;
			} else
				return false;
		}

		public int hashCode() {
			return Float.floatToIntBits((float) _dConstVal);
		}

		public ArithExpr substitute(HashMap<String, ArithExpr> subst) {
			return this;
		}

		public Double evaluate(HashMap<String, Double> cont_assign) {
			return _dConstVal;
		}

		@Override
		public Double evaluateRange(HashMap<String, Double> low_assign, HashMap<String, Double> high_assign,
				boolean use_low) {
			return _dConstVal;
		}

		public void collectVars(HashSet<String> vars) {
		}

		public Expr makeCanonical() {
			return this;
		}

	}

	public static class VarExpr extends ArithExpr {
		public String _sVarName = null;
		public int _nHashCode = -1;

		public VarExpr(String name) {
			_sVarName = name.intern();
			_nHashCode = _sVarName.hashCode();
		}

		public String toString() {
			return _sVarName;
		}

		public int hashCode() {
			return _nHashCode;
		}

		public boolean equals(Object o) {
			if (o instanceof VarExpr) {
				VarExpr v = (VarExpr) o;
				return this._sVarName == v._sVarName;
			} else
				return false;
		}

		public ArithExpr substitute(HashMap<String, ArithExpr> subst) {
			ArithExpr v = subst.get(_sVarName);
			return v != null ? v : this;
		}

		public Double evaluate(HashMap<String, Double> cont_assign) {
			return cont_assign.get(_sVarName);
		}

		@Override
		public Double evaluateRange(HashMap<String, Double> low_assign, HashMap<String, Double> high_assign,
				boolean use_low) {
			return use_low ? low_assign.get(_sVarName) : high_assign.get(_sVarName);
		}

		public void collectVars(HashSet<String> vars) {
			vars.add(_sVarName);
		}

		public Expr makeCanonical() {
			return new OperExpr(PROD, new DoubleExpr(1d), this);
		}

		@Override
		public String toString(boolean format) {
			return toString();
		}
	}

	public static String indent(int depth) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i <= depth; i++)
			sb.append(STRING_TAB);
		return sb.toString();
	}

	///////////////////////////////////////////////////////////////////
	// Cache flushing
	///////////////////////////////////////////////////////////////////
	
	public void clearSpecialNodes() {
		_hsSpecialNodes.clear();
	}

	public void addSpecialNode(Integer n) {
		if (n == null) {
			System.out.println("addSpecialNode: null");
			new Exception().printStackTrace();
			System.exit(1);
		} 
		_hsSpecialNodes.add(n);
	}

	public void flushCaches() {
		System.out.print("[FLUSHING CACHES... ");

		// Can always clear these
		_hmReduceCache.clear();
		_hmReduceLeafOpCache.clear();
		_hmApplyCache.clear();
		_hmINode2Vars.clear();
		_hmReduceAnnotateCache.clear();

		// Set up temporary alternates to these HashMaps
		_hmNode2IntNew = new HashMap<XADDNode, Integer>();
		_hmInt2NodeNew = new HashMap<Integer, XADDNode>();

		// Copy over 'special' nodes then set new maps
		System.out.println(_hsSpecialNodes);
		for (Integer n : _hsSpecialNodes) {
			copyInNewCacheNode(n);
		}
		_hmNode2Int = _hmNode2IntNew;
		_hmInt2Node = _hmInt2NodeNew;

		Runtime.getRuntime().gc();
	}

	private void copyInNewCacheNode(Integer id) {

		if (_hmInt2NodeNew.containsKey(id)) {
			return;
		}
		
		// Insert node into cache
		XADDNode node = _hmInt2Node.get(id);
		_hmInt2NodeNew.put(id, node);
		_hmNode2IntNew.put(node, id);
		
		// Recurse on children (if any) and insert them
		if (node instanceof XADDINode) {
			copyInNewCacheNode(((XADDINode) node)._high);
			copyInNewCacheNode(((XADDINode) node)._low);
		} 
	}

}