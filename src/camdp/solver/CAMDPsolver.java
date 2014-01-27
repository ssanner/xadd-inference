package camdp.solver;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import camdp.CAMDP;
import xadd.XADD;

public abstract class CAMDPsolver {
	
	public CAMDP _mdp;
	public XADD _context;
	public Integer _valueDD;
	public String _solveMethod = null;
	
	
	public final static boolean DISPLAY_PREMAX_Q = false;
	public final static boolean DISPLAY_POSTMAX_Q = false;
	public final static boolean DISPLAY_V = true;
	public final static boolean DISPLAY_MAX = false;
	
	/* Cache maintenance */
	public final static boolean ALWAYS_FLUSH = false; // Always flush DD caches?
	public final static double FLUSH_PERCENT_MINIMUM = 0.3d; // Won't flush until < amt
	
	/* Approximation Parameters */
	public double APPROX_ERROR = 0.0d;
	public boolean APPROX_ALWAYS = false;
	public boolean APPROX_PRUNING = false;
	public boolean COMPARE_OPTIMAL = false;
	
	public void setApproxTest(double eps, PrintStream log, boolean always) {
		APPROX_ERROR = eps;
		_resultStream = log;
		APPROX_ALWAYS = always;
		COMPARE_OPTIMAL = true;
	}

	
	/* For printing */
	public final static String RESULTS_DIR = "results"; // Diagnostic output destination
	
	public static DecimalFormat _df = new DecimalFormat("#.########");
	public PrintStream _logStream = null;
	public PrintStream _resultStream = null;	
	public final static String ASCII_BAR = "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%"; // Display shortcut
	
	/* *********************** Methods *********************** */
	
	public int solve(int max_iter){
		System.out.println("Abstract CAMDP solver.");
		return 0;
	}
	
	public void flushCaches() { }
    public void flushCaches(List<Integer> special_nodes) {}
    
    public void makeResultStream(){
    	try{
    		System.out.println("testing filename:" + _mdp._problemFile);
    		_resultStream = new PrintStream(new FileOutputStream(RESULTS_DIR+_mdp._problemFile + _solveMethod + ".rslt"));
    	}
    	catch (FileNotFoundException e){
    		System.err.println("Couldn't create result Stream for: "+RESULTS_DIR+_mdp._problemFile + ".rslt\nException:"+e);
    	}
    }
    
	public String makeXADDLabel(String xadd, int iter, double approx)
	{
		return  xadd+"^"+iter+"-"+String.format("%03d",Math.round(1000*approx));
	}

    public String makeXADDLabel(String xadd, int trial, int depth, double approx)
	{
		return  xadd+" T"+trial+"d"+depth+" -a"+String.format("%03d",Math.round(1000*approx));
	}


}