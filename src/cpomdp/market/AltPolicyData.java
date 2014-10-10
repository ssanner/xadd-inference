/**
 * 
 */
package cpomdp.market;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * @author skinathil
 *
 */
public class AltPolicyData {

	// { horizon -> {  ObservationName -> { GammaSets } } }
	private HashMap<Integer, HashMap<String, ArrayList<GammaSet>>> data = null;
	
	/**
	 * 
	 */
	public AltPolicyData() {
		this.data = new HashMap<Integer, HashMap<String, ArrayList<GammaSet>>>();
	}

	/**
	 * 
	 * @param horizon
	 * @param observationName
	 * @param currBPActObsGamma
	 */
	public void updatePolicy(Integer horizon, String observationName, GammaSet currBPActObsGamma) {
	
		ArrayList<GammaSet> obsList = null;
		HashMap<String, ArrayList<GammaSet>> obMap = null;

		if(this.data.containsKey(horizon)) {							
			obMap = this.data.get(horizon);

			if(obMap.containsKey(observationName)) {
				obsList = obMap.get(observationName);
			} 
			else {
				obsList = new ArrayList<GammaSet>();
			}
		}
		else {			
			obMap = new HashMap<String, ArrayList<GammaSet>>();
			obsList = new ArrayList<GammaSet>();
		}
		
		obsList.add(currBPActObsGamma);
		obMap.put(observationName, obsList);
		this.data.put(horizon, obMap);		
	}	
	
	/**
	 * 
	 * @return
	 */
	public Set<Integer> getHorizons() {
		return this.data.keySet();
	}

	/**
	 * 
	 * @param horizon
	 * @return
	 */
	public Set<String> getObservationNames(Integer horizon) {
		return this.getHorizonData(horizon).keySet();
	}
	
	/**
	 * 
	 * @param horizon
	 * @return
	 */
	public HashMap<String, ArrayList<GammaSet>> getHorizonData(Integer horizon) {
		return this.data.get(horizon);
	}
	
	/**
	 * 
	 * @param horizon
	 * @param observationName
	 * @return
	 */
	public ArrayList<GammaSet> getObservationData(Integer horizon, String observationName) {
		HashMap<String, ArrayList<GammaSet>> horizonData = this.getHorizonData(horizon);
		return horizonData.get(observationName);
	}
}
