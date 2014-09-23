/**
 * 
 */
package cpomdp.market.utils;

import xadd.XADD;

/**
 * XADD Wrapper class. Implemented as a Singleton to ensure that only one XADD
 * "context" is used.
 * 
 * @author	Shamin Kinathil 
 * @since	2014-09-03	
 */
public class XADDWrapper {
	
	private static class XADDHolder {
		private static final XADD Instance = new XADD();
	}
	
	public static XADD getInstance() {
		return XADDHolder.Instance;
	}
	
    private XADDWrapper() {    	
    }
}
