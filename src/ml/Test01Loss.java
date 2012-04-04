package ml;

import graph.Graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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

	DataReader dr;	//reads and stores classification data
	String dataFileName = "./src/ml/data2.txt";
	
	int buildXADDForRow(int i, XADD xadd_context) {
		String expr = Double.toString(dr.y(i)) + " * w0 ";
		for (int j=0; j<dr.xDim(); j++) {
			String term = Double.toString(Math.abs(dr.y(i) * dr.x(i, j)));
			term = term + " * w" + (j+1);
			if (dr.y(i) * dr.x(i,j) < 0) term = " - " + term;
			else term = " + " + term;
			expr = expr + term;
		}
		expr = "(" + expr + ")" + " < 0";
		ArrayList l = new ArrayList();
		ArrayList l1 = new ArrayList();
		ArrayList l2 = new ArrayList();
		l.add(expr);
		l1.add("1");
		l.add(l1);
		l2.add("0");
		l.add(l2);
		int xadd_id = xadd_context.buildCanonicalXADD(l);
		return xadd_id;
	}
	
	int build01LossXADD(XADD xadd_context) {
		
		int xadd_n = buildXADDForRow(0, xadd_context);
		for (int i=1; i<dr.nRows(); i++) {
			int xadd_i = buildXADDForRow(i, xadd_context);
			xadd_n = xadd_context.apply(xadd_n, xadd_i, XADD.SUM);
			xadd_n = xadd_context.reduceLP(xadd_n);
		}
		
		return xadd_n;
	}
	
	public Test01Loss() {
		
		dr = new DataReader(dataFileName);

		XADD xadd_context = new XADD();
		int dd1 = build01LossXADD(xadd_context);
		
		Graph g1 = xadd_context.getGraph(dd1);
		g1.launchViewer();
		
	}
	
	public static void main(String[] args) {
		new Test01Loss();
	}

}
