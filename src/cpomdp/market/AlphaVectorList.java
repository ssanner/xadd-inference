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
//public class AlphaVectorList implements Iterable<AlphaVector>{
public class AlphaVectorList {
    private ArrayList<AlphaVector> list;
    private String action = "";
    private String observation = "";
    private Integer horizon = null;

    /*
     * Constructors
     */
    
	public AlphaVectorList(String action, String observation, 
										ArrayList<AlphaVector> alphaVectors) {
		
		this.setAction(action);
		this.setObservation(observation);
		this.list = alphaVectors;
	}    
    
	public AlphaVectorList(String action, String observation) {
		this.setAction(action);
		this.setObservation(observation);
		this.list = new ArrayList<AlphaVector>();
	}
    
	public AlphaVectorList(String action) {
		this.setAction(action);
		this.list = new ArrayList<AlphaVector>();
	}	
	
	public AlphaVectorList() {
		this.list = new ArrayList<AlphaVector>();
	}	
	
	public void addAlphaVector(AlphaVector alphaVector) {		
		this.list.add(alphaVector);
	}	
	
	public Integer size() {
		return this.list.size();
	}
	
//    public Iterator<AlphaVector> iterator() {
//    	return this.list.listIterator(); 
//    }

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
	
	public ArrayList<AlphaVector> getAlphaVectors() {
		return this.list;
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
}
