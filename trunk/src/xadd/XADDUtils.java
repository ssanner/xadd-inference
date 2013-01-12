package xadd;

import jahuwaldt.plot.CircleSymbol;
import jahuwaldt.plot.ContourPlot;
import jahuwaldt.plot.Plot2D;
import jahuwaldt.plot.PlotDatum;
import jahuwaldt.plot.PlotPanel;
import jahuwaldt.plot.PlotRun;
import jahuwaldt.plot.PlotRunList;
import jahuwaldt.plot.PlotSymbol;
import jahuwaldt.plot.SimplePlotXY;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.WindowConstants;

import net.ericaro.surfaceplotter.JSurfacePanel;
import net.ericaro.surfaceplotter.surface.ArraySurfaceModel;

import plot.PlotExample;
import util.DevNullPrintStream;

public class XADDUtils {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		XADD xadd_context = new XADD();
		// Put all boolean variables first to avoid reordering clashes
		xadd_context.getVarIndex(xadd_context.new BoolDec("f"), true);
		xadd_context.getVarIndex(xadd_context.new BoolDec("g"), true);
		xadd_context.getVarIndex(xadd_context.new BoolDec("h"), true);

		// Setup static vars
		HashMap<String, Double> dvars = new HashMap<String, Double>();
		dvars.put("k", 12d);
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
		//int xadd2u = TestXADD.TestBuild(xadd_context,
		//		"./src/xadd/triangle_mix.xadd");

		// 2D
		PlotXADD(xadd_context, xadd2u, -20d, 0.25d, 20d, bvars, dvars, "y",
				"Uniform Mix");
		
		// 3D Contour
		Plot3DXADD(xadd_context, xadd2u, -20d, 0.25d, 20d, -20d, 0.25d, 20d,
				bvars, dvars, "y", "x1",
				"Uniform Mix Contour");
		
//		// 3D Surface by specific grids -- for compatibility with Contour plot
//		Plot3DSurfXADD(xadd_context, xadd2u, -20d, 0.25d, 20d, -20d, 0.25d, 20d, 
//				bvars, dvars, "y", "x1",
//				"Uniform Mix Surface");
		
		// Or by number of samples per dimension (since must match)
		Plot3DSurfXADD(xadd_context, xadd2u, -20d, 20d, -20d, 20d, 40,
				bvars, dvars, "y", "x1",
				"Uniform Mix Surface");

		// Ensure distribution is normalized
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
		int xadd_defint = context.computeDefiniteIntegral(xadd, int_var);
		// System.out.println("Int result: " + context.getString(xadd_int));
		//int xadd_indef = context.reduceProcessXADDLeaf(xadd,
		//		context.new XADDLeafIndefIntegral(int_var), /*canonical_reorder*/false);
		//context.getGraph(xadd_indef).launchViewer();
		context.getGraph(xadd_defint).launchViewer();
		System.out.println("Evaluating @ " + static_bvars + " " + static_dvars);
		return context.evaluate(xadd_defint, static_bvars, static_dvars);
	}

	public static void PlotXADD(XADD context, int xadd, double low, double inc,
			double high, String xVar, String title) {
		PlotXADD(context, xadd, low, inc, high, 
				new HashMap<String, Boolean>(), new HashMap<String, Double>(),
				xVar, title);
	}
	
	public static void PlotXADD(XADD context, int xadd, double low, double inc,
			double high, HashMap<String, Boolean> static_bvars,
			HashMap<String, Double> static_dvars, String xVar, String title) {
		
		PrintStream ps = null;
		String filename = title.replace('^', '_').replace("(", "").replace(")", "").replace(":", "_").replace(" ", "").replace(".dot","") + ".txt";
		title = RemovePathAndExt(title);
		try {
			ps = new PrintStream(new FileOutputStream(filename));
		} catch (Exception e) {
			System.err.println("Could not open " + filename + " for data export.");
			ps = new DevNullPrintStream();
		}
		
		// Create a Simple 2D XY plot window.
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

			ps.println(x + "\t" + y);
			xArr[i] = x;
			yArr[i] = y;
			
		}
		ps.close();
		
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
		
		// Export png (not publication quality, but more quickly viewed)
		ExportPanelToPNG(panel, filename.replace(".txt", ".png"));
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
		
		static_dvars = new HashMap<String, Double>(static_dvars);
		PrintStream ps = null;
		String filename = title.replace('^', '_').replace("(", "").replace(")", "").replace(":", "_").replace(" ", "").replace(".dot","") + ".txt";; 
		title = RemovePathAndExt(title);
		try {
			ps = new PrintStream(new FileOutputStream(filename));
		} catch (Exception e) {
			System.err.println("Could not open " + filename + " for data export.");
			ps = new DevNullPrintStream();
		}

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
		for (int j = 0; j < alX.size(); j++) {
			for (int i = 0; i < alY.size(); i++) {

				double x = alX.get(j);
				double y = alY.get(i);

				static_dvars.put(xVar, x);
				static_dvars.put(yVar, y);
				double z = context.evaluate(xadd, static_bvars, static_dvars);
				if (Double.isInfinite(z)) z=Float.NaN;
				static_dvars.remove(xVar);
				static_dvars.remove(yVar);

				//if (z > 0.1d)
				//	System.out.println("f(" + x + "," + y + ") = " + z);
				ps.println(x + "\t" + y + "\t" + z);
				xArr[i][j] = x;
				yArr[i][j] = y;
				zArr[i][j] = z; //x + y; //z;
			}
			ps.println();
		}
		ps.close();
		
//		for (int i=0;i<alY.size();i++)
//		{
//			for (int j=0;j<alX.size();j++)
//				System.out.print(" " + zArr[i][j]);
//			System.out.println();
//		}
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
		
		// Export png (not publication quality, but more quickly viewed)
		ExportPanelToPNG(panel, filename.replace(".txt", ".png"));
	}
	
	public static void Plot3DSurfXADD(XADD context, int xadd, double low_x,
			double inc_x, double high_x, double low_y, double inc_y,
			double high_y, String xVar, String yVar, String title) {

		Plot3DSurfXADD(context, xadd, low_x, inc_x, high_x, low_y, inc_y, high_y,
				new HashMap<String, Boolean>(), new HashMap<String, Double>(),
				xVar, yVar, title);
	}

	public static void Plot3DSurfXADD (XADD context, int xadd,
			double low_x, double inc_x, double high_x, double low_y, double inc_y, double high_y,
			HashMap<String, Boolean> static_bvars, HashMap<String, Double> static_dvars,
			String xVar, String yVar, String title) {
		int sampleX= (int) Math.round( (high_x-low_x)/inc_x);
		int sampleY= (int) Math.round( (high_y-low_y)/inc_y);
		if (sampleX != sampleY){
			System.out.println("samples X and Y must be equal for Surface Plot, using X samples");
		}
		Plot3DSurfXADD ( context, xadd, low_x, high_x, low_y, high_y, (int) Math.ceil( (high_x-low_x)/inc_x),
			static_bvars, static_dvars,	xVar, yVar, title); 
	}

	public static void Plot3DSurfXADD (XADD context, int xadd,
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
				float z = context.evaluate(xadd, static_bvars, static_dvars).floatValue();
				if (Float.isInfinite(z)) z=Float.NaN;
				static_dvars.remove(xVar);
				static_dvars.remove(yVar);

				//if (z > 0.1d)
				//	System.out.println("f(" + x + "," + y + ") = " + z);
				ps.println(x + "\t" + y + "\t" + z);
				xArr[i][j] = x;
				yArr[i][j] = y;
				zArr[i][j] = z; //x + y; //z;
			}
		}
		ps.close();
		
		// Create a Simple 2D XY plot window.
		JSurfacePanel jsp = new JSurfacePanel();
		jsp.setTitleText(title);

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
		@Override
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
