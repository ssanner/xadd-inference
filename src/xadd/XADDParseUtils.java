package xadd;

import java.text.DecimalFormat;
import java.util.ArrayList;

import camdp.HierarchicalParser;

import logic.kb.fol.FOPC;
import xadd.ExprLib.*;
import xadd.XADD;
import xadd.XADD.*;

// Parses a tree-based written input format allowing operations and pre-defined functions at leaves
public class XADDParseUtils {

	public final static DecimalFormat _df = new DecimalFormat("#.########################");	
	
	@SuppressWarnings("rawtypes")
	public static int BuildCanonicalXADD(XADD context, ArrayList l) {
		if (l.size() == 1) {
			// Terminal node// System.out.println("Parsing: '" + l.get(0) + "'");
			if (!(l.get(0) instanceof String)) {
				System.err.println("Cannot parse terminal: '" + l.get(0)
						+ "'\n... make sure terminals are surrounded in [ ]");
				System.exit(1);
			}
			String s = (String) l.get(0);
			// Work around a parser issue with singleton vars in brackets
			if (s.startsWith("["))
				s = s.substring(1, s.length() - 1);
			
			s = s.trim();
			if (s.equalsIgnoreCase("legal") || s.equalsIgnoreCase("neg-inf") || s.equalsIgnoreCase("-Infinity")) {
				return context.NEG_INF;
			} else if (s.equalsIgnoreCase("illegal") || s.equalsIgnoreCase("pos-inf") || s.equalsIgnoreCase("Infinity")) {
				return context.POS_INF;
			} else {
				int n = ParseIntoXADD(context, s);
				if (n < 0) {
					System.err.println("Failed to parse: '" + s + "'");
					System.exit(1);
				}
				return n;
			}
		} else if (l.size() == 3) {
			// TODO: could also allow arithmetic expressions internally here
			
			// Internal node
			String expr = ((String) l.get(0));
			Decision d = null;
			// System.out.println("Expr: " + expr);
			if (expr.startsWith("[")) {
				CompExpr c = CompExpr.ParseCompExpr(expr);
				if (c != null) {
					// System.out.println("CompExpr: " + c);
					d = context.new ExprDec(c);
				}
			} else {
				d = context.new BoolDec(expr);
			}

			if (d == null) {
				System.out.println("Could not buildNonCanonicalXADD for terminal '"	+ l + "'");
				return -1;
			} else {
				// System.out.println("Var expr: " + d);
				int var = context.getVarIndex(d, true);
				int high = BuildCanonicalXADD(context, (ArrayList) l.get(1));
				int low = BuildCanonicalXADD(context, (ArrayList) l.get(2));

				return context.getINodeCanon(var, low, high);
			}
		} else {
			// Unknown
			System.out.println("Could not buildNonCanonicalXADD for "+ l.size() + " args '" + l + "'");
			return -1;
		}
	}
	
	public static int ParseIntoXADD(XADD context, String s) {
		try {
			FOPC.Node res = FOPC.parse(s + " = 0");
			// if (res != null) System.out.println("==> " + res.toFOLString());
			return Convert2XADD(context, ((FOPC.PNode) res).getBinding(0));
		} catch (Exception e) {
			System.err.println("Could not convert: " + s + "\n" + e);
			e.printStackTrace(System.err);
			System.exit(1);
			return -1;
		}
	}

	public static int Convert2XADD(XADD context, FOPC.Term t) {
		// System.out.println("Convert2ArithExpr: " + t.toFOLString());
		if (t instanceof FOPC.TVar) {
			return context.getTermNode(new VarExpr(((FOPC.TVar) t)._sName));
		} else if (t instanceof FOPC.TScalar) {
			return context.getTermNode(new DoubleExpr(
					((FOPC.TScalar) t)._dVal));
		} else if (t instanceof FOPC.TInteger) {
			return context.getTermNode(new DoubleExpr(
					((FOPC.TInteger) t)._nVal));
		} else if (t instanceof FOPC.TFunction) {
			return ConvertFunction2XADD(context, (FOPC.TFunction) t);
		} else
			return -1;
	}

	@SuppressWarnings("rawtypes")
	// Builds XADD polynomial (approximations) of supported functions
	public static int ConvertFunction2XADD(XADD context, FOPC.TFunction t) {

		if (t._nArity == 0)
			return context.getTermNode(new VarExpr(t._sFunName));

		if (t._sFunName.equals("N") && t._nArity == 4) {

			ArithExpr expr = ArithExpr.Convert2ArithExpr(t.getBinding(0));
			ArithExpr mu = ArithExpr.Convert2ArithExpr(t.getBinding(1));
			ArithExpr var = ArithExpr.Convert2ArithExpr(t.getBinding(2));
			ArithExpr width = ArithExpr.Convert2ArithExpr(t.getBinding(3)); // truncated
			// outside
			// of
			// +/-
			// width

			if (!(var instanceof DoubleExpr)) {
				System.out
				.println("Currently cannot handle non-constant variance: "
						+ var.toString());
				System.exit(1);
			}

			if (!(width instanceof DoubleExpr)) {
				System.out
				.println("Currently cannot handle non-constant width: "
						+ width.toString());
				System.exit(1);
			}
			double dwidth = ((DoubleExpr) width)._dConstVal;

			double Z = 3d / (4d * dwidth * dwidth * dwidth);
			double DW2 = dwidth * dwidth;
			String s = "([" + expr + " >= " + mu + " - " + dwidth + "] "
					+ "([" + expr + " <= " + mu + " + " + dwidth + "] "
					+ "( [" + _df.format(Z) + " * " + "(-((" + expr + " - "
					+ mu + ") * (" + expr + " - " + mu + ")) + " + DW2
					+ ")] )" + "( [0.0] ) ) ( [0.0] ) )";
			// String s = "([7.0])";

			// System.out.println("Produced: " + s);

			ArrayList l = HierarchicalParser.ParseString(s);
			// System.out.println("Parsed: " + l);
			int dd = BuildCanonicalXADD(context, (ArrayList) l.get(0));
			return dd;

		} else if (t._sFunName.equals("U") && t._nArity == 4) {

			ArithExpr expr = ArithExpr.Convert2ArithExpr(t.getBinding(0));
			ArithExpr mu = ArithExpr.Convert2ArithExpr(t.getBinding(1));
			ArithExpr widthl = ArithExpr.Convert2ArithExpr(t.getBinding(2)); // width
			// left
			ArithExpr widthr = ArithExpr.Convert2ArithExpr(t.getBinding(3)); // width
			// right

			if (!(widthl instanceof DoubleExpr)) {
				System.out
				.println("Currently cannot handle non-constant variance: "
						+ widthl.toString());
				System.exit(1);
			}
			double dwidthl = ((DoubleExpr) widthl)._dConstVal;

			if (!(widthr instanceof DoubleExpr)) {
				System.out
				.println("Currently cannot handle non-constant width: "
						+ widthr.toString());
				System.exit(1);
			}
			double dwidthr = ((DoubleExpr) widthr)._dConstVal;

			if (dwidthl < 0 || dwidthr < 0) {
				System.out.println("Negative widths (" + dwidthl + ","
						+ dwidthr + ") not allowed.");
				System.exit(1);
			}

			double Z = 1d / (dwidthl + dwidthr);
			String s = "([" + expr + " >= " + mu + " - " + dwidthl + "] "
					+ "([" + expr + " <= " + mu + " + " + dwidthr + "] "
					+ "( [" + _df.format(Z) + "] )"
					+ "( [0.0] ) ) ( [0.0] ) )";
			ArrayList l = HierarchicalParser.ParseString(s);
			int dd = BuildCanonicalXADD(context, (ArrayList) l.get(0));
			return dd;

		} else if (t._sFunName.equals("T") && t._nArity == 4) {

			ArithExpr expr = ArithExpr.Convert2ArithExpr(t.getBinding(0));
			ArithExpr mu = ArithExpr.Convert2ArithExpr(t.getBinding(1));
			ArithExpr widthl = ArithExpr.Convert2ArithExpr(t.getBinding(2)); // width
			// left
			ArithExpr widthr = ArithExpr.Convert2ArithExpr(t.getBinding(3)); // width
			// right

			if (!(widthl instanceof DoubleExpr)) {
				System.out
				.println("Currently cannot handle non-constant variance: "
						+ widthl.toString());
				System.exit(1);
			}
			double dwidthl = ((DoubleExpr) widthl)._dConstVal;

			if (!(widthr instanceof DoubleExpr)) {
				System.out
				.println("Currently cannot handle non-constant width: "
						+ widthr.toString());
				System.exit(1);
			}
			double dwidthr = ((DoubleExpr) widthr)._dConstVal;

			if (dwidthl < 0 || dwidthr < 0) {
				System.out.println("Negative widths (" + dwidthl + ","
						+ dwidthr + ") not allowed.");
				System.exit(1);
			}

			double H = 2d / (dwidthr + dwidthl);
			String s = null;

			// Handle cases where left- or right-hand sides are empty
			if (dwidthl == 0d) {
				s = "([" + expr + " >= " + mu + "] " + "([" + expr + " <= "
						+ mu + " + " + dwidthr + "] " + "( ["
						+ _df.format(-H / dwidthr) + " * " + "(" + expr
						+ " - " + mu + " - " + widthr + ")] )"
						+ "( [0.0] ) ) ( [0.0] ) )";
			} else if (dwidthr == 0d) {
				s = "([" + expr + " >= " + mu + " - " + dwidthl + "] "
						+ "([" + expr + " <= " + mu + "] " + "( ["
						+ _df.format(H / dwidthl) + " * " + "(" + expr
						+ " - " + mu + " + " + widthl + ")] )"
						+ "( [0.0] ) ) ( [0.0] ) )";
			} else {
				s = "([" + expr + " >= " + mu + " - " + dwidthl + "] "
						+ "([" + expr + " <= " + mu + " + " + dwidthr
						+ "] " + "([" + expr + " <= " + mu + "] " + "( ["
						+ _df.format(H / dwidthl) + " * " + "(" + expr
						+ " - " + mu + " + " + widthl + ")] )" + "( ["
						+ _df.format(-H / dwidthr) + " * " + "(" + expr
						+ " - " + mu + " - " + widthr + ")] ))"
						+ "( [0.0] ) ) ( [0.0] ) )";
			}

			ArrayList l = HierarchicalParser.ParseString(s);
			int dd = BuildCanonicalXADD(context, (ArrayList) l.get(0));
			return dd;

		} else if (t._nArity == 2) {

			// A standard operator expression convertible to a terminal node
			int xadd1 = Convert2XADD(context, t.getBinding(0));
			int xadd2 = Convert2XADD(context, t.getBinding(1));
			int op = XADD.UND;
			if (t._sFunName.equals("f_add")) {
				op = XADD.SUM;
			} else if (t._sFunName.equals("f_sub")) {
				op = XADD.MINUS;
			} else if (t._sFunName.equals("f_mul")) {
				op = XADD.PROD;
			} else if (t._sFunName.equals("f_div")) {
				op = XADD.DIV;
			} else {
				System.err.println("Convert2XADD: Could not process binary function: " + t.toFOLString());
				System.exit(1);
			}
			return context.apply(xadd1, xadd2, op);
		} else {
			System.err.println("Convert2XADD: Could not process: " + t.toFOLString());
			System.exit(1);
			return -1;
		}
	}

}
