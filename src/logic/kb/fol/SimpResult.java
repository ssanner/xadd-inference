//////////////////////////////////////////////////////////////////////
//
// First-Order Logic Package
//
// Class:  SimpResult - Return value for redundancy removal method
// Author: Scott Sanner (ssanner@cs.toronto.edu)
// Date:   7/25/03
//
//////////////////////////////////////////////////////////////////////

package logic.kb.fol;

import java.io.*;
import java.util.*;

public class SimpResult {

    /* Internal constants */
    public final static int UNKNOWN  = 0;
    public final static int INEQUIV  = 1;
    public final static int EQUIV    = 2;
    public final static int SUBSUMED = 3;
    public final static int SUBSUMES = 4;

    /* Internal vars */
    public int             _nState;        // Reason for return value
    public FOPC.Node       _result;        // The node to replace current node with
    public HashMap         _hmUnifyMap;    // Var -> Term replacement rules as a 
                                           // result of unification

    /* Constructor */
    public SimpResult(FOPC.Node node, int state, HashMap unify_map) {

	_result        = node;
	_nState        = state;
	_hmUnifyMap    = unify_map;
    }
}
