/**
 * Simplex like method to solve loss 01 optimally.
 * At each step, want to swap one point if loss reduced.                                                       
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

import lpsolve.LP;
import lpsolve.LpSolve;


public class Loss01BB {

    private final static double zeroThreshold = 1e-7; // |number| < epsilon <=> number=0.0
    private DataReader dr;    // reads and stores classification data
    private String dataFileName;
    private boolean foundBBSolution;
    private boolean[] c, c1;    // min/current truth values of optimal solution, for each data point
    // c[i] = true, iff sum(x_ij w_ij) * y_i < 0 ( => f_loss(i) = 1)
    private int loss0, loss, loss1;    // min/current value of 01 loss function

    int nw;                    // number of parameters w0 ... w_nw
    private double[] w0, w;        // possible value of w0..wD for optimal solution
    private int[] id;        // order of row to visit
    private boolean[] c0;    // preferred truth value for of rows in id above

    double[] obj_coef;        // objective coef matrix
    double[] ub, lb;        // upper bound and lower bound of the solution

    private boolean showLog;
    private boolean showViz;
    private Visualizer viz;


    // returns feature weights of the solution
    public double[] getWeights() {
        return w;
    }

    public double[] getWeightsSimplex() {
        return w0;
    }

    public int getLoss() {
        return loss;
    }

    public int getLossSimplex() {
        return loss0;
    }

    private String dbl2Str(double x) {
        DecimalFormat df = new DecimalFormat("0.000");
        String s = (df.format(x));
        return s;
    }

    // print out loss value & corresponding weights
    public void printSolution() {
        if (!showLog) return;
        if (loss == loss0)
            System.out.println("No better solution found by BB.");
        else
            printSolution("BB Minimal");
    }

    private void printSolution(String prefix) {
        if (!showLog) return;
        if (prefix != null && prefix != "")
            System.out.print(prefix + " ");
        System.out.print("Loss= " + loss);
        System.out.print("   (");
        for (int j = 0; j < w.length - 1; j++)
            System.out.print("w" + (j + 1) + "= " + dbl2Str(w[j]) + ", ");
        System.out.println("bias= " + dbl2Str(w[w.length - 1]) + ")");
    }


    private boolean isValidSolution(double[] w) {
        if (w == null) return false;
        for (int j = 1; j < w.length; j++)
            if (Math.abs(w[j]) > zeroThreshold) return true;
        return false;
    }

    private double[] calculateW(double bias, boolean[] c) {

        LP lp = new LP(nw, lb, ub, obj_coef, LP.MINIMIZE);
        lp._solver.setVerbose(0);
        lp.addEqConstraint(lp.genUnitVector(nw - 1), bias);

        for (int i = 0; i < dr.nRows(); i++) {
            if (c[i])
                lp.addLTConstraint(dr.wxy(i), 0.0);
            else
                lp.addGeqConstraint(dr.wxy(i), 0.0);
        }

        double[] w1 = lp.solve();
        if (lp._status != LpSolve.INFEASIBLE)
            return w1;
        return null;
    }

    private double[] getBestW(boolean[] c) {
        double[] bestw = calculateW(1.0, c);
        if (!isValidSolution(bestw))
            bestw = calculateW(-1.0, c);
        return bestw;
    }


    private boolean isSolvable(double bias) {

        LP lp = new LP(nw, lb, ub, obj_coef, LP.MINIMIZE);
        lp._solver.setVerbose(0);
        for (int i = 0; i < dr.nRows(); i++) {                        //add constraints
            if (c1[i])
                lp.addLTConstraint(dr.wxy(i), 0.0);
            else
                lp.addGeqConstraint(dr.wxy(i), 0.0);
        }

        lp.addEqConstraint(lp.genUnitVector(0), bias);    //add bias = -1 or 1

        lp.solve();
        boolean solvable = lp._status != LpSolve.INFEASIBLE;
        lp.free();

        return solvable;
    }


    private void findSolution(int r) {

        if (r >= dr.nRows()) {
            double[] w2 = getBestW(c1);
            if (isValidSolution(w2)) {
                // found a better solution
                foundBBSolution = true;
                w = w2;
                loss = loss1;
                c = c1.clone();
                if (showViz) {
                    printSolution(" * BB new:");
                    viz.updateW(w);
                }
            }
        } else { //search all possible paths
            c1[id[r]] = c0[id[r]];
            if (c1[id[r]]) loss1++;
            if (loss1 < loss)
                findSolution(r + 1);
            if (c1[id[r]]) loss1--;

            c1[id[r]] = !c0[id[r]];
            if (c1[id[r]]) loss1++;
            if (loss1 < loss)
                findSolution(r + 1);
            if (c1[id[r]]) loss1--;
        }
    }

    private void solveLoss01(String filename, boolean showVisualizer, boolean showLog) {

        this.showViz = showVisualizer;
        this.showLog = showLog;
        dataFileName = filename;
        dr = new DataReader(dataFileName);

        if (dr.nRows() > 0) {

            if (showViz) {
                viz = new Visualizer(filename);
                viz.pack();
                viz.setVisible(true);
            }

            // start with solution of Loss01Simplex
            Loss01PS ls = new Loss01PS(dataFileName);
            w0 = ls.getWeights();
            w = w0.clone();
            loss0 = ls.getLoss();
            loss = loss0;
            id = ls.getOrderedIndices();
            c0 = new boolean[dr.nRows()];
            for (int i = 0; i < dr.nRows(); i++) {
                double y = w[w.length - 1];    // init y to bias
                for (int j = 0; j < dr.xDim(); j++)
                    y += dr.x(id[i], j) * w[j];
                if ((Math.abs(y) < zeroThreshold) || (y * dr.y(id[i]) > 0))
                    c0[id[i]] = false;
                else
                    c0[id[i]] = true;
            }

            printSolution("Simplex Solution:");
            if (showViz) viz.updateW(w);

            // now do branch and bound search
            nw = dr.xDim() + 1;            // set number of params w
            obj_coef = new double[nw];
            ub = new double[nw];
            lb = new double[nw];
            for (int j = 0; j < nw; j++) {    // set objective = min sum (w_i)
                obj_coef[j] = 1.0;
                lb[j] = -1e4;
                ub[j] = 1e4;
            }

            foundBBSolution = false;
            c = c0.clone();
            loss = loss0;
            c1 = new boolean[dr.nRows()];

            findSolution(0);

            if (foundBBSolution)
                printSolution();
            else if (showLog)
                System.out.println("BB hasn't found any better solution.");
        }
    }

    // constructors
    Loss01BB(String filename) {
        solveLoss01(filename, false, false);
    }

    Loss01BB(String filename, boolean showVisualizer) {
        solveLoss01(filename, showVisualizer, false);
    }

    Loss01BB(String filename, boolean showVisualizer, boolean showLog) {
        solveLoss01(filename, showVisualizer, showLog);
    }

    public static void main(String[] args) {
        String fname = "./src/ml/data_test.txt";
        Loss01BB ls = new Loss01BB(fname, true, true);
    }

}
