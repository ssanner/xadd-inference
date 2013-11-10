package hgm.utils.vis;

import hgm.utils.Function;
import net.ericaro.surfaceplotter.JSurfacePanel;
import net.ericaro.surfaceplotter.surface.ArraySurfaceModel;
import util.DevNullPrintStream;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Hadi Afshar.
 * Date: 22/10/13
 * Time: 10:26 AM
 */
public class GeneralVisualizationUtils {
    public static double PRECISION =0.25d;

    public static void plot(Function function, double lowerBound, double upperBound, String title) {
        String[] varNames = function.getEffectiveVarNames();

        switch (function.getInputArgumentDimension()) {
            case 2: plot3DSurfXADD(function,
                    lowerBound, PRECISION,  upperBound,
                    lowerBound, PRECISION, upperBound, varNames[0], varNames[1], title);
                break;
            default:
                throw new RuntimeException("not supported");
        }
    }


    public static void plot3DSurfXADD(Function function,
                                      double low_x, double inc_x, double high_x,
                                      double low_y, double inc_y, double high_y,
                                      String xVar, String yVar, String title) {

        plot3DSurfXADD(function, low_x, inc_x, high_x, low_y, inc_y, high_y,
                new HashMap<String, Boolean>(), new HashMap<String, Double>(),
                xVar, yVar, title);
    }

    private static void plot3DSurfXADD(Function function,
                                       double low_x, double inc_x, double high_x, double low_y, double inc_y, double high_y,
                                       HashMap<String, Boolean> static_bvars, HashMap<String, Double> static_dvars,
                                       String xVar, String yVar, String title) {
        int sampleX= (int) Math.round( (high_x-low_x)/inc_x);
        int sampleY= (int) Math.round( (high_y-low_y)/inc_y);
        if (sampleX != sampleY){
            System.out.println("samples X and Y must be equal for Surface Plot, using X samples");
        }
        plot3DSurfXADD(function, low_x, high_x, low_y, high_y, (int) Math.ceil((high_x - low_x) / inc_x),
                static_bvars, static_dvars, xVar, yVar, title);
    }

    private static void plot3DSurfXADD(Function function,
                                       double low_x, double high_x, double low_y, double high_y, int nSamples,
                                       HashMap<String, Boolean> static_bvars, HashMap<String, Double> static_dvars,
                                       String xVar, String yVar, String title) {

        ArrayList<Float> alX = new ArrayList<Float>(nSamples);
        float inc_x = (float) (high_x-low_x)/nSamples;
        float temp_x = (float) low_x;
        for (int i=0;i<nSamples; i++){
            temp_x += inc_x;
            alX.add(temp_x);
        }
        ArrayList<Float> alY = new ArrayList<Float>(nSamples);
        float inc_y = (float) (high_y-low_y)/nSamples;
        float temp_y = (float) low_y;
        for (int i=0;i<nSamples; i++){
            temp_y += inc_y;
            alY.add(temp_y);
        }
        if (alX.size() != alY.size()) {
            System.err.println("ERROR: Surface plotting requires the same number of samples along the x and y axes");
            return;
        }

        PrintStream ps = null;
        String filename = title.replace('^', '_').replace("(", "").replace(")", "").replace(":", "_").replace(" ", "").replace(".dot","") + ".txt";
        title = RemovePathAndExt(title);
        try {
            ps = new PrintStream(new FileOutputStream(filename));
        } catch (Exception e) {
            System.err.println("Could not open " + filename + " for data export.");
            ps = new DevNullPrintStream();
        }

        static_dvars = new HashMap<String, Double>(static_dvars);
        float[][] xArr = new float[alY.size()][alX.size()];
        float[][] yArr = new float[alY.size()][alX.size()];
        float[][] zArr = new float[alY.size()][alX.size()];
        for (int i = 0; i < alY.size(); i++) {
            for (int j = 0; j < alX.size(); j++) {

                float x = alX.get(j);
                float y = alY.get(i);

                static_dvars.put(xVar, (double)x);
                static_dvars.put(yVar, (double)y);
                float z = (float) function.func(x, y);//context.evaluate(xadd, static_bvars, static_dvars).floatValue();
                if (Float.isInfinite(z)) z=Float.NaN;
                static_dvars.remove(xVar);
                static_dvars.remove(yVar);

                //if (z > 0.1d)
                //	System.out.println("f(" + x + "," + y + ") = " + z);
                ps.println(x + "\t" + y + "\t" + z);
                xArr[i][j] = x;
                yArr[i][j] = y;
                zArr[j][i] = z; //x + y; //z;
            }
        }
        ps.close();

        // Create a Simple 2D XY plot window.
        JSurfacePanel jsp = new JSurfacePanel();
        jsp.setTitleText(title + " X = "+ xVar+ " Y = "+ yVar);

        JFrame jf = new JFrame(title);
        jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        ArraySurfaceModel sm = new ArraySurfaceModel();
        sm.setValues(xArr[0][0],xArr[alY.size()-1][alX.size()-1],
                yArr[0][0],yArr[alY.size()-1][alX.size()-1],alX.size(), zArr, null);
        sm.setDisplayXY(true);
        sm.setDisplayZ(true);
        sm.setDisplayGrids(true);
        jsp.setModel(sm);

        // Add export button
        JToolBar toolbar = new JToolBar();
        JButton button = new JButton();
        button.setText("Re-export SVG and PNG to " + filename.replace(".txt", "") + "{.png,.svg}");
        button.addActionListener(new ExportActionListener(jsp, filename.replace(".txt", "")));
        toolbar.add(button);

        jf.getContentPane().add(toolbar, BorderLayout.NORTH);
        jf.getContentPane().add(jsp, BorderLayout.CENTER);
        jf.getContentPane().setPreferredSize(new Dimension(580, 560));
        jf.pack();
        jf.setVisible(true);

        // Export svg
        ExportSurfPaneltoSVG(jsp, filename.replace(".txt", ".svg"));

        // Export png (not publication quality, but more quickly viewed)
        ExportPanelToPNG(jsp, filename.replace(".txt", ".png"));
    }

    // Export button support for 3D view
    public static class ExportActionListener implements ActionListener {
        String _filename = null;
        JSurfacePanel _jsp = null;
        public ExportActionListener(JSurfacePanel jsp, String fname) {
            _jsp = jsp;
            _filename = fname;
        }

        public void actionPerformed(ActionEvent e) {
            // Export to PNG and SVG
            System.out.println("Re-exporting SVG and PNG to '" + _filename.replace(".txt", "") + "{.png,.svg}'");
            ExportSurfPaneltoSVG(_jsp, _filename + ".svg");
            ExportPanelToPNG(_jsp, _filename + ".png");
        }
    }

    public static void ExportSurfPaneltoSVG(JSurfacePanel jsp, String filename) {
        try {
            File svg_file = new File(filename);
            jsp.getSurface().doExportSVG(svg_file);
            //System.out.println("Exported SVG file: " + svg_file.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Could not open " + filename + " for SVG export.");
        }
    }

    public static void ExportPanelToPNG(JPanel panel, String filename) {
        BufferedImage bi = new BufferedImage(panel.getSize().width, panel.getSize().height, BufferedImage.TYPE_INT_ARGB);
        Graphics g = bi.createGraphics();
        panel.paint(g);  // seems this is a non-blocking call
        try {
            Thread.sleep(500); // allow paint to complete
            File png_file = new File(filename);
            ImageIO.write(bi, "png", png_file);
            //System.out.println("Exported PNG file: " + png_file.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Could not export: " + filename);
        }
        g.dispose();
    }

    public static String RemovePathAndExt(String label) {
        String[] split = label.split("[\\\\/]");
        //for (String s : split) System.out.print("'" + s + "' ");
        label = split[split.length - 1];
        split = label.split("[\\.:]");
        //for (String s : split) System.out.print("'" + s + "' ");
//		if (split.length == 2)
//			label = split[0];
//		else if (split.length > 2)
//			label = split[0] + ":" + split[2];
//		return label;
        if (split.length - 1 < 0)
            return split[0];
        else
            return split[split.length - 1];
    }
}
