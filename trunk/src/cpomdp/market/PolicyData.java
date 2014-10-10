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
public class PolicyData {

	// { horizon -> { beliefPointID -> { ObservationName -> { GammaSets } } } }
	private HashMap<Integer, HashMap<Integer, HashMap<String, ArrayList<GammaSet>>>> data = null;
	
	/**
	 * 
	 */
	public PolicyData() {
		this.data = new HashMap<Integer, HashMap<Integer, HashMap<String, ArrayList<GammaSet>>>>();
	}

	/**
	 * 
	 * @param horizon
	 * @param beliefPointID
	 * @param observationName
	 * @param currBPActObsGamma
	 */
	public void updatePolicy(Integer horizon, Integer beliefPointID, 
						String observationName, GammaSet currBPActObsGamma) {
	
		ArrayList<GammaSet> obsList = null;
		HashMap<String, ArrayList<GammaSet>> obMap = null;
		HashMap<Integer, HashMap<String, ArrayList<GammaSet>>> bpMap = null;

		if(this.data.containsKey(horizon)) {							
			bpMap = this.data.get(horizon);

			if(bpMap.containsKey(beliefPointID)) {
				obMap = bpMap.get(beliefPointID);

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
		}
		else {
			bpMap = new HashMap<Integer, HashMap<String, ArrayList<GammaSet>>>();
			obsList = new ArrayList<GammaSet>();
			obMap = new HashMap<String, ArrayList<GammaSet>>();
		}

		obsList.add(currBPActObsGamma);
		obMap.put(observationName, obsList);
		bpMap.put(beliefPointID, obMap);						
		this.data.put(horizon, bpMap);		
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
	public Set<Integer> getBeliefPoints(Integer horizon) {
		return this.getHorizonData(horizon).keySet();
	}

	/**
	 * 
	 * @param horizon
	 * @param beliefPointID
	 * @return
	 */
	public Set<String> getObservationNames(Integer horizon, Integer beliefPointID) {
		return this.getBeliefPointData(horizon, beliefPointID).keySet();
	}
	
	/**
	 * 
	 * @param horizon
	 * @return
	 */
	public HashMap<Integer, HashMap<String, ArrayList<GammaSet>>> getHorizonData(Integer horizon) {
		return this.data.get(horizon);
	}
	
	/**
	 * 
	 * @param horizon
	 * @param beliefPointID
	 * @return
	 */
	public HashMap<String, ArrayList<GammaSet>> getBeliefPointData(Integer horizon, Integer beliefPointID) {
		HashMap<Integer, HashMap<String, ArrayList<GammaSet>>> horizonData = this.getHorizonData(horizon);
		return horizonData.get(beliefPointID);
	}
	
	/**
	 * 
	 * @param horizon
	 * @param beliefPointID
	 * @param observationName
	 * @return
	 */
	public ArrayList<GammaSet> getObservationData(Integer horizon, Integer beliefPointID, String observationName) {		
		HashMap<String, ArrayList<GammaSet>> bpData = this.getBeliefPointData(horizon, beliefPointID);
		return bpData.get(observationName);
	}
}
