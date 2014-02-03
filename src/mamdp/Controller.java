/**
 * 
 */

package mamdp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;

import mamdp.utils.*;
import xadd.ExprLib.ArithExpr;
import xadd.ExprLib.VarExpr;
import xadd.XADD;

/**
 * File: Controller
 * 
 * @author Shamin Kinathil 
 * Date: 20140108 
 * NOTES: Coordinates all experiments for this project
 */
public class Controller {

    private static final String PACKAGE_DIR = "mamdp";
    private static final String SOURCE_DIR = "src";
    private static final String DOMAIN_DIR = "domains";
    
    private static String RootDirectoryPath = "";
    private static String DomainDirectoryPath = "";

    private static XADD context = null;

    private static String ProjectRootDirectory() {

        if (Controller.RootDirectoryPath.isEmpty()) {
            Controller.RootDirectoryPath = System.getProperty("user.dir")
                                            + File.separator + SOURCE_DIR 
                                            + File.separator + PACKAGE_DIR;
        }

        return Controller.RootDirectoryPath;
    }

    private static String DomainDirectory(String domainName) {

        if (Controller.DomainDirectoryPath.isEmpty()) {
            Controller.DomainDirectoryPath = Controller.ProjectRootDirectory()
                                    + File.separator + Controller.DOMAIN_DIR;
        }

        return Controller.DomainDirectoryPath + File.separator + domainName;
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

    /**
     * @param args
     */
    public static void main(String[] args) {

        /*----------------------------------------------------------------------
         * Instantiate an XADD
         ---------------------------------------------------------------------*/
        
        XADD xadd = Controller.getXADD();
        XADDHelper.setXADDInstance(xadd);
        VIHelper.setXADDInstance(xadd);

        /*----------------------------------------------------------------------
         * Run a Stochastic Game
         ---------------------------------------------------------------------*/
                
        /*
         * One Shot Games
         */        
        //Controller.OneShotGame("chicken");
        //Controller.OneShotGame("matching_pennies");
        //Controller.OneShotBinaryOptionGame("binary_option", 1);
        
        /*
         * Sequential Games
         */
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
        int constraintsID = XADDHelper.BuildXADD(constraintsFile);
        int rewardID = XADDHelper.BuildXADD(rewardFile);

        XADDHelper.PlotXADD(constraintsID, "Constraints");
        XADDHelper.PlotXADD(rewardID, "Reward");

        int aVarID = Controller.getXADD().getVarIndex(Controller.getXADD().new BoolDec("a"), false);
        int bVarID = Controller.getXADD().getVarIndex(Controller.getXADD().new BoolDec("b"), false);

        int pTID = Controller.getXADD().getTermNode(new VarExpr("p"));
        int pFID = Controller.getXADD().scalarOp(Controller.getXADD().scalarOp(pTID, -1, XADD.PROD), 1, XADD.SUM);

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

        XADDHelper.PlotXADD(qFuncID, "After 1");

        /*
         * 2. Minimise over the actions for Player B ('b')
         */

        highID = Controller.getXADD().opOut(qFuncID, bVarID, XADD.RESTRICT_HIGH);
        lowID = Controller.getXADD().opOut(qFuncID, bVarID, XADD.RESTRICT_LOW);

        qFuncID = Controller.getXADD().apply(highID, lowID, XADD.MIN);

        XADDHelper.PlotXADD(qFuncID, "After 2");

        /*
         * 3. Maximise over the 
         */

        vFuncID = VIHelper.maxOutVar(qFuncID, "p", 0, 1, logStream);
        XADDHelper.PlotXADD(vFuncID, "After 3");

        /*
         * Plot the value function
         */
        String plotTitle = "Value Function - " + domainName;
        XADDHelper.Display2D(vFuncID, plotTitle, plot2DFile, domainDir);        
        
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
        int rewardID = XADDHelper.BuildXADD(rewardFile);
        int valueTransitionID = XADDHelper.BuildXADD(transitionsFile);
        int inventoryTransitionID = XADDHelper.BuildXADD(inventoryFile);

        int executeTransitionID = XADDHelper.BuildXADD(domainDir + File.separator + "execute_value_transition.xadd");
        int buyTransitionID = XADDHelper.BuildXADD(domainDir + File.separator + "buy_transition.xadd");
        int sellTransitionID = XADDHelper.BuildXADD(domainDir + File.separator + "sell_transition.xadd");
                
        XADDHelper.PlotXADD(rewardID, "Reward - " + domainName);
        XADDHelper.PlotXADD(valueTransitionID, "Value Transition - " + domainName);
        XADDHelper.PlotXADD(inventoryTransitionID, "Inventory Transition - " + domainName);
        XADDHelper.PlotXADD(executeTransitionID, "Execute - " + domainName);
        XADDHelper.PlotXADD(buyTransitionID, "Buy - " + domainName);
        XADDHelper.PlotXADD(sellTransitionID, "Sell - " + domainName);
        
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
        
        XADDHelper.PlotXADD(ehighID, "E");
        XADDHelper.PlotXADD(elowID, "~E");

        XADDHelper.PlotXADD(bhighID, "B");
        XADDHelper.PlotXADD(blowID, "~B");
        
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
        
        XADDHelper.PlotXADD(qFuncID, "After 1");        
        
        /*
         * 2. Minimise over the actions for Player B ('b')
         */

        int sVarID = Controller.getXADD().getVarIndex(Controller.getXADD().new BoolDec("s"), false);
        
        int shighID = Controller.getXADD().opOut(qFuncID, sVarID, XADD.RESTRICT_HIGH);
        int slowID = Controller.getXADD().opOut(qFuncID, sVarID, XADD.RESTRICT_LOW);

        qFuncID = Controller.getXADD().apply(shighID, slowID, XADD.MIN);

        XADDHelper.PlotXADD(qFuncID, "After 2");        

        /*
         * 3. Maximise over the 
         */

        vFuncID = VIHelper.maxOutVar(qFuncID, "pe", 0, 1, logStream);
        vFuncID = VIHelper.maxOutVar(vFuncID, "pb", 0, 1, logStream);
        
        XADDHelper.PlotXADD(vFuncID, "After 3");        
        
        /*
         * Plot the value function
         */
        String plotTitle = "Value Function - " + domainName;
        //Controller.Display2D(vFuncID, plotTitle, plot2DFile, domainDir);
        XADDHelper.Display3D(vFuncID, plotTitle, plot2DFile, domainDir);
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

        double discountValue = 0.8;

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
        int rewardID = XADDHelper.BuildXADD(rewardFile);
        int valueTransitionID = XADDHelper.BuildXADD(transitionsFile);
        int inventoryTransitionID = XADDHelper.BuildXADD(inventoryFile);

        int executeTransitionID = XADDHelper.BuildXADD(domainDir + File.separator + "execute_value_transition.xadd");
        int buyTransitionID = XADDHelper.BuildXADD(domainDir + File.separator + "buy_value_transition.xadd");
        int buyInvTransitionID = XADDHelper.BuildXADD(domainDir + File.separator + "buy_inventory_transition.xadd");
        int sellTransitionID = XADDHelper.BuildXADD(domainDir + File.separator + "sell_value_transition.xadd");
                
        XADDHelper.PlotXADD(rewardID, "Reward - " + domainName);
        XADDHelper.PlotXADD(valueTransitionID, "Value Transition - " + domainName);
        XADDHelper.PlotXADD(inventoryTransitionID, "Inventory Transition - " + domainName);
                
        XADDHelper.PlotXADD(executeTransitionID, "Execute - " + domainName);
        XADDHelper.PlotXADD(buyTransitionID, "Buy - " + domainName);
        XADDHelper.PlotXADD(buyInvTransitionID, "Buy Inventory - " + domainName);
        XADDHelper.PlotXADD(sellTransitionID, "Sell - " + domainName);           
        
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
        int pbFID = Controller.getXADD().scalarOp(Controller.getXADD().scalarOp(peTID, -1, XADD.PROD), 1, XADD.SUM);
        
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
            
            qFuncID = VIHelper.regress(qFuncID, executeTransitionID, hsBooleanVariables, hsContinuousVariables);
//            XADDHelper.PlotXADD(qFuncID, "Q - after regression (e)");
            qFuncID = VIHelper.regress(qFuncID, buyTransitionID, hsBooleanVariables, hsContinuousVariables);
//            XADDHelper.PlotXADD(qFuncID, "Q - after regression (b)");
            qFuncID = VIHelper.regress(qFuncID, sellTransitionID, hsBooleanVariables, hsContinuousVariables);
//            XADDHelper.PlotXADD(qFuncID, "Q - after regression (s)");
            qFuncID = VIHelper.regress(qFuncID, buyInvTransitionID, hsBooleanVariables, hsContinuousVariables);
//            XADDHelper.PlotXADD(qFuncID, "Q - after regression (bInv)");
            
            // Add reward
            qFuncID = Controller.getXADD().apply(rewardID, qFuncID, XADD.SUM);
//            XADDHelper.PlotXADD(qFuncID, "Q - after reward");
            
            /*
             * 1. Sum over (marginalise) the actions for Player A ('a')
             */
            
            int ehighID = Controller.getXADD().opOut(qFuncID, eVarID, XADD.RESTRICT_HIGH);
            int elowID = Controller.getXADD().opOut(qFuncID, eVarID, XADD.RESTRICT_LOW);

            int bhighID = Controller.getXADD().opOut(ehighID, bVarID, XADD.RESTRICT_HIGH);
            int blowID = Controller.getXADD().opOut(elowID, bVarID, XADD.RESTRICT_LOW);
            
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
            
//            XADDHelper.PlotXADD(qFuncID, "After 1 " + + currIterationNum);             
            
            /*
             * 2. Minimise over the actions for Player B ('b')
             */
            
            int shighID = Controller.getXADD().opOut(qFuncID, sVarID, XADD.RESTRICT_HIGH);
            int slowID = Controller.getXADD().opOut(qFuncID, sVarID, XADD.RESTRICT_LOW);

            qFuncID = Controller.getXADD().apply(shighID, slowID, XADD.MIN);

//            XADDHelper.PlotXADD(qFuncID, "After 2 " + currIterationNum);       
            
            /*
             * 3. Maximise over the 
             */

            vFuncID = VIHelper.maxOutVar(qFuncID, "pe", 0, 1, logStream);
            vFuncID = VIHelper.maxOutVar(vFuncID, "pb", 0, 1, logStream);
            
            XADDHelper.PlotXADD(vFuncID, "After 3 " + currIterationNum);
            
            /*
             * Plot the value function
             */
            String plotTitle = "Value Function - " + domainName + " " + currIterationNum;
            XADDHelper.Display3D(vFuncID, plotTitle, plot3DFile, domainDir);
        }
    }
}
