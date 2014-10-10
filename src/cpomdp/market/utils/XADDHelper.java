/*
 *  XADDHelper.hava 
 * 
 * NOTES:
 *          1. This Class contains modified functions from xadd.XADD.java
 */
package cpomdp.market.utils;

import graph.Graph;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.text.DecimalFormat;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JToolBar;

import net.ericaro.surfaceplotter.JSurfacePanel;
import net.ericaro.surfaceplotter.surface.ArraySurfaceModel;
import xadd.XADDUtils;
import xadd.XADDUtils.ExportActionListener;
import camdp.CAMDP.FileOptions;

/**
 * Contains a selection of methods from XADD.java
 * 
 * @author 	Shamin Kinathil
 * @since	
 *
 */
public class XADDHelper {

    private static Runtime RUNTIME = Runtime.getRuntime();
    
    public static Integer COUNTER = 0;

    /* Cache maintenance */
    public final static boolean ALWAYS_FLUSH = false; // Always flush DD caches?
    public final static double FLUSH_PERCENT_MINIMUM = 0.3d; // Won't flush until < amt
   
    /* For printing */
    public static DecimalFormat _df = new DecimalFormat("#.########");

    /**
     * Build and XADD from a file
     * 
     * @param fileLocation	
     * @return	XADD instance built according to the file specification
     */
    public static int BuildXADD(String fileLocation) {

        File specFile = new File(fileLocation);
        if (!(specFile.exists() && specFile.isFile() && specFile.canRead())) {
            System.out.println("\nError: Unable to locate readable file: '" + "'" + specFile.toString());
        }

//        return XADDHelper.getXADD().buildCanonicalXADDFromFile(specFile.toString());
        return XADDWrapper.getInstance().buildCanonicalXADDFromFile(specFile.toString());
    }

    /**
     * 
     * @param xaddID
     * @param plotTitle
     */
    public static void PlotXADD(int xaddID, String plotTitle) {
        
        try {
            Graph gc = XADDWrapper.getInstance().getGraph(xaddID);
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
     * @param plottingFile
     * @param logFileRoot
     */
    public static void ExportXADD(int xadd_id, String label, String plottingFile,
                                                            String logFileRoot) {

        label = label.replace(".csamdp", "").replace(".camdp", "")
                .replace(".cmdp", "").replace('^', '_').replace("(", "")
                .replace(")", "").replace(":", "_").replace(" ", "");

        String xadd_filename = logFileRoot + "." + label + ".xadd";
        XADDWrapper.getInstance().exportXADDToFile(xadd_id, xadd_filename);

        // Copy over plotting options if they exist
        File file2D = new File(plottingFile + ".2d");
        if (file2D.exists()) {
            FileOptions opt = new FileOptions(plottingFile + ".2d");
            opt.exportToFile(xadd_filename + ".2d");
        }

        File file3D = new File(plottingFile + ".3d");
        if (file3D.exists()) {
            FileOptions opt = new FileOptions(plottingFile + ".3d");
            opt.exportToFile(xadd_filename + ".3d");
        }
    }

    /**
     * 
     * @param xadd_id
     * @param label
     * @param plottingFile
     * @param logFileRoot
     */
    public static void Display2D(int xadd_id, String label, String plottingFile, 
                                                            String logFileRoot) {

        XADDHelper.ExportXADD(xadd_id, label, plottingFile, logFileRoot);

        // If DISPLAY_2D is enabled, it is expected that necessary parameters
        // have been placed in a _problemFile + ".2d"
        FileOptions opt = new FileOptions(plottingFile + ".2d");

        XADDUtils.PlotXADD(XADDWrapper.getInstance(), xadd_id, opt._varLB.get(0),
                opt._varInc.get(0), opt._varUB.get(0), opt._bassign,
                opt._dassign, opt._var.get(0), logFileRoot + "." + label);
    }

    /**
     * 
     * @param xadd_id
     * @param label
     * @param plottingFile
     * @param logFileRoot
     */
    public static void Display3D(int xadd_id, String label, String plottingFile, 
                                                            String logFileRoot) {

        // If DISPLAY_3D is enabled, it is expected that necessary parameters 
        // have been placed in a _problemFile + ".3d"
        FileOptions opt = new FileOptions(plottingFile + ".3d");
        
        XADDUtils.Plot3DSurfXADD(XADDWrapper.getInstance(), xadd_id, 
                opt._varLB.get(0), opt._varInc.get(0), opt._varUB.get(0), 
                opt._varLB.get(1), opt._varInc.get(1), opt._varUB.get(1), 
                opt._bassign, opt._dassign, opt._var.get(0), opt._var.get(1), 
                (logFileRoot + "." + label).replace(".", File.separator));
    }    
    
    /**
     * 
     * @param plotTitle
     * @param outputFileName
     * @param xArr
     * @param yArr
     * @param zArr
     * @param alXSize
     * @param alYSize
     */
    public static void Plot3D(String plotTitle, String outputFileName, 
    		float[][] xArr, float[][] yArr, float[][] zArr, int alXSize, int alYSize) {
		
		// Plot
        // Create a Simple 2D XY plot window.
        JSurfacePanel jsp = new JSurfacePanel();
        jsp.setTitleText(plotTitle + " X = " + "mean" + " Y = " + "width");

        JFrame jf = new JFrame(plotTitle);
        jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        ArraySurfaceModel sm = new ArraySurfaceModel();
        sm.setValues(xArr[0][0], xArr[alYSize - 1][alXSize - 1],
                	yArr[0][0], yArr[alYSize - 1][alXSize - 1], 
                	alXSize, zArr, null);
        sm.setDisplayXY(true);
        sm.setDisplayZ(true);
        sm.setDisplayGrids(true);
        jsp.setModel(sm);

        // Add export button
        JToolBar toolbar = new JToolBar();
        JButton button = new JButton();
        button.setText("Re-export SVG and PNG to " + outputFileName.replace(".txt", "") + "{.png,.svg}");
        button.addActionListener(new ExportActionListener(jsp, outputFileName.replace(".txt", "")));
        toolbar.add(button);

        jf.getContentPane().add(toolbar, BorderLayout.NORTH);
        jf.getContentPane().add(jsp, BorderLayout.CENTER);
        jf.getContentPane().setPreferredSize(new Dimension(580, 560));
        jf.pack();
        jf.setVisible(true);

        // Export svg
        XADDUtils.ExportSurfPaneltoSVG(jsp, outputFileName.replace(".txt", ".svg"));

        //Try/catch added by Hadi
        try {
            // Export png (not publication quality, but more quickly viewed)
        	XADDUtils.ExportPanelToPNG(jsp, outputFileName.replace(".txt", ".png"));
        } catch (Exception e) {
            e.printStackTrace();
        }	
    }
    
    /**
     * 
     * @param special_nodes
     * @param forceFlush
     */
    public static void FlushCaches(List<Integer> special_nodes, boolean forceFlush) {

        if (((double) RUNTIME.freeMemory() /
                (double) RUNTIME.totalMemory()) > FLUSH_PERCENT_MINIMUM && !forceFlush) {
            System.out.println("No need to flush caches.");
            return; // Still enough free mem to exceed minimum requirements
        }

        // Commence cache flushing
        System.out.println("Before flush: " + XADDWrapper.getInstance()._hmInt2Node.size() + " XADD nodes in use, " + "freeMemory: " +
                _df.format(RUNTIME.freeMemory() / 10e6d) + " MB = " +
                _df.format(100d * RUNTIME.freeMemory() / (double) RUNTIME.totalMemory()) + "% available memory");

        XADDWrapper.getInstance().clearSpecialNodes();
        
        for (Integer node : special_nodes)
        	XADDWrapper.getInstance().addSpecialNode(node);

        XADDWrapper.getInstance().flushCaches();

        System.out.println("After flush: " + XADDWrapper.getInstance()._hmInt2Node.size() + " XADD nodes in use, " + "freeMemory: " +
                _df.format(RUNTIME.freeMemory() / 10e6d) + " MB = " +
                _df.format(100d * RUNTIME.freeMemory() / (double) RUNTIME.totalMemory()) + "% available memory");
    }
}
