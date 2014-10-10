/**
 * 
 */
package cpomdp.market;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import xadd.ExprLib.DoubleExpr;
import xadd.XADD.XADDTNode;
import cpomdp.market.utils.XADDHelper;
import cpomdp.market.utils.XADDOperator;
import cpomdp.market.utils.XADDWrapper;

/**
 * @author skinathil
 *
 */
public abstract class PBVI {

	private final static Double EPSILON =  0.000001;
	
	/*
	 * Constructors
	 */
	
	/**
	 * 
	 */
	public PBVI() {
		// TODO Auto-generated constructor stub
	}

	/*
	 * Methods
	 */
	
	/**
	 * 
	 * @param beliefDirPath
	 * @return
	 */
	public static ArrayList<Integer> InitialiseBeliefPointsFromFolder(String beliefDirPath) {
	
		ArrayList<Integer> beliefPoints = new ArrayList<Integer>();
		
    	File bpDir = new File(beliefDirPath);
    	
    	// Iterate through all of the files in the bpDir
    	File[] listOfFiles = bpDir.listFiles();
    	for (File file : listOfFiles) {
    		
    		Integer xaddID = PBVI.InitialiseBeliefPoint(file.getPath());
    		
    		if(xaddID != Integer.MIN_VALUE) {
    			beliefPoints.add(xaddID);
    		}
    	}
    	
    	return beliefPoints;
	}

	/**
	 * 
	 * @param beliefPointFile
	 * @return
	 */
	public static Integer InitialiseBeliefPoint(String beliefPointFilePath) {

		Integer bpXADD = XADDHelper.BuildXADD(beliefPointFilePath);
		XADDHelper.PlotXADD(bpXADD, "BP: " + bpXADD.toString() + " " + beliefPointFilePath);

		// Check that the bpXADD is valid i.e. it integrates to 1
		if(!PBVI.IsValidBeliefPoint(bpXADD)) {
			System.out.println("PBVI.InitialiseBeliefPoint: Invalid belief point contained in " + beliefPointFilePath);
			return Integer.MIN_VALUE;
		}

		return bpXADD;
	}
	
	/**
	 * Determines whether a belief point is valid. i.e. if it integrates to 1.0
	 * 
	 * @param 	bpID 			(Integer)
	 * @return	validBelief?	(Boolean)
	 */
	public static boolean IsValidBeliefPoint(Integer bpID) {

		boolean validBP = true;
		
		// Integrate out all variables in the bpID XADD
		bpID = PBVI.Integrate(bpID);
		
		XADDTNode terminalNode = XADDWrapper.getNode(bpID);
		Double val = ((DoubleExpr) terminalNode._expr)._dConstVal;	
		
		assert(Math.abs(val - 1.0) < PBVI.EPSILON) : 
			"Belief point does not integrate out to 1.0";

		return validBP;
	}
	
	public static boolean IsValidBeliefPoint(Integer bpID, Boolean assertValidity) {
		
		boolean validBP = true;
		
		// Integrate out all variables in the bpID XADD
		bpID = PBVI.Integrate(bpID);
		
		XADDTNode terminalNode = XADDWrapper.getNode(bpID);
		Double val = ((DoubleExpr) terminalNode._expr)._dConstVal;	

		validBP = (Math.abs(val - 1.0) < PBVI.EPSILON);
		
		if(assertValidity) {
			assert(validBP) : "Belief point does not integrate out to 1.0";
		}
		
		return validBP;
	}	
	
	/**
	 * 
	 * @param xaddID
	 * @return
	 */
	public static Integer Integrate(Integer xaddID) {
		
		// Get the variables in the bpID XADD
		HashSet<String> varsToRegress = XADDWrapper.collectVars(xaddID);
		
		return PBVI.Integrate(xaddID, varsToRegress);
	}

	/**
	 * 
	 * @param xaddID
	 * @param vars
	 * @return
	 */
	public static Integer Integrate(Integer xaddID, Set<String> vars) {
	
		for (String var : vars) {
			xaddID = XADDWrapper.computeDefiniteIntegral(xaddID, var);
		}
		
		return xaddID;
	}	
	
	/**
	 * Calculate the ArgMax of a GammaSet (set of AlphaVectors) with respect to
	 * a given bpID
	 * 
	 * @param gammaSet
	 * @param bpID
	 * @return Maximal AlphaVector
	 */
	public static AlphaVector ArgMax(GammaSet gammaSet, Integer bpID) {

		AlphaVector maxAlphaVector = null;
		Integer innerProdXADD = null;		
		Double innerProdValue = null;
		Double maxInnerProdValue = Double.NEGATIVE_INFINITY;

		//		XADDHelper.PlotXADD(bpID, "BP: " + bpID);

		if(gammaSet.size() == 1) {
			return gammaSet.getVectors().get(0);
		}

		// Iterate through each AlphaVector in the gammaSet
		for (AlphaVector currAlphaV : gammaSet) {
//			XADDHelper.PlotXADD(currAlphaV.getXADD(), currAlphaV.toString());

			// The inner product of the currAlphaV and the  bpID
			innerProdXADD = XADDWrapper.Apply(currAlphaV.getXADD(), bpID, XADDOperator.PROD);
//			XADDHelper.PlotXADD(innerProdXADD, "Inner Product BP: " + bpID + " * " + currAlphaV.toString());

			// Compute the definite integral of the innerProdXADD over the continuous
			// variables
			
			// Integrate out all variables in the innerProdXADD
			innerProdXADD = PBVI.Integrate(innerProdXADD);
			
//			Integer int1 =  XADDWrapper.computeDefiniteIntegral(innerProdXADD, "i");
//			//			XADDHelper.PlotXADD(int1, "Integral 1 v");
//			Integer int2 =  XADDWrapper.computeDefiniteIntegral(int1, "v");

			// The result ...
			XADDTNode t = XADDWrapper.getNode(innerProdXADD);
			innerProdValue = ((DoubleExpr) t._expr)._dConstVal;
//			System.out.format("%s x %s = %f%n", bpID, currAlphaV, innerProdValue);

			if(innerProdValue > maxInnerProdValue) {
				maxAlphaVector = currAlphaV;
				maxInnerProdValue = innerProdValue; 
			}
		}		

		return maxAlphaVector;
	}	
}
