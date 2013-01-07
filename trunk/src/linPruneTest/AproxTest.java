package linPruneTest;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

import cmdp.CMDP;
import camdp.CAMDP;

public class AproxTest {

	public static PrintStream makeLog(String filename, double error, String dir) {
		String []f1 = filename.split("\\.");
		String []longname = (f1[0]).split("/");
		String name = longname[longname.length-1];
		String approx = String.format("%02d", Math.round(100*error) );
		String logName = dir+"/"+name+"_"+approx+".log";
		try{
			return new PrintStream(new FileOutputStream(logName));
		} catch (Exception e){
			System.err.println("Could not create Log '" + logName +"' in '" + dir + "' to produce output files.");
			System.exit(1);
		} return null;
	}
	
	public static void usage(){
		System.out.println("Usage: 5 or 6 params:\n AproxTest filename logdir isCAMDP? iter Napprox ApproxStepSize\n");
		System.exit(1);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length < 5 || args.length > 7) {
			usage();
		}

		// Parse problem filename
		String filename = args[0];
		String resultsDir = args[1];
		boolean printToFile = true; //log format: iter, nodes, branches, time, max, totTime, Mem
		if (resultsDir.toUpperCase().compareTo("STD_OUT") == 0 ) {
			printToFile = false;
		}
		PrintStream out = System.out;
		
		//Parse CAMDP
		boolean isCAMDP = Boolean.parseBoolean(args[2]);
		boolean valuePlot = false;
		// Parse iterations
		int iter = -1;
		int nApproxSteps = -1;
		double approxStepSize = 0.1d;
		try {
			iter = Integer.parseInt(args[3]);
			nApproxSteps = Integer.parseInt(args[4]);
			if (args.length >5) approxStepSize = Double.parseDouble(args[5]);
			if (args.length >6) valuePlot = (Integer.parseInt(args[6]))>0;
			if ((nApproxSteps) * approxStepSize > 1) System.err.format("\nIllegal approx step %f or number %d \n",
														approxStepSize, nApproxSteps);;
		} catch (NumberFormatException nfe) {
				System.err.println("\nIllegal iteration or step values\n");
				System.exit(1);
		}
		
		//Solve with different approximation levels a CAMDP
		if (isCAMDP){
			//Build a CAMDP, display, solve
			CAMDP camdp = new CAMDP(filename);
			camdp.DISPLAY_2D = valuePlot;
			camdp.DISPLAY_3D = false;
			
			for(int approx=0; approx<=nApproxSteps;approx++){
				double error = approx*approxStepSize;
				System.out.format("Solving %s with %d iter, %f error\n",filename,iter,error);
				if (printToFile) out = makeLog(filename, error, resultsDir);
				camdp.setApproxTest(error, out, false);
				int iter_used = camdp.solve(iter);
				camdp.flushCaches(true);
			}
		}
		
		//Solve with different approximation levels a CMDP
		else{
			// Build a CMDP
			CMDP cmdp = new CMDP(filename);
	
			for(int approx=0; approx<=nApproxSteps;approx++){
				double error = approx*approxStepSize;
				System.out.format("Solving %s with %d iter, %f error\n",filename,iter,error);
				if (printToFile) out = makeLog(filename, error, resultsDir);
				cmdp.setApproxTest(error, out, false);
				int iter_used = cmdp.solve(iter);
				cmdp.flushCaches(true);
			}
		}
		System.out.println("Approx Test, Over!");
		System.exit(0);
	}
}
