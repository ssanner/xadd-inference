/* Symbolic Variable Elimination
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @author Ehsan Abbasnejad
 */
package sve;

import graph.Graph;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import sve.GraphicalModel.Factor;

import camdp.HierarchicalParser;
import xadd.XADD;
import xadd.ExprLib.DoubleExpr;
import xadd.ExprLib.ArithExpr;
import xadd.ExprLib.VarExpr;
import xadd.XADDUtils;

public class SVE {

	public final static HashMap<String,Boolean> EMPTY_BOOL = new HashMap<String,Boolean>();
	public final static HashMap<String,Double>  EMPTY_DOUBLE = new HashMap<String,Double>();
	
	public GraphicalModel _gm = null;
	public ArrayList<String> _alVariableOrder = null;
	public XADD _context      = null;
	public Query _lastQuery   = null;
	public ArrayList<Integer> _alVariableResultSaveNodes = new ArrayList<Integer>();
	
	public SVE(GraphicalModel gm) {
		_gm = gm;
		_context = _gm._context;
	}

	//////////////////////////////////////////////////////////////////////////////////
	//                             Main Inference Routines
	//////////////////////////////////////////////////////////////////////////////////

	public Factor infer(Query q) {
		return infer(q, null);
	}
	
	public Factor infer(Query q, ArrayList<String> var_ordering) {
	
		System.out.println(q._alQueryVars + " | " + q._hmCVarAssign + ", " + q._hmBVarAssign);
		
		// TODO: If known to be a Bayes net, can eliminate all descendants of query/evidence

		// TODO: Handle unassigned evidence (simply don't instantiate) -- note that this 
		//       will lead to a ratio of case statements (division not generally defined) 
		
		// TODO: Handle expectation for general algebraic expression
		
		// Set up model and variable ordering
		if (_lastQuery == null || !_lastQuery._hmVar2Expansion.equals(q._hmVar2Expansion)) {

			// Instantiate the graphical model (expand indexed variables into factors)
			_gm.instantiateGMTemplate(q._hmVar2Expansion);

			// Find best variable order -- build map from each variable to CPTs
			Graph g = new Graph(/*directed*/true, false, true, false);
			g.setBottomToTop(false);
			g.setMultiEdges(false); 
			for (Factor f : _gm._alFactors)
				g.addAllUniLinks(f._vars, f._vars);
			//g.launchViewer();			
			
			// Use a topological sort to find a good variable ordering
			if (var_ordering == null)
				_alVariableOrder = (ArrayList<String>)g.computeBestOrder();
			else 
				_alVariableOrder = var_ordering;
			System.out.println("Using best ordering: " + _alVariableOrder);
		}
		_lastQuery = q;
		_context = _gm._alFactors.get(0)._localContext;
		
		// Instantiate all assigned variables and add them to the factor list
		ArrayList<Factor> factors = new ArrayList<Factor>();
		for (Factor f : _gm._alFactors) {
			
			int instantiated_f = f._xadd;
			HashMap<String, ArithExpr> subst = new HashMap<String, ArithExpr>();
			
			for (String var : f._vars) {
				if (q._hmBVarAssign.containsKey(var)) {
					// Boolean assignment -- restrict
					boolean bassign = q._hmBVarAssign.get(var);
					instantiated_f = _context.opOut(instantiated_f, _context.getBoolVarIndex(var), 
							bassign ? XADD.RESTRICT_HIGH :  XADD.RESTRICT_LOW);
				} else if (q._hmCVarAssign.containsKey(var)) {
					// Continuous assignment -- add substitution
					double dassign = q._hmCVarAssign.get(var);
					subst.put(var, new DoubleExpr(dassign));
				}				
			}
			instantiated_f = _context.substitute(instantiated_f, subst);
			factors.add(_gm.new Factor(instantiated_f));
		}

		// Decide which variables to eliminate
		ArrayList<String> vars_to_eliminate = new ArrayList<String>();
		for (String var : _alVariableOrder)
			if (!q._alQueryVars.contains(var) && 
				!q._hmBVarAssign.containsKey(var) && 
				!q._hmCVarAssign.containsKey(var))
				vars_to_eliminate.add(var);
		
		// Do variable elimination
		ArrayList<Factor> factors_with_var = new ArrayList<Factor>();
		ArrayList<Factor> factors_without_var = new ArrayList<Factor>();
		for (String var : vars_to_eliminate) {
			System.out.println("Eliminating: " + var);
			
			// Split factors into sets that contain and do not contain the variable
			splitFactors(var, factors, factors_with_var, factors_without_var);
			System.out.println(" - with var: " + factors_with_var.size() + ", without var: " + factors_without_var.size());
			
			// Multiply factors that contain variable and marginalize out variable,
			// adding this new factor and all without the variable to the factors list
			Factor xadd_with_var = multiplyFactors(factors_with_var);
			System.out.println("Marginalizing out: " + _context.getString(xadd_with_var._xadd));
			Factor xadd_marginal = marginalizeOut(xadd_with_var, var);
			factors.clear();
			factors.addAll(factors_without_var);
			factors.add(xadd_marginal);
			System.out.println(" - remaining factors: " + factors.size());
			
			// Flush caches
			_context.clearSpecialNodes();
			for (Integer xadd : _alVariableResultSaveNodes)
				_context.addSpecialNode(xadd);
			for (Factor f : _gm._alFactors)
				_context.addSpecialNode(f._xadd);
			for (Factor f : factors)
				_context.addSpecialNode(f._xadd);
			_context.flushCaches();
		}
		
		// Done variable elimination, have a set of factors just over query vars,
		// need to compute normalizer
		Factor result = multiplyFactors(factors);
		Factor norm_result = normalize(result);
		System.out.println("Done: result " + norm_result._vars 
				/*+ ":\n" + _context.getString(norm_result._xadd)*/);
		
		// Data visualization and export for 1D and 2D cases
		// (see plot_local.m and plot3d.m for 1D and 2D plotting of output files in Matlab)
		if (norm_result._vars.size() == 1) {
			Visualize1DFactor(norm_result, "P(" + q._alQueryVars + " | " + 
					q._hmBVarAssign + ", " + q._hmCVarAssign + ")");
			ExportData(norm_result, q._sFilename + ".txt");
		} else if (norm_result._vars.size() == 2) {
			Visualize2DFactor(norm_result, "P(" + q._alQueryVars + " | " + 
					q._hmBVarAssign + ", " + q._hmCVarAssign + ")");
			Export3DData(norm_result, q._sFilename);
		}
		
		_alVariableResultSaveNodes.add(norm_result._xadd);
		
		return norm_result;
	}

	private void splitFactors(
		String split_var, ArrayList<Factor> factor_source,
		ArrayList<Factor> factors_with_var, ArrayList<Factor> factors_without_var) {
		
		factors_with_var.clear();
		factors_without_var.clear();
		for (Factor f : factor_source)
			if (f._vars.contains(split_var))
				factors_with_var.add(f);
			else
				factors_without_var.add(f);
	}
	
	private Factor multiplyFactors(ArrayList<Factor> factors) {
		int mult_xadd = _context.ONE;
		for (Factor f : factors)
			mult_xadd = _context.applyInt(mult_xadd, f._xadd, XADD.PROD);
		return _gm.new Factor(mult_xadd);
	}
	
	private Factor marginalizeOut(Factor f, String var) {
		
		// Take appropriate action based on whether var is boolean or continuous
		int bool_var_index = _context.getBoolVarIndex(var);
		int xadd_marginal = -1; 
		if (bool_var_index > 0) {
			// Sum out boolean variable
			int restrict_high = _context.opOut(f._xadd, bool_var_index, XADD.RESTRICT_HIGH);
			int restrict_low  = _context.opOut(f._xadd, bool_var_index, XADD.RESTRICT_LOW);
			xadd_marginal = _context.apply(restrict_high, restrict_low, XADD.SUM);
		} else {
			// Integrate out continuous variable
			xadd_marginal = _context.computeDefiniteIntegral(f._xadd, var);
		}
		return _gm.new Factor(xadd_marginal);
	}
	
	private Factor normalize(Factor f) {
		int xadd_norm = f._xadd;
		for (String var : f._vars)
			xadd_norm = _context.computeDefiniteIntegral(xadd_norm, var);
		double norm = _context.evaluate(xadd_norm, EMPTY_BOOL, EMPTY_DOUBLE);
		xadd_norm = _context.scalarOp(f._xadd, 1d/norm, XADD.PROD);
		return _gm.new Factor(xadd_norm);
	}
	
	public static double Get1DExpectedValue(Factor f) {
		if (f._vars.size() > 1)
			return Double.NaN;
		
		String var = f._vars.iterator().next();
		int xadd_norm = f._xadd;
		int xadd_var  = f._localContext.getTermNode(new VarExpr(var));
		int xadd_prod = f._localContext.apply(xadd_norm, xadd_var, XADD.PROD); 
		int result    = f._localContext.computeDefiniteIntegral(xadd_prod, var);
		return f._localContext.evaluate(result, EMPTY_BOOL, EMPTY_DOUBLE);
	}

	//////////////////////////////////////////////////////////////////////////////////
	//                       Visualization and Plotting Tools
	//////////////////////////////////////////////////////////////////////////////////
	
	public static void Visualize1DFactor(Factor norm_result, String title) {
		String var = norm_result._vars.iterator().next();
		double min_val = norm_result._localContext._hmMinVal.get(var);
		double max_val = norm_result._localContext._hmMaxVal.get(var);
		XADDUtils.PlotXADD(norm_result._localContext, norm_result._xadd, 
				min_val, 0.1d, max_val, var, title);
		double integral = XADDUtils.TestNormalize(norm_result._localContext, norm_result._xadd, var);
		if (Math.abs(integral - 1d) > 0.001d) 
			System.err.println("WARNING: distribition does not integrate out to 1: " + integral);
	}

	public void Visualize2DFactor(Factor norm_result, String title) {
		Iterator iter = norm_result._vars.iterator(); 
		String varX = (String)iter.next();
		String varY = (String)iter.next();
		double min_val_x = norm_result._localContext._hmMinVal.get(varX);
		double max_val_x = norm_result._localContext._hmMaxVal.get(varX);
		double min_val_y = norm_result._localContext._hmMinVal.get(varY);
		double max_val_y = norm_result._localContext._hmMaxVal.get(varY);
		XADDUtils.Plot3DSurfXADD(norm_result._localContext, norm_result._xadd, 
				min_val_x, 0.5d, max_val_x,
				min_val_y, 0.5d, max_val_y, 
				varX, varY, title);
	}

	private void ExportData(Factor norm_result, String filename) {
		PrintStream ps = null;
		try {
			ps = new PrintStream(new FileOutputStream(filename));
		} catch (Exception e) {
			System.err.println("Could not open " + filename + " for data export.");
			return;
		}
		String var = norm_result._vars.iterator().next();
		double low = norm_result._localContext._hmMinVal.get(var);
		double high = norm_result._localContext._hmMaxVal.get(var);
		
		HashMap<String,Double> dvars = new HashMap<String,Double>();

		ArrayList<Double> alX = new ArrayList<Double>();
		for (double x = low; x <= high; x += 0.1d)
			alX.add(x);
		
		double[] xArr = new double[alX.size()];
		double[] yArr = new double[alX.size()];
		for (int i = 0; i < alX.size(); i++) {
			double x = alX.get(i);
			dvars.put(var, x);
			double y = norm_result._localContext.evaluate(norm_result._xadd, EMPTY_BOOL, dvars);
			dvars.remove(var);
			
			ps.println(x + "\t" + y);
		}
		ps.close();
	}

	private void Export3DData(Factor norm_result, String filename) {
		Export3DData(norm_result, null, filename);
	}
		
	private void Export3DData(Factor norm_result, Factor divisor, String filename) {
		PrintStream ps_x = null;
		PrintStream ps_y = null;
		PrintStream ps_z = null;
		try {
			ps_x = new PrintStream(new FileOutputStream(filename + ".x.txt"));
			ps_y = new PrintStream(new FileOutputStream(filename + ".y.txt"));
			ps_z = new PrintStream(new FileOutputStream(filename + ".z.txt"));
		} catch (Exception e) {
			System.err.println("Could not open " + filename + " for data export.");
			return;
		}

		HashMap<String, Boolean> static_bvars = new HashMap<String, Boolean>();
		HashMap<String, Double>  static_dvars = new HashMap<String, Double>();
		
		Iterator iter = norm_result._vars.iterator(); 
		String varX = (String)iter.next();
		String varY = (String)iter.next();
		double low_x = norm_result._localContext._hmMinVal.get(varX);
		double high_x = norm_result._localContext._hmMaxVal.get(varX);
		double low_y = norm_result._localContext._hmMinVal.get(varY);
		double high_y = norm_result._localContext._hmMaxVal.get(varY);
		double inc_x = 0.5d;
		double inc_y = inc_x;
		
		// Create a Simple 2D XY plot window.
		ArrayList<Double> alX = new ArrayList<Double>();
		for (double x = low_x; x <= high_x; x += inc_x)
			alX.add(x);
		ArrayList<Double> alY = new ArrayList<Double>();
		for (double y = low_y; y <= high_y; y += inc_y)
			alY.add(y);

		double[][] xArr = new double[alY.size()][alX.size()];
		double[][] yArr = new double[alY.size()][alX.size()];
		double[][] zArr = new double[alY.size()][alX.size()];
		for (int i = 0; i < alY.size(); i++) {
			for (int j = 0; j < alX.size(); j++) {

				double x = alX.get(j);
				double y = alY.get(i);

				static_dvars.put(varX, x);
				static_dvars.put(varY, y);
				double z = norm_result._localContext.evaluate(norm_result._xadd, static_bvars, static_dvars);
				if (divisor != null) {
					//System.out.println(static_bvars + " " + static_dvars);
					//System.out.println(divisor._localContext.getString(divisor._xadd));
					z /= divisor._localContext.evaluate(divisor._xadd, static_bvars, static_dvars);
					if (Double.isInfinite(z) || Double.isNaN(z) || z < 0d)
						z = 0d;
				}
				static_dvars.remove(varX);
				static_dvars.remove(varY);

				xArr[i][j] = x;
				yArr[i][j] = y;
				zArr[i][j] = z; //x + y; //z;
				
				ps_x.print((j == 0 ? "" : "\t") + x);
				ps_y.print((j == 0 ? "" : "\t") + y);
				ps_z.print((j == 0 ? "" : "\t") + z);
			}
			ps_x.println();
			ps_y.println();
			ps_z.println();
		}
		
		ps_x.close();
		ps_y.close();
		ps_z.close();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		//GraphicalModel gm = new GraphicalModel("./src/sve/test.gm");
		//Query q = new Query("./src/sve/test.query");
		//Factor result = sve.infer(q);
		
		//TestLocalization();
		TestRadar();
	}

	public static void TestLocalization() {
		
		GraphicalModel gm = new GraphicalModel("./src/sve/tracking.gm");		
		SVE sve = new SVE(gm);
		
		Factor result1 = sve.infer(new Query("./src/sve/tracking.query.1"));
		Factor result2 = sve.infer(new Query("./src/sve/tracking.query.2"));
		Factor result3 = sve.infer(new Query("./src/sve/tracking.query.3"));
		Factor result4 = sve.infer(new Query("./src/sve/tracking.query.4"));
		Factor result5 = sve.infer(new Query("./src/sve/tracking.query.5"));
		Factor result6 = sve.infer(new Query("./src/sve/tracking.query.6"));
		
		System.out.println("Expected Value 1: " + Get1DExpectedValue(result1));
		System.out.println("Expected Value 2: " + Get1DExpectedValue(result2));
		System.out.println("Expected Value 3: " + Get1DExpectedValue(result3));
		System.out.println("Expected Value 4: " + Get1DExpectedValue(result4));
		System.out.println("Expected Value 5: " + Get1DExpectedValue(result5));
		System.out.println("Expected Value 6: " + Get1DExpectedValue(result6));
	}
	
	public static void TestRadar() {

		GraphicalModel gm = new GraphicalModel("./src/sve/radar.gm");
		SVE sve = new SVE(gm);
		//Query q = new Query("./src/sve/radar.query.1");
		//gm.instantiateGMTemplate(q._hmVar2Expansion);
		//System.out.println(gm);

		Query q1 = new Query("./src/sve/radar.query.5");
		Factor result1 = sve.infer(q1, CreateRadarVariableOrder(q1));

		Query q2 = new Query("./src/sve/radar.query.4");
		Factor result2 = sve.infer(q2, CreateRadarVariableOrder(q2));
		
		if (true) {
			sve.Export3DData(result2, result1, "./src/sve/radar.query.6");
		}
	}
	
	public static ArrayList<String> CreateRadarVariableOrder(Query q) {
		ArrayList<String> var_order = new ArrayList<String>();
//		for (Integer i : q._hmVar2Expansion.get("i")) {
//			var_order.add("b_" + i);
//			var_order.add("o_" + i);
//			var_order.add("x_" + i);
//		}
		for (Integer i : q._hmVar2Expansion.get("i")) {
			var_order.add("o_" + i);
		}
		for (Integer i : q._hmVar2Expansion.get("i")) {
			var_order.add("b_" + i);
		}
		for (Integer i : q._hmVar2Expansion.get("i")) {
			var_order.add("x_" + i);
		}
		return var_order;
	}
}
