// SCOTT's NOTES ON PRODUCTIONS... need NODELIST, VARLIST, TERMLIST
// and Java classes/constructors to handle

package logic.kb.fol.kif;

import java.util.*;
import logic.kb.fol.*;

public class KIFParser {

	public static synchronized FOPC.Node parse(String s) {
		HierarchicalParser hp = new HierarchicalParser();
		ArrayList al = hp.parseString(s);
		//System.out.println("Input String: " + s);
		//System.out.println("Output ArrayList from HP: " + al);
		
		// Here, we just want the first KIF formula...
		if (!(al.get(0) instanceof ArrayList)) {
			System.err.println("KIF statement must begin with '('");
			return null;
		}
		ArrayList first = (ArrayList)al.get(0);
		FOPC.Node ret = SafeParseKIF(first);
		//System.out.println("Final parse: " + ret.toFOLString());
		return ret;
	}

	public static synchronized ArrayList parseFile(String filename) {
		HierarchicalParser hp = new HierarchicalParser();
		ArrayList al = hp.parseFile(filename);
		//System.out.println(al);
		ArrayList ret = new ArrayList();
		for (Iterator i = al.iterator(); i.hasNext(); ) {
			ArrayList sublist = (ArrayList)i.next();
			ret.add(SafeParseKIF(sublist));
		}
		return ret;
	}

	private static FOPC.Node SafeParseKIF(ArrayList l) {
		try {
			return List2FOPC(l);
		} catch (Exception e) {
			e.printStackTrace(System.err);
			System.err.println(e);
			System.err.println("Could not parse: " + l);
			System.err.println("Continuing...");
			return null;
		}
	}
	
	//  node = (conn:and/or nodelist)
	//  node = (conn:not node)
	//  node = (quant:exists/forall/count varlist)
	//  node = (ident termlist)
	//  node = (=... term term)
	public static FOPC.Node List2FOPC(ArrayList l) {
		if (l.size() < 0) {
			System.err.println("Empty list is illegal");
			return null;
		} else if (!(l.get(0) instanceof Symbol)) {
			System.err.println("Expected symbol, but got " + l.get(0));
			return null;
		}
		
		FOPC.Node RESULT = null;
		Symbol first = (Symbol)l.get(0);
		switch (first._nID) {
		case Symbol.FORALL:
		case Symbol.EXISTS:
		case Symbol.COUNT: {
			// Quantifier
			RESULT = new FOPC.QNode(List2FOPC((ArrayList)l.get(2)));
			int quant_id = 
					(first._nID == Symbol.FORALL) 
					? FOPC.QNode.FORALL 
					: ((first._nID == Symbol.EXISTS) 
					      ? FOPC.QNode.EXISTS 
					      : FOPC.QNode.COUNT);
			ArrayList vars = (ArrayList)l.get(1);
			Iterator vi = vars.iterator();
			while (vi.hasNext()) {
				Symbol v = (Symbol)vi.next();
				if (v._nID != Symbol.VARIABLE) {
					System.err.println("Expected var but got " + v + " at line " +
							v._nLine);
					return null;
				}
				((FOPC.QNode)RESULT).addQuantifier(quant_id, new FOPC.TVar((String)v._value));
			}
			
		} break;
		case Symbol.AND:
		case Symbol.OR: {
			// N-ary Connectives
			int conn = first._nID == Symbol.AND ? FOPC.ConnNode.AND : FOPC.ConnNode.OR;
			RESULT = new FOPC.ConnNode(conn);
			for (int i = 1; i < l.size(); i++) {
				((FOPC.ConnNode)RESULT).addSubNode(List2FOPC((ArrayList)l.get(i)));
			}
		} break;
		case Symbol.IMPLY:
		case Symbol.EQUIV: {
			// Binary Connective
			int conn = first._nID == Symbol.IMPLY ? FOPC.ConnNode.IMPLY : FOPC.ConnNode.EQUIV;
			RESULT = new FOPC.ConnNode(conn, 
						List2FOPC((ArrayList)l.get(1)),
						List2FOPC((ArrayList)l.get(2)));
		} break;
		case Symbol.NOT: {
			// Unary Connective
			RESULT = new FOPC.NNode(List2FOPC((ArrayList)l.get(1)));
		} break;
		case Symbol.EQUAL:
		case Symbol.NEQUAL:
		case Symbol.LESS:
		case Symbol.LESSEQ:
		case Symbol.GREATEREQ:
		case Symbol.GREATER: {
			// boolean is_neg, int pred_id, TermList l, TermList r
			boolean is_neg = first._nID == Symbol.NEQUAL ||
                             first._nID == Symbol.GREATEREQ || 
                             first._nID == Symbol.GREATER;
			int pred_id = FOPC.PNode.INVALID;
			if (first._nID == Symbol.NEQUAL || first._nID == Symbol.EQUAL) {
				pred_id = FOPC.PNode.EQUALS;
			} else if (first._nID == Symbol.LESSEQ || first._nID == Symbol.GREATER) {
				pred_id = FOPC.PNode.LESSEQ;
			} else if (first._nID == Symbol.LESS || first._nID == Symbol.GREATEREQ) {
				pred_id = FOPC.PNode.LESS;
			}
			FOPC.TermList lhs = new FOPC.TermList(ParseTerm(l.get(1)));
			FOPC.TermList rhs = new FOPC.TermList(ParseTerm(l.get(2)));
			RESULT = new FOPC.PNode(is_neg, pred_id, lhs, rhs);
			
		} break;
		case Symbol.IDENT: {
			// Predicate - first parse term list
			FOPC.TermList tl = new FOPC.TermList();
			for (int i = 1; i < l.size(); i++) {
				tl = new FOPC.TermList(tl, 
						new FOPC.TermList(ParseTerm(l.get(i))));
			}
			
			// boolean is_neg, String pred, TermList l
			RESULT = new FOPC.PNode(false, (String)first._value, tl);
		} break;
		case Symbol.TRUE: 
		case Symbol.FALSE: {
			// True/False
			RESULT = new FOPC.TNode(first._nID == Symbol.TRUE);
		} break;
		default: {
			System.err.println("Unexpected symbol " + first + " at line " +
					first._nLine);
		} break;
		}
		return RESULT;
	}
		
	public static FOPC.Term ParseTerm(Object o) {
		
		if (o instanceof Symbol) {
			Symbol s = (Symbol)o;
			switch (s._nID) {
			case Symbol.INTEGER:  return new FOPC.TInteger((Integer)s._value);
			case Symbol.DOUBLE:   return new FOPC.TScalar((Double)s._value);
			case Symbol.IDENT:    return new FOPC.TFunction((String)s._value);
			case Symbol.VARIABLE: return new FOPC.TVar((String)s._value);
			default: System.err.println("Expected FOL term but received " + s +
					                    " at line " + s._nLine); 
			}
			return null;
		}
		
		ArrayList l = (ArrayList)o;
		if (l.size() < 0) return null;
		
		FOPC.Term RESULT = null;
		Symbol first = (Symbol)l.get(0);
		switch (first._nID) {
		case Symbol.PLUS:
		case Symbol.MINUS:
		case Symbol.MOD:
		case Symbol.DIV:
		case Symbol.TIMES: {
			// Pre-defined function
			FOPC.TermList lhs = new FOPC.TermList(ParseTerm(l.get(1)));
			FOPC.TermList rhs = new FOPC.TermList(ParseTerm(l.get(2)));

			switch (first._nID) {
			case Symbol.PLUS:  RESULT = new FOPC.TFunction("f_add", lhs, rhs); break;
			case Symbol.MINUS: RESULT = new FOPC.TFunction("f_sub", lhs, rhs); break;
			case Symbol.MOD:   RESULT = new FOPC.TFunction("f_mod", lhs, rhs); break;
			case Symbol.DIV:   RESULT = new FOPC.TFunction("f_div", lhs, rhs); break;
			case Symbol.TIMES: RESULT = new FOPC.TFunction("f_mul", lhs, rhs); break;
			}
		} break;
		case Symbol.IDENT: {
			// User-defined function - first parse term list
			FOPC.TermList tl = new FOPC.TermList();
			for (int i = 1; i < l.size(); i++) {
				tl = new FOPC.TermList(tl, 
						new FOPC.TermList(ParseTerm(l.get(i))));
			}
			
			// boolean is_neg, String pred, TermList l
			RESULT = new FOPC.TFunction((String)first._value, tl);
		} break;
		default: {
			System.err.println("Unexpected term symbol " + first + " at line " +
					first._nLine);
		} break;
		}
		return RESULT;
	}
	
	public static void main(String[] args) {
		ArrayList al = parseFile(args[0]);
		System.out.println("Parsed: '" + args[0] + "'");
		for (int i = 0; i < al.size(); i++) {
			System.out.println("[" + i + "] " + al.get(i));
		}
		
	}
}
