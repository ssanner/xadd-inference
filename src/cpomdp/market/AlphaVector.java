/**
 * 
 */
package cpomdp.market;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import xadd.ExprLib.ArithExpr;
import xadd.XADD;
import cpomdp.market.utils.XADDHelper;

/**
 * @author skinathil
 *
 */
public class AlphaVector {
	
	private Map.Entry<String, HashMap<String, ArithExpr>> action = null;
	private Map.Entry<String, Integer> observation = null;
	private String actionName = null;
	private String observationName = null;
	private Integer horizon = null;
	private Integer valueXADD = XADDHelper.ZeroXADD();
	
//	public AlphaVector(Integer horizonNum, 
//			Map.Entry<String, HashMap<String, ArithExpr>> actionEntry, String obsName) {
//		
//		this.setHorizon(horizonNum);
//		this.setAction(actionEntry);
//		this.setObservation(obsName);
//				
//		// Set the default value to be Zero
////		this.setValueXADD(XADDHelper.ZeroXADD());
//	}

	public AlphaVector(Integer horizonNum, String actionName, String obsName) {
		
		this.setHorizon(horizonNum);
		this.setActionName(actionName);
		this.setObservation(obsName);
				
		// Set the default value to be Zero
//		this.setValueXADD(XADDHelper.ZeroXADD());
	}
	
	private void setActionName(String actionName2) {
		// TODO Auto-generated method stub
		this.actionName = actionName2;
	}

//	public AlphaVector(Integer horizonNum, 
//			Map.Entry<String, HashMap<String, ArithExpr>> actionEntry) {
//		
//		this.setHorizon(horizonNum);
//		this.setAction(actionEntry);
//				
//		// Set the default value to be Zero
////		this.setValueXADD(XADDHelper.ZeroXADD());
//	}	
	
	public AlphaVector(Integer horizonNum, String actionName) {
		
		this.setHorizon(horizonNum);
		this.setActionName(actionName);
				
		// Set the default value to be Zero
//		this.setValueXADD(XADDHelper.ZeroXADD());
	}		
	
	public AlphaVector(Integer horizonNum) {
		
		this.setHorizon(horizonNum);
//		this.setAction(null);
//		this.setObservation(null);		
//		this.setValueXADD(XADDHelper.ZeroXADD());
	}
	
	/**
	 * @return the horizon
	 */
	public Integer getHorizon() {
		return horizon;
	}

	/**
	 * @param horizon the horizon to set
	 */
	public void setHorizon(Integer horizon) {
		this.horizon = horizon;
	}
	
	/**
	 * @return the valueXADD
	 */
	public Integer getValueXADD() {
		return valueXADD;
	}

	/**
	 * @param valueXADD the valueXADD to set
	 */
	public void setValueXADD(Integer valueXADD) {
		this.valueXADD = valueXADD;
	}

	/**
	 * @return the action
	 */
	public String getActionName() {
		
		return this.actionName;		
	}

	/**
	 * @param action the action to set
	 */
	private void setAction(Map.Entry<String, HashMap<String, ArithExpr>> action) {
		this.action = action;
	}

	/**
	 * @return the observation
	 */
	public String getObservationName() {
		return this.observationName;
	}

	/**
	 * @param obsName the observation to set
	 */
	private void setObservation(String obsName) {
		this.observationName = obsName;
	}
	
	/**
	 * @param alphaVector the alphaVector to be added to the current 
	 */
	public void add(AlphaVector alphaVector) {

		Integer newAlphaVectorXADD = XADDHelper.Apply(this.valueXADD, 
										alphaVector.getValueXADD(), XADD.SUM);		
		this.setValueXADD(newAlphaVectorXADD);
	}

	@Override
	public String toString() {		
		
		String strFormat = String.format("AVec (H:%d", this.getHorizon());
		
		if(this.getActionName() != null) {
			strFormat = String.format("%s A:%s", strFormat, this.getActionName());
		}
		
		if(this.getObservationName() != null) {
			strFormat = String.format("%s O:%s", strFormat, this.getObservationName());
		}

		strFormat = String.format("%s)", strFormat);
		
		return strFormat;		
		
//		String str = String.format("AVec H:%d A:%s O:%s", 
//				this.getHorizon(),  
//				this.getActionName(), 
//				this.getObservationName());
//		
//		return str;
	}
}
