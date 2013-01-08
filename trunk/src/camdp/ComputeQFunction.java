package camdp;

import graph.Graph;

import java.util.*;

import util.IntTriple;
import xadd.XADD;
import xadd.XADD.*;

public class ComputeQFunction {

	public XADD _context = null;
	public CAMDP _camdp  = null;

	public ComputeQFunction(XADD context ,CAMDP camdp)
	{
		_context  = context;
		_camdp = camdp;
	}
	
	private IntTriple _contRegrKey = new IntTriple(-1,-1,-1);
	
	/**
	 * Regress a DD through an action
	 **/
	public int regress(int vfun, CAction a) {
      
		XADDNode n = _context.getNode(vfun);
		
		// What state variables in vfun do we have to regress?  
		// Note: no action params in vfun.
		HashSet<String> state_vars_in_vfun  = n.collectVars();
		_camdp._logStream.println("** Regressing " + a._sName + "\n- State vars in vfun: " + state_vars_in_vfun);
		
		// Prime the value function
		int q = _context.substitute(vfun, _camdp._hmPrimeSubs); 
		_camdp._logStream.println("- Primed value function:\n" + _context.getString(q));

		// Regress continuous variables first in order given in 
		for (String var : state_vars_in_vfun) {
			if (!_camdp._hsContSVars.contains(var))
				continue; // Not regressing boolean variables yet, skip

			// Get cpf for continuous var'
			String var_prime = var + "'";
			int var_id = _context.getVarIndex( _context.new BoolDec(var_prime), false);
			Integer dd_conditional_sub = a._hmVar2DD.get(var_prime);

			_camdp._logStream.println("- Integrating out: " + var_prime + "/" + var_id + " in\n" + _context.getString(dd_conditional_sub));

			// Check cache
			_contRegrKey.set(var_id, dd_conditional_sub, q);
			Integer result = null;
			if ((result = _camdp._hmContRegrCache.get(_contRegrKey)) != null) {
				q = result;
				continue;
			}
			
			// Perform regression via delta function substitution
			q = _context.reduceProcessXADDLeaf(dd_conditional_sub, 
					_context.new DeltaFunctionSubstitution(var_prime, q), true);
			
			// Cache result
			_camdp._logStream.println("-->: " + _context.getString(q));
			_camdp._hmContRegrCache.put(new IntTriple(_contRegrKey), q);
		}
		
		// Regress boolean variables second
		for (String var : state_vars_in_vfun) {
			if (!_camdp._hsBoolSVars.contains(var))
				continue; // Continuous variables already regressed, skip
		
			// Get cpf for boolean var'
			String var_prime = var + "'";
			int var_id = _context.getVarIndex( _context.new BoolDec(var_prime), false);
			Integer dd_cpf = a._hmVar2DD.get(var_prime);
			
			_camdp._logStream.println("- Summing out: " + var_prime + "/" + var_id + " in\n" + _context.getString(dd_cpf));
			q = _context.apply(q, dd_cpf, XADD.PROD);
			
			// Following is a safer way to marginalize (instead of using opOut
			// based on apply) in the event that two branches of a boolean variable 
			// had equal probability and were collapsed.
			int restrict_high = _context.opOut(q, var_id, XADD.RESTRICT_HIGH);
			int restrict_low  = _context.opOut(q, var_id, XADD.RESTRICT_LOW);
			q = _context.apply(restrict_high, restrict_low, XADD.SUM);
		}
		
		// TODO: Policy maintenance: currently unfinished in this version
		// - if no action variables, can just annotate each Q-function with action
		// - if action variables then need to maintain action name along with
		//   the substitutions made at the leaves (which can occur recursively for
		//   multivariable problems)
		// if (_camdp.MAINTAIN_POLICY) { 
		//      ... 
		// }
			
		// Multiply in discount and add reward
    	q = _context.apply(a._reward,  
				_context.scalarOp(q, _camdp._bdDiscount.doubleValue(), XADD.PROD), 
				XADD.SUM);	

    	// Ensure Q-function is properly constrained and minimal (e.g., subject to constraints)
		for (Integer constraint : _camdp._alConstraints)
			q = _context.apply(q, constraint, XADD.PROD);
		if (_camdp._alConstraints.size() > 0)
			q = _context.reduceLP(q);
		
		// Optional Display
		_camdp._logStream.println("- Q^" + _camdp._nCurIter + "(" + a._sName + ", " + a._actionParam + " )\n" + _context.getString(q));
		if (CAMDP.DISPLAY_PREMAX_Q)
			_camdp.doDisplay(q, "Q-" + a._sName + "-" + a._actionParam + "^" + _camdp._nCurIter + "-" + Math.round(100*_camdp.APPROX_ERROR));
		
		// Continuous action parameter maximization
		for (int i=0; i < a._actionParam.size(); i++) {
			String var = a._actionParam.get(i);
			double lb  = a._contBounds.get(i*2);
			double ub  = a._contBounds.get(i*2+1);

			_camdp._logStream.println("- Maxing out var '" + var + "': [" + lb + "," + ub + "]");
			q = maxOutVar(q, var, lb, ub);
			_camdp._logStream.println("-->: " + _context.getString(q));
			
			// Can be computational expensive (max-out) so flush caches if needed
			_camdp.flushCaches(Arrays.asList(q) /* additional node to save */);
		}
		_camdp._logStream.println("- Q^" + _camdp._nCurIter + "(" + a._sName + " )\n" + _context.getString(q));

		return q;
	}
	
	public int maxOutVar(int ixadd, String var, double lb, double ub) {
		XADDLeafMax max = _context.new XADDLeafMax(var, lb, ub, _camdp._logStream);
		ixadd  = _context.reduceProcessXADDLeaf(ixadd, max, false);
		return max._runningMax;
	}
}
