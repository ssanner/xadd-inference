/**
 * 
 */

package mamdp;

import graph.Graph;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import xadd.ExprLib.ArithExpr;
import xadd.ExprLib.VarExpr;
import xadd.XADD;
import xadd.XADD.XADDLeafMinOrMax;
import xadd.XADDUtils;
import camdp.CAMDP.FileOptions;

/**
 * File: Controller
 * 
 * @author Shamin Kinathil Date: 20140108 NOTES: Coordinates all experiments for
 *         this project
 */
public class Controller {

    public static final String USAGE_STRING = "Usage: [XADD-filename]";

    private static final String SOURCE_DIR = "src";
    private static final String DOMAIN_DIR = "domains";

    private static String RootDirectoryPath = "";
    private static String DomainDirectoryPath = "";

    private static XADD context = null;

    private enum Domains {
        MATCHING_PENNIES, CONTINGENT_CLAIM
    };

    private static void Usage() {
        System.out.println("\n" + Controller.USAGE_STRING);
        System.exit(1);
    }

    private static String ProjectRootDirectory() {

        if (Controller.RootDirectoryPath.isEmpty()) {
            Controller.RootDirectoryPath = System.getProperty("user.dir")
                    + File.separator + SOURCE_DIR + File.separator + "mamdp";
        }

        return Controller.RootDirectoryPath;
    }

    private static String DomainDirectory(String domain) {

        if (Controller.DomainDirectoryPath.isEmpty()) {
            Controller.DomainDirectoryPath = Controller.ProjectRootDirectory()
                    + File.separator + Controller.DOMAIN_DIR;
        }

        return Controller.DomainDirectoryPath + File.separator + domain;
    }

    /*-------------------------------------------------------------------------
     * XADD related functions 
     *-----------------------------------------------------------------------*/

    private static XADD getXADD() {

        if (context == null) {
            context = new XADD();
        }

        return context;
    }

    private static int BuildXADD(String fileLocation) {

        File specFile = new File(fileLocation);
        if (!(specFile.exists() && specFile.isFile() && specFile.canRead())) {
            System.out.println("\nError: Unable to locate readable file: '"
                    + "'" + specFile.toString());
        }

        return Controller.getXADD().buildCanonicalXADDFromFile(
                specFile.toString());
    }

    private static void PlotXADD(int xaddID, String plotTitle) {
        Graph gc = Controller.getXADD().getGraph(xaddID);
        gc.launchViewer(plotTitle);
    }

    private static void ExportXADD(int xadd_id, String label,
            String problemFile, String logFileRoot) {

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

    private static void Display2D(int xadd_id, String label,
            String problemFile, String logFileRoot) {

        Controller.ExportXADD(xadd_id, label, problemFile, logFileRoot);

        // If DISPLAY_2D is enabled, it is expected that necessary parameters
        // have been placed in a _problemFile + ".2d"
        FileOptions opt = new FileOptions(problemFile + ".2d");

        XADDUtils.PlotXADD(Controller.getXADD(), xadd_id, opt._varLB.get(0),
                opt._varInc.get(0), opt._varUB.get(0), opt._bassign,
                opt._dassign, opt._var.get(0), logFileRoot + "." + label);
    }

    private static void Display3D(int xadd_id, String label,
                            String problemFile, String logFileRoot) {

        // If DISPLAY_3D is enabled, it is expected that necessary parameters 
        // have been placed in a _problemFile + ".3d"
        FileOptions opt = new FileOptions(problemFile + ".3d");
        
        XADDUtils.Plot3DSurfXADD(Controller.getXADD(), xadd_id, 
                opt._varLB.get(0), opt._varInc.get(0), opt._varUB.get(0), 
                opt._varLB.get(1), opt._varInc.get(1), opt._varUB.get(1), 
                opt._bassign, opt._dassign, opt._var.get(0), opt._var.get(1), 
                logFileRoot + "." + label);
    }
    
    
    /*-------------------------------------------------------------------------
     * Value Iteration related Functions
     *-----------------------------------------------------------------------*/

    private static int regress(int qFuncID, Integer cpfID, 
            HashSet<String> hsBooleanVariables, HashSet<String> hsContinuousVariables) {
        
        HashSet<String> varsToRegress = Controller.getXADD().collectVars(qFuncID);
        Graph g = Controller.buildDBNDependencyDAG(cpfID, varsToRegress);
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
            
            //Controller.PlotXADD(qFuncID, "Q - after regression (" + varToEliminate + ")");
        }        
        
        return qFuncID;
    }
    
    private static int maxOutVar(int ixadd, String var, double lb, double ub,
            PrintStream logStream) {

        XADDLeafMinOrMax max = Controller.getXADD().new XADDLeafMinOrMax(var,
                lb, ub, true, logStream);
        ixadd = Controller.getXADD().reduceProcessXADDLeaf(ixadd, max, false);

        return max._runningResult;
    }

    private static int regressBooleanVariable(int qFuncID, Integer cpfID,
            String var) {

        // Get cpf for boolean var'
        int var_id = Controller.getXADD().getVarIndex(
                Controller.getXADD().new BoolDec(var), false);
        Integer dd_cpf = cpfID;

        qFuncID = Controller.getXADD().apply(qFuncID, dd_cpf, XADD.PROD);

        // Following is a safer way to marginalize (instead of using opOut
        // based on apply) in the event that two branches of a boolean variable
        // had equal probability and were collapsed.
        int restrict_high = Controller.getXADD().opOut(qFuncID, var_id,
                XADD.RESTRICT_HIGH);
        int restrict_low = Controller.getXADD().opOut(qFuncID, var_id,
                XADD.RESTRICT_LOW);
        qFuncID = Controller.getXADD().apply(restrict_high, restrict_low,
                XADD.SUM);

        return qFuncID;
    }

    private static int regressContinuousVariable(int qFuncID, Integer cpfID,
            String var) {

        // Get cpf for continuous var'
        // int var_id = Controller.getXADD()._cvar2ID.get(var);
        Integer dd_conditional_sub = cpfID;

        // Perform regression via delta function substitution
        qFuncID = Controller.getXADD()
                .reduceProcessXADDLeaf(dd_conditional_sub,
                        Controller.getXADD().new DeltaFunctionSubstitution(var, qFuncID), true);

        return qFuncID;
    }

    // Works backward from this root factor
    private static Graph buildDBNDependencyDAG(Integer cpfID, HashSet<String> vars) {
        
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
            Controller.buildDBNDependencyDAGInt(cpfID, var, g, already_seen);

        return g;
    }

    // Consider that vars belong to a parent factor, recursively call
    // for every child factor and link child to parent
    //
    // have R(x1i,b1i,x2'), DAG has (b1i -> x1i -> R), (b1i -> R), (x2' -> R)...
    // {x1i, b1i, x2'}
    // recursively add in parents for each of x2', xli, bli
    private static void buildDBNDependencyDAGInt(Integer cpfID,
            String parent_var, Graph g, HashSet<String> already_seen) {

        if (already_seen.contains(parent_var))
            return;

        already_seen.add(parent_var);

        Integer dd_cpf = cpfID;
        if (dd_cpf == null) {
            System.err.println("Could not find CPF definition for variable '"
                    + parent_var);
            System.exit(1);
        }

        HashSet<String> children = Controller.getXADD().collectVars(dd_cpf);
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

            Controller.buildDBNDependencyDAGInt(cpfID, child_var, g,
                    already_seen);
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
        //Controller.SequentialMatchingPennies(2);
        //Controller.Chicken();
        
        /*
         * One Shot Games
         */        
        //Controller.OneShotGame("chicken");
        //Controller.OneShotGame("matching_pennies");
        
        /*
         * Sequential Games
         */
        //Controller.OneShotBinaryOptionGame("binary_option", 1);
        Controller.FiniteHorizonGame("binary_option", 10);        
    }
    
    private static void OneShotGame(String domainName) {
        
        String domainDir = Controller.DomainDirectory(domainName);
        String rewardFile = domainDir + File.separator + "reward.xadd";
        String constraintsFile = domainDir + File.separator + "constraints.xadd";
        String plot2DFile = domainDir + File.separator + "plot";
        String logFile = domainDir + File.separator + domainName + ".log";

        Integer vFuncID = null;
        Integer qFuncID = null;
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

        int aVarID = Controller.getXADD().getVarIndex(
                                Controller.getXADD().new BoolDec("a"), false);
        int bVarID = Controller.getXADD().getVarIndex(
                                Controller.getXADD().new BoolDec("b"), false);

        int pTID = Controller.getXADD().getTermNode(new VarExpr("p"));
        int pFID = Controller.getXADD()
                    .scalarOp(Controller.getXADD().scalarOp(pTID, -1, XADD.PROD),
                                                                    1, XADD.SUM);

        // Set the Q-function to be the reward function
        // NOTE:    This is because it is a one-shot problem
        qFuncID = rewardID;

        /*
         * 1. Sum over (marginalise) the actions for Player A ('a')
         */

        int highID = Controller.getXADD().opOut(rewardID, aVarID,
                                                            XADD.RESTRICT_HIGH);
        int lowID = Controller.getXADD().opOut(rewardID, aVarID,
                                                            XADD.RESTRICT_LOW);

        qFuncID = Controller.getXADD().apply(
                    Controller.getXADD().apply(highID, pTID, XADD.PROD),
                    Controller.getXADD().apply(lowID, pFID, XADD.PROD), XADD.SUM);

        Controller.PlotXADD(qFuncID, "After 1");

        /*
         * 2. Minimise over the actions for Player B ('b')
         */

        highID = Controller.getXADD()
                .opOut(qFuncID, bVarID, XADD.RESTRICT_HIGH);
        lowID = Controller.getXADD().opOut(qFuncID, bVarID, XADD.RESTRICT_LOW);

        qFuncID = Controller.getXADD().apply(highID, lowID, XADD.MIN);

        Controller.PlotXADD(qFuncID, "After 2");

        /*
         * 3. Maximise over the 
         */

        vFuncID = Controller.maxOutVar(qFuncID, "p", 0, 1, logStream);
        Controller.PlotXADD(vFuncID, "After 3");

        /*
         * Plot the value function
         */
        String plotTitle = "Value Function - " + domainName;
        Controller.Display2D(vFuncID, plotTitle, plot2DFile, domainDir);        
        
    }
    
    private static void OneShotBinaryOptionGame(String domainName, Integer horizon) {
        
        String domainDir = Controller.DomainDirectory(domainName);
        String rewardFile = domainDir + File.separator + "reward.xadd";
        String transitionsFile = domainDir + File.separator + "transitions.xadd";
        String inventoryFile = domainDir + File.separator + "inventory_transition.xadd";
        String logFile = domainDir + File.separator + domainName + ".log";
        String plot2DFile = domainDir + File.separator + "plot";
        
        Integer qFuncID = null;
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
        int rewardID = Controller.BuildXADD(rewardFile);
        int valueTransitionID = Controller.BuildXADD(transitionsFile);
        int inventoryTransitionID = Controller.BuildXADD(inventoryFile);

        int executeTransitionID = Controller.BuildXADD(domainDir + File.separator + "execute_transition.xadd");
        int buyTransitionID = Controller.BuildXADD(domainDir + File.separator + "buy_transition.xadd");
        int sellTransitionID = Controller.BuildXADD(domainDir + File.separator + "sell_transition.xadd");
                
        Controller.PlotXADD(rewardID, "Reward - " + domainName);
        Controller.PlotXADD(valueTransitionID, "Value Transition - " + domainName);
        Controller.PlotXADD(inventoryTransitionID, "Inventory Transition - " + domainName);
        Controller.PlotXADD(executeTransitionID, "Execute - " + domainName);
        Controller.PlotXADD(buyTransitionID, "Buy - " + domainName);
        Controller.PlotXADD(sellTransitionID, "Sell - " + domainName);

        
        /*
         * 
         */
        
        // Set the Q-function to be the reward function
        // NOTE:    This is because it is a one-shot problem
        qFuncID = rewardID;
        
        /*
         * 1. Sum over (marginalise) the actions for Player A ('a')
         */

        int eVarID = Controller.getXADD().getVarIndex(Controller.getXADD().new BoolDec("e"), false);
        int bVarID = Controller.getXADD().getVarIndex(Controller.getXADD().new BoolDec("b"), false);
        
        int peTID = Controller.getXADD().getTermNode(new VarExpr("pe"));
        int peFID = Controller.getXADD().scalarOp(Controller.getXADD().scalarOp(peTID, -1, XADD.PROD),
                                                                    1, XADD.SUM);
        int pbTID = Controller.getXADD().getTermNode(new VarExpr("pb"));
        int pbFID = Controller.getXADD().scalarOp(Controller.getXADD().scalarOp(peTID, -1, XADD.PROD),
                                                                    1, XADD.SUM);
        
        int ehighID = Controller.getXADD().opOut(rewardID, eVarID, XADD.RESTRICT_HIGH);
        int elowID = Controller.getXADD().opOut(rewardID, eVarID, XADD.RESTRICT_LOW);

        int bhighID = Controller.getXADD().opOut(ehighID, bVarID, XADD.RESTRICT_HIGH);
        int blowID = Controller.getXADD().opOut(elowID, bVarID, XADD.RESTRICT_LOW);
        
        Controller.PlotXADD(ehighID, "E");
        Controller.PlotXADD(elowID, "~E");

        Controller.PlotXADD(bhighID, "B");
        Controller.PlotXADD(blowID, "~B");
        
        // Execute action
        int eXADDId = Controller.getXADD().apply(
                Controller.getXADD().apply(ehighID, peTID, XADD.PROD),
                Controller.getXADD().apply(elowID, peFID, XADD.PROD), 
                                                        XADD.SUM);
                
        // Buy action        
        int bXADDId = Controller.getXADD().apply(
                           Controller.getXADD().apply(bhighID, pbTID, XADD.PROD),
                           Controller.getXADD().apply(blowID, pbFID, XADD.PROD), 
                                                                   XADD.SUM);
        
        qFuncID = Controller.getXADD().apply(eXADDId, bXADDId, XADD.SUM);
        
        Controller.PlotXADD(qFuncID, "After 1");        
        
        /*
         * 2. Minimise over the actions for Player B ('b')
         */

        int sVarID = Controller.getXADD().getVarIndex(Controller.getXADD().new BoolDec("s"), false);
        
        int shighID = Controller.getXADD().opOut(qFuncID, sVarID, XADD.RESTRICT_HIGH);
        int slowID = Controller.getXADD().opOut(qFuncID, sVarID, XADD.RESTRICT_LOW);

        qFuncID = Controller.getXADD().apply(shighID, slowID, XADD.MIN);

        Controller.PlotXADD(qFuncID, "After 2");        

        /*
         * 3. Maximise over the 
         */

        vFuncID = Controller.maxOutVar(qFuncID, "pe", 0, 1, logStream);
        vFuncID = Controller.maxOutVar(vFuncID, "pb", 0, 1, logStream);
        
        Controller.PlotXADD(vFuncID, "After 3");        
        
        /*
         * Plot the value function
         */
        String plotTitle = "Value Function - " + domainName;
        //Controller.Display2D(vFuncID, plotTitle, plot2DFile, domainDir);
        Controller.Display3D(vFuncID, plotTitle, plot2DFile, domainDir);
    }
    
    private static void FiniteHorizonGame(String domainName, Integer horizon) {
    
        String domainDir = Controller.DomainDirectory(domainName);
        String rewardFile = domainDir + File.separator + "reward.xadd";
        String transitionsFile = domainDir + File.separator + "transitions.xadd";
        String inventoryFile = domainDir + File.separator + "inventory_transition.xadd";
        String logFile = domainDir + File.separator + domainName + ".log";
        String plot3DFile = domainDir + File.separator + "plot";
        
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
        int rewardID = Controller.BuildXADD(rewardFile);
        int valueTransitionID = Controller.BuildXADD(transitionsFile);
        int inventoryTransitionID = Controller.BuildXADD(inventoryFile);

        int executeTransitionID = Controller.BuildXADD(domainDir + File.separator + "execute_value_transition.xadd");
        int buyTransitionID = Controller.BuildXADD(domainDir + File.separator + "buy_value_transition.xadd");
        int buyInvTransitionID = Controller.BuildXADD(domainDir + File.separator + "buy_inventory_transition.xadd");
        int sellTransitionID = Controller.BuildXADD(domainDir + File.separator + "sell_value_transition.xadd");
                
        Controller.PlotXADD(rewardID, "Reward - " + domainName);
        Controller.PlotXADD(valueTransitionID, "Value Transition - " + domainName);
        Controller.PlotXADD(inventoryTransitionID, "Inventory Transition - " + domainName);
                
        Controller.PlotXADD(executeTransitionID, "Execute - " + domainName);
        Controller.PlotXADD(buyTransitionID, "Buy - " + domainName);
        Controller.PlotXADD(buyInvTransitionID, "Buy Inventory - " + domainName);
        Controller.PlotXADD(sellTransitionID, "Sell - " + domainName);           
        
        hmPrimeSubs.put("x1", new VarExpr("x1'"));
        hmPrimeSubs.put("x2", new VarExpr("x2'"));
        hmPrimeSubs.put("e", new VarExpr("e'"));
        hmPrimeSubs.put("b", new VarExpr("b'"));
        hmPrimeSubs.put("s", new VarExpr("s'"));

        hsBooleanVariables.add("e");
        hsBooleanVariables.add("b");
        hsBooleanVariables.add("s");
        
        hsContinuousVariables.add("x1");
        hsContinuousVariables.add("x2");
        hsContinuousVariables.add("x1'");
        hsContinuousVariables.add("x2'");
        
        hsVariables.addAll(hsBooleanVariables);
        hsVariables.addAll(hsContinuousVariables);        
                
        int eVarID = Controller.getXADD().getVarIndex(Controller.getXADD().new BoolDec("e"), false);
        int bVarID = Controller.getXADD().getVarIndex(Controller.getXADD().new BoolDec("b"), false);
        int sVarID = Controller.getXADD().getVarIndex(Controller.getXADD().new BoolDec("s"), false);
        
        int peTID = Controller.getXADD().getTermNode(new VarExpr("pe"));
        int peFID = Controller.getXADD().scalarOp(Controller.getXADD().scalarOp(peTID, -1, XADD.PROD),
                                                                    1, XADD.SUM);
        int pbTID = Controller.getXADD().getTermNode(new VarExpr("pb"));
        int pbFID = Controller.getXADD().scalarOp(Controller.getXADD().scalarOp(peTID, -1, XADD.PROD),
                                                                    1, XADD.SUM);
        
        /*
         * 1. 
         */
        
        while (currIterationNum < horizon) {
            currIterationNum++;

            System.out.println("Iteration number: " + currIterationNum);

            // Prime the vFuncID
            prevID = vFuncID;

            // Iterate over each action
            maxID = null;

            // Prime the value function
            qFuncID = Controller.getXADD().substitute(vFuncID, hmPrimeSubs);

            // Discount
            qFuncID = Controller.getXADD().scalarOp(qFuncID, discountValue, XADD.PROD);
            
            // Regression step
            
            qFuncID = Controller.regress(qFuncID, executeTransitionID, hsBooleanVariables, hsContinuousVariables);
//            Controller.PlotXADD(qFuncID, "Q - after regression (e)");
            qFuncID = Controller.regress(qFuncID, buyTransitionID, hsBooleanVariables, hsContinuousVariables);
//            Controller.PlotXADD(qFuncID, "Q - after regression (b)");
            qFuncID = Controller.regress(qFuncID, sellTransitionID, hsBooleanVariables, hsContinuousVariables);
//            Controller.PlotXADD(qFuncID, "Q - after regression (s)");
            qFuncID = Controller.regress(qFuncID, buyInvTransitionID, hsBooleanVariables, hsContinuousVariables);
//            Controller.PlotXADD(qFuncID, "Q - after regression (bInv)");
            
            
/*            HashSet<String> varsToRegress = Controller.getXADD().collectVars(qFuncID);
            Graph g = Controller.buildDBNDependencyDAG(valueTransitionID, varsToRegress);
            List variableEliminationOrder = g.topologicalSort(true);
            
            // Regress each variable in the topological order
            for (Object obj : variableEliminationOrder) {
                
                String varToEliminate = (String) obj;

                if (hsBooleanVariables.contains(varToEliminate)) {
                    qFuncID = regressBooleanVariable(qFuncID, valueTransitionID, varToEliminate);
                }
                else if (hsContinuousVariables.contains(varToEliminate)) {
                    qFuncID = regressContinuousVariable(qFuncID, valueTransitionID, varToEliminate);
                }
                
                Controller.PlotXADD(qFuncID, "Q - after regression (" + varToEliminate + ")");
            }*/
            
            //Controller.PlotXADD(qFuncID, "Q - after regression");
            
            // Add reward
            qFuncID = Controller.getXADD().apply(rewardID, qFuncID, XADD.SUM);
//            Controller.PlotXADD(qFuncID, "Q - after reward");
            
            /*
             * 1. Sum over (marginalise) the actions for Player A ('a')
             */
            
            int ehighID = Controller.getXADD().opOut(qFuncID, eVarID, XADD.RESTRICT_HIGH);
            int elowID = Controller.getXADD().opOut(qFuncID, eVarID, XADD.RESTRICT_LOW);

            int bhighID = Controller.getXADD().opOut(ehighID, bVarID, XADD.RESTRICT_HIGH);
            int blowID = Controller.getXADD().opOut(elowID, bVarID, XADD.RESTRICT_LOW);
            
/*            Controller.PlotXADD(ehighID, "E");
            Controller.PlotXADD(elowID, "~E");

            Controller.PlotXADD(bhighID, "B");
            Controller.PlotXADD(blowID, "~B");*/
            
            // Execute action
            int eXADDId = Controller.getXADD().apply(
                    Controller.getXADD().apply(ehighID, peTID, XADD.PROD),
                    Controller.getXADD().apply(elowID, peFID, XADD.PROD), 
                                                            XADD.SUM);
                    
            // Buy action        
            int bXADDId = Controller.getXADD().apply(
                               Controller.getXADD().apply(bhighID, pbTID, XADD.PROD),
                               Controller.getXADD().apply(blowID, pbFID, XADD.PROD), 
                                                                       XADD.SUM);
            
            qFuncID = Controller.getXADD().apply(eXADDId, bXADDId, XADD.SUM);
            
//            Controller.PlotXADD(qFuncID, "After 1 " + + currIterationNum);             
            
            /*
             * 2. Minimise over the actions for Player B ('b')
             */
            
            int shighID = Controller.getXADD().opOut(qFuncID, sVarID, XADD.RESTRICT_HIGH);
            int slowID = Controller.getXADD().opOut(qFuncID, sVarID, XADD.RESTRICT_LOW);

            qFuncID = Controller.getXADD().apply(shighID, slowID, XADD.MIN);

//            Controller.PlotXADD(qFuncID, "After 2 " + currIterationNum);       
            
            /*
             * 3. Maximise over the 
             */

            vFuncID = Controller.maxOutVar(qFuncID, "pe", 0, 1, logStream);
            vFuncID = Controller.maxOutVar(vFuncID, "pb", 0, 1, logStream);
            
            Controller.PlotXADD(vFuncID, "After 3 " + currIterationNum);
            
            /*
             * Plot the value function
             */
            String plotTitle = "Value Function - " + domainName + " " + currIterationNum;
            Controller.Display3D(vFuncID, plotTitle, plot3DFile, domainDir);            
            
        }
    }
    
    /**
     * Finite Horizon Stochastic Game
     */
    private static void SequentialMatchingPennies(Integer numIterations) {

        String domainDir = Controller
                .DomainDirectory("sequential_matching_pennies");
        String rewardFile = domainDir + File.separator + "reward.xadd";
        String constraintsFile = domainDir + File.separator
                + "constraints.xadd";
        String transitionsFile = domainDir + File.separator
                + "transitions.xadd";
        String logFile = domainDir + File.separator
                + "sequential_matching_pennies" + ".log";
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

        int aVarID = Controller.getXADD().getVarIndex(
                Controller.getXADD().new BoolDec("a"), false);
        int bVarID = Controller.getXADD().getVarIndex(
                Controller.getXADD().new BoolDec("b"), false);

        int pTID = Controller.getXADD().getTermNode(new VarExpr("p"));
        int pFID = Controller.getXADD()
                .scalarOp(Controller.getXADD().scalarOp(pTID, -1, XADD.PROD),
                        1, XADD.SUM);

        hmPrimeSubs.put("x", new VarExpr("x'"));
        hmPrimeSubs.put("a", new VarExpr("a'"));
        hmPrimeSubs.put("b", new VarExpr("b'"));

        hsBooleanVariables.add("a");
        hsBooleanVariables.add("b");
        hsContinuousVariables.add("x");

        hsVariables.addAll(hsBooleanVariables);
        hsVariables.addAll(hsContinuousVariables);

        /*
         * 1.
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
            qFuncID = Controller.getXADD().scalarOp(qFuncID, discountValue,
                    XADD.PROD);

            HashSet<String> varsToRegress = Controller.getXADD().collectVars(
                    qFuncID);
            Graph g = Controller.buildDBNDependencyDAG(transitionsID,
                    varsToRegress);
            List variableEliminationOrder = g.topologicalSort(true);

            // Regress each variable in the topological order
            for (Object obj : variableEliminationOrder) {
                String varToEliminate = (String) obj;

                if (hsBooleanVariables.contains(varToEliminate)) {
                    qFuncID = regressBooleanVariable(qFuncID, transitionsID,
                            varToEliminate);
                }
                else if (hsContinuousVariables.contains(varToEliminate)) {
                    qFuncID = regressContinuousVariable(qFuncID, transitionsID,
                            varToEliminate);
                }
            }

            // Add reward
            qFuncID = Controller.getXADD().apply(rewardID, qFuncID, XADD.SUM);
            Controller.PlotXADD(qFuncID, "Value after reward");

            vFuncID = qFuncID;

            /*
             * 1. Sum over (marginalise) the actions for Player A ('a')
             */

            int highID = Controller.getXADD().opOut(rewardID, aVarID,
                    XADD.RESTRICT_HIGH);
            int lowID = Controller.getXADD().opOut(rewardID, aVarID,
                    XADD.RESTRICT_LOW);

            vFuncID = Controller.getXADD().apply(
                    Controller.getXADD().apply(highID, pTID, XADD.PROD),
                    Controller.getXADD().apply(lowID, pFID, XADD.PROD),
                    XADD.SUM);

            Controller.PlotXADD(vFuncID, "After 1");

            /*
             * 2. Minimise over the actions for Player B ('b')
             */

            highID = Controller.getXADD().opOut(vFuncID, bVarID,
                    XADD.RESTRICT_HIGH);
            lowID = Controller.getXADD().opOut(vFuncID, bVarID,
                    XADD.RESTRICT_LOW);

            vFuncID = Controller.getXADD().apply(highID, lowID, XADD.MIN);

            Controller.PlotXADD(vFuncID, "After 2");

            /*
             * 3. Max out the ...
             */

            vFuncID = Controller.maxOutVar(vFuncID, "p", 0, 1, logStream);
            Controller.PlotXADD(vFuncID, "After 3");

            /*
             * Plot the value function
             */
            Controller.Display2D(vFuncID, "Value Function", plot2DFile,
                    domainDir);

            // Check for convergence
//            if (prevID.equals(vFuncID)) {
//                System.out.println("Converged at iteration " + currIterationNum);
//                break;
//            }
        }
    }
}
