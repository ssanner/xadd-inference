/**
 * 
 */

package cpomdp.market;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

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
public class PBVIDiscreteAction {

	public static String DOMAIN_DIR_PATH = null;	
	private final static Double EPSILON =  0.000001; 

	/**
	 * 
	 * @param beliefPointFile
	 * @param beliefPoints
	 * @return
	 */
	public static ArrayList<Integer> InitialiseBeliefPoints(String beliefPointFile, ArrayList<Integer> beliefPoints) {
		
		if(beliefPoints == null) {
			beliefPoints = new ArrayList<Integer>();
		}
		
		Integer bpXADD = PBVIDiscreteAction.InitialiseBeliefPoint(beliefPointFile);		
		beliefPoints.add(bpXADD);
		
		return beliefPoints;
	}
	
	/**
	 * 
	 * @param beliefPointFile
	 * @return
	 */
	public static Integer InitialiseBeliefPoint(String beliefPointFile) {
		
		Integer bpXADD = XADDHelper.BuildXADD(beliefPointFile);
		
		// Check that the bpXADD is valid i.e. it integrates to 1
		if(!PBVIDiscreteAction.validBeliefPoint(bpXADD)) {
			System.out.println("PBVI.InitialiseBeliefPoints: Invalid belief point contained in " + beliefPointFile);
		}
		
		return bpXADD;
	}	

	/**
	 * Examines whether a belief point is valid. i.e. if it integrates to 1.0
	 * 
	 * @param 	bpID 			(Integer)
	 * @return	validBelied?	(Boolean)
	 */
	private static boolean validBeliefPoint(Integer bpID) {
		
		boolean validBP = true;
		
		// Get the variables in the bpID XADD
				
		int integral1 = XADDWrapper.getInstance().computeDefiniteIntegral(bpID, "v");
		int integral2 = XADDWrapper.getInstance().computeDefiniteIntegral(integral1, "i");
		
		// The result of integrating over the two continuous variables
		XADDTNode t = (XADDTNode) XADDWrapper.getInstance().getNode(integral2);
		Double val = ((DoubleExpr) t._expr)._dConstVal;
				
		assert(Math.abs(val - 1.0) < PBVIDiscreteAction.EPSILON) : 
			"Belief point does not integrate out to 1.0";
		
		return validBP;
	}
	
	/**
	 * 
	 * @return
	 */
	private static Integer InitialiseUncertaintyXADD(Double mu, Double sigma) {
		String uBPFile = PBVIDiscreteAction.DOMAIN_DIR_PATH + File.separator + "uncertainty_belief_point.xadd";
		
		Integer uncertaintyXADD = XADDHelper.BuildXADD(uBPFile);
	
		HashMap<String, ArithExpr> a1 = new HashMap<String, ArithExpr>();
		
        a1.put("m", new DoubleExpr(mu));
        a1.put("w", new DoubleExpr(sigma));
        a1.put("p", new DoubleExpr(1/(12 * (sigma*sigma))));
        a1.put("sq", new DoubleExpr(Math.sqrt(3)));
		
		uncertaintyXADD = XADDHelper.substitute(uncertaintyXADD, a1);
		
		XADDHelper.PlotXADD(uncertaintyXADD, "Uncertainty XADD Mu: " + mu + " Sigma: " + sigma);
		
		int integral1 = XADDWrapper.getInstance().computeDefiniteIntegral(uncertaintyXADD, "v");
		int integral2 = XADDWrapper.getInstance().computeDefiniteIntegral(integral1, "i");
		
		XADDTNode t = (XADDTNode) XADDWrapper.getInstance().getNode(integral2);
		Double val = ((DoubleExpr) t._expr)._dConstVal;
		
		assert(Math.abs(val - 1.0) < PBVIDiscreteAction.EPSILON) : 
			"Distribution does not integrate out to 1.0";		
		
		return uncertaintyXADD;
	}
	
	/**
	 * 
	 * @param mmPOMDP
	 * @param beliefSet
	 * @param numIterations
	 * @param discountFactor
	 */
	public static void Run(POMDP mmPOMDP, ArrayList<Integer> beliefSet,
												Integer numIterations, 
												Double discountFactor) {
		
		/*
		 * Extract information from the mmPOMDP
		 */
		HashMap<String, HashMap<String, ArithExpr>> actionSetMap = mmPOMDP.actionMap;
		HashMap<String, ArithExpr> stateSetMap = mmPOMDP.hmPrimeSubs;	
        HashSet<String> observationSet = mmPOMDP.observationSet;
        
        Integer rFunc = mmPOMDP.rewardFunc;
        HashMap<String, Integer> tFuncMap = mmPOMDP.transitionFuncMap;
        HashMap<String, Integer> oFuncMap = mmPOMDP.observationFuncMap;

        /*
         * 
         */
        
        Double mu = 3.0;
        Double sigma = 2.0;
//        Integer uncertaintyXADD = PBVIDiscreteAction.InitialiseUncertaintyXADD(mu, sigma);
                /*
         * Initialise an ..
         */
        GammaSet prevGamma = new GammaSet(0);
        
        AlphaVector zeroAVec = new AlphaVector(0);
        prevGamma.addVector(zeroAVec);
        
        GammaSet currGamma = null;
        
        for(int horizon = 1; horizon < (numIterations + 1); horizon++) {
        	System.out.println("H: " + horizon);
        	
        	if(horizon > 1) {
        		prevGamma = currGamma;
        	}
        	
        	// Initialise a GammaSet to hold all of the AlphaVectors for the current
        	//horizon
        	currGamma = new GammaSet(horizon);
        	
        	// Iterate through all belief points in the beliefSet
			for (Integer beliefPointID : beliefSet) {
				System.out.println("\tBP: " + beliefPointID);
				
				// Initialise a GammaSet to hold all of the AlphaVectors for the
				// current (horizon, beliefPointID) combination
				GammaSet currBPGamma = new GammaSet(horizon, beliefPointID);
				
				// Iterate through the actions in the actionSetMap
				Iterator<String> aKeyIterator = actionSetMap.keySet().iterator();
				while(aKeyIterator.hasNext()) {
					String actionName = aKeyIterator.next();
					HashMap<String, ArithExpr> bidAskMap = actionSetMap.get(actionName);
					
					System.out.println("\t\tA: " + actionName);
					
				    // Substitute the "ap" and "bp" variables in the rFunc with 
					// the contents of the bidAskMap
				    rFunc = XADDHelper.substitute(mmPOMDP.rewardFunc, bidAskMap);
				    
				    AlphaVector optActAVec = new AlphaVector(horizon, actionName);
				    
				    // Iterate through all of the observations in the observationSet
				    for(String obsName : observationSet){
				    	System.out.println("\t\t\tO: " + obsName);
				    	
				    	// Initialise a GammaSet to hold all of the AlphaVectors
				    	// for the current (horizon, beliefPointID, actionName, obsName)
				    	// combination
				    	GammaSet currBPActObsGamma = new GammaSet(horizon, beliefPointID, actionName, obsName);
				    	
				    	Integer oFunc = oFuncMap.get(obsName);
				    	Integer oFuncPrimed = XADDHelper.substitute(oFunc, stateSetMap);
				    	
					    // Substitute the "ap" and "bp" variables in the oFuncPrimed 
				    	// with the contents of the bidAskMap
				    	oFuncPrimed = XADDHelper.substitute(oFuncPrimed, bidAskMap);
				    	
					    // Iterate through all of the alpha vectors in the prevGamma
						// set
				    	Iterator<AlphaVector> aVecIterator = prevGamma.iterator();
				    	while(aVecIterator.hasNext()) {
				    		AlphaVector prevAVec = aVecIterator.next();
				    		System.out.println("\t\t\t\tPrev " + prevAVec);
				    		
				    		// Initialise a new AlphaVector for the (actionName, obsName)
				    		AlphaVector newAVec = new AlphaVector(horizon, actionName, obsName); 
				    		
				    		Integer newAVecXADD = prevAVec.getValueXADD(); 
				    		
			    			// Prime the variables in the newAVecXADD
			    			newAVecXADD = XADDHelper.substitute(newAVecXADD, stateSetMap);
//			    			XADDHelper.PlotXADD(newAVecXADD, "After prime");

			    			// Multiply through by the oFuncPrimed
			    			newAVecXADD = XADDHelper.Apply(newAVecXADD, oFuncPrimed, XADD.PROD);
//			    			XADDHelper.PlotXADD(newAVecXADD, "After * oFuncPrimed");

				    		for(String currState : stateSetMap.keySet()) {
				    			System.out.println("\t\t\t\t\tState = " + currState);
				    			
				    			String nextState = stateSetMap.get(currState).toString();
				    			
				    			Integer tFunc = tFuncMap.get(currState);
				    			
							    // Substitute the "ap" and "bp" variables in the tFunc 
								// with the bidAskMap				    			
				    			tFunc = XADDHelper.substitute(tFunc, bidAskMap);
				    			
				    			// Regress the continuous variables
				    			newAVecXADD = VIHelper.regressContinuousVariable(newAVecXADD, tFunc, nextState);
//				    			XADDHelper.PlotXADD(newAVecXADD, "After Regress " + stateSetMap.get(currState).toString());
				    		}				    		
				    		
				    		// Discount
				    		newAVecXADD = XADDHelper.ScalarOp(newAVecXADD, discountFactor, XADD.PROD);
				    		
				    		// Add the rFunc
			    			newAVecXADD = XADDHelper.Apply(newAVecXADD, rFunc, XADD.SUM);				
				    		
			    			// Add the newAVecXADD to the newAlphaVector 
			    			newAVec.setValueXADD(newAVecXADD);
			    			System.out.println("\t\t\t\tNew " + newAVec);
				    		
			    			// Add the newAVec to the currBPActObsGamma set
			    			currBPActObsGamma.addVector(newAVec);
//			    			System.out.println("\t\t\t\t" + currBPActObsGamma)		    			
				    	}

				    	// Make sure that there is one new AlphaVector per AlphaVector
				    	// in the prevGamma set
				    	assert(currBPActObsGamma.size() == prevGamma.size()) :
			    			System.out.format("Incorrect number of AlphaVectors in currBPActObsGamma. Expected %d%n", prevGamma.size()) ;
				    	
				    	// Calculate argmax_{\alpha \in \Gamma^{a, o}} (\alpha \cdot b)
				    	// The optimal AlphaVector in the currBPActObsGamma for the current
				    	// beliefPointID
	    				AlphaVector optActObsAVec = PBVIDiscreteAction.ArgMaxOperation(currBPActObsGamma, beliefPointID);
	    				System.out.format("\t\t\t%s. Opt %s%n", currBPActObsGamma, optActObsAVec);
	    				
				    	// Add the optActObsAVec to the optActAVec
	    				optActAVec.add(optActObsAVec);
				    }
				    
				    // Add the optActAVec to the currBPGamma set
			    	currBPGamma.addVector(optActAVec);
			    	System.out.println("\t\t" + currBPGamma);
				}
				
		    	// Calculate argmax_{\alpha \in \Gamma^{a, b}, \forall a \in A} (\Gamma^{a, b} \cdot b)
		    	// The optimal AlphaVector in the currBPActGamma for the current
		    	// beliefPointID
				AlphaVector optAVec = PBVIDiscreteAction.ArgMaxOperation(currBPGamma, beliefPointID);
				System.out.format("\t%s. Opt %s%n", currBPGamma, optAVec);
				
				// Add the optAVec for the current beliefPointID to the currGamma set
				currGamma.addVector(optAVec);
				
				String plotTitle = "BP: " + beliefPointID + " " + optAVec.toString();
				XADDHelper.PlotXADD(optAVec.getValueXADD(), plotTitle);
				
	            XADDHelper.Display3D(optAVec.getValueXADD(), plotTitle.replace(":", "-"), 
	            		PBVIDiscreteAction.DOMAIN_DIR_PATH + File.separator + "plot", 
	            		PBVIDiscreteAction.DOMAIN_DIR_PATH.replace(".", File.separator));				
//	            	            
//	            Integer tmpXADD = XADDHelper.Apply(uncertaintyXADD, optAVec.getValueXADD(), XADD.PROD);
//	            String plotTitle2 = "BP: " + beliefPointID + " " + optAVec.toString() + " Uncertainty " + " Mu: " + mu + " Sigma: " + sigma;
//	            XADDHelper.PlotXADD(tmpXADD, plotTitle2);
//	            
//	            XADDHelper.Display3D(tmpXADD, plotTitle2.replace(":", "-"), 
//	            		PBVIDiscreteAction.DOMAIN_DIR_PATH + File.separator + "plot", 
//	            		PBVIDiscreteAction.DOMAIN_DIR_PATH.replace(".", File.separator));	            
			}
		
			System.out.format("%s%n", currGamma);
        }
	}
	
	/**
	 * 
	 * @param horizon
	 * @param policyData
	 */
	private static void ExtractPolicy(Integer horizon, HashMap<Integer, List<AlphaVectorList>> policyData) {

		List<AlphaVectorList> gammaAOList = policyData.get(horizon);
		
		System.out.format("Horizon %d |gammaAOList| = %d%n", horizon, gammaAOList.size());
//		System.out.format("Horizon %d |gammaAOList| = %d%n Gamma^{%s}_{%s}", horizon, gammaAO.getAction(), gammaAO.getObservation());
		
		// Find the 
		
		if (horizon > 1) {			
			PBVIDiscreteAction.ExtractPolicy(horizon - 1, policyData);
		}
		
	}
	
	/**
	 * 
	 * @param list
	 * @param beliefPoint
	 * @return
	 */
	private static AlphaVector ArgMaxOperation(GammaSet gammaSet, Integer beliefPoint) {
		
		AlphaVector maxAlphaVector = null;
		Integer innerProdXADD = null;		
		Double innerProdValue = null;
		Double maxInnerProdValue = Double.NEGATIVE_INFINITY;
		
		XADDHelper.PlotXADD(beliefPoint, "BP: " + beliefPoint);
		
		if(gammaSet.size() == 1) {
			return gammaSet.getVectors().get(0);
		}
		
		// Iterate through each AlphaVector in the gammaSet
		for (AlphaVector currAlphaV : gammaSet) {
			XADDHelper.PlotXADD(currAlphaV.getValueXADD(), currAlphaV.toString());
			
			// The inner product of the currAlphaV and the  beliefPoint
			innerProdXADD = XADDHelper.Apply(currAlphaV.getValueXADD(), beliefPoint, XADD.PROD);
			XADDHelper.PlotXADD(innerProdXADD, "Inner Product BP: " + beliefPoint + " " + currAlphaV.toString());
			
			// Compute the definite integral of the innerProdXADD over the continuous
			// variables
			Integer int1 =  XADDWrapper.getInstance().computeDefiniteIntegral(innerProdXADD, "i");
			XADDHelper.PlotXADD(int1, "Integral 1 v");
			Integer int2 =  XADDWrapper.getInstance().computeDefiniteIntegral(int1, "v");
			
			// The result ...
			XADDTNode t = (XADDTNode) XADDWrapper.getInstance().getNode(int2);
			innerProdValue = ((DoubleExpr) t._expr)._dConstVal;
			
			if(innerProdValue > maxInnerProdValue) {
				maxAlphaVector = currAlphaV;
				maxInnerProdValue = innerProdValue; 
			}
		}		
		
		return maxAlphaVector;
	}

}
