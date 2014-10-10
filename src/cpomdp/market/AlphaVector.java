/**
 * 
 */
package cpomdp.market;

import cpomdp.market.utils.XADDOperator;
import cpomdp.market.utils.XADDWrapper;

/**
 * @author skinathil
 *
 */
public class AlphaVector {

	private String actionName = null;
	private String observationName = null;
	private Integer horizon = null;
	private Integer beliefPointID = null;
	private Integer valueXADD = XADDWrapper.ZERO();

	/*
	 * Constructors
	 */
	
	/**
	 * 
	 * @param horizonNum
	 * @param actionName
	 * @param obsName
	 */
	public AlphaVector(Integer horizonNum, String actionName, String obsName) {
		
		this.setHorizon(horizonNum);
		this.setActionName(actionName);
		this.setObservation(obsName);
	}
	
	/**
	 * 
	 * @param horizonNum
	 * @param actionName
	 */
	public AlphaVector(Integer horizonNum, String actionName) {
		
		this.setHorizon(horizonNum);
		this.setActionName(actionName);
	}		
	
	/**
	 * 
	 * @param horizonNum
	 */
	public AlphaVector(Integer horizonNum) {
		
		this.setHorizon(horizonNum);
	}
	
	/*
	 * Getters and Setters
	 */
	
	/**
	 * @return the horizon
	 */
	public Integer getHorizon() {
		return horizon;
	}

	/**
	 * @param horizon the horizon to set
	 */
	private void setHorizon(Integer horizon) {
		this.horizon = horizon;
	}
	
	/**
	 * @return the valueXADD
	 */
	public Integer getXADD() {
		return valueXADD;
	}

	/**
	 * @param valueXADD the valueXADD to set
	 */
	public void setXADD(Integer valueXADD) {
		this.valueXADD = valueXADD;
	}

	/**
	 * @return the action
	 */
	public String getActionName() {
		
		return this.actionName;		
	}

	/**
	 * 
	 * @param actionName2
	 */
	private void setActionName(String actionName2) {
		this.actionName = actionName2;
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
	
	/*
	 * Constructors
	 */
	
	/**
	 * @param alphaVector the alphaVector to be added to the current 
	 */
	public void add(AlphaVector aVec) {

		Integer newAVecXADD = XADDWrapper.Apply(this.valueXADD, 
											aVec.getXADD(), XADDOperator.SUM);		
		this.setXADD(newAVecXADD);
	}

	@Override
	public String toString() {
		
		String strFormat = String.format("AVec (H:%d", this.getHorizon());

		if(this.getBeliefPointID() != null) {
			strFormat = String.format("%s BP:%s", strFormat, this.getBeliefPointID().toString());
		}
		
		
		if(this.getActionName() != null) {
			strFormat = String.format("%s A:%s", strFormat, this.getActionName());
		}
		
		if(this.getObservationName() != null) {
			strFormat = String.format("%s O:%s", strFormat, this.getObservationName());
		}
		
		return String.format("%s)", strFormat);
	}

	/**
	 * @return the beliefPointID
	 */
	public Integer getBeliefPointID() {
		return beliefPointID;
	}

	/**
	 * @param beliefPointID the beliefPointID to set
	 */
	public void setBeliefPointID(Integer beliefPointID) {
		this.beliefPointID = beliefPointID;
	}
}
