/**
 * Simplex like method to solve loss 01 optimally
 * at each step, want to swap one point if loss reduced.                                                       
 * Heuristic: start with D points nearest to the hyperplane
 * defined by SVM. Then consider replacing points nearest to 
 * the current hyperplane.
 * 
 * @author Tan T. Nguyen (tan1889@gmail.com)
 * @version 30/4/2012
 *
 **/

package ml;

import java.text.DecimalFormat;
import de.bwaldvogel.liblinear.*;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;


public class Loss01Simplex {
	
	private final static double delta = 1e-6;
	private DataReader dr;	// reads and stores classification data
	private String dataFileName;
	private Solution solution;
//	private double[] w;		// possible value of w0..wD for optimal solution, bias = wD
//	private int loss;  		// value of loss function
//	private SortedSet<fval> f;		// stores sorted function evaluation values 
	private Set<Integer> S;	// set of current selected points forming the hyperplane
	
	
	// stores a solution
	private final class Solution {
		public final double[] w;
		public final int loss;
		public final SortedSet<fval> f;
		Solution(double[] w, int loss, SortedSet<fval> f) { 
			this.w = w; 
			this.loss = loss; 
			this.f = f;
		}
	}
		
	// comparable class to store function evaluation in SortedSet.
	private final class fval implements Comparable<fval> {
		public final double val;
		public final int idx;
		fval(double v, int i) { val = v; idx = i; }
		
		public int compareTo(fval f2) {
			if (Math.abs(val) < Math.abs(f2.val)) return -1;
			else if (Math.abs(val) > Math.abs(f2.val)) return 1;
			else return idx - f2.idx;
		}
	}
	

	// returns feature weights of the solution
	public double[] getWeights() { return solution.w; }
	
    private String dbl2Str(double x) {
    	DecimalFormat df = new DecimalFormat("0.000");
    	String s = (df.format(x));
    	return s;
    }
	
    // print out loss value & corresponding weights
    public void printSolution() {
    	printSolution("Minimal");
    }
    
	private void printSolution(String prefix) {
		if (prefix != null && prefix != "")
			System.out.print(prefix + " ");
		System.out.print("Loss= " + solution.loss);
		System.out.print("   (");
		for (int j=0; j<solution.w.length-1; j++) 
			System.out.print("w" + (j+1) + "= " + dbl2Str(solution.w[j]) + ", ");
		System.out.println("bias= " + dbl2Str(solution.w[solution.w.length -1]) + ")");
	}
	
    
    // calculate total loss corresponding to weights w1, write function values to f1
    private int calculateLoss(double[] w1, SortedSet<fval> f1) {
    	int totalLoss = 0;
    	f1.clear();
    	for (int i=0; i<dr.nRows(); i++) {
    		double y = w1[w1.length -1]; 	// init y to bias
    		for (int j=0; j<dr.xDim(); j++)
        		y += dr.x(i,j) * w1[j];
    		f1.add(new fval(y, i));
    		if (y * dr.y(i) < 0) 	// misclassification
    			totalLoss++;
    	}
    	return Math.min(totalLoss, dr.nRows() - totalLoss); // can alwaf1 reverse sign of all weighhts
    }
	
    private Solution getSolutionFromWeights(double[] w) {
    	SortedSet<fval> f = new TreeSet<fval>();
		int loss = calculateLoss(w, f);
		return new Solution(w, loss, f);
    }
    
    // find the SVM solution, assign to w, f, loss
	private void findBestSVM() {
		Problem prob = new Problem();
		prob.l = dr.nRows();
		prob.n = dr.xDim()+1;
		prob.bias = 1d;
		prob.y = new int[prob.l];
		prob.x = new FeatureNode[prob.l][prob.n];
		for (int i=0; i<dr.nRows(); i++) {
			prob.y[i] = dr.y(i);
			for (int j=0; j<dr.xDim(); j++) {
				prob.x[i][j] = new FeatureNode(j+1, dr.x(i,j));
			}
			// add additional input feature for bias:
			prob.x[i][dr.xDim()] = new FeatureNode(dr.xDim()+1, 1d);	
		}
		
		Parameter param = new Parameter(SolverType.L1R_L2LOSS_SVC, 1, 0.01);

		Model model = Linear.train(prob, param);
		solution = getSolutionFromWeights(model.getFeatureWeights());
		printSolution("SVM:");
	}
	
	// solve the matrix equation and return weights vector corresponding to
	// a set of D points from the data inputs.
	private Solution getBestSolutionFromS() {
        double [] rhs = new double[dr.xDim()];	// bias = 1, rhs = -1
		double [][] xvals = new double[dr.xDim()][dr.xDim()]; // matrix of x_i in S
		Iterator<Integer> it = S.iterator();
		for (int i=0; i<dr.xDim(); i++) {
			rhs[i] = -1d;	//bias =1 => rhs = -1
			xvals[i] = dr.x(it.next());
		}
        RealMatrix X = new Array2DRowRealMatrix(xvals);
        DecompositionSolver solver = new LUDecomposition(X).getSolver();
        RealVector b = new ArrayRealVector(rhs);
        RealVector w = solver.solve(b);
        
        double[] w1 = new double[dr.xDim() + 1];
        System.arraycopy(w.toArray(), 0, w1, 0, dr.xDim());
        w1[dr.xDim()] = 1d - delta;
        double[] w2 = w1.clone();
        w2[dr.xDim()] = 1d + delta;
        
        Solution s1 = getSolutionFromWeights(w1);
        Solution s2 = getSolutionFromWeights(w2);
        if (s1.loss < s2.loss) return s1;
        return s2;
	}
	
	
	// convert a int set to an int array 
	private int[] set2Array(Set<Integer> set) {
		int[] a = new int[set.size()];   
		int i = 0;   for (Integer val : set) 
			a[i++] = val;   
		return a; 
	}
	
	// start from D points nearest to SVM hyperplane, tweak this set 
	// until no further tweak would reduce loss => found minimal loss
	private void minimizeLoss() {
		// INIT: add D points nearest to SVM plane to S
		Iterator<fval> it = solution.f.iterator();
		S = new HashSet<Integer>();
		for (int i=0; i<dr.xDim(); i++) 
			S.add(it.next().idx);
		// update w, loss, f
		solution = getBestSolutionFromS();
		printSolution("Simplex initial:");
		
		boolean swapped;
	
		do {
			swapped = false;
			it = solution.f.iterator();
			int[] arrS = set2Array(S);
			
			while (!swapped && it.hasNext()) {
				int i1 = it.next().idx;	// possible point to swap
				if (!S.contains(i1)) {
					for (int j=0; j<dr.xDim(); j++) {
						S.remove(arrS[j]); S.add(i1); // swap two points from/to S
						Solution s2 = getBestSolutionFromS();
						
						if (s2.loss < solution.loss) {
							// swap reduces loss => do it and break to another swap
							solution = s2;
							swapped = true;
							printSolution("Simplex new:");
							break;
						}
						else {
							// swap doesn't reduce loss => undo swap
							S.remove(i1); S.add(arrS[j]); 
						}
					}
				}
					
			}
		} while (swapped); //only stop when no point is swapped => found minimum loss
	}
	
	
	// constructor
	public Loss01Simplex(String filename) {
		
		dataFileName = filename;
		dr = new DataReader(dataFileName);
		
		if (dr.nRows() > 0) {
			findBestSVM();	// find best SVM solution (assign to w, loss, f)
			if (dr.nRows() > dr.xDim() -1) 
				minimizeLoss();
			printSolution();
		}
	}
	
	
	public static void main(String[] args) {
		String fname = "./src/ml/data_test.txt";
		Loss01Simplex ls = new Loss01Simplex(fname);
		Visualizer viz = new Visualizer(fname, ls.getWeights());
		viz.pack();
		viz.setVisible(true);
	}

}
