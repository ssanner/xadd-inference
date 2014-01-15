//////////////////////////////////////////////////////////////////////
//
// File:     HierarchicalParser.java
// Author:   Scott Sanner, University of Toronto (ssanner@cs.toronto.edu)
// Date:     9/1/2003
// Requires: comshell package
//
// Description:
//
//   Parsing of hierarchical files (i.e. LISP-like).
//
//////////////////////////////////////////////////////////////////////

// Package definition
package camdp;

// Packages to import

import java.math.*;
import java.util.*;

/**
 * Input helper class.
 *
 * @author Scott Sanner & Hadi Afshar
 * @version 1.0
 * @language Java (JDK 1.3)
 */
public class HierarchicalParser {
    /**
     * Keyword identifier
     */
    public static class Keyword {
        public String _s;

        public Keyword(String s) {
            _s = s;
        }

        public String toString() {
            return "K:[" + _s + "]";
        }

        public boolean matches(String s) {
            return _s.equalsIgnoreCase(s);
        }
    }

    /**
     * Static file parsing methods
     */
    public static ArrayList ParseString(String content) {

        try {
            TokenStream ts = new TokenStream();
            ts.openFromStringContent(content);
            return ParseFileInt(ts, 0);
        } catch (TokenStreamException tse) {
            System.out.println("Error: " + tse);
            return null;
        }

    }

    /**
     * Static file parsing methods
     */
    public static ArrayList ParseFile(String filename) {

        try {
            TokenStream ts = new TokenStream();
            ts.open(filename);
            return ParseFileInt(ts, 0);
        } catch (TokenStreamException tse) {
            System.out.println("Error: " + tse);
            return null;
        }

    }

    /**
     * Handles paren nesting and converting Integer.Integer -> Double. Assumes
     * an Integer must follow a period.
     * 
     * Modified by Hadi to handle comments
     */
    public static ArrayList ParseFileInt(TokenStream ts, int level) {

        ArrayList a = new ArrayList();
        try {
            Token t;
            for(;;) {
//            while ((t = ts.nextToken()) != null) {
                t = firstNonCommentToken(ts, ts.nextToken());
                if (t == null) break;



                if (t._sToken == null) {
                    switch (t._nSymbolID) {
                        case Token.LPAREN:
                            ArrayList sublist = ParseFileInt(ts, level + 1);
                            //if (sublist.size() > 0)
                            a.add(sublist);
                            break;
                        case Token.RPAREN:
                            return a;
                        case Token.PERIOD: {
                            Token t_next = ts.nextToken();
                            if (Character.isLetter(t_next._sToken.charAt(0))) {
                                // Keyword - so can load Spudd output files as well
                                if (!t_next._sToken.trim().equals("")) {
                                    //System.out.println("Adding: " + t_next._sToken);
                                    a.add(new Keyword(t_next._sToken));
                                }
                            } else {
                                // Decimal number
                                int max_index = a.size() - 1;
                                Object o = a.get(max_index);
                                String bds = null;
                                if (o instanceof String) {
                                    try {
                                        bds = ((String) o) + "." + t_next._sToken;
                                        a.set(max_index, new BigDecimal(bds));
                                    } catch (NumberFormatException nfe) {
                                        System.out.println("Parse error after period: " + t);
                                        System.out.println("Could not translate: " + bds);
                                        System.exit(1);
                                    }
                                } else {
                                    System.out.println("Number must preceed '.' "
                                            + "followed by number: " + t);
                                    System.exit(1);
                                }
                            }
                        }
                        break;
                    }
                } else if (t._bInteger) {
                    if (!t._sToken.trim().equals("")) {
                        //System.out.println("Adding: " + t._sToken);
                        a.add(t._sToken); // Could make into a double
                    }
                } else {
                    if (!t._sToken.trim().equals("")) {
                        //System.out.println("Adding: " + t._sToken);
                        a.add(t._sToken);
                    }
                }
            }
        } catch (TokenStreamException tse) {
            System.out.println("Error: " + tse);
            return null;
        }

        if (level != 0) {
            System.out.println("'" + ts._sFilename
                    + "' contains unbalanced parentheses!");
            System.exit(1);
        }

        return a;
    }

    // Added by Hadi: starting from the "startingToken", it skips commented lines (in case any) and returns a non-commented token
    private static Token firstNonCommentToken(TokenStream ts, Token currentToken) throws TokenStreamException {
        if (isTheStartOfACommentedBlock(currentToken)) {
            int commentLine = currentToken._nLine;
            for (;;){
                currentToken = ts.nextToken();
                if (currentToken== null) return null;
                if (currentToken._nLine != commentLine) return firstNonCommentToken(ts, currentToken);
            }
        }

        return currentToken;
    }

    private static boolean isTheStartOfACommentedBlock(Token token) {
        return token!=null && token._sToken !=null && token._sToken.equals("%");
    }
}
