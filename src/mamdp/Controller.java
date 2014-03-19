package mamdp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
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
            
            // NOTES: This is a HACK. It is used with the "simple_binary_option"
            //          domain.
            //context.getVarIndex(context.new BoolDec("b"), true);
            //context.getVarIndex(context.new BoolDec("m"), true);
            //context.getVarIndex(context.new BoolDec("s"), true);
        }

        return context;
    }
    
    private static void clearXADD() {

        if (context != null) {
            System.out.println("Clearing XADD");
            context = null;
        }
    }
    

    /**
     * @param args
     */
    public static void main(String[] args) {

        /*----------------------------------------------------------------------
         * Instantiate an XADD
         ---------------------------------------------------------------------*/

        Controller.clearXADD();
        
        XADD xadd = Controller.getXADD();        
        XADDHelper.setXADDInstance(xadd);
        VIHelper.setXADDInstance(xadd);

        /*----------------------------------------------------------------------
         * Run a Stochastic Game
         ---------------------------------------------------------------------*/

        /*
         * Sequential Games
         */

//        Controller.MatchingPennies("sequential_matching_pennies", 5);        
//        Controller.BinaryOptionDomain("simple_binary_option", 20);
//        Controller.EnergyDomain("energy_production", 5);
        Controller.SimpleEnergyDomain("simple_energy_production", 10);
        
        xadd.flushCaches();
    }
    
    private static void SimpleEnergyDomain(String domainName, Integer horizon) {
        String domainDir = Controller.DomainDirectory(domainName);
        String rewardFile = domainDir + File.separator + "reward.xadd";
        
        String productionTransitionsFile = domainDir + File.separator + "production_transition.xadd";
        String demandTransitionFile = domainDir + File.separator + "demand_transition.xadd";
        String profitTransitionFile = domainDir + File.separator + "profit_transition.xadd";
        
        String logFile = domainDir + File.separator + domainName + ".log";
        String plot3DFile = domainDir + File.separator + "plot";
        
        Integer prevID = null;
        Integer vFuncID = Controller.getXADD().ZERO;
        Integer qFuncID = null;

        HashMap<String, ArithExpr> hmPrimeSubs = new HashMap<String, ArithExpr>();
        
        double discountValue = 0.9;

        PrintStream logStream = null;

        int currIterationNum = 0;

        /*
         * Set up
         */

        XADDHelper.FlushCaches(new ArrayList<Integer>(), false);
        
        try {
            logStream = new PrintStream(new FileOutputStream(logFile));
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }          
        
        // Build the XADDs
        int rewardID = XADDHelper.BuildXADD(rewardFile);
        Integer prodTransitionID = XADDHelper.BuildXADD(productionTransitionsFile);
        Integer demTransitionID = XADDHelper.BuildXADD(demandTransitionFile);
//        Integer profTransitionID = XADDHelper.BuildXADD(profitTransitionFile);

        //int epsID = XADDHelper.BuildXADD(domainDir + File.separator + "epsilon.xadd");

        XADDHelper.PlotXADD(rewardID, "Reward - " + domainName);
        XADDHelper.PlotXADD(prodTransitionID, "Production Transition - " + domainName);
        XADDHelper.PlotXADD(demTransitionID, "Demand Transition - " + domainName);
//        XADDHelper.PlotXADD(profTransitionID, "Profit Transition - " + domainName);
                
        // Nature's action variables (increase)
        Integer xVarID = Controller.getXADD().getVarIndex(Controller.getXADD().new BoolDec("x"), false);
        
        // Producer's action variables (increase)
        Integer uVarID = Controller.getXADD().getVarIndex(Controller.getXADD().new BoolDec("u"), false);
        
        // The probabilities over each of the Producer's pure strategies
        Integer puTID = Controller.getXADD().getTermNode(new VarExpr("pu"));
        Integer puFID = Controller.getXADD().scalarOp(Controller.getXADD().scalarOp(puTID, -1, XADD.PROD),1, XADD.SUM);
        
        hmPrimeSubs.put("p", new VarExpr("p'"));
        hmPrimeSubs.put("d", new VarExpr("d'"));        
//        hmPrimeSubs.put("w", new VarExpr("w'"));
        
        /*
         * Solve the MAMDP
         */
        // Prime the reward function        
        Integer primedRewardID = Controller.getXADD().substitute(rewardID, hmPrimeSubs);
        
        ArrayList<Integer> specialNodes = new ArrayList<Integer>();
        specialNodes.add(primedRewardID);
        specialNodes.add(prodTransitionID);
        specialNodes.add(demTransitionID);
        specialNodes.add(xVarID);
        specialNodes.add(uVarID);
        specialNodes.add(puTID);
        specialNodes.add(puFID);
        
        while (currIterationNum < horizon) {
            currIterationNum++;
            
            System.out.println("Iteration number: " + currIterationNum);

            // Prime the vFuncID
            prevID = vFuncID;

            // Prime the value function
//            XADDHelper.PlotXADD(prevID, "Q - unprimed");
            qFuncID = Controller.getXADD().substitute(prevID, hmPrimeSubs);
//            XADDHelper.PlotXADD(qFuncID, "Q - primed");

            // Discount
            qFuncID = Controller.getXADD().scalarOp(qFuncID, discountValue, XADD.PROD);            
            
            // Add reward
            qFuncID = Controller.getXADD().apply(primedRewardID, qFuncID, XADD.SUM);
//            XADDHelper.PlotXADD(qFuncID, "Q - After reward - " + currIterationNum);
            
            // Regression step
            System.out.println(currIterationNum + ": ----Regression Step");
            
//            qFuncID = VIHelper.regressContinuousVariable(qFuncID, profTransitionID, "w'");
//            XADDHelper.PlotXADD(qFuncID, "Q - after regression (w')");
            
            qFuncID = VIHelper.regressContinuousVariable(qFuncID, prodTransitionID, "p'");
//            XADDHelper.PlotXADD(qFuncID, "Q - after regression (p')");
            
            qFuncID = VIHelper.regressContinuousVariable(qFuncID, demTransitionID, "d'");
//            XADDHelper.PlotXADD(qFuncID, "Q - after regression (d')");
            
            /*
             * 1. Sum over (marginalise) the actions for Producer
             */
            
            System.out.println(currIterationNum + ": ----Sum over (marginalise) the actions for Producer");

            // Increase
            int uhighID = Controller.getXADD().opOut(qFuncID, uVarID, XADD.RESTRICT_HIGH);
//            XADDHelper.PlotXADD(uhighID, "uhighID");
            
            int ulowID = Controller.getXADD().opOut(qFuncID, uVarID, XADD.RESTRICT_LOW);
//            XADDHelper.PlotXADD(ulowID, "ulowID");
            
            qFuncID = Controller.getXADD().apply(
                        Controller.getXADD().apply(uhighID, puTID, XADD.PROD),
                        Controller.getXADD().apply(ulowID, puFID, XADD.PROD), 
                                                                    XADD.SUM);            
//            XADDHelper.PlotXADD(qFuncID, "After 1 - " + currIterationNum);
            
            /*
             * 2. Minimise over the actions for Nature
             */
            System.out.println(currIterationNum + ": ----Minimise over the actions for Nature");
            
            // Increase
            int xhighID = Controller.getXADD().opOut(qFuncID, xVarID, XADD.RESTRICT_HIGH);
//            XADDHelper.PlotXADD(xhighID, "xhighID");
            
            int xlowID = Controller.getXADD().opOut(qFuncID, xVarID, XADD.RESTRICT_LOW);
//            XADDHelper.PlotXADD(xlowID, "xlowID");
           
            qFuncID = Controller.getXADD().apply(xhighID, xlowID, XADD.MIN);
//            XADDHelper.PlotXADD(qFuncID, "After 2 - " + currIterationNum);
            
            specialNodes.add(qFuncID);
            XADDHelper.FlushCaches(specialNodes, true);
            specialNodes.remove(specialNodes.size() - 1);
            /*
             * 3. Maximise over the mixed strategy probabilities
             */
            
            System.out.println(currIterationNum + ": ----Maxout Producer's pure strategy probabilities------");
            vFuncID = VIHelper.maxOutVar(qFuncID, "pu", 0, 1, logStream);
            
//            XADDHelper.PlotXADD(vFuncID, "After 3 - " + currIterationNum);
            
            /*
             * Plot the value function
             */
            System.out.println(currIterationNum + ": ----Plot Producer's value function------");
            String plotTitle = "Value Function - " + domainName + " " + currIterationNum;
            XADDHelper.Display3D(vFuncID, plotTitle, plot3DFile, domainDir);      
            
            // Flush the cache
            
            specialNodes.add(vFuncID);
//            specialNodes.add(profTransitionID);

            XADDHelper.FlushCaches(specialNodes, true);
        }
        System.out.println("----COMPLETE------");        
    }
    
    private static void EnergyDomain(String domainName, Integer horizon) {
        String domainDir = Controller.DomainDirectory(domainName);
        String rewardFile = domainDir + File.separator + "reward.xadd";
        
        String productionTransitionsFile = domainDir + File.separator + "production_transition.xadd";
        String demandTransitionFile = domainDir + File.separator + "demand_transition.xadd";
        String profitTransitionFile = domainDir + File.separator + "profit_transition.xadd";
        
        String logFile = domainDir + File.separator + domainName + ".log";
        
        Integer prevID = null;
        Integer vFuncID = Controller.getXADD().ZERO;
        Integer qFuncID = null;

        HashMap<String, ArithExpr> hmPrimeSubs = new HashMap<String, ArithExpr>();
        
        double discountValue = 0.9;

        PrintStream logStream = null;

        int currIterationNum = 0;

        /*
         * Set up
         */

        try {
            logStream = new PrintStream(new FileOutputStream(logFile));
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }          
        
        // Build the XADDs
        int rewardID = XADDHelper.BuildXADD(rewardFile);
        int prodTransitionID = XADDHelper.BuildXADD(productionTransitionsFile);
        int demTransitionID = XADDHelper.BuildXADD(demandTransitionFile);
        int profTransitionID = XADDHelper.BuildXADD(profitTransitionFile);

        //int epsID = XADDHelper.BuildXADD(domainDir + File.separator + "epsilon.xadd");

        XADDHelper.PlotXADD(rewardID, "Reward - " + domainName);
        XADDHelper.PlotXADD(prodTransitionID, "Production Transition - " + domainName);
        XADDHelper.PlotXADD(demTransitionID, "Demand Transition - " + domainName);
        XADDHelper.PlotXADD(profTransitionID, "Profit Transition - " + domainName);
                
        // Nature's action variables (increase, hold, decrease)
        int xVarID = Controller.getXADD().getVarIndex(Controller.getXADD().new BoolDec("x"), false);
        int yVarID = Controller.getXADD().getVarIndex(Controller.getXADD().new BoolDec("y"), false);
        int zVarID = Controller.getXADD().getVarIndex(Controller.getXADD().new BoolDec("z"), false);
        
        // Producer's action variables (increase, hold, decrease)
        int uVarID = Controller.getXADD().getVarIndex(Controller.getXADD().new BoolDec("u"), false);
        int hVarID = Controller.getXADD().getVarIndex(Controller.getXADD().new BoolDec("h"), false);
        int rVarID = Controller.getXADD().getVarIndex(Controller.getXADD().new BoolDec("r"), false);
        
        // The probabilities over each of the Producer's pure strategies
        int puTID = Controller.getXADD().getTermNode(new VarExpr("pu"));
        int phTID = Controller.getXADD().getTermNode(new VarExpr("ph"));        
        int prTID = Controller.getXADD().getTermNode(new VarExpr("pr"));
        
        hmPrimeSubs.put("p", new VarExpr("p'"));
        hmPrimeSubs.put("d", new VarExpr("d'"));        
        hmPrimeSubs.put("w", new VarExpr("w'"));
        
        /*
         * Solve the MAMDP
         */

        // Prime the reward function        
        int primedRewardID = Controller.getXADD().substitute(rewardID, hmPrimeSubs);
        
        while (currIterationNum < horizon) {
            currIterationNum++;

            System.out.println("Iteration number: " + currIterationNum);

            // Prime the vFuncID
            prevID = vFuncID;

            // Prime the value function
//            XADDHelper.PlotXADD(prevID, "Q - unprimed");
            qFuncID = Controller.getXADD().substitute(prevID, hmPrimeSubs);
//            XADDHelper.PlotXADD(qFuncID, "Q - primed");

            // Prime the reward function
//            XADDHelper.PlotXADD(rewardID, "R - unprimed");            
//            rewardID = Controller.getXADD().substitute(rewardID, hmPrimeSubs);
//            XADDHelper.PlotXADD(rewardID, "R - primed");            

            // Discount
            qFuncID = Controller.getXADD().scalarOp(qFuncID, discountValue, XADD.PROD);            
            
            // Add reward
            qFuncID = Controller.getXADD().apply(primedRewardID, qFuncID, XADD.SUM);
            XADDHelper.PlotXADD(qFuncID, "Q - After reward - " + currIterationNum);
            
            // Regression step
            System.out.println(currIterationNum + ": ----Regression Step");
            
            qFuncID = VIHelper.regressContinuousVariable(qFuncID, profTransitionID, "w'");
            XADDHelper.PlotXADD(qFuncID, "Q - after regression (w')");
            
            qFuncID = VIHelper.regressContinuousVariable(qFuncID, prodTransitionID, "p'");
            XADDHelper.PlotXADD(qFuncID, "Q - after regression (p')");
            
            qFuncID = VIHelper.regressContinuousVariable(qFuncID, demTransitionID, "d'");
            XADDHelper.PlotXADD(qFuncID, "Q - after regression (d')");
            
            // Add reward
//            qFuncID = Controller.getXADD().apply(rewardID, qFuncID, XADD.SUM);
//            XADDHelper.PlotXADD(qFuncID, "Q - After reward - " + currIterationNum);
            
            /*
             * 1. Sum over (marginalise) the actions for Producer
             */
            
            int uXADDId = Controller.getXADD().ZERO;
            int rXADDId = Controller.getXADD().ZERO;
            int hXADDId = Controller.getXADD().ZERO;
            
            System.out.println(currIterationNum + ": ----Sum over (marginalise) the actions for Producer");

            // Increase
            int uhighID = Controller.getXADD().opOut(qFuncID, uVarID, XADD.RESTRICT_HIGH);
            uhighID = Controller.getXADD().opOut(uhighID, rVarID, XADD.RESTRICT_LOW);
            XADDHelper.PlotXADD(uhighID, "uhighID");
            
            int ulowID = Controller.getXADD().opOut(qFuncID, uVarID, XADD.RESTRICT_LOW);
            XADDHelper.PlotXADD(ulowID, "ulowID");
            
            uXADDId = Controller.getXADD().apply(uhighID, puTID, XADD.PROD); 
            XADDHelper.PlotXADD(uXADDId, "uXADDId");
            
            // Decrease
            int rhighID = Controller.getXADD().opOut(ulowID, rVarID, XADD.RESTRICT_HIGH);
            XADDHelper.PlotXADD(rhighID, "rhighID");
            
            rXADDId = Controller.getXADD().apply(rhighID, prTID, XADD.PROD);
            XADDHelper.PlotXADD(rXADDId, "rXADDId");
            
            int rlowID = Controller.getXADD().opOut(ulowID, rVarID, XADD.RESTRICT_LOW);
            XADDHelper.PlotXADD(rlowID, "rlowID");
            
            // Hold
            int hhighID = Controller.getXADD().opOut(rlowID, hVarID, XADD.RESTRICT_HIGH);
            XADDHelper.PlotXADD(hhighID, "hhighID");
            
            hXADDId = Controller.getXADD().apply(hhighID, phTID, XADD.PROD);
            XADDHelper.PlotXADD(hXADDId, "hXADDId");
            
            qFuncID = Controller.getXADD().apply(Controller.getXADD().apply(rXADDId, uXADDId, XADD.SUM), hXADDId, XADD.SUM);
            XADDHelper.PlotXADD(qFuncID, "After 1 - " + currIterationNum);
            
            /*
             * 2. Minimise over the actions for Nature
             */
            System.out.println(currIterationNum + ": ----Minimise over the actions for Nature");
            
            // Increase
            int xhighID = Controller.getXADD().opOut(qFuncID, xVarID, XADD.RESTRICT_HIGH);
            XADDHelper.PlotXADD(xhighID, "xhighID");
            
            int xlowID = Controller.getXADD().opOut(qFuncID, xVarID, XADD.RESTRICT_LOW);
            XADDHelper.PlotXADD(xlowID, "xlowID");
            
            // Decrease
            int yhighID = Controller.getXADD().opOut(xlowID, yVarID, XADD.RESTRICT_HIGH);
            XADDHelper.PlotXADD(yhighID, "yhighID");
            
            int ylowID = Controller.getXADD().opOut(yhighID, yVarID, XADD.RESTRICT_LOW);
            XADDHelper.PlotXADD(ylowID, "ylowID");
            
            // Hold
            int zhighID = Controller.getXADD().opOut(ylowID, zVarID, XADD.RESTRICT_HIGH);
            XADDHelper.PlotXADD(zhighID, "zhighID");
           
            qFuncID = Controller.getXADD().apply(Controller.getXADD().apply(xhighID, yhighID, XADD.MIN), zhighID, XADD.MIN);
            XADDHelper.PlotXADD(qFuncID, "After 2 - " + currIterationNum);
            
            /*
             * 3. Maximise over the mixed strategy probabilities
             */
            
            System.out.println(currIterationNum + ": ----Maxout Producer's pure strategy probabilities------");
            vFuncID = VIHelper.maxOutVar(qFuncID, "pu", 0, 1, logStream);
            XADDHelper.PlotXADD(vFuncID, "After maxOutVar pu - " + currIterationNum);
            vFuncID = VIHelper.maxOutVar(vFuncID, "pr", 0, 1, logStream);
            XADDHelper.PlotXADD(vFuncID, "After maxOutVar pr - " + currIterationNum);
            vFuncID = VIHelper.maxOutVar(vFuncID, "ph", 0, 1, logStream);
            XADDHelper.PlotXADD(vFuncID, "After maxOutVar ph - " + currIterationNum);
            
            XADDHelper.PlotXADD(vFuncID, "After 3 - " + currIterationNum);
            
            /*
             * Plot the value function
             */
//            String plotTitle = "Value Function - " + domainName + " " + currIterationNum;
//            XADDHelper.Display3D(vFuncID, plotTitle, plot3DFile, domainDir);            
        }
        
    }
    
    private static void MatchingPennies(String domainName, Integer horizon) {
    
        String domainDir = Controller.DomainDirectory(domainName);
//        String rewardFile = domainDir + File.separator + "reward.xadd";
        String rewardFile = domainDir + File.separator + "reward_asymmetric.xadd";
        
        String transitionsFile = domainDir + File.separator + "transitions.xadd";
        
        String logFile = domainDir + File.separator + domainName + ".log";
        String plot2DFile = domainDir + File.separator + "plot";

        Integer vFuncID = Controller.getXADD().ZERO;
        Integer qFuncID = null;

        Integer prevID = null;
        
        double discountValue = 0.9;

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
        
        int transitionID = XADDHelper.BuildXADD(transitionsFile);
                
        XADDHelper.PlotXADD(rewardID, "Reward - " + domainName);
        XADDHelper.PlotXADD(transitionID, "transitionID - " + domainName);
        
        hsVariables.addAll(hsBooleanVariables);
        hsVariables.addAll(hsContinuousVariables);        

        int aVarID = Controller.getXADD().getVarIndex(Controller.getXADD().new BoolDec("a"), false);
        int bVarID = Controller.getXADD().getVarIndex(Controller.getXADD().new BoolDec("b"), false);
        
        int paTID = Controller.getXADD().getTermNode(new VarExpr("p"));
        int paFID = Controller.getXADD().scalarOp(Controller.getXADD().scalarOp(paTID, -1, XADD.PROD), 1, XADD.SUM);
        
        hmPrimeSubs.put("x", new VarExpr("x'"));
        
        /*
         * 1. 
         */
        
        while (currIterationNum < horizon) {
            currIterationNum++;

            System.out.println("Iteration number: " + currIterationNum);

            // Prime the vFuncID
            prevID = vFuncID;

            // Prime the value function
//            XADDHelper.PlotXADD(vFuncID, "Q - before prime - " + currIterationNum);
            qFuncID = Controller.getXADD().substitute(vFuncID, hmPrimeSubs);
//            XADDHelper.PlotXADD(qFuncID, "Q - after prime - " + currIterationNum);
            
            // Discount
            qFuncID = Controller.getXADD().scalarOp(qFuncID, discountValue, XADD.PROD);
            
            // Regression step
            System.out.println(currIterationNum + ": ----Perform regression------");
            
            // TODO:Testing this out
            qFuncID = VIHelper.regressContinuousVariable(qFuncID, transitionID, "x'");
//            XADDHelper.PlotXADD(qFuncID, "Q - after regression");
            
            // Add reward
            qFuncID = Controller.getXADD().apply(rewardID, qFuncID, XADD.SUM);
//            XADDHelper.PlotXADD(qFuncID, "Q - after reward");
            
            /*
             * 1. Sum over (marginalise) the actions for Player A ('a')
             */
            System.out.println(currIterationNum + ": ----Sum over (marginalise) the actions for Player A ('a')------");
            
            int ahighID = Controller.getXADD().opOut(qFuncID, aVarID, XADD.RESTRICT_HIGH);
            int alowID = Controller.getXADD().opOut(qFuncID, aVarID, XADD.RESTRICT_LOW);
            
//            XADDHelper.PlotXADD(ahighID, "ahighID - " + currIterationNum);
//            XADDHelper.PlotXADD(alowID, "alowID - " + currIterationNum);            
                        
            // Execute action
            qFuncID = Controller.getXADD().apply(
                    Controller.getXADD().apply(ahighID, paTID, XADD.PROD),
                    Controller.getXADD().apply(alowID, paFID, XADD.PROD), 
                                                                    XADD.SUM);
//            XADDHelper.PlotXADD(qFuncID, "After 1 - " + currIterationNum);             
            
            /*
             * 2. Minimise over the actions for Player B ('b')
             */
            System.out.println(currIterationNum + ": ----Minimise over the actions for Player B ('b')------");
            
            int bhighID = Controller.getXADD().opOut(qFuncID, bVarID, XADD.RESTRICT_HIGH);
//            XADDHelper.PlotXADD(bhighID, "bhighID - " + currIterationNum);

            int blowID = Controller.getXADD().opOut(qFuncID, bVarID, XADD.RESTRICT_LOW);
//            XADDHelper.PlotXADD(blowID, "blowID - " + currIterationNum);        

            qFuncID = Controller.getXADD().apply(bhighID, blowID, XADD.MIN);
//            XADDHelper.PlotXADD(qFuncID, "After 2 - " + currIterationNum);       
            
            /*
             * 3. Maximise over the 
             */
            System.out.println(currIterationNum + ": ----Maxout Player A's pure strategy probabilities------");
            vFuncID = VIHelper.maxOutVar(qFuncID, "p", 0, 1, logStream);
            
            XADDHelper.PlotXADD(vFuncID, "After 3 - " + currIterationNum);
            
            /*
             * Plot the value function
             */
            String plotTitle = "Value Function - " + domainName + " - " + currIterationNum;
            XADDHelper.Display2D(vFuncID, plotTitle, plot2DFile, domainDir);
        }
    }

    private static void BinaryOptionDomain(String domainName, Integer horizon) {
        
        String domainDir = Controller.DomainDirectory(domainName);
        String rewardFile = domainDir + File.separator + "reward.xadd";
        
        String valueTransitionsFile = domainDir + File.separator + "value_transition.xadd";
        String inventoryTransitionFile = domainDir + File.separator + "inventory_transition.xadd";
        
        String logFile = domainDir + File.separator + domainName + ".log";
        String plot3DFile = domainDir + File.separator + "plot";
        
        Integer prevID = null;
        Integer vFuncID = Controller.getXADD().ZERO;
        Integer qFuncID = null;

        HashMap<String, ArithExpr> hmPrimeSubs = new HashMap<String, ArithExpr>();
        
        double discountValue = 0.9;

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
        int valTransitionID = XADDHelper.BuildXADD(valueTransitionsFile);
        int invTransitionID = XADDHelper.BuildXADD(inventoryTransitionFile);

        int epsID = XADDHelper.BuildXADD(domainDir + File.separator + "epsilon.xadd");

//        XADDHelper.PlotXADD(rewardID, "Reward - " + domainName);
//        XADDHelper.PlotXADD(valTransitionID, "Value Transition - " + domainName);
//        XADDHelper.PlotXADD(invTransitionID, "Inventory Transition - " + domainName);
                
        // The Market's action variables (sell)
        int mVarID = Controller.getXADD().getVarIndex(Controller.getXADD().new BoolDec("m"), false);
        
        // The Trader's action variables (buy, sell and hold)
        int bVarID = Controller.getXADD().getVarIndex(Controller.getXADD().new BoolDec("b"), false);
        int sVarID = Controller.getXADD().getVarIndex(Controller.getXADD().new BoolDec("s"), false);
        int hVarID = Controller.getXADD().getVarIndex(Controller.getXADD().new BoolDec("h"), false);
        
        // The probabilities over each of the Trader's pure strategies
        int psTID = Controller.getXADD().getTermNode(new VarExpr("ps"));
        int psFID = Controller.getXADD().scalarOp(Controller.getXADD().scalarOp(psTID, -1, XADD.PROD),1, XADD.SUM);
        
        int pbTID = Controller.getXADD().getTermNode(new VarExpr("pb"));
        int pbFID = Controller.getXADD().scalarOp(Controller.getXADD().scalarOp(pbTID, -1, XADD.PROD), 1, XADD.SUM);
        
        int phTID = Controller.getXADD().getTermNode(new VarExpr("ph"));
        int phFID = Controller.getXADD().scalarOp(Controller.getXADD().scalarOp(phTID, -1, XADD.PROD), 1, XADD.SUM);
        
        hmPrimeSubs.put("v", new VarExpr("v'"));
        hmPrimeSubs.put("i", new VarExpr("i'"));
        /*
         * 1. 
         */
        
        while (currIterationNum < horizon) {
            currIterationNum++;

            System.out.println("Iteration number: " + currIterationNum);

            // Prime the vFuncID
            prevID = vFuncID;

            // Prime the value function
//            XADDHelper.PlotXADD(prevID, "Q - unprimed");
            qFuncID = Controller.getXADD().substitute(prevID, hmPrimeSubs);
//            XADDHelper.PlotXADD(qFuncID, "Q - primed");
            
            // Discount
            qFuncID = Controller.getXADD().scalarOp(qFuncID, discountValue, XADD.PROD);
            
            // Regression step
            System.out.println(currIterationNum + ": ----Regression Step");
            
            qFuncID = VIHelper.regressContinuousVariable(qFuncID, valTransitionID, "v'");
//            XADDHelper.PlotXADD(qFuncID, "Q - after regression (v')");
            
            qFuncID = VIHelper.regressContinuousVariable(qFuncID, invTransitionID, "i'");
//            XADDHelper.PlotXADD(qFuncID, "Q - after regression (i')");
            
//            qFuncID = VIHelper.regress(qFuncID, epsID, hsBooleanVariables, hsContinuousVariables);
            
            // Add reward
            qFuncID = Controller.getXADD().apply(rewardID, qFuncID, XADD.SUM);
//            XADDHelper.PlotXADD(qFuncID, "Q - after reward");
            
            /*
             * 1. Sum over (marginalise) the actions for Player A ('a')
             */
            
            int bXADDId = Controller.getXADD().ZERO;
            int sXADDId = Controller.getXADD().ZERO;
            int hXADDId = Controller.getXADD().ZERO;
            
            System.out.println(currIterationNum + ": ----Sum over (marginalise) the actions for Player A ('a')");

            // Sell action
            int shighID = Controller.getXADD().opOut(qFuncID, sVarID, XADD.RESTRICT_HIGH);
            shighID = Controller.getXADD().opOut(shighID, bVarID, XADD.RESTRICT_LOW);            
//            XADDHelper.PlotXADD(shighID, "shighID");
            
            int slowID = Controller.getXADD().opOut(qFuncID, sVarID, XADD.RESTRICT_LOW);
//            XADDHelper.PlotXADD(slowID, "slowID");
            
            sXADDId = Controller.getXADD().apply(shighID, psTID, XADD.PROD); 
//            XADDHelper.PlotXADD(sXADDId, "sXADDId");
//            
//            sXADDId = Controller.getXADD().apply(
//                    Controller.getXADD().apply(shighID, psTID, XADD.PROD),
//                    Controller.getXADD().apply(slowID, psFID, XADD.PROD), 
//                                                                    XADD.SUM);
//            XADDHelper.PlotXADD(sXADDId, "sXADDId");
            
            // Buy action
            int bhighID = Controller.getXADD().opOut(slowID, bVarID, XADD.RESTRICT_HIGH);
//            XADDHelper.PlotXADD(bhighID, "bhighID");
            
            bXADDId = Controller.getXADD().apply(bhighID, pbTID, XADD.PROD);
//            XADDHelper.PlotXADD(bXADDId, "bXADDId");
            
            int blowID = Controller.getXADD().opOut(slowID, bVarID, XADD.RESTRICT_LOW);
//            XADDHelper.PlotXADD(blowID, "blowID");            
//            
//            bXADDId = Controller.getXADD().apply(
//                           Controller.getXADD().apply(bhighID, pbTID, XADD.PROD),
//                           Controller.getXADD().apply(blowID, pbFID, XADD.PROD), 
//                                                                   XADD.SUM);
//            XADDHelper.PlotXADD(bXADDId, "bXADDId");
            
            // Hold action        
            int hhighID = Controller.getXADD().opOut(blowID, hVarID, XADD.RESTRICT_HIGH);
//            XADDHelper.PlotXADD(hhighID, "hhighID");
            
            hXADDId = Controller.getXADD().apply(hhighID, phTID, XADD.PROD);
//            XADDHelper.PlotXADD(hXADDId, "hXADDId");
            
//            int hlowID = Controller.getXADD().opOut(blowID, hVarID, XADD.RESTRICT_LOW);
//            XADDHelper.PlotXADD(hlowID, "hlowID");
            
//            int hXADDId = Controller.getXADD().apply(
//                           Controller.getXADD().apply(hhighID, phTID, XADD.PROD),
//                           Controller.getXADD().apply(hlowID, phFID, XADD.PROD), 
//                                                                   XADD.SUM);            
//            XADDHelper.PlotXADD(hXADDId, "hXADDId");
            
            qFuncID = Controller.getXADD().apply(Controller.getXADD().apply(bXADDId, sXADDId, XADD.SUM), hXADDId, XADD.SUM);            
//            XADDHelper.PlotXADD(qFuncID, "After 1 - " + currIterationNum);             
            
            /*
             * 2. Minimise over the actions for Player B ('b')
             */
            System.out.println(currIterationNum + ": ----Minimise over the actions for Player B ('b')");
            
            int mhighID = Controller.getXADD().opOut(qFuncID, mVarID, XADD.RESTRICT_HIGH);
//            XADDHelper.PlotXADD(mhighID, "mhighID");
            int mlowID = Controller.getXADD().opOut(qFuncID, mVarID, XADD.RESTRICT_LOW);
//            XADDHelper.PlotXADD(mlowID, "mlowID");

            qFuncID = Controller.getXADD().apply(mhighID, mlowID, XADD.MIN);
//            XADDHelper.PlotXADD(qFuncID, "After 2 - " + currIterationNum);       
            
            /*
             * 3. Maximise over the 
             */
            
            System.out.println(currIterationNum + ": ----Maxout Player A's pure strategy probabilities------");
            vFuncID = VIHelper.maxOutVar(qFuncID, "ps", 0, 1, logStream);
            vFuncID = VIHelper.maxOutVar(vFuncID, "pb", 0, 1, logStream);
            vFuncID = VIHelper.maxOutVar(vFuncID, "ph", 0, 1, logStream);
            
//            XADDHelper.PlotXADD(vFuncID, "After 3 - " + currIterationNum);
            
            /*
             * Plot the value function
             */
            String plotTitle = "Value Function - " + domainName + " " + currIterationNum;
            XADDHelper.Display3D(vFuncID, plotTitle, plot3DFile, domainDir);
        }
        System.out.println("----COMPLETE------");
    }

}
