//////////////////////////////////////////////////////////////////////
//
// File:     Action.java
// Author:   Scott Sanner, University of Toronto (ssanner@cs.toronto.edu)
// Date:     9/1/2003
// Requires: comshell package
//
// Description:
//
//   Class for action representation/construction.
//
//////////////////////////////////////////////////////////////////////

// Package definition
package camdp;

// Packages to import

import java.util.*;

import xadd.XADD;


/**
 * Class for action representation/construction.
 * 
 * @version 1.0
 * @author Scott Sanner
 * @language Java (JDK 1.3)
 **/
public class CAction {
	/* Local constants */

	/* Local vars */
	public CAMDP _mdp; // MDP of which this action is a part
	public String _sName; // Name of this action
	public ArrayList<Double> _contBounds = new ArrayList<Double>(2);
	public HashMap<String, Integer> _hmVar2DD;
	public Integer _reward;
	public ArrayList<String> _contState;
	public ArrayList<String> _actionParam;

	ArrayList<Boolean> _checkState;
	/**
	 * Constructor
	 * @param aVars 
	 **/
	public CAction(CAMDP mdp, String name, ArrayList<Double> params, HashMap<String, ArrayList> cpt_desc, Integer reward, ArrayList<String> cont, ArrayList<String> aVars) {

		_mdp = mdp;
		_sName = name;
		_hmVar2DD = new HashMap<String, Integer>();
		_reward = reward;
		_contState = cont;
		_actionParam = aVars;
		_contBounds = params;
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
			xadd = _mdp._context.buildCanonicalXADD(e.getValue());
					
			// If a boolean variable, need to construct dual action diagram
			//System.out.println(var + " vs " + _mdp._alBVars);
			if (_mdp._alBVars.contains(var.substring(0, var.length() - 1))) {
				int var_index = _mdp._context.getVarIndex( _mdp._context.new BoolDec(var/* + "'"*/), false);
				int high_branch = xadd;
				int low_branch = _mdp._context.apply(
						_mdp._context.getTermNode(XADD.ONE), high_branch, XADD.MINUS);
				xadd = _mdp._context.getINode(var_index, low_branch, high_branch);
				xadd = _mdp._context.makeCanonical(xadd);
				_hmVar2DD.put(var, xadd);
			} else{
				_hmVar2DD.put(var, xadd);
				}
		}
		//now check which variables did not have any next state because they have continuous action,
		//to add bounds to them. Also if this action is continuous this has to be done
		/*for (int i=0;i<_checkState.size();i++)
			if ((_checkState.get(i)==false)&&(_actionParam.size()>0))
			{
				String var = _contState.get(i)+"'";
				String act = _actionParam.get(i);
				ArrayList actionBound1 = new ArrayList();
				ArrayList actionBound2 = new ArrayList();
				ArrayList actionBound3 = new ArrayList();
				ArrayList actionBound4 = new ArrayList();
				ArrayList actionBound5 = new ArrayList();
				
				
				actionBound1.add("["+act +">=-1000000]");
				actionBound2.add("["+act +"<=1000000]");
				
				actionBound3.add("["+var+"["+act +"]");
				actionBound2.add(actionBound3);
				
				actionBound4.add("["+var +"]");
				actionBound2.add(actionBound4);
				actionBound1.add(actionBound2);
				
				actionBound5.add("["+var +"]");
				actionBound1.add(actionBound5);
				
				
				xadd = _mdp._context.buildCanonicalXADD(actionBound1);
				_hmVar2DD.put(var, xadd); 
			}*/
		
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(_sName + " + params: ");
		for (int i=0;i< _contBounds.size();i++)
			sb.append( _contBounds.get(i) + " ");
		sb.append( ":\n");
		for (Map.Entry<String,Integer> me : _hmVar2DD.entrySet()) {
			XADD.XADDNode n = _mdp._context.getNode(me.getValue());
			sb.append("*****\n" + me.getKey() + " " + n.collectVars() + ":\n" + 
					  _mdp._context.getString(me.getValue()) + "\n");
		}
		
		XADD.XADDNode r = _mdp._context.getNode(_reward);
		sb.append("*****\nReward: " + r.collectVars() + ":\n" + 
				  _mdp._context.getString(_reward) + "\n");
		
		sb.append("*****\n");

		return sb.toString();
	}
}
