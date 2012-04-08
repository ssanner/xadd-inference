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

import javax.swing.JFrame;
import javax.swing.Timer;


/**
 * Visualize data points and boundary line for a given 2 dimensions 
 * of a data with classification target. Data coordinates & range are 
 * scaled & change automatically to fit the canvas.
 * @author Tan T. Nguyen
 */
public class Visualizer extends JFrame implements KeyListener{
    
	private Dimension dim = new Dimension(700, 700);
    private DataReader dr;
    private double[] w; // model parameters w0, w1, w2, ... if all zeros then auto initiate
    private int x1, x2;	// id of column of data for dimension x1, x2 on the graph
    private int margin = 50;	// left, right, top, bottom margin of canvas
    private double[] incr = { 0.01, 0.005, 0.005 }; // increase w0, w1, w2 by this much when tweaked
    private double scale;		// graphic scale factor relative to input data range
    private double max1 = 0, max2 = 0; // max value of data dimension x1, x2
	private double min1 = Double.MAX_VALUE, min2 = Double.MAX_VALUE; // min of x1, x2
	private int loss = 0;
	
	
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
			this.x1 = col1;
			this.x2 = col2;
			initializeParams();
			standardizeW();
			calculateLoss();
			
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		}
	}
	
	private void standardizeW() {
		//standardize w0 = -1.0
		w[x1+1] = - w[x1+1] / w[0];
		w[x1+2] = - w[x1+2] / w[0];
		w[0] = - 1.0;
	}
	
	// set initial w if required, and find the scale of coordinates
	private void initializeParams() {
    	double x1m0=0, x1m1=0, x2m0=0, x2m1=0; // x1m0 = means of all x1 in class 0

    	int n0=0, n1=0; // n0 = number of rows in class 0
    	for (int i=0; i<dr.nRows(); i++) {
    		if (dr.y(i) <= 0) { //class 0
    			n0++;
    			x1m0 += dr.x(i, x1);
    			x2m0 += dr.x(i, x2);
    		}
    		else {
    			n1++;
    			x1m1 += dr.x(i, x1);
    			x2m1 += dr.x(i, x2);
    		}
    		// find min max
    		if (dr.x(i, x1) > max1) max1 = dr.x(i, x1);
    		if (dr.x(i, x1) < min1) min1 = dr.x(i, x1);
    		if (dr.x(i, x2) > max2) max2 = dr.x(i, x2);
    		if (dr.x(i, x2) < min2) min2 = dr.x(i, x2);
    	}
    	x1m0 = x1m0 / n0;
    	x2m0 = x2m0 / n0;
    	x1m1 = x1m1 / n1;
    	x2m1 = x2m1 / n1;
    	if (w[0]==0.0 && w[x1+1]==0.0 && w[x2+1]==0.0) {
    		w[x1+1] = x1m1 - x1m0;
    		w[x2+1] = x2m1 - x2m0;
    		w[0] = -w[x1+1] * (x1m0 + x1m1)/2 - w[x2+1] * (x2m0 + x2m1)/2;
    	}
    	scale = (dim.width - 2 * margin) / Math.max(max1 - min1, max2 - min2);
    }
	

	
    public void paint(Graphics g) {
    	drawAxes(g);
        drawDataPoints(g);
        drawBoundaryLine(g);
        drawLoss(g);
    }
    
    
    // change original x1 coordinate to width coordinate of canvas 
    private int getW(double x1) {
    	double width = margin - min1 * scale + x1 * scale;
    	return (int) width;
    }
    
    // change original x2 coordinate to height coordinate of canvas 
    private int getH(double x2) {
    	double height = margin + max2 * scale - x2 * scale;
    	return (int) height;
    }
    
    private String dbl2Str(double x) {
    	DecimalFormat df = new DecimalFormat("0.00");
    	String s = (df.format(x));
    	return s;
    }
    
    // evaluate loss function of row i with current w
    private int evalLoss(int i) {
    	double f = w[0] + dr.x(i,x1) * w[x1+1] + dr.x(i,x2)*w[x2+1];
    	
    	// only true if w0 < 0, else needs to reverse.
    	if (f * dr.y(i) < 0) return 1;
    	return 0;
    }
    
    private void calculateLoss() {
    	loss = 0;
    	for (int i=0; i<dr.nRows(); i++)
    		loss += evalLoss(i);
    }
    
    private void drawLoss(Graphics g) {
    	g.setColor(Color.MAGENTA);
    	g.drawString("LOSS = " + loss, dim.width - margin - 50, dim.height - 20);
    }
    
    private void drawAxes(Graphics g) {
    	g.clearRect(0, 0, dim.width, dim.height);
    	g.setColor(Color.GRAY);
    	g.drawLine(margin, dim.height/2, dim.width - margin, dim.height/2);
    	g.drawLine(dim.width/2, margin, dim.width/2, dim.height - margin);
    	g.drawLine(getW(min1), dim.height/2 - 2, getW(min1), dim.height/2 + 2);
    	g.drawString(dbl2Str(min1), getW(min1) - 10 , dim.height/2 + 15);
    	g.drawLine(getW(max1), dim.height/2 - 2, getW(max1), dim.height/2 + 2);
    	g.drawString(dbl2Str(max1), getW(max1) - 10 , dim.height/2 + 15);
    	g.drawLine(dim.width/2 - 2, getH(max2), dim.width/2 + 2, getH(max2));
    	g.drawString(dbl2Str(max2), dim.width/2 + 6 , getH(max2) + 4);  
    	g.drawLine(dim.width/2 - 2, getH(min2), dim.width/2 + 2, getH(min2));
    	g.drawString(dbl2Str(min2), dim.width/2 + 6 , getH(min2) + 4);
    	g.setColor(Color.BLACK);
    	g.drawString("w0: " + dbl2Str(w[0]) + "     w1: " + dbl2Str(w[x1+1]) 
    			+ "     w2: " + dbl2Str(w[x2+1]) 
    			+ "     [Use QA WS ED to tweak]", margin, dim.height - 20);
    	g.setColor(Color.ORANGE);
    	g.drawString(dr.nRows() + " POINTS", dim.width - 230, dim.height - 20);
    }
    
    private void drawDataPoints(Graphics g) {
    	for (int i=0; i<dr.nRows(); i++) {
    		if (dr.y(i) > 0) { // class 1
    			g.setColor(Color.BLUE);
    			g.drawLine(getW(dr.x(i, x1)) - 2, getH(dr.x(i, x2)), getW(dr.x(i, x1)) + 2, getH(dr.x(i, x2)));
    			g.drawLine(getW(dr.x(i, x1)), getH(dr.x(i, x2)) - 2, getW(dr.x(i, x1)), getH(dr.x(i, x2)) + 2);
    		}
    		else {
    			g.setColor(Color.RED);
    			g.drawOval(getW(dr.x(i, x1)), getH(dr.x(i, x2)), 4, 4);
    		}
    	}
    }
    
    private void drawBoundaryLine(Graphics g) {
    	int a1, b1, a2, b2;
    	if (Math.abs(w[x2+1]) > 1e-6) {
    		a1 = getW(min1);
    		b1 = getH((-w[0]-w[x1+1]*min1)/w[x2+1]);
    		a2 = getW(max1);
    		b2 = getH((-w[0]-w[x1+1]*max1)/w[x2+1]);
    	}
    	else {
    		a1 = a2 = getW(-w[0]/w[x1+1]);
    		b1 = getH(max2);
    		b2 = getH(min2);
    		
    	}
    	g.setColor(Color.GREEN);
    	g.drawLine(a1, b1, a2, b2);
    }

	@Override
	public void keyPressed(KeyEvent e) { }

	@Override
	public void keyReleased(KeyEvent e) { }

	@Override
	public void keyTyped(KeyEvent e) {
		char c = e.getKeyChar();
		boolean wchanged = false;
		
		if (c == 'q' || c == 'Q') {
			w[0] += incr[0];
			wchanged = true;
		}
		else if (c == 'a' || c == 'A') {
			w[0] -= incr[0];
			wchanged = true;
		}
		else if (c == 'w' || c == 'W') {
			w[x1+1] += incr[1];
		wchanged = true;
		}
		else if (c == 's' || c == 'S') {
			w[x1+1] -= incr[1];
			wchanged = true;
		}
		else if (c == 'e' || c == 'E') {
			w[x2+1] += incr[2];
			wchanged = true;
		}
		else if (c == 'd' || c == 'D') {
			w[x2+1] -= incr[2];
			wchanged = true;
		}
		
		if (wchanged) {
			standardizeW();
			calculateLoss();
			this.paint(this.getGraphics());
		}
	}
	
    public static void main(String args[]) {
    	Visualizer viz = new Visualizer("./src/ml/data.txt");
    	viz.pack();
    	viz.setVisible(true);
    }

}
