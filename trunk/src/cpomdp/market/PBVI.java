/**
 * 
 */

package cpomdp.market;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import cpomdp.market.utils.VIHelper;
import cpomdp.market.utils.XADDHelper;
import cpomdp.market.utils.XADDWrapper;
import xadd.ExprLib.ArithExpr;
import xadd.ExprLib.DoubleExpr;
import xadd.XADD;
import xadd.XADD.XADDTNode;

/**
 * @author skinathil
 *
 */
public class PBVI {

	public static String DomainDirectoryPath = null;
	
	private final static String ACTION_TYPE_ERROR_MSG = "ActionType unsupported";
	
	private final static Double EPSILON =  0.000001; 

	/**
	 * 
	 * @param beliefPointFile
	 * @param beliefPoints
	 * @return
	 */
	public static ArrayList<Integer> InitialiseBeliefPoints(String beliefPointFile, ArrayList<Integer> beliefPoints) {
		
		if(beliefPoints == null) {
			beliefPoints = PBVI.InitialiseBeliefPoints(beliefPointFile);
		}
		else {
			Integer bpXADD = XADDHelper.BuildXADD(beliefPointFile);
			
			// Check that the bpXADD is valid i.e. it integrates to 1			
			if(!PBVI.validBeliefPoint(bpXADD)) {
				System.out.println("PBVI.InitialiseBeliefPoints: Invalid belief point contained in " + beliefPointFile);
			}
			else {
				beliefPoints.add(bpXADD);
			}
		}
		
		return beliefPoints;
	}
	
	/**
	 * 
	 * @param beliefPointFile
	 * @return
	 */
	public static ArrayList<Integer> InitialiseBeliefPoints(String beliefPointFile) {
		
		ArrayList<Integer> beliefPoints = new ArrayList<Integer>();
		Integer bpXADD = XADDHelper.BuildXADD(beliefPointFile);
		
		// Check that the bpXADD is valid i.e. it integrates to 1
		if(!PBVI.validBeliefPoint(bpXADD)) {
			System.out.println("PBVI.InitialiseBeliefPoints: Invalid belief point contained in " + beliefPointFile);
		}
		else {
			beliefPoints.add(bpXADD);
		}
		
		return beliefPoints;
	}	
	
	/**
	 * Runs the Point-based Value Iteration algorithm for a given number of 
	 * iterations. 
	 * 
	 * @param actionType		ActionType enumeration (Discrete or Continuous)
	 * @param mmPOMDP			MMPOMDP Object
	 * @param numIterations		Number of iterations
	 * @param discountFactor	Discount factor
	 * @return
	 */
	public static void Run(ActionType actionType, POMDP mmPOMDP, 
							ArrayList<Integer> beliefPointSet,
							Integer numIterations, 
							Double discountFactor) {

		switch (actionType) {
		case DISCRETE:	
			PBVI.DiscreteAction(mmPOMDP, beliefPointSet, numIterations, discountFactor);	
			break;
//		case CONTINUOUS:
//			PBVI.ContinuousAction(mmPOMDP, numIterations, discountFactor);
//			break;
		default:
			throw new UnsupportedOperationException(PBVI.ACTION_TYPE_ERROR_MSG);
		}
	}

	/**
	 * 
	 * @param bpID
	 * @return
	 */
	private static boolean validBeliefPoint(Integer bpID) {
		
		boolean validBP = false;
		
		// Get the variables in the bpID XADD
				
		int integral1 = XADDWrapper.getInstance().computeDefiniteIntegral(bpID, "v");
		int integral2 = XADDWrapper.getInstance().computeDefiniteIntegral(integral1, "i");
//		XADDHelper.PlotXADD(integral2, "integral 2: " + bpID);
		
		// The result of integrating over the two continuous variables
		XADDTNode t = (XADDTNode) XADDWrapper.getInstance().getNode(integral2);
		Double val = ((DoubleExpr) t._expr)._dConstVal;
				
//		if(Double.compare(val, 1.0) == 0) {
		if(Math.abs(val - 1.0) < PBVI.EPSILON) {			
			validBP = true;
		}
		
		return validBP;
	}
	
	/**
	 * PBVI Algorithm to solve discrete action POMDP
	 * 
	 * @param mmPOMDP
	 * @param numIterations
	 * @param discountFactor
	 * @return
	 */
	private static void DiscreteAction(POMDP mmPOMDP, 
												ArrayList<Integer> beliefSet,
												Integer numIterations, 
												Double discountFactor) {
		
        // The set of states, actions and observations in the POMDP
		HashMap<String, HashMap<String, ArithExpr>> actionSetMap = mmPOMDP.actionMap;
		HashMap<String, ArithExpr> stateSetMap = mmPOMDP.hmPrimeSubs;	
        HashSet<String> observationSet = mmPOMDP.observationSet;
        
        Integer rFunc = mmPOMDP.rewardFunc;
        HashMap<String, Integer> tFuncMap = mmPOMDP.transitionFuncMap;
        HashMap<String, Integer> oFuncMap = mmPOMDP.observationFuncMap;
        
        AlphaVectorList gammaCurr = null;
        AlphaVectorList gammaPrev = new AlphaVectorList();
        gammaPrev.setHorizon(0);
        
        // {beliefPoint ID -> { horizon -> AlphaVectorList}}
        LinkedHashMap<Integer, HashMap<Integer, List<AlphaVectorList>>> policyData = new
        		LinkedHashMap<Integer, HashMap<Integer, List<AlphaVectorList>>>();
        
        // Add the initial AlphaVector to the gammaPrev
        AlphaVector alphaVectorZero = new AlphaVector(0);
        
        for(int horizon = 1; horizon < (numIterations + 1); horizon++) {
        	System.out.println("Horizon: " + horizon);
        	
        	// gammaPrev to the gammaCurr, if gammaCurr exists 
        	// if it exists
        	if (horizon == 1) {
        		gammaPrev.addAlphaVector(alphaVectorZero);
        	}
        	else {
        		gammaPrev = gammaCurr;
        	}

    		// Initialise a new AlphaVector list for the current horizon
        	gammaCurr = new AlphaVectorList();
        	gammaCurr.setHorizon(horizon);
        	
        	// Iterate through all belief points in the beliefSet
			for (Integer beliefPointID : beliefSet) {
				System.out.println("\tBelief point: " + beliefPointID);
//				XADDHelper.PlotXADD(beliefPointID, "BP" + beliefPointID);
				
				HashMap<Integer, List<AlphaVectorList>> horizonData = null;
				if(policyData.containsKey(beliefPointID)) {
					horizonData = policyData.get(beliefPointID);
				}
				else {
					horizonData = new HashMap<Integer, List<AlphaVectorList>>();
				}
				
				horizonData.put(horizon, new ArrayList<AlphaVectorList>());
				
			    // Initialise an AlphaVectorList for the current beliefPointID
				AlphaVectorList gammaBelief = new AlphaVectorList();
				
				// Iterate through the actions in the actionSetMap
				for(Entry<String, HashMap<String, ArithExpr>> actEntry : actionSetMap.entrySet()){
					
					String actionName = actEntry.getKey();
					System.out.println("\t\tAction = " + actionName);
					HashMap<String, ArithExpr> actionValue = actEntry.getValue();
				    
				    // Initialise an AlphaVectorList for the current (actionName, beliefPointID) combination
					AlphaVectorList gammaActionBelief = new AlphaVectorList(actionName);
					gammaActionBelief.setHorizon(horizon);
					
			    	// The best AlphaVector for the current (actionName, beliefPointID)
				    AlphaVector bestActionBeliefAVec = new AlphaVector(horizon, actEntry, null);
				    
				    // Substitute the "ap" and "bp" variables in the rFunc with the contents
				    // of the actionValue map
//					XADDHelper.PlotXADD(rFunc, "rFunc Before subs");		
				    rFunc = XADDHelper.substitute(mmPOMDP.rewardFunc, actionValue);
//					XADDHelper.PlotXADD(rFunc, "rFunc After subs " + actionName);		
				    
				    // Iterate through all of the observations in the observationSet
				    for(String obsName : observationSet){
				    	
//				    	String obsName = obsEntry.getKey();
				    	System.out.println("\t\t\tObservation = " + obsName);
				    	
						Integer oFunc = oFuncMap.get(obsName);
						
					    // Substitute the "ap" and "bp" variables in the oFunc 
						// with the actionValue
						oFunc = XADDHelper.substitute(oFunc, actionValue);
//						XADDHelper.PlotXADD(oFunc, "oFunc After subs "  + actionName);
						
						Integer oFuncPrimed = XADDHelper.substitute(oFunc, stateSetMap);
						
				    	// Initialise an AlphaVectorList for the current (actionName, obsName) combination
				    	AlphaVectorList gammaActionObservation = new AlphaVectorList(actionName, obsName);		
				    	gammaActionObservation.setHorizon(horizon);
			    		
					    // Iterate through all of the alpha vectors in the gammaPrev
				    	ArrayList<AlphaVector> prevAlphaVectors = gammaPrev.getAlphaVectors();
				    	for(AlphaVector prevAVec : prevAlphaVectors) {
				    		System.out.format("\t\t\t\tAlphaVector(%s, %s). Horizon = %d%n", actionName, obsName, prevAVec.getHorizon());
				    		
//				    		String title =  "A: " + prevAVec.getActionName() + " O: " + prevAVec.getObservationName() + " H = " + prevAVec.getHorizon();
//				    		XADDHelper.PlotXADD(prevAVec.getValueXADD(), title);
				    		
				    		// Initialise a new AlphaVector for the (actionName, obsName)
				    		AlphaVector newAlphaVector = new AlphaVector(horizon, actEntry, obsName);				    		
				    		Integer newAVecXADD = XADDHelper.ZeroXADD();

			    			// Prime the variables in the prevAVec
			    			newAVecXADD = XADDHelper.substitute(prevAVec.getValueXADD(), stateSetMap);
//			    			XADDHelper.PlotXADD(newAVecXADD, "After prime");

			    			// Multiply through by the oFunc
			    			newAVecXADD = XADDHelper.Apply(newAVecXADD, oFuncPrimed, XADD.PROD);
//			    			XADDHelper.PlotXADD(newAVecXADD, "After * oFuncPrimed");
			    			
				    		for(String currState : stateSetMap.keySet()) {
				    			System.out.println("\t\t\t\t\tState = " + currState);
				    			
				    			Integer tFunc = tFuncMap.get(currState);
				    			
							    // Substitute the "ap" and "bp" variables in the tFunc 
								// with the actionValue				    			
				    			tFunc = XADDHelper.substitute(tFunc, actionValue);
//				    			tFunc = XADDHelper.substitute(tFunc, stateSetMap);
				    			
				    			// Regress the continuous variables
				    			newAVecXADD = VIHelper.regressContinuousVariable(newAVecXADD, tFunc, stateSetMap.get(currState).toString());
//				    			XADDHelper.PlotXADD(newAVecXADD, "After Regress " + stateSetMap.get(currState).toString());
				    		}				    		
				    		
				    		// Discount
				    		newAVecXADD = XADDHelper.ScalarOp(newAVecXADD, discountFactor, XADD.PROD);
//			    			XADDHelper.PlotXADD(newAVecXADD, "After * discountFactor");
				    		
				    		// Add the rFunc
			    			newAVecXADD = XADDHelper.Apply(newAVecXADD, rFunc, XADD.SUM);				
//			    			XADDHelper.PlotXADD(newAVecXADD, "After + Reward");
				    		
			    			// Add the newAVecXADD to the newAlphaVector 
			    			newAlphaVector.setValueXADD(newAVecXADD);
			    			
				    		// Add the newAlphaVector to the gammaActionObservation
				    		gammaActionObservation.addAlphaVector(newAlphaVector);
				    	}
				    	
				    	System.out.format("\t\t\t\t|Gamma^{%s}_{%s}| = %d%n", actionName, obsName, gammaActionObservation.size());
				    	
				    	// Append the gammaActionObservation
				    	List<AlphaVectorList> tmpList = horizonData.get(horizon);
				    	tmpList.add(gammaActionObservation);
				    	horizonData.put(horizon, tmpList);    	
				    	
				    	// Select the AlphaVector in the gammaActionObservation i.e. 
				    	// that maximises: argmax(alphaVector * beliefPointID)
				    	AlphaVector bestActionObsAVec = PBVI.ArgMaxOperation(gammaActionObservation, beliefPointID);	
//				    	String plotTitle = "Best Action Observation AlphaVector " 
//						    	+ bestActionObsAVec.getActionName() + " " + bestActionObsAVec.getObservationName() 
//						    	+ " BP" + beliefPointID + "H" + horizon;
//				    	XADDHelper.PlotXADD(bestActionObsAVec.getValueXADD(), plotTitle);
				    	
				    	// Update the bestActionBeliefAVec
				    	bestActionBeliefAVec.add(bestActionObsAVec);				    	
				    }

				    // Add the bestActionBeliefAVec to the gammaActionBelief 
			    	gammaActionBelief.addAlphaVector(bestActionBeliefAVec);
			    	System.out.format("\t\t\t\t|Gamma^{%s}_{%d}| = %d%n", actionName, beliefPointID, gammaActionBelief.size());
//			    	XADDHelper.PlotXADD(bestActionBeliefAVec.getValueXADD(), "Best Action Belief AlphaVector: " + bestActionBeliefAVec.getActionName());
			    	
 			    	gammaBelief.addAlphaVector(bestActionBeliefAVec);
				}
				
				System.out.format("\t\t\t\t|Gamma_{%d}| = %d%n", beliefPointID, gammaBelief.size());
				
				policyData.put(beliefPointID, horizonData);				
				
				// Select the best AlphaVector for the current beliefPointID
				// NOTE: Essentially, this means finding the best action for the beliefPointID
				AlphaVector bestBeliefAVec = PBVI.ArgMaxOperation(gammaBelief, beliefPointID);
				String plotTitle = "Best Belief AlphaVector: " + bestBeliefAVec.getActionName() + " BP" + beliefPointID + "H" + horizon;
				XADDHelper.PlotXADD(bestBeliefAVec.getValueXADD(), plotTitle);			
				
				// Add the bestBeliefAVec to the gammaPrev
				gammaCurr.addAlphaVector(bestBeliefAVec);
				
	            plotTitle = "BP" + beliefPointID + "H" + horizon + "Action" + bestBeliefAVec.getActionName();
	            XADDHelper.Display3D(bestBeliefAVec.getValueXADD(), plotTitle, 
	            		PBVI.DomainDirectoryPath + File.separator + "plot", 
	            		PBVI.DomainDirectoryPath.replace(".", File.separator));
        	}
			
			System.out.format("|Gamma| = %d%n", gammaCurr.size());
        }
        
        // Extract the optimal policy for the given beliefState
        
        PBVI.ExtractPolicy(numIterations, policyData.get(beliefSet.get(0)));
        
	}
	
	private static void ExtractPolicy(Integer horizon, HashMap<Integer, List<AlphaVectorList>> policyData) {

		List<AlphaVectorList> gammaAOList = policyData.get(horizon);
		
		System.out.format("Horizon %d |gammaAOList| = %d%n", horizon, gammaAOList.size());
//		System.out.format("Horizon %d |gammaAOList| = %d%n Gamma^{%s}_{%s}", horizon, gammaAO.getAction(), gammaAO.getObservation());
		
		// Find the 
		
		if (horizon > 1) {			
			PBVI.ExtractPolicy(horizon - 1, policyData);
		}
		
	}
	
	/**
	 * 
	 * @param list
	 * @param beliefPoint
	 * @return
	 */
	private static AlphaVector ArgMaxOperation(AlphaVectorList list, Integer beliefPoint) {
		
		AlphaVector maxAlphaVector = null;
		Integer innerProdXADD = null;		
		Double innerProdValue = null;
		Double maxInnerProdValue = Double.NEGATIVE_INFINITY;
		
//		XADDHelper.PlotXADD(beliefPoint, "Belief Point");
		
		// Iterate through each AlphaVector in the list
		ArrayList<AlphaVector> alphaVectors = list.getAlphaVectors();
		for (AlphaVector currAlphaV : alphaVectors) {
//			XADDHelper.PlotXADD(currAlphaV.getValueXADD(), "AlphaVector " + currAlphaV.getActionName());
			
			// The inner product of the currAlphaV and the  beliefPoint
			innerProdXADD = XADDHelper.Apply(currAlphaV.getValueXADD(), beliefPoint, XADD.PROD);
//			XADDHelper.PlotXADD(innerProdXADD, "Inner Product");
			
			// Compute the definite integral of the innerProdXADD over the continuous
			// variables
			Integer int1 =  XADDWrapper.getInstance().computeDefiniteIntegral(innerProdXADD, "v");
//			XADDHelper.PlotXADD(int1, "Integral 1 v");
			Integer int2 =  XADDWrapper.getInstance().computeDefiniteIntegral(int1, "i");
//			XADDHelper.PlotXADD(int2, "Integral 2 i");			
			
			XADDTNode t = (XADDTNode) XADDWrapper.getInstance().getNode(int2);
			innerProdValue = ((DoubleExpr) t._expr)._dConstVal;
			
			if(innerProdValue > maxInnerProdValue) {
				maxAlphaVector = currAlphaV;
				maxInnerProdValue = innerProdValue; 
			}
		}		
		
		return maxAlphaVector;
	}
	
//	/**
//	 * PBVI Algorithm to solve continuous action POMDP
//	 * 
//	 * @param mmPOMDP			MMPOMDP Object
//	 * @param numIterations		Number of iterations
//	 * @param discountFactor	Discount factor
//	 * @return
//	 */
//	private static void ContinuousAction(POMDP mmPOMDP, 
//			Integer numIterations, Double discountFactor) {
////		return null;
//	}	
}
