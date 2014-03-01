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
    
	//Running Configurations
	private static int VERBOSE = 1;
    private static int SOLUTION = 3; // 1 -> Value Iteration 2-> RTDP 3-> RTDPFH
    private static int NTRIALS = 4; // For RTDP and RTDPFH solution
    
    // Results Configurations
    private static final boolean SAVE_RESULTS = false;
	private static final boolean PRINT_RESULTS = true;
	private static final boolean COMPARE_RESULTS = false;
	
    
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
            System.err.println("\nIllegal iteration value\n");
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
        
        if (VERBOSE >0) System.out.println("Main Solution Start:");
        if (VERBOSE > 1){
            System.out.println(mdp.toString(false, false));
        }
        
        switch (SOLUTION){
        case 1:
            CAMDPsolver solver = new VI(mdp, iter);
            int used = solver.solve();
            if (SAVE_RESULTS) solver.saveResults();
            if (PRINT_RESULTS) solver.printResults();
            if (VERBOSE > 0) System.out.println("\nValue Iteration Solution complete, required " + used + " / " + iter + " iterations.");
            break;
             
        case 2:
            checkInitialS(mdp);
            solver = new CRTDP(mdp, NTRIALS, iter);
            used = solver.solve();
            if (SAVE_RESULTS) solver.saveResults();
            if (PRINT_RESULTS) solver.printResults();


            if (VERBOSE > 0) System.out.println("\nContinuous RTDP Solution complete, " + used + " trials of depth " + iter + ".");
            break;
//        case 3:
//            checkInitialS(mdp);
//            CAMDPsolver solver1 = new VI(mdp,iter);
//            int used1 = solver1.solve();
//            CAMDPsolver solver2 = new CRTDP(mdp, NTRIALS, iter);
//            int used2 = solver2.solve();
//            if (SAVE_RESULTS){
//            	solver1.saveResults();
//            	solver2.saveResults();
//            }
//            if (PRINT_RESULTS){
//            	solver1.printResults();
//            	solver2.printResults();
//            }
//            if (COMPARE_RESULTS){}
//            if (VERBOSE > 0) {
//            	System.out.println("Compare Solution complete, VI required " + used1 + " / " + iter + " iterations.");
//            	System.out.println("Continuous RTDP required " + used2 + " trials of depth " + iter + ".");
//            }
//            break;
        case 3:
            checkInitialS(mdp);
            solver = new CRTDPFH(mdp,NTRIALS, iter);
            used = solver.solve();
            if (SAVE_RESULTS) solver.saveResults();
            if (PRINT_RESULTS) solver.printResults();
            if (VERBOSE > 0) System.out.println("\nContinuous RTDPFH Solution complete, " + used + " trials of depth " + iter + ".");
            break;            
            
    default:
            System.err.println("\n Invalid Solution Method!");
            System.exit(1);
        }
        
        if (VERBOSE >0) System.out.println("MAIN-FINISH");
    }

    private static void checkInitialS(CAMDP mdp) {
        if (!mdp._hsBoolIVars.isEmpty() || !mdp._hsContIVars.isEmpty() || !mdp._hsNoiseVars.isEmpty()){
            System.err.println("CAMDP Solver Main Fail: RTDP Solution incompatible with intermediate and noise Vars!");
            System.exit(1);
        }
        if (mdp._initialS == null){
        	System.err.println("CAMDP Solver Main Fail: RTDP Solution incompatible with undefined initial state.");
            System.exit(1);
        }
    }
}