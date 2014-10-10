/**
 * 
 */

package cpomdp.market.test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import xadd.ExprLib.ArithExpr;
import xadd.ExprLib.DoubleExpr;
import xadd.XADD.XADDTNode;
import cpomdp.market.AlphaVector;
import cpomdp.market.GammaSet;
import cpomdp.market.POMDP;
import cpomdp.market.utils.VIHelper;
import cpomdp.market.utils.XADDHelper;
import cpomdp.market.utils.XADDOperator;
import cpomdp.market.utils.XADDWrapper;

/**
 * @author skinathil
 *
 */
public class PBVIDiscreteAction2 {

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

		Integer bpXADD = PBVIDiscreteAction2.InitialiseBeliefPoint(beliefPointFile);		
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
		XADDHelper.PlotXADD(bpXADD, "BP: " + bpXADD.toString() + " " + beliefPointFile);

		// Check that the bpXADD is valid i.e. it integrates to 1
		if(!PBVIDiscreteAction2.IsValidBeliefPoint(bpXADD)) {
			System.out.println("PBVI.InitialiseBeliefPoints: Invalid belief point contained in " + beliefPointFile);
		}

		return bpXADD;
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
		HashMap<String, ArithExpr> stateSetMap = mmPOMDP.stateMap;	
		HashSet<String> observationSet = mmPOMDP.observationSet;

		Integer rFunc = mmPOMDP.rewardFunc;
		HashMap<String, Integer> tFuncMap = mmPOMDP.transitionFuncMap;
		HashMap<String, Integer> oFuncMap = mmPOMDP.observationFuncMap;

		/*
		 * Initialise data structure to hold the information needed to extract a 
		 * policy
		 */

		//		HashMap<Integer, ArrayList<GammaSet>> policyData = new HashMap<Integer, ArrayList<GammaSet>>();
		// { horizon -> { beliefPointID -> { ObservationName -> { GammaSets } } } }
		HashMap<Integer, HashMap<Integer, HashMap<String, ArrayList<GammaSet>>>> 
		policyData = new HashMap<Integer, HashMap<Integer, HashMap<String, ArrayList<GammaSet>>>>(); 

		/* Initialise an Uncertainty XADD
		 * 
		 */

//		Double mu = 50.0;
//		Double sigma = 25.0;
        Integer uncertaintyXADD = PBVIDiscreteAction2.InitialiseUncertaintyXADD(10.0, 1.0);
        HashSet<String> m = new HashSet<String>();
        m.add("v");
        m.add("i");

		/*
		 * Initialise the Value function at time zero (GammaSet)
		 */
		GammaSet prevGamma = new GammaSet(0);
		prevGamma.addVector(new AlphaVector(0));

		GammaSet currGamma = null;

		/*
		 * Execute the PBVI algorithm
		 */
		
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
					rFunc = XADDWrapper.substitute(mmPOMDP.rewardFunc, bidAskMap);
//					XADDHelper.PlotXADD(rFunc, "rFunc " + actionName);

					AlphaVector optActAVec = new AlphaVector(horizon, actionName);

					// Iterate through all of the observations in the observationSet
					for(String obsName : observationSet){
						System.out.println("\t\t\tO: " + obsName);

						// Initialise a GammaSet to hold all of the AlphaVectors
						// for the current (horizon, beliefPointID, actionName, obsName)
						// combination
						GammaSet currBPActObsGamma = new GammaSet(horizon, beliefPointID, actionName, obsName);

						Integer oFunc = oFuncMap.get(obsName);
						Integer oFuncPrimed = XADDWrapper.substitute(oFunc, stateSetMap);
						
						// Substitute the "ap" and "bp" variables in the oFuncPrimed 
						// with the contents of the bidAskMap
						oFuncPrimed = XADDWrapper.substitute(oFuncPrimed, bidAskMap);
//						XADDHelper.PlotXADD(oFuncPrimed, "oFuncPrimed " + actionName + " " + obsName);
						
						// Iterate through all of the alpha vectors in the prevGamma
						// set
						Iterator<AlphaVector> aVecIterator = prevGamma.iterator();
						while(aVecIterator.hasNext()) {
							AlphaVector prevAVec = aVecIterator.next();
							System.out.println("\t\t\t\tPrev " + prevAVec);

							// Initialise a new AlphaVector for the (actionName, obsName)
							AlphaVector newAVec = new AlphaVector(horizon, actionName, obsName); 

							Integer newAVecXADD = prevAVec.getXADD(); 
//							XADDHelper.Display3D(newAVecXADD, "prevAVec", 
//									PBVIDiscreteAction.DOMAIN_DIR_PATH + File.separator + "plot", 
//									PBVIDiscreteAction.DOMAIN_DIR_PATH.replace(".", File.separator));

							// Prime the variables in the newAVecXADD
							newAVecXADD = XADDWrapper.substitute(newAVecXADD, stateSetMap);
//							XADDHelper.PlotXADD(newAVecXADD, "After prime");

							// Multiply through by the oFuncPrimed
							newAVecXADD = XADDWrapper.Apply(newAVecXADD, oFuncPrimed, XADDOperator.PROD);
//							XADDHelper.PlotXADD(newAVecXADD, "After * oFuncPrimed " + actionName + " " + obsName);

							for(String currState : stateSetMap.keySet()) {
								System.out.println("\t\t\t\t\tState = " + currState);

								String nextState = stateSetMap.get(currState).toString();

								Integer tFunc = tFuncMap.get(currState);

								// Substitute the "ap" and "bp" variables in the tFunc 
								// with the bidAskMap				    			
								tFunc = XADDWrapper.substitute(tFunc, bidAskMap);
//								XADDHelper.PlotXADD(tFunc, "tFunc " + currState + " " + actionName);

								// Regress the continuous variables
								newAVecXADD = VIHelper.RegressContinuousVariable(newAVecXADD, tFunc, nextState);
//								XADDHelper.PlotXADD(newAVecXADD, "After Regress " + nextState);
							}				    		

							// Discount
							newAVecXADD = XADDWrapper.ScalarOp(newAVecXADD, discountFactor, XADDOperator.PROD);
//							XADDHelper.PlotXADD(newAVecXADD, "After Discount");
							
							// Add the rFunc
							newAVecXADD = XADDWrapper.Apply(newAVecXADD, rFunc, XADDOperator.SUM);				
//							XADDHelper.PlotXADD(newAVecXADD, "After + Reward");							
//							XADDHelper.Display3D(newAVecXADD, "newAVec", 
//									PBVIDiscreteAction.DOMAIN_DIR_PATH + File.separator + "plot", 
//									PBVIDiscreteAction.DOMAIN_DIR_PATH.replace(".", File.separator));							
							
							// Add the newAVecXADD to the newAlphaVector 
							newAVec.setXADD(newAVecXADD);
							System.out.println("\t\t\t\tNew " + newAVec);

							// Add the newAVec to the currBPActObsGamma set
							currBPActObsGamma.addVector(newAVec);
//							System.out.println("\t\t\t\t" + currBPActObsGamma);   			
						}

						// Make sure that there is one new AlphaVector per AlphaVector
						// in the prevGamma set
						assert(currBPActObsGamma.size() == prevGamma.size()) :
							System.out.format("Incorrect number of AlphaVectors in currBPActObsGamma. Expected %d%n", prevGamma.size()) ;

						/*
						 *  Add the currBPActObsGamma to the policyData map under the horizon
						 */
						
						ArrayList<GammaSet> obsList = null;
						HashMap<String, ArrayList<GammaSet>> obMap = null;
						HashMap<Integer, HashMap<String, ArrayList<GammaSet>>> bpMap = null;

						if(policyData.containsKey(horizon)) {							
							bpMap = policyData.get(horizon);

							if(bpMap.containsKey(beliefPointID)) {
								obMap = bpMap.get(beliefPointID);

								if(obMap.containsKey(obsName)) {
									obsList = obMap.get(obsName);
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
						obMap.put(obsName, obsList);
						bpMap.put(beliefPointID, obMap);						
						policyData.put(horizon, bpMap);

						// Calculate argmax_{\alpha \in \Gamma^{a, o}} (\alpha \cdot b)
						// The optimal AlphaVector in the currBPActObsGamma for the current
						// beliefPointID
						AlphaVector optActObsAVec = PBVIDiscreteAction2.ArgMaxOperation(currBPActObsGamma, beliefPointID);
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
				AlphaVector optAVec = PBVIDiscreteAction2.ArgMaxOperation(currBPGamma, beliefPointID);
				System.out.format("\t%s. Opt %s%n", currBPGamma, optAVec);

				// Add the optAVec for the current beliefPointID to the currGamma set
				currGamma.addVector(optAVec);

				/*
				 *  Plot the optAVec
				 */				
				
				String plotTitle = "BP: " + beliefPointID + " " + optAVec.toString();
//				XADDHelper.PlotXADD(optAVec.getXADD(), plotTitle);

				XADDHelper.Display3D(optAVec.getXADD(), plotTitle.replace(":", "-"), 
						PBVIDiscreteAction2.DOMAIN_DIR_PATH + File.separator + "plot", 
						PBVIDiscreteAction2.DOMAIN_DIR_PATH.replace(".", File.separator));				
         
			}

			System.out.format("%s%n", currGamma);

			/*
			 *  Plot the optAVec as a function of uncertainty
			 */
			
//			for(AlphaVector aVec : currGamma) {
//				
//	            Integer innerProdXADD = XADDWrapper.Apply(uncertaintyXADD, aVec.getXADD(), XADDOperator.PROD);
//	            XADDHelper.PlotXADD(innerProdXADD, "innerProdXADD");
//	            innerProdXADD = PBVIDiscreteAction.Integrate(innerProdXADD, m);
//	            
//	            String plotTitle2 = aVec.toString() + " Uncertainty";
//	            XADDHelper.PlotXADD(innerProdXADD, plotTitle2);
//	            
//	            XADDHelper.Display3D(innerProdXADD, plotTitle2.replace(":", "-"), 
//	            		PBVIDiscreteAction.DOMAIN_DIR_PATH + File.separator + "u_plot", 
//	            		PBVIDiscreteAction.DOMAIN_DIR_PATH.replace(".", File.separator));
//			}
		}

		/*
		 * Policy Extraction
		 */

		// Iterate through all belief points in the beliefSet
		for (Integer beliefPointID : beliefSet) {
			System.out.println("\tBP: " + beliefPointID);

			PBVIDiscreteAction2.ExtractPolicy(numIterations, beliefPointID, null, policyData);
		}
	}

	public static void Run2(POMDPTest mmPOMDP, ArrayList<Integer> beliefSet,
			Integer numIterations, 
			Double discountFactor) {
		
		/*
		 * Extract information from the mmPOMDP
		 */
		HashMap<String, ArithExpr> stateSetMap = mmPOMDP.stateMap;	
		HashSet<String> observationSet = mmPOMDP.observationSet;
		HashSet<String> actionSet = mmPOMDP.actionSet;

//		Integer rFunc = mmPOMDP.rewardFunc;
		Integer rFunc = null;
		HashMap<String, Integer> tFuncMap = mmPOMDP.transitionFuncMap;
		HashMap<String, Integer> rFuncMap = mmPOMDP.rewardFuncMap;
		HashMap<String, Integer> oFuncMap = mmPOMDP.observationFuncMap;

		/*
		 * Initialise data structure to hold the information needed to extract a 
		 * policy
		 */

		//		HashMap<Integer, ArrayList<GammaSet>> policyData = new HashMap<Integer, ArrayList<GammaSet>>();
		// { horizon -> { beliefPointID -> { ObservationName -> { GammaSets } } } }
		HashMap<Integer, HashMap<Integer, HashMap<String, ArrayList<GammaSet>>>> 
		policyData = new HashMap<Integer, HashMap<Integer, HashMap<String, ArrayList<GammaSet>>>>(); 

		/* 
		 * Initialise an Uncertainty XADD		 * 
		 */

        Integer uncertaintyXADD = PBVIDiscreteAction2.InitialiseUncertaintyXADD(10.0, 1.0);
        HashSet<String> m = new HashSet<String>();
        m.add("x");
		
		/*
		 * Initialise the Value function at time zero (GammaSet)
		 */
		GammaSet prevGamma = new GammaSet(0);
		prevGamma.addVector(new AlphaVector(0));

		GammaSet currGamma = null;

		/*
		 * Execute the PBVI algorithm
		 */
		
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
				for(String actionName : actionSet) {
					System.out.println("\t\tA: " + actionName);
					
					rFunc = rFuncMap.get(actionName);
//					XADDHelper.PlotXADD(rFunc, "rFunc");
					
					AlphaVector optActAVec = new AlphaVector(horizon, actionName);

					// Iterate through all of the observations in the observationSet
					for(String obsName : observationSet){
						System.out.println("\t\t\tO: " + obsName);

						// Initialise a GammaSet to hold all of the AlphaVectors
						// for the current (horizon, beliefPointID, actionName, obsName)
						// combination
						GammaSet currBPActObsGamma = new GammaSet(horizon, beliefPointID, actionName, obsName);

						Integer oFunc = oFuncMap.get(obsName);
						Integer oFuncPrimed = XADDWrapper.substitute(oFunc, stateSetMap);
//						XADDHelper.PlotXADD(oFuncPrimed, "oFuncPrimed");
						
						// Iterate through all of the alpha vectors in the prevGamma
						// set
						Iterator<AlphaVector> aVecIterator = prevGamma.iterator();
						while(aVecIterator.hasNext()) {
							AlphaVector prevAVec = aVecIterator.next();
							System.out.println("\t\t\t\tPrev " + prevAVec);

							// Initialise a new AlphaVector for the (actionName, obsName)
							AlphaVector newAVec = new AlphaVector(horizon, actionName, obsName); 

							Integer newAVecXADD = prevAVec.getXADD();

							// Prime the variables in the newAVecXADD
							newAVecXADD = XADDWrapper.substitute(newAVecXADD, stateSetMap);
//							XADDHelper.PlotXADD(newAVecXADD, "After prime");

							// Multiply through by the oFuncPrimed
							newAVecXADD = XADDWrapper.Apply(newAVecXADD, oFuncPrimed, XADDOperator.PROD);
//							XADDHelper.PlotXADD(newAVecXADD, "After * oFuncPrimed " + actionName + " " + obsName);

							for(String currState : stateSetMap.keySet()) {
								System.out.println("\t\t\t\t\tState = " + currState);

								String nextState = stateSetMap.get(currState).toString();

								Integer tFunc = tFuncMap.get(actionName);
//								XADDHelper.PlotXADD(tFunc, "tFunc");

								// Regress the continuous variables
								newAVecXADD = VIHelper.RegressContinuousVariable(newAVecXADD, tFunc, nextState);
//								XADDHelper.PlotXADD(newAVecXADD, "After Regress " + nextState);
							}				    		

							// Discount
							newAVecXADD = XADDWrapper.ScalarOp(newAVecXADD, discountFactor, XADDOperator.PROD);
//							XADDHelper.PlotXADD(newAVecXADD, "After Discount");
							
							// Add the rFunc							
							newAVecXADD = XADDWrapper.Apply(newAVecXADD, rFunc, XADDOperator.SUM);				
//							XADDHelper.PlotXADD(newAVecXADD, "After + Reward");							
//							XADDHelper.Display3D(newAVecXADD, "newAVec", 
//									PBVIDiscreteAction.DOMAIN_DIR_PATH + File.separator + "plot", 
//									PBVIDiscreteAction.DOMAIN_DIR_PATH.replace(".", File.separator));							
							
							// Add the newAVecXADD to the newAlphaVector 
							newAVec.setXADD(newAVecXADD);
							System.out.println("\t\t\t\tNew " + newAVec);

							// Add the newAVec to the currBPActObsGamma set
							currBPActObsGamma.addVector(newAVec);
//							System.out.println("\t\t\t\t" + currBPActObsGamma);   			
						}

						// Make sure that there is one new AlphaVector per AlphaVector
						// in the prevGamma set
						assert(currBPActObsGamma.size() == prevGamma.size()) :
							System.out.format("Incorrect number of AlphaVectors in currBPActObsGamma. Expected %d%n", prevGamma.size()) ;

						/*
						 *  Add the currBPActObsGamma to the policyData map under the horizon
						 */
						
						ArrayList<GammaSet> obsList = null;
						HashMap<String, ArrayList<GammaSet>> obMap = null;
						HashMap<Integer, HashMap<String, ArrayList<GammaSet>>> bpMap = null;

						if(policyData.containsKey(horizon)) {							
							bpMap = policyData.get(horizon);

							if(bpMap.containsKey(beliefPointID)) {
								obMap = bpMap.get(beliefPointID);

								if(obMap.containsKey(obsName)) {
									obsList = obMap.get(obsName);
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
						obMap.put(obsName, obsList);
						bpMap.put(beliefPointID, obMap);						
						policyData.put(horizon, bpMap);

						// Calculate argmax_{\alpha \in \Gamma^{a, o}} (\alpha \cdot b)
						// The optimal AlphaVector in the currBPActObsGamma for the current
						// beliefPointID
						AlphaVector optActObsAVec = PBVIDiscreteAction2.ArgMaxOperation(currBPActObsGamma, beliefPointID);
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
				AlphaVector optAVec = PBVIDiscreteAction2.ArgMaxOperation(currBPGamma, beliefPointID);
				System.out.format("\t%s. Opt %s%n", currBPGamma, optAVec);

				// Add the optAVec for the current beliefPointID to the currGamma set
				currGamma.addVector(optAVec);

				/*
				 *  Plot the optAVec
				 */				
				
				String plotTitle = "BP: " + beliefPointID + " " + optAVec.toString();
				XADDHelper.PlotXADD(optAVec.getXADD(), plotTitle);

				XADDHelper.Display2D(optAVec.getXADD(), plotTitle.replace(":", "-"), 
						PBVIDiscreteAction2.DOMAIN_DIR_PATH + File.separator + "plot", 
						PBVIDiscreteAction2.DOMAIN_DIR_PATH.replace(".", File.separator));				

				/*
				 *  Plot the optAVec as a function of uncertainty
				 */
				
//	            Integer innerProdXADD = XADDWrapper.Apply(uncertaintyXADD, optAVec.getXADD(), XADDOperator.PROD);
////	            XADDHelper.PlotXADD(innerProdXADD, "innerProdXADD");
//	            
//	            // Integrate out the variables not related to uncertainty
//	            innerProdXADD = PBVIDiscreteAction.Integrate(innerProdXADD, m);
//	            String plotTitle2 = optAVec.toString() + " Uncertainty";
////	            XADDHelper.PlotXADD(innerProdXADD, plotTitle2);
//	            
//	            XADDHelper.Display2D(innerProdXADD, plotTitle2.replace(":", "-"), 
//	            		PBVIDiscreteAction.DOMAIN_DIR_PATH + File.separator + "u_plot", 
//	            		PBVIDiscreteAction.DOMAIN_DIR_PATH.replace(".", File.separator));	            
			}

			System.out.format("%s%n", currGamma);
			
			for(AlphaVector aVec : currGamma) {
			
            Integer innerProdXADD = XADDWrapper.Apply(uncertaintyXADD, aVec.getXADD(), XADDOperator.PROD);
            XADDHelper.PlotXADD(innerProdXADD, "innerProdXADD");
            
            // Integrate out the variables not related to uncertainty
            innerProdXADD = PBVIDiscreteAction2.Integrate(innerProdXADD, m);
            String plotTitle2 = aVec.toString() + " Uncertainty";
            XADDHelper.PlotXADD(innerProdXADD, plotTitle2);
            
            XADDHelper.Display2D(innerProdXADD, plotTitle2.replace(":", "-"), 
            		PBVIDiscreteAction2.DOMAIN_DIR_PATH + File.separator + "u_plot", 
            		PBVIDiscreteAction2.DOMAIN_DIR_PATH.replace(".", File.separator));	 
			}
		}


		/*
		 * Policy Extraction
		 */

		// Iterate through all belief points in the beliefSet
		for (Integer beliefPointID : beliefSet) {
			System.out.println("\tBP: " + beliefPointID);

			PBVIDiscreteAction2.ExtractPolicy(numIterations, beliefPointID, null, policyData);
		}		
		
	}
	
	/**
	 * Determines whether a belief point is valid. i.e. if it integrates to 1.0
	 * 
	 * @param 	bpID 			(Integer)
	 * @return	validBelief?	(Boolean)
	 */
	private static boolean IsValidBeliefPoint(Integer bpID) {

		boolean validBP = true;
		
		// Integrate out all variables in the bpID XADD
		bpID = PBVIDiscreteAction2.Integrate(bpID);
		
		XADDTNode terminalNode = XADDWrapper.getNode(bpID);
		Double val = ((DoubleExpr) terminalNode._expr)._dConstVal;	
		
		assert(Math.abs(val - 1.0) < PBVIDiscreteAction2.EPSILON) : 
			"Belief point does not integrate out to 1.0";

		return validBP;
	}

	/**
	 * 
	 * @param xaddID
	 * @return
	 */
	private static Integer Integrate(Integer xaddID) {
		
		// Get the variables in the bpID XADD
		HashSet<String> varsToRegress = XADDWrapper.collectVars(xaddID);
		
		return PBVIDiscreteAction2.Integrate(xaddID, varsToRegress);
	}

	/**
	 * 
	 * @param xaddID
	 * @param vars
	 * @return
	 */
	private static Integer Integrate(Integer xaddID, HashSet<String> vars) {
	
		for (String var : vars) {
			xaddID = XADDWrapper.computeDefiniteIntegral(xaddID, var);
		}
		
		return xaddID;
	}
	
	
	/**
	 * 
	 * @param mu
	 * @param sigma
	 * @return
	 */
	private static Integer InitialiseUncertaintyXADD(Double mu, Double sigma) {
		String uBPFile = PBVIDiscreteAction2.DOMAIN_DIR_PATH + File.separator + "uncertainty_belief_point_1.xadd";
//		String uBPFile = PBVIDiscreteAction.DOMAIN_DIR_PATH + File.separator + "uncertainty_belief_point.xadd";

		Integer uncertaintyXADD = XADDHelper.BuildXADD(uBPFile);

//		HashMap<String, ArithExpr> a1 = new HashMap<String, ArithExpr>();
////		a1.put("m", new DoubleExpr(mu));
////		a1.put("w", new DoubleExpr(sigma));		
//		a1.put("sq", new DoubleExpr(Math.sqrt(3)));
//		uncertaintyXADD = XADDWrapper.substitute(uncertaintyXADD, a1);
//
		XADDHelper.PlotXADD(uncertaintyXADD, "Uncertainty XADD");
		
//		uncertaintyXADD = XADDWrapper.computeDefiniteIntegral(uncertaintyXADD, "x");
//		XADDHelper.PlotXADD(uncertaintyXADD, "Uncertainty XADD Mu: " + mu + " Sigma: " + sigma);
//		int integral2 = XADDWrapper.computeDefiniteIntegral(integral1, "i");

//		XADDTNode t = XADDWrapper.getNode(integral2);
//		Double val = ((DoubleExpr) t._expr)._dConstVal;
//
//		assert(Math.abs(val - 1.0) < PBVIDiscreteAction.EPSILON) : 
//			"Distribution does not integrate out to 1.0";		

		return uncertaintyXADD;
	}	

	/**
	 * Extract Policy
	 * 
	 * @param horizon
	 * @param policyData
	 */
	private static void ExtractPolicy(Integer horizon, Integer beliefPointID, String actionName, 
			HashMap<Integer, HashMap<Integer, HashMap<String, ArrayList<GammaSet>>>> policyData) {

		// Get the data relevant to the horizon and beliefPointID
		HashMap<Integer, HashMap<String, ArrayList<GammaSet>>> horizonData = policyData.get(horizon);
		HashMap<String, ArrayList<GammaSet>> bpData = horizonData.get(beliefPointID);
		String optActionName = null;

		GammaSet bpGammaSet = new GammaSet(horizon, beliefPointID, actionName);
		GammaSet bpGammaSet2 = new GammaSet(horizon, beliefPointID);

		for (Entry<String, ArrayList<GammaSet>> entry : bpData.entrySet()) {
			String obsName = entry.getKey();
			ArrayList<GammaSet> gammaSetList = entry.getValue();

			//Iterate through all of the AlphaVectors in the gammaSetList
			for ( GammaSet gammaSet: gammaSetList) {

				// Skip the gammaSet if it doesn't relate to the actionName
				if(actionName != null && gammaSet.getAction() != actionName) {
					continue;
				}

				// Add all AlphaVectors in the gammaSet to the bpGammaSet
				for (AlphaVector aVec : gammaSet) {
					bpGammaSet.addVector(aVec);
				}
			}

			// Calculate argmax_{\alpha \in \Gamma^{a, b}, \forall a \in A} (\Gamma^{a, b} \cdot b)
			// The optimal AlphaVector in the bpGammaSet for the current beliefPointID
			AlphaVector optAVec = PBVIDiscreteAction2.ArgMaxOperation(bpGammaSet, beliefPointID);
			optActionName = optAVec.getActionName();

			if(actionName == null) {
				bpGammaSet2.addVector(optAVec);
			}
			else {
				System.out.format("BP %d Horizon %d Action %s Obs = %s -> Action %s %n", beliefPointID, horizon, actionName, obsName, optActionName);

				if (horizon > 1) {
					System.out.format("Recurse PBVIDiscreteAction.ExtractPolicy(%d, %d, %s) Obs = %s %n", horizon - 1, beliefPointID, optActionName, obsName);
					PBVIDiscreteAction2.ExtractPolicy(horizon - 1, beliefPointID, optActionName, policyData);
				}
			}
		}

		if(actionName == null) {
			AlphaVector optAVec = PBVIDiscreteAction2.ArgMaxOperation(bpGammaSet2, beliefPointID);
			optActionName = optAVec.getActionName();
		}

		System.out.format("BP %d Horizon %d Action %s -> Action %s %n", beliefPointID, horizon, actionName, optActionName);
		System.out.format("PBVIDiscreteAction.ExtractPolicy(%d, %d, %s) %n", horizon, beliefPointID, optActionName);

		if (horizon > 1) {			
			System.out.format("Recurse PBVIDiscreteAction.ExtractPolicy(%d, %d, %s) %n", horizon - 1, beliefPointID, optActionName);
			PBVIDiscreteAction2.ExtractPolicy(horizon - 1, beliefPointID, optActionName, policyData);
		}
	}

	/**
	 * Expand belief set
	 * 
	 * @param mmPOMDP
	 * @param beliefSet
	 * @return
	 */
	private static ArrayList<Integer> ExpandBeliefSet(POMDP mmPOMDP, ArrayList<Integer> beliefSet) {

		/*
		 * Extract information from the mmPOMDP
		 */
		HashMap<String, HashMap<String, ArithExpr>> actionSetMap = mmPOMDP.actionMap;
		HashMap<String, ArithExpr> stateSetMap = mmPOMDP.stateMap;	
		HashSet<String> observationSet = mmPOMDP.observationSet;

		HashMap<String, Integer> tFuncMap = mmPOMDP.transitionFuncMap;
		HashMap<String, Integer> oFuncMap = mmPOMDP.observationFuncMap;

		ArrayList<Integer> newBeliefSet = new ArrayList<Integer>();

		// Iterate through all belief points in the beliefSet
		for (Integer beliefPointID : beliefSet) {
			System.out.println("\tBP: " + beliefPointID);

			// Iterate through the actions in the actionSetMap
			Iterator<String> aKeyIterator = actionSetMap.keySet().iterator();
			while(aKeyIterator.hasNext()) {
				String actionName = aKeyIterator.next();
				HashMap<String, ArithExpr> bidAskMap = actionSetMap.get(actionName);

				System.out.println("\t\tA: " + actionName);

				// Iterate through all of the observations in the observationSet
				for(String obsName : observationSet){
					System.out.println("\t\t\tO: " + obsName);

					Integer oFunc = oFuncMap.get(obsName);
					Integer oFuncPrimed = XADDWrapper.substitute(oFunc, stateSetMap);

					// Substitute the "ap" and "bp" variables in the oFuncPrimed 
					// with the contents of the bidAskMap
					oFuncPrimed = XADDWrapper.substitute(oFuncPrimed, bidAskMap);

					Integer newBeliefPoint = beliefPointID;

					// Prime the variables in the newBeliefPoint
					newBeliefPoint = XADDWrapper.substitute(newBeliefPoint, stateSetMap);
					//			    			XADDHelper.PlotXADD(newBeliefPoint, "After prime");

					// Multiply through by the oFuncPrimed
					newBeliefPoint = XADDWrapper.Apply(newBeliefPoint, oFuncPrimed, XADDOperator.PROD);					

					for(String currState : stateSetMap.keySet()) {
						System.out.println("\t\t\t\t\tState = " + currState);

						String nextState = stateSetMap.get(currState).toString();

						Integer tFunc = tFuncMap.get(currState);

						// Substitute the "ap" and "bp" variables in the tFunc with the bidAskMap				    			
						tFunc = XADDWrapper.substitute(tFunc, bidAskMap);

						// Regress the continuous variables
						newBeliefPoint = VIHelper.RegressContinuousVariable(newBeliefPoint, tFunc, nextState);
						//				    			XADDHelper.PlotXADD(newAVecXADD, "After Regress " + stateSetMap.get(currState).toString());

						// Calculate the distance between the current beliefPoint and
						// the newBeliefPoint (L2 norm)
						Integer diffXADD = XADDWrapper.Apply(beliefPointID, newBeliefPoint, XADDOperator.MINUS);
						Integer normXADD = XADDWrapper.Apply(diffXADD, diffXADD, XADDOperator.PROD);
					}				    									
				}
			}
		}

		return beliefSet;
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

		//		XADDHelper.PlotXADD(beliefPoint, "BP: " + beliefPoint);

		if(gammaSet.size() == 1) {
			return gammaSet.getVectors().get(0);
		}

		// Iterate through each AlphaVector in the gammaSet
		for (AlphaVector currAlphaV : gammaSet) {
//			XADDHelper.PlotXADD(currAlphaV.getXADD(), currAlphaV.toString());

			// The inner product of the currAlphaV and the  beliefPoint
			innerProdXADD = XADDWrapper.Apply(currAlphaV.getXADD(), beliefPoint, XADDOperator.PROD);
//			XADDHelper.PlotXADD(innerProdXADD, "Inner Product BP: " + beliefPoint + " * " + currAlphaV.toString());

			// Compute the definite integral of the innerProdXADD over the continuous
			// variables
			
			// Integrate out all variables in the innerProdXADD
			innerProdXADD = PBVIDiscreteAction2.Integrate(innerProdXADD);
			
//			Integer int1 =  XADDWrapper.computeDefiniteIntegral(innerProdXADD, "i");
//			//			XADDHelper.PlotXADD(int1, "Integral 1 v");
//			Integer int2 =  XADDWrapper.computeDefiniteIntegral(int1, "v");

			// The result ...
			XADDTNode t = XADDWrapper.getNode(innerProdXADD);
			innerProdValue = ((DoubleExpr) t._expr)._dConstVal;
//			System.out.format("%s x %s = %f%n", beliefPoint, currAlphaV, innerProdValue);

			if(innerProdValue > maxInnerProdValue) {
				maxAlphaVector = currAlphaV;
				maxInnerProdValue = innerProdValue; 
			}
		}		

		return maxAlphaVector;
	}

}