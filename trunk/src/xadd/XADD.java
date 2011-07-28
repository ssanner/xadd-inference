//////////////////////////////////////////////////////////////////////
//
// Extended Algebraic Decision Diagrams
//
// Decision tests have labels -- either boolean variables or arithmetic
//                               expressions (could be relational expressions
//                               as well -- same as FOADD, but not used here)
//                               Need a list of decisions (type: var, expr)
// 
//                               leaves are expressions (can be constants)
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
import java.util.List;
import java.util.Map;

import logic.kb.fol.FOPC;
import lpsolve.LP;
import lpsolve.LpSolve;
import util.IntPair;
import util.IntTriple;
import camdp.CAction;
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

	// Debug
	public final static boolean CHECK_LOCAL_ORDERING = true;
	public final static boolean SHOW_DECISION_EVAL = false;
	public final static boolean DEBUG_EVAL_RANGE = false;
	public final static boolean DEBUG_CONSTRAINTS = false;

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
	public final static int ERROR = 15;
	public final static String[] _aOpNames = {/* 0 */"UND",
	/* 1 */"+", "-", "*", "/", "max", "min", "|l", "|h",
	/* 9 */"=", "!=", ">", ">=", "<", "<=", "ERROR" };

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
	public HashMap<IntTriple, Integer> _hmReduceLPCache = new HashMap<IntTriple, Integer>();
	public HashMap<IntPair, Integer> _hmReduceLeafOpCache = new HashMap<IntPair, Integer>();
	public HashMap<IntTriple, Integer> _hmApplyCache = new HashMap<IntTriple, Integer>();
	public HashMap<XADDINode, HashSet<String>> _hmINode2Vars = new HashMap<XADDINode, HashSet<String>>();
	public HashSet<String> _hsBooleanVars = new HashSet<String>();

	public HashMap<String, Double> _hmMinVal = new HashMap<String, Double>();
	public HashMap<String, Double> _hmMaxVal = new HashMap<String, Double>();

	// Flush
	public HashSet<Integer> _hsSpecialNodes = new HashSet<Integer>();
	public HashMap<XADDNode, Integer> _hmNode2IntNew = new HashMap<XADDNode, Integer>();
	public HashMap<Integer, XADDNode> _hmInt2NodeNew = new HashMap<Integer, XADDNode>();

	// Methods
	public XADD() {

	}

	public int getBoolVarIndex(String bool_name) {
		return getVarIndex(new BoolDec(bool_name), false);
	}

	public int getVarIndex(Decision d) {
		return getVarIndex(d, false);
	}

	public int getVarIndex(Decision d, boolean create) {

		if (USE_CANONICAL_NODES) {
			// System.out.println(">> Before canonical: " + d);
			d = d.makeCanonical();
			// System.out.println(">> After canonical: " + d);
		}

		int index = _alOrder.indexOf(d);
		if (index >= 0 || !create)
			return index;
		else {
			_alOrder.add(d);
			return _alOrder.size() - 1;
		}
	}

	public XADDTNode _tempTNode = new XADDTNode(null);

	public int getTermNode(ArithExpr e) {

		if (USE_CANONICAL_NODES) {
			// System.out.println(">> TNode: Before canonical: " + e);
			e = (ArithExpr) e.makeCanonical();
			// System.out.println(">> TNode: After canonical: " + e);
		}

		_tempTNode.set(e);
		Integer id = _hmNode2Int.get(_tempTNode);
		if (id == null) {
			// Not in cache so create
			id = _nodeCounter;
			XADDTNode node = new XADDTNode(e);
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

	// //////////////////////////////////////////////////////////////

	// TODO: This DD can still be out of order if parse tree had out-of-order
	// variables.
	public int buildCanonicalXADD(ArrayList l) {
		return reduce(buildNonCanonicalXADD(l));
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
		return root.toString(format);
	}

	// ////////////////////////////////////////////////////////////////////

	// TODO: Implement garbage collection with my counter increment trick
	// and marking.

	public IntTriple _tempReduceKey = new IntTriple(-1, -1, -1);

	public int reduce(int node) {
		return reduceOp(node, -1, -1);
	}

	public int opOut(int node_id, int var_id, int op) {
		int ret = reduceOp(node_id, var_id, op);

		// operations like sum and product may get decisions out of order
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
		// by default if hashing and equality testing are working in getTNode
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
				ret = apply(low, high, op); // may not be canonical, but will be fixed
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

		// If its an internal node, check the reduce cache
		_tempReduceLPKey.set(node_id, test_var.hashCode(), test_dec.hashCode());
		if ((ret = _hmReduceLPCache.get(_tempReduceLPKey)) != null) {
			// System.out.println("In cache, returning: " + qret);
			return ret;
		}

		XADDINode inode = (XADDINode) n;

		// //////////////////////////////////////////////////////////////////////
		// if (CHECK_MIN_MAX) { // Handled by makeCanonical
		// *
		// }

		// //////////////////////////////////////////////////////////////////////
		// if (ret == null) {
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
		// }
		// //////////////////////////////////////////////////////////////////////

		// Put return value in cache and return
		_hmReduceLPCache.put(new IntTriple(node_id, test_var.hashCode(), test_dec.hashCode()), ret);
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
				System.out.println("Unexpected RHS constraint value: " + e._expr._rhs);
				new Exception().printStackTrace(System.out);
				System.exit(1);
			}

			// From here we just need convert LHS to coefficients and construct
			// correct constraint from CompExpr type
			double[] coefs = new double[_cvar2ID.size()];
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
	private double setCoefficients(ArithExpr e, double[] coefs) {

		int error = 0;
		double accum = 0d;
		if (e instanceof OperExpr) {
			OperExpr o = ((OperExpr) e);
			if (o._type == PROD) {
				if (o._terms.size() != 2)
					error = 1;
				else {
					int index = _cvar2ID.get(((VarExpr) o._terms.get(1))._sVarName);
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
			new Exception().printStackTrace(System.out);
			System.exit(1);
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
			ArithExpr expr = ((XADDTNode) n)._expr;
			expr = expr.substitute(subst);
			return getTermNode(expr);
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
		//
		// ret = getINode(var, low, high);
		int T_ZERO = getTermNode(ZERO);
		int T_ONE = getTermNode(ONE);
		int ind_true = getINode(var, /* low */T_ZERO, /* high */T_ONE);
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

	// ///////////

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

	// This internal class is intended to be used to specify inline operations to perform at the
	// leaves of an XADD... the operation returns a new XADD for the leaf.
	//
	// 'decisions' and 'decision_values' are parallel arrays indicating the path taken to 'leaf_val'
	// 'xadd' is the XADD being subsituted for 'var'
	public abstract class XADDLeafOperation {
		public abstract int processXADDLeaf(ArrayList<Decision> decisions, ArrayList<Boolean> decision_values,
				ArithExpr leaf_val);
	}

	
	/////////////////////////////////////////////////////
	
	///New class added by Zahra 
	//Class of XADDLeafMax that preforms max on the leaves 
	//This max is over the LB,UB or any root of the polynomial 
	
	public class XADDLeafMax extends XADDLeafOperation {
		
		public int _runningMax; // XADD for the running max of all leaf substitutions
		CAction _action;
		double _VarCoef;
		String _actionString;

		public XADDLeafMax(CAction action_var,String action_St) {
			_action = action_var;
			_VarCoef = Double.NEGATIVE_INFINITY;
			_actionString = action_St.intern(); // incase there is more than one continuous parameter per action
			//we need the action because the bounds are required here
			_runningMax = getTermNode(ZERO);
		}

		public int processXADDLeaf(ArrayList<Decision> decisions, ArrayList<Boolean> decision_values, ArithExpr leaf_val) 
		{

			// Multiply these in later
			HashMap<Decision, Boolean> int_var_indep_decisions = new HashMap<Decision, Boolean>();

			// Next compute the upper and lower bounds based on the decisions
			System.out.println("=============================");
			System.out.println("Current node: " + leaf_val);
			System.out.println("Decisions to get to get here: " + decisions + " = " + decision_values + "\n===\n");
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

				_VarCoef = 0d;
				ArithExpr lhs_isolated = removeActionVar(comp._lhs);
				System.out.println("Pre: " + comp + " == " + is_true + ", int var [" + _actionString + "]"
						+ "\nLHS isolated: " + lhs_isolated + "\n ==>  " + _VarCoef + " * "
						+ _actionString);

				if (_VarCoef == 0d) {
					int_var_indep_decisions.put(d, is_true);
					continue;
				}

				// We have coef*x + expr COMP_OPER 0
				boolean flip_comparison = (_VarCoef < 0d) && (comp._type != EQ) && (comp._type != NEQ);
				ArithExpr new_rhs = (ArithExpr) new OperExpr(MINUS, ZERO, new OperExpr(PROD, new DoubleExpr(
						1d / _VarCoef), lhs_isolated)).makeCanonical();

				// Divide through by coef (pos or neg)
				// - if coef neg, flip expression
				// - if decision neg, flip expression
				// - if both, don't flip
				int comp_oper = comp._type;
				if ((/* negated */!is_true && !flip_comparison) || (/* not negated */is_true && flip_comparison)) {
					comp_oper = CompExpr.flipCompOper(comp_oper);
				}

				// Now mark this as a lower or upper bound
				// - lower bounds: x > f(y), x >= f(y)
				// - upper bounds: x < f(z), x <= f(z)
				if (comp_oper == GT || comp_oper == GT_EQ)
					lower_bound.add(new_rhs);
				else if (comp_oper == LT || comp_oper == LT_EQ)
					upper_bound.add(new_rhs);
				else {
					System.out.println("Cannot currently handle: "
							+ new CompExpr(comp_oper, new VarExpr(_actionString), new_rhs));
					System.out.println("Note: = triggers substitution, not sure how to handle ~=");
					new Exception().printStackTrace();
					System.exit(1);
				}
			}
			//add the bounds in the action hashmap to the LB and UB
			for (int p = 0; p<_action._actionParam.size(); p++)
			{
				if (_action._actionParam.get(p).equals(_actionString))
				{
					// TODO: _contBounds should store Doubles
					lower_bound.add(new DoubleExpr(_action._contBounds.get(p*2)));
					upper_bound.add(new DoubleExpr(_action._contBounds.get((p*2)+1)));
					continue;
				}
			}
			
			
			// Now explicitly compute lower and upper bounds as XADDs
			//
			// If these are polynomials, must go to +/- infinity at limits so cannot
			// be used to approximate cdfs. Hence we must assume that there will always
			// be limits on the polynomial functions implicit in the bounds.
			int xadd_lower_bound = -1;
			if (lower_bound.isEmpty()) {
				System.err.println("No explicit lower bound given for '" + _actionString + "', using -1e6");
				System.err.println("Constraints: " + decisions);
				System.err.println("Assignments: " + decision_values);
				xadd_lower_bound = getTermNode(new DoubleExpr(-1e6));
			} else {
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
				System.err.println("No explicit upper bound given for '" + _actionString + "', using +1e6");
				System.err.println("Constraints: " + decisions);
				System.err.println("Assignments: " + decision_values);
				xadd_upper_bound = getTermNode(new DoubleExpr(1e6));
			} else {
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
			System.out.println("Lower bound:\n" + getString(xadd_lower_bound));
			System.out.println("Upper bound:\n" + getString(xadd_upper_bound));

			
			// Now compute:
			// leaf_integral{int_var = xadd_upper_bound} - leaf_integral{int_var = xadd_lower_bound}
			int int_eval_lower = substituteXADDforVarInArithExpr(leaf_val, _actionString, xadd_lower_bound);
			int int_eval_upper = substituteXADDforVarInArithExpr(leaf_val, _actionString, xadd_upper_bound);
			int int_eval = apply(int_eval_upper, int_eval_lower, MAX);

			// Finally, multiply in boolean decisions and irrelevant comparison expressions
			// to the XADD and add it to the running sum
			//
			// - HashMap<Decision,Boolean> int_var_indep_decisions
			for (Map.Entry<Decision, Boolean> me : int_var_indep_decisions.entrySet()) {
				double high_val = me.getValue() ? 1d : 0d;
				double low_val = me.getValue() ? 0d : 1d;
				int_eval = apply(int_eval, getVarNode(me.getKey(), low_val, high_val), PROD);
			}
			_runningMax = apply(_runningMax, int_eval, MAX);
			// reduceLP
			HashSet<String> all_vars = collectVars(_runningMax);
			all_vars.removeAll(_hsBooleanVars);
			//_runningMax = reduceLP(_runningMax, new ArrayList<String>(all_vars));

			// All return information is stored in _runningSum so no need to return
			// any information here... just keep diagram as is
			System.out.println("Result: " + getString(int_eval));
			return getTermNode(leaf_val);
		}
		// Assume expression is canonical, hence in sum of products form (could be a single term)
		public ArithExpr removeActionVar(ArithExpr leaf_val) {
			ArithExpr ret_expr = null;
			if (leaf_val instanceof OperExpr && ((OperExpr) leaf_val)._type == SUM) {
				ret_expr = removeActionVarMultipleTerms((OperExpr) leaf_val);
			} else if (leaf_val instanceof OperExpr && ((OperExpr) leaf_val)._type == PROD) {
				ret_expr = removeActionVarTerm((OperExpr) leaf_val);
			} else if ((leaf_val instanceof VarExpr) || (leaf_val instanceof DoubleExpr)) {
				OperExpr temp = new OperExpr(PROD, Arrays.asList(leaf_val));
				ret_expr = removeActionVarTerm(temp);
			} else {
				System.out.println("removeActionVar: Unsupported expression '" + leaf_val + "'");
				System.exit(1);
			}
			return ret_expr;
		}

		// Must be a SUM of terms to get here
		public OperExpr removeActionVarMultipleTerms(OperExpr expr) {
			if (expr._type != SUM) {
				System.out.println("removeActionVarMultipleTerms: Expected SUM, got '" + expr + "'");
				System.exit(1);
			}
			ArrayList<ArithExpr> remaining_terms = new ArrayList<ArithExpr>();
			for (ArithExpr e : expr._terms) {
				if (e instanceof OperExpr) {
					remaining_terms.add(removeActionVarTerm((OperExpr) e));
				} else if (e instanceof VarExpr) {
					OperExpr temp = new OperExpr(PROD, Arrays.asList(e));
					remaining_terms.add(removeActionVarTerm(temp));
				} else if (e instanceof DoubleExpr) {
					remaining_terms.add(e);
				} else {
					System.out.println("removeActionVarMultipleTerms: Unsupported expression '" + e + "'");
					System.exit(1);
				}
			}
			return new OperExpr(SUM, remaining_terms);
		}

		// A single term (PROD)
		public ArithExpr removeActionVarTerm(OperExpr expr) {
			if (expr._type != PROD) {
				System.out.println("removeActionVarTerm: Expected PROD, got '" + expr + "'");
				System.exit(1);
			}

			// Process all terms (optional double followed by vars)
			int _var_count = 0;
			int parameterNo =0;
			double coef = 1d;
			ArrayList<ArithExpr> factors = new ArrayList<ArithExpr>();
			for (ArithExpr e : expr._terms) {
				if (e instanceof DoubleExpr) {
					coef *= ((DoubleExpr) e)._dConstVal;
				} else if (e instanceof VarExpr) {
					// Both interned so we can test direct equality
					for (int i=0;i<_action._actionParam.size();i++)
						if (((VarExpr) e)._sVarName.equals(_action._actionParam.get(i).intern()))
						{
							_var_count++;
							parameterNo = i;
						}
					if (_var_count==0)
							factors.add(e);
				} else {
					System.out.println("removeActionVarTerm: Unsupported expression '" + e + "'");
					System.exit(1);
				}
			}

			// Check for proper form
			if (_var_count > 0) {
				if (_var_count > 1 || factors.size() > 0) {
					System.out.println("removeActionVarTerm: integration var '" + _action._actionParam.get(parameterNo)
							+ "' must appear linearly in constraint '" + expr + "'");
					System.exit(1);
				}
				// If get here only coef*_integrationVar
				_VarCoef += coef;
				return ZERO; // Just add a zero term in this place
			} else {
				factors.add(0, new DoubleExpr(coef));
				return new OperExpr(PROD, factors);
			}
		}
	}
	////////////////////////////////////////////////////
		
		
	// Given argument var v, the XADD on which this is called contains substitutions
	// for v at its leaves that are to be substituted into argument XADD xadd and returned
	// at the leaves (because these branches are the conditions under which the substitutions
	// are made).
	//
	// Need to ensure make_canonical is true in this case
	public class DeltaFunctionSubstitution extends XADDLeafOperation {
		String _subVar;
		int _xaddSubAtLeaves = -1;
		HashMap<String, ArithExpr> _leafSubs = new HashMap<String, ArithExpr>();

		public DeltaFunctionSubstitution(String subvar, int xadd_sub_at_leaves) {
			_subVar = subvar;
			_xaddSubAtLeaves = xadd_sub_at_leaves;
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

		public int processXADDLeaf(ArrayList<Decision> decisions, ArrayList<Boolean> decision_values, ArithExpr leaf_val) {

			// Return an XADD for the resulting expression
			ArithExpr ret_expr = differentiateLeaf(leaf_val);
			return getTermNode(ret_expr);
		}

		// Assume expression is canonical, hence in sum of products form (could be a single term)
		public ArithExpr differentiateLeaf(ArithExpr leaf_val) {
			ArithExpr ret_expr = null;
			if (leaf_val instanceof OperExpr && ((OperExpr) leaf_val)._type == SUM) {
				ret_expr = differentiateMultipleTerms((OperExpr) leaf_val);
			} else if (leaf_val instanceof OperExpr && ((OperExpr) leaf_val)._type == PROD) {
				ret_expr = differentiateTerm((OperExpr) leaf_val);
			} else if ((leaf_val instanceof VarExpr) || (leaf_val instanceof DoubleExpr)) {
				OperExpr temp = new OperExpr(PROD, Arrays.asList(leaf_val));
				ret_expr = differentiateTerm(temp);
			} else {
				System.out.println("differentiateLeaf: Unsupported expression '" + leaf_val + "'");
				System.exit(1);
			}
			return ret_expr;
		}

		// Must be a SUM of terms to get here
		public OperExpr differentiateMultipleTerms(OperExpr expr) {
			if (expr._type != SUM) {
				System.out.println("differentiateMultipleTerms: Expected SUM, got '" + expr + "'");
				System.exit(1);
			}
			ArrayList<ArithExpr> differentiated_terms = new ArrayList<ArithExpr>();
			for (ArithExpr e : expr._terms) {
				if (e instanceof OperExpr) {
					differentiated_terms.add(differentiateTerm((OperExpr) e));
				} else if ((e instanceof VarExpr) || (e instanceof DoubleExpr)) {
					OperExpr temp = new OperExpr(PROD, Arrays.asList(e));
					differentiated_terms.add(differentiateTerm(temp));
				} else {
					System.out.println("differentiateMultipleTerms: Unsupported expression '" + e + "'");
					System.exit(1);
				}
			}
			return new OperExpr(SUM, differentiated_terms);
		}

		// A single term (PROD)
		public ArithExpr differentiateTerm(OperExpr expr) {
			if (expr._type != PROD) {
				System.out.println("differentiateTerm: Expected PROD, got '" + expr + "'");
				System.exit(1);
			}

			// Process all terms (optional double followed by vars)
			int derivative_var_count = 0;
			int last_var_added_at = -1;
			DoubleExpr d = new DoubleExpr(1d);
			ArrayList<ArithExpr> factors = new ArrayList<ArithExpr>();
			for (ArithExpr e : expr._terms) {
				if (e instanceof DoubleExpr) {
					DoubleExpr d2 = (DoubleExpr) e;
					d = new DoubleExpr(d._dConstVal * d2._dConstVal);
				} else if (e instanceof VarExpr) {
					factors.add(e);
					// Both interned so we can test direct equality
					if (((VarExpr) e)._sVarName == _derivativeVar) {
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
	}

	public class XADDLeafIndefIntegral extends XADDLeafOperation {
		String _integrationVar;

		public XADDLeafIndefIntegral(String integration_var) {
			_integrationVar = integration_var.intern();
		}

		public int processXADDLeaf(ArrayList<Decision> decisions, ArrayList<Boolean> decision_values, ArithExpr leaf_val) {

			// Return an XADD for the resulting expression
			ArithExpr ret_expr = integrateLeaf(leaf_val);
			return getTermNode(ret_expr);
		}

		// Assume expression is canonical, hence in sum of products form (could be a single term)
		public ArithExpr integrateLeaf(ArithExpr leaf_val) {
			ArithExpr ret_expr = null;
			if (leaf_val instanceof OperExpr && ((OperExpr) leaf_val)._type == SUM) {
				ret_expr = integrateMultipleTerms((OperExpr) leaf_val);
			} else if (leaf_val instanceof OperExpr && ((OperExpr) leaf_val)._type == PROD) {
				ret_expr = integrateTerm((OperExpr) leaf_val);
			} else if ((leaf_val instanceof VarExpr) || (leaf_val instanceof DoubleExpr)) {
				OperExpr temp = new OperExpr(PROD, Arrays.asList(leaf_val));
				ret_expr = integrateTerm(temp);
			} else {
				System.out.println("processXADDLeaf: Unsupported expression '" + leaf_val + "'");
				System.exit(1);
			}
			return ret_expr;
		}

		// Must be a SUM of terms to get here
		public OperExpr integrateMultipleTerms(OperExpr expr) {
			if (expr._type != SUM) {
				System.out.println("integrateMultipleTerms: Expected SUM, got '" + expr + "'");
				System.exit(1);
			}
			ArrayList<ArithExpr> integrated_terms = new ArrayList<ArithExpr>();
			for (ArithExpr e : expr._terms) {
				if (e instanceof OperExpr) {
					integrated_terms.add(integrateTerm((OperExpr) e));
				} else if ((e instanceof VarExpr) || (e instanceof DoubleExpr)) {
					OperExpr temp = new OperExpr(PROD, Arrays.asList(e));
					integrated_terms.add(integrateTerm(temp));
				} else {
					System.out.println("integrateMultipleTerms: Unsupported expression '" + e + "'");
					System.exit(1);
				}
			}
			return new OperExpr(SUM, integrated_terms);
		}

		// A single term (PROD)
		public ArithExpr integrateTerm(OperExpr expr) {
			if (expr._type != PROD) {
				System.out.println("integrateTerm: Expected PROD, got '" + expr + "'");
				System.exit(1);
			}

			// Process all terms (optional double followed by vars)
			int integration_var_count = 0;
			DoubleExpr d = new DoubleExpr(1d);
			ArrayList<ArithExpr> factors = new ArrayList<ArithExpr>();
			for (ArithExpr e : expr._terms) {
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
					if (((VarExpr) e)._sVarName == _integrationVar)
						integration_var_count++;
				} else {
					System.out.println("integrateTerm: Unsupported expression '" + e + "'");
					System.exit(1);
				}
			}

			// Perform integration
			factors.add(new VarExpr(_integrationVar));
			d = new DoubleExpr(d._dConstVal / (double) (integration_var_count + 1));
			factors.add(0, d);

			return new OperExpr(PROD, factors);
		}
	}

	
	public int computeDefiniteIntegral(int xadd, String int_var) {
		XADDLeafDefIntegral integrator = new XADDLeafDefIntegral(int_var);
		reduceProcessXADDLeaf(xadd, integrator, /* canonical_reorder */false);
		return integrator._runningSum;
	}

	public class XADDLeafDefIntegral extends XADDLeafIndefIntegral {
		int _runningSum; // XADD for the running sum of all leaf substitutions
		double _integrationVarCoef;

		public XADDLeafDefIntegral(String integration_var) {
			super(integration_var);
			_integrationVarCoef = Double.NEGATIVE_INFINITY;

			// Start with the zero XADD
			_runningSum = getTermNode(ZERO);
		}

		public int processXADDLeaf(ArrayList<Decision> decisions, ArrayList<Boolean> decision_values, ArithExpr leaf_val) {

			// Multiply these in later
			HashMap<Decision, Boolean> int_var_indep_decisions = new HashMap<Decision, Boolean>();

			//upper and lower bounds based on the decisions
			System.out.println("=============================");
			System.out.println("Current node: " + leaf_val);
			System.out.println("Decisions to get to get here: " + decisions + " = " + decision_values + "\n===\n");
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

				_integrationVarCoef = 0d;
				ArithExpr lhs_isolated = removeIntegrationVar(comp._lhs);
				System.out.println("Pre: " + comp + " == " + is_true + ", int var [" + _integrationVar + "]"
						+ "\nLHS isolated: " + lhs_isolated + "\n ==>  " + _integrationVarCoef + " * "
						+ _integrationVar);

				if (_integrationVarCoef == 0d) {
					int_var_indep_decisions.put(d, is_true);
					continue;
				}

				// We have coef*x + expr COMP_OPER 0
				boolean flip_comparison = (_integrationVarCoef < 0d) && (comp._type != EQ) && (comp._type != NEQ);
				ArithExpr new_rhs = (ArithExpr) new OperExpr(MINUS, ZERO, new OperExpr(PROD, new DoubleExpr(
						1d / _integrationVarCoef), lhs_isolated)).makeCanonical();

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
				System.err.println("No explicit lower bound given for '" + _integrationVar + "', using -1e6");
				System.err.println("Constraints: " + decisions);
				System.err.println("Assignments: " + decision_values);
				xadd_lower_bound = getTermNode(new DoubleExpr(-1e6));
			} else {
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
				System.err.println("No explicit upper bound given for '" + _integrationVar + "', using +1e6");
				System.err.println("Constraints: " + decisions);
				System.err.println("Assignments: " + decision_values);
				xadd_upper_bound = getTermNode(new DoubleExpr(1e6));
			} else {
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
			System.out.println("Lower bound:\n" + getString(xadd_lower_bound));
			System.out.println("Upper bound:\n" + getString(xadd_upper_bound));

			// Compute the integral of this leaf w.r.t. the integration variable
			ArithExpr leaf_integral = integrateLeaf(leaf_val);
			System.out.println("Integral: " + leaf_integral);

			// Now compute:
			// leaf_integral{int_var = xadd_upper_bound} - leaf_integral{int_var = xadd_lower_bound}
			int int_eval_lower = substituteXADDforVarInArithExpr(leaf_integral, _integrationVar, xadd_lower_bound);
			int int_eval_upper = substituteXADDforVarInArithExpr(leaf_integral, _integrationVar, xadd_upper_bound);
			int int_eval = apply(int_eval_upper, int_eval_lower, MINUS);

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
			System.out.println("Result: " + getString(int_eval));
			return getTermNode(leaf_val);
		}

		// Assume expression is canonical, hence in sum of products form (could be a single term)
		public ArithExpr removeIntegrationVar(ArithExpr leaf_val) {
			ArithExpr ret_expr = null;
			if (leaf_val instanceof OperExpr && ((OperExpr) leaf_val)._type == SUM) {
				ret_expr = removeIntegrationVarMultipleTerms((OperExpr) leaf_val);
			} else if (leaf_val instanceof OperExpr && ((OperExpr) leaf_val)._type == PROD) {
				ret_expr = removeIntegrationVarTerm((OperExpr) leaf_val);
			} else if ((leaf_val instanceof VarExpr) || (leaf_val instanceof DoubleExpr)) {
				OperExpr temp = new OperExpr(PROD, Arrays.asList(leaf_val));
				ret_expr = removeIntegrationVarTerm(temp);
			} else {
				System.out.println("removeIntegrationVar: Unsupported expression '" + leaf_val + "'");
				System.exit(1);
			}
			return ret_expr;
		}

		// Must be a SUM of terms to get here
		public OperExpr removeIntegrationVarMultipleTerms(OperExpr expr) {
			if (expr._type != SUM) {
				System.out.println("removeIntegrationVarMultipleTerms: Expected SUM, got '" + expr + "'");
				System.exit(1);
			}
			ArrayList<ArithExpr> remaining_terms = new ArrayList<ArithExpr>();
			for (ArithExpr e : expr._terms) {
				if (e instanceof OperExpr) {
					remaining_terms.add(removeIntegrationVarTerm((OperExpr) e));
				} else if (e instanceof VarExpr) {
					OperExpr temp = new OperExpr(PROD, Arrays.asList(e));
					remaining_terms.add(removeIntegrationVarTerm(temp));
				} else if (e instanceof DoubleExpr) {
					remaining_terms.add(e);
				} else {
					System.out.println("removeIntegrationVarMultipleTerms: Unsupported expression '" + e + "'");
					System.exit(1);
				}
			}
			return new OperExpr(SUM, remaining_terms);
		}

		// A single term (PROD)
		public ArithExpr removeIntegrationVarTerm(OperExpr expr) {
			if (expr._type != PROD) {
				System.out.println("removeIntegrationVarTerm: Expected PROD, got '" + expr + "'");
				System.exit(1);
			}

			// Process all terms (optional double followed by vars)
			int integration_var_count = 0;
			double coef = 1d;
			ArrayList<ArithExpr> factors = new ArrayList<ArithExpr>();
			for (ArithExpr e : expr._terms) {
				if (e instanceof DoubleExpr) {
					coef *= ((DoubleExpr) e)._dConstVal;
				} else if (e instanceof VarExpr) {
					// Both interned so we can test direct equality
					if (((VarExpr) e)._sVarName == _integrationVar)
						integration_var_count++;
					else
						factors.add(e);
				} else {
					System.out.println("removeIntegrationVarTerm: Unsupported expression '" + e + "'");
					System.exit(1);
				}
			}

			// Check for proper form
			if (integration_var_count > 0) {
				if (integration_var_count > 1 || factors.size() > 0) {
					System.out.println("removeIntegrationVarTerm: integration var '" + _integrationVar
							+ "' must appear linearly in constraint '" + expr + "'");
					System.exit(1);
				}
				// If get here only coef*_integrationVar
				_integrationVarCoef += coef;
				return ZERO; // Just add a zero term in this place
			} else {
				factors.add(0, new DoubleExpr(coef));
				return new OperExpr(PROD, factors);
			}
		}
	}

	// Note: use canonical_reorder=false for integration since internal nodes *cannot* get
	// out of order, but use canonical_reorder=true for substitution
	public int reduceProcessXADDLeaf(int id, XADDLeafOperation leaf_sub, boolean canonical_reorder) {
		int ret_node = reduceProcessXADDLeaf(id, leaf_sub, new ArrayList<Decision>(), new ArrayList<Boolean>());
		if (canonical_reorder)
			return makeCanonical(ret_node);
		else
			return ret_node;
	}

	public IntPair _tempReduceLeafOpKey = new IntPair(-1, -1);

	private int reduceProcessXADDLeaf(int id, XADDLeafOperation leaf_op, ArrayList<Decision> decisions,
			ArrayList<Boolean> decision_values) {

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
																								// a node id means
																								// canonical
		}

		// If its an internal node, check the reduce cache
		_tempReduceLeafOpKey.set(id, leaf_op.hashCode());
		if ((ret = _hmReduceLeafOpCache.get(_tempReduceLeafOpKey)) != null) {
			// System.out.println("In cache, returning: " + qret);
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
		_hmReduceLeafOpCache.put(new IntPair(id, leaf_op.hashCode()), ret);
		return ret;
	}

	// ///////////

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
		// TODO: should maintain a reusable reduce cache here
		return makeCanonical(ret);
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

		if ((n1 instanceof XADDTNode) && (n2 instanceof XADDTNode)) {
			XADDTNode xa1 = (XADDTNode) n1;
			XADDTNode xa2 = (XADDTNode) n2;

			if ((op != MAX) && (op != MIN))
				return getTermNode(new OperExpr(op, xa1._expr, xa2._expr));

			ArithExpr diff = new OperExpr(MINUS, xa1._expr, xa2._expr);
			CompExpr comp = null;

			if (USE_MINUS_COMP)
				comp = new CompExpr(LT, diff, new DoubleExpr(0d));
			else
				comp = new CompExpr(LT_EQ, xa1._expr, xa2._expr);
			Decision d = new ExprDec(comp);

			// Do a range check here
			// if (CHECK_MIN_MAX) {
			// TautDec tdec = checkRangeImpliedDecision(d);
			// if (tdec != null)
			// d = tdec; // Change to a tautology... getInode will handle this
			// }

			int var_index = getVarIndex(d, true);
			// System.out.println("ComputeTerm::max var_index " + _alOrder.get(var_index) + ": " + comp);
			// System.exit(1);
			int node1 = getTermNode(xa1._expr);
			int node2 = getTermNode(xa2._expr);
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

	// // ////////////////////////////////////////////////////////////////
	// // Flushing and special node maintenance
	// // ////////////////////////////////////////////////////////////////
	//
	// // Designate/remove/clear nodes to persist through flushing
	// public void addSpecialNode(int n) {
	// _hsSpecialNodes.add(new ADDRNode(n));
	// }
	//
	// public void removeSpecialNode(int n) {
	// _hsSpecialNodes.remove(new ADDRNode(n));
	// }
	//
	// public void clearSpecialNodes() {
	// _hsSpecialNodes.clear();
	// }
	//
	// // Flush caches but save special nodes
	// public void flushCaches(boolean print_info) {
	//
	// // Print starting info
	// if (print_info) {
	// System.out.print("[FLUSHING CACHES... ");
	// // showCacheSize();
	// ResetTimer();
	// }
	//
	// // Can always clear these
	// _hmPairs = new HashMap();
	// _hmReduceMap = new HashMap();
	// _hmPruneMap = new HashMap();
	//
	// // Set up temporary alternates to these HashMaps
	// _hmNewADDNodes = new HashMap();
	// _hmNewINodeCache = new HashMap();
	// _hmNewDNodeCache = new HashMap();
	//
	// // Copy over 'special' nodes then set new maps
	// Iterator i = _hsSpecialNodes.iterator();
	// while (i.hasNext()) {
	// cacheNode((ADDRNode) i.next());
	// }
	// _hmADDNodes = _hmNewADDNodes;
	// _hmINodeCache = _hmNewINodeCache;
	// _hmDNodeCache = _hmNewDNodeCache;
	//
	// // Print results
	// if (GC_DURING_FLUSH) {
	// RUNTIME.gc();
	// }
	// if (print_info) {
	// System.out.print(" TIME: " + GetElapsedTime());
	// System.out
	// .print("  RESULT: "
	// + _df
	// .format(((double) RUNTIME.freeMemory() / (double) RUNTIME
	// .totalMemory())));
	// System.out.print("  CACHE: " + getCacheSize() + "] ");
	// }
	// }
	//
	// // ////////////////////////////////////////////////////////////////
	// // Internal data structure maintenance
	// // ////////////////////////////////////////////////////////////////
	//
	// // Quick cache snapshot
	// public void showCacheSize() {
	// System.out.println("APPLY CACHE:  " + _hmPairs.size());
	// System.out.println("REDUCE CACHE: " + _hmReduceMap.size());
	// System.out.println("INODE CACHE:  " + _hmINodeCache.size() + "\n");
	// }
	//
	// // Total cache snapshot
	// public long getCacheSize() {
	// return _hmPairs.size() + _hmReduceMap.size() + _hmINodeCache.size();
	// }
	//
	// // An exact count for the ADD rooted at _nRoot
	// public long countExactNodes(int id) {
	// HashSet cset = new HashSet();
	// countExactNodesInt(cset, id);
	// return cset.size();
	// }
	//
	// public Set getExactNodes(int id) {
	// HashSet cset = new HashSet();
	// countExactNodesInt(cset, id);
	// return cset;
	// }
	//
	// public void countExactNodesInt(HashSet cset, int id) {
	// if (cset.contains(new Integer(id))) {
	// return;
	// }
	// ADDNode n = getNode(id);
	// // Uncomment the following to get internal-only count
	// // if (n instanceof ADDDNode) {
	// // return;
	// // }
	// cset.add(new Integer(id));
	// if (n instanceof ADDINode) {
	// countExactNodesInt(cset, ((ADDINode) n)._nLow);
	// countExactNodesInt(cset, ((ADDINode) n)._nHigh);
	// }
	// }
	//
	// public Set getVarIDs(int id) {
	// HashSet cset = new HashSet();
	// HashSet gset = new HashSet();
	// collectGIDsInt(cset, gset, id);
	// return gset;
	// }
	//
	// public void collectGIDsInt(HashSet cset, HashSet gset, int id) {
	// if (cset.contains(new Integer(id))) {
	// return;
	// }
	// cset.add(new Integer(id));
	// ADDNode n = getNode(id);
	// if (n instanceof ADDINode) {
	// gset.add(new Integer(((ADDINode) n)._nGlobalID));
	// collectGIDsInt(cset, gset, ((ADDINode) n)._nLow);
	// collectGIDsInt(cset, gset, ((ADDINode) n)._nHigh);
	// }
	// }
	//
	// // ///////////////////////////////////////////////////////////////
	// // Order maintenance
	// // ///////////////////////////////////////////////////////////////
	//
	// // Probably have more efficient ways to do a lot of these using
	// // binary search and hash tables
	//
	// // Order check - both must occur in list!
	// public boolean comesBefore(int gid1, int gid2) {
	// // Get level for gid1 and gid2
	// int l1 = ((Integer) _hmGVarToLevel.get(new Integer(gid1))).intValue();
	// int l2 = ((Integer) _hmGVarToLevel.get(new Integer(gid2))).intValue();
	//
	// // Determine which comes first (i.e. earlier level)
	// return (l1 <= l2);
	// }
	//
	//
	// // //////////////////////////////////////////////////////////////
	// // Miscellaneous methods
	// // //////////////////////////////////////////////////////////////
	//
	// public String toString() {
	//
	// StringBuffer sb = new StringBuffer();
	//
	// // Show order
	// sb.append("Var order: " + _alOrder + "\n");
	// // sb.append("Val range: " + procList(getValueRange()) + "\n");
	//
	// // Recurse from the root and show each branch
	// sb.append("Structure:\n" + getNode(_nRoot).toString(this, 0) + "\n");
	//
	// return sb.toString();
	// }

	// //////////////////////////////////////////////////////////////
	// Inner Classes
	// //////////////////////////////////////////////////////////////

	// XADD has to map integers to decisions, probably in an array
	// since lookup will only ever be linear to match BoolDec
	// and ExprDec's... all decision tests will be with integers,
	// IDs only accessed for adding new decisions, printing,
	// evaluation, and when maximizing to build new decisions.
	//
	// Need ability to substitute VarExpr with more complex
	// ArithExpr's during regression.
	//
	// Requires equals() for ExprDec & BoolDec
	//
	//

	// //////////////////////////////////////////////////////////////

	public void clearMarks() {
		_markCounter++;
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

		public XADDTNode(ArithExpr e) {
			if (e instanceof OperExpr && ((OperExpr) e)._terms.size() == 1)
				_expr = ((OperExpr) e)._terms.get(0);
			else
				_expr = e;
		}

		public void set(ArithExpr e) {
			if (e instanceof OperExpr && ((OperExpr) e)._terms.size() == 1)
				_expr = ((OperExpr) e)._terms.get(0);
			else
				_expr = e;
		}

		public int hashCode() {
			return _expr.hashCode();
		}

		public boolean equals(Object o) {
			if (o instanceof XADDTNode)
				return this._expr.equals(((XADDTNode) o)._expr);
			else
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
			g.addNodeLabel(this_node, _expr.toString()/* +" id:"+id */);
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

		@Override
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

		@Override
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

		@Override
		public String toString(boolean format) {
			return Boolean.toString(_bTautology);
		}
	}

	public class BoolDec extends Decision {
		public String _sVarName = null;

		public BoolDec(String var_name) {
			_sVarName = var_name.intern();
			_hsBooleanVars.add(_sVarName);
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

		@Override
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
				CompExpr new_comp = (CompExpr) e._expr.makeCanonical();
				d = new ExprDec(new_comp);
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
				// else {
				//
				// // Invert the decision and check for implication...
				// // curiously note that this testing is asymmetrical due
				// // to equality and hence the above might not catch half
				// // of the implications:
				// // a \in [-10,0]
				// // if (a < 0) x else y
				// // becomes if (0 >= a)
				// // null true
				// ExprDec d2 = new ExprDec(new CompExpr(invertType(d1._expr._type),
				// d1._expr._rhs, d1._expr._lhs));
				//
				// tdec = checkRangeImpliedDecision(d2);
				// if (tdec != null) {
				// // We inverted the decision, so now we have to invert
				// d = new TautDec(!tdec._bTautology);
				// }
				// }
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

		public static ArithExpr Convert2OperExpr(FOPC.TFunction t) {
			// System.out.println("Convert2OperExpr: [" + t._nArity + "] "
			// + t.toFOLString());
			if (t._nArity == 0) {
				return new VarExpr(t._sFunName);
			}
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

	// /////////////flush/////////////////////
	public void clearSpecialNodes() {
		_hsSpecialNodes.clear();
	}

	public void addSpecialNode(Integer n) {
		try {
			if (n == null)
				throw new Exception("addSpecialNode: null");
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
			System.exit(1);
		}
		_hsSpecialNodes.add(n);
	}

	public void flushCaches() {
		System.out.print("[FLUSHING CACHES... ");

		// Can always clear these
		_hmReduceCache.clear();
		_hmReduceLPCache.clear();
		_hmReduceLeafOpCache.clear();
		_hmApplyCache.clear();
		_hmINode2Vars.clear();

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
		Object node = _hmInt2Node.get(id);
		if (node instanceof XADDINode) {
			Integer fh = ((XADDINode) node)._high;
			Integer fl = ((XADDINode) node)._low;
			_hmInt2NodeNew.put(id, (XADDINode) node);
			_hmNode2IntNew.put((XADDINode) node, id);
			copyInNewCacheNode(((XADDINode) node)._high);
			copyInNewCacheNode(((XADDINode) node)._low);
		} else if (node instanceof XADDTNode) {
			_hmInt2NodeNew.put(id, (XADDTNode) node);
			_hmNode2IntNew.put((XADDTNode) node, id);

		}
	}

	// //////////////////////////////////////////////////////////////

	public static void main(String[] args) throws Exception {

		// TestPolyOps();
		// if (0 <= 1) return;

		System.out.println(Double.MAX_VALUE + " , " + (-Double.MAX_VALUE));
		/*
		 * TestParse("[a]");
		 */
		/*
		 * TestParse("[a + b + 3 + 4]"); TestParse("[a + b + 3 + 4 >= 3 / 7]"); TestParse("[a + b + 3 + -4 * y = 9]");
		 * TestParse("[((a + b) * (3 * 4)]"); // Mismatched parens TestParse("[(a + b) * (3 * 4)]");
		 * 
		 * // Build and display an XADD
		 */
		XADD xadd_context = new XADD();

		// Put all boolean variables first to avoid reordering clashes
		xadd_context.getVarIndex(xadd_context.new BoolDec("f"), true);
		xadd_context.getVarIndex(xadd_context.new BoolDec("g"), true);
		xadd_context.getVarIndex(xadd_context.new BoolDec("h"), true);

		int xadd_circle = TestBuild(xadd_context, "./src/xadd/circle.xadd");
		Graph gc = xadd_context.getGraph(xadd_circle); gc.launchViewer();
		//System.in.read();
		
		// Collect all vars
		HashSet<String> vars = xadd_context.collectVars(xadd_circle);
		System.out.println("Vars in circle.xadd: " + vars);

		// Test out indefinite integration
		// int int1_xac = xadd_context.reduceProcessXADDLeaf(xadd_circle,
		// xadd_context.new XADDLeafIndefIntegral("x1"), /*canonical_reorder*/false);
		// xadd_context.getGraph(int1_xac).launchViewer();
		//
		// int int2_xac = xadd_context.reduceProcessXADDLeaf(int1_xac,
		// xadd_context.new XADDLeafIndefIntegral("x2"), /*canonical_reorder*/false);
		// xadd_context.getGraph(int2_xac).launchViewer();

		int xadd1 = TestBuild(xadd_context, "./src/xadd/test1.xadd");
		int xadd2 = TestBuild(xadd_context, "./src/xadd/test2.xadd");

		int xadd3 = TestBuild(xadd_context, "./src/xadd/test3.xadd");

		int x1_d = TestBuild(xadd_context, "./src/xadd/test4.xadd");
		int d = TestBuild(xadd_context, "./src/xadd/test5.xadd");
		HashMap<String, ArithExpr> hm = new HashMap<String, ArithExpr>();
		hm.put("x1", new XADD.DoubleExpr(5d));
		int d2 = xadd_context.substitute(x1_d, hm);
		System.out.println(xadd_context.getString(d));
		System.out.println(xadd_context.getString(d2));
		int prod = xadd_context.apply(d, d2, PROD);
		System.out.println(xadd_context.getString(prod));

		HashMap<String, Double> hm_eval = new HashMap<String, Double>();
		hm_eval.put("d", 1d);
		System.out.println(xadd_context.evaluate(prod, new HashMap<String, Boolean>(), hm_eval));

		int dd_norm = xadd_context.computeDefiniteIntegral(prod, "d");
		XADDTNode t = (XADDTNode) xadd_context.getNode(dd_norm);
		double norm_const = ((DoubleExpr) t._expr)._dConstVal;
		int recip_dd_norm = xadd_context.getTermNode(new DoubleExpr(1d / norm_const));
		int final_result = xadd_context.apply(prod, recip_dd_norm, PROD);
		System.out.println(xadd_context.getString(final_result));

		dd_norm = xadd_context.computeDefiniteIntegral(final_result, "d");
		t = (XADDTNode) xadd_context.getNode(dd_norm);
		norm_const = ((DoubleExpr) t._expr)._dConstVal;
		System.out.println("Final integrated value = " + norm_const);

		xadd_context.computeDefiniteIntegral(xadd3, "x1");

		// Derivative test
		// int der1_xac = xadd_context.reduceProcessXADDLeaf(xadd1,
		// xadd_context.new XADDLeafDerivative("x1"), /*canonical_reorder*/false);
		// xadd_context.getGraph(der1_xac).launchViewer();
		//
		// int der2_xac = xadd_context.reduceProcessXADDLeaf(xadd2,
		// xadd_context.new XADDLeafDerivative("x1"), /*canonical_reorder*/false);
		// xadd_context.getGraph(der2_xac).launchViewer();

		// Elim Example 1: Definite integral over *continuous var* for polynomial leaf case statement
		int int1_xad = xadd_context.computeDefiniteIntegral(xadd1, "x1");
		xadd_context.getGraph(int1_xad).launchViewer();

		int int2_xad = xadd_context.computeDefiniteIntegral(xadd2, "x2");
		xadd_context.getGraph(int2_xad).launchViewer();

		// Elim Example 2: Marginalizing out a *boolean var* (in this case f)
		// (make sure all boolean diagrams are "completed" to include
		// branches for f=true and f=false)
		int restrict_high = xadd_context.opOut(xadd1, xadd_context.getBoolVarIndex("f"), RESTRICT_HIGH);
		int restrict_low = xadd_context.opOut(xadd1, xadd_context.getBoolVarIndex("f"), RESTRICT_LOW);
		int marginal = xadd_context.apply(restrict_high, restrict_low, SUM);
		xadd_context.getGraph(marginal).launchViewer();

		// Note: if you can guarantee every leaf is reached by a true or false path through
		// the variable being summed out, you can use the shortcut operation
		// int marginal = xadd_context.opOut(xadd1, xadd_context.getBoolVarIndex("f"), SUM);

		// Elim Example 3: Integrating out x for [z * \delta(x - y)] for XADDs y and z
		// Note: it is assumed that z contains references to x that will be substituted according to y,
		// y should *not* contain the variable x
		//
		// Here y = xadd_circle(x1,x2,r1,r2), x = k, z = xadd1(k,x1,f)
		xadd1 = TestBuild(xadd_context, "./src/xadd/test1.xadd");

		int y = xadd_context.reduceProcessXADDLeaf(xadd_circle, xadd_context.new DeltaFunctionSubstitution("k", xadd1), /* canonical_reorder */
		true);
		xadd_context.getGraph(y).launchViewer();
		System.out.println(xadd_context.getString(xadd_circle));
		System.out.println("-------------\n" + xadd_context.getString(y));

		System.exit(1);
		xadd_context.getGraph(y).launchViewer();
		System.out.println(xadd_context.getString(xadd_circle));

		// Pause
		System.in.read();

		// *****************TESTING MAX***********
		int xadd4 = TestBuild(xadd_context, "./src/xadd/test4.xadd");
		int xadd5 = TestBuild(xadd_context, "./src/xadd/test5.xadd");
		int xaddrRes = xadd_context.apply(xadd4, xadd5, XADD.MAX);
		Graph gRes = xadd_context.getGraph(xadd_context.reduceLP(xaddrRes, Arrays.asList("x1", "x2", "r1")));
		gRes.launchViewer();

		int xadd_implied = TestBuild(xadd_context, "./src/xadd/implied.xadd");
		Graph gb = xadd_context.getGraph(xadd_implied);
		gb.launchViewer();
		xadd_implied = xadd_context.reduceLP(xadd_implied, Arrays.asList("x1", "x2", "r1"));
		Graph gb2 = xadd_context.getGraph(xadd_implied);
		gb2.launchViewer();
		if (true)
			return;

		// **************************************
		System.out.println(">> PROD Operations");
		int xaddr1 = xadd_context.apply(xadd1, xadd2, XADD.PROD);
		Graph g1 = xadd_context.getGraph(xaddr1);
		g1.launchViewer();
		int xaddr2 = xadd_context.opOut(xaddr1, 2, PROD);
		Graph g2 = xadd_context.getGraph(xaddr2);
		g2.launchViewer();
		System.out.println(">> MAX Operation");
		int xaddr3 = xadd_context.apply(xadd1, xadd2, XADD.MAX);
		Graph g3 = xadd_context.getGraph(xaddr3);
		g3.launchViewer();

		System.out.println(">> Substitutions");
		HashMap<String, ArithExpr> subst = new HashMap<String, ArithExpr>();
		subst.put("k", new DoubleExpr(10d));
		int xaddr4 = xadd_context.substitute(xaddr3, subst);
		Graph g4 = xadd_context.getGraph(xaddr4);
		g4.launchViewer();
		subst.put("x1", new DoubleExpr(0d/* 5d */));
		int xaddr5 = xadd_context.substitute(xaddr3, subst);
		Graph g5 = xadd_context.getGraph(xaddr5);
		g5.launchViewer();
		System.out.println("Vars: " + xadd_context._alOrder);

		HashMap<String, Boolean> bool_assign = new HashMap<String, Boolean>();
		bool_assign.put("f", true);
		bool_assign.put("g", true); // if h instead, eval will be null

		HashMap<String, Double> cont_assign = new HashMap<String, Double>();
		cont_assign.put("k", 0d);
		cont_assign.put("x1", -5d);

		System.out.println(">> Evaluations");
		System.out.println("1 Eval: [" + bool_assign + "], [" + cont_assign + "]" + ": "
				+ xadd_context.evaluate(xaddr3, bool_assign, cont_assign));
		cont_assign.put("x1", 10d);
		System.out.println("2 Eval: [" + bool_assign + "], [" + cont_assign + "]" + ": "
				+ xadd_context.evaluate(xaddr3, bool_assign, cont_assign));
		cont_assign.put("x2", 7d);
		System.out.println("3 Eval: [" + bool_assign + "], [" + cont_assign + "]" + ": "
				+ xadd_context.evaluate(xaddr3, bool_assign, cont_assign));
	}

	public static void TestParse(String s) {
		s = s.substring(1, s.length() - 1);
		CompExpr e = CompExpr.parse(s);
		System.out.println("CompExpr for  '" + s + "': " + e);
		ArithExpr a = ArithExpr.parse(s);
		System.out.println("ArithExpr for '" + s + "': " + a + "\n");
		VarExpr sub = new VarExpr("a");
		ArithExpr a2 = ArithExpr.parse("[c + a]");
		HashMap<String, ArithExpr> subst = new HashMap<String, ArithExpr>();
		subst.put("a", a2);
		if (a != null) {
			System.out.println("EX + EX = " + ArithExpr.op(a, a, SUM));
			System.out.println("EX - EX = " + ArithExpr.op(a, a, MINUS));
			System.out.println("EX * EX = " + ArithExpr.op(a, a, PROD));
			System.out.println("EX / EX = " + ArithExpr.op(a, a, DIV));
			System.out.println("EX == EX: " + ArithExpr.parse(s).equals(a));
			System.out.println("EX != EX * EX: " + ArithExpr.op(a, a, PROD).equals(a));
			System.out.println("EX+EX:" + sub + "/" + a2 + ": " + ArithExpr.op(a, a, SUM).substitute(subst));
		} else if (e != null) {
			System.out.println("EX == EX: " + CompExpr.parse(s).equals(e));
			System.out.println("EX:" + sub + "/" + a2 + ": " + e.substitute(subst));
		}
	}

	public static int TestBuild(XADD xadd_context, String filename) {
		ArrayList l = HierarchicalParser.ParseFile(filename);
		System.out.println("Parsed file contents for '" + filename + "': " + l.get(0) + "\n");
		int dd1 = xadd_context.buildCanonicalXADD((ArrayList) l.get(0));
		Graph g1 = xadd_context.getGraph(dd1);
		g1.launchViewer();
		// int dd2 = xadd_context.apply(dd1, dd1, XADD.SUM);
		// Graph g2 = xadd_context.getGraph(dd2); g2.launchViewer();
		// int dd3 = xadd_context.apply(dd1, dd1, XADD.PROD);
		// Graph g3 = xadd_context.getGraph(dd3); g3.launchViewer();
		return dd1;
	}

	public static void TestPolyOps() {
		XADD xadd_context = new XADD();
		int xadd1 = TestBuild(xadd_context, "src/xadd/test3.xadd");
		int xaddr1 = xadd_context.apply(xadd1, xadd1, XADD.SUM);
		xadd_context.getGraph(xaddr1).launchViewer();
		int xaddr2 = xadd_context.apply(xadd1, xadd1, XADD.MINUS);
		xadd_context.getGraph(xaddr2).launchViewer();
		int xaddr3 = xadd_context.apply(xadd1, xadd1, XADD.PROD);
		xadd_context.getGraph(xaddr3).launchViewer();
		int xaddr4 = xadd_context.apply(xaddr3, xaddr3, XADD.PROD);
		xadd_context.getGraph(xaddr4).launchViewer();

	}

	
}
