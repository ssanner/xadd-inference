package xadd;

import jahuwaldt.plot.CircleSymbol;
import jahuwaldt.plot.ContourPlot;
import jahuwaldt.plot.Log10AxisScale;
import jahuwaldt.plot.Plot2D;
import jahuwaldt.plot.PlotAxis;
import jahuwaldt.plot.PlotDatum;
import jahuwaldt.plot.PlotPanel;
import jahuwaldt.plot.PlotRun;
import jahuwaldt.plot.PlotRunList;
import jahuwaldt.plot.PlotSymbol;
import jahuwaldt.plot.SimplePlotXY;
import jahuwaldt.plot.SquareSymbol;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.WindowConstants;

import plot.PlotExample;
import graph.Graph;
import xadd.XADD.BoolDec;

public class TestXADDDist {

	/**
	 * @param args
	 */
	//public static String  NAME_FILE_2D="refinement2D";
	public static String  NAME_FILE_2D="roverNonlinear2D";
	public static int counter = 0;
	public static void main(String[] args) {

		XADD xadd_context = new XADD();
		counter = 0;
		// Put all boolean variables first to avoid reordering clashes
		xadd_context.getVarIndex(xadd_context.new BoolDec("f"), true);
		xadd_context.getVarIndex(xadd_context.new BoolDec("g"), true);
		xadd_context.getVarIndex(xadd_context.new BoolDec("h"), true);

		// Setup static vars
		HashMap<String, Double> dvars = new HashMap<String, Double>();
		dvars.put("k", 0d);
		dvars.put("x1", 0d);
		HashMap<String, Boolean> bvars = new HashMap<String, Boolean>();
		bvars.put("f", true);

		// int xadd1 = XADD.TestBuild(xadd_context, "./src/xadd/test2.xadd");
		// int xadd1 = TestXADD.TestBuild(xadd_context,
		// "./src/xadd/normal_mix_simple.xadd");
		// xadd_context.getGraph(xadd1).launchViewer();
		// PlotXADD(xadd_context, xadd1, -10d, 0.25d, 10d, bvars, dvars, "y",
		// "Normal Mix Simple");
		// System.out.println("NS Norm: " + TestNormalize(xadd_context, xadd1,
		// bvars, dvars, "y") + "\n\n\n");
		//
		// int xadd1u = TestXADD.TestBuild(xadd_context,
		// "./src/xadd/uniform_mix_simple.xadd");
		// xadd_context.getGraph(xadd1u).launchViewer();
		// PlotXADD(xadd_context, xadd1u, -10d, 0.25d, 10d, bvars, dvars, "y",
		// "Uniform Mix Simple");
		// System.out.println("US Norm: " + TestNormalize(xadd_context, xadd1u,
		// bvars, dvars, "y") + "\n\n\n");
		//
		// int xadd1t = TestXADD.TestBuild(xadd_context,
		// "./src/xadd/triangle_mix_simple.xadd");
		// xadd_context.getGraph(xadd1t).launchViewer();
		// PlotXADD(xadd_context, xadd1t, -10d, 0.25d, 10d, bvars, dvars, "y",
		// "Triangular Mix Simple");
		// System.out.println("TS Norm: " + TestNormalize(xadd_context, xadd1t,
		// bvars, dvars, "y") + "\n\n\n");

		// int xadd2 = TestXADD.TestBuild(xadd_context,
		// "./src/xadd/normal_mix.xadd");
		// xadd_context.getGraph(xadd2).launchViewer();
		// dvars.put("k", 9d);
		// dvars.put("x1", 1d);
		// PlotXADD(xadd_context, xadd2, -20d, 0.25d, 20d, bvars, dvars, "y",
		// "Normal Mix");
		// System.out.println("N Norm: " + TestNormalize(xadd_context, xadd2,
		// bvars, dvars, "y") + "\n\n\n");

		int xadd2u = TestXADD.TestBuild(xadd_context,
				"./src/xadd/uniform_mix.xadd");
		xadd_context.getGraph(xadd2u).launchViewer();
		PlotXADD(xadd_context, xadd2u, -20d, 0.25d, 20d, bvars, dvars, "y",
				"Uniform Mix");
		System.out.println("U Norm: "
				+ TestNormalize(xadd_context, xadd2u, bvars, dvars, "y")
				+ "\n\n\n");

		// int xadd2t = TestXADD.TestBuild(xadd_context,
		// "./src/xadd/triangle_mix.xadd");
		// xadd_context.getGraph(xadd2t).launchViewer();
		// PlotXADD(xadd_context, xadd2t, -20d, 0.25d, 20d, bvars, dvars, "y",
		// "Triangular Mix");
		// System.out.println("T Norm: " + TestNormalize(xadd_context, xadd2t,
		// bvars, dvars, "y") + "\n\n\n");
		//
		// int xadd_all = TestXADD.TestBuild(xadd_context,
		// "./src/xadd/all_mix.xadd");
		// xadd_context.getGraph(xadd_all).launchViewer();
		// PlotXADD(xadd_context, xadd_all, -20d, 0.25d, 20d, bvars, dvars, "y",
		// "All Mix");
		// System.out.println("T Norm: " + TestNormalize(xadd_context, xadd_all,
		// bvars, dvars, "y") + "\n\n\n");
	}

	public static double TestNormalize(XADD context, int xadd, String int_var) {
		return TestNormalize(context, xadd, new HashMap<String, Boolean>(),
				new HashMap<String, Double>(), int_var);
	}

	public static double TestNormalize(XADD context, int xadd,
			HashMap<String, Boolean> static_bvars,
			HashMap<String, Double> static_dvars, String int_var) {
		int xadd_int = context.computeDefiniteIntegral(xadd, int_var);
		// System.out.println("Int result: " + context.getString(xadd_int));
		System.out.println("Evaluating @ " + static_bvars + " " + static_dvars);
		return context.evaluate(xadd_int, static_bvars, static_dvars);
	}

	public static void PlotXADD(XADD context, int xadd, double low, double inc,
			double high, String xVar, String title) {
		PlotXADD(context, xadd, low, inc, high, new HashMap<String, Boolean>(),
				new HashMap<String, Double>(), xVar, title);
	}

	public static void PlotXADD(XADD context, int xadd, double low, double inc,
			double high, HashMap<String, Boolean> static_bvars,
			HashMap<String, Double> static_dvars, String xVar, String title) {

		// Create a Simple 2D XY plot window.
		ArrayList<Double> alX = new ArrayList<Double>();
		for (double x = low; x <= high; x += inc)
			alX.add(x);
		counter++;
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(NAME_FILE_2D+counter+".txt"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		double[] xArr = new double[alX.size()];
		double[] yArr = new double[alX.size()];
		for (int i = 0; i < alX.size(); i++) {
			double x = alX.get(i);
			static_dvars.put(xVar, x);
			double y = context.evaluate(xadd, static_bvars, static_dvars);
			static_dvars.remove(xVar);

			try {
				out.append(y + " ");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			xArr[i] = x;
			yArr[i] = y;
			
		}
		try {
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Plot2D aPlot = new SimplePlotXY(xArr, yArr, title, xVar + " @ "
				+ static_bvars + " " + static_dvars, "f(" + xVar + ")", null,
				null, null/* new jahuwaldt.plot.CircleSymbol() *//* new XSymbol() */);

		// Color line red
		PlotRunList runs = aPlot.getRuns();
		PlotRun run = (PlotRun) runs.get(0);
		run.setLineColor(Color.red);

		// Make the horizontal axis a log axis.
		// PlotAxis xAxis = aPlot.getHorizontalAxis();
		// xAxis.setScale(new Log10AxisScale());

		// Create a 2nd run and add it to the plot.
		// PlotRunList runs = aPlot.getRuns();
		// runs.add(new PlotRun(xArr, yArrAlt, true, new SquareSymbol()));

		PlotPanel panel = new PlotPanel(aPlot);
		panel.setBackground(Color.white);
		PlotExample window = new PlotExample(title, panel);
		window.setSize(500, 300);
		window.setLocation(50, 50);
		window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		window.show();
	}

	public static void Plot3DXADD(XADD context, int xadd, double low_x,
			double inc_x, double high_x, double low_y, double inc_y,
			double high_y, String xVar, String yVar, String title) {

		Plot3DXADD(context, xadd, low_x, inc_x, high_x, low_y, inc_y, high_y,
				new HashMap<String, Boolean>(), new HashMap<String, Double>(),
				xVar, yVar, title);
	}

	public static void Plot3DXADD(XADD context, int xadd, double low_x,
			double inc_x, double high_x, double low_y, double inc_y,
			double high_y, HashMap<String, Boolean> static_bvars,
			HashMap<String, Double> static_dvars, String xVar, String yVar,
			String title) {

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

				static_dvars.put(xVar, x);
				static_dvars.put(yVar, y);
				double z = context.evaluate(xadd, static_bvars, static_dvars);
				if (Double.isInfinite(z)) z=-300;
				static_dvars.remove(xVar);
				static_dvars.remove(yVar);

				if (z > 0.1d)
					System.out.println("f(" + x + "," + y + ") = " + z);
				xArr[i][j] = x;
				yArr[i][j] = y;
				zArr[i][j] = z; //x + y; //z;
			}
		}
		for (int i=0;i<alY.size();i++)
		{
			for (int j=0;j<alX.size();j++)
				System.out.print(" " + zArr[i][j]);
			System.out.println();
		}
		//String title = "f(" + xVar + "," + yVar + ") @ " + static_bvars + " " + static_dvars;
		Plot2D aPlot = new ContourPlot(xArr, yArr, zArr, 12, false, title, 
				xVar, yVar, null, null);

		// Colorize the contours.
		((ContourPlot) aPlot).colorizeContours(Color.blue, Color.red);

		// Create a run that contains the original XY data points we just put
		// contours through.
		// We'll plot it with symbols so we can see the location of the original
		// data points.
		PlotSymbol symbol = new CircleSymbol();
		symbol.setBorderColor(Color.gray);
		symbol.setSize(4);
		PlotRun run = new PlotRun();
		for (int i = 0; i < alY.size(); i++) {
			for (int j = 0; j < alX.size(); j++) {
				run.add(new PlotDatum(xArr[i][j], yArr[i][j], false, symbol));
			}
		}

		// Add this new run of points to the plot.
		//aPlot.getRuns().add(run);

		// Now proceed with creating the plot window.
		PlotPanel panel = new PlotPanel(aPlot);
		panel.setBackground(Color.white);
		PlotExample window = new PlotExample(title, panel);
		window.setSize(500, 300);
		window.setLocation(100, 100);
		window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		window.show();
	}

}
