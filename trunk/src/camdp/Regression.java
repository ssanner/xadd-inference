package camdp;

import java.util.ArrayList;
import java.util.HashMap;

import xadd.XADD;
import xadd.XADD.ArithExpr;

public class Regression {
	
	public final static boolean DISPLAY_SUBST = false;
	XADD xadd = null;
	
	public Regression(XADD context)
	{
		xadd = context;
	}

	// entry point for descending on branches of next variable
	// or simply making substitutions if all variables to branch on are exhausted
	// TODO: Deal with non-canonical XADD result (call reduce)
	public int regress(ArrayList<XADD.XADDNode> node_list, 
		ArrayList<String> var_names, ArrayList<XADD.ArithExpr> subst, 
		int index, int vfun) {
		
		// Check if at terminal
		if (index >= node_list.size()) {
		    // Make substitution
			HashMap<String,ArithExpr> leaf_subs = new HashMap<String,ArithExpr>();
			for (int i = 0; i < var_names.size(); i++) 
				leaf_subs.put(var_names.get(i), subst.get(i));
			if(DISPLAY_SUBST){ 
							System.out.println("Substituting: " + leaf_subs + 
					"\ninto:" + xadd.getString(vfun));
			}
			int sub_dd = xadd.substitute(vfun, leaf_subs);
			//Graph gr = _context.getGraph(sub_dd);
			//gr.launchViewer(1300, 770);
			return sub_dd;
		}
		
		// Must be nonterminal so continue to recurse
		return regress2(node_list.get(index), node_list, var_names, subst, index, vfun);
	}
	
	// TODO: Deal with non-canonical XADD result (call reduce)
	// Recurses to leaves and returns substituted XADD... branches on
	// current diagram for current node until a leaf is reached indicating
	// a substitution... subst recorded then regress called with advance
	// to next variable transition
	public int regress2(XADD.XADDNode cur, ArrayList<XADD.XADDNode> node_list, 
			ArrayList<String> var_names, ArrayList<XADD.ArithExpr> subst, 
			int index, int vfun) {
		
		if (cur instanceof XADD.XADDINode) {
			// Branch each way and recombine in new result
			XADD.XADDINode inode = (XADD.XADDINode)cur;
			XADD.XADDNode low = xadd.getNode(inode._low);
			int new_low = regress2(low, node_list, var_names, subst, index, vfun);
			XADD.XADDNode high = xadd.getNode(inode._high);
			int new_high = regress2(high, node_list, var_names, subst, index, vfun);
			
			// TODO: Deal with non-canonical XADD (ordering could change due to subst)
			return xadd.getINode(inode._var, new_low, new_high);
			
		} else { 
			// Terminal, so identify substition and continue to next index
			XADD.XADDTNode tnode = (XADD.XADDTNode)cur;
			subst.set(index, tnode._expr);
			// This substitution uncovered, advance to next index
			int ret_node = regress(node_list, var_names, subst, index + 1, vfun);
			subst.set(index, null);
			return ret_node;
		}
	}
}
