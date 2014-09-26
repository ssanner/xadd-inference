/**
 * 
 */
package cpomdp.market;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author skinathil
 *
 */
public class GammaSet implements Iterable<AlphaVector>{

	private ArrayList<AlphaVector> vectors = new ArrayList<AlphaVector>();

	private String action = null;
	private String observation = null;
	private Integer horizon = null;
	private Integer beliefPoint = null;

	/*
	 * Constructors
	 */

	public GammaSet(Integer horizon, Integer beliefPoint, String actionName, 
						String obsName, ArrayList<AlphaVector> alphaVectors) {

		this.setHorizon(horizon);
		this.setBeliefPoint(beliefPoint);
		this.setAction(action);
		this.setObservation(observation);
		this.setVectors(alphaVectors);		
	}		
	
	public GammaSet(Integer horizon, Integer beliefPoint, String actionName, 
															String obsName) {

		this.setHorizon(horizon);
		this.setBeliefPoint(beliefPoint);
		this.setAction(actionName);
		this.setObservation(obsName);
	}			
	
	public GammaSet(Integer horizon, Integer beliefPoint, String actionName) {	
		this.setHorizon(horizon);
		this.setBeliefPoint(beliefPoint);
		this.setAction(actionName);
	}	

	public GammaSet(Integer horizon, Integer beliefPoint) {	
		this.setHorizon(horizon);
		this.setBeliefPoint(beliefPoint);
	}		

	public GammaSet(Integer horizon) {	
		this.setHorizon(horizon);
	}		
	

	/**
	 * 
	 * @param alphaVector
	 */
	public void addVector(AlphaVector alphaVector) {
		this.vectors.add(alphaVector);
	}	

	/**
	 * 
	 * @return the number of vectors
	 */
	public Integer size() {
		return this.vectors.size();
	}

	/**
	 * @return the action
	 */
	public String getAction() {
		return action;
	}

	/**
	 * @param action the action to set
	 */
	public void setAction(String action) {
		this.action = action;
	}

	/**
	 * @return the observation
	 */
	public String getObservation() {
		return observation;
	}

	/**
	 * @param observation the observation to set
	 */
	public void setObservation(String observation) {
		this.observation = observation;
	}	

	/**
	 * 
	 * @return the vectors
	 */
	public ArrayList<AlphaVector> getVectors() {
		return this.vectors;
	}

	/**
	 * 
	 * @param vectors
	 */
	private void setVectors(ArrayList<AlphaVector> vectors) {
		this.vectors = vectors;
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
	 * @return the beliefPoint
	 */
	public Integer getBeliefPoint() {
		return beliefPoint;
	}

	
	/**
	 * @param beliefPoint the beliefPoint to set
	 */
	public void setBeliefPoint(Integer beliefPoint) {
		this.beliefPoint = beliefPoint;
	}

	@Override
	public Iterator<AlphaVector> iterator() {
		return vectors.iterator();
	}
	
	@Override
	public String toString() {
		
		String strFormat = String.format("|Gamma H:%d", this.getHorizon());
		
		if(this.getBeliefPoint() != null) {
			strFormat = String.format("%s BP:%d", strFormat, this.getBeliefPoint());
		}
		
		if(this.getAction() != null) {
			strFormat = String.format("%s A:%s", strFormat, this.getAction());
		}
		
		if(this.getObservation() != null) {
			strFormat = String.format("%s O:%s", strFormat, this.getObservation());
		}

		strFormat = String.format("%s| = %d", strFormat, this.size());
		
		return strFormat;
	}
}
