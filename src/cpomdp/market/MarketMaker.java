/**
 * 
 */
package cpomdp.market;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import xadd.XADD;
import cpomdp.market.utils.XADDWrapper;

/**
 * @author 	Shamin Kinathil
 * @since	2014-09-03 
 *
 */
public class MarketMaker {

    private static final String PACKAGE_DIR_NAME = "cpomdp.market";
    private static final String SOURCE_DIR_NAME = "trunk.src";
    private static String PACKAGE_DIR_PATH = "";
    
	/**
	 * @param args
	 */
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws FileNotFoundException {

		/*
		 *	Setup the environment 
		 */

//		MarketMaker.SetupEnvironment();
		
		Domain.ProjectRootDirectory(MarketMaker.ProjectRootDirectory());
//		PBVIDiscreteAction.ProjectRootDirectory(MarketMaker.ProjectRootDirectory());
		
		Domain daDomain = new Domain("discrete_action");

		// Initialise a POMDP using the daDomain
		MMPOMDP mmPOMDP = new MMPOMDP(daDomain);
		
		// Initialise a set of belief points
		ArrayList<Integer> beliefPointSet = PBVI.InitialiseBeliefPointsFromFolder(daDomain.getBeliefsPath());

		// Solve the mmPOMDP using the PBVI algorithm	
		Integer numIterations = 5;
		Double discountFactor = 1.0;
		
		DateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");
		Date dateobj = new Date();
		String outputDirPath = MarketMaker.ProjectRootDirectory() + "output_" + df.format(dateobj);

		PBVIDiscreteAction.Run(mmPOMDP, beliefPointSet, numIterations, discountFactor, outputDirPath, false);
	}

    /**
     * Set up environment for execution
     */
    private static void SetupEnvironment() {

    	// Initialise an XADD instance (Singleton)
        XADD xaddInstance = XADDWrapper.getInstance();
    }	
	
    /**
     *
     * @return
     */
    private static String ProjectRootDirectory() {

        if (MarketMaker.PACKAGE_DIR_PATH.isEmpty()) {
            MarketMaker.PACKAGE_DIR_PATH = (System.getProperty("user.dir")
                    + File.separator + SOURCE_DIR_NAME
                    + File.separator + PACKAGE_DIR_NAME
                    + File.separator).replace(".", File.separator);
        }

        return MarketMaker.PACKAGE_DIR_PATH;
    }

}
