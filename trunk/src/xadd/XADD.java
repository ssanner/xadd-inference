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
import util.IntTriple;

import java.text.*;
import java.util.*;

import cmdp.HierarchicalParser;

import logic.kb.fol.FOPC;

/**
 * General class for implementation of ADD data structure
 **/
public class XADD  {
	
	// Visualization
	public final static boolean GRAPH_USE_COLOR = true;

	// Flags
	public final static boolean CHECK_MIN_MAX  = true;  // Will be buggy if min/max of expr 
														// not at extrema of domain
	public final static boolean USE_MINUS_COMP = false; // Error, all max/min comps reduce to false!
	public final static boolean SIMPLIFY_MINUS = false; // Not working, remove both sides
	
	// Operators
	public final static int UND   = 0;
	public final static int SUM   = 1;
	public final static int MINUS = 2;
	public final static int PROD  = 3;
	public final static int DIV   = 4;
	public final static int MAX   = 5;
	public final static int MIN   = 6;
	public final static int RESTRICT_LOW  = 7;
	public final static int RESTRICT_HIGH = 8;
	public final static int EQ    = 9;
	public final static int NEQ   = 10;
	public final static int GT    = 11;
	public final static int GT_EQ = 12;
	public final static int LT    = 13;
	public final static int LT_EQ = 14;
	public final static int ERROR = 15;
	public final static String[] _aOpNames = {/*0*/ "UND", 
		  /*1*/ "+", "-", "*", "/", "max", "min", "|l", "|h",
		  /*9*/ "=", "!=", ">", ">=", "<", "<=", "ERROR"};

	// Constants	
	public final static String STRING_TAB = "   ";
	public final static DecimalFormat _df = new DecimalFormat("#.##");
	
	public final static ArithExpr ZERO = new DoubleExpr(0d); 
	public final static ArithExpr ONE  = new DoubleExpr(1d);
	
	// Variable Maintenance
	public ArrayList<Decision> _alOrder = new ArrayList<Decision>();
	
	// Node Maintenance
	public int _markCounter = 1;
	public int _nodeCounter = 1;
	public HashMap<XADDNode,Integer> _hmNode2Int = new HashMap<XADDNode,Integer>();
	public HashMap<Integer,XADDNode> _hmInt2Node = new HashMap<Integer,XADDNode>();
	
	// Reduce & Apply Caches
	public HashMap<IntTriple,Integer> _hmReduceCache = new HashMap<IntTriple,Integer>();
	public HashMap<IntTriple,Integer> _hmApplyCache  = new HashMap<IntTriple,Integer>();
	
	public HashMap<String,Double> _hmMinVal = new HashMap<String,Double>();
	public HashMap<String,Double> _hmMaxVal = new HashMap<String,Double>();
	
	// Methods
	public XADD() {
		
	}
	
	public int getVarIndex(Decision d) {
		return getVarIndex(d, false);
	}

	public int getVarIndex(Decision d, boolean create) {
		
		// Check for tautology (or inconsistency)
		if (d instanceof ExprDec) {
			ExprDec e = (ExprDec)d;
			if (   e._expr._lhs instanceof DoubleExpr
				&& e._expr._rhs instanceof DoubleExpr) {
				// Directly evaluate the inequality
				double dval_lhs = ((DoubleExpr)e._expr._lhs)._dConstVal;
				double dval_rhs = ((DoubleExpr)e._expr._rhs)._dConstVal;
				TautDec tdec = null;
				switch (e._expr._type) {
					case EQ:    tdec = new TautDec(dval_lhs == dval_rhs); break;
					case NEQ:   tdec = new TautDec(dval_lhs != dval_rhs); break;
					case GT:    tdec = new TautDec(dval_lhs >  dval_rhs); break;
					case GT_EQ: tdec = new TautDec(dval_lhs >= dval_rhs); break;
					case LT:    tdec = new TautDec(dval_lhs <  dval_rhs); break;
					case LT_EQ: tdec = new TautDec(dval_lhs <= dval_rhs); break;
				}							
				if (tdec != null)
					d = tdec;
			} else if (CHECK_MIN_MAX) {
				// Check for evaluations based on minimum and maximum values
				TautDec tdec = null;
				Double lhs_max = e._expr._lhs.evaluate(_hmMaxVal);
				Double lhs_min = e._expr._lhs.evaluate(_hmMinVal);
				Double rhs_max = e._expr._rhs.evaluate(_hmMaxVal);
				Double rhs_min = e._expr._rhs.evaluate(_hmMinVal);
				if (lhs_max == null)
					lhs_max = Double.MAX_VALUE;
				if (rhs_max == null)
					rhs_max = Double.MAX_VALUE;
				if (lhs_min == null)
					lhs_min = -Double.MAX_VALUE;
				if (rhs_min == null)
					rhs_min = -Double.MAX_VALUE;
				switch (e._expr._type) {
					case EQ:    
					case NEQ:   
						if ((lhs_min > rhs_max || rhs_min > lhs_max)
							|| ((lhs_max == lhs_min) && (rhs_max == rhs_min) && (lhs_min != rhs_min))) {
							// Indicates they cannot possibly be equal
							tdec = new TautDec((e._expr._type == EQ) ? false : true);
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
				if (tdec != null)
					d = tdec;
			}
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
	
	public XADDNode getNode(int node_id) {
		return _hmInt2Node.get(node_id);
	}
	
	public XADDINode _tempINode = new XADDINode(-1, -1, -1);
	public int getINode(int var, int low, int high) {
	
		// First check if low == high... in this case, just perform the
		// obvious equivalent reduction (this saves headaches later)
		if (low == high) {
			return low;
		}
		
		// Handle tautological variables
		Decision d = _alOrder.get(var);
		if (d instanceof TautDec) {
			if (((TautDec)d)._bTautology)
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
	
	////////////////////////////////////////////////////////////////
	
	// TODO: This DD can still be out of order if parse tree had out-of-order
	//       variables.
	public int buildCanonicalXADD(ArrayList l) {
		return reduce(buildNonCanonicalXADD(l));
	}

	public int buildNonCanonicalXADD(ArrayList l) {
		if (l.size() == 1) {
			// Terminal node
			String s = (String)l.get(0);
			// Work around a parser issue with singleton vars in brackets
			if (s.startsWith("["))
				s = s.substring(1,s.length()-1);
			ArithExpr a = ArithExpr.parse(s);
			if (a == null) {
				//System.out.println("Could not buildNonCanonicalXADD for terminal '" + (String)l.get(0) + "'");
				return -1;
			} else
				return getTermNode(a);
		} else if (l.size() == 3) {
			// Internal node
			String expr = ((String)l.get(0));
			Decision d = null;
			//System.out.println("Expr: " + expr);
			if (expr.startsWith("[")) {
				CompExpr c = CompExpr.parse(expr);
				if (c != null) {
					//System.out.println("CompExpr: " + c);
					d = new ExprDec(c);
				}
			} else {
				d = new BoolDec(expr);
			}
			
			if (d == null) {
				System.out.println("Could not buildNonCanonicalXADD for terminal '" + l + "'");
				return -1;
			} else {
				//System.out.println("Var expr: " + d);
				int index = getVarIndex(d, true);
				int high  = buildNonCanonicalXADD((ArrayList)l.get(1));
				int low   = buildNonCanonicalXADD((ArrayList)l.get(2));
				//System.out.println("New inode: " + index + " - " + low + ", " + high);
				//System.out.println("Var: " + _alOrder.get(index));
				return getINode(index, low, high);
			}
		} else {
			// Unknown
			System.out.println("Could not buildNonCanonicalXADD for " + 
							   l.size() + " args '" + l + "'");
			return -1;
		}
	}

	//////////////////////////////////////////////////////////////////////
	
	public Graph getGraph(int id) {
		Graph g = new Graph(true /* directed */, false /* bottom-to-top */, false /* left-to-right */,
							true /* multi-links */);
		XADDNode root = _hmInt2Node.get(id);
		root.toGraph(g, id);
		return g;
	}

	public String getString(int id) {
		XADDNode root = _hmInt2Node.get(id);
		return root.toString();
	}

	//////////////////////////////////////////////////////////////////////

	// TODO: Implement garbage collection with my counter increment trick
	//       and marking.  
		
	public IntTriple _tempReduceKey = new IntTriple(-1,-1,-1);
	
	public int reduce(int node) {
		return reduceOp(node, -1, -1);
	}
	
	public int opOut(int node_id, int var_id, int op) {
		return reduceOp(node_id, var_id, op);
	}
	
	public int reduceOp(int node_id, int var_id, int op) {

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

		XADDINode inode = (XADDINode)n;
		
		int low = -1;
		int high = -1;
		if (op != RESTRICT_HIGH) {
			low = reduceOp(inode._low, var_id, op);
		}
		if (op != RESTRICT_LOW) {
			high = reduceOp(inode._high, var_id, op);
		}
		
		if (op != -1 && var_id != -1 && var_id == inode._var) {
			if (op == RESTRICT_LOW) {
				ret = low;
			} else if (op == RESTRICT_HIGH) {
				ret = high;
			} else if (op == SUM || op == PROD) { // op \in {MINUS, DIV} not commutative
				                                  // not obvious if low or high comes first
				ret = apply(low, high, op);
			} else {
				System.out.println("ERROR: id:" + op + "/ name:" + _aOpNames[op] + 
						" expected in node cache, but not found!");
				new Exception().printStackTrace();
				System.exit(1);		
			}
		} else {
			// GetInode will handle the case of low == high
			ret = getINode(inode._var, low, high);
		}
		
		// Put return value in cache and return
		_hmReduceCache.put(new IntTriple(node_id, var_id, op), ret);
		return ret;
	}

	// Not worrying about reusing cache across class
	// TODO: substitution has to enforce order if it is violated
	public int substitute(int node_id, HashMap<String,ArithExpr> subst) {
		return reduceSub(node_id, subst, new HashMap<Integer,Integer>());
	}
	
	// TODO: substitution has to enforce order if it is violated
	//       could possibly do with a pairwise reordering...
	public int reduceSub(int node_id, HashMap<String,ArithExpr> subst, 
						 HashMap<Integer,Integer> subst_cache) {

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
			ArithExpr expr = ((XADDTNode)n)._expr;
			expr = expr.substitute(subst);
			return getTermNode(expr);
		}
		
		// If its an internal node, check the reduce cache
		if ((ret = subst_cache.get(node_id)) != null) {
			// System.out.println("In cache, returning: " + qret);
			return ret;
		}

		// Handle an internal node
		XADDINode inode = (XADDINode)n;
		
		int low = reduceSub(inode._low, subst, subst_cache);
		int high = reduceSub(inode._high, subst, subst_cache);
		
		int var  = inode._var;
		Decision d = _alOrder.get(var);
		if (d instanceof ExprDec) {
			// Only need to substitute if a var expression
			CompExpr comp = ((ExprDec)d)._expr;
			comp = comp.substitute(subst);
			d = new ExprDec(comp);
			var = getVarIndex(d, true);
		}
					
		// Check ordering
		ret = getINode(var, low, high);
		XADDNode low_n  = _hmInt2Node.get(low);
		if (low_n instanceof XADDINode) {
			XADDINode low_ni = (XADDINode)low_n;
			if (var > low_ni._var) {
				System.out.println("Reordering problem: " + var + " > " + low_ni._var);
				System.out.println(_alOrder.get(var));
				System.out.println(_alOrder.get(low_ni._var));
				new Exception().printStackTrace(System.out);
				//System.exit(1);
			}
		}
		XADDNode high_n = _hmInt2Node.get(high);
		if (high_n instanceof XADDINode) {
			XADDINode high_ni = (XADDINode)high_n;
			if (var > high_ni._var) {
				System.out.println("Reordering problem: " + var + " > " + high_ni._var);
				System.out.println(_alOrder.get(var));
				System.out.println(_alOrder.get(high_ni._var));
				new Exception().printStackTrace(System.out);
				//System.exit(1);
			}
		}		
		
		// Put return value in cache and return
		subst_cache.put(node_id, ret);
		return ret;
	}
	
	public Double evaluate(int node_id,
						   HashMap<String,Boolean> bool_assign, 
				   		   HashMap<String,Double>  cont_assign) {
		
		// Get root
		XADDNode n = _hmInt2Node.get(node_id);
		
		// Traverse decision diagram until terminal found
		while (n instanceof XADDINode) {
			XADDINode inode = (XADDINode)n;
			Decision d = _alOrder.get(inode._var);
			Boolean branch_high = null;
			if (d instanceof TautDec) 
				branch_high = ((TautDec)d)._bTautology;
			else if (d instanceof BoolDec)
				branch_high = bool_assign.get(((BoolDec)d)._sVarName);
			else if (d instanceof ExprDec)
				branch_high = ((ExprDec)d)._expr.evaluate(cont_assign);
			
			// Not all required variables were assigned
			if (branch_high == null)
				return null;
			
			// Advance down to next node
			n = _hmInt2Node.get(branch_high ? inode._high : inode._low);
		}
		
		// Now at a terminal node so evaluate expression
		XADDTNode t = (XADDTNode)n;
		return t._expr.evaluate(cont_assign);
	}
	
	public int scalarOp(int dd, double val, int op) {
		int dd_val = getTermNode(new DoubleExpr(val));
		return apply(dd, dd_val, op);
	}
	
	public IntTriple _tempApplyKey = new IntTriple(-1,-1,-1);

	public int apply(int a1, int a2, int op) {
		
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
					if (((XADDINode) n1)._var <
						((XADDINode) n2)._var) {
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
			int low = apply(v1low, v2low, op);
			int high = apply(v1high, v2high, op);

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
			XADDTNode xa1 = (XADDTNode)n1;
			XADDTNode xa2 = (XADDTNode)n2;
			
			if ((op != MAX) && (op != MIN))
				return getTermNode(new OperExpr(op, xa1._expr, xa2._expr));
			
			ArithExpr diff = new OperExpr(MINUS, xa1._expr, xa2._expr);
			CompExpr comp = null;
			if (USE_MINUS_COMP)
				comp = new CompExpr(LT, diff, new DoubleExpr(0d));
			else
				comp = new CompExpr(LT_EQ, xa1._expr, xa2._expr);
			Decision d = new ExprDec(comp);
			int var_index = getVarIndex(d, true);
			System.out.println("ComputeTerm::max var_index " + _alOrder.get(var_index) + ": " + comp);
			//System.exit(1);
			int node1 = getTermNode(xa1._expr);
			int node2 = getTermNode(xa2._expr);
			return getINode(var_index, 
					op == MAX ? node1 : node2,
					op == MAX ? node2 : node1);
		}
		
		if (n1 instanceof XADDTNode) {
			// If we get here a2 must be XADDINode
			XADDTNode xa1 = (XADDTNode)n1;
			
			// Check for identity operation value for n1
			if (   (op == SUM  && xa1._expr.equals(ZERO))
			    || (op == PROD && xa1._expr.equals(ONE))) {
				return a2;
			}
		}
	
		if (n2 instanceof XADDTNode) {
			// If we get here a1 must be XADDINode
			XADDTNode xa2 = (XADDTNode)n2;
			
			// Check for identity operation value for n2
			if (   (op == SUM   && xa2._expr.equals(ZERO))
			    || (op == PROD  && xa2._expr.equals(ONE))
			    || (op == MINUS && xa2._expr.equals(ZERO))
			    || (op == DIV   && xa2._expr.equals(ONE))) {
				return a1;
			}
		}

		return null;
	}

//	// ////////////////////////////////////////////////////////////////
//	// Flushing and special node maintenance
//	// ////////////////////////////////////////////////////////////////
//
//	// Designate/remove/clear nodes to persist through flushing
//	public void addSpecialNode(int n) {
//		_hsSpecialNodes.add(new ADDRNode(n));
//	}
//
//	public void removeSpecialNode(int n) {
//		_hsSpecialNodes.remove(new ADDRNode(n));
//	}
//
//	public void clearSpecialNodes() {
//		_hsSpecialNodes.clear();
//	}
//
//	// Flush caches but save special nodes
//	public void flushCaches(boolean print_info) {
//
//		// Print starting info
//		if (print_info) {
//			System.out.print("[FLUSHING CACHES... ");
//			// showCacheSize();
//			ResetTimer();
//		}
//
//		// Can always clear these
//		_hmPairs = new HashMap();
//		_hmReduceMap = new HashMap();
//		_hmPruneMap = new HashMap();
//
//		// Set up temporary alternates to these HashMaps
//		_hmNewADDNodes = new HashMap();
//		_hmNewINodeCache = new HashMap();
//		_hmNewDNodeCache = new HashMap();
//
//		// Copy over 'special' nodes then set new maps
//		Iterator i = _hsSpecialNodes.iterator();
//		while (i.hasNext()) {
//			cacheNode((ADDRNode) i.next());
//		}
//		_hmADDNodes = _hmNewADDNodes;
//		_hmINodeCache = _hmNewINodeCache;
//		_hmDNodeCache = _hmNewDNodeCache;
//
//		// Print results
//		if (GC_DURING_FLUSH) {
//			RUNTIME.gc();
//		}
//		if (print_info) {
//			System.out.print(" TIME: " + GetElapsedTime());
//			System.out
//					.print("  RESULT: "
//							+ _df
//									.format(((double) RUNTIME.freeMemory() / (double) RUNTIME
//											.totalMemory())));
//			System.out.print("  CACHE: " + getCacheSize() + "] ");
//		}
//	}
//
//	// ////////////////////////////////////////////////////////////////
//	// Internal data structure maintenance
//	// ////////////////////////////////////////////////////////////////
//
//	// Quick cache snapshot
//	public void showCacheSize() {
//		System.out.println("APPLY CACHE:  " + _hmPairs.size());
//		System.out.println("REDUCE CACHE: " + _hmReduceMap.size());
//		System.out.println("INODE CACHE:  " + _hmINodeCache.size() + "\n");
//	}
//
//	// Total cache snapshot
//	public long getCacheSize() {
//		return _hmPairs.size() + _hmReduceMap.size() + _hmINodeCache.size();
//	}
//
//	// An exact count for the ADD rooted at _nRoot
//	public long countExactNodes(int id) {
//		HashSet cset = new HashSet();
//		countExactNodesInt(cset, id);
//		return cset.size();
//	}
//
//	public Set getExactNodes(int id) {
//		HashSet cset = new HashSet();
//		countExactNodesInt(cset, id);
//		return cset;
//	}
//
//	public void countExactNodesInt(HashSet cset, int id) {
//		if (cset.contains(new Integer(id))) {
//			return;
//		}
//		ADDNode n = getNode(id);
//		// Uncomment the following to get internal-only count
//		// if (n instanceof ADDDNode) {
//		// return;
//		// }
//		cset.add(new Integer(id));
//		if (n instanceof ADDINode) {
//			countExactNodesInt(cset, ((ADDINode) n)._nLow);
//			countExactNodesInt(cset, ((ADDINode) n)._nHigh);
//		}
//	}
//
//	public Set getVarIDs(int id) {
//		HashSet cset = new HashSet();
//		HashSet gset = new HashSet();
//		collectGIDsInt(cset, gset, id);
//		return gset;
//	}
//
//	public void collectGIDsInt(HashSet cset, HashSet gset, int id) {
//		if (cset.contains(new Integer(id))) {
//			return;
//		}
//		cset.add(new Integer(id));
//		ADDNode n = getNode(id);
//		if (n instanceof ADDINode) {
//			gset.add(new Integer(((ADDINode) n)._nGlobalID));
//			collectGIDsInt(cset, gset, ((ADDINode) n)._nLow);
//			collectGIDsInt(cset, gset, ((ADDINode) n)._nHigh);
//		}
//	}
//
//	// ///////////////////////////////////////////////////////////////
//	// Order maintenance
//	// ///////////////////////////////////////////////////////////////
//
//	// Probably have more efficient ways to do a lot of these using
//	// binary search and hash tables
//
//	// Order check - both must occur in list!
//	public boolean comesBefore(int gid1, int gid2) {
//		// Get level for gid1 and gid2
//		int l1 = ((Integer) _hmGVarToLevel.get(new Integer(gid1))).intValue();
//		int l2 = ((Integer) _hmGVarToLevel.get(new Integer(gid2))).intValue();
//
//		// Determine which comes first (i.e. earlier level)
//		return (l1 <= l2);
//	}
//
//
//	// //////////////////////////////////////////////////////////////
//	// Miscellaneous methods
//	// //////////////////////////////////////////////////////////////
//
//	public String toString() {
//
//		StringBuffer sb = new StringBuffer();
//
//		// Show order
//		sb.append("Var order: " + _alOrder + "\n");
//		// sb.append("Val range: " + procList(getValueRange()) + "\n");
//
//		// Recurse from the root and show each branch
//		sb.append("Structure:\n" + getNode(_nRoot).toString(this, 0) + "\n");
//
//		return sb.toString();
//	}

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
	
	////////////////////////////////////////////////////////////////

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
		public HashSet<String> collectVars() {
			HashSet<String> vars = new HashSet<String>();
			collectVars(vars);
			return vars;
		}
		public abstract String toString(int depth);
		public abstract void toGraph(Graph g, int id);
		public abstract void collectVars(HashSet<String> vars);
	}
	
	public class XADDTNode extends XADDNode {
		public ArithExpr _expr;
		public XADDTNode(ArithExpr e) {
			if (e instanceof OperExpr && ((OperExpr)e)._terms.size() == 1)
				_expr = ((OperExpr)e)._terms.get(0);
			else
				_expr = e;
		}
		public void set(ArithExpr e) {
			if (e instanceof OperExpr && ((OperExpr)e)._terms.size() == 1)
				_expr = ((OperExpr)e)._terms.get(0);
			else
				_expr = e;
		}
		public int hashCode() {
			return _expr.hashCode();
		}
		public boolean equals(Object o) {
			if (o instanceof XADDTNode)
				return this._expr.equals(((XADDTNode)o)._expr);
			else
				return false;
		}
		public void collectVars(HashSet<String> vars) {
			_expr.collectVars(vars);
		}
	    public void toGraph(Graph g, int id) {
	    	String this_node = Integer.toString(id);
	    	g.addNode(this_node);
			g.addNodeLabel(this_node, _expr.toString());
			if (GRAPH_USE_COLOR)
				g.addNodeColor(this_node, "lightsalmon"); // red, darkred, lightsalmon
			g.addNodeShape(this_node, "box");
			g.addNodeStyle(this_node, "filled");
	    }
		public String toString(int depth) {
			return "[ "/*"#" + _hmNode2Int.get(this) + ": "*/ + _expr.toString() + " ]";
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
			return (_var) + (_low << 10) - (_high << 20)
				   + (_high >>> 20) - (_low >>> 10);
		}
		public boolean equals(Object o) {
			if (o instanceof XADDINode) {
				XADDINode n = (XADDINode)o;
				return this._var == n._var 
					&& this._low == n._low 
					&& this._high == n._high;
			} else
				return false;
		}
		public void collectVars(HashSet<String> vars) {
			XADDNode low = _hmInt2Node.get(_low);
			XADDNode high = _hmInt2Node.get(_high);
			Decision d = _alOrder.get(_var);
			d.collectVars(vars);
			low.collectVars(vars);
			high.collectVars(vars);
		}
		public void toGraph(Graph g, int id) {
			
			// Main node
			String this_node = Integer.toString(id);
			//System.out.println("var: " + _var);
			//System.out.println(_alOrder);
			g.addNode(this_node);
			g.addNodeLabel(this_node, _alOrder.get(_var).toString());
			if (GRAPH_USE_COLOR)
				g.addNodeColor(this_node, "lightblue"); // green, lightblue
			g.addNodeShape(this_node, "ellipse");
			g.addNodeStyle(this_node, "filled");
		
			// Children
			XADDNode low = _hmInt2Node.get(_low);
			String low_node = Integer.toString(_low);
			g.addUniLink(this_node, low_node, "black", "dashed", Graph.EMPTY_STR);
			low.toGraph(g, _low);

			XADDNode high = _hmInt2Node.get(_high);
			String high_node = Integer.toString(_high);
			g.addUniLink(this_node, high_node, "black", "solid", Graph.EMPTY_STR);
			high.toGraph(g, _high);
		}
		public String toString(int depth) {
			StringBuffer sb = new StringBuffer();
			sb.append("[ "/*"#" + _hmNode2Int.get(this) + 
					" v" + _var + ": "*/ + _alOrder.get(_var));

			// Node level cache
			XADDNode n2 = _hmInt2Node.get(_high);
			if (n2 != null) {
				sb.append("\n" + indent(depth) + "h:[ " + n2.toString(depth + 1) + "] ");
			} else {
				sb.append("h:[null] ");
			}
			XADDNode n1 = _hmInt2Node.get(_low);
			if (n1 != null) {
				sb.append("\n" + indent(depth) + "l:[ "	+ n1.toString(depth + 1) + "] ");
			} else {
				sb.append("l:[null] ");
			}
			sb.append("] ");

			return sb.toString();
		}
	}
	
	////////////////////////////////////////////////////////////////
	
	public static abstract class Decision { 
		public abstract void collectVars(HashSet<String> vars);
	}

	// Represents a tautology or its negation (inconsistency)
	public static class TautDec extends Decision {
		public boolean _bTautology;
		public TautDec(boolean isTaut) {
			_bTautology = isTaut;
		}
		public int hashCode() {
			return _bTautology ? 1 : 0;
		}
		public boolean equals(Object o) {
			if (o instanceof TautDec)
				return ((TautDec)o)._bTautology == _bTautology;
			else
				return false;
		}
		public void collectVars(HashSet<String> vars) {	}
		public String toString() {
			return Boolean.toString(_bTautology);
		}
	}

	public static class BoolDec extends Decision {
		public String _sVarName = null;
		public BoolDec(String var_name) {
			_sVarName = var_name.intern();
		}
		public int hashCode() {
			return _sVarName.hashCode();
		}
		public boolean equals(Object o) {
			if (o instanceof BoolDec)
				return ((BoolDec)o)._sVarName == _sVarName;
			else
				return false;
		}
		public void collectVars(HashSet<String> vars) {	
			vars.add(_sVarName);
		}
		public String toString() {
			return _sVarName;
		}
	}

	public static class ExprDec extends Decision {
		public CompExpr _expr = null;
		public ExprDec(CompExpr expr) {
			_expr = expr;
		}
		public int hashCode() {
			return _expr.hashCode();
		}
		public boolean equals(Object o) {
			if (o instanceof ExprDec)
				return ((ExprDec)o)._expr.equals(_expr);
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
	}
	
	/////////////////////////////////////////////////////////////////////
	
	public static abstract class Expr implements Comparable<Expr> {	
		public int compareTo(Expr o) {
			return this.hashCode() - o.hashCode();
		}
	}
	
	public static class CompExpr extends Expr {
				
		public int       _type = UND;
		public ArithExpr _lhs = null;
		public ArithExpr _rhs = null;
		
		public CompExpr(int type, ArithExpr lhs, ArithExpr rhs) {
			_type = type;
			_lhs = lhs;
			_rhs = rhs;
		}
		public boolean equals(Object o) {
			if (o instanceof CompExpr) {
				CompExpr c = (CompExpr)o;
				return this._type == c._type 
					&& this._lhs.equals(c._lhs) 
					&& this._rhs.equals(c._rhs);
			} else
				return false;
		}
		public int hashCode() {
			int i2 = _lhs.hashCode();
			int i3 = _rhs.hashCode();
			return (_type) + (i2 << 10) - (i3 << 20)
			   + (i3 >>> 20) - (i2 >>> 10);
		}
		public static CompExpr parse(String s) {
			try {
				FOPC.Node res = FOPC.parse(s);
				return Convert2CompExpr((FOPC.PNode)res);
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
				} break;
				case FOPC.PNode.LESS: {
					type = res._bIsNegated ? GT_EQ : LT;
				} break;
				case FOPC.PNode.LESSEQ: {
					type = res._bIsNegated ? GT : LT_EQ;
				} break;
				}
			}
			ArithExpr lhs = ArithExpr.Convert2ArithExpr((FOPC.Term)res.getBinding(0));
			ArithExpr rhs = ArithExpr.Convert2ArithExpr((FOPC.Term)res.getBinding(1));
			if (lhs == null || rhs == null || type == UND)
				return null;
			else 
				return new CompExpr(type, lhs, rhs);	
		}
		public String toString() {
			return _lhs + " " + _aOpNames[_type] + " " + _rhs;
		}
		public CompExpr substitute(HashMap<String,ArithExpr> subst) {
			ArithExpr lhs = _lhs.substitute(subst);
			ArithExpr rhs = _rhs.substitute(subst);
			return new CompExpr(_type, lhs, rhs);
		}
		public Boolean evaluate(HashMap<String,Double>  cont_assign) {
			
			Double dval_lhs = _lhs.evaluate(cont_assign);
			Double dval_rhs = _rhs.evaluate(cont_assign);
			
			if (dval_lhs == null || dval_rhs == null)
				return null;
			
			switch (_type) {
				case EQ:    return (dval_lhs == dval_rhs);
				case NEQ:   return (dval_lhs != dval_rhs);
				case GT:    return (dval_lhs >  dval_rhs);
				case GT_EQ: return (dval_lhs >= dval_rhs);
				case LT:    return (dval_lhs <  dval_rhs);
				case LT_EQ: return (dval_lhs <= dval_rhs);
				default:    return null;
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
				//if (res != null) 
				//	System.out.println("==> " + res.toFOLString());
				return Convert2ArithExpr(((FOPC.PNode)res).getBinding(0));
			} catch (Exception e) {
				return null;
			}
		}
		public static ArithExpr Convert2ArithExpr(FOPC.Term t) {
			//System.out.println("Convert2ArithExpr: " + t.toFOLString());
			if (t instanceof FOPC.TVar) {
				return new VarExpr(((FOPC.TVar)t)._sName);
			} else if (t instanceof FOPC.TScalar) {
				return new DoubleExpr(((FOPC.TScalar)t)._dVal);
			} else if (t instanceof FOPC.TInteger) {
				return new DoubleExpr(((FOPC.TInteger)t)._nVal);
			} else if (t instanceof FOPC.TFunction) {
				return OperExpr.Convert2OperExpr((FOPC.TFunction)t);
			} else
				return null;
		}
		public static ArithExpr op(ArithExpr f1, ArithExpr f2, int op) {
			if (f1 instanceof DoubleExpr && 
				  (op == SUM || op == PROD)) {
				// operands reordered
				return op(f2, ((DoubleExpr)f1)._dConstVal, op);
			} else if (f2 instanceof DoubleExpr) {
				return op(f1, ((DoubleExpr)f2)._dConstVal, op);
			} else if (f1 instanceof OperExpr && f2 instanceof OperExpr
					&& ((OperExpr)f1)._type == ((OperExpr)f2)._type
					&& ((OperExpr)f1)._type == op
					&& (op == SUM || op == PROD)) {
				ArrayList<ArithExpr> terms = (ArrayList<ArithExpr>)((OperExpr)f1)._terms.clone();
				terms.addAll(((OperExpr)f2)._terms);
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
					return new DoubleExpr(((DoubleExpr)f1)._dConstVal + d);
				else if (op == PROD)
					return new DoubleExpr(((DoubleExpr)f1)._dConstVal * d);
				else if (op == MINUS)
					return new DoubleExpr(((DoubleExpr)f1)._dConstVal - d);
				else if (op == DIV)
					return new DoubleExpr(((DoubleExpr)f1)._dConstVal / d);
				else {
					ArrayList<ArithExpr> terms = new ArrayList<ArithExpr>();
					terms.add(new DoubleExpr(d));
					terms.add(f1);
					return new OperExpr(op, terms);
				}
			} else if (f1 instanceof OperExpr
					&& ((OperExpr)f1)._type == op
					&& (op == SUM || op == PROD)) {
				ArrayList<ArithExpr> terms = (ArrayList<ArithExpr>)((OperExpr)f1)._terms.clone();
				terms.add(new DoubleExpr(d));
				return new OperExpr(op, terms);
			} else {
				ArrayList<ArithExpr> terms = new ArrayList<ArithExpr>();
				terms.add(new DoubleExpr(d));
				terms.add(f1);
				return new OperExpr(op, terms);
			}
		}
		public abstract ArithExpr substitute(HashMap<String,ArithExpr> subst);
		public abstract Double evaluate(HashMap<String,Double>  cont_assign);
		public abstract void collectVars(HashSet<String> vars);
	}
	
	public static class OperExpr extends ArithExpr {

		public int _type = UND;
		public ArrayList<ArithExpr> _terms = null;
		
		public OperExpr(int type, ArithExpr t1, ArithExpr t2) {
			//this(type, Arrays.asList(new ArithExpr[] {t1, t2}));
			this(type, Arrays.asList(t1, t2));
		}
		
		public OperExpr(int type, List<ArithExpr> terms) {
			_type = type;
			// For now allowing all operators
			// if (_type < SUM || _type > DIV)
			//   return;
			
			// Ensure subtraction and division are binary operators
			if ((_type == MINUS || _type == DIV) && terms.size() != 2) {
				_type = ERROR;
				return;
			}
			_terms = new ArrayList<ArithExpr>();
			
			// Remove common terms during a minus
			if (_type == MINUS && SIMPLIFY_MINUS) {
				// TODO: NEEDS TO SIMPLIFY BOTH SIDES OF MINUS!!!
				ArithExpr t1 = terms.get(0);
				ArithExpr t2 = terms.get(1);
				if (t1 instanceof OperExpr && ((OperExpr)t1)._type == SUM) {
					for (ArithExpr t11 : ((OperExpr)t1)._terms) {
						if (t2 != null && t2 instanceof OperExpr && ((OperExpr)t2)._type == SUM) {
							int index = ((OperExpr)t2)._terms.indexOf(t11);
							if (index >= 0)
								((OperExpr)t2)._terms.remove(index);
						} else if (t2 != null) {
							if (t11.equals(t2))
								t2 = null;
						}
					}	
				} else if (t2 instanceof OperExpr && ((OperExpr)t2)._type == SUM) {
					for (ArithExpr t22 : ((OperExpr)t2)._terms) {
						if (t1 != null && t1 instanceof OperExpr && ((OperExpr)t1)._type == SUM) {
							int index = ((OperExpr)t1)._terms.indexOf(t22);
							if (index >= 0)
								((OperExpr)t1)._terms.remove(index);
						} else if (t1 != null) {
							if (t22.equals(t1))
								t1 = null;
						}
					}	
				} else if (t1.equals(t2)) {
					t1 = null;
					t2 = null;
				}
				
				if (t1 == null && t2 == null) {
					_type = SUM;
					_terms.add(new DoubleExpr(0));
					return;
				} else if (t1 == null) {
					// Would have to negate t2 to return immediately
					terms.set(0, new DoubleExpr(0));
				} else if (t2 == null) {
					if (t1 instanceof OperExpr) {
						_type = ((OperExpr)t1)._type;
						_terms.addAll(((OperExpr)t1)._terms);
						return;
					} else {
						_type = SUM;
						_terms.add(t1);
						return;
					}
				} // else just fall through and process as normal
			}
			
			if ((_type == SUM) || (_type == PROD)) {
				double accum = (_type == SUM) ? 0d : 1d;
				for (int i = 0; i < terms.size(); i++) {
					ArithExpr e = terms.get(i);
					if (e instanceof DoubleExpr) {
						if (_type == SUM)
							accum += ((DoubleExpr)e)._dConstVal;
						else 
							accum *= ((DoubleExpr)e)._dConstVal;
					} else if (e instanceof OperExpr
							   && ((OperExpr)e)._type == type) {
						_terms.addAll(((OperExpr)e)._terms);
					} else
						_terms.add(e);
				}
				if (accum == 0d && _type == PROD) {
					// Anything times zero is zero
					_terms.clear();
					_terms.add(new DoubleExpr(0d));
				} else if (accum != ((_type == SUM) ? 0d : 1d)) 
					_terms.add(new DoubleExpr(accum));
			} else {
				_terms.addAll(terms);
			}
			Collections.sort(_terms);
		}
		public boolean equals(Object o) {
			if (o instanceof OperExpr) {
				OperExpr e = (OperExpr)o;
				return this._type == e._type 
					&& this._terms.equals(e._terms); 
			} else
				return false;
		}
		public int hashCode() {
			return _terms.toString().hashCode() - _type;
		}
		public static ArithExpr Convert2OperExpr(FOPC.TFunction t) {
			//System.out.println("Convert2OperExpr: [" + t._nArity + "] " 
			//		+ t.toFOLString());
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
				if (term1 instanceof DoubleExpr
					&& term2 instanceof DoubleExpr
					&& ((DoubleExpr)term1)._dConstVal == 0d)
					return new DoubleExpr(-((DoubleExpr)term2)._dConstVal);
			} else if (t._sFunName.equals("f_mul")) {
				type = PROD;
			} else if (t._sFunName.equals("f_div")) {
				type = DIV;
			} 
			if (type == UND)
				return null;
			if ((type == SUM || type == PROD) 
				&& (term1 instanceof OperExpr) 
				&& ((OperExpr)term1)._type == type) {
				terms.addAll(((OperExpr)term1)._terms);
				term1 = null;
			}
			if ((type == SUM || type == PROD) 
					&& (term2 instanceof OperExpr) 
					&& ((OperExpr)term2)._type == type) {
					terms.addAll(((OperExpr)term2)._terms);
					term2 = null;
			}
			if (term1 != null)
				terms.add(term1);
			if (term2 != null)
				terms.add(term2);
			return new OperExpr(type, terms);
		}
		public String toString() {
			StringBuilder sb = new StringBuilder("(");
			for (int i = 0; i < _terms.size(); i++) {
				if (i != 0)
					sb.append(" " + _aOpNames[_type] + " ");
				sb.append(_terms.get(i));
			}
			sb.append(")");
			return sb.toString();
		}
		public void collectVars(HashSet<String> vars) {
			for (ArithExpr e : _terms)
				e.collectVars(vars);
		}
		public ArithExpr substitute(HashMap<String,ArithExpr> subst) {
			ArrayList<ArithExpr> terms2 = new ArrayList<ArithExpr>();
			for (ArithExpr expr : _terms)
				terms2.add(expr.substitute(subst));
			OperExpr expr = new OperExpr(_type, terms2);
			if (expr._terms.size() == 1)
				return expr._terms.get(0);
			else 
				return expr;
		}
		public Double evaluate(HashMap<String,Double>  cont_assign) {
			Double accum = _terms.get(0).evaluate(cont_assign);
			for (int i = 1; i < _terms.size() && accum != null; i++) {
				Double term_eval = _terms.get(i).evaluate(cont_assign);
				if (term_eval == null)
					accum = null;
				else
					switch(_type) {
						case SUM:	accum = accum + term_eval; break;
						case MINUS:	accum = accum - term_eval; break;
						case PROD:	accum = accum * term_eval; break;
						case DIV:	accum = accum / term_eval; break;
						default: 	accum = null;
					}
			}
			return accum;
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
		public boolean equals(Object o) {
			if (o instanceof DoubleExpr) {
				DoubleExpr d = (DoubleExpr)o;
				return this._dConstVal == d._dConstVal; 
			} else
				return false;
		}
		public int hashCode() {
			return Float.floatToIntBits((float)_dConstVal);
		}
		public ArithExpr substitute(HashMap<String,ArithExpr> subst) {
			return this;
		}
		public Double evaluate(HashMap<String,Double>  cont_assign) {
			return _dConstVal;
		}
		public void collectVars(HashSet<String> vars) { }

	}
	
	public static class VarExpr extends ArithExpr {
		public String _sVarName = null;
		public VarExpr(String name) {
			_sVarName = name.intern();
		}
		public String toString() {
			return _sVarName;
		}
		public int hashCode() {
			return _sVarName.hashCode();
		}
		public boolean equals(Object o) {
			if (o instanceof VarExpr) {
				VarExpr v = (VarExpr)o;
				return this._sVarName == v._sVarName; 
			} else
				return false;
		}
		public ArithExpr substitute(HashMap<String,ArithExpr> subst) {
			ArithExpr v = subst.get(_sVarName);
			return v != null ? v : this;  
		}
		public Double evaluate(HashMap<String,Double>  cont_assign) {
			return cont_assign.get(_sVarName);
		}
		public void collectVars(HashSet<String> vars) {
			vars.add(_sVarName);
		}
 	}
	
    public static String indent(int depth) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i <= depth; i++)
			sb.append(STRING_TAB);
		return sb.toString();
	}

	////////////////////////////////////////////////////////////////
	
	public static void main(String[] args) {
		System.out.println(Double.MAX_VALUE + " , " + (-Double.MAX_VALUE));
		TestParse("[a]");
		TestParse("[a + b]");
		TestParse("[a + b + 3 + 4]");
		TestParse("[a + b + 3 + 4 >= 3 / 7]");
		TestParse("[a + b + 3 + -4 * y = 9]");
		TestParse("[((a + b) * (3 * 4)]"); // Mismatched parens
		TestParse("[(a + b) * (3 * 4)]");
		
		// Build and display an XADD
		XADD xadd_context = new XADD();
		// Put all boolean variables first to avoid reordering clashes
		xadd_context.getVarIndex(new XADD.BoolDec("f"), true);
		xadd_context.getVarIndex(new XADD.BoolDec("g"), true);
		xadd_context.getVarIndex(new XADD.BoolDec("h"), true);
		int xadd1 = TestBuild(xadd_context, "src/xadd/test1.xadd");
		int xadd2 = TestBuild(xadd_context, "src/xadd/test2.xadd");
		int xaddr1 = xadd_context.apply(xadd1, xadd2, XADD.PROD);
		Graph g1 = xadd_context.getGraph(xaddr1); g1.launchViewer();
		int xaddr2 = xadd_context.opOut(xaddr1, 2, PROD);
		Graph g2 = xadd_context.getGraph(xaddr2); g2.launchViewer();
		int xaddr3 = xadd_context.apply(xadd1, xadd2, XADD.MAX);
		Graph g3 = xadd_context.getGraph(xaddr3); g3.launchViewer();
		HashMap<String, ArithExpr> subst = new HashMap<String, ArithExpr>();
		subst.put("k", new DoubleExpr(10d));
		int xaddr4 = xadd_context.substitute(xaddr3, subst);
		Graph g4 = xadd_context.getGraph(xaddr4); g4.launchViewer();
		subst.put("x1", new DoubleExpr(0d/*5d*/));
		int xaddr5 = xadd_context.substitute(xaddr3, subst);
		Graph g5 = xadd_context.getGraph(xaddr5); g5.launchViewer();
		System.out.println("Vars: " + xadd_context._alOrder);
	
		HashMap<String,Boolean> bool_assign = new HashMap<String,Boolean>();
		bool_assign.put("f", true);
		bool_assign.put("g", true); // if h instead, eval will be null
		
		HashMap<String,Double> cont_assign = new HashMap<String,Double>();
		cont_assign.put("k",  0d);
		cont_assign.put("x1", -5d);
		
		System.out.println("Eval: [" + bool_assign + "], [" + cont_assign + "]"
						   + ": " + xadd_context.evaluate(xaddr3, bool_assign, cont_assign));		
		cont_assign.put("x1", 10d);
		System.out.println("Eval: [" + bool_assign + "], [" + cont_assign + "]"
				   + ": " + xadd_context.evaluate(xaddr3, bool_assign, cont_assign));		
		cont_assign.put("x2", 7d);
		System.out.println("Eval: [" + bool_assign + "], [" + cont_assign + "]"
				   + ": " + xadd_context.evaluate(xaddr3, bool_assign, cont_assign));		
	}
	
	public static void TestParse(String s) {
		s = s.substring(1,s.length()-1);
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
		int dd1 = xadd_context.buildCanonicalXADD((ArrayList)l.get(0));
		int dd2 = xadd_context.apply(dd1, dd1, XADD.SUM);
		int dd3 = xadd_context.apply(dd1, dd1, XADD.PROD);
		Graph g1 = xadd_context.getGraph(dd1); g1.launchViewer();
		//Graph g2 = xadd_context.getGraph(dd2); g2.launchViewer();
		//Graph g3 = xadd_context.getGraph(dd3); g3.launchViewer();
		return dd1;
	}
}
