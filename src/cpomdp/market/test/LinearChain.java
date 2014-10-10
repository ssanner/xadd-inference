package cpomdp.market.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import xadd.XADD;
import cpomdp.market.PBVIDiscreteAction;
import cpomdp.market.utils.VIHelper;
import cpomdp.market.utils.XADDHelper;
import cpomdp.market.utils.XADDWrapper;

public class LinearChain {

	
    private static final String PACKAGE_DIR_NAME = "cpomdp.market";
    private static final String SOURCE_DIR_NAME = "trunk.src";
    private static final String DOMAIN_DIR_NAME = "domains";
    private static final String BELIEF_POINT_DIR_NAME = "beliefs";
    	
    private static String PACKAGE_DIR_PATH = "";
    private static String DOMAINS_DIR_PATH = "";

	public static String USAGE_STRING = "Usage: ";    
    
	/**
	 * 
	 * @return
	 */
    private static String ProjectRootDirectory() {

        if (LinearChain.PACKAGE_DIR_PATH.isEmpty()) {
            LinearChain.PACKAGE_DIR_PATH = (System.getProperty("user.dir")
                            + File.separator + SOURCE_DIR_NAME 
                            + File.separator + PACKAGE_DIR_NAME).replace(".", File.separator);
        }

        return LinearChain.PACKAGE_DIR_PATH;
    }

    /**
     * 
     * @param domainName
     * @return
     */
    private static String DomainDirectory(String domainName) {

        if (LinearChain.DOMAINS_DIR_PATH.isEmpty()) {
            LinearChain.DOMAINS_DIR_PATH = (LinearChain.ProjectRootDirectory()
                                    			+ File.separator 
                                    			+ LinearChain.DOMAIN_DIR_NAME).replace(".", File.separator);
        }

        return LinearChain.DOMAINS_DIR_PATH + File.separator + domainName;
    }    
	
	/**
	 * Set up environment for execution
	 */
	private static void SetupEnvironment() {

		XADD xaddInstance = XADDWrapper.getInstance();
		XADDHelper.setXADDInstance(xaddInstance);
		VIHelper.setXADDInstance(xaddInstance);
	}
	
	/**
	 * @param args
	 */
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws FileNotFoundException {

		/*
		 *	Setup the environment 
		 */

		LinearChain.SetupEnvironment();
		
		String domainDir = LinearChain.DomainDirectory("linear_chain");		
		String bpDir = domainDir + File.separator + BELIEF_POINT_DIR_NAME;
		
		/*
		 *	Instantiate the POMDP Market Maker 
		 */
		
		// Initialise a POMDP using the files from domainDirectory
		POMDPTest mmPOMDP = new POMDPTest(domainDir);

		/*
		 * Initialise the belief points
		 */
		
		// Initialise a set of belief points
		String bpFile1 = bpDir + File.separator + "belief_point_1.xadd";
		String bpFile2 = bpDir + File.separator + "belief_point_2.xadd";
		String bpFile3 = bpDir + File.separator + "belief_point_3.xadd"; 
		
		ArrayList<Integer> beliefPointSet = PBVIDiscreteAction.InitialiseBeliefPoints(bpFile1, null);
		beliefPointSet = PBVIDiscreteAction.InitialiseBeliefPoints(bpFile2, beliefPointSet);		
		beliefPointSet = PBVIDiscreteAction.InitialiseBeliefPoints(bpFile3, beliefPointSet);
		
		/*
		 * Solve the mmPOMDP using the PBVI algorithm
		 */
		
		PBVIDiscreteAction2.DOMAIN_DIR_PATH = domainDir;
		
		// Solve the mmPOMDP using the PBVI algorithm	
		Integer numIterations = 10;
		Double discountFactor = 1.0;

		PBVIDiscreteAction2.Run2(mmPOMDP, beliefPointSet, numIterations, discountFactor);
	}
}
