/**
 * Simplex like method to solve loss 01 optimally
 * at each step, want to swap one point if loss reduced                                                       
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

	private DataReader dr;	// reads and stores classification data
	private String dataFileName;
	private double[] w;		// possible value of w0..wD for optimal solution, bias = wD
	private int loss;  		// value of loss function
	private SortedSet<fval> f;		// stores sorted function evaluation values 
	private Set<Integer> S;	// set of current selected points forming the hyperplane
	
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

	public double[] getWeights() { return w; }
	
    private String dbl2Str(double x) {
    	DecimalFormat df = new DecimalFormat("0.000");
    	String s = (df.format(x));
    	return s;
    }
	
    public void printSolution() {
    	printSolution("Minimal");
    }
    
	private void printSolution(String prefix) {
		if (prefix != null && prefix != "")
			System.out.print(prefix + " ");
		System.out.print("Loss= " + loss);
		System.out.print("   (");
		for (int j=0; j<w.length-1; j++) 
			System.out.print("w" + (j+1) + "= " + dbl2Str(w[j]) + ", ");
		System.out.println("bias= " + dbl2Str(w[w.length -1]) + ")");
	}
	
    
    // calculate total loss corresponding to weights w1, write function values to f1
    private int calTotalLoss(double[] w1, SortedSet<fval> f1) {
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
		w = model.getFeatureWeights();
		f = new TreeSet<fval>();
		loss = calTotalLoss(w, f);
		printSolution("SVM:");
	}
	
	private double[] calWeightsFromPoints(Set<Integer> S) {
        double [] rhs = new double[dr.xDim()];	// bias = 1, rhs = -1
		double [][] xvals = new double[dr.xDim()][dr.xDim()]; // matrix of x_i in S
		Iterator<Integer> it = S.iterator();
		for (int i=0; i<dr.xDim(); i++) {
			rhs[i] = -1d;	// bias = -1
			xvals[i] = dr.x(it.next());
		}
        RealMatrix X = new Array2DRowRealMatrix(xvals);
        DecompositionSolver solver = new LUDecomposition(X).getSolver();
        RealVector b = new ArrayRealVector(rhs);
        RealVector v = solver.solve(b);
        double[] res = new double[dr.xDim() + 1];
        System.arraycopy(v.toArray(), 0, res, 0, dr.xDim());
        res[dr.xDim()] = 1d;
        return res;
	}
	
	private int[] set2Array(Set<Integer> set) {
		int[] a = new int[set.size()];   
		int i = 0;   for (Integer val : set) 
			a[i++] = val;   
		return a; 
	}
	
	private void minimizeLoss() {
		// add D points nearest to SVM plane to S
		Iterator<fval> itf = f.iterator();
		S = new HashSet<Integer>();
		for (int i=0; i<dr.xDim(); i++) 
			S.add(itf.next().idx);
		w = calWeightsFromPoints(S);
		loss = calTotalLoss(w, f);
		printSolution("Simplex initial:");
		
		boolean swapped;
		do {
			swapped = false;
			itf = f.iterator();
			int[] arrS = set2Array(S);
			
			while (!swapped && itf.hasNext()) {
				int i1 = itf.next().idx;
				if (!S.contains(i1)) {
					for (int j=0; j<dr.xDim(); j++) {
						S.remove(arrS[j]); S.add(i1); 
						double[] w1 = calWeightsFromPoints(S);
						SortedSet<fval> f1 = new TreeSet<fval>();
						int loss1 = calTotalLoss(w1, f1);
						if (loss1 < loss) {
							loss = loss1;
							w = w1;
							f = f1;
							swapped = true;
							printSolution("Simplex new:");
							break;
						}
						else {
							S.remove(i1); S.add(arrS[j]); 
						}
					}
				}
					
			}
		} while (swapped);
		

	}
	
	
	// constructor
	public Loss01Simplex(String filename) {
		
		dataFileName = filename;
		dr = new DataReader(dataFileName);
		
		if (dr.nRows() > 0) {
			findBestSVM();	// find best SVM solution (assign to w, loss, f)
			if (dr.nRows() > dr.xDim() -1) 
				minimizeLoss();
			// now can't swap anymore => set right class & find one from optSolutions, 
			// preference to class 1
		}
	}
	
	
	public static void main(String[] args) {
		String fname = "./src/ml/data_test.txt";
		Loss01Simplex ls = new Loss01Simplex(fname);
		ls.printSolution();
		Visualizer viz = new Visualizer(fname, ls.getWeights());
		viz.pack();
		viz.setVisible(true);
	}

}
