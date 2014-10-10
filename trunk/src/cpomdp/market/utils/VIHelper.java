/*
 * VIHelper.java
 * 
 * NOTES:
 *          1. This Class contains modified functions from camdp.ComputeQFunction.java
 *              related to the Value Iteration algorithm
 */
package cpomdp.market.utils;

import xadd.XADD;

/**
 * Contains helper methods for the XADD version of the Value Iteration algorithm 
 * @author Shamin Kinathil
 *
 */
public class VIHelper {

    private static XADD context = null;
    
    public static void setXADDInstance(XADD xaddInstance) {
        if (context == null) {
            context = xaddInstance;
        }
    }
    
    private static XADD getXADD() {
        
        if (context == null) {
            System.out.println("\nXADD uninitialised\n");
            System.exit(1);
        }        
        
        return context;
    }
    
    /*-------------------------------------------------------------------------
     * Value Iteration related Functions
     *-----------------------------------------------------------------------*/

    /**
     * 
     * @param qFuncID
     * @param cpfID
     * @param hsBooleanVariables
     * @param hsContinuousVariables
     * @return
     */
//    @SuppressWarnings("rawtypes")
//	public static int regress(int qFuncID, Integer cpfID, 
//                                        HashSet<String> hsBooleanVariables, 
//                                        HashSet<String> hsContinuousVariables) {
//        
//        HashSet<String> varsToRegress = VIHelper.getXADD().collectVars(qFuncID);
//        Graph g = VIHelper.buildDBNDependencyDAG(cpfID, varsToRegress);
//        List variableEliminationOrder = g.topologicalSort(true);
//
//        XADDHelper.PlotXADD(qFuncID, "Q - before regression ");        
//        
//        // Regress each variable in the topological order
//        for (Object obj : variableEliminationOrder) {
//            
//            String varToEliminate = (String) obj;
//            System.out.println("Eliminating variable " + varToEliminate);
//            
//            if (hsBooleanVariables.contains(varToEliminate)) {
//                qFuncID = regressBooleanVariable(qFuncID, cpfID, varToEliminate);
//            }
//            else if (hsContinuousVariables.contains(varToEliminate)) {
//                qFuncID = RegressContinuousVariable(qFuncID, cpfID, varToEliminate);
//            }
//            
//            XADDHelper.PlotXADD(qFuncID, "Q - after regression (" + varToEliminate + ")");
//        }        
//        
//        return qFuncID;
//    }
    
    /**
     * 
     * @param ixadd
     * @param var
     * @param lb
     * @param ub
     * @param logStream
     * @return
//     */
//    public static int maxOutVar(int ixadd, String var, double lb, double ub, 
//                                                        PrintStream logStream) {
//
//        XADDLeafMinOrMax max = VIHelper.getXADD().new XADDLeafMinOrMax(var, lb, ub, true, logStream);
//        ixadd = VIHelper.getXADD().reduceProcessXADDLeaf(ixadd, max, false);
//
//        return max._runningResult;
//    }

    /**
     * 
     * @param qFuncID
     * @param cpfID
     * @param var
     * @return
     */
//    @SuppressWarnings("unused")
//	public static int regressBooleanVariable(int qFuncID, Integer cpfID, String var) {
//
//        // Get cpf for boolean var'
//        int var_id = VIHelper.getXADD().getVarIndex(VIHelper.getXADD().new BoolDec(var), false);
//        Integer dd_cpf = cpfID;
//        
//        XADDHelper.PlotXADD(var_id, "regressBooleanVariable - var_id");
//        XADDHelper.PlotXADD(qFuncID, "regressBooleanVariable - qFuncID");
//        XADDHelper.PlotXADD(dd_cpf, "regressBooleanVariable - dd_cpf");
//        
//        int bhighID2 = VIHelper.getXADD().opOut(dd_cpf, var_id, XADD.RESTRICT_HIGH);
//        XADDHelper.PlotXADD(bhighID2, "bhighID2");
//        
//        int bhighID3 = VIHelper.getXADD().opOut(qFuncID, var_id, XADD.RESTRICT_HIGH);
//        XADDHelper.PlotXADD(bhighID3, "bhighID3");
//        
//        qFuncID = VIHelper.getXADD().apply(qFuncID, dd_cpf, XADD.PROD);
//        XADDHelper.PlotXADD(qFuncID, "regressBooleanVariable (" + var + ") QFUNC ID");
//        
//        int var_id2 = VIHelper.getXADD().getVarIndex(VIHelper.getXADD().new BoolDec(var), false);
//        
//        // Following is a safer way to marginalize (instead of using opOut
//        // based on apply) in the event that two branches of a boolean variable
//        // had equal probability and were collapsed.
//        int restrict_high = VIHelper.getXADD().opOut(qFuncID, var_id, XADD.RESTRICT_HIGH);
//        XADDHelper.PlotXADD(restrict_high, "regressBooleanVariable (" + var + ") QFUNC ID");
//        
//        int restrict_low = VIHelper.getXADD().opOut(qFuncID, var_id, XADD.RESTRICT_LOW);
//        XADDHelper.PlotXADD(restrict_low, "regressBooleanVariable (" + var + ") QFUNC ID");
//        
//        qFuncID = VIHelper.getXADD().apply(restrict_high, restrict_low, XADD.SUM);
//        XADDHelper.PlotXADD(qFuncID, "regressBooleanVariable (" + var + ") XADD.SUM");
//
//        return qFuncID;
//    }

    /**
     * 
     * @param qFuncID
     * @param cpfID
     * @param var
     * @return
     */
    public static int RegressContinuousVariable(int qFuncID, Integer cpfID, String var) {

        // Get cpf for continuous var'
        // int var_id = ValueIterationHelper.getXADD()._cvar2ID.get(var);
        Integer dd_conditional_sub = cpfID;
        
//        XADDHelper.PlotXADD(qFuncID, "regressContinuousVariable - qFuncID Before");
//        XADDHelper.PlotXADD(cpfID, "regressContinuousVariable - cpfID");
        
        // Perform regression via delta function substitution
        qFuncID = XADDWrapper.getInstance().reduceProcessXADDLeaf(dd_conditional_sub,
        		XADDWrapper.getInstance().new DeltaFunctionSubstitution(var, qFuncID), true);
        
//        XADDHelper.PlotXADD(qFuncID, "regressContinuousVariable - qFuncID After");
        
        return qFuncID;
    }

    // Works backward from this root factor
    /**
     * 
     * @param cpfID
     * @param vars
     * @return
     */
//    public static Graph buildDBNDependencyDAG(Integer cpfID, HashSet<String> vars) {
//        
//        Graph g = new Graph(true, false, true, false);
//        HashSet<String> already_seen = new HashSet<String>();
//
//        // We don't want to generate parents for the following "base" variables
//        // NOTE: Unprimed variables
//        Iterator<String> it = vars.iterator();
//        
//        while(it.hasNext()) {
//            already_seen.add(it.next().replace("'", ""));            
//            //already_seen.addAll(hsVars);
//        }
//
//        for (String var : vars)
//            VIHelper.buildDBNDependencyDAGInt(cpfID, var, g, already_seen);
//
//        return g;
//    }

    // Consider that vars belong to a parent factor, recursively call
    // for every child factor and link child to parent
    //
    // have R(x1i,b1i,x2'), DAG has (b1i -> x1i -> R), (b1i -> R), (x2' -> R)...
    // {x1i, b1i, x2'}
    // recursively add in parents for each of x2', xli, bli
    /**
     * 
     * @param cpfID
     * @param parent_var
     * @param g
     * @param already_seen
     */
//    public static void buildDBNDependencyDAGInt(Integer cpfID, String parent_var, 
//                                        Graph g, HashSet<String> already_seen) {
//
//        if (already_seen.contains(parent_var))
//            return;
//
//        already_seen.add(parent_var);
//
//        Integer dd_cpf = cpfID;
//        if (dd_cpf == null) {
//            System.err.println("Could not find CPF definition for variable '" + parent_var);
//            System.exit(1);
//        }
//
//        HashSet<String> children = VIHelper.getXADD().collectVars(dd_cpf);
//        for (String child_var : children) {
//            // In the case of boolean variables, the dual action diagram
//            // contains the parent,
//            // because this is not a substitution, it is a distribution over the
//            // parent.
//            // Hence we need to explicitly prevent boolean variable self-loops
//            // -- this is not
//            // an error.
//            if (!child_var.equals(parent_var)) {
//                g.addUniLink(child_var, parent_var);
//                // System.out.println("Adding link " + child_var + " --> " +
//                // parent_var);
//            }
//            else if (child_var.equals(parent_var)) {
//                // SUSPICIOUS CODE :p (avoid removing variables that dont have
//                // dependencies
//                g.addNode(parent_var);
//            }
//
//            VIHelper.buildDBNDependencyDAGInt(cpfID, child_var, g, already_seen);
//        }
//    }
}
