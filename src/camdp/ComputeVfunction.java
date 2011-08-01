package camdp;

import graph.Graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import util.IntTriple;
import xadd.XADD;
import xadd.XADD.ArithExpr;
import xadd.XADD.BoolDec;
import xadd.XADD.DeltaFunctionSubstitution;
import xadd.XADD.XADDLeafMax;

public class ComputeVfunction {
	XADD xadd= null;
	CAMDP camdp = null;
	public final static boolean PRINTFINALQ = false;
	public HashMap<IntTriple,Integer> hashReg;
	public ComputeVfunction(XADD context,CAMDP camdp2)
	{
		xadd = context;
		camdp = camdp2;
		hashReg = camdp.get_hmRegrKey2Node();
	}
	
	/**
	 * Regress a DD through an action
	 **/
	public int computeValueFunction(int vfun, CAction a) {
      
		XADD.XADDNode n = xadd.getNode(vfun);
		HashSet<String> vars = n.collectVars();
		ArrayList<String> cvar_names = new ArrayList<String>();
		HashMap<String,Integer> bvar_dds = new HashMap<String,Integer>();
		HashMap<String,ArithExpr> prime_subs = new HashMap<String,ArithExpr>();
		ArrayList<XADD.XADDNode> node_list = new ArrayList<XADD.XADDNode>();
		ArrayList<XADD.ArithExpr> subst = new ArrayList<XADD.ArithExpr>();
		ArrayList<Integer> node_id_list = new ArrayList<Integer>();
		
		//Vars contains both state and action parameters. Take action parameters out
		//Action parameters can be in the decision tree, but not in the marginalization of the sum and integral 
		for (int i=0;i< a._actionParam.size();i++)
			for (int j=0;j<vars.size();j++)
				if (vars.contains(a._actionParam.get(i)))
					vars.remove(a._actionParam.get(i));
		
		//Also add the variables that do not have transitions but are bounded according to the action:
		for (int i=0;i< a._contState.size();i++)
			for (int j=0;j<vars.size();j++)
				if (!(vars.contains(a._contState.get(i))))
					vars.add(a._contState.get(i));
		
		// This relies on the fact all necessary variables have been properly
		// defined and that there are no name clashes between continuous vars
		// and boolean vars.
		for (String var : vars) {
			String var_prime = var + "'";
			prime_subs.put(var, new XADD.VarExpr(var_prime));
			Integer dd = a._hmVar2DD.get(var_prime);
			if (camdp.get_alBVars().contains(var)) {
				bvar_dds.put(var_prime, dd); // Note: var is unprimed
			} else {
				cvar_names.add(var_prime);
				node_list.add(xadd.getNode(dd)); //node_list has all XADDs from action a that are related with valueDD variables
				subst.add(null);
				node_id_list.add(dd);
			}
		}

		System.out.println("\n" + a._sName);
		System.out.println("BVars: " + bvar_dds.keySet());
		System.out.println("CVars: " + cvar_names);
		System.out.println("Node:  " + node_list);
		System.out.println("Subst: " + subst);
		
		

		// (1) we substitute variable primes (boolean and continuous): v -> v'
		// (2) we regress the continuous variables c' one at a time 
		// (3) we multiply in and sum out the CPTs for each b'
		int q = xadd.substitute(vfun, prime_subs); 
		
		// Do the *deterministic regression* for the continuous variables
		
		for (int i = 0; i < node_list.size(); i++) {
			
			IntTriple regr_key = new IntTriple(q, node_list.get(i).hashCode(), cvar_names.get(i).hashCode());
			Integer cache_node = null;
			if ((cache_node = hashReg.get(regr_key)) != null) {
				q = cache_node;
			} else {
			
				
				System.out.println("*** Regressing, size before: " + xadd.getNodeCount(q));
				//Regression regression = new Regression(xadd);
				//q = regression.regress(temp_node_list, temp_cvar_names, temp_subst, 0, q);//regress(_valueDD, a);
				q= xadd.reduceProcessXADDLeaf(node_id_list.get(i), xadd.new DeltaFunctionSubstitution(cvar_names.get(i), q), true);
				System.out.println("*** - size before makeCanonical: " + xadd.getNodeCount(q));
				q = xadd.makeCanonical(q);
				System.out.println("*** - size after: " + xadd.getNodeCount(q));
				
				hashReg.put(regr_key, q);
			}
		}
		
		// Do the *decision-theoretic regression* for the primed boolean variables
		// - multiply in CPT for v'
		// - marginalize out v'
		for (Map.Entry<String, Integer> e : bvar_dds.entrySet()) {
			String var_prime = e.getKey();
			int var_id = xadd.getVarIndex( xadd.new BoolDec(var_prime), false);
			Integer dd_mult = e.getValue();
			System.out.println("- Summing out: " + var_prime + "/" + 
					           var_id + " in\n" + xadd.getString(dd_mult));
			q = xadd.apply(q, dd_mult, XADD.PROD);
			
			// Following is a safer way to marginalize in the event that two branches
			// of a boolean variable had equal probability and were collapsed.
			//q = _context.opOut(q, var_id, XADD.SUM);
			int restrict_high = xadd.opOut(q, var_id, XADD.RESTRICT_HIGH);
			int restrict_low  = xadd.opOut(q, var_id, XADD.RESTRICT_LOW);
			q = xadd.apply(restrict_high, restrict_low, XADD.SUM);
		}
		
		// Multiply in discount and add reward
    	q = xadd.apply(a._reward, 
				xadd.scalarOp(q, camdp.get_bdDiscount().doubleValue(), XADD.PROD), 
				XADD.SUM);
    	
		for (Integer constraint : camdp.get_alConstraints()) {
			q = xadd.apply(q, constraint, XADD.PROD);
		}
		//************************** 
		//Continuous Action code
		//now we have the q_value at the leaves, 
		//we want to max-out according to each action parameter
		// Apply LB and UB for each action, use running max to keep the maximum action
		//TODO: for nonlinear, consider the differential 
		//Get leaves, compute LB,UB, take maximum of LB and UB, substitute leaf with it
		//LB= max_{over-a}(explicit_a bounds or default (-1000000),branches where a> exists)
		//UB = min_{over-a}(explicit_a bounds or default(100000), branches where a< exists)
		int oldq=0;
		q= computeMax(q, a, a._actionParam.get(0));
		for (int i=1;i<a._actionParam.size();i++)
		{
			oldq = q;
			q= computeMax(oldq, a, a._actionParam.get(i));
			//q = xadd.apply(q, oldq, XADD.MAX);
		}
		//*************************
		
		if(PRINTFINALQ){ 
			System.out.println("- Final Q(" + a._sName + "):\n" + xadd.getString(q));
		}
    	
		return q;
	}
	public XADD getXadd() {
		return xadd;
	}
	public void setXadd(XADD xadd) {
		this.xadd = xadd;
	}
	
	public HashMap<IntTriple, Integer> getHashReg() {
		return hashReg;
	}
	public void setHashReg(HashMap<IntTriple, Integer> hashReg) {
		this.hashReg = hashReg;
	}
	public int computeMax(int ixadd,CAction action, String _var) {
		XADDLeafMax max = xadd.new XADDLeafMax(action,_var);
		Graph g = xadd.getGraph(ixadd);
		g.addNode("_temp_");
		g.addNodeLabel("_temp_", "Q (before reduceProcessXADDLeaf");
		g.addNodeShape("_temp_", "square");
		g.addNodeStyle("_temp_", "filled");
		g.addNodeColor("_temp_", "lightblue");
		g.launchViewer(1300, 770);
		ixadd  = xadd.reduceProcessXADDLeaf(ixadd, max, false);
		g = xadd.getGraph(max._runningMax);
		g.addNode("_temp_");
		g.addNodeLabel("_temp_", "Q (before reduceLP)");
		g.addNodeShape("_temp_", "square");
		g.addNodeStyle("_temp_", "filled");
		g.addNodeColor("_temp_", "lightblue");
		g.launchViewer(1300, 770);
		max._runningMax = xadd.reduceLP(max._runningMax,camdp.contVars);
		 g = xadd.getGraph(max._runningMax);
		g.addNode("_temp_");
		g.addNodeLabel("_temp_", "Q after reduceLP");
		g.addNodeShape("_temp_", "square");
		g.addNodeStyle("_temp_", "filled");
		g.addNodeColor("_temp_", "lightblue");
		g.launchViewer(1300, 770);
		//no difference was made here after canonical
		//max._runningMax = xadd.makeCanonical(max._runningMax);
		//if a decision consisting of the action is negative, make it positive
		
		/* g = xadd.getGraph(max._runningMax);
			g.addNode("_temp_");
			g.addNodeLabel("_temp_", "Q after makeCanonical");
			g.addNodeShape("_temp_", "square");
			g.addNodeStyle("_temp_", "filled");
			g.addNodeColor("_temp_", "lightblue");
			g.launchViewer(1300, 770);*/
		return max._runningMax;
	}
}
