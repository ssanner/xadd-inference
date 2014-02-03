package mamdp.utils;

import graph.Graph;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import xadd.XADD;
import xadd.XADD.XADDLeafMinOrMax;

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

    public static int regress(int qFuncID, Integer cpfID, HashSet<String> hsBooleanVariables, HashSet<String> hsContinuousVariables) {
        
        HashSet<String> varsToRegress = VIHelper.getXADD().collectVars(qFuncID);
        Graph g = VIHelper.buildDBNDependencyDAG(cpfID, varsToRegress);
        List variableEliminationOrder = g.topologicalSort(true);
        
        // Regress each variable in the topological order
        for (Object obj : variableEliminationOrder) {
            
            String varToEliminate = (String) obj;

            if (hsBooleanVariables.contains(varToEliminate)) {
                qFuncID = regressBooleanVariable(qFuncID, cpfID, varToEliminate);
            }
            else if (hsContinuousVariables.contains(varToEliminate)) {
                qFuncID = regressContinuousVariable(qFuncID, cpfID, varToEliminate);
            }
            
            //ValueIterationHelper.PlotXADD(qFuncID, "Q - after regression (" + varToEliminate + ")");
        }        
        
        return qFuncID;
    }
    
    public static int maxOutVar(int ixadd, String var, double lb, double ub, PrintStream logStream) {

        XADDLeafMinOrMax max = VIHelper.getXADD().new XADDLeafMinOrMax(var, lb, ub, true, logStream);
        ixadd = VIHelper.getXADD().reduceProcessXADDLeaf(ixadd, max, false);

        return max._runningResult;
    }

    public static int regressBooleanVariable(int qFuncID, Integer cpfID, String var) {

        // Get cpf for boolean var'
        int var_id = VIHelper.getXADD().getVarIndex(VIHelper.getXADD().new BoolDec(var), false);
        Integer dd_cpf = cpfID;

        qFuncID = VIHelper.getXADD().apply(qFuncID, dd_cpf, XADD.PROD);

        // Following is a safer way to marginalize (instead of using opOut
        // based on apply) in the event that two branches of a boolean variable
        // had equal probability and were collapsed.
        int restrict_high = VIHelper.getXADD().opOut(qFuncID, var_id, XADD.RESTRICT_HIGH);
        int restrict_low = VIHelper.getXADD().opOut(qFuncID, var_id, XADD.RESTRICT_LOW);
        qFuncID = VIHelper.getXADD().apply(restrict_high, restrict_low, XADD.SUM);

        return qFuncID;
    }

    public static int regressContinuousVariable(int qFuncID, Integer cpfID, String var) {

        // Get cpf for continuous var'
        // int var_id = ValueIterationHelper.getXADD()._cvar2ID.get(var);
        Integer dd_conditional_sub = cpfID;

        // Perform regression via delta function substitution
        qFuncID = VIHelper.getXADD()
                .reduceProcessXADDLeaf(dd_conditional_sub,
                        VIHelper.getXADD().new DeltaFunctionSubstitution(var, qFuncID), true);

        return qFuncID;
    }

    // Works backward from this root factor
    public static Graph buildDBNDependencyDAG(Integer cpfID, HashSet<String> vars) {
        
        Graph g = new Graph(true, false, true, false);
        HashSet<String> already_seen = new HashSet<String>();

        // We don't want to generate parents for the following "base" variables
        // NOTE: Unprimed variables
        Iterator<String> it = vars.iterator();
        
        while(it.hasNext()) {
            already_seen.add(it.next().replace("'", ""));            
            //already_seen.addAll(hsVars);
        }

        for (String var : vars)
            VIHelper.buildDBNDependencyDAGInt(cpfID, var, g, already_seen);

        return g;
    }

    // Consider that vars belong to a parent factor, recursively call
    // for every child factor and link child to parent
    //
    // have R(x1i,b1i,x2'), DAG has (b1i -> x1i -> R), (b1i -> R), (x2' -> R)...
    // {x1i, b1i, x2'}
    // recursively add in parents for each of x2', xli, bli
    public static void buildDBNDependencyDAGInt(Integer cpfID, String parent_var, Graph g, HashSet<String> already_seen) {

        if (already_seen.contains(parent_var))
            return;

        already_seen.add(parent_var);

        Integer dd_cpf = cpfID;
        if (dd_cpf == null) {
            System.err.println("Could not find CPF definition for variable '" + parent_var);
            System.exit(1);
        }

        HashSet<String> children = VIHelper.getXADD().collectVars(dd_cpf);
        for (String child_var : children) {
            // In the case of boolean variables, the dual action diagram
            // contains the parent,
            // because this is not a substitution, it is a distribution over the
            // parent.
            // Hence we need to explicitly prevent boolean variable self-loops
            // -- this is not
            // an error.
            if (!child_var.equals(parent_var)) {
                g.addUniLink(child_var, parent_var);
                // System.out.println("Adding link " + child_var + " --> " +
                // parent_var);
            }
            else if (child_var.equals(parent_var)) {
                // SUSPICIOUS CODE :p (avoid removing variables that dont have
                // dependencies
                g.addNode(parent_var);
            }

            VIHelper.buildDBNDependencyDAGInt(cpfID, child_var, g, already_seen);
        }
    }
}
