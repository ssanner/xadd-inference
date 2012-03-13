package CPOMDP;

import java.util.*;

import xadd.XADD;


public class COAction {

	public CPOMDP _pomdp; // MDP of which this action is a part
	public String _sName; // Name of this action
	public ArrayList<Double> _contBounds = new ArrayList<Double>(2);
	public HashMap<String, Integer> _hmVar2DD;
	public HashMap<String, Integer> _hmObs2DD;
	public Integer _reward;
	public ArrayList<String> _contState;
	public ArrayList<String> _contObs;
	public ArrayList<String> _boolVars;
	public ArrayList<String> _boolObs;
	/**
	 * Constructor
	 * @param aVars 
	 * @param bVars 
	 **/
	public COAction(CPOMDP pomdp, String name, HashMap<String, ArrayList> cpt_desc,HashMap<String, ArrayList> cpt_obs, Integer reward, ArrayList<String> cont, ArrayList<String> oVars, ArrayList<String> bVars,ArrayList<String> boVars) {

		_pomdp = pomdp;
		_sName = name;
		_hmVar2DD = new HashMap<String, Integer>();
		_hmObs2DD = new HashMap<String, Integer>();
		_reward = reward;
		_contState = cont;
		_contObs = oVars;
		_boolVars = bVars;
		_boolObs = boVars;
		buildAction(cpt_desc);
		buildObservation(cpt_obs);
	}


	/**
	 * Build action description DDs for transition and observation
	 **/
	public void buildAction(HashMap<String,ArrayList> cpt_desc) 
	{

		// Head will be for current next-state
		int xadd;
		for (Map.Entry<String,ArrayList> e : cpt_desc.entrySet()) {
			
			//adding action upper and lower bounds if available, 
			//TODO: add code here if we want to add the action constraints on the xadd (eg. a2>a1) 
			//reward and transition both use the action constraints in the max operation
			String var = e.getKey();
			xadd = _pomdp._context.buildCanonicalXADD(e.getValue());
					
			// If a boolean variable, need to construct dual action diagram
			if (_boolVars.contains(var.substring(0, var.length() - 1))) {
				int var_index = _pomdp._context.getVarIndex( _pomdp._context.new BoolDec(var/* + "'"*/), false);
				int high_branch = xadd;
				int low_branch = _pomdp._context.apply(
						_pomdp._context.getTermNode(XADD.ONE), high_branch, XADD.MINUS);
				xadd = _pomdp._context.getINode(var_index, low_branch, high_branch);
				xadd = _pomdp._context.makeCanonical(xadd);
				_hmVar2DD.put(var, xadd);
			} else{
				_hmVar2DD.put(var, xadd);
				}
		}
				
	}
	
	public void buildObservation(HashMap<String,ArrayList> cpt_obs) {

		// Head will be for current next-state
		int xadd;
		for (Map.Entry<String,ArrayList> e : cpt_obs.entrySet()) {

			String var = e.getKey();			
			xadd = _pomdp._context.buildCanonicalXADD(e.getValue());
					
			// If a boolean variable, need to construct dual action diagram
			if (_boolObs.contains(var.substring(0, var.length() - 1))) {
				int var_index = _pomdp._context.getVarIndex( _pomdp._context.new BoolDec(var/* + "'"*/), false);
				int high_branch = xadd;
				int low_branch = _pomdp._context.apply(
						_pomdp._context.getTermNode(XADD.ONE), high_branch, XADD.MINUS);
				xadd = _pomdp._context.getINode(var_index, low_branch, high_branch);
				xadd = _pomdp._context.makeCanonical(xadd);
				_hmObs2DD.put(var, xadd);
			} else{
				_hmObs2DD.put(var, xadd);
				}
		}
				
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(_sName + " ( ");
		sb.append("):\n");
		for (Map.Entry<String,Integer> me : _hmVar2DD.entrySet()) {
			XADD.XADDNode n = _pomdp._context.getNode(me.getValue());
			sb.append("*****\n" + me.getKey() + " " + n.collectVars() + ":\n" + 
					  _pomdp._context.getString(me.getValue()) + "\n");
		}
		for (Map.Entry<String,Integer> me : _hmObs2DD.entrySet()) {
			XADD.XADDNode n = _pomdp._context.getNode(me.getValue());
			sb.append("*****\n" + me.getKey() + " " + n.collectVars() + ":\n" + 
					  _pomdp._context.getString(me.getValue()) + "\n");
		}
		XADD.XADDNode r = _pomdp._context.getNode(_reward);
		sb.append("*****\nReward: " + r.collectVars() + ":\n" + 
				  _pomdp._context.getString(_reward) + "\n");
		
		sb.append("*****\n");

		return sb.toString();
	}
}