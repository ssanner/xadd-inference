package sve;

import graph.Graph;

import java.util.ArrayList;
import java.util.HashMap;

import sve.GraphicalModel.Factor;

import cmdp.HierarchicalParser;
import xadd.XADD;
import xadd.XADD.ArithExpr;
import xadd.XADD.VarExpr;

public class SVE {

	public GraphicalModel _gm = null;
	public ArrayList<String> _alVariableOrder = null;
	public XADD _context      = null;
	public Query _lastQuery   = null;
	
	public SVE(GraphicalModel gm) {
		_gm = gm;
		_context = _gm._context;
	}

	public Factor infer(Query q) {
	
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
			g.launchViewer();			
			
			// Use a topological sort to find a good variable ordering
			_alVariableOrder = (ArrayList<String>)g.computeBestOrder();
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
					subst.put(var, new XADD.DoubleExpr(dassign));
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
			Factor xadd_marginal = marginalizeOut(xadd_with_var, var);
			factors.clear();
			factors.addAll(factors_without_var);
			factors.add(xadd_marginal);
			System.out.println(" - remaining factors: " + factors.size());
		}
		
		// Done variable elimination, have a set of factors just over query vars,
		// need to compute normalizer
		Factor result = multiplyFactors(factors);
		Factor norm_result = normalize(result);
		System.out.println("Done: result " + norm_result._vars + ":\n" + _context.getString(norm_result._xadd));
		
		return norm_result;
	}

	public void splitFactors(
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
	
	public Factor multiplyFactors(ArrayList<Factor> factors) {
		int mult_xadd = _context.getTermNode(XADD.ONE);
		for (Factor f : factors)
			mult_xadd = _context.applyInt(mult_xadd, f._xadd, XADD.PROD);
		return _gm.new Factor(mult_xadd);
	}
	
	public Factor marginalizeOut(Factor f, String var) {
		
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
	
	public Factor normalize(Factor f) {
		int xadd_norm = f._xadd;
		for (String var : f._vars)
			xadd_norm = _context.computeDefiniteIntegral(xadd_norm, var);
		double norm = _context.evaluate(xadd_norm, 
			new HashMap<String,Boolean>(), new HashMap<String,Double>());
		xadd_norm = _context.scalarOp(xadd_norm, norm, XADD.PROD);
		return _gm.new Factor(xadd_norm);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		//GraphicalModel gm = new GraphicalModel("./src/sve/test.gm");
		//Query q = new Query("./src/sve/test.query");

		GraphicalModel gm = new GraphicalModel("./src/sve/tracking.gm");
		Query q = new Query("./src/sve/tracking.query.1");
		
		SVE sve = new SVE(gm);
		Factor result = sve.infer(q);
	}

}
