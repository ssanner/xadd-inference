/**
 * 
 */
package cpomdp.market;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * @author skinathil
 *
 */
public class Domain {

	public static final String DOMAINS_DIR_NAME = "domains";
	
	public static final String BELIEFS_DIR_NAME = "beliefs";
	public static final String OBSERVATIONS_DIR_NAME = "observations";
	public static final String REWARDS_DIR_NAME = "rewards";
	public static final String TRANSITIONS_DIR_NAME = "transitions";
	
	public static String PROJECT_ROOT_DIR_PATH = "";
	public static String DOMAINS_DIR_PATH = "";
	
	private String domainName = null;
    private String domainPath = null;
    private String beliefsPath = null;
    private String observationsPath = null;
    private String rewardsPath = null;
    private String transitionsPath = null;    

   /*
    * Constructors
    */
   
	/**
	 * 
	 * @param domainName
	 */
	public Domain(String domainName) {
		
		String domainDirPath = Domain.DomainDirectoryPath(domainName); 
		
		// Check that the domainDirPath exists
		
		File file = new File(domainDirPath);
		if (file.exists() && file.isDirectory() && file.canRead() && this.checkForRequiredFolders(domainDirPath)) {
			this.domainName = domainName;
			
			this.domainPath = domainDirPath;			
			this.beliefsPath = domainDirPath + BELIEFS_DIR_NAME;
			this.observationsPath = domainDirPath + OBSERVATIONS_DIR_NAME;
			this.rewardsPath = domainDirPath + REWARDS_DIR_NAME;
			this.transitionsPath = domainDirPath + TRANSITIONS_DIR_NAME;
		}
	}
	
    /*
     * Methods
     */
		
    /**
     * 
     * @param rootDirPath
     * @return
     */
    public static String ProjectRootDirectory(String rootDirPath) {
    	
    	if (Domain.PROJECT_ROOT_DIR_PATH.isEmpty() && rootDirPath != null) {
        	rootDirPath = rootDirPath.trim();
        	
        	if(!rootDirPath.endsWith(File.separator)) {
        		rootDirPath = rootDirPath + File.separator;
        	}
        	
        	Domain.PROJECT_ROOT_DIR_PATH = rootDirPath;
    	}
    	
    	return Domain.PROJECT_ROOT_DIR_PATH;
    }
    
    /**
     * 
     * @return
     */
    public static String DomainsDirectory() {
    	
    	if(Domain.DOMAINS_DIR_PATH.isEmpty()) {
    		Domain.DOMAINS_DIR_PATH = Domain.ProjectRootDirectory(null) + 
					Domain.DOMAINS_DIR_NAME + File.separator;
    	}
    	
    	return Domain.DOMAINS_DIR_PATH; 	
    }
    
    /**
    *
    * @param domainName
    * @return
    */
   private static String DomainDirectoryPath(String domainName) {
       return Domain.DomainsDirectory() + domainName + File.separator;
   }    
    
   /*
    * Getters and Setters
    */	
	
	/**
	 * @return the domainName
	 */
	public String getDomainName() {
		return this.domainName;
	}

	/**
	 * @return the domainPath
	 */
	public String getDomainPath() {
		return domainPath;
	}   

	/**
	 * @return the beliefsPath
	 */
	public String getBeliefsPath() {
		return beliefsPath;
	}

	/**
	 * @return the observationsPath
	 */
	public String getObservationsPath() {
		return observationsPath;
	}

	/**
	 * @return the rewardsPath
	 */
	public String getRewardsPath() {
		return rewardsPath;
	}

	/**
	 * @return the transitionsPath
	 */
	public String getTransitionsPath() {
		return transitionsPath;
	}
		
	
	/**
	 * 
	 * @param domainDir
	 * @return
	 */
	private Boolean checkForRequiredFolders(String domainDir) {
		
		Boolean foldersExist = true;
		
		ArrayList<String> requiredDirectories = new ArrayList<String>(); 
		
		requiredDirectories.add(BELIEFS_DIR_NAME);
		requiredDirectories.add(OBSERVATIONS_DIR_NAME);
		requiredDirectories.add(REWARDS_DIR_NAME);
		requiredDirectories.add(TRANSITIONS_DIR_NAME);
		
		for(String dir : requiredDirectories) {
			
			Path dirPath = Paths.get(domainDir + dir);
			
			if(!Files.isDirectory(dirPath, LinkOption.NOFOLLOW_LINKS) || !Files.isReadable(dirPath)) {
				System.err.println("Unable to locate " + dir + " under " + domainDir);
				foldersExist = false;
			}
		}
		
		return foldersExist;
	}
	
}
