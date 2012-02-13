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
package cmdp;

// Packages to import
import java.io.*;
import java.util.*;

import xadd.XADD;
import xadd.XADD.ArithExpr;
import xadd.XADD.BoolDec;

/**
 * Class for action representation/construction.
 * 
 * @version 1.0
 * @author Scott Sanner
 * @language Java (JDK 1.3)
 **/
public class Action {
	/* Local constants */

	/* Local vars */
	public CMDP _mdp; // MDP of which this action is a part
	public CPOMDP _pomdp;
	public String _sName; // Name of this action
	public HashMap<String, Integer> _hmVar2DD;
	public HashMap<String, Integer> _hmObs2DD;
	public Integer _reward;

	/**
	 * Constructor
	 **/
	public Action(CMDP mdp, String name, HashMap<String, ArrayList> cpt_desc, Integer reward) {

		_mdp = mdp;
		_sName = name;
		_hmVar2DD = new HashMap<String, Integer>();
		_reward = reward;
		buildAction(cpt_desc);
	}

	public Action(CPOMDP pomdp, String name, HashMap<String, ArrayList> cpt_desc,HashMap<String, ArrayList> obs_desc, Integer reward) {

		_pomdp = pomdp;
		_sName = name;
		_hmVar2DD = new HashMap<String, Integer>();
		_hmObs2DD = new HashMap<String, Integer>();
		_reward = reward;
		buildAction(cpt_desc,obs_desc);
	}

	/**
	 * Build action description DDs
	 **/
	public void buildAction(HashMap<String,ArrayList> cpt_desc) {

		// Head will be for current next-state
		for (Map.Entry<String,ArrayList> e : cpt_desc.entrySet()) {

			String var = e.getKey();
			int xadd = _mdp._context.buildCanonicalXADD(e.getValue());

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
	}

	public void buildAction(HashMap<String,ArrayList> cpt_desc,HashMap<String,ArrayList> obs_desc) {

		// Head will be for current next-state
		for (Map.Entry<String,ArrayList> e : cpt_desc.entrySet()) {

			String var = e.getKey();
			int xadd = _pomdp._context.buildCanonicalXADD(e.getValue());

			// If a boolean variable, need to construct dual action diagram
			//System.out.println(var + " vs " + _mdp._alBVars);
			if (_pomdp._alBVars.contains(var.substring(0, var.length() - 1))) {
				int var_index = _pomdp._context.getVarIndex( _pomdp._context.new BoolDec(var/* + "'"*/), false);
				int high_branch = xadd;
				int low_branch = _mdp._context.apply(
						_pomdp._context.getTermNode(XADD.ONE), high_branch, XADD.MINUS);
				xadd = _pomdp._context.getINode(var_index, low_branch, high_branch);
				xadd = _pomdp._context.makeCanonical(xadd);
				_hmVar2DD.put(var, xadd);
			} else{
				_hmVar2DD.put(var, xadd);
			}
		}

		for (Map.Entry<String,ArrayList> e : obs_desc.entrySet()) {

			String var = e.getKey();
			int xadd = _pomdp._context.buildCanonicalXADD(e.getValue());

			// If a boolean variable, need to construct dual action diagram
			//System.out.println(var + " vs " + _mdp._alBVars);
			if (_pomdp._alBVars.contains(var.substring(0, var.length() - 1))) {
				int var_index = _pomdp._context.getVarIndex( _pomdp._context.new BoolDec(var/* + "'"*/), false);
				int high_branch = xadd;
				int low_branch = _mdp._context.apply(
						_pomdp._context.getTermNode(XADD.ONE), high_branch, XADD.MINUS);
				xadd = _pomdp._context.getINode(var_index, low_branch, high_branch);
				xadd = _pomdp._context.makeCanonical(xadd);
				_hmObs2DD.put(var, xadd);
			} else{
				_hmObs2DD.put(var, xadd);
			}
		}
	}


	public String toString() 
	{
		if (_hmObs2DD.size()>0)
		{
			StringBuffer sb = new StringBuffer();
			sb.append(_sName + ":\n");
			for (Map.Entry<String,Integer> me : _hmVar2DD.entrySet()) {
				XADD.XADDNode n = _pomdp._context.getNode(me.getValue());
				sb.append("*****\n" + me.getKey() + " " + n.collectVars() + ":\n" + 
						_pomdp._context.getString(me.getValue()) + "\n");
			}
			for (Map.Entry<String,Integer> me : _hmObs2DD.entrySet()) 
			{
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
		else 
		{
			StringBuffer sb = new StringBuffer();
			sb.append(_sName + ":\n");
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
}
