package camdp.solver;

import java.io.File;
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
		
	/* Approximation Parameters */
	public double APPROX_ERROR = 0.0d;
	public boolean APPROX_ALWAYS = false;
	public boolean APPROX_PRUNING = false;
	public boolean COMPARE_OPTIMAL = false;
	
	/* For printing */
	public final static String RESULTS_DIR = "./results"; // Diagnostic output destination
	public static DecimalFormat _df = new DecimalFormat("#.########");
	public PrintStream _logStream = null;
	public PrintStream _resultStream = null;	
	public final static String ASCII_BAR = "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%"; // Display shortcut
	
	/* Time & Memory Management */
	public long _lTime = 0;
	
	/*Solution maintenance */
	public ArrayList<Integer> solutionDDList = new ArrayList<Integer>();
	public ArrayList<Long> solutionTimeList = new ArrayList<Long>();
	public ArrayList<Integer> solutionNodeList = new ArrayList<Integer>();
	public ArrayList<Double> solutionInitialSValueList = new ArrayList<Double>();
	public ArrayList<Double> solutionMaxValueList = new ArrayList<Double>();
	
	/* *********************** Methods *********************** */
	public void setupSolution(){
		solutionDDList.add(null);
		solutionTimeList.add(null);
		solutionNodeList.add(null);
		solutionInitialSValueList.add(null);
		solutionMaxValueList.add(null);
	}

	public void setApproxTest(double eps, PrintStream log, boolean always) {
		APPROX_ERROR = eps;
		_resultStream = log;
		APPROX_ALWAYS = always;
		COMPARE_OPTIMAL = true;
	}
	
	public int solve(int max_iter){
		System.out.println("Abstract CAMDP solver.");
		return 0;
	}
	    
    public void makeResultStream(){
		int filenamestart = _mdp._problemFile.lastIndexOf('/');
		String filename = _mdp._problemFile.substring(filenamestart,_mdp._problemFile.length()-5);
		String problemType = _mdp.CONTINUOUS_ACTIONS? "/contact":"/discact"; 
		String dir = RESULTS_DIR + problemType + filename;
		
		//System.out.println("testing filename:" + dir + "/" + _solveMethod + ".rslt");
    	try{
    		new File(dir).mkdirs();
    		_resultStream = new PrintStream(new FileOutputStream(dir + "/" + _solveMethod + ".rslt"));
    	}
    	catch (FileNotFoundException e){
    		System.err.println("Couldn't create result Stream for: "+dir + "/" + _solveMethod + ".rslt\nException:"+e);
    	}
    }
    
    public void printResults(){
    }
    
    public void saveResults(){
    	//Results: N (trial or Iter), Time, Nodes, InitialS Value.
    	for(int i=1; i< solutionDDList.size(); i++){
    		_resultStream.format("%d %d %d %f\n", i, solutionTimeList.get(i), solutionNodeList.get(i), (_mdp._initialS != null) ? solutionInitialSValueList.get(i): "0");
    	}
    }
    
    /////// Time Management utilities ////////////////////// 

	// Reset elapsed time
	public void resetTimer() {
		_lTime = System.currentTimeMillis(); 
	}

	// Get the elapsed time since resetting the timer
	public long getElapsedTime() {
		return System.currentTimeMillis() - _lTime;
	}

	public void flushCaches(){
		flushCaches(new ArrayList<Integer>());
	}
	public void flushCaches(List<Integer> specialNodes){
		ArrayList<Integer> moreSpecialNodes = new ArrayList<Integer>();
		moreSpecialNodes.addAll(specialNodes);
		moreSpecialNodes.add(_valueDD);
		for(int i=1;i<solutionDDList.size();i++) moreSpecialNodes.add(solutionDDList.get(i));
		_mdp.flushCaches(moreSpecialNodes);
	}
}