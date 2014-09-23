/**
 * 
 */
package cpomdp.market;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import xadd.ExprLib.ArithExpr;
import xadd.ExprLib.VarExpr;
import xadd.ExprLib.DoubleExpr;
import cpomdp.market.utils.XADDHelper;

/**
 * @author skinathil
 *
 */
public class POMDP {

	/**
	 * 
	 * @param domainDir
	 * @return
	 */
	private static HashMap<String, Integer> InitialiseFromDomainFiles(String domainDir) {
		
		System.out.println("In POMDP.InitialiseFromDomainFiles(): " + domainDir);
		
		HashMap<String, Integer> xaddMap = new HashMap<String, Integer>();
		
		// Check that the domainDir exists
		File f = new File(domainDir);
		if (f.exists() && f.isDirectory()) {
//			int buyVarID = XADDHelper.NewBoolDec("b");
//			xaddMap.put("buy", buyVarID);
//			
//			int sellVarID = XADDHelper.NewBoolDec("s");
//			xaddMap.put("sell", sellVarID);
//			
//			int holdVarID = XADDHelper.NewBoolDec("h");
//			xaddMap.put("hold", holdVarID);

			int valueTransitionID = XADDHelper.BuildXADD(domainDir + File.separator + "transition_value.xadd");
			xaddMap.put("valueTransition", valueTransitionID);
//			XADDHelper.PlotXADD(valueTransitionID, "Transition Value");
			
			int inventoryTransitionID = XADDHelper.BuildXADD(domainDir + File.separator + "transition_inventory.xadd");
			xaddMap.put("inventoryTransition", inventoryTransitionID);
//			XADDHelper.PlotXADD(inventoryTransitionID, "Transition Inventory");
			
			int buyObservationID = XADDHelper.BuildXADD(domainDir + File.separator + "observation_buy.xadd");
			xaddMap.put("buyObservation", buyObservationID);
//			XADDHelper.PlotXADD(buyObservationID, "Observation Buy");
			
			int sellObservationID = XADDHelper.BuildXADD(domainDir + File.separator + "observation_sell.xadd");
			xaddMap.put("sellObservation", sellObservationID);
//			XADDHelper.PlotXADD(sellObservationID, "Observation Sell");
			
			int holdObservationID = XADDHelper.BuildXADD(domainDir + File.separator + "observation_hold.xadd");
			xaddMap.put("holdObservation", holdObservationID);
//			XADDHelper.PlotXADD(holdObservationID, "Observation Hold");
			
			int rewardID = XADDHelper.BuildXADD(domainDir + File.separator + "reward_func.xadd");
			xaddMap.put("rewardFunc", rewardID);
//			XADDHelper.PlotXADD(rewardID, "Reward");		
			
			// Iterate through all of the XADDs and apply the substitution the
			// actionValue
			// HACK!
			HashMap<String, ArithExpr> actionValue = new HashMap<String, ArithExpr>();
			actionValue.put("n", new DoubleExpr(0.001));			
			for(Entry<String, Integer> e : xaddMap.entrySet()) {
				Integer xaddID = XADDHelper.substitute(e.getValue(), actionValue);
				xaddMap.put(e.getKey(), xaddID);
			}
		} 
		else {
			System.out.println("Unable to locate domainDir: " + domainDir);
		}
	
		return xaddMap;
	}
	
	public HashSet<String> stateSet;
	public HashSet<String> observationSet;
	public HashMap<String, ArithExpr> hmPrimeSubs;
	
	public Integer rewardFunc;
	
	public HashMap<String, HashMap<String, ArithExpr>> actionMap;
	public HashMap<String, Integer> observationMap;
	public HashMap<String, Integer> transitionFuncMap;
	public HashMap<String, Integer> observationFuncMap;
	
	/**
	 * 
	 * @param domainDir
	 */
	public POMDP(String domainDir) {
		
		// Load the domain files from the provided domainDir
		HashMap<String, Integer> xaddMap = POMDP.InitialiseFromDomainFiles(domainDir);
		
		stateSet = new HashSet<String>();
		observationSet = new HashSet<String>();
		hmPrimeSubs = new HashMap<String, ArithExpr>();
		actionMap = new HashMap<String, HashMap<String, ArithExpr>>();
		observationMap = new HashMap<String, Integer>();
		transitionFuncMap = new HashMap<String, Integer>();
		observationFuncMap = new HashMap<String, Integer>();
		
		/*
		 * States
		 */
		stateSet.add("v"); // value
		stateSet.add("i"); // inventory
		
        hmPrimeSubs.put("v", new VarExpr("v'"));
        hmPrimeSubs.put("i", new VarExpr("i'"));

        /*
         *  Actions
         */
        HashMap<String, ArithExpr> a1 = new HashMap<String, ArithExpr>();
        HashMap<String, ArithExpr> a2 = new HashMap<String, ArithExpr>();
        HashMap<String, ArithExpr> a3 = new HashMap<String, ArithExpr>();
        HashMap<String, ArithExpr> a4 = new HashMap<String, ArithExpr>();
   
        a1.put("bp", new DoubleExpr(10.0));
        a1.put("ap", new DoubleExpr(15.0));

        a2.put("bp", new DoubleExpr(20.0));
        a2.put("ap", new DoubleExpr(25.0));
        
        a3.put("bp", new DoubleExpr(30.0));
        a3.put("ap", new DoubleExpr(35.0));
                
        a4.put("bp", new DoubleExpr(40.0));
        a4.put("ap", new DoubleExpr(45.0));
        
		actionMap.put("a1", a1);
		actionMap.put("a2", a2);
		actionMap.put("a3", a3);
		actionMap.put("a4", a4);		
		
		/*
		 * Observations
		 */
		observationSet.add("buy");
		observationSet.add("sell");
		observationSet.add("hold");
//		observationMap.put("buy", xaddMap.get("buy"));
//		observationMap.put("sell", xaddMap.get("sell"));
//		observationMap.put("hold", xaddMap.get("hold"));
		
		/*
		 * Transition Function
		 */
		transitionFuncMap.put("v", xaddMap.get("valueTransition"));
		transitionFuncMap.put("i", xaddMap.get("inventoryTransition"));
		
		/*
		 * Observation Function
		 */
		observationFuncMap.put("buy", xaddMap.get("buyObservation"));
		observationFuncMap.put("sell", xaddMap.get("sellObservation"));
		observationFuncMap.put("hold", xaddMap.get("holdObservation"));
		
		/*
		 * Reward Function
		 */
		rewardFunc = xaddMap.get("rewardFunc");
	}
}
