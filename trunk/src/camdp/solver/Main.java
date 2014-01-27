package camdp.solver;

import camdp.CAMDP;

/**
 * Main Executable class for Continuous State and Action MDP (CAMDP) 
 * 
 * @version 1.0
 * @author Luis Vianna
 * @author Zahra Zamani
 * @author Scott Sanner
 * @language Java (JDK 1.5)
 **/

public class Main {
	
	private static int VERBOSE = 1;
	private static int SOLUTION = 2; // 1 -> Value Iteration 2-> RTDP
	private static int NTRIALS = 30; // For RTDP solution
	
	public static void Usage() {
		System.out.println("\nUsage: MDP-filename #iter display-2D? display-3D? [dApproxPrune]");
		System.exit(1);
	}

	public static void main(String args[]) {
		if (args.length < 4 || args.length >5) {
			Usage();
		}
		
		// Parse problem filename
		String filename = args[0];

		// Parse iterations
		int iter = -1;
		try {
			iter = Integer.parseInt(args[1]);
		} catch (NumberFormatException nfe) {
			System.out.println("\nIllegal iteration value\n");
			Usage();
		}

		// Build a CAMDP, display, solve
		CAMDP mdp = new CAMDP(filename);
		mdp.DISPLAY_2D = Boolean.parseBoolean(args[2]);
		mdp.DISPLAY_3D = Boolean.parseBoolean(args[3]);
		
		//optional argument modifies 
		if (args.length == 5){
			mdp.APPROX_ERROR = Double.parseDouble(args[4]);
		}
		
		System.out.println("Main Solution Start:");
		if (VERBOSE > 1){
			System.out.println(mdp.toString(false, false));
		}
		
		CAMDPsolver solver = null;
		switch (SOLUTION){
		case 1:
			 solver = new VI(mdp);
			 break;
		case 2:
			if (!mdp._hsBoolIVars.isEmpty() || !mdp._hsContIVars.isEmpty() || !mdp._hsNoiseVars.isEmpty()){
				System.err.println("CAMDP Main Fail: RTDP Solution incompatible with intermediate and noise Vars!");
				System.exit(1);
			}
			solver = new CRTDP(mdp,NTRIALS);
			break;
		default:
			System.out.println("\n Invalid Solution Method!");
			System.exit(1);
		}
		int used = solver.solve(iter);
		
		if (VERBOSE > 0){
			System.out.println("\nSolution complete, required " + 
					used + " / " + iter + " iterations or trials.");
			if (VERBOSE > 1){
				System.out.println("Cache Sizes before Flush: ");
				mdp._context.showCacheSize();
				mdp.flushCaches(true);
				System.out.println("Cache Sizes after Flush:");
				mdp._context.showCacheSize();
			}
		}
		System.out.println("MAIN-FINISH");
	}
}