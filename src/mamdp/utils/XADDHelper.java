/*
 *  XADDHelper.hava 
 * 
 * NOTES:
 *          1. This Class contains modified functions from xadd.XADD.java
 */
package mamdp.utils;

import graph.Graph;

import java.io.File;

import camdp.CAMDP.FileOptions;
import xadd.XADD;
import xadd.XADDUtils;

/**
 * Contains XADD methods from XADD.java
 * @author Shamin Kinathil
 *
 */
public class XADDHelper {

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
     * XADD related functions 
     *-----------------------------------------------------------------------*/
    
    /**
     * 
     * @param fileLocation
     * @return
     */
    public static int BuildXADD(String fileLocation) {

        File specFile = new File(fileLocation);
        if (!(specFile.exists() && specFile.isFile() && specFile.canRead())) {
            System.out.println("\nError: Unable to locate readable file: '" + "'" + specFile.toString());
        }

        return XADDHelper.getXADD().buildCanonicalXADDFromFile(specFile.toString());
    }

    /**
     * 
     * @param xaddID
     * @param plotTitle
     */
    public static void PlotXADD(int xaddID, String plotTitle) {
        
        try {
            Graph gc = XADDHelper.getXADD().getGraph(xaddID);
            gc.launchViewer(plotTitle);
        
        } catch (IllegalArgumentException e) {
            System.err.println("XADDHelper.PlotXADD Caught Exception: " + plotTitle 
                    + "\n"+ e.getMessage());
        }
    }

    /**
     * 
     * @param xadd_id
     * @param label
     * @param problemFile
     * @param logFileRoot
     */
    public static void ExportXADD(int xadd_id, String label, String problemFile,
                                                            String logFileRoot) {

        label = label.replace(".csamdp", "").replace(".camdp", "")
                .replace(".cmdp", "").replace('^', '_').replace("(", "")
                .replace(")", "").replace(":", "_").replace(" ", "");

        String xadd_filename = logFileRoot + "." + label + ".xadd";
        XADDHelper.getXADD().exportXADDToFile(xadd_id, xadd_filename);

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

    /**
     * 
     * @param xadd_id
     * @param label
     * @param problemFile
     * @param logFileRoot
     */
    public static void Display2D(int xadd_id, String label, String problemFile, 
                                                            String logFileRoot) {

        XADDHelper.ExportXADD(xadd_id, label, problemFile, logFileRoot);

        // If DISPLAY_2D is enabled, it is expected that necessary parameters
        // have been placed in a _problemFile + ".2d"
        FileOptions opt = new FileOptions(problemFile + ".2d");

        XADDUtils.PlotXADD(XADDHelper.getXADD(), xadd_id, opt._varLB.get(0),
                opt._varInc.get(0), opt._varUB.get(0), opt._bassign,
                opt._dassign, opt._var.get(0), logFileRoot + "." + label);
    }

    /**
     * 
     * @param xadd_id
     * @param label
     * @param problemFile
     * @param logFileRoot
     */
    public static void Display3D(int xadd_id, String label, String problemFile, 
                                                            String logFileRoot) {

        // If DISPLAY_3D is enabled, it is expected that necessary parameters 
        // have been placed in a _problemFile + ".3d"
        FileOptions opt = new FileOptions(problemFile + ".3d");
        
        XADDUtils.Plot3DSurfXADD(XADDHelper.getXADD(), xadd_id, 
                opt._varLB.get(0), opt._varInc.get(0), opt._varUB.get(0), 
                opt._varLB.get(1), opt._varInc.get(1), opt._varUB.get(1), 
                opt._bassign, opt._dassign, opt._var.get(0), opt._var.get(1), 
                logFileRoot + "." + label);
    }    
    
}
