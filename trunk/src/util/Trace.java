////////////////////////////////////////////////////////
//
// Class:  Trace
// Author: Scott P. Sanner
//
// Description: This class serves as a trace utility.
//              SetTraceLevel(int) is used to set the
//              trace level and then the MACRO T is used
//              to print out messages which are below
//              the level threshold.
//
////////////////////////////////////////////////////////

// Include files for I/O and Backgammon classes
package util;

import java.io.*;
import java.util.*;

public class Trace {

    private PrintStream m_osOut;
    private int m_nTraceLevel;
    private boolean m_bDebug;
    
    public Trace() {
	m_bDebug = true;
	m_osOut = System.out;
	m_nTraceLevel = 0;
    }
    
    public Trace(String filename, int tl, boolean debug) {
	m_bDebug = debug;
	if (!debug) return;
	try {
	    m_osOut = new PrintStream(new FileOutputStream(filename)); 
	} catch (FileNotFoundException e) {
	    System.out.println(e);
	    System.exit(1);
	}
	m_nTraceLevel = tl;
    }
    
    public void M(int level, String msg) {
	if (m_bDebug && level <= m_nTraceLevel) 
	    m_osOut.println(msg);
    }

    public void M(int level, String msg, int i1, int i2) {
	if (m_bDebug && level <= m_nTraceLevel) 
	    m_osOut.println(msg + ":" + i1 + ":" + i2);
    }
}

