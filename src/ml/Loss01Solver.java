package ml;

import graph.Graph;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import lpsolve.LP;
import lpsolve.LpSolve;
import lpsolve.LpSolveException;

import xadd.XADD;
import xadd.XADD.ArithExpr;
import xadd.XADD.BoolDec;
import xadd.XADD.CompExpr;
import xadd.XADD.Decision;
import xadd.XADD.DeltaFunctionSubstitution;
import xadd.XADD.DoubleExpr;
import xadd.XADD.ExprDec;
import xadd.XADD.TautDec;
import xadd.XADD.VarExpr;
import xadd.XADD.XADDINode;
import xadd.XADD.XADDLeafMax;
import xadd.XADD.XADDNode;
import xadd.XADD.XADDTNode;
import cmdp.HierarchicalParser;

public class Loss01Solver {

	private DataReader dr;	// reads and stores classification data
	private String dataFileName;
	
	private boolean foundSolution;
	private boolean[] c, c_min;	// current/min truth values of optimal solution, for each data point
								// c_min[i] = true, iff sum(x_ij w_ij) * y_i < 0 ( => f_loss(i) = 1)
	private int floss, f_min;	// current/min value of 01 loss function
	
	int nw;					// number of parameters w0 ... w_nw
	private double[] w;		// possible value of w0..wD for optimal solution 

	double[] obj_coef = new double[nw]; // objective coef matrix
	
	private int xadd_id;			// stores the final xadd id 
	private XADD xadd_context;		// and the xadd context

	
	
	// getters
	public boolean hasSolution() { return foundSolution; }
	public double[] getSolution() { 
		if (isValidSolution(w)) return w; 
		return null;
	}
	
	
	
	// helper functions
	
    private String dbl2Str(double x) {
    	DecimalFormat df = new DecimalFormat("0.00");
    	String s = (df.format(x));
    	return s;
    }
    
	private String row2Expr(int i, boolean isTrue) {
		String expr = Double.toString(dr.y(i)) + " * w0 ";
		for (int j=0; j<dr.xDim(); j++) {
			String term = Double.toString(Math.abs(dr.y(i) * dr.x(i, j)));
			term = term + " * w" + (j+1);
			if (dr.y(i) * dr.x(i,j) < 0) term = " - " + term;
			else term = " + " + term;
			expr = expr + term;
		}
		if (isTrue) return "(" + expr + ")" + " < 0";
		else return "(" + expr + ")" + " >= 0";
	}
	
	public void showSolution() {
		
		if (!foundSolution) 
			System.out.println("No solution has been found.");
		else {
			
			System.out.println("Minimal f_loss value: " + f_min + " attained under conditions:");
			for (int i=0; i<dr.nRows(); i++) 
				System.out.println(row2Expr(i, c_min[i]));
		
			if (!isValidSolution(w)) 
				System.out.println("Unable to determine w (unbounded).");
			else {
				System.out.print("Possible solution: ");
				for (int j=0; j<w.length; j++) 
					System.out.print("   w" + j + " = " + dbl2Str(w[j]));
				System.out.println();			
			}
		} 
	}
	
	
	
	
	// functions for building XADD

	public void showXADDGraph() {
		build01LossXADD();
		Graph g1 = xadd_context.getGraph(xadd_id);
		g1.launchViewer();
	}
	
	private int buildXADDForRow(int i) {
		String expr = row2Expr(i, true);
		ArrayList l = new ArrayList();
		ArrayList l1 = new ArrayList();
		ArrayList l2 = new ArrayList();
		l.add(expr);
		l1.add("1");
		l.add(l1);
		l2.add("0");
		l.add(l2);
		int id = xadd_context.buildCanonicalXADD(l);
		return id;
	}
	
	private void build01LossXADD() {
		
		xadd_context = new XADD();
		
		xadd_id = buildXADDForRow(0);
		for (int i=1; i<dr.nRows(); i++) {
			int id2 = buildXADDForRow(i);
			xadd_id = xadd_context.apply(xadd_id, id2, XADD.SUM);
			xadd_id = xadd_context.reduceLP(xadd_id);
		}
	}
	
	
	
	
	// functions for finding optimal solution
	
	private boolean isSolvable(double w0, int r) {
		
		LP lp = new LP(nw, obj_coef, LP.MINIMIZE);		//set objective
		lp._solver.setVerbose(1);
		for (int i=0; i<=r; i++) {						//add constraints
			if (c[i])
				lp.addLTConstraint(dr.wxy(i), 0.0);
			else
				lp.addGeqConstraint(dr.wxy(i), 0.0);
		}
		
		lp.addEqConstraint(lp.genUnitVector(0), w0);	//add w0 = -1 or 1
		
		lp.solve();
		boolean solvable = lp._status != LpSolve.INFEASIBLE;
		lp.free();
		
		return solvable;
	}
	
	
	private void findSolution(int r) {
		
		if (r >= dr.nRows()) { 	
			if (isSolvable(1.0, r-1) || isSolvable(-1.0, r-1)) {
				// found a better solution
				foundSolution = true;
				f_min = floss;
				System.arraycopy(c, 0, c_min, 0, c.length);
				System.out.println("new f_min = " + f_min);
			}
		}
		else { //search all possible paths
			findSolution(r+1);
			if (floss + 1 < f_min) {	//prune paths with floss > f_min
				c[r] = true;
				floss++;
				findSolution(r+1);
				c[r] = false;
				floss--;
			}
		}
	}
	
	
	
	
	// functions for finding the best possible w

	private double[] getBestW() {
		double[] bestw = calculateW(1.0);
		if (!isValidSolution(bestw))
			bestw = calculateW(-1.0);
		return bestw;
	}
	
	private double[] calculateW(double w0) {
		int nw = dr.xDim() + 1;
		double[] obj_coef = new double[nw];
		
		for (int j=0; j<nw; j++) { obj_coef[j] = 1.0; }
		
		LP lp = new LP(nw, obj_coef, LP.MINIMIZE);
		lp._solver.setVerbose(1);
		lp.addEqConstraint(lp.genUnitVector(0), w0);
		
		for (int i=0; i< c_min.length; i++) {
			if (c_min[i]) 
				lp.addLTConstraint(dr.wxy(i), 0.0);
			else 
				lp.addGeqConstraint(dr.wxy(i), 0.0);
		}
		
		double[] w = lp.solve();
		if (lp._status != LpSolve.INFEASIBLE) 
			return w;
		return null;
	}
	
	private boolean isValidSolution(double[] w) {
		if (w == null) return false;
		for (int j=1; j < w.length; j++)
			if (Math.abs(w[j]) > 1e-6) return true;
		return false;
	}
	
	
	
	// constructor
	public Loss01Solver(String filename) {
		
		dataFileName = filename;
		dr = new DataReader(dataFileName);
		
		if (dr.nRows() > 0) {
			
			nw = dr.xDim() + 1;			// set number of params w
			obj_coef = new double[nw];
			for (int j=0; j<nw; j++) 	// set objective = min sum (w_i)
				obj_coef[j] = 1.0; 
			
			// search all possible solutions and store the best
			foundSolution = false;

			c = new boolean[dr.nRows()];
			c_min = new boolean[dr.nRows()];
			
			floss = 0;
			f_min = Integer.MAX_VALUE;
			
			findSolution(0);
			
			if (foundSolution) w = getBestW();
		}
	}
	
	
	public static void main(String[] args) {
		String fname = "./src/ml/data_test.txt";
		Loss01Solver ls = new Loss01Solver(fname);
		//ls.showXADDGraph();	// will cause calculation of XADD -> slow!
		ls.showSolution();
		Visualizer viz = new Visualizer(fname, ls.getSolution());
		viz.pack();
		viz.setVisible(true);
	}

}
