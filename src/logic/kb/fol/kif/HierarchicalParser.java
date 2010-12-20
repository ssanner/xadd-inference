//////////////////////////////////////////////////////////////////////
//
// File:     HierarchicalParser.java
// Author:   Scott Sanner, University of Toronto (ssanner@cs.toronto.edu)
// Date:     9/1/2003
// Requires: comshell package
//
// Description:
//
//   Parsing of hierarchical files (i.e. LISP-like).  Really should
//   use a regular expression tokenizer like jlex... this is a bit of
//   a hack.
//
//////////////////////////////////////////////////////////////////////

// Package definition
package logic.kb.fol.kif;

// Packages to import
import java.io.*;
import java.math.*;
import java.util.*;

/**
 * Input helper class.
 *
 * @version   1.0
 * @author    Scott Sanner
 * @language  Java (JDK 1.3)
 **/
public class HierarchicalParser
{	
	public Yylex  _lex;
	public Symbol _lastToken;
	public String _source;
	
	public HierarchicalParser() {
		_lex = null;
		_lastToken = null;
		_source = null;
	}
	
	 /** Static file parsing methods
     **/
    public ArrayList parseFile(String filename) {
		try {
			_lex = new Yylex(new FileReader(filename));
			_source = filename;
		    return parse(0);
		} catch (IOException ioe) {
		    System.out.println("Error: " + ioe);
		    return null;
		}
    }

    public ArrayList parseString(String content) {
		_lex = new Yylex(new StringReader(content));
		_source = content;
		return parse(0);
    }
  
    public ArrayList parse(int level) {

		ArrayList a = new ArrayList();
	    Symbol t = null;
	    while ((t = nextToken())._nID != Symbol.EOF) {
				
		    switch (t._nID) {
				case Symbol.LPAREN: a.add(parse(level + 1)); break;
				case Symbol.RPAREN: return a;
				default: a.add(t);
		    }
	    }
	    
		if (level != 0) {
		    System.err.println("'" + _source + 
				       "' contains unbalanced parentheses at line " + 
				       (t==null ? "BEGINNING" : t._nLine) + "!");
		} 
	
		return a;
    }
    
    protected Symbol nextToken() {
    	Symbol s = null;
    	try {
    	    while ((s = _lex.nextToken())._nID == Symbol.COMMENT);
    	} catch (Exception e) {
    	    System.err.println("Error while parsing: " + e);
    	    System.err.println("Last token: " + _lastToken);
    	}
    	_lastToken = s;
    	return s;
    }
}
