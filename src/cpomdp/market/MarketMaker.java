/**
 * 
 */
package cpomdp.market;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import cpomdp.market.utils.VIHelper;
import cpomdp.market.utils.XADDHelper;
import cpomdp.market.utils.XADDWrapper;

//import org.yaml.snakeyaml.Yaml;

import xadd.XADD;

/**
 * @author 	Shamin Kinathil
 * @since	2014-09-03 
 *
 */
public class MarketMaker {

    private static final String PACKAGE_DIR_NAME = "cpomdp.market";
    private static final String SOURCE_DIR_NAME = "trunk.src";
    private static final String DOMAIN_DIR_NAME = "domains";
    	
    private static String PACKAGE_DIR_PATH = "";
    private static String DOMAINS_DIR_PATH = "";

	public static String USAGE_STRING = "Usage: ";    
    
	/**
	 * 
	 * @return
	 */
    private static String ProjectRootDirectory() {

        if (MarketMaker.PACKAGE_DIR_PATH.isEmpty()) {
            MarketMaker.PACKAGE_DIR_PATH = (System.getProperty("user.dir")
                            + File.separator + SOURCE_DIR_NAME 
                            + File.separator + PACKAGE_DIR_NAME).replace(".", File.separator);
        }

        return MarketMaker.PACKAGE_DIR_PATH;
    }

    /**
     * 
     * @param domainName
     * @return
     */
    private static String DomainDirectory(String domainName) {

        if (MarketMaker.DOMAINS_DIR_PATH.isEmpty()) {
            MarketMaker.DOMAINS_DIR_PATH = (MarketMaker.ProjectRootDirectory()
                                    			+ File.separator 
                                    			+ MarketMaker.DOMAIN_DIR_NAME).replace(".", File.separator);
        }

        return MarketMaker.DOMAINS_DIR_PATH + File.separator + domainName;
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

		MarketMaker.SetupEnvironment();
		
		String domainDir = MarketMaker.DomainDirectory("discrete_action");
		
		/*
		 *	Instantiate the POMDP Market Maker 
		 */
		
		// Initialise a POMDP using the files from domainDirectory
		POMDP mmPOMDP = new POMDP(domainDir);
				
		/*
		 * Solve the mmPOMDP using the PBVI algorithm
		 */
		
		PBVIDiscreteAction.DOMAIN_DIR_PATH = domainDir;
		
		// Initialise a set of belief points
		String bpFile1 = domainDir + File.separator + "belief_point_1.xadd";
		String bpFile2 = domainDir + File.separator + "belief_point_2.xadd";
//		String bpFile3 = domainDir + File.separator + "belief_point_3.xadd"; 
//		String bpFile4 = domainDir + File.separator + "belief_point_4.xadd"; 
//		String bpFile5 = domainDir + File.separator + "belief_point_5.xadd"; 
		ArrayList<Integer> beliefPointSet = PBVIDiscreteAction.InitialiseBeliefPoints(bpFile1, null);
		beliefPointSet = PBVIDiscreteAction.InitialiseBeliefPoints(bpFile2, beliefPointSet);		
//		beliefPointSet = PBVIDiscreteAction.InitialiseBeliefPoints(bpFile3, beliefPointSet);
//		beliefPointSet = PBVIDiscreteAction.InitialiseBeliefPoints(bpFile4, beliefPointSet);
//		beliefPointSet = PBVIDiscreteAction.InitialiseBeliefPoints(bpFile5, beliefPointSet);
		
		// Solve the mmPOMDP using the PBVI algorithm	
		Integer numIterations = 3;
		Double discountFactor = 1.0;

		PBVIDiscreteAction.Run(mmPOMDP, beliefPointSet, numIterations, discountFactor);
	}
}
