//////////////////////////////////////////////////////////////////////
//
// First-Order Logic Package
//
// Class:  Internal Query Binding Data Struct
// Author: Scott Sanner (ssanner@cs.toronto.edu)
// Date:   11/20/03
//
// TODO:
// -----
// - Handle FOPC.TVars and FOPC.Terms instead of Strings.
//
//////////////////////////////////////////////////////////////////////    

package logic.kb.fol;

import java.util.*;

// An internal structure for holding array bindings for queries with
// free variables
public class BindingSet {

    private ArrayList _vars;
    private ArrayList _types;
    private ArrayList _bindings; // An array of ArrayLists
    private ArrayList _proofs;
    private HashMap   _hmVarToIndex;

    public BindingSet() {
	_vars     = new ArrayList();
	_types    = new ArrayList();
	_bindings = new ArrayList();
	_proofs   = new ArrayList();
	_hmVarToIndex = new HashMap();
    }

    public void addVar(String v) {
	_hmVarToIndex.put(v, new Integer(_vars.size()));
	_vars.add(v);	    
	_types.add(null);
    }

    public void addVar(String v, String type) {
	_hmVarToIndex.put(v, new Integer(_vars.size()));
	_vars.add(v);	    
	_types.add(type);
    }

    public List getVars() {
	return _vars;
    }

    public int makeNewBindingEntry() {

	// Go through bindings from 1.. _bindings.size()-1 to see if match
	ArrayList a = null;
	if (endRedundant()) {
	    a = (ArrayList)_bindings.get(_bindings.size() - 1);
	    a.clear();
	} else {
	    a = new ArrayList();
	    _bindings.add(a);
	    _proofs.add(new ArrayList());
	}

	for (int i = 0; i < _vars.size(); i++) {
	    a.add(null);
	}

	return (_bindings.size() - 1);
    }

    public BindingSet seal() {
	if (endRedundant()) {
	    _proofs.remove(_bindings.size() - 1);
	    _bindings.remove(_bindings.size() - 1);
	}
	//if (_vars.size() == 0) {
	//    _bindings = new ArrayList();
	//    _proofs   = new ArrayList();
	//}
	return this;
    }

    public boolean endRedundant() {
	int i, b;
	boolean match = false;
	if (_bindings.size() > 1) {
	    ArrayList cur_b = (ArrayList)_bindings.get(_bindings.size() - 1);
	    for (b = 0; b < (_bindings.size() - 1); b++) {
		ArrayList comp_b = (ArrayList)_bindings.get(b);
		match = true;
		for (i = 0; match && i < _vars.size(); i++) {
		    match = cur_b.get(i).equals(comp_b.get(i));
		}
		if (match) {
		    return true;
		} 
	    }
	}
	return false;
    }

    public void addBinding(int index, String var, String binding) {
	int bi = ((Integer)_hmVarToIndex.get(var)).intValue();
	((ArrayList)_bindings.get(index)).set(bi, binding);
    }

    public void addProof(int index, ArrayList proof) {
	_proofs.set(index, proof);
    }

    public int getNumBindings() {
	return _bindings.size();
    }

    public String getBinding(int index, String var) {
	int bi = ((Integer)_hmVarToIndex.get(var)).intValue();
	return (String)((ArrayList)_bindings.get(index)).get(bi);
    }

    public String toString() {
	StringBuffer sb = new StringBuffer();
	if (_bindings.size() != _proofs.size()) {
	    System.out.println("BindingSet: Array size mismatch");
	    System.exit(1);
	}
	sb.append("# Bindings = " + _bindings.size() + "   [ Types ");
	for (int ti = 0; ti < _types.size(); ti++) {
	    String var = (String)_vars.get(ti);
	    String type = (String)_types.get(ti);
	    sb.append(var + ":" + (type == null ? "Top" : type) + " ");
	}
        sb.append(" ]\n");
	Iterator bi = _bindings.iterator();
	Iterator pi = _proofs.iterator();
	while (bi.hasNext()) {
	    sb.append("[");
	    ArrayList b = (ArrayList)bi.next();
	    int vi;
	    for (vi = 0; vi < _vars.size(); vi++) {
		sb.append(" " + _vars.get(vi) + "=" + b.get(vi));
	    }
	    ArrayList proof = (ArrayList)pi.next();
	    if (proof.size() > 0) {
		sb.append("  Proof: " + proof.toString());
	    }
	    sb.append(" ]\n");
	}
	return sb.toString();
    }
}
