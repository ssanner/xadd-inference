//////////////////////////////////////////////////////////////////////
//
// File:     InputStreamLoader.java
// Author:   Scott Sanner, University of Toronto (ssanner@cs.toronto.edu)
// Date:     9/1/2003
// Requires: comshell package
//
// Description:
//
//   Use to load an input stream from a file (or URL if not file).
//
//////////////////////////////////////////////////////////////////////

// Package definition
package util;

// Packages to import
import java.io.*;
import java.net.*;

/**
 * @version   1.0
 * @author    Scott Sanner
 * @language  Java (JDK 1.3)
 **/
public class InputStreamLoader {

    /* Returns null of OpenStream failed */
    public static InputStream OpenStream(String surl) {

        InputStream in;
	
        try {
            File ff = new File(surl);
            in = new FileInputStream(ff);
        }
        catch (Exception ignore) {
            try {
                URL url = new URL(surl);
                in = url.openStream();
            }
            catch (Exception e) {
                System.err.println("Failed to open: '" + surl + "'");
                return null;
            }
        }

	return in;
    }	
}
