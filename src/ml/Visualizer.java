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

public class Visualizer extends JFrame implements ActionListener, KeyListener{
    private Dimension dim = new Dimension(700, 700);
    private DataReader dr;
    private Timer timer;
    private double w0, w1, w2; // model parameters
    int border = 50;	// left, right, top, bottom border of screen
    double incr = 0.1;
    double scale = 1;	// graphic scale factor relative to input data range
    double max1 = 0, max2 = 0; // max1 = max value of x1
	double min1 = Double.MAX_VALUE, min2 = Double.MAX_VALUE; // min1 = min value of x1
	
	public Visualizer (String dataFileName) {
		
		this.setPreferredSize(dim);
		this.setTitle("01 Loss Visualizer. DataFile: " + dataFileName);
		this.addKeyListener(this);
		
		dr = new DataReader(dataFileName);
		if (dr.xDim() == 2) {
			initializeParams();
			timer = new Timer(1000 / 15, this);
			timer.start();
		}
		else {
			System.out.println("Sorry, visualizer only supports 2D data.");
		}
	}
	
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == timer) {
			paint(this.getGraphics());
		}
	}
	
    public void paint(Graphics g) {
    	drawAxes(g);
        drawDataPoints(g);
        drawBoundaryLine(g);
    }
    
    private void initializeParams() {
    	double x1m0=0, x1m1=0, x2m0=0, x2m1=0; // x1m0 = means of all x1 in class 0

    	int n0=0, n1=0; // n0 = number of rows in class 0
    	for (int i=0; i<dr.nRows(); i++) {
    		if (dr.y(i) <= 0) { //class 0
    			n0++;
    			x1m0 += dr.x(i, 0);
    			x2m0 += dr.x(i, 1);
    		}
    		else {
    			n1++;
    			x1m1 += dr.x(i, 0);
    			x2m1 += dr.x(i, 1);
    		}
    		// find min max
    		if (dr.x(i, 0) > max1) max1 = dr.x(i, 0);
    		if (dr.x(i, 0) < min1) min1 = dr.x(i, 0);
    		if (dr.x(i, 1) > max2) max2 = dr.x(i, 1);
    		if (dr.x(i, 1) < min2) min2 = dr.x(i, 1);
    	}
    	x1m0 = x1m0 / n0;
    	x2m0 = x2m0 / n0;
    	x1m1 = x1m1 / n1;
    	x2m1 = x2m1 / n1;
    	w1 = x1m1 - x1m0;
    	w2 = x2m1 - x2m0;
    	w0 = -w1 * (x1m0 + x1m1)/2 - w2 * (x2m0 + x2m1)/2;
    	scale = (dim.width - 2 * border) / Math.max(max1 - min1, max2 - min2);
    }
    
    private int getW(double x1) {
    	double width = border - min1 * scale + x1 * scale;
    	return (int) width;
    }
    
    private int getH(double x2) {
    	double height = border + max2 * scale - x2 * scale;
    	return (int) height;
    }
    
    private String dbl2Str(double x) {
    	DecimalFormat df = new DecimalFormat("0.00");
    	String s = (df.format(x));
    	return s;
    }
    
    private void drawAxes(Graphics g) {
    	g.clearRect(0, 0, dim.width, dim.height);
    	g.setColor(Color.GRAY);
    	g.drawLine(border, dim.height/2, dim.width - border, dim.height/2);
    	g.drawLine(dim.width/2, border, dim.width/2, dim.height - border);
    	g.drawLine(getW(min1), dim.height/2 - 2, getW(min1), dim.height/2 + 2);
    	g.drawString(dbl2Str(min1), getW(min1) - 10 , dim.height/2 + 15);
    	g.drawLine(getW(max1), dim.height/2 - 2, getW(max1), dim.height/2 + 2);
    	g.drawString(dbl2Str(max1), getW(max1) - 10 , dim.height/2 + 15);
    	g.drawLine(dim.width/2 - 2, getH(max2), dim.width/2 + 2, getH(max2));
    	g.drawString(dbl2Str(max2), dim.width/2 + 6 , getH(max2) + 4);  
    	g.drawLine(dim.width/2 - 2, getH(min2), dim.width/2 + 2, getH(min2));
    	g.drawString(dbl2Str(min2), dim.width/2 + 6 , getH(min2) + 4);
    	g.setColor(Color.BLACK);
    	g.drawString("w0: " + dbl2Str(w0) + "     w1: " + dbl2Str(w1) 
    			+ "     w2: " + dbl2Str(w2) 
    			+ "     [Use QA WS ED to tweak]", border, dim.height - 20);
    }
    
    private void drawDataPoints(Graphics g) {
    	for (int i=0; i<dr.nRows(); i++) {
    		if (dr.y(i) > 0) { // class 1
    			g.setColor(Color.RED);
    			g.drawLine(getW(dr.x(i, 0))-2, getH(dr.x(i, 1)), getW(dr.x(i, 0))+2, getH(dr.x(i, 1)));
    			g.drawLine(getW(dr.x(i, 0)), getH(dr.x(i, 1))-2, getW(dr.x(i, 0)), getH(dr.x(i, 1))+2);
    		}
    		else {
    			g.setColor(Color.BLUE);
    			g.drawOval(getW(dr.x(i, 0)), getH(dr.x(i, 1)), 4, 4);
    		}
    	}
    }
    
    private void drawBoundaryLine(Graphics g) {
    	int x1, y1, x2, y2;
    	if (w2 != 0) {
    		x1 = getW(min1);
    		y1 = getH((-w0-w1*min1)/w2);
    		x2 = getW(max1);
    		y2 = getH((-w0-w1*max1)/w2);
    	}
    	else {
    		x1 = x2 = getW(-w0/w1);
    		y1 = getH(max2);
    		y2 = getH(min2);
    		
    	}
    	g.setColor(Color.GREEN);
    	g.drawLine(x1, y1, x2, y2);
    }

	@Override
	public void keyPressed(KeyEvent e) { }

	@Override
	public void keyReleased(KeyEvent e) { }

	@Override
	public void keyTyped(KeyEvent e) {
		char c = e.getKeyChar();
		if (c == 'q' || c == 'Q')
			w0 += incr;
		else if (c == 'a' || c == 'A')
			w0 -= incr;
		else if (c == 'w' || c == 'W')
			w1 += incr;
		else if (c == 's' || c == 'S')
			w1 -= incr;
		else if (c == 'e' || c == 'E')
			w2 += incr;
		else if (c == 'd' || c == 'D')
			w2 -= incr;
	}
	
    public static void main(String args[]) {
    	Visualizer viz = new Visualizer("./src/ml/data3.txt");
    	viz.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	viz.pack();
    	viz.setVisible(true);
    }

}
