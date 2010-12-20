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
	public String _sName; // Name of this action
	public HashMap<String, Integer> _hmVar2DD;

	/**
	 * Constructor
	 **/
	public Action(CMDP mdp, String name, HashMap<String, ArrayList> cpt_desc) {

		_mdp = mdp;
		_sName = name;
		_hmVar2DD = new HashMap<String, Integer>();
		buildAction(cpt_desc);
	}

	/**
	 * Build action description DDs
	 **/
	public void buildAction(HashMap<String,ArrayList> cpt_desc) {

		// TODO: Need to allow for boolean variables... these
		//       diagrams need dual action diagrams.  The boolean
		//       version just gets 
		
		// Head will be for current next-state
		for (Map.Entry<String,ArrayList> e : cpt_desc.entrySet()) {
			_hmVar2DD.put(e.getKey(), _mdp._context.buildCanonicalXADD(e.getValue()));
		}
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(_sName + ":\n");
		for (Map.Entry<String,Integer> me : _hmVar2DD.entrySet()) {
			XADD.XADDNode n = _mdp._context.getNode(me.getValue());
			sb.append("*****\n" + me.getKey() + " " + n.collectVars() + ":\n" + 
					  _mdp._context.getString(me.getValue()) + "\n");
		}
		sb.append("*****\n");
		return sb.toString();
	}
}
