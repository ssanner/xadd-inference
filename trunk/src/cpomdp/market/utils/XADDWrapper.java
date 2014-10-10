/**
 * 
 */
package cpomdp.market.utils;

import java.util.HashMap;
import java.util.HashSet;

import xadd.ExprLib.ArithExpr;
import xadd.XADD;
import xadd.XADD.BoolDec;
import xadd.XADD.Decision;
import xadd.XADD.XADDTNode;

/**
 * XADD Wrapper class. Implemented as a Singleton to ensure that only one XADD
 * "context" is used.
 * 
 * @author	Shamin Kinathil 
 * @since	2014-09-03	
 */
public class XADDWrapper {
	
	/**
	 * 
	 * @author skinathil
	 *
	 */
	private static class XADDHolder {
		private static final XADD Instance = new XADD();
	}
	
	/**
	 * 
	 * @return
	 */
	public static XADD getInstance() {
		return XADDHolder.Instance;
	}
	
	/**
	 * 
	 */
    private XADDWrapper() {    	
    }

    /**
     * 
     * @param xadd1
     * @param xadd2
     * @param operator
     * @return
     */
    public static int Apply(Integer xadd1, Integer xadd2, XADDOperator operator) {
    	return XADDWrapper.getInstance().apply(xadd1, xadd2, operator.getValue());
    }        

	/**
	 * 
	 * @param dd
	 * @param val
	 * @param operator
	 * @return
	 */
	public static Integer ScalarOp(Integer dd, Double val, XADDOperator operator) {
		return XADDWrapper.getInstance().scalarOp(dd, val, operator.getValue());
	}    	
	
    /**
     * 
     * @param valueXADD
     * @param stateSetMap
     * @return
     */
	public static int substitute(Integer node_id, HashMap<String, ArithExpr> subst) {
		return XADDWrapper.getInstance().substitute(node_id, subst);
	}	
	
	/**
	 * 
	 * @param variableName
	 * @return
	 */
    public static BoolDec BoolDec(String variableName) {
    	return (XADDWrapper.getInstance()).new BoolDec(variableName);
    }	
	
    /**
     * 
     * @param xadd
     * @param int_var
     * @return
     */
    public static int computeDefiniteIntegral(int xadd, String int_var) {
    	return XADDWrapper.getInstance().computeDefiniteIntegral(xadd, int_var);
    }
    
    /**
     * 
     * @param node_id
     * @return
     */
    public static XADDTNode getNode(int node_id) {
    	return (XADDTNode) XADDWrapper.getInstance().getNode(node_id);
    }
    
    /**
     * 
     * @param d
     * @param create
     * @return
     */
    public static Integer getVarIndex(Decision d, Boolean create) { 
    	return XADDWrapper.getInstance().getVarIndex(d, false);
    }
    
    /**
     * 
     * @param xadd
     * @return
     */
    public static HashSet<String> collectVars(int xadd) {
    	return XADDWrapper.getInstance().collectVars(xadd);
    }
    
    /**
     * 
     * @param node_id
     * @param var_id
     * @param op
     * @return
     */
    public static int opOut(int node_id, int var_id, int op) {
    	return XADDWrapper.getInstance().opOut(node_id, var_id, op);
    }
    
    /**
     * 
     * @return
     */
    public static int ZERO() {
    	return XADDWrapper.getInstance().ZERO;
    }
    
    /**
     * 
     * @return
     */
    public static int NEG_INF() {
    	return XADDWrapper.getInstance().NEG_INF;
    }
}
