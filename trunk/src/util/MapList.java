//////////////////////////////////////////////////////////////////////
//
// File:     MultiMap.java
// Author:   Scott Sanner, University of Toronto (ssanner@cs.toronto.edu)
// Date:     9/1/2003
// Requires: comshell package
//
// Description:
//
//   MultiMap data structure.
//
//   NOTE: Transitive closure is really reflexive transitive closure!!!
//
//////////////////////////////////////////////////////////////////////

// Package definition
package util;

// Packages to import

import java.io.*;
import java.math.*;
import java.util.*;

import graph.*;

/**
 * @author Scott Sanner
 * @version 1.0
 * @language Java (JDK 1.3)
 */
public class MapList {

    public Map _hm;
    public boolean _bUnique; // Restricts to unique values (i.e., a map interpretation)

    public MapList() {
        this(false, false);
    }

    public MapList(boolean unique) {
        this(unique, false);
    }

    public MapList(boolean unique, boolean hash) {
        _hm = hash ? new HashMap() : new TreeMap();
        _bUnique = unique;
    }

    public void setUnique(boolean unique) {
        _bUnique = unique;
    }

    public ArrayList getValues(Object key) {
        ArrayList s = (ArrayList) _hm.get(key);
        if (s == null) {
            s = new ArrayList();
            _hm.put(key, s);
        }
        return s;
    }

    public void putValue(Object key, Object value) {
        if (_bUnique) {
            ArrayList vals = getValues(key);
            vals.clear();
            vals.add(value);
        } else
            getValues(key).add(value);
    }

    public void putValues(Object key, ArrayList values) {
        if (_bUnique) {
            ArrayList val_array = getValues(key);
            if (values.size() >= 1) {
                System.out.println("ERROR: Should not add multiple values for one key in unique MapList");
                System.out.println("       " + key + " -> " + values);
                val_array.clear();
                val_array.add(values.get(0));
            }
        } else
            getValues(key).addAll(values);
    }

    public void clear() {
        _hm.clear();
    }

    public boolean contains(Object key, Object value) {
        return getValues(key).contains(value);
    }

    public Set keySet() {
        return _hm.keySet();
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        Iterator i = _hm.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry me = (Map.Entry) i.next();
            sb.append("    " + me.getKey() + " -> " + me.getValue() + "\n");
        }
        return sb.toString();
    }

    ArrayList _hsTrans = new ArrayList();

    // Node could be problems if this return value is modified,
    // or used after the method called a second time.
    public ArrayList transClosure(Object key) {
        _hsTrans.clear();
        transClosureInt(key);
        return _hsTrans;
    }

    public void transClosureInt(Object key) {
        if (_hsTrans.contains(key)) {
            return;
        }
        _hsTrans.add(key);
        ArrayList s = getValues(key);
        Iterator i = s.iterator();
        while (i.hasNext()) {
            transClosureInt(i.next());
        }
    }
}
