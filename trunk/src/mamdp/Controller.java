/**
 * 
 */

package mamdp;

import graph.Graph;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import util.IntTriple;
import xadd.ExprLib;
import xadd.ExprLib.ArithExpr;
import xadd.ExprLib.VarExpr;
import xadd.XADD;
import xadd.XADD.BoolDec;
import xadd.XADD.DeltaFunctionSubstitution;
import xadd.XADD.XADDLeafMinOrMax;
import xadd.XADDUtils;
import camdp.CAction;
import camdp.CAMDP.FileOptions;

/**
 * File:    Controller
 * @author  Shamin Kinathil
 * Date:    20140108    
 * NOTES:   Coordinates all experiments for this project  
 */
public class Controller {
    
    public static final String USAGE_STRING = "Usage: [XADD-filename]";
    
    private static final String SOURCE_DIR = "src";
    private static final String DOMAIN_DIR = "domains";
    
    private static String RootDirectoryPath = "";
    private static String DomainDirectoryPath = "";
    
    private static XADD context = null;

    private enum Domains {
    	MATCHING_PENNIES,
    	CONTINGENT_CLAIM
    };
    
    private static void Usage() {
        System.out.println("\n" + Controller.USAGE_STRING);
        System.exit(1);
    }   

    private static String ProjectRootDirectory() {
        
        if(Controller.RootDirectoryPath.isEmpty()) {
            Controller.RootDirectoryPath = System.getProperty("user.dir") +
        	    				File.separator + SOURCE_DIR +
    	    					File.separator + "mamdp";
        }
        
        return Controller.RootDirectoryPath;
    }
    
    private static String DomainDirectory(String domain) {
	
	if(Controller.DomainDirectoryPath.isEmpty()) {
	    Controller.DomainDirectoryPath = Controller.ProjectRootDirectory() +
    					File.separator + Controller.DOMAIN_DIR;
	}
	
        return Controller.DomainDirectoryPath + File.separator + domain;
    }    

    /*-------------------------------------------------------------------------
     * XADD related functions 
     *-----------------------------------------------------------------------*/
    
    private static XADD getXADD() {
        
        if(context == null) {
            context = new XADD();
        }
        
        return context;
     }    
    
    private static int BuildXADD(String fileLocation) {
        
        File specFile = new File(fileLocation);
        if(!(specFile.exists() && specFile.isFile() && specFile.canRead())) {
            System.out.println("\nError: Unable to locate readable file: '" + "'" + specFile.toString());
        }

        return Controller.getXADD().buildCanonicalXADDFromFile(specFile.toString());
    }
    
    private static void PlotXADD(int xaddID, String plotTitle) {
        Graph gc = Controller.getXADD().getGraph(xaddID);
        gc.launchViewer(plotTitle);
    }
    
    private static void ExportXADD(int xadd_id, String label, String problemFile, String logFileRoot) {
        
        label = label.replace(".csamdp", "").replace(".camdp", "")
                    .replace(".cmdp", "").replace('^', '_').replace("(", "")
                    .replace(")", "").replace(":", "_").replace(" ", "");
        
        String xadd_filename = logFileRoot + "." + label + ".xadd";
        Controller.getXADD().exportXADDToFile(xadd_id, xadd_filename);

        // Copy over plotting options if they exist
        File file2D = new File(problemFile + ".2d");
        if (file2D.exists()) {
            FileOptions opt = new FileOptions(problemFile + ".2d");
            opt.exportToFile(xadd_filename + ".2d");
        }
        
        File file3D = new File(problemFile + ".3d");
        if (file3D.exists()) {
            FileOptions opt = new FileOptions(problemFile + ".3d");
            opt.exportToFile(xadd_filename + ".3d");
        }
    }
    
    private static void Display2D(int xadd_id, String label, String problemFile, String logFileRoot) {
        
        Controller.ExportXADD(xadd_id, label, problemFile, logFileRoot);
        
        // If DISPLAY_2D is enabled, it is expected that necessary parameters 
        // have been placed in a _problemFile + ".2d"
        FileOptions opt = new FileOptions(problemFile + ".2d");

        XADDUtils.PlotXADD(Controller.getXADD(), xadd_id, 
                opt._varLB.get(0), opt._varInc.get(0), opt._varUB.get(0), 
                opt._bassign, opt._dassign, opt._var.get(0), logFileRoot + "." + label);
    }
    
    private static int maxOutVar(int ixadd, String var, double lb, double ub, PrintStream logStream) {
        
        XADDLeafMinOrMax max = Controller.getXADD().new XADDLeafMinOrMax(var, lb, ub, true, logStream);
        ixadd  = Controller.getXADD().reduceProcessXADDLeaf(ixadd, max, false);
        
        return max._runningResult;
    }

    public static int regressBooleanVariable(int qFuncID, Integer cpfID, String var) {

        // Get cpf for boolean var'
        int var_id = Controller.getXADD().getVarIndex(Controller.getXADD().new BoolDec(var), false);
        Integer dd_cpf = cpfID;
        
        qFuncID = Controller.getXADD().apply(qFuncID, dd_cpf, XADD.PROD);

        // Following is a safer way to marginalize (instead of using opOut
        // based on apply) in the event that two branches of a boolean variable
        // had equal probability and were collapsed.
        int restrict_high = Controller.getXADD().opOut(qFuncID, var_id, XADD.RESTRICT_HIGH);
        int restrict_low = Controller.getXADD().opOut(qFuncID, var_id, XADD.RESTRICT_LOW);
        qFuncID = Controller.getXADD().apply(restrict_high, restrict_low, XADD.SUM);

        return qFuncID;
    }    
    
    public static int regressContinuousVariable(int qFuncID, Integer cpfID, String var) {

        // Get cpf for continuous var'
        //int var_id = Controller.getXADD()._cvar2ID.get(var);
        Integer dd_conditional_sub = cpfID;

        // Perform regression via delta function substitution
        qFuncID = Controller.getXADD().reduceProcessXADDLeaf(dd_conditional_sub,
        	Controller.getXADD().new DeltaFunctionSubstitution(var, qFuncID), true);

        return qFuncID;
    }    
    
    // Works backward from this root factor
    private static Graph buildDBNDependencyDAG(Integer cpfID, HashSet<String> vars, HashSet<String> hsVars) {
        Graph g = new Graph(true, false, true, false);
        HashSet<String> already_seen = new HashSet<String>();

        // We don't want to generate parents for the following "base" variables
        already_seen.addAll(hsVars);

        for (String var : vars)
            Controller.buildDBNDependencyDAGInt(cpfID, var, g, already_seen);

        return g;
    }    
    
    // Consider that vars belong to a parent factor, recursively call
    // for every child factor and link child to parent
    //
    // have R(x1i,b1i,x2'), DAG has (b1i -> x1i -> R), (b1i -> R), (x2' -> R)... {x1i, b1i, x2'}
    // recursively add in parents for each of x2', xli, bli
    private static void buildDBNDependencyDAGInt(Integer cpfID, String parent_var, Graph g, HashSet<String> already_seen) {
        
	if (already_seen.contains(parent_var))
            return;
        
	already_seen.add(parent_var);
        
	Integer dd_cpf = cpfID;
        if (dd_cpf == null) {
            System.err.println("Could not find CPF definition for variable '" + parent_var);
            System.exit(1);
        }
        
        HashSet<String> children = Controller.getXADD().collectVars(dd_cpf);
        for (String child_var : children) {
            // In the case of boolean variables, the dual action diagram contains the parent,
            // because this is not a substitution, it is a distribution over the parent.
            // Hence we need to explicitly prevent boolean variable self-loops -- this is not
            // an error.
            if (!child_var.equals(parent_var)) {
                g.addUniLink(child_var, parent_var);
                //System.out.println("Adding link " + child_var + " --> " + parent_var);
            } else if (child_var.equals(parent_var)) {
                // SUSPICIOUS CODE :p (avoid removing variables that dont have dependencies
                g.addNode(parent_var);
            }
            
            Controller.buildDBNDependencyDAGInt(cpfID, child_var, g, already_seen);
        }
    }    
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        
        /*----------------------------------------------------------------------
         * Parse the input arguments
         ---------------------------------------------------------------------*/
        
        //Controller.MatchingPennies();
        Controller.FiniteHorizonStochasticGame("sequential_matching_pennies", 5);
    }    
    
    /**
     * Finite Horizon Stochastic Game
     */
    private static void FiniteHorizonStochasticGame(String domainName, Integer numIterations) {
	
        String domainDir = Controller.DomainDirectory(domainName);
        String rewardFile = domainDir + File.separator + "reward.xadd";
        String constraintsFile = domainDir + File.separator + "constraints.xadd";
        String transitionsFile = domainDir + File.separator + "transitions.xadd";
        String logFile = domainDir + File.separator + domainName + ".log";
        String plot2DFile = domainDir + File.separator + "plot";

        Integer maxID = null;
        Integer prevID = null;        
        Integer vFuncID = Controller.getXADD().ZERO;
        Integer qFuncID = null;
        
        double discountValue = 1.0;
        
        HashMap<String, ArithExpr> hmPrimeSubs = new HashMap<String, ArithExpr>();
        HashSet<String> hsVariables = new HashSet<String>();
        HashSet<String> hsBooleanVariables = new HashSet<String>();
        HashSet<String> hsContinuousVariables = new HashSet<String>();
        
        PrintStream logStream = null;

        int currIterationNum = 0;
        
        /*
         * 0. Set up 
         */

        try {
            logStream = new PrintStream(new FileOutputStream(logFile));
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        
        // Build the XADDs
        int constraintsID = Controller.BuildXADD(constraintsFile);
        int rewardID = Controller.BuildXADD(rewardFile);
        int transitionsID = Controller.BuildXADD(transitionsFile);

        Controller.PlotXADD(constraintsID, "Constraints");
        Controller.PlotXADD(rewardID, "Reward");
        Controller.PlotXADD(transitionsID, "Transition CPF");

        int aVarID = Controller.getXADD().getVarIndex(Controller.getXADD().new BoolDec("a"), false);
        int bVarID = Controller.getXADD().getVarIndex(Controller.getXADD().new BoolDec("b"), false);

        int pTID = Controller.getXADD().getTermNode(new VarExpr("p"));
        int pFID = Controller.getXADD().scalarOp(Controller.getXADD().scalarOp(pTID, -1, XADD.PROD), 1, XADD.SUM);

        hmPrimeSubs.put("x", new VarExpr("x'"));
        hmPrimeSubs.put("a", new VarExpr("a'"));
        hmPrimeSubs.put("b", new VarExpr("b'"));
        
        hsBooleanVariables.add("a");
        hsBooleanVariables.add("b");
        hsContinuousVariables.add("x");
        
        hsVariables.addAll(hsBooleanVariables);        
        hsVariables.addAll(hsContinuousVariables);
        
        /*
         *	1.  
         */
        
        while (currIterationNum < numIterations) {
            currIterationNum++;

            System.out.println("Iteration number: " + currIterationNum);
            
            // Prime the vFuncID
            prevID = vFuncID;
            
            // Iterate over each action
            maxID = null;
            
            // Prime the value function
            Controller.PlotXADD(vFuncID, "Value before Subs");
            qFuncID = Controller.getXADD().substitute(vFuncID, hmPrimeSubs);
            Controller.PlotXADD(qFuncID, "Value after Subs");
            
            // Discount
            qFuncID = Controller.getXADD().scalarOp(qFuncID, discountValue, XADD.PROD);
            
            HashSet<String> varsToRegress = Controller.getXADD().collectVars(qFuncID);
            Graph g = Controller.buildDBNDependencyDAG(transitionsID, varsToRegress, varsToRegress);            
            List variableEliminationOrder = g.topologicalSort(true);
            
            // Regress each variable in the topological order
            for(Object obj : variableEliminationOrder) {
        	String varToEliminate = (String) obj;
        	
        	if(hsBooleanVariables.contains(varToEliminate)) {
        	    qFuncID = regressBooleanVariable(qFuncID, transitionsID, varToEliminate);
        	} else if (hsContinuousVariables.contains(varToEliminate)) {
        	    qFuncID = regressContinuousVariable(qFuncID, transitionsID, varToEliminate);
        	}
            }
            
            // Add reward
            qFuncID = Controller.getXADD().apply(rewardID, qFuncID, XADD.SUM);
            Controller.PlotXADD(qFuncID, "Value after reward");
            
            vFuncID = qFuncID;
            
            // Check for convergence
            if(prevID.equals(vFuncID)) {
        	System.out.println("Converged at iteration " + currIterationNum);
        	break;
            }
        }
    }
    
    /**
     * Matching pennies example
     */
    private static void MatchingPennies() {
        
        String domainDir = Controller.DomainDirectory("matching_pennies");
        String rewardFile = domainDir + File.separator + "reward.xadd";
        String constraintsFile = domainDir + File.separator + "constraints.xadd";
        String plot2DFile = domainDir + File.separator + "plot";
        String logFile = domainDir + File.separator + "matching_pennies.log";

        Integer vFuncID = null;
        PrintStream logStream = null;
        
        try {
            logStream = new PrintStream(new FileOutputStream(logFile));
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        /*
         * 0. Set up 
         */
        
        // Build the XADDs
        int constraintsID = Controller.BuildXADD(constraintsFile);
        int rewardID = Controller.BuildXADD(rewardFile);

        Controller.PlotXADD(constraintsID, "Constraints");
        Controller.PlotXADD(rewardID, "Reward");
        
        int aVarID = Controller.getXADD().getVarIndex(Controller.getXADD().new BoolDec("a"), false);
        int bVarID = Controller.getXADD().getVarIndex(Controller.getXADD().new BoolDec("b"), false);
        
        int pTID = Controller.getXADD().getTermNode(new VarExpr("p"));
        int pFID = Controller.getXADD().scalarOp(Controller.getXADD().scalarOp(pTID, -1, XADD.PROD), 1, XADD.SUM);
        
        vFuncID = rewardID;
        
        /*
         * 1. Sum over (marginalise) the actions for Player A ('a')
         */
        
        int highID = Controller.getXADD().opOut(rewardID, aVarID, XADD.RESTRICT_HIGH);
        int lowID  = Controller.getXADD().opOut(rewardID, aVarID, XADD.RESTRICT_LOW);
        
        vFuncID = Controller.getXADD().apply(Controller.getXADD().apply(highID, pTID, XADD.PROD), 
                                             Controller.getXADD().apply(lowID, pFID, XADD.PROD), 
                                                    				XADD.SUM);
        
        Controller.PlotXADD(vFuncID, "After 1");
        
        /*
         * 2. Minimise over the actions for Player B ('b')
         */

        highID = Controller.getXADD().opOut(vFuncID, bVarID, XADD.RESTRICT_HIGH);
        lowID  = Controller.getXADD().opOut(vFuncID, bVarID, XADD.RESTRICT_LOW);
        
        vFuncID = Controller.getXADD().apply(highID, lowID, XADD.MIN);
        
        Controller.PlotXADD(vFuncID, "After 2");

        /*
         * 3. Max out the ...
         */
        
        vFuncID = Controller.maxOutVar(vFuncID, "p", 0, 1, logStream);        
        Controller.PlotXADD(vFuncID, "After 3");
        
        /*
         *  Plot the value function 
         */
        Controller.Display2D(vFuncID, "Value Function", plot2DFile, domainDir);
    }
}
