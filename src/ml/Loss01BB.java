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


public class Loss01BB {

	private DataReader dr;	// reads and stores classification data
	private String dataFileName;
	
	private boolean foundSolution;
	private boolean[] c, c_min;	// current/min truth values of optimal solution, for each data point
								// c_min[i] = true, iff sum(x_ij w_ij) * y_i < 0 ( => f_loss(i) = 1)
	private int floss, f_min;	// current/min value of 01 loss function
	private ConvexPolytope cp0, cp1;
	
	int nw;					// number of parameters w0 ... w_nw
	private double[] w;		// possible value of w0..wD for optimal solution 

	double[] obj_coef = new double[nw]; // objective coef matrix
	


	
	
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
	
	
	
	// add heuristic: if a pointed is added to cp0 or cp1, we can calculate 
	// the number of minimum miss classification = points inside pt0, but
	// has true label=1  +  points inside cp1 but have true label=0.
	private void findSolution(int r) {
		
		if (r >= dr.nRows()) { 			// reached a leaf <=> found better solution, because of the way
			foundSolution = true;		// cp0, cp1 are constructed with no intersection => solvability
			f_min = floss;				// is guaranteed!
			System.arraycopy(c, 0, c_min, 0, c.length);
			System.out.println("new f_min = " + f_min);
		}
		
		else if (cp0.contains(dr.x(r))) { // x_r belong to class 0
			if (dr.y(r) > 0) { // r is miss classified in this path
				if (floss + 1 < f_min) {
					floss ++;
					c[r] = true;
					findSolution(r+1);
					c[r] = false;
					floss --;
				}
			}
			else
				findSolution(r+1);
		}
		
		else if (cp1.contains(dr.x(r))) { // x_r belong to class 1
			if (dr.y(r) < 0) { // r is miss classified in this path
				if (floss + 1 < f_min) {
					floss ++;
					c[r] = true;
					findSolution(r+1);
					c[r] = false;
					floss --;
				}
			}
			else
				findSolution(r+1);
		}
		
		else { // not sure which class is x_r --> try all paths
			
			// assume point r belong to its correct class dr.y(r)
			if (dr.y(r) > 0) {		// x_r is class 1, add x to polytope 1
				cp1.addVertix(dr.x(r));
				findSolution(r+1);
				cp1.popVertix();
			}
			else {					// x_r is class 0, add x to polytope 0
				cp0.addVertix(dr.x(r));
				findSolution(r+1);
				cp0.popVertix();
			}
			
			// assume point r classified incorrectly to class -dr.y(r)
			if (floss + 1 < f_min) {	//prune paths with floss > f_min
				floss++;
				c[r] = true;
				
				if (dr.y(r) > 0) {	// x_r miss classified = class 0, add x to polytope 0
					cp0.addVertix(dr.x(r));
					findSolution(r+1);
					cp0.popVertix();
				}
				else {				// x_r miss classified = class 1, add x to polytope 1
					cp1.addVertix(dr.x(r));
					findSolution(r+1);
					cp1.popVertix();
				}
				
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
	public Loss01BB(String filename) {
		
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
			cp0 = new ConvexPolytope(dr.xDim(), dr.nRows());
			cp1 = new ConvexPolytope(dr.xDim(), dr.nRows());
			
			floss = 0;
			f_min = Integer.MAX_VALUE;
			
			findSolution(0);
			
			if (foundSolution) w = getBestW();
		}
	}
	
	
	public static void main(String[] args) {
		String fname = "./src/ml/data_test.txt";
		Loss01BB ls = new Loss01BB(fname);
		ls.showSolution();
		Visualizer viz = new Visualizer(fname, ls.getSolution());
		viz.pack();
		viz.setVisible(true);
	}

}
