package hgm.asve.cnsrv.factor;

import hgm.asve.cnsrv.factory.BaselineXaddFactorFactory;
import xadd.XADDUtils;

import javax.swing.*;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by Hadi Afshar.
 * Date: 10/10/13
 * Time: 5:55 PM
 */
public class FactorVisualizer {
    public double PRECISION_1D = 0.5;
    public double PRECISION_2D = 0.2;
    private BaselineXaddFactorFactory factory;

    public FactorVisualizer(BaselineXaddFactorFactory factory) {
        this.factory = factory;
    }

    //*******************************************************************************************
    public int visualizeFactor(Factor factor, String title) {
        /*if (title == null) {
            title = factory.getQuery().getQueryVariables()
                    + " | " + factory.getQuery().getBooleanInstantiatedEvidence()
                    + ", " + factory.getQuery().getContinuousInstantiatedEvidence();
        }*/
        switch (factor.getScopeVars().size()){
            case 0:
//                JOptionPane.showMessageDialog(null, factor.toString());
                System.out.println("[VISUAL] factor = " + factor);
                return 0;
            case 1:
                visualize1DFactor(factor, title);
                return 1;
            case 2:
                visualize2DFactor(factor, title);
                return 2;
            default:
                return -1;

        }
    }

    public int dataExportFactor(Factor factor, String fileNameWithoutExtension) throws FileNotFoundException {

        if (factor.getScopeVars().size() == 1) {
            dataExport1D(factor, fileNameWithoutExtension + ".txt");
            return 1;
        } else if (factor.getScopeVars().size() == 2) {
            dataExport2D(factor, fileNameWithoutExtension);
            return 2;
        }

        return -1;
    }


    private void visualize1DFactor(Factor factor, String title) {
        if (factor.getScopeVars().size() != 1) throw new RuntimeException("only one variable expected!");

        String var = factor.getScopeVars().iterator().next();

        double min_val = factory.getMinValue(var);
        double max_val = factory.getMaxValue(var);

//        System.out.println("min_val = " + min_val);
//        System.out.println("max_val = " + max_val);

        XADDUtils.PlotXADD(factory.getContext(), factor._xadd, min_val, 0.1d, max_val, var, title);
//        double integral = XADDUtils.TestNormalize(factor.getFactory().getContext(), factor.getNodeId(), var);
//        if (Math.abs(integral - 1d) > 0.001d)
//            System.err.println("WARNING: distribition does not integrate out to 1: " + integral);
    }

    private void visualize2DFactor(Factor factor, String title) {

        Iterator<String> iterator = factor.getScopeVars().iterator();
        String varX = iterator.next();
        String varY = iterator.next();
        Double min_val_x = factory.getContext()._hmMinVal.get(varX);
        Double max_val_x = factory.getContext()._hmMaxVal.get(varX);
        Double min_val_y = factory.getContext()._hmMinVal.get(varY);
        Double max_val_y = factory.getContext()._hmMaxVal.get(varY);

        assert (min_val_x != null && max_val_x != null && min_val_y != null && max_val_y != null);

        XADDUtils.Plot3DSurfXADD(factor._localContext, factor._xadd,
                min_val_x, PRECISION_2D, max_val_x,
                min_val_y, PRECISION_2D, max_val_y,
                varX, varY, title);
    }

    //*********************************************************
    private void dataExport1D(Factor factor, String filename) throws FileNotFoundException {
        PrintStream ps;
        ps = new PrintStream(new FileOutputStream(filename));
        String var = factor._vars.iterator().next();
        double low = factor._localContext._hmMinVal.get(var);
        double high = factor._localContext._hmMaxVal.get(var);

        HashMap<String, Double> dvars = new HashMap<String, Double>();

        ArrayList<Double> alX = new ArrayList<Double>();
        for (double x = low; x <= high; x += PRECISION_1D)
            alX.add(x);

        double[] xArr = new double[alX.size()];
        double[] yArr = new double[alX.size()];
        for (int i = 0; i < alX.size(); i++) {
            double x = alX.get(i);
            dvars.put(var, x);
            double y = factor._localContext.evaluate(factor._xadd, new HashMap<String, Boolean>() /*empty bool*/, dvars);
            dvars.remove(var);

            ps.println(x + "\t" + y);
        }
        ps.close();
    }

    private void dataExport2D(Factor factor, String filenameWithoutExtension) throws FileNotFoundException {
        dataExport2D(factor, null, filenameWithoutExtension);
    }

    private void dataExport2D(Factor factor, Factor divisor, String filenameWithoutExtension) throws FileNotFoundException {
        PrintStream ps_x;
        PrintStream ps_y;
        PrintStream ps_z;

        ps_x = new PrintStream(new FileOutputStream(filenameWithoutExtension + ".x.txt"));
        ps_y = new PrintStream(new FileOutputStream(filenameWithoutExtension + ".y.txt"));
        ps_z = new PrintStream(new FileOutputStream(filenameWithoutExtension + ".z.txt"));

        HashMap<String, Boolean> static_bvars = new HashMap<String, Boolean>();
        HashMap<String, Double> static_dvars = new HashMap<String, Double>();

        Iterator iter = factor._vars.iterator();
        String varX = (String) iter.next();
        String varY = (String) iter.next();
        double low_x = factor._localContext._hmMinVal.get(varX);
        double high_x = factor._localContext._hmMaxVal.get(varX);
        double low_y = factor._localContext._hmMinVal.get(varY);
        double high_y = factor._localContext._hmMaxVal.get(varY);
        double inc_x = 0.5d;
        double inc_y = inc_x;

        // Create a Simple 2D XY plot window.
        ArrayList<Double> alX = new ArrayList<Double>();
        for (double x = low_x; x <= high_x; x += inc_x)
            alX.add(x);
        ArrayList<Double> alY = new ArrayList<Double>();
        for (double y = low_y; y <= high_y; y += inc_y)
            alY.add(y);

        double[][] xArr = new double[alY.size()][alX.size()];
        double[][] yArr = new double[alY.size()][alX.size()];
        double[][] zArr = new double[alY.size()][alX.size()];
        for (int i = 0; i < alY.size(); i++) {
            for (int j = 0; j < alX.size(); j++) {

                double x = alX.get(j);
                double y = alY.get(i);

                static_dvars.put(varX, x);
                static_dvars.put(varY, y);
                double z = factor._localContext.evaluate(factor._xadd, static_bvars, static_dvars);
                if (divisor != null) {
                    // System.out.println(static_bvars + " " + static_dvars);
                    // System.out.println(divisor._localContext.getString(divisor._xadd));
                    z /= divisor._localContext.evaluate(divisor._xadd, static_bvars, static_dvars);
                    if (Double.isInfinite(z) || Double.isNaN(z) || z < 0d)
                        z = 0d;
                }
                static_dvars.remove(varX);
                static_dvars.remove(varY);

                xArr[i][j] = x;
                yArr[i][j] = y;
                zArr[i][j] = z; // x + y; //z;

                ps_x.print((j == 0 ? "" : "\t") + x);
                ps_y.print((j == 0 ? "" : "\t") + y);
                ps_z.print((j == 0 ? "" : "\t") + z);
            }
            ps_x.println();
            ps_y.println();
            ps_z.println();
        }

        ps_x.close();
        ps_y.close();
        ps_z.close();
    }

}
