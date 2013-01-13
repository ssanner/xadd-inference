package xadd;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JToolBar;

import net.ericaro.surfaceplotter.JSurfacePanel;
import net.ericaro.surfaceplotter.surface.ArraySurfaceModel;

import camdp.CAMDP.FileOptions;

// TODO: Check for reading of XADDs in Tree LISP-like format
// TODO: Provide option for displaying XADD graphs
public class XADDViewer {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		if (args.length == 0 || args.length > 2)
			Exit("\nUsage: xadd-file1 [xadd-file2]");
		
		XADD xadd_context = new XADD();
		int xadd1 = xadd_context.importXADDFromFile(args[0]);
		int xadd2 = -1;
		if (args.length > 1) 
			xadd2 = xadd_context.importXADDFromFile(args[1]);
		
		// First check for 3D FileOptions 
		FileOptions opt = null;
		String file3dopts = args[0] + ".3d";
		File file3d = new File(file3dopts);
		if (file3d.exists())
			opt = new FileOptions(file3dopts);
		else
			Exit("\nERROR: expected " + file3dopts + " with plot information");

		if (opt == null) {
		}
		
		// TODO: enable dual plot view
		Plot3DSurfXADD(xadd_context, xadd1, xadd2, 
				opt._varLB.get(0), opt._varInc.get(0), opt._varUB.get(0), 
				opt._varLB.get(1), opt._varInc.get(1), opt._varUB.get(1), 
				opt._bassign, opt._dassign, opt._var.get(0), opt._var.get(1), 
				args[0] + ((args.length > 1) ? " and " + args[1] : ""), args[0]);

	}

	public static void Exit(String msg) {
		System.out.println(msg);
		System.exit(1);
	}

	public static void Plot3DSurfXADD (XADD context, int xadd1, int xadd2, 
			double low_x, double inc_x, double high_x, double low_y, double inc_y, double high_y,
			HashMap<String, Boolean> static_bvars, HashMap<String, Double> static_dvars,
			String xVar, String yVar, String title, String filename) {
		int sampleX= (int) Math.round( (high_x-low_x)/inc_x);
		int sampleY= (int) Math.round( (high_y-low_y)/inc_y);
		if (sampleX != sampleY){
			System.out.println("samples X and Y must be equal for Surface Plot, using X samples");
		}
		Plot3DSurfXADD ( context, xadd1, xadd2, low_x, high_x, low_y, high_y, (int) Math.ceil( (high_x-low_x)/inc_x),
			static_bvars, static_dvars,	xVar, yVar, title, filename); 
	}

	public static void Plot3DSurfXADD (XADD context, int xadd1, int xadd2, 
			double low_x, double high_x, double low_y, double high_y, int nSamples,
			HashMap<String, Boolean> static_bvars, HashMap<String, Double> static_dvars,
			String xVar, String yVar, String title, String filename) {
		
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
		
		static_dvars = new HashMap<String, Double>(static_dvars);
		float[][] xArr = new float[alY.size()][alX.size()];
		float[][] yArr = new float[alY.size()][alX.size()];
		float[][] zArr1 = new float[alY.size()][alX.size()];
		float[][] zArr2 = new float[alY.size()][alX.size()];
		for (int i = 0; i < alY.size(); i++) {
			for (int j = 0; j < alX.size(); j++) {

				float x = alX.get(j);
				float y = alY.get(i);

				static_dvars.put(xVar, (double)x);
				static_dvars.put(yVar, (double)y);
				float z1 = context.evaluate(xadd1, static_bvars, static_dvars).floatValue();
				if (Float.isInfinite(z1)) z1=Float.NaN;
				float z2 = 0;
				if (xadd2 != -1) {
					z2 = context.evaluate(xadd2, static_bvars, static_dvars).floatValue();
					if (Float.isInfinite(z2)) z2=Float.NaN;
				}
				static_dvars.remove(xVar);
				static_dvars.remove(yVar);

				//if (z > 0.1d)
				//	System.out.println("f(" + x + "," + y + ") = " + z);
				xArr[i][j] = x;
				yArr[i][j] = y;
				zArr1[i][j] = z1;
				zArr2[i][j] = z2;
			}
		}
		
		// Create a Simple 2D XY plot window.
		JSurfacePanel jsp = new JSurfacePanel();
		jsp.setTitleText(title);

		JFrame jf = new JFrame(title);
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		ArraySurfaceModel sm = new ArraySurfaceModel();
		sm.setValues(xArr[0][0],xArr[alY.size()-1][alX.size()-1],
				yArr[0][0],yArr[alY.size()-1][alX.size()-1],alX.size(), zArr1, 
				(xadd2 != -1 ? zArr2 : null));
		sm.setDisplayXY(true);
		sm.setDisplayZ(true);
		sm.setDisplayGrids(true);
		sm.setBoxed(true);
		if (xadd2 != -1)
			sm.setBothFunction(true);
		jsp.setModel(sm);

		// Add export button
		JToolBar toolbar = new JToolBar();
		JButton button = new JButton();
		button.setText("Re-export SVG and PNG to " + filename.replace(".txt", "") + "{.png,.svg}");
		button.addActionListener(new XADDUtils.ExportActionListener(jsp, filename));
		toolbar.add(button);
		
		jf.getContentPane().add(toolbar, BorderLayout.NORTH);
		jf.getContentPane().add(jsp, BorderLayout.CENTER);
		jf.getContentPane().setPreferredSize(new Dimension(580, 560));
		jf.pack();
		jf.setVisible(true);

		// Export svg
		XADDUtils.ExportSurfPaneltoSVG(jsp, filename + ".svg");
		
		// Export png (not publication quality, but more quickly viewed)
		XADDUtils.ExportPanelToPNG(jsp, filename + ".png");
	}
}
