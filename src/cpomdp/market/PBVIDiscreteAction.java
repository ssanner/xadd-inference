/**
 * 
 */

package cpomdp.market;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import util.DevNullPrintStream;
import xadd.ExprLib.ArithExpr;
import xadd.ExprLib.DoubleExpr;
import xadd.XADD.XADDTNode;
import cpomdp.market.utils.VIHelper;
import cpomdp.market.utils.XADDHelper;
import cpomdp.market.utils.XADDOperator;
import cpomdp.market.utils.XADDWrapper;

/**
 * @author skinathil
 *
 */
public class PBVIDiscreteAction {

	public static String DOMAIN_DIR_PATH = null;
	public static String OUTPUT_DIR_NAME = "output";

	public static final Boolean SHOW_PLOTS = false;
	
	/**
	 * 
	 * @param mmPOMDP
	 * @param beliefSet
	 * @param numIterations
	 * @param discountFactor
	 */
	public static void Run(MMPOMDP mmPOMDP, ArrayList<Integer> beliefSet,
														Integer numIterations, 
														Double discountFactor, 
														String outputDirPath,
														Boolean showPlots) {

		/*
		 * Extract information from the mmPOMDP
		 */
		
		HashMap<String, HashMap<String, ArithExpr>> actionSetMap = mmPOMDP.actionMap;
		HashMap<String, ArithExpr> stateSetMap = mmPOMDP.stateMap;	
		Set<String> observationSet = mmPOMDP.observationSet;

		Integer rFunc = null;
		HashMap<String, Integer> tFuncMap = mmPOMDP.transitionFuncMap;
		HashMap<String, Integer> oFuncMap = mmPOMDP.observationFuncMap;

		Set<String> continuousVarSet = stateSetMap.keySet();
		
		String domainDirPath = mmPOMDP.domain.getDomainPath();

		// Create the outputDirPath folder, if it doesn't already exist
		try {
			Files.createDirectory(Paths.get(outputDirPath));
		} catch (IOException e) {
			e.printStackTrace();
			outputDirPath = "";
		}
		
		/*
		 * Initialise data structure to hold the information needed to extract a 
		 * policy
		 */

		// { horizon -> { beliefPointID -> { ObservationName -> { GammaSets } } } }
		PolicyData policyData2 = new PolicyData();
		
		AltPolicyData policyDataAlt = new AltPolicyData();

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
						policyData2.updatePolicy(horizon, beliefPointID, obsName, currBPActObsGamma);
						
						policyDataAlt.updatePolicy(horizon, obsName, currBPActObsGamma);

						// Calculate argmax_{\alpha \in \Gamma^{a, o}} (\alpha \cdot b)
						// The optimal AlphaVector in the currBPActObsGamma for the current
						// beliefPointID
						AlphaVector optActObsAVec = PBVI.ArgMax(currBPActObsGamma, beliefPointID);
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
				AlphaVector optAVec = PBVI.ArgMax(currBPGamma, beliefPointID);
				optAVec.setBeliefPointID(beliefPointID);
				System.out.format("\t%s. Opt %s%n", currBPGamma, optAVec);

				// Add the optAVec for the current beliefPointID to the currGamma set
				currGamma.addVector(optAVec);

				/*
				 *  Plot the optAVec
				 */				

				if (PBVIDiscreteAction.SHOW_PLOTS) {
					String plotTitle = "BP: " + beliefPointID + " " + optAVec.toString();
					XADDHelper.PlotXADD(optAVec.getXADD(), plotTitle);

					XADDHelper.Display3D(optAVec.getXADD(), plotTitle.replace(":", "-"), 
						domainDirPath + "plot", outputDirPath);
				}

			}

			System.out.format("%s%n", currGamma);

			/*
			 *  Plot the value function (currGamma) at the current horizon
			 */

			if (PBVIDiscreteAction.SHOW_PLOTS)
				PBVIDiscreteAction.PlotValueFunction(currGamma, continuousVarSet, 
					50.0, 0.0, 1.0, 
					50.0, 0.0, 1.0, 
					outputDirPath, domainDirPath);						
		}

		/*
		 * Policy Extraction
		 */
		Integer peBPID = PBVI.InitialiseBeliefPoint(domainDirPath + "policy_extraction_belief_point_1.xadd");
		ArrayList<Integer> peBPList = new ArrayList<Integer>();
		peBPList.add(peBPID);
		
//		PBVIDiscreteAction.ExtractPolicies(peBPList, numIterations, policyData2);
		PBVIDiscreteAction.ExtractPolicies(peBPList, numIterations, policyDataAlt);
	}

	public static void ExtractPolicies(ArrayList<Integer> beliefSet, 
							Integer ultimateHorizon, AltPolicyData policyData) {

		// Iterate through all belief points in the beliefSet
		for (Integer beliefPointID : beliefSet) {
			System.out.format("Extract Policy for %s%n", beliefPointID);

			PBVIDiscreteAction.ExtractPolicy(ultimateHorizon, beliefPointID, null, policyData);
		}				
	}

	public static void ExtractPolicy(Integer currHorizon, Integer beliefPointID, 
			String actionName, AltPolicyData policyData) {
	
		GammaSet gammaSet = null;
		GammaSet gammaSet2 = null;
		GammaSet gammaSet3 = null;
		GammaSet allAlpha = null;
		String optActionName = null;
		String optObsActionName = null;
		
		Boolean recurseFlag = currHorizon > 1 ? true : false;

		if(actionName == null) {
			gammaSet = new GammaSet(currHorizon, beliefPointID);
			gammaSet2 = new GammaSet(currHorizon, beliefPointID);
			gammaSet3 = new GammaSet(currHorizon, beliefPointID);
			
			allAlpha = new GammaSet(currHorizon, beliefPointID);
		} 
		else {
			gammaSet = new GammaSet(currHorizon, beliefPointID, actionName);
			gammaSet2 = new GammaSet(currHorizon, beliefPointID, actionName);			
			gammaSet3 = new GammaSet(currHorizon, beliefPointID, actionName);
		}

		Set<String> obsNames = policyData.getObservationNames(currHorizon);
		
		// Iterate through the obsNames
		for(String obsName : obsNames) {
			
			System.out.format("H: %d O: %s A: %s %n", currHorizon, obsName, actionName == null ? "-" : actionName);
			
			// The data pertaining to the currHorizon, beliefPointID and obsName
			ArrayList<GammaSet> obsGammaSetList = policyData.getObservationData(currHorizon, obsName);
			
			//Iterate through all of the AlphaVectors in the obsGammaSetList
			for (GammaSet obsGammaSet: obsGammaSetList) {
				
				if(actionName == null) {
					allAlpha.mergeGammaSet(obsGammaSet);
					continue;
				}
				
				if(actionName != null && obsGammaSet.getAction() == actionName) {
					// The optimal AlphaVector for the currHorizon, beliefPointID and obsName
					AlphaVector optAVec = PBVI.ArgMax(obsGammaSet, beliefPointID);
					gammaSet2.addVector(optAVec);
				}
			}
			
			if(gammaSet2.size() > 0) {
				AlphaVector optAVec = PBVI.ArgMax(gammaSet2, beliefPointID);
				optObsActionName = optAVec.getActionName();
	
				System.out.format("\tOpt: %s%n", optObsActionName);
				
				if(recurseFlag)
					PBVIDiscreteAction.ExtractPolicy(currHorizon - 1, beliefPointID, optObsActionName, policyData);		
			}
		}
		
		if(actionName == null) {
			AlphaVector optAVec = PBVI.ArgMax(allAlpha, beliefPointID);
			optActionName = optAVec.getActionName();
			
			System.out.format("\tOpt: %s%n", optActionName);
			
			if(recurseFlag)
				PBVIDiscreteAction.ExtractPolicy(currHorizon - 1, beliefPointID, optActionName, policyData);
		}		
	}
	
	/**
	 * 
	 * @param gammaSet
	 * @param continuousVarSet
	 * @param highX
	 * @param lowX
	 * @param incrementX
	 * @param highY
	 * @param lowY
	 * @param incrementY
	 */
	private static void PlotValueFunction(GammaSet gammaSet, Set<String> continuousVarSet,
			Double highX, double lowX, Double incrementX, 
			Double highY, double lowY, Double incrementY, 
			String outputDirPath, String domainDirPath) {

		/*
		 *  Plot the optAVec as a function of uncertainty
		 */

		System.out.format("Plotting %s%n", gammaSet);

		Integer numXSamples = (int) Math.ceil((highX - lowX)/incrementX);
		Integer numYSamples = (int) Math.ceil((highY - lowY)/incrementY);

		ArrayList<Float> alX = new ArrayList<Float>(numXSamples);
		ArrayList<Float> alY = new ArrayList<Float>(numYSamples);

		float temp_x = (float) lowX;
		for (int i = 0; i < numXSamples; i++) {
			temp_x += incrementX;
			alX.add(temp_x);
		}			

		float temp_y = (float) lowY;
		for (int i = 0; i < numYSamples; i++) {
			temp_y += incrementY;
			alY.add(temp_y);
		}			

		for(AlphaVector aVec : gammaSet) {

			float[][] xArr = new float[alY.size()][alX.size()];
			float[][] yArr = new float[alY.size()][alX.size()];
			float[][] zArr = new float[alY.size()][alX.size()];

			Integer uncertaintyXADD = null;

			String title = aVec.toString().replace("AVec ", "").replace(" ", "_")
					.replace('^', '_').replace("(", "").replace(")", "")
					.replace("[", "").replace("]", "").replace(":", "-")
					.replace(" ", "").replace(".dot", "");
			String outputFileName = outputDirPath + File.separator + title + ".txt";

			PrintStream ps = null;				
			try {
				ps = new PrintStream(new FileOutputStream(outputFileName));
			} catch (Exception e) {
				System.err.println("Could not open " + outputFileName + " for data export.");
				ps = new DevNullPrintStream();
			}

			for (int i = 0; i < alY.size(); i++) {
				for (int j = 0; j < alX.size(); j++) {

					float mean = alX.get(j);
					float width = alY.get(i);

					uncertaintyXADD = PBVIDiscreteAction.InitialiseUncertaintyXADD(domainDirPath,mean, width);

					Integer innerProdXADD = XADDWrapper.Apply(uncertaintyXADD, aVec.getXADD(), XADDOperator.PROD);
					innerProdXADD = PBVI.Integrate(innerProdXADD, continuousVarSet);

					// Get the value of the innerProdXADD
					XADDTNode t = XADDWrapper.getNode(innerProdXADD);
					double innerProdValue = ((DoubleExpr) t._expr)._dConstVal;

					xArr[i][j] = mean;
					yArr[i][j] = width;
					zArr[j][i] = (float) innerProdValue;

					ps.println(mean + "\t" + width + "\t" + (float) innerProdValue);
				}
			}
			ps.close();

			System.out.format("%s%n", outputFileName);

			XADDHelper.Plot3D(title, outputFileName, xArr, yArr, zArr, alX.size(), alY.size());
		}
	}

	/**
	 * 
	 * @param mean
	 * @param width
	 * @return
	 */
	private static Integer InitialiseUncertaintyXADD(String domainDirPath, float mean, float width) {

		String uBPFile = domainDirPath + "uncertainty_belief_point.xadd";
		Integer uncertaintyXADD = XADDHelper.BuildXADD(uBPFile);
		
//		XADDHelper.PlotXADD(uncertaintyXADD, "Uncertainty XADD Before");
		
		Double prob = null;

		// Substitute values for "lb", "ub" and "p"
		HashMap<String, ArithExpr> subs = new HashMap<String, ArithExpr>();
		subs.put("m", new DoubleExpr(mean));
		
//		if(mean < width) {
//			subs.put("lb", new DoubleExpr(0));
//			prob = Math.pow((1/(0 + width)), 2);
//		}
//		else {
			subs.put("lb", new DoubleExpr(width));
			prob = Math.pow((1/(width + width)), 2);
//		}
		subs.put("ub", new DoubleExpr(width));		

		subs.put("p", new DoubleExpr(prob));
		
		uncertaintyXADD = XADDWrapper.substitute(uncertaintyXADD, subs);
		
//		if(!PBVI.IsValidBeliefPoint(uncertaintyXADD, false)) {
//			System.out.format("INVALID: (%f, %f)%n", mean, width);
//			XADDHelper.PlotXADD(uncertaintyXADD, "Uncertainty XADD After (" + mean + ", " + width + ")");
//		} else {
//			System.out.format("VALID: (%f, %f)%n", mean, width);
//		}
		
		return uncertaintyXADD;		
	}

	/**
	 * 
	 * @param beliefSet
	 * @param ultimateHorizon
	 * @param policyData
	 */
	public static void ExtractPolicies(ArrayList<Integer> beliefSet, 
			Integer ultimateHorizon, PolicyData policyData) {

		// Iterate through all belief points in the beliefSet
		for (Integer beliefPointID : beliefSet) {
			System.out.format("Extract Policy for %s%n", beliefPointID);

			PBVIDiscreteAction.ExtractPolicy(ultimateHorizon, beliefPointID, null, policyData);
		}				
	}

	/**
	 * 
	 * @param currHorizon
	 * @param beliefPointID
	 * @param actionName
	 * @param policyData
	 */
	public static void ExtractPolicy(Integer currHorizon, Integer beliefPointID, 
									String actionName, PolicyData policyData) {

		GammaSet gammaSet = null;
		GammaSet gammaSet2 = null;
		GammaSet gammaSet3 = null;
		GammaSet allAlpha = null;
		String optActionName = null;
		String optObsActionName = null;
		
		Boolean recurseFlag = currHorizon > 1 ? true : false;

		if(actionName == null) {
			gammaSet = new GammaSet(currHorizon, beliefPointID);
			gammaSet2 = new GammaSet(currHorizon, beliefPointID);
			gammaSet3 = new GammaSet(currHorizon, beliefPointID);
			
			allAlpha = new GammaSet(currHorizon, beliefPointID);
		} 
		else {
			gammaSet = new GammaSet(currHorizon, beliefPointID, actionName);
			gammaSet2 = new GammaSet(currHorizon, beliefPointID, actionName);			
			gammaSet3 = new GammaSet(currHorizon, beliefPointID, actionName);
		}

		Set<String> obsNames = policyData.getObservationNames(currHorizon, beliefPointID);
		
		// Iterate through the obsNames
		for(String obsName : obsNames) {
			
			System.out.format("H: %d O: %s A: %s %n", currHorizon, obsName, actionName == null ? "-" : actionName);
			
			// The data pertaining to the currHorizon, beliefPointID and obsName
			ArrayList<GammaSet> obsGammaSetList = policyData.getObservationData(currHorizon, beliefPointID, obsName);
			
			//Iterate through all of the AlphaVectors in the obsGammaSetList
			for (GammaSet obsGammaSet: obsGammaSetList) {
				
				if(actionName == null) {
					allAlpha.mergeGammaSet(obsGammaSet);
					continue;
				}
				
				if(actionName != null && obsGammaSet.getAction() == actionName) {
					// The optimal AlphaVector for the currHorizon, beliefPointID and obsName
					AlphaVector optAVec = PBVI.ArgMax(obsGammaSet, beliefPointID);
					gammaSet2.addVector(optAVec);
				}
			}
			
			if(gammaSet2.size() > 0) {
				AlphaVector optAVec = PBVI.ArgMax(gammaSet2, beliefPointID);
				optObsActionName = optAVec.getActionName();
	
				System.out.format("\tOpt: %s%n", optObsActionName);
				
				if(recurseFlag)
					PBVIDiscreteAction.ExtractPolicy(currHorizon - 1, beliefPointID, optObsActionName, policyData);		
			}
		}
		
		if(actionName == null) {
			AlphaVector optAVec = PBVI.ArgMax(allAlpha, beliefPointID);
			optActionName = optAVec.getActionName();
			
			if(recurseFlag)
				PBVIDiscreteAction.ExtractPolicy(currHorizon - 1, beliefPointID, optActionName, policyData);
		}
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
			AlphaVector optAVec = PBVI.ArgMax(bpGammaSet, beliefPointID);
			optActionName = optAVec.getActionName();

			if(actionName == null) {
				bpGammaSet2.addVector(optAVec);
			}
			else {
				System.out.format("BP %d Horizon %d Action %s Obs = %s -> Action %s %n", beliefPointID, horizon, actionName, obsName, optActionName);

				if (horizon > 1) {
					System.out.format("Recurse PBVIDiscreteAction.ExtractPolicy(%d, %d, %s) Obs = %s %n", horizon - 1, beliefPointID, optActionName, obsName);
					PBVIDiscreteAction.ExtractPolicy(horizon - 1, beliefPointID, optActionName, policyData);
				}
			}
		}

		if(actionName == null) {
			AlphaVector optAVec = PBVI.ArgMax(bpGammaSet2, beliefPointID);
			optActionName = optAVec.getActionName();
		}

		System.out.format("BP %d Horizon %d Action %s -> Action %s %n", beliefPointID, horizon, actionName, optActionName);
		System.out.format("PBVIDiscreteAction.ExtractPolicy(%d, %d, %s) %n", horizon, beliefPointID, optActionName);

		if (horizon > 1) {			
			System.out.format("Recurse PBVIDiscreteAction.ExtractPolicy(%d, %d, %s) %n", horizon - 1, beliefPointID, optActionName);
			PBVIDiscreteAction.ExtractPolicy(horizon - 1, beliefPointID, optActionName, policyData);
		}
	}

	/**
	 * Expand belief set
	 * 
	 * @param mmPOMDP
	 * @param beliefSet
	 * @return
	 */
	private static ArrayList<Integer> ExpandBeliefSet(MMPOMDP mmPOMDP, ArrayList<Integer> beliefSet) {

		/*
		 * Extract information from the mmPOMDP
		 */
		HashMap<String, HashMap<String, ArithExpr>> actionSetMap = mmPOMDP.actionMap;
		HashMap<String, ArithExpr> stateSetMap = mmPOMDP.stateMap;	
		Set<String> observationSet = mmPOMDP.observationSet;

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

}