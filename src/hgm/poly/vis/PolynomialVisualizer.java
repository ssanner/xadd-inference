package hgm.poly.vis;

import hgm.poly.ConstrainedPolynomial;
import hgm.poly.PolynomialException;
import hgm.sampling.VarAssignment;
import jahuwaldt.plot.*;
import net.ericaro.surfaceplotter.JSurfacePanel;
import net.ericaro.surfaceplotter.surface.ArraySurfaceModel;
import plot.PlotExample;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Hadi Afshar.
 * Date: 23/02/14
 * Time: 9:40 PM
 */
public class PolynomialVisualizer {
    public static void visualize(ConstrainedPolynomial constrainedPolynomial, double min, double max, double step, String title) {
        int numVars = constrainedPolynomial.collectContinuousVars().length;
        switch (numVars) {
            case 0:
                throw new PolynomialException("0 vars... in a polynomial factory");
            case 1:
                visualize1DimXadd(constrainedPolynomial, min, max, step, title);
                break;
            case 2:
                visualize2DimXadd(constrainedPolynomial, min, max, step, title);
                break;
            default:
                System.err.println("a node with numVars = " + numVars + " cannot be visualized");
        }
    }

//    public static void visualize(ode node, String title, XADD context) {
//        int numVars = node.collectVars().size();
//        if (numVars == 1) visualize1DimXadd(node, title, context);
//        else if (numVars == 2) visualize2DimXadd(node, title, context);
//        else System.err.println("a node with numVars = " + numVars + " cannot be visualized");
//    }

    /*public static void visualize2DimXadd(ConstrainedPolynomial cp, String title) {
        String[] vars = cp.collectVars();
        String varX = iterator.next();
        String varY = iterator.next();
        double min_val_x = context._hmMinVal.get(varX);
        double max_val_x = context._hmMaxVal.get(varX);
        double min_val_y = context._hmMinVal.get(varY);
        double max_val_y = context._hmMaxVal.get(varY);
        XADDUtils.Plot3DSurfXADD(context, context._hmNode2Int.get(node),
                min_val_x, 0.5d, max_val_x,
                min_val_y, 0.5d, max_val_y,
                varX, varY, title);
    }*/

    public static void visualize2DimXadd(ConstrainedPolynomial cp,  double min, double max, double step, String title) {

        String[] vars = cp.collectContinuousVars();
        Plot3DSurfXADD(cp,
                min, step, max,
                min, step, max,
                vars[0], vars[1], title);
    }

    public static void Plot3DSurfXADD(ConstrainedPolynomial cp,
                                      double low_x, double inc_x, double high_x,
                                      double low_y, double inc_y, double high_y,
//                                      HashMap<String, Boolean> static_bvars, HashMap<String, Double> static_dvars,
                                      String xVar, String yVar, String title) {
        int sampleX = (int) Math.round((high_x - low_x) / inc_x);
        int sampleY = (int) Math.round((high_y - low_y) / inc_y);
        if (sampleX != sampleY) {
            System.out.println("samples X and Y must be equal for Surface Plot, using X samples");
        }
        Plot3DSurfXADD(cp, low_x, high_x, low_y, high_y, (int) Math.ceil((high_x - low_x) / inc_x),
//                static_bvars, static_dvars,
                xVar, yVar, title);
    }

    public static void Plot3DSurfXADD(ConstrainedPolynomial cp,
                                      double low_x, double high_x,
                                      double low_y, double high_y,
                                      int nSamples,
//                                      HashMap<String, Boolean> static_bvars,
//                                      HashMap<String, Double> static_dvars,
                                      String xVar, String yVar, String title) {

        ArrayList<Float> alX = new ArrayList<Float>(nSamples);
        float inc_x = (float) (high_x - low_x) / nSamples;
        float temp_x = (float) low_x;
        for (int i = 0; i < nSamples; i++) {
            temp_x += inc_x;
            alX.add(temp_x);
        }
        ArrayList<Float> alY = new ArrayList<Float>(nSamples);
        float inc_y = (float) (high_y - low_y) / nSamples;
        float temp_y = (float) low_y;
        for (int i = 0; i < nSamples; i++) {
            temp_y += inc_y;
            alY.add(temp_y);
        }
        if (alX.size() != alY.size()) {
            System.err.println("ERROR: Surface plotting requires the same number of samples along the x and y axes");
            return;
        }

//        PrintStream ps = null;
//        String filename = title.replace('^', '_').replace("(", "").replace(")", "").replace(":", "_").replace(" ", "").replace(".dot", "") + ".txt";
//        title = RemovePathAndExt(title);
//        try {
//            ps = new PrintStream(new FileOutputStream(filename));
//        } catch (Exception e) {
//            System.err.println("Could not open " + filename + " for data export.");
//            ps = new DevNullPrintStream();
//        }

        VarAssignment varAssign = new VarAssignment(new HashMap<String, Boolean>(), new HashMap<String, Double>());//static_bvars, static_dvars);
        float[][] xArr = new float[alY.size()][alX.size()];
        float[][] yArr = new float[alY.size()][alX.size()];
        float[][] zArr = new float[alY.size()][alX.size()];
        for (int i = 0; i < alY.size(); i++) {
            for (int j = 0; j < alX.size(); j++) {

                float x = alX.get(j);
                float y = alY.get(i);

                varAssign.assignOrCreateContinuousVariable(xVar, (double) x);
                varAssign.assignOrCreateContinuousVariable(yVar, (double) y);
                float z = (float) cp.evaluate(varAssign);
                if (Float.isInfinite(z)) z = Float.NaN;

//                ps.println(x + "\t" + y + "\t" + z);
                xArr[i][j] = x;
                yArr[i][j] = y;
                zArr[j][i] = z; //x + y; //z;
            }
        }
//        ps.close();

        // Create a Simple 2D XY plot window.
        JSurfacePanel jsp = new JSurfacePanel();
        jsp.setTitleText(title + " X = " + xVar + " Y = " + yVar);

        JFrame jf = new JFrame(title);
        jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        ArraySurfaceModel sm = new ArraySurfaceModel();
        sm.setValues(xArr[0][0], xArr[alY.size() - 1][alX.size() - 1],
                yArr[0][0], yArr[alY.size() - 1][alX.size() - 1], alX.size(), zArr, null);
        sm.setDisplayXY(true);
        sm.setDisplayZ(true);
        sm.setDisplayGrids(true);
        jsp.setModel(sm);

        // Add export button
        JToolBar toolbar = new JToolBar();
//        JButton button = new JButton();
//        button.setText("Re-export SVG and PNG to " + filename.replace(".txt", "") + "{.png,.svg}");
//        button.addActionListener(new ExportActionListener(jsp, filename.replace(".txt", "")));
//        toolbar.add(button);

        jf.getContentPane().add(toolbar, BorderLayout.NORTH);
        jf.getContentPane().add(jsp, BorderLayout.CENTER);
        jf.getContentPane().setPreferredSize(new Dimension(580, 560));
        jf.pack();
        jf.setVisible(true);

        // Export svg
//        ExportSurfPaneltoSVG(jsp, filename.replace(".txt", ".svg"));

        //Try/catch added by Hadi
//        try {
            // Export png (not publication quality, but more quickly viewed)
//            ExportPanelToPNG(jsp, filename.replace(".txt", ".png"));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }


    /*public static void visualize1DimXadd(node, String title, XADD context) {
        if (node.collectVars().size() != 1) throw new RuntimeException("only one variable expected!");

        String var = node.collectVars().iterator().next();

        double min_val = context._hmMinVal.get(var);
        double max_val = context._hmMaxVal.get(var);

        XADDUtils.PlotXADD(context, context._hmNode2Int.get(node), min_val, 0.1d, max_val, var, title);
    }*/

    //***************************************************************************************************************

    public static void visualize1DimXadd(ConstrainedPolynomial cp,  double min, double max, double step, String title) {
//        if (node.collectVars().size() != 1) throw new RuntimeException("only one variable expected!");

        String var = cp.collectContinuousVars()[0];

        plotXADD(cp, min, step, max, var, title);
    }

    @SuppressWarnings("unchecked")
    public static void plotXADD(ConstrainedPolynomial cp,
                                double low, double inc,
                                double high, String xVar, String title) {

//        PrintStream ps = null;
//        String filename = title.replace('^', '_').replace("(", "").replace(")", "").replace(":", "_").replace(" ", "").replace(".dot", "") + ".txt";
//        title = RemovePathAndExt(title);
//        try {
//            ps = new PrintStream(new FileOutputStream(filename));
//        } catch (Exception e) {
//            System.err.println("Could not open " + filename + " for data export.");
//            ps = new DevNullPrintStream();
//        }

        // Generate (x,y) pairs in alX and alY, also track count of infinite points
        ArrayList<Double> alX = new ArrayList<Double>();
        ArrayList<Double> alY = new ArrayList<Double>();
        for (double x = low; x <= high; x += inc)
            alX.add(x);
        double min_y_val = Double.POSITIVE_INFINITY;
        double max_y_val = Double.NEGATIVE_INFINITY;
        int num_inf_points = 0;

        VarAssignment varAssign = new VarAssignment(new HashMap<String, Boolean>(), new HashMap<String, Double>());
        for (int i = 0; i < alX.size(); i++) {

            double x = alX.get(i);
            varAssign.assignOrCreateContinuousVariable(xVar, x);
            double y = cp.evaluate(varAssign);
//            static_dvars.remove(xVar);

            alY.add(y);
//            ps.println(x + "\t" + y);

            if (Double.isInfinite(y))
                num_inf_points++;
            else {
                // Record min max values if not infinite
                if (y < min_y_val)
                    min_y_val = y;
                if (y > max_y_val)
                    max_y_val = y;
            }
        }
//        ps.close();

        // Go over points separate into infinity and non-infinity lines
        double[] xArr = new double[alX.size() - num_inf_points];
        double[] yArr = new double[alX.size() - num_inf_points];
        double[] xArrInf = new double[num_inf_points];
        double[] yArrInf = new double[num_inf_points];
        int index = 0;
        int index_inf = 0;
        for (int i = 0; i < alX.size(); i++) {
            double x = alX.get(i);
            double y = alY.get(i);
            if (Double.isInfinite(y)) {
                xArrInf[index_inf] = x;
                yArrInf[index_inf] = (y > 0d ? max_y_val : min_y_val);
                index_inf++;
            } else {
                xArr[index] = x;
                yArr[index] = y;
                index++;
            }
        }

        Plot2D aPlot = new SimplePlotXY(xArr, yArr, title, xVar , null,
                null, null/* new jahuwaldt.plot.CircleSymbol() *//* new XSymbol() */);

        // Plot non-infinity as red without markers
        PlotRunList runs = aPlot.getRuns();
        PlotRun run = (PlotRun) runs.get(0);
        run.setLineColor(Color.red);

        // Plot infinity values as X's at the minimum or maximum y value
        PlotRun run2 = new PlotRun(xArrInf, yArrInf, false, new XSymbol());
        runs.add(run2);

        // Make the horizontal axis a log axis.
        // PlotAxis xAxis = aPlot.getHorizontalAxis();
        // xAxis.setScale(new Log10AxisScale());

        PlotPanel panel = new PlotPanel(aPlot);
        panel.setBackground(Color.white);
        PlotExample window = new PlotExample(title, panel);
        window.setSize(500, 300);
        window.setLocation(50, 50);
        window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        window.show();

        // Export png (not publication quality, but more quickly viewed)
//        ExportPanelToPNG(panel, filename.replace(".txt", ".png"));
    }

}
