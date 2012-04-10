package ml;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Random;


public class DataGenerator {
	
	private int D; 					// dimension of x: (x1..xD)
	private int N;					// how many data points
	private double radius = 100d; 	// range of data -range, range
	private String fname;
	private boolean generated;
	
	// getters
	public int nRows() { return N; }
	public int xDim() { return D; }
	public String fileName() { return fname; }
	public boolean dataGenerated() { return generated; }
	
	
	private String dbl2Str(double x) {
    	DecimalFormat df = new DecimalFormat("0.00");
    	String s = (df.format(x));
    	return s;
    }
	
	// convert vector x and target y to a string in csv data format
	private String point2Str(double[] x, int y) {
		String s = "";
		for (int j=0; j<x.length; j++) 
			s += dbl2Str(x[j]) + ",";
		s += y;
		return s;
	}
	
	// generate a random vector within radius r from origin
	private double[] randomPoint(double r) {
		Random rand = new Random();
		double[] rp = new double[D];
		for (int j=0; j<D; j++)
			rp[j] = 2* r * (rand.nextDouble() - 0.5);
		return rp;
	}
	
	// generate a random vector within radius from origin
	private double[] randomGaussianPoint(double[] c, double[] r) {
		Random rand = new Random();
		double[] rgp = new double[D];
		for (int j=0; j<D; j++)
			rgp[j] = c[j] + r[j] * rand.nextGaussian();
		return rgp;
	}
	
	public DataGenerator(int nrows, int xvars, String filename) {
		
		N = nrows;
		D = xvars;
		fname = filename;
		double[] c0 = randomPoint(radius/3d); // center class 0
		double[] c1 = randomPoint(radius/3d); // center of class 1
		double[] r0 = randomPoint(radius/6d);	// radius of class 0
		double[] r1 = randomPoint(radius/6d);	// radius of class 1
		
		try {
			FileWriter outFile = new FileWriter(fname);
			PrintWriter out = new PrintWriter(outFile);
			
			for (int i=0; i<N/2; i++) {
				double[] x = randomGaussianPoint(c0, r0);
				out.println(point2Str(x, 0));
			}
			for (int i=N/2; i<N; i++) {
				double[] x = randomGaussianPoint(c1, r1);
				out.println(point2Str(x, 1));
			}
			out.close();
			generated = true;
		} 
		catch (IOException e){
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		DataGenerator dg = new DataGenerator(20, 2, "./src/ml/data_test.txt");
		System.out.println("Successfully generated " + dg.nRows() + " rows to: " + dg.fileName());
		Visualizer viz = new Visualizer("./src/ml/data_test.txt", 0, 1);
		viz.pack();
    	viz.setVisible(true);
	}

}
