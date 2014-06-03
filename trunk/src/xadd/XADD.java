//////////////////////////////////////////////////////////////////////
//
// Extended Algebraic Decision Diagrams
//
// @author Scott Sanner (ssanner@gmail.com)
// @author Zahra Zamani
// @author Luis Vianna
//////////////////////////////////////////////////////////////////////

package xadd;

import graph.Graph;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.*;

import util.IntPair;
import util.IntTriple;
import camdp.HierarchicalParser;
import xadd.ExprLib.ArithExpr;
import xadd.ExprLib.CoefExprPair;
import xadd.ExprLib.CompOperation;
import xadd.ExprLib.DoubleExpr;
import xadd.ExprLib.OperExpr;
import xadd.ExprLib.VarExpr;
import xadd.ExprLib.ArithOperation;
import xadd.ExprLib.CompExpr;

/**
 * General class for implementation of ADD data structure
 */
public class XADD {

    ///////////
    //Members//
    ///////////

    // Visualization
    public final static boolean GRAPH_USE_COLOR = true;
    // Flags
    public final static boolean USE_CANONICAL_NODES = true; // Store nodes in canonical format?
    public final static boolean NORMALIZE_DECISIONS = true; //Store decision with normalized coefficients?
	private static final boolean USE_APPLY_GET_INODE_CANON = false;
	private static final boolean TEST_CANON_METHODS = false;

	
    private static final boolean WARN_BOUND_UNUSED_VAR = false;
    private static final boolean WARN_INODE_CANON_NEG_DEC = false;
    public final static int MAX_BRANCH_COUNT = 1000000;

    // Debug
    public final static boolean CHECK_LOCAL_ORDERING = true;
    public final static boolean SHOW_DECISION_EVAL = false;
    public final static boolean DEBUG_EVAL_RANGE = false;
    public final static boolean HANDLE_NONLINEAR = false;
    public final static boolean VERBOSE_MIN_MAX = false;

    // Operator constants
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
    public final static int ROUND = 16;
    public final static int ERROR = 17; // This should always be max index -- anything index >= is ERROR
    public final static String[] _aOpNames = {/* 0 */"UND",
        /* 1 */"+", "-", "*", "/", "max", "min", "|l", "|h",
        /* 9 */"=", "!=", ">", ">=", "<", "<=", "LIN", "ROUND", "ERROR"};

    // Printing constants
    public final static String STRING_TAB = "   ";

    //Precision constants
    /**
     * NOTE: all precision parameters are reset since in case leaves are of degree more than 1, they produce significant errors. (Hadi)
     */
    public static double PRECISION = 0.0;//1e-10;   //'final' removed by Hadi. Hadi makes it 0.0 (see equality() of class DoubleExpr for the reason behind it)
    public final static double DEFAULT_UPPER_BOUND = Double.MAX_VALUE;//1e+10d; //change by Hadi
    public final static double DEFAULT_LOWER_BOUND = -DEFAULT_UPPER_BOUND;
    public static final Integer ROUND_PRECISION = null;//changed by Hadi. Null represents no rounding (solves lots of problems)


    //XADD Variable Maintenance
    public HashSet<String> _hsBooleanVars = new HashSet<String>();
    public ArrayList<String> _alBooleanVars = new ArrayList<String>();
    public HashMap<String, Integer> _cvar2ID;
    private ArrayList<String> _alContinuousVars;
    private HashSet<String> _hsContinuousVars;

    private ReduceLPContext RLPContext = null;

    //Bound Maintenance
    public HashMap<String, Double> _hmMinVal = new HashMap<String, Double>();
    public HashMap<String, Double> _hmMaxVal = new HashMap<String, Double>();
    public double[] upperBounds = new double[0];
    public double[] lowerBounds = new double[0];

    //Decision Variable Maintenance
    public ArrayList<Decision> _alOrder = new ArrayList<Decision>();

    // Node Maintenance
    public int _markCounter = 1;
    public int _nodeCounter = 1;
    public HashMap<XADDNode, Integer> _hmNode2Int = new HashMap<XADDNode, Integer>();
    public HashMap<Integer, XADDNode> _hmInt2Node = new HashMap<Integer, XADDNode>();

    // Reduce & Apply Caches
    public HashMap<IntTriple, Integer> _hmReduceCache = new HashMap<IntTriple, Integer>();
    public HashMap<IntPair, Integer> _hmReduceLeafOpCache = new HashMap<IntPair, Integer>();
    public HashMap<Integer, Integer> _hmReduceCanonCache = new HashMap<Integer, Integer>();
    public HashMap<IntPair, Integer> _hmReduceAnnotateCache = new HashMap<IntPair, Integer>();
    public HashMap<IntTriple, Integer> _hmApplyCache = new HashMap<IntTriple, Integer>();
    public HashMap<XADDINode, HashSet<String>> _hmINode2Vars = new HashMap<XADDINode, HashSet<String>>();

    // Flush
    public HashSet<Integer> _hsSpecialNodes = new HashSet<Integer>();
    public HashMap<XADDNode, Integer> _hmNode2IntNew = new HashMap<XADDNode, Integer>();
    public HashMap<Integer, XADDNode> _hmInt2NodeNew = new HashMap<Integer, XADDNode>();

    public int ZERO = -1;
    public int ONE = -1;
    public int POS_INF = -1;
    public int NEG_INF = -1;
    public int NAN = -1;

    /////////////////////////////////////////////////////////
    //                   XADD Methods                      //
    /////////////////////////////////////////////////////////

    ///////////////////////////
    //XADD Creation Methods //
    //////////////////////////

    public XADD() {
        // Ensure that the 0th decision ID is invalid
        _alOrder.add(new NullDec());
        _alContinuousVars = new ArrayList<String>();
        _hsContinuousVars = new HashSet<String>();
        _cvar2ID = new HashMap<String, Integer>();
        createStandardNodes();
        RLPContext = new ReduceLPContext(this);
    }

    public int createDoubleNode(double d){
    	return getTermNode(new ExprLib.DoubleExpr(d));
    }
    
    public void createStandardNodes() {
        ZERO = getTermNode(ExprLib.ZERO);
        ONE = getTermNode(ExprLib.ONE);
        POS_INF = getTermNode(ExprLib.POS_INF);
        NEG_INF = getTermNode(ExprLib.NEG_INF);
        NAN = getTermNode(ExprLib.NAN);
        
        //NaN Node requires special creation
//        int id = _nodeCounter;
//        XADDTNode node = new XADDTNode(ExprLib.NAN, null);
//        _hmNode2Int.put(node, id);
//        _hmInt2Node.put(id, node);
//        _nodeCounter++;
//        NAN = id;
    }

    //Adding XADD Variables
    //Only method to add new continuous var to XADD, keeps different var references synched.
    public void addContinuousVar(String s) {
        _hsContinuousVars.add(s);
        _alContinuousVars.add(s);
        _cvar2ID.put(s, _alContinuousVars.size() - 1);
        boundsUpToDate();
    }

    //Only methods to add bounds for a continuous var to XADD,
    public void addContinuousVarMaxBound(String var, double max) {
        addContinuousVarBounds(var, null, max);
    }

    public void addContinuousVarMinBound(String var, double min) {
        addContinuousVarBounds(var, min, null);
    }

    public void addContinuousVarBounds(String var, Double min, Double max) {
        Integer id = _cvar2ID.get(var);
        if (id == null) {
            if (WARN_BOUND_UNUSED_VAR) {
                System.err.println("Warning: Adding bound for non-existing variable " + var);
            }
            addContinuousVar(var);
            id = _cvar2ID.get(var);
        }
        if (min != null) {
            _hmMinVal.put(var, min);
            lowerBounds[id] = min;
        }
        if (max != null) {
            _hmMaxVal.put(var, max);
            upperBounds[id] = max;
        }
    }

    public void addContinuousVarsBounds(HashMap<String, Double> minVal, HashMap<String, Double> maxVal) {
        for (String var : minVal.keySet()) {
            addContinuousVarBounds(var, minVal.get(var), maxVal.get(var));
        }
    }

    //Ensures variable bounds are initialized and updated
    private boolean boundsUpToDate() {
        int nvars = _alContinuousVars.size();
        if (this.lowerBounds.length == nvars) return true;

        this.lowerBounds = new double[nvars];
        this.upperBounds = new double[nvars];

        for (Map.Entry<String, Integer> me : _cvar2ID.entrySet()) {
            String cvar = me.getKey();
            Double bound = this._hmMinVal.get(cvar);
            if (bound == null) {
                bound = DEFAULT_LOWER_BOUND;
                this._hmMinVal.put(cvar, bound);
            }
            this.lowerBounds[me.getValue()] = bound;
            bound = this._hmMaxVal.get(cvar);
            if (bound == null) {
                bound = DEFAULT_UPPER_BOUND;
                this._hmMaxVal.put(cvar, bound);
            }
            this.upperBounds[me.getValue()] = bound;
        }
        return false;
    }

    //Create Decision "vars"
    public int getVarIndex(Decision d, boolean create) {

        if (USE_CANONICAL_NODES) {
            //System.out.println(">> Before canonical: " + d);
            d = d.makeCanonical();
            //System.out.println(">> After canonical: " + d);
            if (d instanceof ExprDec && !((ExprDec) d)._expr._rhs.equals(ExprLib.ZERO)) {
                System.err.println("Expected RHS 0 in canonical ExprDec, but got: " + d);
                System.exit(1);
            }
        }
        int index = _alOrder.indexOf(d);
        // If not found, try negating d
        if (index < 0 && d instanceof ExprDec) {
            CompExpr comp = ((ExprDec) d)._expr;
            // Don't flip inequality since only using <, instead negate (Luis's idea)
            // CompExpr neg_comp = new CompExpr(CompExpr.flipCompOper(comp._type), comp._lhs, comp._rhs);
            CompExpr neg_comp = new CompExpr(comp._type, ArithExpr.op(comp._lhs, ExprLib.NEG_ONE, ArithOperation.PROD), comp._rhs);
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
            index = _alOrder.size() - 1;

            if (d instanceof BoolDec) {
                if (_hsBooleanVars.add(((BoolDec)d)._sVarName)) // false if already in set
                    _alBooleanVars.add(((BoolDec)d)._sVarName);
            } else if (d instanceof ExprDec) {
                HashSet<String> all_vars = new HashSet<String>();
                ((ExprDec) d)._expr.collectVars(all_vars);
                for (String s : all_vars) {
                    // Expressions should only contain continuous variables
                    if (_hsBooleanVars.contains(s)) {
                    	System.err.println("getVarIndex: " + s + " cannot be both a boolean and continuous variable, i.e., in " + d);
                    	System.exit(1);
                    }
                    if (!_hsContinuousVars.contains(s))
                        addContinuousVar(s);
                }
            }
            return index;
        }
    }

    //Create Nodes
    public XADDTNode _tempTNode = new XADDTNode(null, null);

    public int getTermNode(ArithExpr e) {
        return getTermNode(e, null);
    }

    // Note: var index can never be 0, negative var index now means negated decision
    public int getTermNode(XADDTNode n) {
        return getTermNode(n._expr, n._annotate);
    }

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

            // Add in all new continuous variables
            HashSet<String> all_vars = new HashSet<String>();
            node._expr.collectVars(all_vars);
            for (String s : all_vars)
                // Boolean variables would have been added immediately in BoolDec
                // so are already in _hsBooleanVars
                if (!_hsBooleanVars.contains(s) && !_hsContinuousVars.contains(s)) {
                    addContinuousVar(s);
                }
        }
        return id;
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
            System.err.println("Invalid node (low,high)=(" + low + "," + high
                    + ") for var: " + var);
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

    public int getINodeCanon(int var, int low, int high) {
    	if (var <= 0){
    		//Shouldn't all decisions be positive?
    		if (WARN_INODE_CANON_NEG_DEC) System.out.println("Warning: Canonizing Negative Decision:"+var+" =>"+_alOrder.get(Math.abs(var)));
    		return getINodeCanon(-var, high, low);
    	}
    	
    	if (TEST_CANON_METHODS){
			int result1 = getINodeCanonApplyTrick(var, low, high);        
			int result2 = getINodeCanonInsert(var,low,high);
				
			if (result1 != result2 && result1 != NAN){
				System.out.println("Canonical Error (Difference not on NAN):");
				System.out.println("PROD Result:");
				System.out.println(getExistNode(result1));
				System.out.println("New Canon:");
				System.out.println(getExistNode(result2));
			
				getINodeCanonApplyTrick(var, low, high);
				getINodeCanonInsert(var,low,high);
			}
			return (USE_APPLY_GET_INODE_CANON)? result1: result2;
    	}
    	else 
    		return (USE_APPLY_GET_INODE_CANON)? getINodeCanonApplyTrick(var, low, high): getINodeCanonInsert(var,low,high);
    }

    public int getINodeCanonApplyTrick(int var, int low, int high) {
        int ind_true = getINode(var, /* low */ZERO, /* high */ONE);
        int ind_false = getINode(var, /* low */ONE, /* high */ZERO);
        int true_half = applyInt(ind_true, high, PROD);
        // this enforces canonicity so can use applyInt rather than apply
        int false_half = applyInt(ind_false, low, PROD);
        int result = applyInt(true_half, false_half, SUM);        
//            getGraph(true_half).launchViewer("[" + result + "] INode Input True Half");
//            getGraph(false_half).launchViewer("[" + result + "] Input Input False Half");
//            getGraph(result).launchViewer("[" + result + "] Output INode");        
        return result;
    }

    
    public int getINodeCanonInsert(int var, int low, int high){
        int false_half = reduceInsertNode(low, var, ZERO, true);
        int true_half = reduceInsertNode(high, var, ZERO, false);
        return applyInt(true_half, false_half, SUM);
    }
    
    public int reduceInsertNode(int orig, int decision, int node_to_insert_on_dec_value, boolean dec_value){
    	return reduceInsertNodeInt(orig, decision, node_to_insert_on_dec_value, dec_value, new HashMap<Integer,Integer>()); //Map only for orig
    }
    
    public int reduceInsertNodeInt(int orig, int decision, int insertNode, boolean dec_value, HashMap<Integer,Integer> _hmInsertNodeCache){
    	Integer ret = _hmInsertNodeCache.get(orig);
    	if (ret != null) return ret;
    	
    	XADDNode n = getExistNode(orig);
    	if ( (n instanceof XADDTNode) || (n instanceof XADDINode && ((XADDINode)n)._var > decision) ){
    		ret = dec_value? getINode(decision, orig, insertNode): getINode(decision, insertNode, orig);
    	}
    	else {
    		XADDINode inode = (XADDINode) n;
    		if (decision > inode._var){
    			int low = reduceInsertNodeInt(inode._low, decision, insertNode, dec_value, _hmInsertNodeCache);
    			int high = reduceInsertNodeInt(inode._high, decision, insertNode, dec_value, _hmInsertNodeCache);
    			ret = getINode(inode._var, low, high);
    		}
    		else{
    			//Inserting same Decision as in DD
    			if (dec_value) {
    				ret = reduceInsertNodeInt(inode._low, decision, insertNode, dec_value, _hmInsertNodeCache);
    			}
    			else{
    				ret = reduceInsertNodeInt(inode._high, decision, insertNode, dec_value, _hmInsertNodeCache);
    			}
    			
    		}
    	}
    	_hmInsertNodeCache.put(orig, ret);
    	return ret;
    }

    public int getVarNode(Decision d, double low_val, double high_val) {
        int var = getVarIndex(d, true);
        int low = getTermNode(new DoubleExpr(low_val));
        int high = getTermNode(new DoubleExpr(high_val));
        return getINode(var, low, high);
    }

    @SuppressWarnings("rawtypes")
    public int buildCanonicalXADD(ArrayList l) {
        return XADDParseUtils.BuildCanonicalXADD(this, l);
    }

    @SuppressWarnings("rawtypes")
    public int buildCanonicalXADDFromString(String s) {
        ArrayList l = HierarchicalParser.ParseString(s);
        return XADDParseUtils.BuildCanonicalXADD(this, (ArrayList) l.get(0));
    }

    @SuppressWarnings("rawtypes")
    public int buildCanonicalXADDFromFile(String filename) {
        ArrayList l = HierarchicalParser.ParseFile(filename);
        return XADDParseUtils.BuildCanonicalXADD(this, (ArrayList) l.get(0));
    }

    //////////////////////////////////////
    ///////Canonical and Substitute//////
    /////////////////////////////////////

    // Convert a diagram with decision nodes that are potentially out of order
    // to one with nodes in order using the "apply trick"
    public int makeCanonical(int node_id) {
        // NOTE: while both approaches 1 and 2 produce the same result, the empty substitution
        //       incurred by approach 1 seems to fortuitously perturb expression and decision
        //       order in a way that leads to a noticeable speedup on some problems.  It also
        //       saves us from keep another cache around, which saves space.

        // CANONIZATION APPROACH 1
        //return reduceSub(node_id, new HashMap<String, ArithExpr>(), new HashMap<Integer, Integer>());

        // CANONIZATION APPROACH 2
        _hmReduceCanonCache.clear();
        return makeCanonicalInt(node_id);
    }

    public int makeCanonicalInt(int node_id) {
        Integer ret = null;
        XADDNode n = getExistNode(node_id);

        // A terminal node should be reduced (and cannot be restricted)
        // by default but there is a chance this TNode was created directly
        if (n instanceof XADDTNode)
            return getTermNode((XADDTNode) n);

        // Check to see if this node has already been made canonical
        if ((ret = _hmReduceCanonCache.get(node_id)) != null)
            return ret;

        // Handle an internal node
        XADDINode inode = (XADDINode) n;

        // Recursively ensure canonicity for subdiagrams
        int low = makeCanonicalInt(inode._low);
        int high = makeCanonicalInt(inode._high);

        // Enforce canonicity via the "apply trick" at this level.
        //
        // Note: var decision expressions should be canonical to get
        // assigned an ID in _alOrder, but sometimes they can be further
        // reduced to a tautological decision and this will check for it.
        int var = inode._var;
        Decision d = _alOrder.get(var);
        if (d instanceof ExprDec)
            var = getVarIndex(d, true);
        ret = getINodeCanon(var, low, high);

        // Error check
        if (CHECK_LOCAL_ORDERING)
            checkLocalOrderingAndExitOnError(ret);

        // Put return value in cache and return
        _hmReduceCanonCache.put(node_id, ret);
        return ret;
    }

    //Symbolic substitution methods
    public int substitute(int node_id, HashMap<String, ArithExpr> subst) {
        return reduceSub(node_id, subst, new HashMap<Integer, Integer>());
    }

    // Substitution has to enforce order if it is violated, hence the
    // additional call to getInodeCanon.
    public int reduceSub(int node_id, HashMap<String, ArithExpr> subst,
                         HashMap<Integer, Integer> subst_cache) {

        Integer ret = null;
        XADDNode n = getExistNode(node_id);

        // A terminal node should be reduced (and cannot be restricted)
        // by default if hashing and equality testing are working in getTNode
        if (n instanceof XADDTNode) {
            ArithExpr expr = ((XADDTNode) n)._expr.substitute(subst);
            Object annotation = ((XADDTNode) n)._annotate;
            if (annotation != null && annotation instanceof ArithExpr) {
                annotation = ((ArithExpr) annotation).substitute(subst);
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

        // substitution could have affected a variable reordering,
        // making canon INode is via apply.
        ret = getINodeCanon(var, low, high);

        if (CHECK_LOCAL_ORDERING)
            checkLocalOrderingAndExitOnError(ret);

        // Put return value in cache and return
        subst_cache.put(node_id, ret);
        return ret;
    }
    
    public Integer substituteNode(int main_id, int target_id, int replace_id){
    	HashMap<Integer,Integer> subNodeCache =  new HashMap<Integer, Integer>();
    	subNodeCache.put(target_id, replace_id);
    	return reduceSubstituteNode(main_id, subNodeCache);
    }
    // XADD substitution -> replace terminal node with another XADD Node
    public Integer reduceSubstituteNode(int main_id, HashMap<Integer, Integer> subNodeCache){

    	Integer ret = null;
    	if ( (ret = subNodeCache.get(main_id)) != null){
    		return ret;
    	}

        XADDNode n = getExistNode(main_id);

        //If terminal means no more nodes to substitute
        if (n instanceof XADDTNode) {
            return main_id;
        }

        XADDINode inode = (XADDINode) n;

        int low = reduceSubstituteNode(inode._low, subNodeCache);
        int high = reduceSubstituteNode(inode._high, subNodeCache);

        int var = inode._var;

        // substitution could have affected a variable reordering, making canon INode is via apply.
        ret = getINodeCanon(var, low, high);

        if (CHECK_LOCAL_ORDERING)
            checkLocalOrderingAndExitOnError(ret);

        // Put return value in cache and return
        subNodeCache.put(main_id, ret);
        return ret;
    }
    
    public void checkLocalOrderingAndExitOnError(int node) {
        XADDNode new_node = getExistNode(node);
        if (new_node instanceof XADDINode) {
            XADDINode new_inode = (XADDINode) new_node;
            int var_id = new_inode._var;
            XADDNode low_n = getExistNode(new_inode._low);
            if (low_n instanceof XADDINode) {
                XADDINode low_ni = (XADDINode) low_n;
                if (var_id > low_ni._var) {
                    System.out.println("Reordering problem: " + var_id + " > " + low_ni._var);
                    System.out.println(var_id + ": " + _alOrder.get(var_id));
                    System.out.println(low_ni._var + ": " + _alOrder.get(low_ni._var));
                    new Exception().printStackTrace(System.out);
                    System.exit(1);
                }
            }
            XADDNode high_n = getExistNode(new_inode._high);
            if (high_n instanceof XADDINode) {
                XADDINode high_ni = (XADDINode) high_n;
                if (var_id > high_ni._var) {
                    System.out.println("Reordering problem: " + var_id + " > " + high_ni._var);
                    System.out.println(var_id + ": " + _alOrder.get(var_id));
                    System.out.println(high_ni._var + ": " + _alOrder.get(high_ni._var));
                    new Exception().printStackTrace(System.out);
                    System.exit(1);
                }
            }
        }

    }

    // Convert an ArithExpr to an XADD while performing (optional) substitution
    // of an XADD for a var (set var = null if no substitution is needed)
    public int substituteXADDforVarInArithExpr(ArithExpr leaf_val, String var,
                                               int xadd) {
        if (leaf_val instanceof OperExpr) {
            OperExpr oper_expr = (OperExpr) leaf_val;
            int running_xadd = substituteXADDforVarInArithExpr(
                    oper_expr._terms.get(0), var, xadd);
            for (int i = 1; i < oper_expr._terms.size(); i++) {
                int next_operand = substituteXADDforVarInArithExpr(
                        oper_expr._terms.get(i), var, xadd);
                running_xadd = apply(running_xadd, next_operand,
                        ArithOperation.toXADDOper(oper_expr._type) /* SUM, PROD, ... */);
            }
            return running_xadd;
        } else if (leaf_val instanceof VarExpr) {
            VarExpr var_expr = (VarExpr) leaf_val;
            if (var_expr._sVarName == var) // assume interned
            {
                return xadd;
            } else
                return getTermNode(leaf_val);
        } else if (leaf_val instanceof DoubleExpr) {
            return getTermNode(leaf_val);
        } else {
            System.out.println("substituteXADDforVar: Unsupported expression '" + leaf_val + "'");
            System.exit(1);
            return -1;
        }
    }

    /////////////////////////////////////
    // Variable/Node Retrieval Methods //
    /////////////////////////////////////

    //List of Continuous Variables
    @SuppressWarnings("unchecked")
    public ArrayList<String> getContinuousVarList() {
        return (ArrayList<String>) _alContinuousVars.clone();
    }

    //todo this method should be renamed: since it generates a boolean var in case it does not exist, with the current naming, it is bug-prone.
    @Deprecated
    public int getBoolVarIndex(String bool_name) {
        return getVarIndex(new BoolDec(bool_name), false);
    }

    public int getCVarIndex(String var) {
        Integer ret = _cvar2ID.get(var);
        if (ret != null) return _cvar2ID.get(var);
        System.err.println("Variable not found! " + var + " in " + _cvar2ID);
        return -1;
    }

    public int getVarIndex(Decision d) {
        return getVarIndex(d, false);
    }

    //
    public XADDNode getNode(int node_id) {
        return _hmInt2Node.get(node_id);
    }

    public XADDNode getExistNode(int node_id) {
        XADDNode n = _hmInt2Node.get(node_id);
        if (n == null) {
            System.err.println("Unexpected Missing node: " + node_id);
            new Exception().printStackTrace();
        }
        return n;
    }

    public String getString(int id) {
        return getString(id, true);
    }

    //collect XADD Variables present within a node
    public HashSet<String> collectVars(int id) {
        XADDNode n = getExistNode(id);
        return n.collectVars();
    }

    // Returns all variables in this XADD
    public int getBranchCount(int id) {
        XADDNode root = getExistNode(id);
        return root.countBranches();
    }

    public int getLeafCount(int id) {
        XADDNode root = getExistNode(id);
        return root.countLeaves(id, new HashSet<Integer>());
    }

    public int getNodeCount(int id) {
        XADDNode root = getExistNode(id);
        return root.collectNodes().size();
    }

    ///////////////////////////////////////
    //         Operation Methods         //
    ///////////////////////////////////////

    public int scalarOp(int dd, double val, int op) {
        int dd_val = getTermNode(new DoubleExpr(val));
        return apply(dd, dd_val, op); // could make non-canonical so have to use apply
    }

    public IntTriple _tempApplyKey = new IntTriple(-1, -1, -1);

    public int apply(int a1, int a2, int op) {
        int ret = applyInt(a1, a2, op);
        if (op == MIN || op == MAX)
            ret = makeCanonical(ret);
        return ret;
    }

    public int applyInt(int a1, int a2, int op) {

        // adding divBranch, -1 if no divison, 1 if branch false, 2 if branch
        // true
        _tempApplyKey.set(a1, a2, op);
        Integer ret = _hmApplyCache.get(_tempApplyKey);
        if (ret != null) {
            return ret;
        }

        // Can we create a terminal node here?
        XADDNode n1 = getExistNode(a1);
        XADDNode n2 = getExistNode(a2);
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
//		else {
//			System.out.println("ComputeTermNode: " + n1.toString() + " " + _aOpNames[op] + " " + n2.toString() + "\n                 = " + getString(ret));
//		}

        _hmApplyCache.put(new IntTriple(a1, a2, op), ret);
        return ret;
    }

    // Computes a terminal node value if possible
    public Integer computeTermNode(int a1, XADDNode n1, int a2, XADDNode n2, int op) {
    	
    	//NaN cannot become valid by operations 
    	if (a1 == NAN || a2 ==NAN){
            return NAN;
    	}
    	
	    //Zero identities first -- critical for getINodeCanon to work
    	if (op == PROD && (a1 == ZERO || a2 == ZERO) ){ //Compare XADD Nodes, there shouldn't be more than one ZERO XADDTNode!
    		return ZERO;
    	}

        //Identities
        if ( (op == SUM && a1==ZERO) || (op == PROD && a1 ==ONE) ) {
            return a2;
        }
        if ( ( (op == SUM || op == MINUS) && a2==ZERO) || ( (op == PROD || op == DIV) && a2 ==ONE) ) {
            return a1;
        }

        // Infinity identities
        // NOTE: * and / can only be evaluated on leaf since 0*inf = 0, but for x!=0 x*inf = inf
        // (Luis) Edit! -1* POS_INF = NEG_INF! 0*Inf is treated in 0 case above, /0 is always undefined/error   
        if (a1 == POS_INF) {
            if (op == SUM || op == MINUS || op == MAX)
                return POS_INF;
            else if (op == MIN)
                return a2;
        } else if (a1 == NEG_INF) {
            // -inf op a2
            if (op == SUM || op == MINUS || op == MIN)
                return NEG_INF;
            else if (op == MAX)
                return a2;
        }
        if (a2 == POS_INF) {
            if (op == SUM || op == MAX)
                return POS_INF;
            else if (op == MINUS)
            	return NEG_INF;
            else if (op == MIN)
                    return a1;
        } else if (a2 == NEG_INF) {
                // -inf op a2
        	if (op == SUM || op == MIN)
                    return NEG_INF;
        	else if (op == MINUS)
            	return POS_INF;
        	else if (op == MAX)
                    return a1;
        }

        // Old INF, ZERO and NAN GetTermNode code
//	    //Zero identities first -- critical for getINodeCanon to work
//    	if (op == PROD && 
//    			(n1 instanceof XADDTNode && ((XADDTNode)n1)._expr.equals(ExprLib.ZERO) ||
//    		    (n2 instanceof XADDTNode && ((XADDTNode)n2)._expr.equals(ExprLib.ZERO)))) {
//    		return ZERO;
//        }
//    	
//        // Check for identities if first operand is terminal
//        if (n1 instanceof XADDTNode) {
//
//            XADDTNode xa1 = (XADDTNode) n1;
//
//            // Check for identity operation value for n1
//            if ((op == SUM && xa1._expr.equals(ExprLib.ZERO))
//                    || (op == PROD && xa1._expr.equals(ExprLib.ONE))) {
//                return a2;
//            }
//
//            // NaN identity for terminal computations (any tnode op NaN = NaN)
//            if (n2 instanceof XADDTNode &&
//            	xa1._expr instanceof DoubleExpr && Double.isNaN(((DoubleExpr)xa1._expr)._dConstVal)) {
//                return NAN;
//            }
//
//            // Infinity identities
//            // NOTE: * and / can only be evaluated on leaf since 0*inf = 0, but for x!=0 x*inf = inf
//            if (xa1._expr instanceof DoubleExpr && Double.isInfinite(((DoubleExpr)xa1._expr)._dConstVal) && ((DoubleExpr)xa1._expr)._dConstVal > 0) {
//               // +inf op a2
//                if (op == SUM || op == MINUS || op == MAX
//                        || ((op == PROD || op == DIV) && (n2 instanceof XADDTNode) && !((XADDTNode) n2).equals(ExprLib.ZERO)))
//                    return POS_INF;
//                else if (op == MIN)
//                    return a2;
//            } else if (xa1._expr instanceof DoubleExpr && Double.isInfinite(((DoubleExpr)xa1._expr)._dConstVal) && ((DoubleExpr)xa1._expr)._dConstVal < 0) {
//                // -inf op a2
//                if (op == SUM || op == MINUS || op == MIN
//                        || ((op == PROD || op == DIV) && (n2 instanceof XADDTNode) && !((XADDTNode) n2).equals(ExprLib.ZERO)))
//                    return getTermNode(ExprLib.NEG_INF);
//                else if (op == MAX)
//                    return a2;
//            }
//        }
//        
//        // Check for identities if second operand is terminal
//        if (n2 instanceof XADDTNode) {
//
//            XADDTNode xa2 = (XADDTNode) n2;
//
//            // Check for identity operation value for n2
//            if ((op == SUM && xa2._expr.equals(ExprLib.ZERO))
//                    || (op == PROD && xa2._expr.equals(ExprLib.ONE))
//                    || (op == MINUS && xa2._expr.equals(ExprLib.ZERO))
//                    || (op == DIV && xa2._expr.equals(ExprLib.ONE))) {
//                return a1;
//            }
//
//            // NaN identity for terminal computations (any tnode op NaN = NaN)
//            if (n1 instanceof XADDTNode &&
//                xa2._expr instanceof DoubleExpr && Double.isNaN(((DoubleExpr)xa2._expr)._dConstVal)) {
//	            return NAN;
//	        }
//
//            // Infinity identities
//            if (xa2._expr instanceof DoubleExpr && Double.isInfinite(((DoubleExpr)xa2._expr)._dConstVal) && ((DoubleExpr)xa2._expr)._dConstVal > 0) { // (xa2._expr.equals(POS_INF)) {
//                // a1 op +inf
//                if (op == SUM || op == MAX || (op == PROD && (n1 instanceof XADDTNode) && !((XADDTNode) n1).equals(ZERO)))
//                    return POS_INF;
//                    // Not sure how to handle minus and div, ignoring for now
//                else if (op == MIN)
//                    return a1;
//            } else if (xa2._expr instanceof DoubleExpr && Double.isInfinite(((DoubleExpr)xa2._expr)._dConstVal) && ((DoubleExpr)xa2._expr)._dConstVal < 0) {
//                // a1 op -inf
//                if (op == SUM || op == MIN || (op == PROD && (n1 instanceof XADDTNode) && !((XADDTNode) n1).equals(ZERO)))
//                    return NEG_INF;
//                    // Not sure how to handle minus and div, ignoring for now
//                else if (op == MAX)
//                    return a1;
//            }
//        }
        
        // Handle result if both operands are terminals and one of the special
        // identities above did not hold
        if ((n1 instanceof XADDTNode) && (n2 instanceof XADDTNode)) {
            XADDTNode xa1 = (XADDTNode) n1;
            XADDTNode xa2 = (XADDTNode) n2;

            // Operations: +,-,*,/
            if ((op != MAX) && (op != MIN)) {
                //System.out.println("Returning: " + new OperExpr(ArithOperation.fromXADDOper(op), xa1._expr, xa2._expr));
                return getTermNode(new OperExpr(ArithOperation.fromXADDOper(op), xa1._expr, xa2._expr));
            }

            CompExpr comp = new CompExpr(CompOperation.LT_EQ, xa1._expr, xa2._expr);
            Decision d = new ExprDec(comp);

            // Get canonical version of decision
            int var_index = getVarIndex(d, true);

            int node1, node2;
            node1 = getTermNode(xa1._expr);
            node2 = getTermNode(xa2._expr);

            // Operations: min/max -- return a decision node
            return getINode(var_index, op == MAX ? node1 : node2,
                    op == MAX ? node2 : node1);
        }

        return null;
    }

    ////////////////////////
    // Evaluation methods //
    ////////////////////////
    public Boolean evaluateDecision(Decision d, HashMap<String, Boolean> bool_assign, HashMap<String, Double> cont_assign) {
        if (d instanceof TautDec)
            return ((TautDec) d)._bTautology;
        else if (d instanceof BoolDec)
            return bool_assign.get(((BoolDec) d)._sVarName);
        else if (d instanceof ExprDec) {
        	if (SHOW_DECISION_EVAL) { System.out.println(" - " + ((ExprDec) d)._expr + ": " + ((ExprDec) d)._expr.evaluate(cont_assign));}
        	return  ((ExprDec) d)._expr.evaluate(cont_assign);            
        }
        return null;
    }
    
    public Double evaluate(int node_id, HashMap<String, Boolean> bool_assign, HashMap<String, Double> cont_assign) {
        // Get root
        XADDNode n = getExistNode(node_id);

        // Traverse decision diagram until terminal found
        while (n instanceof XADDINode) {
            XADDINode inode = (XADDINode) n;
            Boolean branch_high = evaluateDecision(_alOrder.get(inode._var), bool_assign, cont_assign);
            
            // Not all required variables were assigned
            if (branch_high == null)
                return null;

            // Advance down to next node
            n = getExistNode(branch_high ? inode._high : inode._low);
        }

        // Now at a terminal node so evaluate expression
        XADDTNode t = (XADDTNode) n;
        return t._expr.evaluate(cont_assign);
    }

    public double evaluateExpr(ArithExpr e, double assign[]) {
        HashMap<String, Double> cont_assign = new HashMap<String, Double>();
        for (int i = 0; i < _cvar2ID.size(); i++) {
            cont_assign.put(_alContinuousVars.get(i), assign[i]);
        }
        return e.evaluate(cont_assign);
    }

    //Partial Evaluation, evaluates until a expression is obtained, but does not evaluate the expression
    public Integer getLeaf(int node_id, HashMap<String, Boolean> bool_assign, HashMap<String, Double> cont_assign) {
        // Get root
    	int id = node_id;
    	XADDNode n = getExistNode(node_id);
        
        // Traverse decision diagram until terminal found
        while (n instanceof XADDINode) {
        	XADDINode inode = (XADDINode) n;
        	Boolean branch_high = evaluateDecision(_alOrder.get(inode._var), bool_assign, cont_assign);
            // Not all required variables were assigned
            if (branch_high == null)	return null;
            // Advance down to next node
            id = branch_high ? inode._high : inode._low;
            n = getExistNode(id);
        }
        
        // Now at a terminal node so evaluate expression
        return id;
    }
    
    ///////////////////////////////////////
    //        Reduce  Methods           //
    //////////////////////////////////////

    //Reduce Operation
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
            System.err.println("Illegal op-code '" + op + "' for var id: "
                    + var_id);
            new Exception().printStackTrace();
            System.exit(1);
        }

        Integer ret = null;
        XADDNode n = getExistNode(node_id);


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
            } else if (op == SUM || op == PROD) { // op \in {MINUS, DIV} not
                // commutative
                // not obvious if low or high comes first
                ret = apply(low, high, op); // may not be canonical, but will be
                // fixed
            } else {
                System.out
                        .println("ERROR: id:" + op + "/ name:" + _aOpNames[op] + " expected in node cache, but not found!  (Or illegal op.)");
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

    // NOTE: can only linearize decisions that have one quadratic variable,
    //       otherwise have to complete the square symbolically and take
    //       a symbolic square root of an XADD... OK except that no longer
    //       a polynomial for purposes of current code.
    // Linearize unidimensional quadratics
    public int reduceLinearize(int node_id) {
        node_id = reduceLinearizeInt(node_id);
        return makeCanonical(node_id); // Redundant for current code, but may add new linearizations
    }

    private int reduceLinearizeInt(int node_id) {

        Integer ret = null;
        XADDNode n = getExistNode(node_id);

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

        int low = reduceLinearizeInt(inode._low);
        int high = reduceLinearizeInt(inode._high);

        Decision d = _alOrder.get(inode._var);

        // For now we'll only do linearization of quadratic decisions
        Integer xadd_dec_true = linearizeQuadDecision(d);
        if (xadd_dec_true == null)
            ret = getINode(inode._var, low, high);
        else {
            int xadd_dec_false = applyInt(ONE, xadd_dec_true, MINUS); // can use applyInt rather than apply
            int true_half = applyInt(xadd_dec_true, high, PROD); // Note: this enforces canonicity so
            int false_half = applyInt(xadd_dec_false, low, PROD); // can use applyInt rather than apply
            ret = applyInt(true_half, false_half, SUM);
        }

        // Put return value in cache and return
        _hmReduceCache.put(new IntTriple(node_id, -1, LINEARIZE), ret);
        return ret;
    }

    // Returns XADD for true branch indicator of decision, otherwise null
    public Integer linearizeQuadDecision(Decision d) {

        if (!(d instanceof ExprDec))
            return null;

        ExprDec e = (ExprDec) d;
        OperExpr lhs = (OperExpr) e._expr._lhs;
        if (lhs._type != ArithOperation.SUM)
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

                if (a instanceof OperExpr && ((OperExpr) a)._type == ArithOperation.PROD) {

                    // Count order of polynomial in var
                    int count_var = 0;
                    Double coef = null;
                    for (ArithExpr a_sub : ((OperExpr) a)._terms) {
                        if (a_sub.equals(varExpr))
                            count_var++;
                        else if (a_sub instanceof DoubleExpr)
                            coef = ((coef == null) ? ((DoubleExpr) a_sub)._dConstVal
                                    : coef * ((DoubleExpr) a_sub)._dConstVal);
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
                    const_coef += ((DoubleExpr) a)._dConstVal;
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
                //      LOG_SYMB (x + d/2) flip(COMP) -SQRT((d/2)^2 + e)
                //    [else]
                //      substitute x/0 then makeCanonical
                //      (to find out whether true or false since for any x, always on same side)
                //
                // LOG_SYMB:
                //   COMP=>,>= / a > 0: LOG_SYMB = OR
                //   COMP=<,<= / a > 0: LOG_SYMB = AND
                //   COMP=>,>= / a < 0: LOG_SYMB = AND
                //   COMP=<,<= / a < 0: LOG_SYMB = OR
                boolean is_and = (quad_coef < 0 && (e._expr._type == CompOperation.GT || e._expr._type == CompOperation.GT_EQ))
                        || (quad_coef > 0 && (e._expr._type == CompOperation.LT || e._expr._type == CompOperation.LT_EQ));

                double var_d = linear_coef / quad_coef;
                double var_e = -const_coef / quad_coef;

                //Build expressions for first and second inequality
                CompOperation comp_oper = e._expr._type;
                if (quad_coef < 0)
                    comp_oper = CompExpr.flipCompOper(comp_oper);
                CompOperation flip_comp_oper = CompExpr.flipCompOper(comp_oper);
                OperExpr lhs_expr = new OperExpr(ArithOperation.SUM, new VarExpr(var), new DoubleExpr(var_d / 2d));
                double rhs_pre_sqrt = var_e + var_d * var_d / 4d;
                if (rhs_pre_sqrt < 0) {
                    // Check for imaginary roots... quadratic never crosses zero
                    HashMap<String, ArithExpr> subst = new HashMap<String, ArithExpr>();
                    subst.put(var, ExprLib.ZERO);
                    TautDec new_dec = (TautDec) (new ExprDec(e._expr.substitute(subst)).makeCanonical());
                    return new_dec._bTautology ? ONE : ZERO; // will be multiplied by true branch
                }
                double rhs_const = Math.sqrt(rhs_pre_sqrt);
                int var1_id = getVarIndex(new ExprDec(new CompExpr(comp_oper, lhs_expr, new
                        DoubleExpr(rhs_const))), true);
                int var2_id = getVarIndex(new ExprDec(new CompExpr(flip_comp_oper, lhs_expr, new
                        DoubleExpr(-rhs_const))), true);

                // Build XADD indicator from expressions
                // Note: disjunction a v b = 1 - (1 - a)*(1 - b)
                int ret_xadd = -1;
                if (is_and) {
                    int expr1_xadd = getINode(var1_id, /* low */ZERO, /* high */ONE);
                    int expr2_xadd = getINode(var2_id, /* low */ZERO, /* high */ONE);
                    ret_xadd = apply(expr1_xadd, expr2_xadd, PROD);
                } else {
                    int expr1_xadd = getINode(var1_id, /* low */ONE, /* high */ZERO);
                    int expr2_xadd = getINode(var2_id, /* low */ONE, /* high */ZERO);
                    ret_xadd = apply(ONE, apply(expr1_xadd, expr2_xadd, PROD), MINUS);
                }
                //System.out.println("LINEARIZE -- started with: " + e + "... returning\n" +
                //		getString(ret_xadd));
                //System.exit(1);
                return ret_xadd;
            }
        } // Done processing 'var'

        return null; // We didn't find a quadratic before this
    }

    //Annotate XADD
    IntPair _tempReduceAnnotateKey = new IntPair(-1, -1);

    public int annotateXADD(int node_id, Object annotation) {
        Integer ret = null;
        XADDNode node = getExistNode(node_id);

        // Directly annotate terminal nodes only
        if ((node instanceof XADDTNode)) {

            XADDTNode tnode = (XADDTNode) node;
            return getTermNode(tnode._expr, annotation);
        }

        // If its an internal node, first check the annotate cache
        _tempReduceAnnotateKey.set(node_id, annotation.hashCode());
        if ((ret = _hmReduceAnnotateCache.get(_tempReduceAnnotateKey)) != null)
            return ret;

        // Not annotated before so recursively annotate
        XADDINode inode = (XADDINode) node;
        int low = annotateXADD(inode._low, annotation);
        int high = annotateXADD(inode._high, annotation);
        ret = getINode(inode._var, low, high);
        _hmReduceAnnotateCache.put(new IntPair(node_id, annotation.hashCode()), ret);
        return ret;
    }

    //Round coefficients of all expressions of XADD
    public IntTriple _tempReduceKey = new IntTriple(-1, -1, -1);

    public int reduceRound(int node_id) {

        Integer ret = null;
        XADDNode n = getExistNode(node_id);

        if (n instanceof XADDTNode) {
            ArithExpr new_expr = ((XADDTNode) n)._expr.round();
            return getTermNode(new_expr);
        }

        // If its an internal node, check the reduce cache
        _tempReduceKey.set(node_id, -1, ROUND);
        if ((ret = _hmReduceCache.get(_tempReduceKey)) != null)
            return ret;


        XADDINode inode = (XADDINode) n;

        // Round decision if possible
        int var = inode._var;
        Decision d = _alOrder.get(var);
        if (d instanceof ExprDec) {
            ExprDec new_expr_dec = ((ExprDec) d).round();
            var = getVarIndex(new_expr_dec, true);
        }
        int low = reduceRound(inode._low);
        int high = reduceRound(inode._high);

        // For now we'll only do linearization of quadratic decisions
        ret = getINode(var, low, high);
        // Put return value in cache and return
        _hmReduceCache.put(new IntTriple(node_id, -1, ROUND), ret);
        return ret;
    }

    ///////////////////////////////////////////////////////////////
    // Verify feasibility and redundancy of all paths in the XADD
    ///////////////////////////////////////////////////////////////

    // Consistency and Redundancy Checking - ReduceLP
    public int reduceLP(int node_id) {
        return RLPContext.reduceLP(node_id);
    }

    public int reduceLP(int node_id, boolean redun) {
        return RLPContext.reduceLP(node_id, redun);
    }

    // Linear XADD Prunning
    public int linPruneRel(int node_id, double error) {
        LinearApproximationMethod linPrune = new LinearApproximationMethod(node_id, this);
        node_id = linPrune.linPruneRel(node_id, error);
        return reduceLP(node_id);
    }

    // Linear Maximization
    public double linMaxVal(int node_id) {
        LinearXADDMethod linMax = new LinearXADDMethod(node_id, this);
        return linMax.linMaxVal(node_id);
    }

    public double linMaxDiff(int id1, int id2) {
        int dif1 = reduceLP(apply(id1, id2, XADD.MINUS));
        int dif2 = reduceLP(apply(id2, id1, XADD.MINUS));
        return Math.max(linMaxVal(dif1), linMaxVal(dif2));
    }

    ////////////////////////////////////////
    //          Cache Maintenance         //
    ////////////////////////////////////////

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
        System.out.print("[FLUSHING CACHES... " + (_hmNode2Int.size() + _hmInt2Node.size()) + " nodes -> ");

        // Can always clear these
        _hmReduceCache.clear();
        _hmReduceCanonCache.clear();
        _hmReduceLeafOpCache.clear();
        _hmApplyCache.clear();
        _hmINode2Vars.clear();
        _hmReduceAnnotateCache.clear();

        RLPContext.flushImplications();

        // Set up temporary alternates to these HashMaps
        _hmNode2IntNew = new HashMap<XADDNode, Integer>();
        _hmInt2NodeNew = new HashMap<Integer, XADDNode>();

        // Copy over 'special' nodes then set new maps
        //System.out.println(_hsSpecialNodes);
        for (Integer n : _hsSpecialNodes) {
            copyInNewCacheNode(n);
        }
        _hmNode2Int = _hmNode2IntNew;
        _hmInt2Node = _hmInt2NodeNew;
        createStandardNodes();

        //_hmNode2Int.clear();
        //_hmInt2Node.clear();

        System.out.println((_hmNode2Int.size() + _hmInt2Node.size()) + " nodes]");
        
        Runtime.getRuntime().gc();

    }

    private void copyInNewCacheNode(Integer id) {

        if (_hmInt2NodeNew.containsKey(id)) {
            return;
        }
        Object node = getExistNode(id);
        if (node instanceof XADDINode) {
            _hmInt2NodeNew.put(id, (XADDINode) node);
            _hmNode2IntNew.put((XADDINode) node, id);
            copyInNewCacheNode(((XADDINode) node)._high);
            copyInNewCacheNode(((XADDINode) node)._low);
        } else if (node instanceof XADDTNode) {
            _hmInt2NodeNew.put(id, (XADDTNode) node);
            _hmNode2IntNew.put((XADDTNode) node, id);

        }
    }

    //////////////////////////////////////
    //    Information Display Methods   //
    //////////////////////////////////////

    public void showBounds() {
        System.out.print("Cont Variables bounds: ");
        for (Map.Entry<String, Integer> me : _cvar2ID.entrySet()) {
            String cvar = me.getKey();
            Integer var_id = me.getValue();
            System.out.format("%s in [ %5.3e:%5.3e], ", cvar, lowerBounds[var_id], upperBounds[var_id]);
        }
        System.out.println("end;");
    }

    // Create graphical version of XADD
    public Graph getGraph(int id) {
        Graph g = new Graph(true /* directed */, false /* bottom-to-top */,
                false /* left-to-right */, true /* multi-links */);
        XADDNode root = getExistNode(id);
        root.toGraph(g, id);
        return g;
    }

    public void showGraph(int node, String label) {
        Graph g2 = getGraph(node);
        g2.addNode("_temp_");
        g2.addNodeLabel("_temp_", label);
        g2.addNodeShape("_temp_", "box");
        g2.addNodeStyle("_temp_", "filled");
        g2.addNodeColor("_temp_", "gold1");
        g2.launchViewer(1300, 770);
    }

    public void exportXADDToFile(int id, String filename) {
        PrintStream ps = null;
        try {
            ps = new PrintStream(new FileOutputStream(filename));
            exportXADD(id, ps);
            ps.close();
        } catch (Exception e) {
            System.err.println("WARNING: could not export XADD to " + filename);
            ps.close();
        }
    }

    // Export XADD in DAG format (not as tree)
    public void exportXADD(int id, PrintStream ps) {
        HashSet<Integer> nodes_explored = new HashSet<Integer>();
        HashSet<Integer> dec_explored = new HashSet<Integer>();
        exportXADDInt(id, ps, nodes_explored, dec_explored);
        ps.println("F\t" + nodes_explored.size() + "\t" + dec_explored.size() + "\t(#nodes and #decisions)");
    }

    // Exports XADD node structure, children before parents
    private void exportXADDInt(int id, PrintStream ps, HashSet<Integer> nodes_explored, HashSet<Integer> dec_explored) {

        if (nodes_explored.contains(id))
            return;
        nodes_explored.add(id);

        XADDNode node = getExistNode(id);
        if (node instanceof XADDTNode) {

            // Export terminal node contents
            XADDTNode tnode = (XADDTNode) node;
            ps.println("T\t" + id + "\t" + tnode._expr + "\t" + tnode._annotate);

        } else if (node instanceof XADDINode) {

            XADDINode inode = (XADDINode) node;

            // Export children first
            exportXADDInt(inode._low, ps, nodes_explored, dec_explored);
            exportXADDInt(inode._high, ps, nodes_explored, dec_explored);

            Decision d = _alOrder.get(inode._var);

            // Export decision contents (only first time decision encountered)
            if (!dec_explored.contains(inode._var)) {
                dec_explored.add(inode._var);
                if (d instanceof BoolDec) {
                    BoolDec b = (BoolDec) d;
                    ps.println("B\t" + inode._var + "\t" + b._sVarName);
                } else if (d instanceof ExprDec) {
                    ExprDec e = (ExprDec) d;
                    ps.println("E\t" + inode._var + "\t" + e._expr);
                } else {
                    System.err.println("ERROR: could not export unknown XADD decision type: " + d);
                    System.exit(1);
                }
            }

            // Export internal node structure
            ps.println("I\t" + id + "\t" + inode._var + "\t" + inode._low + "\t" + inode._high);

        } else {
            System.err.println("ERROR: could not export unknown XADD node type:" + node);
            System.exit(1);
        }
    }

    public int importXADDFromFile(String filename) {
        BufferedReader br = null;
        int ret = -1;
        try {
            br = new BufferedReader(new FileReader(filename));
            ret = importXADD(br);
            br.close();
        } catch (Exception e) {
            System.err.println("WARNING: could not export XADD to " + filename);
            try {
                br.close();
            } catch (Exception e2) {
            }
        }
        return ret;
    }

    // Read XADD in DAG format (not from tree... dec order does not matter, will build canonical XADD internally)
    public int importXADD(BufferedReader br) throws Exception {
        // Go through each line, instantiating the appropriate node
        // Need to maintain a remap from what the file ID is and what the new XADD is
        HashMap<Integer, Integer> old2new_node = new HashMap<Integer, Integer>();
        HashMap<Integer, Integer> old2new_dec = new HashMap<Integer, Integer>();
        String line = null;
        int file_id = -1, xadd_id = -1;
        while ((line = br.readLine()) != null && !line.startsWith("F")) {
            String[] split = line.split("\t");
            file_id = new Integer(split[1]);
            if (split[0].equals("T")) {
                //T	2	0	null
                //T	234	(-0.5 + (0.01 * x2))	null
                ArithExpr expr = ArithExpr.ParseArithExpr(split[2]);
                Object annotation = (split[3].equals("null") ? null : split[3]);
                xadd_id = getTermNode(expr, annotation);
                old2new_node.put(file_id, xadd_id);

            } else if (split[0].equals("B")) {
                //B	1	tp
                xadd_id = getVarIndex(new BoolDec(split[2]), true);
                old2new_dec.put(file_id, xadd_id);

            } else if (split[0].equals("E")) {
                //E	39	(-1 + (0.000217 * x1) + (0.000435 * x2)) > 0
                CompExpr comp_expr = CompExpr.ParseCompExpr(split[2]);
                xadd_id = getVarIndex(new ExprDec(comp_expr), true);
                old2new_dec.put(file_id, xadd_id);

            } else if (split[0].equals("I")) {

                // We assume all children and decisions are exported before parents,
                // hence we've built the children, we just have to translate IDs

                //	file_id	var	low	high
                //I	1186	3	709	1185
                int var = old2new_dec.get(new Integer(split[2]));
                int low = old2new_node.get(new Integer(split[3]));
                int high = old2new_node.get(new Integer(split[4]));
                xadd_id = getINodeCanon(var, low, high); // Corrects for ordering w.r.t. this XADD
                old2new_node.put(file_id, xadd_id);
            }
        }
        return old2new_node.get(file_id); // Return XADD ID of last node ID which must be XADD root
    }

    //Create string version of XADD
    public String getString(int id, boolean format) {
        XADDNode root = getExistNode(id);
        int num_nodes = getNodeCount(id);
        if (num_nodes > 30)
            return "[XADD " + id + " contains " + num_nodes + " nodes... too large to print]";
        else
            return root.toString(format);
    }

    // Quick cache snapshot
    public void showCacheSize() {
        System.out.println("APPLY CACHE:    " + _hmApplyCache.size());
        System.out.println("REDUCE CACHE:   " + _hmReduceCache.size());
        System.out.println("REDUCE CACHE C: " + _hmReduceCanonCache.size());
        System.out.println("REDUCE CACHE L: " + _hmReduceLeafOpCache.size());
        System.out.println("REDUCE CACHE A: " + _hmReduceAnnotateCache.size());
        System.out.println("INODE CACHE 1:  " + _hmNode2Int.size());
        System.out.println("INODE CACHE 2:  " + _hmInt2Node.size());
        System.out.println("VAR CACHE 2:    " + _hmINode2Vars.size());

        RLPContext.showImplicationCache();
    }

    public void clearMarks() {
        _markCounter++;
    }

    public static String indent(int depth) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i <= depth; i++)
            sb.append(STRING_TAB);
        return sb.toString();
    }

    ///////////////////////////////////////
    // 			Mask Functions			 //
    ///////////////////////////////////////    
	
    public Integer createMask(Integer id,
	HashMap<String, Boolean> bool_assign,
	HashMap<String, Double> cont_assign, Integer mask_id) {
    	return reduceMask(id, bool_assign, cont_assign, mask_id, new HashMap<Integer,Integer>());
    }
    
    private Integer reduceMask(Integer id,
			HashMap<String, Boolean> bool_assign,
			HashMap<String, Double> cont_assign, Integer mask_id, HashMap<Integer,Integer> maskCache) {

    		Integer ret = null;
    		if ( (ret = maskCache.get(id))!= null){
    			return ret;
    		}
    			
    		XADDNode n = getExistNode(id);

    		if (n instanceof XADDTNode) {
    			ret = id;
    		}
    		else{
    			if (!(n instanceof XADDINode)) { System.err.println("Invalid node, neither TNode nor INode:\n"+n.toString()); } 
                // Traverse decision diagram until terminal found
    			XADDINode inode = (XADDINode) n;
                Boolean branch_high = evaluateDecision(_alOrder.get(inode._var), bool_assign, cont_assign);
                
                
                if (branch_high == null){
                	//Not all required variables were assigned, mask both paths
                	ret = getINode(inode._var, reduceMask(inode._low, bool_assign, cont_assign, mask_id, maskCache), 
                			reduceMask(inode._high, bool_assign, cont_assign,mask_id, maskCache));
                }
                else{                
                	ret = branch_high ? getINode(inode._var, mask_id, reduceMask(inode._high, bool_assign, cont_assign,mask_id, maskCache)):
                						getINode(inode._var, reduceMask(inode._low, bool_assign, cont_assign,mask_id, maskCache), mask_id);
                }
                
            }
    		maskCache.put(id, ret);
            return ret;
    }

//    public Integer createPosInfMask(Integer id,HashMap<String, Boolean> bool_assign, HashMap<String, Double> cont_assign){
//    	return createMask(id, bool_assign, cont_assign, POS_INF);
//    }
    
//    public Integer createPosInfMask(Integer id,
//			HashMap<String, Boolean> bool_assign,
//			HashMap<String, Double> cont_assign) {
//
//    		XADDNode n = getExistNode(id);
//
//            // Traverse decision diagram until terminal found
//    		if (n instanceof XADDINode) {
//                XADDINode inode = (XADDINode) n;
//                Boolean branch_high = evaluateDecision(_alOrder.get(inode._var), bool_assign, cont_assign);
//                
//                //Not all required variables were assigned
//                if (branch_high == null){
//                	return getINode(inode._var, createPosInfMask(inode._low, bool_assign, cont_assign), createPosInfMask(inode._high, bool_assign, cont_assign));
//                }
//                
//                // Advance down to next node
//                return branch_high ? getINode(inode._var, POS_INF, createPosInfMask(inode._high, bool_assign, cont_assign)):
//                					 getINode(inode._var, createPosInfMask(inode._low, bool_assign, cont_assign), POS_INF);
//            }
//    		
//            // else id is a TNode, trivial mask
//            return id;
//    }

    
    
    ///////////////////////////////////////
    // 			Helper Classes 			 //
    ///////////////////////////////////////

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

    // TODO: Move all leaf operation classes to external helper classes

    // This internal class is intended to be used to specify inline operations to perform at the
    // leaves of an XADD... the operation returns a new XADD for the leaf.
    public abstract class XADDLeafOperation {
        // 'decisions' and 'decision_values' are parallel arrays indicating the path taken to 'leaf_val'
        // 'xadd' is the XADD being subsituted for 'var'
        public abstract int processXADDLeaf(ArrayList<Decision> decisions,
                                            ArrayList<Boolean> decision_values, ArithExpr leaf_val);

        public abstract boolean isCacheable();
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Class of XADDLeafMax that preforms max over a variable on the leaves subject
    // to the constraints encountered to reach those leaves.  The leaf max is over
    // the LB, UB or any root of the polynomial.
    ////////////////////////////////////////////////////////////////////////////////

    public class XADDLeafMinOrMax extends XADDLeafOperation {

        public int _runningResult; // XADD for the running max of all leaf substitutions
        double _lowerBound, _upperBound;
        String _minOrMaxVar;
        String _sOpName;
        boolean _bIsMax;
        ArrayList<String> _contVars;
        PrintStream _log = null;

        public XADDLeafMinOrMax(String min_or_max_var, double lower_bound, double upper_bound, boolean is_max, PrintStream ps) {
            _minOrMaxVar = min_or_max_var.intern();
            _bIsMax = is_max;
            _sOpName = _bIsMax ? "MAX" : "MIN";
            _lowerBound = lower_bound;
            _upperBound = upper_bound;
            _runningResult = -1;
            _log = ps;
        }

        // TODO: revisit whether caching is possible, or in what circumstances
        public boolean isCacheable() {
            return false;
        }

        public int processXADDLeaf(ArrayList<Decision> decisions, ArrayList<Boolean> decision_values, ArithExpr leaf_val) {
            if (VERBOSE_MIN_MAX) _log.println("=============== leaf: " + _sOpName + "================");
            if (VERBOSE_MIN_MAX) _log.println("Current node: " + leaf_val);
            if (VERBOSE_MIN_MAX)
                _log.println("Decisions to get to get here: " + decisions + " = " + decision_values + "\n===\n");

            // Bound management
            ArrayList<ArithExpr> lower_bound = new ArrayList<ArithExpr>();
            ArrayList<ArithExpr> upper_bound = new ArrayList<ArithExpr>();
            lower_bound.add(new DoubleExpr(_lowerBound));
            upper_bound.add(new DoubleExpr(_upperBound));

            // Multiply these in later
            HashMap<Decision, Boolean> target_var_indep_decisions = new HashMap<Decision, Boolean>();

            // First compute the upper and lower bounds and var-independent constraints
            // from the decisions
            for (int i = 0; i < decisions.size(); i++) {
                Decision d = decisions.get(i);
                Boolean is_true = decision_values.get(i);
                CompExpr comp = null;
                if (d instanceof BoolDec) {
                    target_var_indep_decisions.put(d, is_true);
                    continue;
                } else if (d instanceof ExprDec) {
                    ExprDec ed = (ExprDec) d;
                    comp = ed._expr;
                } else {
                    _log.println("processXADDLeaf: Unsupported decision type '" + d + "'");
                    System.exit(1);
                }

                // Check that comparison expression is normalized
                if (!comp._rhs.equals(ExprLib.ZERO)) {
                    _log.println("processXADDLeaf: Expected RHS = 0 for '" + comp + "'");
                    System.exit(1);
                }

                // Takes ArithExpr expr1 linear in var, returns (coef,expr2) where expr1 = coef*x + expr2
                CoefExprPair p = comp._lhs.removeVarFromExpr(_minOrMaxVar);
                ArithExpr lhs_isolated = p._expr;
                double var_coef = p._coef;
                if (VERBOSE_MIN_MAX) _log.println("Pre: " + comp + " == " + is_true + ", int var [" + _minOrMaxVar + "]"
                        + "\nLHS isolated: " + lhs_isolated + "\n ==>  " + var_coef + " * " + _minOrMaxVar + ((var_coef == 0d) ? " [independent]" : ""));

                if (var_coef == 0d) {
                    target_var_indep_decisions.put(d, is_true);
                    continue;
                }

                // We have var_coef*x + lhs_isolated {<,<=,>,>=} 0
                // ... need to get x {<,<=,>,>=} 1/var_coef * lhs_isolated
                //     (and inequality gets flipped if var_coef is negative)
                boolean flip_comparison = (var_coef < 0d) && (comp._type != CompOperation.EQ) && (comp._type != CompOperation.NEQ);
                ArithExpr new_rhs = (ArithExpr) new OperExpr(ArithOperation.MINUS, ExprLib.ZERO, new OperExpr(ArithOperation.PROD, new DoubleExpr(
                        1d / var_coef), lhs_isolated)).makeCanonical();

                // Divide through by coef (pos or neg)
                // - if coef neg, flip expression
                // - if decision neg, flip expression
                // - if both, don't flip
                CompOperation comp_oper = comp._type;
                if ((/* negated */!is_true && !flip_comparison) || (/* not negated */is_true && flip_comparison)) {
                    comp_oper = CompExpr.flipCompOper(comp_oper);
                }

                // Now we have (x {<,<=,>,>=} expr)... mark each as lower/upper bound:
                // - lower bounds: x > f(y), x >= f(y)
                // - upper bounds: x < f(z), x <= f(z)
                if (comp_oper == CompOperation.GT || comp_oper == CompOperation.GT_EQ)
                    lower_bound.add(new_rhs);
                else if (comp_oper == CompOperation.LT || comp_oper == CompOperation.LT_EQ)
                    upper_bound.add(new_rhs);
                else {
                    _log.println("Cannot currently handle: "
                            + new CompExpr(comp_oper, new VarExpr(_minOrMaxVar), new_rhs));
                    _log.println("Note: = triggers substitution, not sure how to handle ~=");
                    new Exception().printStackTrace();
                    System.exit(1);
                }
            }

            // Now explicitly compute lower and upper bounds as XADDs
            //
            // If these are polynomials, must go to +/- infinity at limits so cannot
            // be used to approximate cdfs. Hence we must assume that there will always
            // be limits on the polynomial functions implicit in the bounds.
            if (VERBOSE_MIN_MAX) _log.println("Lower bounds: " + lower_bound);
            int xadd_lower_bound = -1;
            for (ArithExpr e : lower_bound) // Lower bound is max of all lower bounds
                xadd_lower_bound = (xadd_lower_bound == -1) ? getTermNode(e)
                        : apply(xadd_lower_bound, getTermNode(e), MAX);

            if (VERBOSE_MIN_MAX) _log.println("Upper bounds: " + upper_bound);
            int xadd_upper_bound = -1;
            for (ArithExpr e : upper_bound) // Upper bound is min of all upper bounds
                xadd_upper_bound = (xadd_upper_bound == -1) ? getTermNode(e)
                        : apply(xadd_upper_bound, getTermNode(e), MIN);

            // Build all constraints for the maximization
            for (ArithExpr e1 : upper_bound) {
                for (ArithExpr e2 : lower_bound) {
                    CompExpr ce = new CompExpr(CompOperation.GT, e1, e2);
                    ExprDec ed = new ExprDec(ce);
                    target_var_indep_decisions.put(ed, Boolean.TRUE);
                }
            }

            // Display lower and upper bounds
            if (VERBOSE_MIN_MAX) _log.println("** Lower bound:\n" + getString(xadd_lower_bound));
            if (VERBOSE_MIN_MAX) _log.println("** Upper bound:\n" + getString(xadd_upper_bound));

            // Determine whether we need to handle the quadratic case, if so,
            // root will be set to a non-null evaluation
            ArithExpr root = null;
            int highest_order = leaf_val.determineHighestOrderOfVar(_minOrMaxVar);
            if (highest_order > 2) {
                _log.println("XADDLeafMax: Cannot currently handle expressions higher than order 2 in " + _minOrMaxVar + ": " + leaf_val);
                System.exit(1);
            } else if (highest_order == 2) {
                ArithExpr first_derivative = leaf_val.differentiateExpr(_minOrMaxVar);

                // Takes ArithExpr expr1 linear in var, returns (coef,expr2) where expr1 = coef*x + expr2
                // setting expr1 = coef*x + expr2 = 0 then x = -expr2/coef
                CoefExprPair p2 = first_derivative.removeVarFromExpr(_minOrMaxVar);

                root = (ArithExpr) (new OperExpr(ArithOperation.MINUS, ExprLib.ZERO, new OperExpr(ArithOperation.PROD, new DoubleExpr(
                        1d / p2._coef), p2._expr)).makeCanonical());
            }

            // Substitute lower and upper bounds into leaf
            int eval_lower = substituteXADDforVarInArithExpr(leaf_val, _minOrMaxVar, xadd_lower_bound);
            int eval_upper = substituteXADDforVarInArithExpr(leaf_val, _minOrMaxVar, xadd_upper_bound);

            // Display lower and upper bound substitution
            if (VERBOSE_MIN_MAX) _log.println("** Substitute in: " + leaf_val);
            if (VERBOSE_MIN_MAX) _log.println("** Lower bound sub:\n" + getString(eval_lower));
            if (VERBOSE_MIN_MAX) _log.println("** Upper bound sub:\n" + getString(eval_upper));

            // We don't know which of UB/LB substitution is maximal so we take the "case"-max
            // ... if there were nonlinearities in leaf, then substitution leads to nonlinear
            //     function which ends up in decisions... need linear decisions so
            //     call reduceLinearize to complete-the-square for quadratic variables
            //     and linearize the decisions (linearize includes makeCanonical)
            // ??? need to avoid case where max leads to an illegal pruning -- occurs???
            //     e.g., could an unreachable constant prune out another reachable node?
            //     (don't think this can happen... still in context of unreachable constraints)
            int min_max_eval = apply(eval_upper, eval_lower, _bIsMax ? MAX : MIN); // handle min and max
            min_max_eval = reduceLinearize(min_max_eval);

            // TODO: investigate... sometimes we are getting a quadratic decision below that should have been linearized!
            // TODO: Yes, in the simple Rover-nonlinear2 we get non linear constraints! !!
            // this -> showGraph(min_max_eval, "afterLinearize"); shows nonlinear XADDs after Linearize,
            min_max_eval = reduceLP(min_max_eval); // Result should be canonical
            if (VERBOSE_MIN_MAX)
                _log.println(_sOpName + " of LB and UB (reduce/linearize): " + getString(min_max_eval));

            // NOTE: Constraints on root have to be multiplied in here, not at end.  -Scott
            if (root != null) {

                int eval_root = substituteXADDforVarInArithExpr(leaf_val, _minOrMaxVar, getTermNode(root));
                if (VERBOSE_MIN_MAX) _log.println("root substitute: " + getString(eval_root));

                // Now incorporate constraints into int_eval, make result canonical
                for (ArithExpr ub : upper_bound) {
                    CompExpr ce = new CompExpr(CompOperation.LT_EQ, root, ub);
                    int ub_xadd = _bIsMax
                            ? getVarNode(new ExprDec(ce), Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)  // +inf gets min'ed to eval_root
                            : getVarNode(new ExprDec(ce), Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY); // -inf gets max'ed to eval_root
                    // For discussion of following operation, see independent decisions modification stage below  
                    eval_root = apply(ub_xadd, eval_root, _bIsMax ? MIN : MAX); // NOTE: this is correct, it is not reversed
                }
                for (ArithExpr lb : lower_bound) {
                    CompExpr ce = new CompExpr(CompOperation.GT, root, lb);
                    int lb_xadd = _bIsMax
                            ? getVarNode(new ExprDec(ce), Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)  // +inf gets min'ed to eval_root
                            : getVarNode(new ExprDec(ce), Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY); // -inf gets max'ed to eval_root
                    // For discussion of following operation, see independent decisions modification stage below  
                    eval_root = apply(lb_xadd, eval_root, _bIsMax ? MIN : MAX); // NOTE: this is correct, it is not reversed 
                }
                //max_eval_root = reduceLinearize(max_eval_root); // Removed previously
                //max_eval_root = reduceLP(max_eval_root); // Result should be canonical

                if (VERBOSE_MIN_MAX) _log.println("constrained root substitute: " + getString(eval_root));
                min_max_eval = apply(min_max_eval, eval_root, _bIsMax ? MAX : MIN); // handle min or max
                min_max_eval = reduceLinearize(min_max_eval);
                min_max_eval = reduceLP(min_max_eval); // Result should be canonical
                if (VERBOSE_MIN_MAX)
                    _log.println(_sOpName + " of constrained root sub and int_eval(LB/UB): " + getString(min_max_eval));
            }

            if (VERBOSE_MIN_MAX)
                _log.println(_sOpName + "_eval before decisions (after sub root): " + getString(min_max_eval));

            // Finally, incorporate boolean decisions and irrelevant comparison expressions
            // to the XADD and add it to the running sum
            for (Map.Entry<Decision, Boolean> me : target_var_indep_decisions.entrySet()) {
                double high_val = ((me.getValue() && _bIsMax) || (!me.getValue() && !_bIsMax)) ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                double low_val = ((me.getValue() && _bIsMax) || (!me.getValue() && !_bIsMax)) ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
                int indep_constraint = getVarNode(me.getKey(), low_val, high_val);
                if (VERBOSE_MIN_MAX)
                    _log.println("max_eval with decisions: " + me.getKey() + " [" + me.getValue() + "] -->\n" + getString(indep_constraint));
                // Note: need to make function -INF when constraints violated: min(f,(v -inf +inf)) = (v -inf f)
                min_max_eval = apply(indep_constraint, min_max_eval, _bIsMax ? MIN : MAX); // NOTE: this is correct, it is not reversed
            }

            if (VERBOSE_MIN_MAX)
                _log.println("Final " + _sOpName + "_eval before linearize: " + getString(min_max_eval));
            min_max_eval = reduceLinearize(min_max_eval);
            if (VERBOSE_MIN_MAX)
                _log.println("After linearize, before reduceLP: id =" + min_max_eval + "\n" + getString(min_max_eval));
            min_max_eval = reduceLP(min_max_eval); // Result should be canonical
            if (VERBOSE_MIN_MAX) _log.println("After linearize and reduceLP: " + getString(min_max_eval));

            if (_runningResult == -1)
                _runningResult = min_max_eval;
            else
                _runningResult = apply(_runningResult, min_max_eval, _bIsMax ? MAX : MIN); // handle min or max

            _runningResult = reduceLinearize(_runningResult);
            _runningResult = reduceLP(_runningResult);
            if (_runningResult != makeCanonical(_runningResult)) {
                System.err.println("processXADDMinOrMax ERROR: encountered non-canonical _runningResult that should have been canonical, continuing.");
                System.err.println(getString(_runningResult));
                System.err.println(getString(makeCanonical(_runningResult)));
                //System.exit(1);
            }
            if (VERBOSE_MIN_MAX) _log.println("running " + _sOpName + " result: " + getString(_runningResult));

            // All return information is stored in _runningMax so no need to return
            // any information here... just keep diagram as is
            return getTermNode(leaf_val);
        }
    }
    ////////////////////////////////////////////////////

    // Given argument var v, the XADD on which this is called contains substitutions
    // for v at its leaves that are to be substituted into argument XADD xadd
    // and returned at the leaves (because these branches are the conditions under
    // which the substitutions are made).
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

        // TODO: revisit whether caching is possible, or in what circumstances
        public boolean isCacheable() {
            return false;
        }

        public int processXADDLeaf(ArrayList<Decision> decisions,
                                   ArrayList<Boolean> decision_values, ArithExpr leaf_val) {

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

        // TODO: verify this is cacheable... should be, just returns modified
        // subdiagrams
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

        // TODO: revisit whether caching is possible... should be, just
        // returning integrated subdiagrams
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
        public int _runningSum; // XADD for the running sum of all leaf substitutions      //Made public by Hadi

        public final static boolean DEBUG_XADD_DEF_INTEGRAL = false;

        public XADDLeafDefIntegral(String integration_var) {
            super(integration_var);

            // Start with the zero XADD
            _runningSum = ZERO;
        }

        // TODO: revisit whether caching is possible, or in what circumstances
        // (i.e., constraints irrelevant to the integration variable)
        public boolean isCacheable() {
            return false;
        }

        public int processXADDLeaf(ArrayList<Decision> decisions,
                                   ArrayList<Boolean> decision_values, ArithExpr leaf_val) {

            // Determine if this will be a delta integral or not
            // ... if we encounter a delta function here that contains
            //     the variable then one of them has to be linear in
            //     the variable, otherwise we exit
            // ... if find delta linear in variable then we extract substitution
            //     and make it to all remaining terms -- delta and non-delta --
            //     and return that result.
            // ... if delta's but do not contain variable then factor
            //     these out for multiplication in at the end
            // ... what to do on encountering summation?  breaks into
            //     individual subproblems of the above, all results summed together!

            // Multiply these in later
            HashMap<Decision, Boolean> int_var_indep_decisions = new HashMap<Decision, Boolean>();

            // upper and lower bounds based on the decisions
            if (DEBUG_XADD_DEF_INTEGRAL) {
                System.out.println("=============================");
                System.out.println("Current node: " + leaf_val);
                System.out.println("Decisions to get to get here: " + decisions
                        + " = " + decision_values + "\n===\n");
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
                    System.out.println("processXADDLeaf: Unsupported decision type '"
                            + d + "'");
                    System.exit(1);
                }

                // Check that comparison expression is normalized
                if (!comp._rhs.equals(ExprLib.ZERO)) {
                    System.out.println("processXADDLeaf: Expected RHS = 0 for '" + comp + "'");
                    System.exit(1);
                }

                // Takes ArithExpr expr1 linear in var, returns (coef,expr2) where expr1 = coef*x + expr2
                CoefExprPair p = comp._lhs.removeVarFromExpr(_integrationVar);
                ArithExpr lhs_isolated = p._expr;
                double var_coef = p._coef;
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
                boolean flip_comparison = (var_coef < 0d) && (comp._type != CompOperation.EQ) && (comp._type != CompOperation.NEQ);
                ArithExpr new_rhs = (ArithExpr) new OperExpr(ArithOperation.MINUS, ExprLib.ZERO, new OperExpr(ArithOperation.PROD, new DoubleExpr(1d / var_coef), lhs_isolated)).makeCanonical();

                // Divide through by coef (pos or neg)
                // - if coef neg, flip expression
                // - if decision neg, flip expression
                // - if both, don't flip
                CompOperation comp_oper = comp._type;
                if ((/* negated */!is_true && !flip_comparison)
                        || (/* not negated */is_true && flip_comparison)) {
                    comp_oper = CompExpr.flipCompOper(comp_oper);
                }

                // Now we have x COMP_OPER expr
                // System.out.println("After arrange: " + new
                // CompExpr(comp_oper, new VarExpr(_integrationVar),
                // new_rhs));

                // Now mark this as a lower or upper bound
                // - lower bounds: x > f(y), x >= f(y)
                // - upper bounds: x < f(z), x <= f(z)
                if (comp_oper == CompOperation.GT || comp_oper == CompOperation.GT_EQ)
                    lower_bound.add(new_rhs);
                else if (comp_oper == CompOperation.LT || comp_oper == CompOperation.LT_EQ)
                    upper_bound.add(new_rhs);
                else {
                    System.out.println("Cannot currently handle: "
                            + new CompExpr(comp_oper, new VarExpr(
                            _integrationVar), new_rhs));
                    System.out
                            .println("Note: = triggers substitution, not sure how to handle ~=");
                    new Exception().printStackTrace();
                    System.exit(1);
                }
            }

            // Now explicitly compute lower and upper bounds as XADDs
            //
            // If these are polynomials, must go to +/- infinity at limits so
            // cannot be used to approximate cdfs. Hence we must assume that there will
            // always be limits on the polynomial functions implicit in the bounds.
            int xadd_lower_bound = -1;
            if (lower_bound.isEmpty()) {
                if (DEBUG_XADD_DEF_INTEGRAL) {
                    System.err.println("No explicit lower bound given for '"
                            + _integrationVar + "', using NEG_INFINITE");
                    System.err.println("Constraints: " + decisions);
                    System.err.println("Assignments: " + decision_values);
                }
                xadd_lower_bound = NEG_INF;
            } else {
                if (DEBUG_XADD_DEF_INTEGRAL)
                    System.out.println("Lower bounds: " + lower_bound);
                for (ArithExpr e : lower_bound) {
                    if (xadd_lower_bound == -1) {
                        xadd_lower_bound = getTermNode(e);
                    } else {
                        // Lower bound is max of all lower bounds
                        xadd_lower_bound = apply(xadd_lower_bound,
                                getTermNode(e), MAX);
                    }
                }
            }

            int xadd_upper_bound = -1;
            if (upper_bound.isEmpty()) {
                if (DEBUG_XADD_DEF_INTEGRAL) {
                    System.err.println("No explicit upper bound given for '"
                            + _integrationVar + "', using INFINITE");
                    System.err.println("Constraints: " + decisions);
                    System.err.println("Assignments: " + decision_values);
                }
                xadd_upper_bound = POS_INF;
            } else {
                if (DEBUG_XADD_DEF_INTEGRAL)
                    System.out.println("Upper bounds: " + upper_bound);
                for (ArithExpr e : upper_bound) {
                    if (xadd_upper_bound == -1) {
                        xadd_upper_bound = getTermNode(e);
                    } else {
                        // Upper bound is min of all upper bounds
                        xadd_upper_bound = apply(xadd_upper_bound,
                                getTermNode(e), MIN);
                    }
                }
            }

            // Build all constraints for the integration
            for (ArithExpr e1 : upper_bound) {
                for (ArithExpr e2 : lower_bound) {
                    CompExpr ce = new CompExpr(CompOperation.GT, e1, e2);
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
            // leaf_integral{int_var = xadd_upper_bound} - leaf_integral{int_var
            // = xadd_lower_bound}
            int int_eval_lower = substituteXADDforVarInArithExpr(leaf_integral, _integrationVar, xadd_lower_bound);
            int int_eval_upper = substituteXADDforVarInArithExpr(leaf_integral, _integrationVar, xadd_upper_bound);
            int int_eval = apply(int_eval_upper, int_eval_lower, MINUS);
            // System.out.println("Result: " + getString(int_eval));

            // Finally, multiply in boolean decisions and irrelevant comparison
            // expressions
            // to the XADD and add it to the running sum
            //
            // - HashMap<Decision,Boolean> int_var_indep_decisions
            for (Map.Entry<Decision, Boolean> me : int_var_indep_decisions
                    .entrySet()) {
                double high_val = me.getValue() ? 1d : 0d;
                double low_val = me.getValue() ? 0d : 1d;
                int_eval = apply(int_eval, getVarNode(me.getKey(), low_val, high_val), PROD);
            }
            _runningSum = apply(_runningSum, int_eval, SUM);

            // All return information is stored in _runningSum so no need to
            // return
            // any information here... just keep diagram as is
            // System.out.println("Running sum: " + getString(_runningSum));
            return getTermNode(leaf_val);
        }
    }


    // Note: use canonical_reorder=false for integration since internal nodes
    // *cannot* get out of order, but use canonical_reorder=true for substitution
    public int reduceProcessXADDLeaf(int id, XADDLeafOperation leaf_sub,
                                     boolean canonical_reorder) {
        int ret_node = reduceProcessXADDLeaf(id, leaf_sub,
                new ArrayList<Decision>(), new ArrayList<Boolean>());
        if (canonical_reorder)
            return makeCanonical(ret_node);
        else
            return ret_node;
    }

    public IntPair _tempReduceLeafOpKey = new IntPair(-1, -1);

    private int reduceProcessXADDLeaf(int id, XADDLeafOperation leaf_op,
                                      ArrayList<Decision> decisions, ArrayList<Boolean> decision_values) {

        // System.out.println("** Reduce: decisions " + decisions + " -> " +
        // decision_values);

        Integer ret = null;
        XADDNode n = getExistNode(id);

        // A terminal node should be reduced (and cannot be restricted)
        // by default if hashing and equality testing are working in getTNode
        if ((n instanceof XADDTNode) && (leaf_op instanceof XADDLeafMinOrMax)) {
            return ((XADDLeafMinOrMax) leaf_op).processXADDLeaf(decisions,
                    decision_values, ((XADDTNode) n)._expr); // Assuming that to have
            // a node id means
            // canonical
        }
        if (n instanceof XADDTNode) {
            return leaf_op.processXADDLeaf(decisions, decision_values,
                    ((XADDTNode) n)._expr); // Assuming that to have
            // a node id means
            // canonical
        }

        // If its an internal node, check the reduce cache
        _tempReduceLeafOpKey.set(id, leaf_op.hashCode());
        if (leaf_op.isCacheable()
                && (ret = _hmReduceLeafOpCache.get(_tempReduceLeafOpKey)) != null) {
            // System.out.println("** In cache, returning: " + getString(ret));
            return ret;
        }

        XADDINode inode = (XADDINode) n;
        Decision d = _alOrder.get(inode._var);

        decisions.add(d);
        decision_values.add(Boolean.FALSE);
        int low = reduceProcessXADDLeaf(inode._low, leaf_op, decisions,
                decision_values);

        decision_values.set(decision_values.size() - 1, Boolean.TRUE);
        int high = reduceProcessXADDLeaf(inode._high, leaf_op, decisions,
                decision_values);

        decisions.remove(decisions.size() - 1);
        decision_values.remove(decision_values.size() - 1);

        // Standard Reduce: getInode will handle the case of low == high
        ret = getINode(inode._var, low, high);
//        ret = getINodeCanon(inode._var, low, high); Should this be made Canonical now?
        
        // Put return value in cache and return
        _hmReduceLeafOpCache.put(new IntPair(id, leaf_op.hashCode()), ret);
        return ret;
    }

    /////////////////////////////////////////////////////////////////////////////////////
    //                               XADD Nodes
    /////////////////////////////////////////////////////////////////////////////////////

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

        public abstract int countLeaves(int id, HashSet<Integer> m);

        public abstract String toString(int depth);

        public abstract String toString(boolean format);

        public abstract void toGraph(Graph g, int id);

        public abstract void collectVars(HashSet<String> vars);

        public abstract void collectNodes(HashSet<XADDNode> nodes);
    }

    //Terminal Node = Leaf, contains only an expression
    public class XADDTNode extends XADDNode {
        public ArithExpr _expr;
        public Object _annotate;

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
                XADDTNode t = (XADDTNode) o;
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
            return "( ["/* "#" + _hmNode2Int.get(this) + ": " */
                    + _expr.toString() + "] )";
        }

        public String toString(boolean format) {
            return "( ["/* "#" + _hmNode2Int.get(this) + ": " */
                    + _expr.toString(format) + "] )";
        }

        @Override
        public int countBranches() {
            return 1;
        }

        public int countLeaves(int id, HashSet<Integer> marked) {
            if (marked.contains(id)) return 0;
            else {
                marked.add(id);
                return 1;
            }
        }

    }

    //Internal Node, constains decision and two child XADD references
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

        //modified by Hadi
        @Override
        public int hashCode() {
            int result = _var;
            result = 31 * result + _low;
            result = 31 * result + _high;
            return result;
        }

        /*
                public int hashCode() {
                    return (_var) + (_low << 10) - (_high << 20) + (_high >>> 20)
                            - (_low >>> 10);
                }
        */
        //added by Hadi
        public XADDNode getLowChild() {
            return _hmInt2Node.get(_low);
        }

        //added by Hadi
        public XADDNode getHighChild() {
            return _hmInt2Node.get(_high);
        }

        //added by Hadi
        public Decision getDecision(){
        return _alOrder.get(_var);
        }

        public boolean equals(Object o) {
            if (o instanceof XADDINode) {
                XADDINode n = (XADDINode) o;
                return this._var == n._var && this._low == n._low
                        && this._high == n._high;
            } else
                return false;
        }

        public void collectNodes(HashSet<XADDNode> nodes) {
            if (nodes.contains(this))
                return;

            nodes.add(this);
            getExistNode(_low).collectNodes(nodes);
            getExistNode(_high).collectNodes(nodes);
        }

        @SuppressWarnings("unchecked")
        public void collectVars(HashSet<String> vars) {

            // Check cache
            HashSet<String> vars2 = _hmINode2Vars.get(this);
            if (vars2 != null) {
                vars.addAll(vars2);
                return;
            }

            XADDNode low = getExistNode(_low);
            XADDNode high = getExistNode(_high);
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
            g.addNodeLabel(this_node, _alOrder.get(_var).toString());
            if (GRAPH_USE_COLOR)
                g.addNodeColor(this_node, "lightblue"); // green, lightblue
            g.addNodeShape(this_node, "ellipse");
            g.addNodeStyle(this_node, "filled");

            // Children
            XADDNode low = getExistNode(_low);
            if (low != null) {
                String low_node = Integer.toString(_low);
                g.addUniLink(this_node, low_node, "black", "dashed",
                        Graph.EMPTY_STR);
                low.toGraph(g, _low);
            }

            XADDNode high = getExistNode(_high);
            if (high != null) {
                String high_node = Integer.toString(_high);
                g.addUniLink(this_node, high_node, "black", "solid",
                        Graph.EMPTY_STR);
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
			 */ + "[" + _alOrder.get(_var).toString(format) + "]");

            // Node level cache
            XADDNode n2 = _hmInt2Node.get(_high);
            if (n2 != null) {
                sb.append("\n" + indent(depth) + " " + n2.toString(depth + 1)
                        + " ");
            } else {
                sb.append("h:[null] ");
            }
            XADDNode n1 = _hmInt2Node.get(_low);
            if (n1 != null) {
                sb.append("\n" + indent(depth) + " " + n1.toString(depth + 1)
                        + " ");
            } else {
                sb.append("l:[null] ");
            }
            sb.append(") ");

            return sb.toString();
        }

        @Override
        public int countBranches() {
            int low_count = getExistNode(_low).countBranches();
            if (low_count > MAX_BRANCH_COUNT || low_count == -1)
                return -1;

            int high_count = getExistNode(_high).countBranches();
            if (high_count > MAX_BRANCH_COUNT || high_count == -1)
                return -1;

            int total = low_count + high_count;
            if (total > MAX_BRANCH_COUNT)
                return -1;

            return total;
        }

        public int countLeaves(int id, HashSet<Integer> marked) {
            if (marked.contains(id)) return 0;
            int low_count = getExistNode(_low).countLeaves(_low, marked);
            int high_count = getExistNode(_high).countLeaves(_high, marked);
            int total = low_count + high_count;

            marked.add(id);
            return total;
        }

        @Override
        public String toString(boolean format) {
            return toString(0, format);
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////
    // Decisions Library - Helper Classes
    /////////////////////////////////////////////////////////////////////////////////////

    public abstract class Decision {
        public abstract void collectVars(HashSet<String> vars);

        public abstract String toString(boolean format);

        public abstract Decision makeCanonical();
    }

    public class NullDec extends Decision {

        public void collectVars(HashSet<String> vars) {
        }

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

        @Override
        public String toString(boolean format) {
            return Boolean.toString(_bTautology);
        }
    }

    //Boolean Variable decision
    public class BoolDec extends Decision {
        public String _sVarName = null;

        public BoolDec(String var_name) {
            _sVarName = var_name.intern();
        }

        public int hashCode() {
            return _sVarName.hashCode();
        }

        public boolean equals(Object o) {
            if (o instanceof BoolDec)
                return ((BoolDec) o)._sVarName.equals(_sVarName);
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

    //Comparison Expression Decision
    public class ExprDec extends Decision {
        public CompExpr _expr = null;

        public ExprDec(CompExpr expr) {
            _expr = expr;
        }

        public ExprDec round() {
            return new ExprDec(new CompExpr(_expr._type, _expr._lhs.round(), _expr._rhs.round()));
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
                // TODO: If there are divisors, multiply through and handle negative operands
                CompExpr new_comp = (CompExpr) e._expr.makeCanonical();
                d = new ExprDec(new_comp);
            }

            // Check for tautology (or inconsistency)
            if (d instanceof ExprDec
                    && ((ExprDec) d)._expr._lhs instanceof DoubleExpr
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
            }
            return d;
        }
    }



//	// USED PREVIOUSLY WHEN WORKING WITH DIVISORS, UNUSED NOW
//  // KEEPING IN B/C WE MAY NEED TO WORK WITH POLY FRACTIONS IN FUTURE
//	public static DoubleExpr findVar(ArithExpr expr, String action,
//			boolean nonlinear) {
//		DoubleExpr result = (DoubleExpr) ZERO;
//		if (expr instanceof OperExpr && ((OperExpr) expr)._type == SUM) {
//			// sum of products
//			OperExpr eOp = ((OperExpr) expr);
//			for (ArithExpr e : eOp._terms) {
//				if (e instanceof OperExpr) {
//					result = findVarTerm((OperExpr) e, action, nonlinear);
//					if (result != (DoubleExpr) ZERO)
//						return result;
//				} else if (e instanceof VarExpr) {
//					OperExpr temp = new OperExpr(PROD, Arrays.asList(e));
//					result = findVarTerm(temp, action, nonlinear);
//					if (result != (DoubleExpr) ZERO)
//						return result;
//				}
//			}
//		} else if (expr instanceof OperExpr && ((OperExpr) expr)._type == PROD) {
//			result = findVarTerm((OperExpr) expr, action, nonlinear);
//			if (result != (DoubleExpr) ZERO)
//				return result;
//		} else if ((expr instanceof VarExpr) || (expr instanceof DoubleExpr)) {
//			OperExpr temp = new OperExpr(PROD, Arrays.asList(expr));
//			result = findVarTerm(temp, action, nonlinear);
//			if (result != (DoubleExpr) ZERO)
//				return result;
//		}
//		return result;
//	}
//	public static DoubleExpr findVarTerm(OperExpr expr, String action1,
//			boolean nonlinear) {
//
//		// Process all terms (optional double followed by vars)
//		int _var_count = 0;
//		double coef = 1d;
//		for (ArithExpr e : expr._terms) {
//			if (e instanceof DoubleExpr) {
//				coef *= ((DoubleExpr) e)._dConstVal;
//			} else if (e instanceof OperExpr) {
//				// don't divide if it comes here, makes it complicated
//			} else if (e instanceof VarExpr) {
//				// Both interned so we can test direct equality
//				if (((VarExpr) e)._sVarName.equals(action1)) {
//					_var_count++;
//				}
//			} else {
//				System.out.println("findVarTerm: Unsupported expression '" + e
//						+ "'");
//				System.exit(1);
//			}
//		}
//
//		if ((nonlinear) && (_var_count > 1))
//			return new DoubleExpr(coef);
//		else if ((!nonlinear) && (_var_count > 0))
//			return new DoubleExpr(coef);
//		else
//			return (DoubleExpr) ZERO;
//
//	}

//	// SCOTT - Another unused method
//	// LUIS  - Good to comment out for now, but eventually we may need to track substitutions
//	//         and that's what this method was intended for -- should be generalized though
//	//         from "policy" to substitutions.
//	public Double evaluate_policy(int node_id,
//			HashMap<String, Boolean> bool_assign,
//			HashMap<String, Double> cont_assign) {
//
//		// Get root
//		XADDNode n = _hmInt2Node.get(node_id);
//
//		// Traverse decision diagram until terminal found
//		while (n instanceof XADDINode) {
//			XADDINode inode = (XADDINode) n;
//			Decision d = _alOrder.get(inode._var);
//			Boolean branch_high = null;
//			if (d instanceof TautDec)
//				branch_high = ((TautDec) d)._bTautology;
//			else if (d instanceof BoolDec)
//				branch_high = bool_assign.get(((BoolDec) d)._sVarName);
//			else if (d instanceof ExprDec) {
//				branch_high = ((ExprDec) d)._expr.evaluate(cont_assign);
//				if (SHOW_DECISION_EVAL) {
//					// System.out.println(" - " + ((ExprDec) d)._expr + ": " +
//					// branch_high);
//				}
//			}
//
//			// Not all required variables were assigned
//			if (branch_high == null)
//				return null;
//
//			// Advance down to next node
//			n = _hmInt2Node.get(branch_high ? inode._high : inode._low);
//		}
//
//		// Now at a terminal node so evaluate expression
//		//
//		XADDTNode t = (XADDTNode) n;
//		if (t._annotate_action == null)
//			t._annotate_action = "";
//		if (t._annotate == null)
//			t._annotate = new DoubleExpr(0);
//		if (t._annotate_action.contains("no"))
//			return (-1 * t._annotate.evaluate(cont_assign));
//		else
//			return t._annotate.evaluate(cont_assign);
//	}


}