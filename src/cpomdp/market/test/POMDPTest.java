/**
 * 
 */
package cpomdp.market.test;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;

import xadd.ExprLib.ArithExpr;
import xadd.ExprLib.VarExpr;
import cpomdp.market.MMPOMDP;
import cpomdp.market.utils.XADDHelper;

/**
 * @author skinathil
 *
 */
public class POMDPTest extends MMPOMDP {

	/**
	 * 
	 * @param domainDir
	 * @return
	 */
	private static HashMap<String, Integer> InitialiseFromDomainFiles(String domainDir) {
		
		System.out.println("In POMDPTest.InitialiseFromDomainFiles(): " + domainDir);
		
		HashMap<String, Integer> xaddMap = new HashMap<String, Integer>();
		
		// Check that the domainDir exists
		File f = new File(domainDir);
		if (f.exists() && f.isDirectory()) {

			int transitionID = XADDHelper.BuildXADD(domainDir + File.separator + "transition_func.xadd");
			xaddMap.put("transitionFunc", transitionID);
//			XADDHelper.PlotXADD(transitionID, "transitionFunc");			
			
			int righttransitionID = XADDHelper.BuildXADD(domainDir + File.separator + "transition_func_right.xadd");
			xaddMap.put("transitionFuncRight", righttransitionID);
//			XADDHelper.PlotXADD(righttransitionID, "transitionFuncRight");
			
			int lefttransitionID = XADDHelper.BuildXADD(domainDir + File.separator + "transition_func_left.xadd");
			xaddMap.put("transitionFuncLeft", lefttransitionID);
//			XADDHelper.PlotXADD(lefttransitionID, "transitionFuncLeft");

			int staytransitionID = XADDHelper.BuildXADD(domainDir + File.separator + "transition_func_stay.xadd");
			xaddMap.put("transitionFuncStay", staytransitionID);
//			XADDHelper.PlotXADD(staytransitionID, "transitionFuncStay");
			
//			int observationID = XADDHelper.BuildXADD(domainDir + File.separator + "observation_func.xadd");
//			xaddMap.put("observationFunc", observationID);
////			XADDHelper.PlotXADD(observationID, "observationFunc");

			int inObservationID = XADDHelper.BuildXADD(domainDir + File.separator + "observation_func_in.xadd");
			xaddMap.put("observationFuncIn", inObservationID);
//			XADDHelper.PlotXADD(inObservationID, "observationFuncIn");

			int outObservationID = XADDHelper.BuildXADD(domainDir + File.separator + "observation_func_out.xadd");
			xaddMap.put("observationFuncOut", outObservationID);
//			XADDHelper.PlotXADD(outObservationID, "observationFuncOut");
						
			int rewardID = XADDHelper.BuildXADD(domainDir + File.separator + "reward_func.xadd");
			xaddMap.put("rewardFunc", rewardID);
//			XADDHelper.PlotXADD(rewardID, "rewardFunc");
			
			int rightRewardID = XADDHelper.BuildXADD(domainDir + File.separator + "reward_func_right.xadd");
			xaddMap.put("rewardFuncRight", rightRewardID);
//			XADDHelper.PlotXADD(rightRewardID, "rewardFuncRight");
			
			int leftRewardID = XADDHelper.BuildXADD(domainDir + File.separator + "reward_func_left.xadd");
			xaddMap.put("rewardFuncLeft", leftRewardID);
//			XADDHelper.PlotXADD(leftRewardID, "rewardFuncLeft");
//
			int stayRewardID = XADDHelper.BuildXADD(domainDir + File.separator + "reward_func_stay.xadd");
			xaddMap.put("rewardFuncStay", stayRewardID);
//			XADDHelper.PlotXADD(stayRewardID, "rewardFuncStay");			
			
		} 
		else {
			System.out.println("Unable to locate domainDir: " + domainDir);
		}
	
		return xaddMap;
	}	
	
	public HashSet<String> actionSet = null;
	public HashMap<String, Integer> rewardFuncMap = null;
	/**
	 * @param domainDir
	 */
	public POMDPTest(String domainDir) {
		super();
		
		// Load the domain files from the provided domainDir
		HashMap<String, Integer> xaddMap = POMDPTest.InitialiseFromDomainFiles(domainDir);
		
		observationSet = new HashSet<String>();
		stateMap = new HashMap<String, ArithExpr>();
		actionSet = new HashSet<String>();
		actionMap = new HashMap<String, HashMap<String, ArithExpr>>();
		transitionFuncMap = new HashMap<String, Integer>();
		observationFuncMap = new HashMap<String, Integer>();
		rewardFuncMap = new HashMap<String, Integer>();
		
		/*
		 * States
		 */

        stateMap.put("x", new VarExpr("x'"));
        
        /*
         * Actions
         */
        
        actionSet.add("right");
        actionSet.add("left");
        actionSet.add("stay");
		
		/*
		 * Observations
		 */
		observationSet.add("in");
		observationSet.add("out");
		
		/*
		 * Transition Function
		 */
		
		transitionFuncMap.put("x", xaddMap.get("transitionFunc"));
		
		transitionFuncMap.put("right", xaddMap.get("transitionFuncRight"));
		transitionFuncMap.put("left", xaddMap.get("transitionFuncLeft"));
		transitionFuncMap.put("stay", xaddMap.get("transitionFuncStay"));
		
		/*
		 * Observation Function
		 */
//		observationFuncMap.put("in", xaddMap.get("observationFunc"));
		observationFuncMap.put("in", xaddMap.get("observationFuncIn"));
		observationFuncMap.put("out", xaddMap.get("observationFuncOut"));		
		
		/*
		 * Reward Function
		 */
		rewardFunc = xaddMap.get("rewardFunc");
		
		rewardFuncMap.put("right", xaddMap.get("rewardFuncRight"));
		rewardFuncMap.put("left", xaddMap.get("rewardFuncLeft"));
		rewardFuncMap.put("stay", xaddMap.get("rewardFuncStay"));		
	}
}
