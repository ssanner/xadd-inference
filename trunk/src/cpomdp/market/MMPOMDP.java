/**
 * 
 */
package cpomdp.market;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import xadd.ExprLib.ArithExpr;
import xadd.ExprLib.VarExpr;
import xadd.ExprLib.DoubleExpr;
import cpomdp.market.utils.XADDHelper;
import cpomdp.market.utils.XADDWrapper;

/**
 * @author skinathil
 *
 */
@SuppressWarnings("unused")
public class MMPOMDP extends XADDPOMDP {
	
	public Domain domain = null;
	public Integer rewardFunc  = null;
	public Set<String> observationSet = null;	
	public HashMap<String, ArithExpr> stateMap  = null;		
	public HashMap<String, Integer> rewardFuncMap = null;
	public HashMap<String, Integer> transitionFuncMap = null;
	public HashMap<String, Integer> observationFuncMap = null;
	public HashMap<String, HashMap<String, ArithExpr>> actionMap =  null;
	
	/*
	 * Constructors
	 */
	
	/**
	 * 
	 */
	public MMPOMDP() {
		
	}
	
	/**
	 * 
	 * @param domain
	 */
	public MMPOMDP(Domain domain) {
		this.domain = domain;
		
		this.observationFuncMap = new HashMap<String, Integer>();
		this.transitionFuncMap = new HashMap<String, Integer>();
		this.rewardFuncMap = new HashMap<String, Integer>();		
		this.stateMap = new HashMap<String, ArithExpr>();
		this.actionMap = new HashMap<String, HashMap<String, ArithExpr>>();
		
		// TODO: Remove this HACK!
		HashMap<String, ArithExpr> noiseSubs = new HashMap<String, ArithExpr>();
		noiseSubs.put("n", new DoubleExpr(0.001));
		
		MMPOMDP.InitialiseFromDomainFiles(this.domain.getObservationsPath(), observationFuncMap, noiseSubs);
		this.observationSet = this.observationFuncMap.keySet();		
		
		HashMap<String, Integer> tempMap = new HashMap<String, Integer>();
		MMPOMDP.InitialiseFromDomainFiles(this.domain.getTransitionsPath(), tempMap, noiseSubs);		
		
		String newState = null;
		
		// Initialise the stateMap
		for(String state : tempMap.keySet()) {
			
			// Truncate the state to be just the first character
			newState = state.substring(0, 1);
			
			this.stateMap.put(newState, new VarExpr(newState + "'"));
			
			this.transitionFuncMap.put(newState, tempMap.get(state));
		}
		
		// TODO: Remove this HACK!
		this.actionMap = MMPOMDP.AddManualActionMap(actionMap);
		
		MMPOMDP.InitialiseFromDomainFiles(this.domain.getRewardsPath(), rewardFuncMap, noiseSubs);
		this.rewardFunc = this.rewardFuncMap.get("reward");		
	}
	
	/**
	 * 
	 * @param numActions
	 * @return
	 */
	public static HashMap<String, HashMap<String, ArithExpr>> GenerateBisAskPrices(Integer numActions) {
		
		HashMap<String, HashMap<String, ArithExpr>> actionMap = new HashMap<String, HashMap<String, ArithExpr>>();
		
		for(int actionNum = 1; actionNum < (numActions + 1);  actionNum++) {
			
			HashMap<String, ArithExpr> bidAskMap = new HashMap<String, ArithExpr>();
			
			// Set the bid and ask price
			bidAskMap.put("bid", new DoubleExpr(0.0));
			bidAskMap.put("ask", new DoubleExpr(0.0));
			
			actionMap.put("a" + actionNum, bidAskMap);
		}
		
		return actionMap;
	}
	
	/**
	 * 
	 * @param actionMap
	 * @return
	 */
	public static HashMap<String, HashMap<String, ArithExpr>> AddManualActionMap(HashMap<String, HashMap<String, ArithExpr>> actionMap) {
		
        HashMap<String, ArithExpr> a1 = new HashMap<String, ArithExpr>();
        HashMap<String, ArithExpr> a2 = new HashMap<String, ArithExpr>();
        HashMap<String, ArithExpr> a3 = new HashMap<String, ArithExpr>();
        HashMap<String, ArithExpr> a4 = new HashMap<String, ArithExpr>();
        HashMap<String, ArithExpr> a5 = new HashMap<String, ArithExpr>();
        HashMap<String, ArithExpr> a6 = new HashMap<String, ArithExpr>();
        HashMap<String, ArithExpr> a7 = new HashMap<String, ArithExpr>();
        HashMap<String, ArithExpr> a8 = new HashMap<String, ArithExpr>();
        HashMap<String, ArithExpr> a9 = new HashMap<String, ArithExpr>();
        HashMap<String, ArithExpr> a10 = new HashMap<String, ArithExpr>();
        HashMap<String, ArithExpr> a11 = new HashMap<String, ArithExpr>();
        HashMap<String, ArithExpr> a12 = new HashMap<String, ArithExpr>();
        HashMap<String, ArithExpr> a13 = new HashMap<String, ArithExpr>();
   
        a1.put("ap", new DoubleExpr(40.0));
        a1.put("bp", new DoubleExpr(20.0));
        
        a2.put("ap", new DoubleExpr(60.0));
        a2.put("bp", new DoubleExpr(40.0));
        
        a3.put("ap", new DoubleExpr(80.0));
        a3.put("bp", new DoubleExpr(60.0));
        
        a4.put("ap", new DoubleExpr(100.0));        
        a4.put("bp", new DoubleExpr(80.0));        
        
        a5.put("ap", new DoubleExpr(100.0));        
        a5.put("bp", new DoubleExpr(1.0));        
        
//        a7.put("ap", new DoubleExpr(90.0));        
//        a7.put("bp", new DoubleExpr(10.0));        
//
//        a8.put("ap", new DoubleExpr(80.0));        
//        a8.put("bp", new DoubleExpr(20.0));        
//
//        a9.put("ap", new DoubleExpr(70.0));        
//        a9.put("bp", new DoubleExpr(30.0));        
//        
//        a10.put("ap", new DoubleExpr(60.0));        
//        a10.put("bp", new DoubleExpr(40.0));        
//        
//        a11.put("ap", new DoubleExpr(20.0));
//        a11.put("bp", new DoubleExpr(17.0));
//
//        a12.put("ap", new DoubleExpr(20.0));
//        a12.put("bp", new DoubleExpr(19.0));        
        
		actionMap.put("a1", a1);
		actionMap.put("a2", a2);
		actionMap.put("a3", a3);
		actionMap.put("a4", a4);
		actionMap.put("a5", a5);	
//		actionMap.put("a6", a6);
//		actionMap.put("a7", a7);
//		actionMap.put("a8", a8);
//		actionMap.put("a9", a9);
//		actionMap.put("a10", a10);
//		actionMap.put("a11", a11);
//		actionMap.put("a12", a12);
//		actionMap.put("a13", a13);		
		
		return actionMap;
	}
	
	/**
	 * 
	 * @param actionName
	 * @param bidAskMap
	 */
	public void addBidAskPrices(String actionName, HashMap<String, ArithExpr> bidAskMap) {		
		this.actionMap.put(actionName, bidAskMap);
	}
	
//	/**
//	 * 
//	 * @param domainDir
//	 */
//	public POMDP(String domainDir) {
//		
//		// Load the domain files from the provided domainDir
//		HashMap<String, Integer> xaddMap = POMDP.InitialiseFromDomainFiles(domainDir);
//		
//		observationSet = new HashSet<String>();
//		stateMap = new HashMap<String, ArithExpr>();
//		actionMap = new HashMap<String, HashMap<String, ArithExpr>>();
//		transitionFuncMap = new HashMap<String, Integer>();
//		observationFuncMap = new HashMap<String, Integer>();
//		
//		/*
//		 * States
//		 */
//
//        stateMap.put("v", new VarExpr("v'")); // value
//        stateMap.put("i", new VarExpr("i'")); // inventory        
//        
//        /*
//         *  Actions
//         */
//        HashMap<String, ArithExpr> a1 = new HashMap<String, ArithExpr>();
//        HashMap<String, ArithExpr> a2 = new HashMap<String, ArithExpr>();
//        HashMap<String, ArithExpr> a3 = new HashMap<String, ArithExpr>();
//        HashMap<String, ArithExpr> a4 = new HashMap<String, ArithExpr>();
//        HashMap<String, ArithExpr> a5 = new HashMap<String, ArithExpr>();
//        HashMap<String, ArithExpr> a6 = new HashMap<String, ArithExpr>();
//        HashMap<String, ArithExpr> a7 = new HashMap<String, ArithExpr>();
//        HashMap<String, ArithExpr> a8 = new HashMap<String, ArithExpr>();
//        HashMap<String, ArithExpr> a9 = new HashMap<String, ArithExpr>();
//        HashMap<String, ArithExpr> a10 = new HashMap<String, ArithExpr>();
//        HashMap<String, ArithExpr> a11 = new HashMap<String, ArithExpr>();
//        HashMap<String, ArithExpr> a12 = new HashMap<String, ArithExpr>();
//        HashMap<String, ArithExpr> a13 = new HashMap<String, ArithExpr>();
//   
//        a1.put("ap", new DoubleExpr(40.0));
//        a1.put("bp", new DoubleExpr(20.0));
//        
//        a2.put("ap", new DoubleExpr(60.0));
//        a2.put("bp", new DoubleExpr(40.0));
//        
//        a3.put("ap", new DoubleExpr(80.0));
//        a3.put("bp", new DoubleExpr(60.0));
//        
//        a4.put("ap", new DoubleExpr(100.0));        
//        a4.put("bp", new DoubleExpr(80.0));        
//        
//        a5.put("ap", new DoubleExpr(100.0));        
//        a5.put("bp", new DoubleExpr(1.0));        
//        
////        a7.put("ap", new DoubleExpr(90.0));        
////        a7.put("bp", new DoubleExpr(10.0));        
////
////        a8.put("ap", new DoubleExpr(80.0));        
////        a8.put("bp", new DoubleExpr(20.0));        
////
////        a9.put("ap", new DoubleExpr(70.0));        
////        a9.put("bp", new DoubleExpr(30.0));        
////        
////        a10.put("ap", new DoubleExpr(60.0));        
////        a10.put("bp", new DoubleExpr(40.0));        
////        
////        a11.put("ap", new DoubleExpr(20.0));
////        a11.put("bp", new DoubleExpr(17.0));
////
////        a12.put("ap", new DoubleExpr(20.0));
////        a12.put("bp", new DoubleExpr(19.0));        
//        
//		actionMap.put("a1", a1);
//		actionMap.put("a2", a2);
//		actionMap.put("a3", a3);
//		actionMap.put("a4", a4);
//		actionMap.put("a5", a5);	
////		actionMap.put("a6", a6);
////		actionMap.put("a7", a7);
////		actionMap.put("a8", a8);
////		actionMap.put("a9", a9);
////		actionMap.put("a10", a10);
////		actionMap.put("a11", a11);
////		actionMap.put("a12", a12);
////		actionMap.put("a13", a13);
//		
//		/*
//		 * Observations
//		 */
//		observationSet.add("buy");
//		observationSet.add("sell");
//		observationSet.add("hold");
//		
//		/*
//		 * Transition Function
//		 */
//		transitionFuncMap.put("v", xaddMap.get("valueTransition"));
//		transitionFuncMap.put("i", xaddMap.get("inventoryTransition"));
//		
//		/*
//		 * Observation Function
//		 */
//		observationFuncMap.put("buy", xaddMap.get("buyObservation"));
//		observationFuncMap.put("sell", xaddMap.get("sellObservation"));
//		observationFuncMap.put("hold", xaddMap.get("holdObservation"));
//		
//		/*
//		 * Reward Function
//		 */
//		rewardFunc = xaddMap.get("rewardFunc");
//	}	
//	
//	/*
//	 * Methods
//	 */
//		
//    /**
//     * 
//     * @param rootDirPath
//     * @return
//     */
//    public static String ProjectRootDirectory(String rootDirPath) {
//    	
//    	if (POMDP.PROJECT_ROOT_DIR_PATH.isEmpty() && rootDirPath != null) {
//        	rootDirPath = rootDirPath.trim();
//        	
//        	if(!rootDirPath.endsWith(File.separator)) {
//        		rootDirPath = rootDirPath + File.separator;
//        	}
//        	
//        	POMDP.PROJECT_ROOT_DIR_PATH = rootDirPath;
//    	}
//    	
//    	return POMDP.PROJECT_ROOT_DIR_PATH;
//    }	
//    
//	/**
//	 * 
//	 * @param domainDir
//	 * @return
//	 */
//	private static HashMap<String, Integer> InitialiseFromDomainFiles(String domainDir) {
//		
//		System.out.println("In POMDP.InitialiseFromDomainFiles(): " + domainDir);
//		
//		HashMap<String, Integer> xaddMap = new HashMap<String, Integer>();
//		
//		// Check that the domainDir exists
//		File f = new File(domainDir);
//		if (f.exists() && f.isDirectory()) {
//
//			int valueTransitionID = XADDHelper.BuildXADD(domainDir + File.separator + "transition_value.xadd");
//			xaddMap.put("valueTransition", valueTransitionID);
////			XADDHelper.PlotXADD(valueTransitionID, "Transition Value");
//			
//			int inventoryTransitionID = XADDHelper.BuildXADD(domainDir + File.separator + "transition_inventory.xadd");
//			xaddMap.put("inventoryTransition", inventoryTransitionID);
////			XADDHelper.PlotXADD(inventoryTransitionID, "Transition Inventory");
//			
//			int buyObservationID = XADDHelper.BuildXADD(domainDir + File.separator + "observation_buy.xadd");
//			xaddMap.put("buyObservation", buyObservationID);
////			XADDHelper.PlotXADD(buyObservationID, "Observation Buy");
//			
//			int sellObservationID = XADDHelper.BuildXADD(domainDir + File.separator + "observation_sell.xadd");
//			xaddMap.put("sellObservation", sellObservationID);
////			XADDHelper.PlotXADD(sellObservationID, "Observation Sell");
//			
//			int holdObservationID = XADDHelper.BuildXADD(domainDir + File.separator + "observation_hold.xadd");
//			xaddMap.put("holdObservation", holdObservationID);
////			XADDHelper.PlotXADD(holdObservationID, "Observation Hold");
//			
//			int rewardID = XADDHelper.BuildXADD(domainDir + File.separator + "reward_func.xadd");
//			xaddMap.put("rewardFunc", rewardID);
////			XADDHelper.PlotXADD(rewardID, "Reward");		
//			
//			// Iterate through all of the XADDs and apply the substitution the
//			// actionValue
//			// HACK!
//			HashMap<String, ArithExpr> actionValue = new HashMap<String, ArithExpr>();
//			actionValue.put("n", new DoubleExpr(0.001));			
//			for(Entry<String, Integer> e : xaddMap.entrySet()) {
//				Integer xaddID = XADDWrapper.substitute(e.getValue(), actionValue);
//				xaddMap.put(e.getKey(), xaddID);
//			}
//		} 
//		else {
//			System.out.println("Unable to locate domainDir: " + domainDir);
//		}
//	
//		return xaddMap;
//	}
	
}
