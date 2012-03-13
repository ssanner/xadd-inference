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
 * @version 1.0
 * @author Scott Sanner
 * @language Java (JDK 1.3)
 */
public class MultiMap {

	public HashMap _hm;

	public MultiMap() {
		_hm = new HashMap();
	}

	public Set getValues(Object key) {
		Set s = (Set) _hm.get(key);
		if (s == null) {
			s = new HashSet();
			_hm.put(key, s);
		}
		return s;
	}

	public void putValue(Object key, Object value) {
		getValues(key).add(value);
	}

	public void putValues(Object key, Set values) {
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

	HashSet _hsTrans = new HashSet();

	// Node could be problems if this return value is modified,
	// or used after the method called a second time.
	public Set transClosure(Object key) {
		_hsTrans.clear();
		transClosureInt(key);
		return _hsTrans;
	}

	public void transClosureInt(Object key) {
		if (_hsTrans.contains(key)) {
			return;
		}
		_hsTrans.add(key);
		Set s = getValues(key);
		Iterator i = s.iterator();
		while (i.hasNext()) {
			transClosureInt(i.next());
		}
	}
}
