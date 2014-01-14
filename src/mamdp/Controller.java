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

import xadd.ExprLib.VarExpr;
import xadd.XADD;
import xadd.XADD.XADDLeafMinOrMax;
import xadd.XADDUtils;
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
    
    private static String DomainDirectoryPath = "";
    
    private static XADD context = null;

    private static void Usage() {
        System.out.println("\n" + Controller.USAGE_STRING);
        System.exit(1);
    }   

    private static String DomainsDirectory() {
        
        if(Controller.DomainDirectoryPath.isEmpty()) {
            Controller.DomainDirectoryPath = System.getProperty("user.dir");
            //+
             //                           File.separator + SOURCE_DIR + 
              //                          File.separator + Controller.DOMAIN_DIR;
        }
        
        return Controller.DomainDirectoryPath;
    }
    
    private static String DomainDirectory(String domain) {        
        return Controller.DomainsDirectory() +
        		File.separator + SOURCE_DIR +
        		File.separator + "mamdp" +
        		File.separator + Controller.DOMAIN_DIR +
        		File.separator + domain;
    }    
    
    private static XADD getXADD() {
        
        if(context == null) {
            context = new XADD();
        }
        
        return context;
     }    
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        
        /*----------------------------------------------------------------------
         * Parse the input arguments
         ---------------------------------------------------------------------*/
        
        //if (args.length != 1) {
        //    Controller.Usage();
       // }
        
        Controller.MatchingPennies();
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
    
    /**
     * Matching pennies example
     */
    private static void MatchingPennies() {
        
        String domainDir = DomainDirectory("matching_pennies");
        String rewardFile = domainDir + File.separator + "reward.xadd";
        String constraintsFile = domainDir + File.separator + "constraints.xadd";
        String plot2DFile = domainDir + File.separator + "plot";
        String logFile = domainDir + File.separator + "matching_pennies.log";

        Integer vFuncID = null;
        PrintStream logStream = null;
        //Double probHead = 0.5; //The probability that Player 1 chooses Heads \pi_a_{T}
        
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
        
        //int pTrueID = context.getTermNode(new DoubleExpr(probHead));
        //int pFalseID = context.getTermNode(new DoubleExpr(1 - probHead));
        
        int pTID = Controller.getXADD().getTermNode(new VarExpr("p"));
        int pFID = Controller.getXADD().getTermNode(new VarExpr("1 - p"));
        
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
        
        //vFuncID = context.apply(vFuncID, constraintsID, XADD.PROD);        
        //Controller.PlotXADD(vFuncID, "After 3 a");
        
        vFuncID = Controller.maxOutVar(vFuncID, "p", 0, 1, logStream);        
        Controller.PlotXADD(vFuncID, "After 3");
        
        /*
         *  Plot the value function 
         */
        Controller.Display2D(vFuncID, "Value Function", plot2DFile, domainDir);
    }
}
