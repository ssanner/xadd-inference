/**
 * Visualize data points and boundary line for a given 2 dimensions 
 * of a data with classification target. Data coordinates & range are 
 * scaled & change automatically to fit the canvas.
 *
 * @author Tan T. Nguyen
 * @version 30/4/2012
 *
 **/

package ml;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Line2D;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.SortedSet;

import javax.swing.JFrame;
import javax.swing.Timer;

import de.bwaldvogel.liblinear.*;

public class Visualizer extends JFrame implements KeyListener {

    private final static double zeroThreshold = 1e-7; // number < epsilon <=> number=0.0
    private Dimension dim = new Dimension(700, 700);
    private DataReader dr;
    private double[] w;    // model parameters w0, w1, w2, ... if all zeros then auto initiate
    private double[] w_old = null;
    private int x1, x2;        // id of column of data corresponding to dimension x1, x2 on the graph
    private int tm = 80;    // top margin of the canvas
    private int lm = 60;    // left margin
    private int bm = 40;    // bottom margin
    private int rm = 40;    // right margin
    private int am = 10;    // margins from boundary of the axis's square
    private int header = 60;// position of header text
    private double scaleW, scaleH; // scale of canvas vs data range
    private double max1 = 0, max2 = 0; // max value of data dimension x1, x2
    private double min1 = Double.MAX_VALUE, min2 = Double.MAX_VALUE; // min of x1, x2


    // constructors

    public Visualizer(String dataFileName) {
        setVisualizer(dataFileName, 0, 1, null);
    }

    public Visualizer(String dataFileName, double[] w) {
        setVisualizer(dataFileName, 0, 1, w);
    }

    public Visualizer(String dataFileName, int col1, int col2) {
        setVisualizer(dataFileName, col1, col2, null);
    }

    public Visualizer(String dataFileName, int col1, int col2, double[] w) {
        setVisualizer(dataFileName, col1, col2, w);
    }

    private void setVisualizer(String dataFileName, int col1, int col2, double[] w) {

        this.setPreferredSize(dim);
        this.setTitle("01 Loss Visualizer. DataFile: " + dataFileName);
        this.addKeyListener(this);

        dr = new DataReader(dataFileName);

        if (x1 >= dr.xDim() || x2 >= dr.xDim())
            System.out.println("x1 or x2 index out of bound.");
        else if (dr.nRows() > 0) {
            if (w == null)
                w = new double[dr.xDim() + 1];
            this.w = w;
            x1 = col1;
            x2 = col2;
            initializeParams();

            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        }
    }


    // set initial w if required, and find the scale of coordinates
    private void initializeParams() {

        for (int i = 0; i < dr.nRows(); i++) {
            // find min max
            if (dr.x(i, x1) > max1) max1 = dr.x(i, x1);
            if (dr.x(i, x1) < min1) min1 = dr.x(i, x1);
            if (dr.x(i, x2) > max2) max2 = dr.x(i, x2);
            if (dr.x(i, x2) < min2) min2 = dr.x(i, x2);
        }

        if (w[dr.xDim()] == 0.0 && w[x1] == 0.0 && w[x2] == 0.0) {
            Problem prob = new Problem();
            prob.l = dr.nRows();
            prob.n = dr.xDim() + 1;
            prob.bias = 1d;
            prob.y = new int[prob.l];
            prob.x = new FeatureNode[prob.l][prob.n];
            for (int i = 0; i < dr.nRows(); i++) {
                prob.y[i] = dr.y(i);
                for (int j = 0; j < dr.xDim(); j++) {
                    prob.x[i][j] = new FeatureNode(j + 1, dr.x(i, j));
                }
                // add additional input feature for bias:
                prob.x[i][dr.xDim()] = new FeatureNode(dr.xDim() + 1, 1d);
            }

            Linear.disableDebugOutput();
            Parameter param = new Parameter(SolverType.L1R_L2LOSS_SVC, 1, 0.01);

            Model model = Linear.train(prob, param);
            w = model.getFeatureWeights();
        }
        scaleW = (dim.width - lm - rm) / (max1 - min1);
        scaleH = (dim.height - tm - bm) / (max2 - min2);
    }

    public void updateW(double[] w_new) {
        w_old = w;
        this.w = w_new;
        this.paint(this.getGraphics());
    }

    public void resetW(double[] w_new) {
        w_old = null;
        this.w = w_new;
        this.paint(this.getGraphics());
    }


    public void paint(Graphics g) {
        drawAxes(g);
        drawHeader(g);
        drawDataPoints(g);
        drawBoundaryLine(g, w, Color.CYAN);
        if (w_old != null)
            drawBoundaryLine(g, w_old, Color.LIGHT_GRAY);
    }


    // change original x1 coordinate to width coordinate of canvas 
    private int getW(double x1) {
        double width = lm - min1 * scaleW + x1 * scaleW;
        return (int) width;
    }

    // change original x2 coordinate to height coordinate of canvas 
    private int getH(double x2) {
        double height = tm + max2 * scaleH - x2 * scaleH;
        return (int) height;
    }

    private String dbl2Str(double x) {
        DecimalFormat df = new DecimalFormat("0.000");
        String s = (df.format(x));
        return s;
    }

    // calculate total loss 
    private int calculateLoss() {
        int totalLoss = 0, onBoundary = 0;
        for (int i = 0; i < dr.nRows(); i++) {
            double y = w[w.length - 1];    // init y to bias
            for (int j = 0; j < dr.xDim(); j++)
                y += dr.x(i, j) * w[j];
            // consider points very closed boundary to be correctly classified
            if (Math.abs(y) < zeroThreshold) {
                y = 0;
                onBoundary++;
            }
            if (y * dr.y(i) < 0)    // misclassification
                totalLoss++;
        }
        if (totalLoss > dr.nRows() - onBoundary - totalLoss) {
            //reverse w will result in better totalLoss
            totalLoss = dr.nRows() - onBoundary - totalLoss;
        }
        return totalLoss;
    }

    private void drawMarker(Graphics g, double val, int axis) {
        if (axis == 1) { //x1 = W
            g.drawLine(getW(val), dim.height - bm + am - 2, getW(val), dim.height - bm + am + 2);
            g.drawString(dbl2Str(val), getW(val) - 10, dim.height - bm + am + 15);
        } else {
            g.drawLine(lm - am - 2, getH(val), lm - am + 2, getH(val));
            g.drawString(dbl2Str(val), 8, getH(val) + 4);
        }
    }

    private void drawAxes(Graphics g) {
        g.clearRect(0, 0, dim.width, dim.height);
        g.setColor(Color.GRAY);
        g.drawRect(lm - am, tm - am, dim.width - lm - rm + 2 * am, dim.height - tm - bm + 2 * am);
        for (double r = 0.0; r <= 1; r += 0.1) {
            drawMarker(g, min1 + r * (max1 - min1), 1);
            drawMarker(g, min2 + r * (max2 - min2), 2);
        }
    }

    private void drawHeader(Graphics g) {
        g.setColor(Color.MAGENTA);
        g.drawString("w0: " + dbl2Str(w[dr.xDim()]) + "   w1: " + dbl2Str(w[x1])
                + "   w2: " + dbl2Str(w[x2])
                + "   [Use QA WS ED to tweak]", lm - am, header);
        g.setColor(Color.ORANGE);
        g.drawString(dr.nRows() + " POINTS", dim.width - 230, header);
        g.setColor(Color.BLUE);
        g.drawString("LOSS = " + calculateLoss(), dim.width - rm - 50, header);
    }

    private void drawDataPoints(Graphics g) {
        for (int i = 0; i < dr.nRows(); i++) {
            if (dr.y(i) > 0) { // class 1
                g.setColor(Color.BLUE);
                g.drawLine(getW(dr.x(i, x1)) - 2, getH(dr.x(i, x2)), getW(dr.x(i, x1)) + 2, getH(dr.x(i, x2)));
                g.drawLine(getW(dr.x(i, x1)), getH(dr.x(i, x2)) - 2, getW(dr.x(i, x1)), getH(dr.x(i, x2)) + 2);
            } else {
                g.setColor(Color.RED);
                g.drawRoundRect(getW(dr.x(i, x1)) - 2, getH(dr.x(i, x2)) - 2, 4, 4, 2, 2);
            }
        }
    }

    private void drawBoundaryLine(Graphics g, double[] w, Color c) {
        int a1, b1, a2, b2;
        if (Math.abs(w[x1]) < 1e-6) { // w1=0 => horizontal line
            a1 = getW(min1);
            a2 = getW(max1);
            b1 = b2 = getH(-w[dr.xDim()] / w[x2]);
        } else if (Math.abs(w[x2]) < 1e-6) { // w2=0 => vertical line
            a1 = a2 = getW(-w[dr.xDim()] / w[x1]);
            b1 = getH(max2);
            b2 = getH(min2);
        } else {
            a1 = getW(min1);
            b1 = getH((-w[dr.xDim()] - w[x1] * min1) / w[x2]);
            if (b1 > getH(min2)) {
                b1 = getH(min2);
                a1 = getW((-w[dr.xDim()] - w[x2] * min2) / w[x1]);
            }
            if (b1 < getH(max2)) {
                b1 = getH(max2);
                a1 = getW((-w[dr.xDim()] - w[x2] * max2) / w[x1]);
            }

            a2 = getW(max1);
            b2 = getH((-w[dr.xDim()] - w[x1] * max1) / w[x2]);
            if (b2 > getH(min2)) {
                b2 = getH(min2);
                a2 = getW((-w[dr.xDim()] - w[x2] * min2) / w[x1]);
            }
            if (b2 < getH(max2)) {
                b2 = getH(max2);
                a2 = getW((-w[dr.xDim()] - w[x2] * max2) / w[x1]);
            }
        }
        g.setColor(c);
        g.drawLine(a1, b1, a2, b2);
    }


    public void keyPressed(KeyEvent e) {
    }


    public void keyReleased(KeyEvent e) {
    }

    private double incrW(double val) {
        double incr = Math.abs(val * 0.002); // change by 0.2%
        if (incr < 0.005) incr = 0.005; // minimal change = 0.005
        return incr;
    }


    public void keyTyped(KeyEvent e) {
        char c = e.getKeyChar();
        boolean wchanged = false;

        if (c == 'q' || c == 'Q') {
            w[dr.xDim()] += incrW(w[dr.xDim()]);
            wchanged = true;
        } else if (c == 'a' || c == 'A') {
            w[dr.xDim()] -= incrW(w[dr.xDim()]);
            wchanged = true;
        } else if (c == 'w' || c == 'W') {
            w[x1] += incrW(w[x1]);
            wchanged = true;
        } else if (c == 's' || c == 'S') {
            w[x1] -= incrW(w[x1]);
            wchanged = true;
        } else if (c == 'e' || c == 'E') {
            w[x2] += incrW(w[x2]);
            wchanged = true;
        } else if (c == 'd' || c == 'D') {
            w[x2] -= incrW(w[x2]);
            wchanged = true;
        }

        if (wchanged)
            this.paint(this.getGraphics());

    }

    public static void main(String args[]) {
        Visualizer viz = new Visualizer("./src/ml/data_test.txt");
        viz.pack();
        viz.setVisible(true);
    }

}
