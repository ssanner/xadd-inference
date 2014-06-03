//////////////////////////////////////////////////////////////////////
//
// Extended Algebraic Decision Diagrams Package
// Linear Pruning of infeasible paths and 
// Removal of redundant decisions
//
// @author Scott Sanner (ssanner@gmail.com)
// @author Zahra Zamani
// @author Luis Vianna
//////////////////////////////////////////////////////////////////////

package xadd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import logic.kb.prop.PropKbCNF;
import lpsolve.LP;
import lpsolve.LpSolve;

import util.MapList;
import xadd.ExprLib.ArithExpr;
import xadd.XADD.BoolDec;
import xadd.ExprLib.CompExpr;
import xadd.XADD.Decision;
import xadd.XADD.ExprDec;
import xadd.XADD.XADDNode;
import xadd.XADD.XADDTNode;
import xadd.XADD.XADDINode;


public class ReduceLPContext {
	
    //ReduceLP Flags
    private final static boolean DEFAULT_CHECK_REDUNDANCY = true; // Test only consistency or also redundancy
    private final static boolean USE_REDUCE_LPv1 = false;//false; //maplist, full redundancy older version
    private final static boolean USE_REDUCE_LPv2 = true;//true; //hashSet, result implied redundancy new version
    private final static boolean SKIP_TEST2 = false; //Skip Minimal region removal. Currenty test2 is very effective in reducing size even at very small slack.
    private static final double IMPLIED_PRECISION_T2 = 1e-40;// XADD.PRECISION;//1e-4; //Precision for removing unreliably feasible constraints
  
    public static final boolean SINGLE_PATH_IMPLIED_RESULT = false; //Stop search if need to check more than one path

    private static final boolean ADD_EXPLICIT_BOUND_CONSTRAINTS_TO_LP = false; //Add bounds as explicit constraints (should not be necessary)
    //Debug Flags
    public final static boolean DEBUG_CONSTRAINTS = false;
    public final static boolean TEST2_INCONSIST_QUIET = true;

    

    //Implication Caches
    //ReduceLPv1
    public MapList _mlImplications = new MapList();
    public MapList _mlNonImplications = new MapList();
    public MapList _mlImplicationsChild = new MapList();
    public MapList _mlIntermediate = new MapList();
    public HashMap<Integer, ArrayList<Integer>> _hmIntermediate = new HashMap<Integer, ArrayList<Integer>>();

    //ReduceLPv2
    public HashMap<HashSet<Integer>, HashSet<Integer>> _hmImplications = new HashMap<HashSet<Integer>, HashSet<Integer>>();
    public HashMap<HashSet<Integer>, HashSet<Integer>> _hmNonImplications = new HashMap<HashSet<Integer>, HashSet<Integer>>();

    private XADD LPcontext = null;

    public ReduceLPContext(XADD global) {
        LPcontext = global;
    }

    public int reduceLP(int node_id) {
        return reduceLP(node_id, DEFAULT_CHECK_REDUNDANCY);
    }

    public int reduceLP(int node_id, boolean redun) {
        LocalReduceLP RLP = new LocalReduceLP(node_id);
        return RLP.reduceLP(node_id, redun);
    }

    public void flushImplications() {
        _mlImplications.clear();
        _mlNonImplications.clear();
        _mlImplicationsChild.clear();
        _mlIntermediate.clear();
        _hmIntermediate.clear();

        _hmImplications.clear();
        _hmNonImplications.clear();
    }

    public String showImplications() {

        StringBuilder sb = new StringBuilder();

        sb.append("Bool vars: " + LPcontext._alBooleanVars + "\n");
        sb.append("Cont vars: " + LPcontext.getContinuousVarList() + "\n");
        sb.append("\nDecision vars:\n");

        for (int i = 1; i < LPcontext._alOrder.size(); i++) {
            Decision d = LPcontext._alOrder.get(i);
            if (d instanceof ExprDec)
                sb.append("[" + i + "]: " + d + "\n");
        }

        sb.append("\nImplications:\n");
        for (Object key : _mlImplications.keySet()) {
            Integer ikey = (Integer) key;
            Decision dkey = LPcontext._alOrder.get(Math.abs(ikey));
            @SuppressWarnings("rawtypes")
            ArrayList values = _mlImplications.getValues(key);
            for (Object value : values) {
                Integer ival = (Integer) value;
                Decision dval = LPcontext._alOrder.get(Math.abs(ival));
                sb.append((ikey < 0 ? "NOT " : "") + dkey + " => " + (ival < 0 ? "NOT " : "") + dval + "\n");
            }
        }
        return sb.toString();
    }

    public void showImplicationCache() {
        if (USE_REDUCE_LPv1) {
            System.out.format("REDUCE_LP1 CACHES: Imp = %d, NonImp = %d, Child = %d, Intermed = %d\n",
                    _mlImplications.keySet().size(),
                    _mlNonImplications.keySet().size(),
                    _mlImplicationsChild.keySet().size(),
                    _mlIntermediate.keySet().size());
        }
        if (USE_REDUCE_LPv2) {
            System.out.format("REDUCE_LP2 CACHE: Imp = %d, NonImp = %d\n",
                    _hmImplications.size(), _hmNonImplications.size());
        }
    }

    private class LocalReduceLP extends LinearXADDMethod {

        public LocalReduceLP(int localRoot) {
            super(localRoot, LPcontext);
        }

        // Consistency and Redundancy Checking - ReduceLP
        public int reduceLP(int node_id, boolean performRedundancy) {

            if (USE_REDUCE_LPv2) {
                node_id = reduceLPv2(node_id, new HashSet<Integer>(), performRedundancy);
            }
            if (USE_REDUCE_LPv1) {
                //System.out.print("using LP1!");
                node_id = reduceLPv1(node_id, performRedundancy);
            }

            return node_id;
        }

        //ReduceLPVersion 1- Indirect Redundancy Check
        @SuppressWarnings("rawtypes")
        private int reduceLPv1(int node_id, boolean performRedundancy) {
            ArrayList<Integer> test_var = new ArrayList<Integer>();
            ArrayList<Boolean> test_dec = new ArrayList<Boolean>();
            _mlImplicationsChild = new MapList();
            //_mlIntermediate = new MapList();
            _mlImplications = new MapList();
            _mlNonImplications = new MapList();
            //HashMap<Integer,Boolean> marked_nodes = new HashMap<Integer,Boolean>();
            //definitions for paths to a node
            ArrayList<Integer> path = new ArrayList<Integer>();
            ArrayList<ArrayList<Integer>> path_no = new ArrayList<ArrayList<Integer>>();
            HashMap<Integer, ArrayList<ArrayList<Integer>>> formulas = new HashMap<Integer, ArrayList<ArrayList<Integer>>>();
            //List for keeping track of false-true branch id's
            for (int i = 0; i < context._alOrder.size(); i++) {
                test_dec.add(null);
            }
            int tree = reduceLPv1(node_id, test_var, test_dec);

            if (performRedundancy) {

                for (int i = 0; i < context._alOrder.size(); i++) {
                    test_dec.add(null);
                }
                //context.showGraph(tree, "V^ After consistency check and before Redundancy check");
                //put formula for root node
                path.add(0);
                path_no.add(path);
                formulas.put(tree, path_no);
                //add all implications to KB
                PropKbCNF kb = new PropKbCNF();
                kb.addFormula("((p^~p) <=> (false))");
                kb.addFormula("((p|~p) <=> (true))");
                _hmIntermediate = new HashMap<Integer, ArrayList<Integer>>();
                ArrayList<Integer> _dl = new ArrayList<Integer>();
                //there are many implications not in this reduced tree, so take them out and also define the new paths of the tree
                getDecisionList(tree, _dl, test_var);
                Iterator it = _mlImplicationsChild.keySet().iterator();
                //
                //Add implications to KB
                //
                while (it.hasNext()) {
                    int parent = (Integer) it.next();
                    //check if decision is inside the reduced tree
                    if (_dl.contains(Math.abs(parent))) {
                        @SuppressWarnings("unchecked")
                        ArrayList<Integer> child = _mlImplicationsChild.getValues(parent);
                        for (int j = 0; j < child.size(); j++) {
                            //check if decision is inside the reduced tree + there is a path from parent to child
                            if (_dl.contains(Math.abs(child.get(j)))) {
                                for (int i = 0; i < _hmIntermediate.size(); i++)
                                    if ((_hmIntermediate.get(i).contains(parent)) && (_hmIntermediate.get(i).contains(child.get(j)))) {
                                        String p = "", c = "";
                                        if (parent < 0)
                                            p = " ~a" + Math.abs(parent) + "a ";
                                        else p = " a" + Math.abs(parent) + "a ";
                                        if (child.get(j) < 0)
                                            c = "~a" + Math.abs(child.get(j)) + "a";
                                        else
                                            c = " a" + Math.abs(child.get(j)) + "a ";
                                        String s = p + " =>" + c;
                                        kb.addFormula(s);
                                        break;
                                    }
                            }
                        }
                    }
                }
                tree = reduceRedundancy(tree, formulas, kb);
            } //END REDUNDANCY

            return tree;
        }

        private int getDecisionList(int node_id, ArrayList<Integer> _decisionList, ArrayList<Integer> test_var) {
            Integer ret = null;
            XADDNode n = context.getExistNode(node_id);
            if (n instanceof XADDTNode) {
                //add this path to the set of intermediate paths
                ArrayList<Integer> temp = new ArrayList<Integer>();
                for (int par_dec : test_var) {
                    //par_dec = test_var.get(par_dec) ? par_dec : -par_dec;
                    temp.add(par_dec);
                }
                //temp.add(inode._var);
                int sizeInt = _hmIntermediate.size();
                _hmIntermediate.put(sizeInt, temp);
                return node_id;
            } else {
                XADDINode inode = (XADDINode) n;
                _decisionList.add(inode._var);

                test_var.add(-inode._var);
                int low = getDecisionList(inode._low, _decisionList, test_var);
                test_var.set(test_var.size() - 1, inode._var);
                int high = getDecisionList(inode._high, _decisionList, test_var);
                test_var.remove(test_var.size() - 1);

                ret = context.getINode(inode._var, low, high);
            }
            return ret;
        }

        private int reduceLPv1(int node_id, ArrayList<Integer> test_var, ArrayList<Boolean> test_dec) {

            Integer ret = null;
            XADDNode n = context.getExistNode(node_id);

            // A terminal node should be reduced (and cannot be restricted)
            // by default if hashing and equality testing are working in getTNode
            if (n instanceof XADDTNode) {
                return node_id; // Assuming that to have a node id means canonical
            }

            XADDINode inode = (XADDINode) n;

            // Reduce based on pairwise interactions only
            Boolean var_implication = null;

            // Full branch implication test
            if (isTestImpliedv1(test_var, test_dec, inode._var, true)) {
                var_implication = true;
            } else if (isTestImpliedv1(test_var, test_dec, inode._var, false)) {
                var_implication = false;
            }

            // Check for implied branches before doing a full reduce on both branches
            if (var_implication == Boolean.TRUE) {

                ret = reduceLPv1(inode._high, test_var, test_dec);
            } else if (var_implication == Boolean.FALSE) {

                ret = reduceLPv1(inode._low, test_var, test_dec);

            } else {

                test_var.add(inode._var);
                test_dec.set(inode._var, false);
                int low = reduceLPv1(inode._low, test_var, test_dec);
                test_dec.set(inode._var, true);
                int high = reduceLPv1(inode._high, test_var, test_dec);
                test_dec.set(inode._var, null);
                test_var.remove(test_var.size() - 1);

                // Standard Reduce: getInode will handle the case of low == high
                ret = context.getINode(inode._var, low, high);
            }

            // Put return value in cache and return
            //_hmReduceLPCache.put(new IntTriple(node_id, test_var.hashCode(), test_dec.hashCode()), ret);
            return ret;
        }

        // Checks cache for singleton decision variable implications or computes and caches
        @SuppressWarnings("unchecked")
        private int reduceRedundancy(int tree, HashMap<Integer, ArrayList<ArrayList<Integer>>> formulas, PropKbCNF kb/*, HashMap<Integer,Boolean> marked_nodes*/) {
            Integer ret = null;
            XADDNode n = context.getExistNode(tree);

            if (n instanceof XADDTNode) {
                return tree; // Assuming that to have a node id means canonical
            }
            //formulas contains the decision node id's of the parent; add new decision node to low/high branch
            else {
                XADDINode inode = (XADDINode) n;
                //Decision current_dec = _alOrder.get(inode._var);
                ArrayList<ArrayList<Integer>> path_list = formulas.get(tree);
                ArrayList<Integer> new_path = new ArrayList<Integer>();
                ArrayList<ArrayList<Integer>> new_path_list1 = new ArrayList<ArrayList<Integer>>();
                ArrayList<ArrayList<Integer>> new_path_list2 = new ArrayList<ArrayList<Integer>>();
                ArrayList<Integer> copy_new_path1 = new ArrayList<Integer>();
                ArrayList<Integer> copy_new_path2 = new ArrayList<Integer>();

                int high, low;
                //each parent may have different paths leading to it, perform this for all paths
                for (int i = 0; i < path_list.size(); i++) {
                    new_path_list1 = new ArrayList<ArrayList<Integer>>();
                    new_path_list2 = new ArrayList<ArrayList<Integer>>();
                    new_path = path_list.get(i);
                    copy_new_path1 = new ArrayList<Integer>();
                    copy_new_path2 = new ArrayList<Integer>();
                    copy_new_path1 = (ArrayList<Integer>) new_path.clone();
                    copy_new_path2 = (ArrayList<Integer>) new_path.clone();
                    copy_new_path1.add(inode._var);
                    new_path_list1.add(copy_new_path1);
                    if (formulas.containsKey(inode._high)) {
                        ArrayList<ArrayList<Integer>> h_child_path = formulas.get(inode._high);
                        h_child_path.add(copy_new_path1);
                    } else formulas.put(inode._high, new_path_list1);
                    copy_new_path2.add(-(inode._var));
                    new_path_list2.add(copy_new_path2);
                    if (formulas.containsKey(inode._low)) {
                        ArrayList<ArrayList<Integer>> l_child_path = formulas.get(inode._low);
                        l_child_path.add(copy_new_path2);
                    } else formulas.put(inode._low, new_path_list2);
                }

                high = reduceRedundancy(inode._high, formulas, kb);
                low = reduceRedundancy(inode._low, formulas, kb);

                //after all the tree has been checked for child-parent implications,
                //now at each inode, test to see if the false or true branch can be removed
                //if low branch can be removed, then (the formula of true branch = formula)
                if (isTestSAT(inode._var, true, formulas.get(low), kb)) {
                    //return correct branch which is the opposite of the returned node
                    return high;
                } else if (isTestSAT(inode._var, false, formulas.get(high), kb)) {
                    return low;
                } else {
                    ret = context.getINode(inode._var, low, high);
                }
            }

            return ret;
        }

        private boolean isTestSAT(int _var, boolean b, ArrayList<ArrayList<Integer>> paths, PropKbCNF kb) {
            //KB contains conjunction of all implications: child => parent , child =>~parent , parent=>child
            //next build query: first build formula of F, put it equal to F(var=b)
            //build F
            if (paths == null)
                return false;
            if (paths.size() < 2) return false;
            String f = "((";
            for (int i = 0; i < paths.size(); i++) {
                ArrayList<Integer> singlePath = paths.get(i);
                f = f.concat("(");
                for (int j = 0; j < singlePath.size() - 1; j++) {
                    //don't add root node parent
                    if (singlePath.get(j) != 0) {
                        if (singlePath.get(j) < 0)
                            f = f.concat(" ~a" + Math.abs(singlePath.get(j)) + "a ^ ");
                        else f = f.concat(" a" + singlePath.get(j) + "a ^ ");
                    }
                }
                //last element
                if (singlePath.get(singlePath.size() - 1) < 0)
                    f = f.concat(" ~a" + Math.abs(singlePath.get(singlePath.size() - 1)) + "a ");
                else f = f.concat(" a" + (singlePath.get(singlePath.size() - 1)) + "a ");
                f = f.concat(")");

                if (i != paths.size() - 1)
                    f = f.concat(" | ");
            }
            f = f.concat(")");
            //build F(_var=b)
            String f_var = "(";
            for (int i = 0; i < paths.size(); i++) {
                ArrayList<Integer> singlePath = paths.get(i);
                f_var = f_var.concat("(");
                for (int j = 0; j < singlePath.size() - 1; j++) {
                    //don't add root node parent
                    if (singlePath.get(j) != 0) {
                        if (Math.abs(singlePath.get(j)) == _var) {
                            if (singlePath.get(j) < 0) {
                                if (b) f_var = f_var.concat(" false ^");
                                else f_var = f_var.concat(" true ^");
                            } else {
                                if (b) f_var = f_var.concat(" true ^");
                                else f_var = f_var.concat(" false ^");
                            }
                        } else {
                            if (singlePath.get(j) < 0)
                                f_var = f_var.concat(" ~a" + Math.abs(singlePath.get(j)) + "a ^ ");
                            else f_var = f_var.concat(" a" + singlePath.get(j) + "a ^ ");
                        }
                    }
                }
                //last element
                if (Math.abs(singlePath.get(singlePath.size() - 1)) == _var) {
                    if (singlePath.get(singlePath.size() - 1) < 0) {
                        if (b) f_var = f_var.concat(" false ");
                        else f_var = f_var.concat(" true ");
                    } else {
                        if (b) f_var = f_var.concat(" true ");
                        else f_var = f_var.concat(" false ");
                    }
                } else {
                    if (singlePath.get(singlePath.size() - 1) < 0)
                        f_var = f_var.concat(" ~a" + Math.abs(singlePath.get(singlePath.size() - 1)) + "a ");
                    else f_var = f_var.concat(" a" + (singlePath.get(singlePath.size() - 1)) + "a ");
                }

                f_var = f_var.concat(")");

                if (i != paths.size() - 1) f_var = f_var.concat(" | ");
            }
            f_var = f_var.concat(")");

            //SAT test
            f = f.concat(" => ");
            f = f.concat(f_var + ")");

            //System.out.println(f + ": " + (kb.querySATSolver(f) ? "entailed" : "not entailed"));
            if (kb.querySATSolver(f))
                return true;
            else
                return false;
        }

        private boolean isTestImpliedv1(ArrayList<Integer> test_var,
                                        ArrayList<Boolean> test_dec, int var_id, boolean dec) {

            if (test_var.size() == 0)
                return false;

            if (DEBUG_CONSTRAINTS) {
                System.out.println("===================\nisTestImplied: " + context._alOrder.get(var_id) + " = " + dec);
                for (int cons : test_var) {
                    System.out.println("- cons :: " + context._alOrder.get(cons) + " = "
                            + test_dec.get(cons));
                }
            }

            // if (a > b + c) == true makes system infeasible then it must be
            // implied
            // that (a > b + c) == false (prove A => B by seeing if A^~B is
            // infeasible)

            // Setup LP
            int nvars = nLocalCVars;
            double[] obj_coef = new double[nvars]; // default all zeros, which is
            // what we want
            LP lp = new LP(nvars, assign2Local(context.lowerBounds, true), assign2Local(context.upperBounds, true), obj_coef, LP.MAXIMIZE);

            // Now add all constraints
            for (Integer constraint_id : test_var) {
                addConstraint(lp, constraint_id, test_dec.get(constraint_id));
            }

            // Finally add the negated decision to test
            addConstraint(lp, var_id, !dec);
            addLocalBoundConstraints(lp);
            // Solve and get decision
            silentSolvelp(lp);


            boolean implied = (lp._status == LpSolve.INFEASIBLE);
            if (DEBUG_CONSTRAINTS)
                System.out.println("Solution: " + LP.PrintVector(lp._x));
            lp.free();

            // Since dec inverted, infeasibility implies that the implication is
            // true
            // System.out.println("- RESULT: " + implied);
            return implied;
        }

        //ReduceLPVersion 2- Only direct Redundancy Check

        //ReduceLPVersion 2- Direct Redundancy Check
        private int reduceLPv2(int node_id, HashSet<Integer> test_dec, boolean redundancy) {
            XADDNode n = context.getExistNode(node_id);

            // A terminal node should be reduced (and cannot be restricted)
            // by default if hashing and equality testing are working in getTNode
            if (n instanceof XADDTNode) {
                return node_id; // Assuming that to have a node id means canonical
            }

            XADDINode inode = (XADDINode) n;

            //boolean variables are independent, no redundancy or infeasibility possible
            if (!(context._alOrder.get(inode._var) instanceof ExprDec)) {
                if (!(context._alOrder.get(inode._var) instanceof BoolDec)) {
                    System.err.println("unexpected decision in reduce_LP: " + context._alOrder.get(inode._var));
                }

                int low = reduceLPv2(inode._low, test_dec, redundancy);
                int high = reduceLPv2(inode._high, test_dec, redundancy);
                return context.getINode(inode._var, low, high);
            }

            // Full branch implication test
            if (isTestImpliedv2(test_dec, inode._var)) {
                return reduceLPv2(inode._high, test_dec, redundancy);
            } else if (isTestImpliedv2(test_dec, -1 * inode._var)) {
                return reduceLPv2(inode._low, test_dec, redundancy);
            }

            //make subtree reducing before redundancy
            test_dec.add(-1 * inode._var);
            int low = reduceLPv2(inode._low, test_dec, redundancy);
            test_dec.remove(-1 * inode._var);
            test_dec.add(inode._var);
            int high = reduceLPv2(inode._high, test_dec, redundancy);
            test_dec.remove(inode._var);

            //After reducing subtrees check if this node became redundant
            if (redundancy) {

                //1 check if true branch is implied in the low branch if current decision is true
                test_dec.add(inode._var);
                boolean lowReplace = isResultImplied(test_dec, low, high);
                test_dec.remove(inode._var);

                if (lowReplace) return low;

                //2 check if false branch is implied in the true branch if current decision is false
                test_dec.add(-inode._var);
                boolean highReplace = isResultImplied(test_dec, high, low);
                test_dec.remove(-inode._var);

                if (highReplace) return high;
            }

            //Standard Reduce: getInode will handle the case of low == high
            return context.getINode(inode._var, low, high);
        }

        //Redundancy simplification 2 - search for node check if one node is the impliedResult on the other branch.
        // The complete test (v3?) would be to test if one subtree can replace the other (considering the decision to be removed) - requires
        // an XADD equivalence under constraints test.
        //Call to check if given the test_dec decisions subtree always reaches "goal", which means that
        // if the node above the subtree is chosing between subtree or goal, we can leave subtree in its place (it will reach still
        // reach goal whenever the first decision would take it to goal.
        private boolean isResultImplied(HashSet<Integer> test_dec, int subtree, int goal) {

            if (subtree == goal) return true;

            XADDNode subtreeNode = context.getExistNode(subtree);
            XADDNode goalNode = context.getExistNode(goal);

            if (subtreeNode instanceof XADDINode) {
                XADDINode iSubtreeNode = (XADDINode) subtreeNode;

                if (goalNode instanceof XADDINode) {
                    //use variable ordering to stop useless searches
                    if (iSubtreeNode._var >= ((XADDINode) goalNode)._var) return false;
                }

                if (isTestImpliedv2(test_dec, -iSubtreeNode._var)) {
                    return isResultImplied(test_dec, iSubtreeNode._low, goal);
                }
                if (isTestImpliedv2(test_dec, iSubtreeNode._var)) {
                    return isResultImplied(test_dec, iSubtreeNode._high, goal);
                }

                if (!SINGLE_PATH_IMPLIED_RESULT) {
                    test_dec.add(-iSubtreeNode._var);
                    boolean impliedInLow = isResultImplied(test_dec, iSubtreeNode._low, goal);
                    test_dec.remove(-iSubtreeNode._var);

                    //if one brach failed no need to test the other one
                    if (!impliedInLow) return false;

                    test_dec.add(iSubtreeNode._var);
                    boolean impliedInHigh = isResultImplied(test_dec, iSubtreeNode._high, goal);
                    test_dec.remove(iSubtreeNode._var);

                    return impliedInHigh;
                }
            }
            return false; //if TNode, only the == check can make it true
        }

        private boolean isTestImpliedv2(HashSet<Integer> test_dec, int dec) {

            if (!(context._alOrder.get(Math.abs(dec)) instanceof ExprDec)) return false;

            HashSet<Integer> impliedSet = _hmImplications.get(test_dec);
            if (impliedSet != null && impliedSet.contains(dec)) return true;

            HashSet<Integer> nonImpliedSet = _hmNonImplications.get(test_dec);
            if (nonImpliedSet != null && nonImpliedSet.contains(dec)) return false;

            if (DEBUG_CONSTRAINTS) {
                System.out.println("===================\nisTestImpliedv2 " + "Checking if " + dec + " " + context._alOrder.get(Math.abs(dec)) + " = " + (dec > 0 ? "true" : "false") + " implied by:");
                showDecList(test_dec);
            }
            if (!test_dec.add(-dec))
                System.err.println("Warning: checking if decision implies its negation! - " + test_dec);
            boolean implied = isInfeasible(test_dec);
            test_dec.remove(-dec);
            if (implied) {
                if (impliedSet == null) {
                    impliedSet = new HashSet<Integer>();
                    _hmImplications.put((HashSet<Integer>) test_dec.clone(), impliedSet);
                }
                impliedSet.add(dec);
            } else {
                if (nonImpliedSet == null) {
                    nonImpliedSet = new HashSet<Integer>();
                    _hmNonImplications.put((HashSet<Integer>) test_dec.clone(), nonImpliedSet);
                }
                nonImpliedSet.add(dec);
            }
            return implied;
        }

        private boolean isInfeasible(HashSet<Integer> test_dec) {

            boolean infeasible = false;

            int nvars = nLocalCVars;
            double[] obj_coef = new double[nvars];
            //Test 1:
            // Test 1 is unrealiable, somehow many reportedly feasible do not pass test 2
            //A => B iff A^~B is infeasible) -maximize arbitrary 1 function

            // Setup LP
            for (int i = 0; i < nvars; i++) obj_coef[i] = 1;
            LP lp = new LP(nvars, assign2Local(context.lowerBounds, true), assign2Local(context.upperBounds, true), obj_coef, LP.MAXIMIZE);
            // Now add all constraints
            for (Integer decision : test_dec) {
                addDecision(lp, decision);
            }
            //Adding box constraints
            addLocalBoundConstraints(lp);

            double soln[] = silentSolvelp(lp);

            if (lp._status == LpSolve.INFEASIBLE) {
                if (DEBUG_CONSTRAINTS) {
                    System.out.println("Infeasible: " + test_dec);
                }
                infeasible = true;
            }
            lp.free();

            if (infeasible || SKIP_TEST2) return infeasible;

            //Test 2 - strict feasibility
            // for each constraint c + f*x > 0 the slack is the greatest value S>0 s.t. c + f*x - S >= 0
            // for each constraint c + f*x < 0 the slack is the greatest value S>0 s.t. c + f*x + S <= 0
            // set the objective as the smallest of the slacks from all constraints.
            double objCoef2[] = new double[nvars + 1];
            double lower2[] = new double[nvars + 1];
            double upper2[] = new double[nvars + 1];

            for (int i = 0; i < nvars; i++) {
                objCoef2[i] = 0;
                lower2[i] = context.lowerBounds[localID2cVarID[i]];
                upper2[i] = context.upperBounds[localID2cVarID[i]];
            }
            objCoef2[nvars] = 1;
            lower2[nvars] = 0; //S >0
            upper2[nvars] = XADD.DEFAULT_UPPER_BOUND;

            LP lp2 = new LP(nvars + 1, lower2, upper2, objCoef2, LP.MAXIMIZE);

            double constrCoef2[] = new double[nvars + 1];
            double constC = 0d;
            for (Integer decision : test_dec) {
                for (int k = 0; k < nvars + 1; k++) constrCoef2[k] = 0d;

                Decision d = context._alOrder.get(Math.abs(decision));
                if (!(d instanceof ExprDec)) continue;
                CompExpr compar = (CompExpr) ((ExprDec) d)._expr;
                boolean greaterComp = compar.isGreater();  
                ArithExpr exp = ((CompExpr) ((ExprDec) d)._expr)._lhs;
                constC = setCoefficientsLocal(exp, constrCoef2);
                if ( (greaterComp && decision > 0) || (!greaterComp && decision < 0) ) {
                    constrCoef2[nvars] = -1; // c + f*x > 0 => f*x - S > -c
                    lp2.addGeqConstraint(constrCoef2, -constC);
                } else {
                    constrCoef2[nvars] = 1; // c + f*x < 0 => f*x + S < -c
                    lp2.addLeqConstraint(constrCoef2, -constC);
                }
            }

            double soln2[] = new double[nvars + 1];
            soln2 = silentSolvelp(lp2);
            double maxSlack = lp2._dObjValue;

            
            if (lp2._status == LpSolve.INFEASIBLE) {
                if (!TEST2_INCONSIST_QUIET){
                	System.err.println("Infeasible at test 2? should have failed the first test!");
                	showDecListEval(test_dec, soln);
                }
                infeasible = true;
            } else if (lp2._status != LpSolve.UNBOUNDED && maxSlack < IMPLIED_PRECISION_T2) {
                if (DEBUG_CONSTRAINTS) {
                    System.out.println("Implied only by test 2: Slack = " + soln2[nvars]);
                    //remove slack from soln2 to be a local assign sol
                    double sol[] = new double[nvars];
                    for (int k = 0; k < nvars; k++) sol[k] = soln2[k];
                    showDecListEval(test_dec, sol);
                }
                infeasible = true;
            }
            lp2.free();
            return infeasible;
        }
    }
}
