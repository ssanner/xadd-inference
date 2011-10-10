package xadd;

import jahuwaldt.plot.CircleSymbol;
import jahuwaldt.plot.Log10AxisScale;
import jahuwaldt.plot.Plot2D;
import jahuwaldt.plot.PlotAxis;
import jahuwaldt.plot.PlotPanel;
import jahuwaldt.plot.PlotRun;
import jahuwaldt.plot.PlotRunList;
import jahuwaldt.plot.SimplePlotXY;
import jahuwaldt.plot.SquareSymbol;

import java.awt.Color;
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
	public static void main(String[] args) {

		XADD xadd_context = new XADD();

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
		
		//int xadd1 = XADD.TestBuild(xadd_context, "./src/xadd/test2.xadd");
		int xadd1 = TestXADD.TestBuild(xadd_context, "./src/xadd/normal_mix_simple.xadd");
		xadd_context.getGraph(xadd1).launchViewer();
		plotXADD(xadd_context, xadd1, -10d, 0.25d, 10d, bvars, dvars, "y", "Normal Mix Simple");
		System.out.println("Norm: " + testNormalize(xadd_context, xadd1, bvars, dvars, "y") + "\n\n\n\n\n");
		
		int xadd2 = TestXADD.TestBuild(xadd_context, "./src/xadd/normal_mix.xadd");
		xadd_context.getGraph(xadd2).launchViewer();
		dvars.put("k", 9d);
		dvars.put("x1", 1d);
		plotXADD(xadd_context, xadd2, -20d, 0.25d, 20d, bvars, dvars, "y", "Normal Mix");
		System.out.println("Norm: " + testNormalize(xadd_context, xadd2, bvars, dvars, "y") + "\n\n\n\n\n");
	}

	public static double testNormalize(XADD context, int xadd, 
			HashMap<String, Boolean> static_bvars, 
			HashMap<String, Double> static_dvars,
			String int_var) {
		int xadd_int = context.computeDefiniteIntegral(xadd, int_var);
		System.out.println("Int result: " + context.getString(xadd_int));
		System.out.println("Evaluating @ " + static_bvars + " " + static_dvars);
		return context.evaluate(xadd_int, static_bvars, static_dvars);
	}
	
	public static void plotXADD(XADD context, int xadd, 
			double low, double inc, double high, 
			HashMap<String, Boolean> static_bvars, 
			HashMap<String, Double> static_dvars,
			String xVar, String title) {
		
		//	Create a Simple 2D XY plot window.
		ArrayList<Double> alX = new ArrayList<Double>();
		for (double x = low; x <= high; x += inc)
			alX.add(x);
		
		double[] xArr = new double[alX.size()];
		double[] yArr = new double[alX.size()];
		for (int i = 0; i < alX.size(); i++) {
			double x = alX.get(i);
			static_dvars.put(xVar, x);
			double y = context.evaluate(xadd, static_bvars, static_dvars);
			static_dvars.remove(xVar);
			
			//System.out.println(x + ": " + y);
			xArr[i] = x;
			yArr[i] = y;
		}
			
		Plot2D aPlot = new SimplePlotXY(xArr, yArr, title, 
				xVar + " @ " + static_bvars + " " + static_dvars, "f(" + xVar + ")",
				null, null, null/*new jahuwaldt.plot.CircleSymbol()*//*new XSymbol()*/);

		// Color line red
		PlotRunList runs = aPlot.getRuns();
		PlotRun run = (PlotRun)runs.get(0);
		run.setLineColor(Color.red);

		//	Make the horizontal axis a log axis.
		//PlotAxis xAxis = aPlot.getHorizontalAxis();
		//xAxis.setScale(new Log10AxisScale());
		
		//	Create a 2nd run and add it to the plot.
		//PlotRunList runs = aPlot.getRuns();
		//runs.add(new PlotRun(xArr, yArrAlt, true, new SquareSymbol()));
		
		PlotPanel panel = new PlotPanel(aPlot);
		panel.setBackground( Color.white );
		PlotExample window = new PlotExample(title, panel);
		window.setSize(500, 300);
		window.setLocation(50,50);
		window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		window.show();	
	}
}
