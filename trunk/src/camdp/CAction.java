package camdp;

import java.util.*;

import xadd.XADD;


public class CAction {

	public CAMDP _camdp; // MDP of which this action is a part
	public String _sName; // Name of this action
	public ArrayList<Double> _contBounds = new ArrayList<Double>(2);
	public HashMap<String, Integer> _hmVar2DD;
	public Integer _reward;
	public ArrayList<String> _contState;
	public ArrayList<String> _actionParam;
	public ArrayList<String> _boolVars;

	ArrayList<Boolean> _checkState;
	/**
	 * Constructor
	 * @param aVars 
	 * @param bVars 
	 **/
	public CAction(CAMDP mdp, String name, ArrayList<Double> params, HashMap<String, ArrayList> cpt_desc, Integer reward, ArrayList<String> cont, ArrayList<String> aVars, ArrayList<String> bVars) {

		_camdp = mdp;
		_sName = name;
		_hmVar2DD = new HashMap<String, Integer>();
		_reward = reward;
		_contState = cont;
		_actionParam = aVars;
		_contBounds = params;
		_boolVars = bVars;
		//to keep record of which variables already has bounds, if the variable is not mentioned, bounds on the action have to be added
		_checkState = new ArrayList<Boolean>();
		for (int i=0;i<_contState.size();i++)
			_checkState.add(i, false);
		
		buildAction(cpt_desc);
	}


	/**
	 * Build action description DDs
	 **/
	public void buildAction(HashMap<String,ArrayList> cpt_desc) {

		// Head will be for current next-state
		int xadd;
		for (Map.Entry<String,ArrayList> e : cpt_desc.entrySet()) {
			
			//adding action upper and lower bounds if available, 
			//TODO: add code here if we want to add the action constraints on the xadd (eg. a2>a1) 
			//reward and transition both use the action constraints in the max operation
			String var = e.getKey();
			String checkBound= var.substring(0, var.length() - 1);
			for (int i=0;i<_contState.size();i++)
				if (checkBound.equalsIgnoreCase(_contState.get(i)))
					_checkState.set(i, true);
			xadd = _camdp._context.buildCanonicalXADD(e.getValue());
					
			// If a boolean variable, need to construct dual action diagram
			if (_boolVars.contains(var.substring(0, var.length() - 1))) {
				int var_index = _camdp._context.getVarIndex( _camdp._context.new BoolDec(var/* + "'"*/), false);
				int high_branch = xadd;
				int low_branch = _camdp._context.apply(
						_camdp._context.getTermNode(XADD.ONE), high_branch, XADD.MINUS,-1);
				xadd = _camdp._context.getINode(var_index, low_branch, high_branch);
				xadd = _camdp._context.makeCanonical(xadd);
				_hmVar2DD.put(var, xadd);
			} else{
				_hmVar2DD.put(var, xadd);
				}
		}
				
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(_sName + " + params: ");
		for (int i=0;i< _contBounds.size();i++)
			sb.append( _contBounds.get(i) + " ");
		sb.append( ":\n");
		for (Map.Entry<String,Integer> me : _hmVar2DD.entrySet()) {
			XADD.XADDNode n = _camdp._context.getNode(me.getValue());
			sb.append("*****\n" + me.getKey() + " " + n.collectVars() + ":\n" + 
					  _camdp._context.getString(me.getValue()) + "\n");
		}
		
		XADD.XADDNode r = _camdp._context.getNode(_reward);
		sb.append("*****\nReward: " + r.collectVars() + ":\n" + 
				  _camdp._context.getString(_reward) + "\n");
		
		sb.append("*****\n");

		return sb.toString();
	}
}