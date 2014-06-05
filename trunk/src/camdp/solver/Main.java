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
    private static String[] solvers = {"Invalid","VI","CRTDP","RTDPFH","VI, CRTDP & CRTDPFH"};// 1 -> Value Iteration 2-> RTDP 3-> RTDPFH, Run all and Save All
    private static int TRIALS_DEFAULT = 15; // For RTDP and RTDPFH solution
    
    // Results Configurations
    private static final boolean SAVE_RESULTS = true;
    private static final boolean PRINT_RESULTS = true;
    
    public static void Usage() {
        System.out.println("\nUsage: MDP-filename #solver #iter #Ddisplay(2or3) #trials [VERBOSE]");
        System.exit(1);
    }

    public static void main(String args[]) {
        int nargs = args.length;
        if (nargs < 5 || nargs >6) {
            Usage();
        }
        
        // Parse problem filename
        String filename = args[0];

        // Parse integers
        int solution = -1;
        int iter = -1;
        int display = -1;
        int trials = TRIALS_DEFAULT;
        try {
            solution = Integer.parseInt(args[1]); 
            iter = Integer.parseInt(args[2]);
            display = Integer.parseInt(args[3]);
            trials = Integer.parseInt(args[4]);
        } catch (NumberFormatException nfe) {
            System.err.println("\nIllegal integer parameters \n");
            Usage();
        }

        // Build a CAMDP, display, solve
        CAMDP mdp = new CAMDP(filename);
        
        mdp.DISPLAY_2D = (display == 2);
        mdp.DISPLAY_3D = (display == 3);
        
        //optional argument modifies 
        //if (args.length > 5){
        //    mdp.APPROX_ERROR = Double.parseDouble(args[5]);
        //}
        
        if (args.length > 5){
            VERBOSE=Integer.parseInt(args[5]);
            CAMDPsolver.debugSetUp(VERBOSE);
        }
        
        
        if (VERBOSE >=0) {
            System.out.println("Main Solution Start: Solving "+filename+" with solving option "+solvers[solution]+" for "+iter+" iterations and "+trials+" trials.");
        }
        if (VERBOSE > 0){
            System.out.println(mdp.toString(false, false));
        }
        
        switch (solution){
        case 1:
            CAMDPsolver solver = new VI(mdp, iter);
            int used = solver.solve();
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
            if (VERBOSE > 0) System.out.println("\nContinuous RTDPFH Solution complete, " + used + " trials of depth " + iter + ".");
            break;            

        case 4:
            solver = new VI(mdp, iter);
            int used1 = solver.solve();
            if (SAVE_RESULTS) solver.saveResults();
            if (PRINT_RESULTS) solver.printResults();
            System.out.println();
            
//            System.out.println("RTDP");
//            checkInitialS(mdp);
//            solver = new CRTDP(mdp, trials, iter);
//            int used2 = solver.solve();
//            if (SAVE_RESULTS) solver.saveResults();
//            if (PRINT_RESULTS) solver.printResults();
//            System.out.println();
            
            System.out.println("RTDPFH");
            solver = new CRTDPFH(mdp, trials, iter);
            int used3 = solver.solve();
            if (SAVE_RESULTS) solver.saveResults();
            if (PRINT_RESULTS) solver.printResults();
            if (VERBOSE > 0) System.out.println("\nTriple solution complete, VI used "+used1+ ", RTDPFH used "+used3);
            break;            
            
    default:
            System.err.println("\n Invalid Solution Method!");
            System.exit(1);
        }
        
//        try {
//            System.in.read();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
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