/**
 * 
 */
package cpomdp.market;

import java.io.File;
import java.util.HashMap;

import xadd.ExprLib.ArithExpr;
import cpomdp.market.utils.XADDHelper;
import cpomdp.market.utils.XADDWrapper;

/**
 * Abstract class for POMDPs implemented using XADDs
 * 
 * @author skinathil
 *
 */
public abstract class XADDPOMDP {

	/**
	 * 
	 */
	public XADDPOMDP() {

	}

	/**
	 *	Build and XADD from files located in a folder
	 * 
	 * @param xaddDirPath
	 * @param xaddMap
	 * @return
	 */
	public static HashMap<String, Integer> InitialiseFromDomainFiles(String xaddDirPath, 
			HashMap<String, Integer> xaddMap) {

		File xaddDir = new File(xaddDirPath);
		String fileName = null;

		// Iterate through all of the files in the xaddDir
		File[] listOfFiles = xaddDir.listFiles();
		for (File file : listOfFiles) {

			fileName = file.getName();

			if (file.isFile() && file.canRead()) {

				int xaddID = XADDHelper.BuildXADD(file.getPath());
				xaddMap.put(fileName.substring(0, fileName.lastIndexOf('.')), xaddID);   
			}
		}

		return xaddMap;
	}

	/**
	 * 
	 * @param xaddDirPath
	 * @param xaddMap
	 * @param subsMap
	 * @return
	 */
	public static HashMap<String, Integer> InitialiseFromDomainFiles(String xaddDirPath, 
			HashMap<String, Integer> xaddMap, HashMap<String, ArithExpr> subsMap) {

		File xaddDir = new File(xaddDirPath);
		String fileName = null;

		// Iterate through all of the files in the xaddDir
		File[] listOfFiles = xaddDir.listFiles();
		for (File file : listOfFiles) {

			fileName = file.getName();

			if (file.isFile() && file.canRead()) {

				int xaddID = XADDHelper.BuildXADD(file.getPath());
				
				xaddID = XADDWrapper.substitute(xaddID, subsMap);
				
				xaddMap.put(fileName.substring(0, fileName.lastIndexOf('.')), xaddID);   
			}
		}

		return xaddMap;
	}
}
