package ml;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Random;


public class DataGenerator {
	// only generate 2D data
	private int D = 2; 		// dimension of x
	private int N = 100;		// how many data points
	private double x10 = 1.0;	// center x1 of class 0
	private double x20 = 1.0;	// center x2 of class 0
	private double r0 = 3.0;	// radius of class 0
	private double x11 = 8.0;	// center x1 of class 1
	private double x21 = 6.0;	// center x2 of class 1
	private double r1 = 4.0;	// radius of class 1
	private String _fileName = "./src/ml/data3.txt";
	
	// getters
	public int nRows() { return N; }
	public int xDim() { return D; }
	public String fileName() { return _fileName; }
	
	private String dbl2Str(double x) {
    	DecimalFormat df = new DecimalFormat("0.000");
    	String s = (df.format(x));
    	return s;
    }
	
	public DataGenerator() {
		try {
			FileWriter outFile = new FileWriter(_fileName);
			PrintWriter out = new PrintWriter(outFile);
			
			Random rand = new Random();
			for (int i=0; i<N/2; i++) {
				double x1 = x10 + r0 * rand.nextGaussian();
				double x2 = x20 + r0 * rand.nextGaussian();
				out.println(dbl2Str(x1) + "," + dbl2Str(x2) + "," + "0");
			}
			for (int i=N/2; i<N; i++) {
				double x1 = x11 + r1 * rand.nextGaussian();
				double x2 = x21 + r1 * rand.nextGaussian();
				out.println(dbl2Str(x1) + "," + dbl2Str(x2) + "," + "1");
			}
			out.close();
		} 
		catch (IOException e){
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		DataGenerator dg = new DataGenerator();
		System.out.println("Successfully generated " + dg.nRows() + " rows to: " + dg.fileName());
	}

}
