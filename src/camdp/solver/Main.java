package camdp.solver;

import java.io.IOException;

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
    private static double APPROX = 0d;
    private static String[] solvers = {"Invalid","VI","CRTDP","RTSDP","Aprox VI", "Approx RTSDP"};// 1 -> Value Iteration 2-> RTDP 3-> RTDPFH, Run all and Save All
    
    // Results Configurations
    private static final boolean SAVE_RESULTS = true;
    private static boolean PRINT_RESULTS = true;
    
    public static void Usage() {
        System.out.println("\nUsage: MDP-filename #solver #iter #Ddisplay(2or3) #trials [VERBOSE(int) SHOW_PLOT?(boolean) Approx]");
        System.exit(1);
    }

    public static void main(String args[]) {
        int nargs = args.length;
        if (nargs < 5 || nargs >8) {
            Usage();
        }
        
        // Parse problem filename
        String filename = args[0];

        // Parse integers
        int solution = -1;
        int iter = -1;
        int display = -1;
        int trials = -1;
        try {
            solution = Integer.parseInt(args[1]); 
            iter = Integer.parseInt(args[2]);
            display = Integer.parseInt(args[3]);
            trials = Integer.parseInt(args[4]);
        } catch (NumberFormatException nfe) {
            System.err.println("\nIllegal integer parameters \n");
            Usage();
        }

        if (args.length > 5) { VERBOSE=Integer.parseInt(args[5]);
        if (args.length > 6) { CAMDPsolver.debugSetUp(VERBOSE, Boolean.parseBoolean(args[6]));
        if (args.length > 7) { APPROX = Double.parseDouble(args[7]);} } }
        
        if (VERBOSE >=0) {
            System.out.println("Main Solution Start: Solving "+filename+" with solver "+solvers[solution]+" for "+iter+" iterations and "+trials+" trials.");
            if (APPROX >0) System.out.println("Approximation Allowed Error: "+APPROX); 
        }
        
        // Build a CAMDP, display, solve
        CAMDP mdp = new CAMDP(filename);
        
        mdp.DISPLAY_2D = (display == 2);
        mdp.DISPLAY_3D = (display == 3);
        
        
        
        
        if (VERBOSE > 2){
            System.out.println(mdp.toString(false, false));
        }
        CAMDPsolver solver;
        int used = -1;
        switch (solution){
        case 1:
             solver = new VI(mdp, iter);
            used = solver.solve();
            if (SAVE_RESULTS) solver.saveResults();
            if (PRINT_RESULTS) solver.printResults();
            if (VERBOSE > 0) System.out.println("\nValue Iteration Solution complete, required " + used + " / " + iter + " iterations.");
            break;
             
//        case 2:
//            checkInitialS(mdp);
//            solver = new CRTDP(mdp, trials, iter);
//            used = solver.solve();
//            if (SAVE_RESULTS) solver.saveResults();
//            if (PRINT_RESULTS) solver.printResults();
//            if (VERBOSE > 0) System.out.println("\nContinuous RTDP Solution complete, " + used + " trials of depth " + iter + ".");
//            break;

        case 3:
            checkInitialS(mdp);
            solver = new CRTDPFH(mdp, trials, iter);
            used = solver.solve();
            if (SAVE_RESULTS) solver.saveResults();
            if (PRINT_RESULTS) solver.printResults();
            if (VERBOSE > 0) System.out.println("\nRTSDP solution complete, " + used + " trials of depth " + iter + ".");
            break;            

        case 4:
            solver = new VI(mdp, iter, APPROX);
            used = solver.solve();
            if (SAVE_RESULTS) solver.saveResults();
            if (PRINT_RESULTS) solver.printResults();
            if (VERBOSE > 0) System.out.println("\nApproximate Value Iteration Solution complete, required " + used + " / " + iter + " iterations.");
            break;

        case 5:
            checkInitialS(mdp);
            solver = new CRTDPFH(mdp, trials, iter, APPROX);
            used = solver.solve();
            if (SAVE_RESULTS) solver.saveResults();
            if (PRINT_RESULTS) solver.printResults();
            if (VERBOSE > 0) System.out.println("\nARTSDP solution complete, " + used + " trials of depth " + iter + ".");
            break;            

            
    default:
            System.err.println("\nInvalid Solution Method!");
            System.exit(1);
        }
        
        if (VERBOSE >= 0) System.out.println("MAIN-FINISH");
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