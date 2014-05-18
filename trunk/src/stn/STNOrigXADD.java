package stn;

import graph.Graph;

import java.util.*;

import camdp.CAction;
import camdp.HierarchicalParser;

import util.DevNullPrintStream;

import xaddorig.*;
import xaddorig.XADD.*;

public class STNOrigXADD {

	public XADD _context = null;
	public HashMap<Integer,String> _hmFactor2Name = null;
	
	public Integer _obj = null;
    ArrayList<Integer> _alObjFactors  = null;
    ArrayList<Integer> _alConsFactors = null;
    ArrayList<Integer> _alAllFactors  = null;
	    
    // Add preferences: values for each type of job... see Peng paper
    // Add disjunction: job2 or job3... need a boolean for this (track assignment)
    //
    // Add in VE
    // Add in maintenance of maximizing assignment
    // Fix infinity detection in computeTermNode (-Inf as well)
    public STNOrigXADD() throws Exception {
    	
        _context = new XADD();
        _hmFactor2Name = new HashMap<Integer,String>();

        _alObjFactors  = new ArrayList<Integer>();
        _alConsFactors = new ArrayList<Integer>();
        _alAllFactors  = new ArrayList<Integer>();
    }
    
    public void initObjective() {

    	_obj = null;
    	
        for (Integer f : _alObjFactors)
        	_obj = (_obj == null ? f : addInObjective(_obj, f));
        
        for (Integer f : _alConsFactors)
        	_obj = addInObjective(_obj, f);

    }
    
    public void postBoundCons(String j1, int lb, int ub) {
    	int cons = ParseXADDString(_context, "([" + j1 + " >= " + lb + "] ([" + j1 + " <= " + ub + "] ([0]) ([Infinity])) ([Infinity]))");
    	_context.getGraph(cons).launchViewer("Bound Constraint Pre-ReduceLP");
    	cons = _context.reduceLP(cons);
    	_hmFactor2Name.put(cons, "bound cons:\n" + j1 + " in [ " + lb + ", " +  ub + " ]");
    	_alConsFactors.add(cons);
    	_alAllFactors.add(cons);
        _context.getGraph(cons).launchViewer("Bound Constraint Post-ReduceLP");
    }
    
    public void postStartAfterGapCons(String j1, String j2, int gap) {
    	int cons = ParseXADDString(_context, "([" + j2 + " > " + j1 + " + " + gap + "] ([0]) ([Infinity]))");
    	cons = _context.reduceLP(cons);
    	_hmFactor2Name.put(cons, "start after cons:\n" + j2 + " > " + j1 + " + " + gap);
        _alConsFactors.add(cons);
    	_alAllFactors.add(cons);
        //_context.getGraph(cons).launchViewer("Start-after Gap Constraint");
    }
    
    public void postDisjCons(String j1, int lb1, int ub1, int lb2, int ub2) {
    	int cons1 = ParseXADDString(_context, "([" + j1 + " >= " + lb1 + "] ([" + j1 + " <= " + ub1 + "] ([0]) ([Infinity])) ([Infinity]))");
    	int cons2 = ParseXADDString(_context, "([" + j1 + " >= " + lb2 + "] ([" + j1 + " <= " + ub2 + "] ([0]) ([Infinity])) ([Infinity]))");
    	int bindex = _context.getVarIndex(_context.new BoolDec("b" + j1), true);
    	int dcons = _context.getINode(bindex, cons1, cons2);
    	dcons = _context.reduceLP(dcons);
    	
    	_hmFactor2Name.put(dcons, "disj cons:\n" + j1 + " in [ " + lb1 + ", " +  ub1 + " ] OR [" + j1 + " in [ " + lb2 + ", " +  ub2 + " ]");
    	_alConsFactors.add(dcons);
    	_alAllFactors.add(dcons);
       _context.getGraph(dcons).launchViewer("Disjunctive Constraint");
    }
    
    public void postPrefCons(String j1, int time, double before, double after) {
    	int cons = ParseXADDString(_context, "([" + j1 + " <= " + time + "] ([" + before + "]) ([" + after + "]))");
    	cons = _context.reduceLP(cons);
    	_hmFactor2Name.put(cons, "pref cons:\n" + j1 + " <= " + time + " ? " + before + " : " + after);
    	_alConsFactors.add(cons);
    	_alAllFactors.add(cons);
        //_context.getGraph(cons).launchViewer("Bound Constraint");
    }
    
    public void postObjective(List<String> jobs) {
    	//int obj = _context.ZERO;
    	//StringBuilder sobj = new StringBuilder();
    	for (String j : jobs) {
    		int summand = ParseXADDString(_context, "([" + j + "])");
    		//obj = _context.apply(obj, summand, XADD.SUM);
    		//if (sobj.length() != 0)
    		//	sobj.append(" + ");
    		//sobj.append(j);
        	summand = _context.reduceLP(summand);
        	_hmFactor2Name.put(summand, "obj: +" + j);
        	_alObjFactors.add(summand);
          	_alAllFactors.add(summand);       		
    	}
//    	_hmFactor2Name.put(obj, "obj:\n" + sobj.toString());
//        _alObjFactors.add(obj);
//        _alAllFactors.add(obj);
        //_context.getGraph(obj).launchViewer("Objective");
    }
    
    public int addInObjective(int obj, int cons) {
    	obj = _context.apply(obj, cons, XADD.SUM);
    	obj = _context.reduceLP(obj);
    	return obj;
    }
    
//    public int maxInConstraint(int obj, int cons) {
//    	return _context.apply(obj, cons, XADD.MAX);
//    }
    
    public int minOutCVar(int obj, String cvar, double lb, double ub) {
    	System.out.println("Min does not exist in XADDOrig");
    	System.exit(1);
    	return -1;
//        XADDLeafMinOrMax min = _context.new XADDLeafMinOrMax(cvar, lb, ub, false /* is_max */, new DevNullPrintStream());
//        _context.reduceProcessXADDLeaf(obj, min, true);
//        int result = _context.reduceLP(min._runningResult);
//        return result;
    }

    public int minOutBVar(int obj, String bvar) {

    	// Following is a safer way to marginalize (instead of using opOut
        // based on apply) in the event that two branches of a boolean variable
        // had equal probability and were collapsed.
    	int var_id = _context.getVarIndex(_context.new BoolDec(bvar), false);
        int restrict_high = _context.opOut(obj, var_id, XADD.RESTRICT_HIGH);
        int restrict_low = _context.opOut(obj, var_id, XADD.RESTRICT_LOW);
        int result = _context.apply(restrict_high, restrict_low, XADD.MIN);
        result = _context.reduceLP(result);
        return result;
    }
    
    public Graph getConstraintGraph(ArrayList<Integer> factors) {
    	
        Graph g = new Graph(/*directed*/false, false, true, false);

        for (Integer f : factors) {
        	HashSet<String> vars = _context.collectVars(f);
        	g.addBiLinks(f.toString(), vars);
        	for (String v : vars) {
        		g.addNodeColor(v, "lightblue");
            	g.addNodeStyle(v, "filled");
            	g.addNodeShape(v, "ellipse");
        	}
        	//System.out.println(f.toString() + " <-> " + vars);
        	g.addNodeShape(f.toString(), "box");
        	g.addNodeStyle(f.toString(), "filled");
        	g.addNodeColor(f.toString(), "lightsalmon");
        	String factor_label = _hmFactor2Name.get(f);
        	if (factor_label == null)
        		factor_label = "factor:" + f;
        	g.addNodeLabel(f.toString(), factor_label);
        }
 
    	return g;
    }
    
    public static int ParseXADDString(XADD xadd_context, String s) {
    	ArrayList l = HierarchicalParser.ParseString(s);
        // System.out.println("Parsed: " + l);
        return xadd_context.buildCanonicalXADD((ArrayList) l.get(0));
    }

    // ===============================================================================
    
    public static void main(String[] args) throws Exception {
   		Test1();
    }

    public static void Test1() throws Exception {
    	
    	STNOrigXADD stn = new STNOrigXADD();
    	        
        List<String> jobs = Arrays.asList(new String[] {"t1", "t2", "t3"});
              
        stn.postObjective(jobs);
        
        stn.postBoundCons("t1", 10, 20);
//        stn.postBoundCons("t2", 20, 40);
//        stn.postBoundCons("t3", 0, 70);
//
//        stn.postDisjCons("t1", 10, 20, 50, 60);
//
//        stn.postStartAfterGapCons("t1", "t2", 10);
//        stn.postStartAfterGapCons("t2", "t3", 20);
//        stn.postStartAfterGapCons("t1", "t3", 30);
//        
//        stn.postPrefCons("t1", 20, -20.0, 0.0);
//        
//        stn.getConstraintGraph(stn._alAllFactors).launchViewer("Constraint factor graph");
//
//        stn.initObjective();
//                
//        stn._context.getGraph(stn._obj).launchViewer("Constrained objective");
//        
//        System.out.println("Vars: " + stn._context._alBooleanVars + ", " + stn._context._cvar2ID.keySet() + " == " + stn._context.collectVars(stn._obj));
//        
//		// Maximize objective
//		stn._obj = stn.minOutCVar(stn._obj, "t1", 0d, 60d);
//		stn._context.getGraph(stn._obj).launchViewer("Constrained objective after min-out t1");
//		
//		stn._obj = stn.minOutCVar(stn._obj, "t2", 0d, 60d);
//		stn._context.getGraph(stn._obj).launchViewer("Constrained objective after min-out t1, t2");
//		
//		stn._obj = stn.minOutCVar(stn._obj, "t3", 0d, 60d);
//		stn._context.getGraph(stn._obj).launchViewer("Constrained objective after min-out t1, t2, t3");
//
//		stn._obj = stn.minOutBVar(stn._obj, "bt1");
//		stn._context.getGraph(stn._obj).launchViewer("Constrained objective after min-out t1, t2, t3, bt1");
    }

}
