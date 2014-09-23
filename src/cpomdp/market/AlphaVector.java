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
	
	private Map.Entry<String, HashMap<String, ArithExpr>> action;
	private Map.Entry<String, Integer> observation;
	private String observationName;
	private Integer horizon = null;
	private Integer valueXADD = null;
	
	public AlphaVector(Integer horizonNum, 
								Map.Entry<String, HashMap<String, ArithExpr>> actionEntry, 
								String obsName) {
		
		this.setHorizon(horizonNum);
		this.setAction(actionEntry);
		this.setObservation(obsName);
				
		// Set the default value to be Zero
		this.setValueXADD(XADDHelper.ZeroXADD());
	}

	public AlphaVector(Integer horizonNum) {
		
		this.setHorizon(horizonNum);
		this.setAction(null);
		this.setObservation(null);		
		this.setValueXADD(XADDHelper.ZeroXADD());
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
		
		if(this.action == null) {
			return "";
		}
		else {
			return this.action.getKey();
		}
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
	
}
