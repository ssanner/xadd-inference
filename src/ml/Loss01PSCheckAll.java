/**
 * Points selection (PS) method to solve loss 01 optimally.
 * At each repeat:
 * 1) Select D points that never been checked before
 * 2) Find local minimum corresponding to that D points by
 *    swap one point int/out if loss reduced.                                                       
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
import java.util.Arrays;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;


public class Loss01PSCheckAll {
	
	private final static double zeroThreshold = 1e-7; // |number| < epsilon <=> number=0.0
	private DataReader dr;	// reads and stores classification data
	private String dataFileName;

	private boolean showViz;
	private boolean showLog;
	private Visualizer viz;

	private int N,D;	// number of data points & number of dimension of input
	private Solution solution;
	
	private int[] id; 	// indices of points ordered by distance to SVM hyperplane
	private boolean[] touched; // point[i] has ben touched[i]
	private boolean selectUntouchedPoints;
	private int[] psId;		// combination base for N choose D
	private Set<Long> selected;

	
	// stores a solution
	private final class Solution {
		public final double[] w;
		public final int loss;
		Solution(double[] w, int loss) { 
			this.w = w; 
			this.loss = loss; 
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
	

	// getters
	public double[] getWeights() { return solution.w; }
	public int getLoss() { return solution.loss; }
	public int[] getOrderedIndices() { return getOrderedIndices(solution); }
	
	private int[] getOrderedIndices(Solution sol) {
		SortedSet<fval> f = new TreeSet<fval>();
    	for (int i=0; i<dr.nRows(); i++) {
    		double y = sol.w[D]; 	// init. y to bias
    		for (int j=0; j<D; j++)
        		y += dr.x(i,j) * sol.w[j];
    		f.add(new fval(y, i));
    	}
    	
		int[] ids = new int[f.size()];   
		int i=0;
		Iterator<fval> it = f.iterator();
		while (it.hasNext()) 
			ids[i++] = it.next().idx;  
		return ids; 
	}
	
	// helpers
    private String dbl2Str(double x) {		// format double to string with 3 decimal places
    	DecimalFormat df = new DecimalFormat("0.000");
    	String s = (df.format(x));
    	return s;
    }
	
    public void printSolution() {			// print out loss & weights of solution
    	if (!showLog) return;
    	printSolution("PS Minimal");
    }
    
	private void printSolution(String prefix) {
		if (!showLog) return;
		if (prefix != null && prefix != "")
			System.out.print(prefix + " ");
		System.out.print("Loss= " + solution.loss);
		System.out.print("   (");
		for (int j=0; j<solution.w.length-1; j++) 
			System.out.print("w" + (j+1) + "= " + dbl2Str(solution.w[j]) + ", ");
		System.out.println("bias= " + dbl2Str(solution.w[solution.w.length -1]) + ")");
	}
	
    
    // calculate total loss corresponding to weights w1, write function values to f1
    private int calculateLoss(double[] w1) {
    	int totalLoss = 0, onBoundary=0;
    	for (int i=0; i<dr.nRows(); i++) {
    		double y = w1[w1.length -1]; 	// init y to bias
    		for (int j=0; j<dr.xDim(); j++)
        		y += dr.x(i,j) * w1[j];
    		// consider points very closed boundary to be correctly classified
    		if (Math.abs(y) < zeroThreshold) {
    			y = 0;
    			onBoundary++;
    		}
    		if (y * dr.y(i) < 0) 	// misclassification
    			totalLoss++;
    	}
    	if (totalLoss > dr.nRows() - onBoundary - totalLoss) {
    		//reverse w will result in better totalLoss
    		totalLoss = dr.nRows() - onBoundary - totalLoss;
    		for (int i=0; i<w1.length; i++)
    			w1[i] = -w1[i];
    	}
    	return totalLoss; 
    }
	
    
    // find the SVM solution for input data
	private Solution getSVMSolution() {
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
		
		Parameter param = new Parameter(SolverType.L1R_L2LOSS_SVC, 1, 0.01); //.L1R_LR
		
		Linear.disableDebugOutput();
		Model model = Linear.train(prob, param);
		double[] ws = model.getFeatureWeights();
		int ls = calculateLoss(ws);
		return new Solution(ws, ls);
	}
	
	// solve the matrix equation and return weights vector corresponding to
	// a set of D points from the data inputs.
	private Solution getSolutionFromPS(int[] ps) {
        double [] rhs = new double[D];	// bias = 1, rhs = -1
		double [][] xvals = new double[D][D]; // matrix of x_i in S
		for (int i=0; i<D; i++) {
			rhs[i] = -1d;	//bias =1 => rhs = -1
			xvals[i] = dr.x(ps[i]);
		}
        RealMatrix X = new Array2DRowRealMatrix(xvals);
        DecompositionSolver solver = new LUDecomposition(X).getSolver();
        RealVector b = new ArrayRealVector(rhs);
        RealVector w;
        try { w = solver.solve(b); }
        catch (Exception e) { // no solution, singular matrix
        	return new Solution(null, Integer.MAX_VALUE);
        }
        
        double[] w0 = new double[D+1];
        System.arraycopy(w.toArray(), 0, w0, 0, D);
        w0[D] = 1d;
        int l0 = calculateLoss(w0);
        return new Solution(w0, l0);
        
	}
	
	private boolean psContains(int[] ps, int val) {
		for (int i=0; i < D; i++)
			if (ps[i] == val) return true;
		return false;
	}

	
	private long getHash(int[] ps) {
		long x = ps[0];
		for (int i=1; i<D; i++) // calculate Cantor pairing number
			x = x*N + ps[i]; 
		return x;
	}

	
	private void printPS(String prefix, int[] a, int loss) {
		System.out.print(prefix + ": (" + a[0]);
		for (int i=1; i<D; i++) 
			System.out.print(", " + a[i]);
		System.out.println(")  [Loss=" + loss+ "]");
	}
	
	// start from points selection ps, tweak this set 
	// until no further tweak would reduce loss => found optimal loss for this ps
	private Solution getOptimalSolutionForPS(int[] ps) {
		
		//Arrays.sort(ps);
		selected.add(getHash(ps));
		if (selectUntouchedPoints) 
			for (int i=0; i<D; i++)
				touched[ps[i]] = true;
		
		Solution sol = getSolutionFromPS(ps);
//		printPS("New PS", ps, sol.loss);
		boolean swapped;
	
		do {
			swapped = false;
			
			for (int i=0; i<N; i++) 
				if (Arrays.binarySearch(ps, id[i]) < 0) {
					for (int j=0; j<D; j++) {
						int[] new_ps = ps.clone();
						new_ps[j] = id[i];
						//Arrays.sort(new_ps);
						//long hash = getHash(new_ps);
						//if (!selected.contains(hash)) {
						//	selected.add(hash);
						//	if (selectUntouchedPoints) touched[id[i]] = true;
							Solution new_sol = getSolutionFromPS(new_ps);
							if (new_sol.loss < sol.loss) {	// swap reduces loss => do it
								sol = new_sol;
								ps = new_ps;
								swapped = true;
//								printPS("Better PS", new_ps, new_sol.loss);
								break;
							}
//							else 
//								printPS("Checked PS", new_ps, new_sol.loss);
						//}
					}
				}
					
		} while (swapped); //only stop when no point is swapped => found minimum loss
		
		return sol;
	}
	

	// assign a next points selection to ps in increasing order of their indices
	private int[] getNextPS(int[] ps) {
		int i = 0;
		if (selectUntouchedPoints) {
			for (int j=0; j<dr.nRows(); j++)
				if (!touched[id[j]]) {
					ps[i++] = id[j];
					if (i >= D) return ps;
				}
			selectUntouchedPoints = false; // not returned before => not enough untouched points anymore
		}
		
		// select next set of points which has not been chosen before 
		// (hash code not in selected), 
		do {
			//ps0 = next ps0 (next combination)
			i = D-1;
			while (i>=0) {
				if (psId[i]<N-D+i) {
					psId[i]++;
					break;
				}
				else {
					while (i>=0 && psId[i]>=N-D+i) i--;
					if (i>=0) {
						psId[i]++;
						for (int j=i+1; j<D; j++) 
							psId[j] = psId[j-1] + 1;
						break;
					}
				}
			}
			
			if (i>=0) {
				for (int j=0; j<D; j++) ps[j] = id[psId[j]];
				//Arrays.sort(ps);
				//if (!selected.contains(getHash(ps))) 
					return ps;
			}
		} while (i>=0);
		
		// all possible points selections have been selected => set ps = null
		return null;
	}
	
	
	private void solveLoss01(String filename, boolean showVisualizer, boolean showLog) {

		long time0, time1=0, timeEnd;
		time0 = System.currentTimeMillis();
		
		this.showViz = showVisualizer;
		this.showLog = showLog;
		dataFileName = filename;
		dr = new DataReader(dataFileName);
		D = dr.xDim();
		N = dr.nRows();
		selected = new TreeSet<Long>(); //HashSet<Long>(); // //;
		touched = new boolean[N];		// all set to false
		selectUntouchedPoints = true;
		psId = new int[D]; 
		for (int i=0; i<D; i++) psId[i]=i;
		
		if (N > 0) {

			if (showViz) {
				viz = new Visualizer(filename);
				viz.pack();
				viz.setVisible(true);
			}

			solution = getSVMSolution();		// find best SVM solution (assign to w, loss)
			printSolution("SVM");
			id = getOrderedIndices(solution); 	// get list of indices ordered by distance to SVM hyperplane

			if (N >= D) { // there must be enough N points to create hyperplane
				int[] ps = new int[D];
				do {
					ps = getNextPS(ps);
					if (ps != null) {
						Solution new_sol = getOptimalSolutionForPS(ps);
						if (new_sol.loss < solution.loss) {
							solution = new_sol;
							time1 = System.currentTimeMillis();
							printSolution(" * PS new:");
							if (showViz)
								viz.updateW(solution.w);
						}
					}
				} while (ps != null);
			}
			
			printSolution();
			
			timeEnd = System.currentTimeMillis();
			System.out.println("CheckAll run time: " + (timeEnd - time0) 
					+ "ms. Optimal solution found in: " + (time1 - time0) + "ms.");
			//System.out.println("Selected PS size: " + selected.size());
			if (showViz)
				viz.resetW(solution.w);
		}
	}
	
	
	// constructors
	Loss01PSCheckAll(String filename) {
		solveLoss01(filename, false, false);
	}
	
	Loss01PSCheckAll(String filename, boolean showVisualizer) {
		solveLoss01(filename, showVisualizer, false);
	}
	
	Loss01PSCheckAll(String filename, boolean showVisualizer, boolean showLog) {
		solveLoss01(filename, showVisualizer, showLog);
	}
	
	public static void main(String[] args) {
		String dataFileName = "./src/ml/data_test.txt";
		final int maxTests = 1000;
		final int nPoints = 40;
		
		for (int i=0; i<maxTests; i++) {
			
			DataGenerator dg = new DataGenerator(nPoints, 2, dataFileName);
			Loss01PS lps = new Loss01PS(dataFileName);
			Loss01PSCheckAll lca = new Loss01PSCheckAll(dataFileName);
			
			if (lps.getLoss() != lca.getLoss()) {
				System.out.println("Test #" + i + ": lossPS= " + lps.getLoss() 
						+ " < lossPSCheckAll= " + lca.getLoss());
				Visualizer viz = new Visualizer(dataFileName, 0, 1);
				viz.pack();
				viz.setVisible(true);
				viz.updateW(lps.getWeights());
				viz.updateW(lca.getWeights());
				break;
			}	
			else 
				System.out.println("Test #" + i + ": lossPS = lossPSCheckAll.");
		}
	}

}
