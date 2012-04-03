package ml;

import graph.Graph;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import xadd.XADD;
import xadd.XADD.ArithExpr;
import xadd.XADD.BoolDec;
import xadd.XADD.CompExpr;
import xadd.XADD.Decision;
import xadd.XADD.DeltaFunctionSubstitution;
import xadd.XADD.DoubleExpr;
import xadd.XADD.ExprDec;
import xadd.XADD.VarExpr;
import xadd.XADD.XADDINode;
import xadd.XADD.XADDLeafMax;
import xadd.XADD.XADDTNode;
import cmdp.HierarchicalParser;

public class Test01Loss {

	String dataFileName = "./src/ml/data2.txt";
	
	double[][] x;	// array of input data
	double[] y;		// array of targets
	int N=0, D=0;	// N = number of records; D = input data dimension
	
	void readData() {
		// read all input data to linked list data
		LinkedList<double[]> data = new LinkedList<double[]>();
		try {             
			BufferedReader reader = new BufferedReader( new FileReader(dataFileName));             
			String strLine = null;                                
			while((strLine = reader.readLine()) != null) {                              
				String[] nums = strLine.split(",");
				if (D == 0) D = nums.length;
				if (D != nums.length)
					System.out.println("Wrong data dimension at line " + (N+1));
				else {
					double[] row = new double[D];
					for (int j=0; j<nums.length; j++)
						row[j] = Double.parseDouble(nums[j]);
					data.add(row);
					N++;
				}
			}
		}
		catch (IOException e) { e.printStackTrace(); } 
		
		// assign data to array x,y
		x = new double[N][D-1];
		y = new double[N];
		int i = 0;
		Iterator<double[]> itr = data.iterator();
		while (itr.hasNext()) {
			double[] row = itr.next();
			for (int j=0; j<D-1; j++) 
				x[i][j] = row[j];
			y[i] = row[D-1];
			if (y[i] <= 0) y[i] = -1;	//forces y[i] = -1 or 1
			if (y[i] > 0) y[i] = 1;
			i++;
		}
		System.out.println(N + "x" + D + " input data have been read!");
	}
	
	int buildXADDForRow(int i, XADD xadd_context) {
		String expr = Double.toString(y[i]) + " * w0 ";
		for (int j=0; j<D-1; j++) {
			String term = Double.toString(Math.abs(y[i] * x[i][j]));
			term = term + " * w" + (j+1);
			if (y[i] * x[i][j] < 0) term = " - " + term;
			else term = " + " + term;
			expr = expr + term;
		}
		expr = "(" + expr + ")" + " < 0";
		ArrayList l = new ArrayList();
		ArrayList l1 = new ArrayList();
		ArrayList l2 = new ArrayList();
		l.add(expr);
		l1.add("1");
		l2.add("0");
		l.add(l1);
		l.add(l2);
		int xadd_id = xadd_context.buildCanonicalXADD(l);
		return xadd_id;
	}
	
	int build01LossXADD(XADD xadd_context) {
		
		int xadd_n = buildXADDForRow(0, xadd_context);
		for (int i=1; i<N; i++) {
			int xadd_i = buildXADDForRow(i, xadd_context);
			xadd_n = xadd_context.apply(xadd_n, xadd_i, XADD.SUM);
			xadd_n = xadd_context.reduceLP(xadd_n);
		}
		
		return xadd_n;
	}
	
	public Test01Loss() {
		
		readData();

		XADD xadd_context = new XADD();
		int dd1 = build01LossXADD(xadd_context);
		
		Graph g1 = xadd_context.getGraph(dd1);
		g1.launchViewer();
		
		//int dd2 = xadd_context.reduceLP(dd1);
		
	}
	
	public static void main(String[] args) {
		new Test01Loss();
	}

}
