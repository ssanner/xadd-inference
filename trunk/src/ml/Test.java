package ml;

import java.text.DecimalFormat;


/**
 * Automatically generate data, run Loss01Solver on them, then visualize result.
 * @author Tan T. Nguyen
 * 
 */
public class Test {
    
	public static void main(String[] args) {
		String dataFileName = "./src/ml/data_test.txt";
		
		// write 10 2D data points to dataFileName
		DataGenerator dg = new DataGenerator(16, 2, dataFileName);
		if (dg.dataGenerated()) 
			System.out.println("Generated " + dg.nRows() + " data points to: " + dg.fileName());
		
		// run Loss01Solver
		Loss01Solver ls = new Loss01Solver(dataFileName);
		if (!ls.hasSolution())
			System.out.println("Loss01Solver couldn't solve the problem.");
		else {
			double[] w = ls.getSolution();
			ls.showSolution();
			
			// visualize the result if 2D data
			Visualizer viz = new Visualizer(dataFileName, 0, 1, w);
			viz.pack();
			viz.setVisible(true);
			
		}	
	}

}
