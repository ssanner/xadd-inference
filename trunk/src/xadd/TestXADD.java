package xadd;

import graph.Graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import xadd.LinearXADDMethod.NamedOptimResult;
import xadd.XADD.*;
import xadd.ExprLib.*;
import camdp.CAction;
import camdp.HierarchicalParser;

public class TestXADD {

    /**
     * @param args .... ..............................................
     */

    public static void main(String[] args) throws Exception {
    	testMinOut(args);
    }

    public static void main4(String[] args) throws Exception {

        // Examples where delta's occur
        // ... one obvious case is running max... need to push delta's into leaves
        //     * standard ops normal, integral takes sum form... delta will subst in constraints
        // ... another is delta of a piecewise function x + y = z where need to eliminate
        //     x or y b/c z is observed

        XADD xadd_context = new XADD();
        int expr1 = TestBuild(xadd_context, "./src/xadd/ex/delta1.xadd");
        int reduced_e1 = xadd_context.reduceLP(expr1);
        xadd_context.getGraph(reduced_e1).launchViewer("Delta reduced");

        // Reducing with LP constraint checking
        //XADDUtils.PlotXADD(xadd_context, reduced_e1, -5, 0.1, 5, "x", "Reduced expression");
        int expr2 = TestBuild(xadd_context, "./src/xadd/ex/xadd_sq_test.xadd");
        xadd_context.getGraph(expr2).launchViewer("XADD Sq");

        int mult_expr = xadd_context.apply(reduced_e1, expr2, XADD.PROD);
        int add_expr = xadd_context.apply(mult_expr, reduced_e1, XADD.SUM);
        xadd_context.getGraph(mult_expr).launchViewer("Delta * XADD Sq");
        xadd_context.getGraph(add_expr).launchViewer("Delta + XADD Sq");

        XADDUtils.PlotXADD(xadd_context, mult_expr, -5, 0.1, 5, "x", "Mult expression");
        XADDUtils.PlotXADD(xadd_context, add_expr, -5, 0.1, 5, "x", "Add expression");

    }

    public static void main3(String[] args) throws Exception {
        XADD xadd_context = new XADD();
        int expr1 = TestBuild(xadd_context, "./src/xadd/ex/xadd_sq_test.xadd");
        xadd_context.getGraph(expr1).launchViewer();
        XADDUtils.PlotXADD(xadd_context, expr1, -10, 0.1, -5, "x", "Original expression");
        
        int reduced_e1 = xadd_context.reduceLP(expr1);
        xadd_context.getGraph(reduced_e1).launchViewer();
        // Reducing with LP constraint checking
        XADDUtils.PlotXADD(xadd_context, reduced_e1, -10, 0.1, -5, "x", "Reduced expression");

        // Squaring
        int sq_expr1 = xadd_context.apply(reduced_e1, reduced_e1, XADD.PROD);
        XADDUtils.PlotXADD(xadd_context, sq_expr1, -10, 0.1, -5, "x", "Squared expression");

        // Indefinite integral
        int int_sq_expr1 = xadd_context.reduceProcessXADDLeaf(sq_expr1,
                xadd_context.new XADDLeafIndefIntegral("x"), /*canonical_reorder*/false);
        xadd_context.getGraph(int_sq_expr1).launchViewer();
        XADDUtils.PlotXADD(xadd_context, int_sq_expr1, -10, 0.1, -5, "x", "Integral squared expression");

        // Definite integral
        int def_int_sq_expr1 = xadd_context.computeDefiniteIntegral(sq_expr1, "x");
        xadd_context.getGraph(def_int_sq_expr1).launchViewer();
        System.out.println(xadd_context.getString(def_int_sq_expr1));
    }

    public static void main2(String[] args) throws Exception {
        double POS_INF = Double.POSITIVE_INFINITY;
        double NEG_INF = Double.NEGATIVE_INFINITY;
        double NAN = Double.NaN;
        System.out.println(0d * POS_INF);
        System.out.println(new Double(POS_INF).equals(POS_INF));
        System.out.println(POS_INF == POS_INF);
        System.out.println(POS_INF);
        System.out.println(NEG_INF);
        System.out.println(NAN);
        System.out.println(POS_INF * POS_INF);
        System.out.println(POS_INF - POS_INF);
        System.out.println(POS_INF - 0d);
        System.out.println(POS_INF - NEG_INF);
        System.out.println(2d + POS_INF);
        System.out.println(2d - POS_INF);
        System.out.println(POS_INF - 2d);
        System.out.println(2d * POS_INF);
        System.out.println(2d / POS_INF);
        System.out.println(POS_INF / 2d);
        System.out.println(Math.max(2d, POS_INF));
        System.out.println(Math.min(2d, POS_INF));
        System.out.println(POS_INF > 0);
        System.out.println(POS_INF < 0);
        System.out.println(POS_INF > POS_INF);
        System.out.println(POS_INF >= POS_INF);
    }

    public static void main1(String[] args) throws Exception {

        TestPolyOps();
        if (0 <= 1) return;

        System.out.println(Double.MAX_VALUE + " , " + (-Double.MAX_VALUE));
        /*
		 * TestParse("[a]");
		 */
		/*
		 * TestParse("[a + b + 3 + 4]"); TestParse("[a + b + 3 + 4 >= 3 / 7]"); TestParse("[a + b + 3 + -4 * y = 9]");
		 * TestParse("[((a + b) * (3 * 4)]"); // Mismatched parens TestParse("[(a + b) * (3 * 4)]");
		 * 
		 * // Build and display an XADD
		 */
        XADD xadd_context = new XADD();

        //////////////////////
        int ixadd = TestBuild(xadd_context, "./src/xadd/ex/test7.xadd");

        Graph g1 = xadd_context.getGraph(ixadd);
        g1.launchViewer();

        int reduce = xadd_context.reduceLP(ixadd);
        g1 = xadd_context.getGraph(reduce);
        g1.launchViewer();

        System.in.read();

        //ixadd  = xadd_context.reduceProcessXADDLeaf(ixadd, max, false);
		/*ArrayList<Decision> decisions = new ArrayList<XADD.Decision>();
		ArrayList<Boolean> decision_values = new ArrayList<Boolean>();
		String leaf = "(99 + (-10 * ay * (1 / (-20 + (-1 * y)))) + (-1 * ay) + (1 * x * ay * (1 / (-20 + (-1 * y)))))";
		ArithExpr leaf_val = ArithExpr.parse(leaf);
		
		//build the decisions
		CompExpr comp_d0 = new CompExpr(xadd_context.GT_EQ, ArithExpr.parse("1 * y"), ArithExpr.parse("0"));
		ExprDec expr_d0 = xadd_context.new ExprDec(comp_d0);
		int obstacleXADD = xadd_context.getVarNode(expr_d0, 0, 1);
		XADDINode inode = (XADDINode) xadd_context._hmInt2Node.get(obstacleXADD);
		Decision dd = xadd_context._alOrder.get(inode._var);
		decisions.add(expr_d0);
		decision_values.add(Boolean.FALSE);
		
		comp_d0 = new CompExpr(xadd_context.GT_EQ, ArithExpr.parse("(1 * ay) + (1 * y)"), ArithExpr.parse("0"));
		expr_d0 = xadd_context.new ExprDec(comp_d0);
		//obstacleXADD =xadd_context.apply(xadd_context.getVarNode(expr_d0, 0, 1),obstacleXADD,xadd_context.PROD); 
		//inode = (XADDINode) xadd_context._hmInt2Node.get(obstacleXADD);
		//dd = xadd_context._alOrder.get(inode._var);
		decisions.add(expr_d0);
		decision_values.add(Boolean.FALSE);
		
		comp_d0 = new CompExpr(xadd_context.GT_EQ, ArithExpr.parse("-20000 + (-1000 * y) + (10 * ay) + (1 * x * ay)"), ArithExpr.parse("0"));
		expr_d0 = xadd_context.new ExprDec(comp_d0);
		decisions.add(expr_d0);
		decision_values.add(Boolean.FALSE);
		
		comp_d0 = new CompExpr(xadd_context.GT_EQ, ArithExpr.parse("-1980 + (1 * x * ay) + (1 * y * ay) + (-99 * y) + (30 * ay)"), ArithExpr.parse("0"));
		expr_d0 = xadd_context.new ExprDec(comp_d0);
		decisions.add(expr_d0);
		decision_values.add(Boolean.FALSE);
		
		comp_d0 = new CompExpr(xadd_context.GT_EQ, ArithExpr.parse("1 * ay"), ArithExpr.parse("0"));
		expr_d0 = xadd_context.new ExprDec(comp_d0);
		decisions.add(expr_d0);
		decision_values.add(Boolean.FALSE);
		
		comp_d0 = new CompExpr(xadd_context.GT_EQ, ArithExpr.parse("10 + (1 * x) + (1 * y)"), ArithExpr.parse("0"));
		expr_d0 = xadd_context.new ExprDec(comp_d0);
		decisions.add(expr_d0);
		decision_values.add(Boolean.FALSE);
		
		comp_d0 = new CompExpr(xadd_context.LT_EQ, ArithExpr.parse("-10 + (1 * x)"), ArithExpr.parse("0"));
		expr_d0 = xadd_context.new ExprDec(comp_d0);
		decisions.add(expr_d0);
		decision_values.add(Boolean.TRUE);
		
		comp_d0 = new CompExpr(xadd_context.GT_EQ, ArithExpr.parse("1 * x"), ArithExpr.parse("0"));
		expr_d0 = xadd_context.new ExprDec(comp_d0);
		decisions.add(expr_d0);
		decision_values.add(Boolean.FALSE);
		
		comp_d0 = new CompExpr(xadd_context.LT_EQ, ArithExpr.parse("10 + (1 * x)"), ArithExpr.parse("0"));
		expr_d0 = xadd_context.new ExprDec(comp_d0);
		decisions.add(expr_d0);
		decision_values.add(Boolean.FALSE);
		
		int ret = max.processXADDLeaf(decisions, decision_values, leaf_val) ;
		*/


        //test linearizing decision nodes
		/*int xadd_test_6 = TestBuild(xadd_context, "./src/xadd/test6.xadd");
		ArrayList<String> cont = new ArrayList<String>();
		cont.add("x");
		cont.add("y");
		cont.add("ay");
		 ret = xadd_context.linearizeDecisions(xadd_test_6, cont);*/

        // Put all boolean variables first to avoid reordering clashes
        xadd_context.getVarIndex(xadd_context.new BoolDec("f"), true);
        xadd_context.getVarIndex(xadd_context.new BoolDec("g"), true);
        xadd_context.getVarIndex(xadd_context.new BoolDec("h"), true);

        int xadd_implied2 = TestBuild(xadd_context, "./src/xadd/ex/implied2.xadd");
        //System.in.read();

        int xadd_circle = TestBuild(xadd_context, "./src/xadd/ex/circle.xadd");
        Graph gc = xadd_context.getGraph(xadd_circle);
        gc.launchViewer();
        //System.in.read();

        // Collect all vars
        HashSet<String> vars = xadd_context.collectVars(xadd_circle);
        System.out.println("Vars in circle.xadd: " + vars);

        // Test out indefinite integration
        // int int1_xac = xadd_context.reduceProcessXADDLeaf(xadd_circle,
        // xadd_context.new XADDLeafIndefIntegral("x1"), /*canonical_reorder*/false);
        // xadd_context.getGraph(int1_xac).launchViewer();
        //
        // int int2_xac = xadd_context.reduceProcessXADDLeaf(int1_xac,
        // xadd_context.new XADDLeafIndefIntegral("x2"), /*canonical_reorder*/false);
        // xadd_context.getGraph(int2_xac).launchViewer();

        int xadd1 = TestBuild(xadd_context, "./src/xadd/ex/test1.xadd");
        int xadd2 = TestBuild(xadd_context, "./src/xadd/ex/test2.xadd");

        int xadd3 = TestBuild(xadd_context, "./src/xadd/ex/test3.xadd");

        int x1_d = TestBuild(xadd_context, "./src/xadd/ex/test4.xadd");
        int d = TestBuild(xadd_context, "./src/xadd/ex/test5.xadd");
        HashMap<String, ArithExpr> hm = new HashMap<String, ArithExpr>();
        hm.put("x1", new DoubleExpr(5d));
        int d2 = xadd_context.substitute(x1_d, hm);
        System.out.println(xadd_context.getString(d));
        System.out.println(xadd_context.getString(d2));
        int prod = xadd_context.apply(d, d2, XADD.PROD);
        System.out.println(xadd_context.getString(prod));
        int prod2 = xadd_context.apply(d2, d2, XADD.PROD);
        System.out.println(xadd_context.getString(prod2));

        //System.in.read();

        HashMap<String, Double> hm_eval = new HashMap<String, Double>();
        hm_eval.put("d", 1d);
        System.out.println(xadd_context.evaluate(prod, new HashMap<String, Boolean>(), hm_eval));

        int dd_norm = xadd_context.computeDefiniteIntegral(prod, "d");
        XADDTNode t = (XADDTNode) xadd_context.getNode(dd_norm);
        double norm_const = ((DoubleExpr) t._expr)._dConstVal;
        int recip_dd_norm = xadd_context.getTermNode(new DoubleExpr(1d / norm_const));
        int final_result = xadd_context.apply(prod, recip_dd_norm, XADD.PROD);
        System.out.println(xadd_context.getString(final_result));

        dd_norm = xadd_context.computeDefiniteIntegral(final_result, "d");
        t = (XADDTNode) xadd_context.getNode(dd_norm);
        norm_const = ((DoubleExpr) t._expr)._dConstVal;
        System.out.println("Final integrated value = " + norm_const);

        xadd_context.computeDefiniteIntegral(xadd3, "x1");

        // Derivative test
        // int der1_xac = xadd_context.reduceProcessXADDLeaf(xadd1,
        // xadd_context.new XADDLeafDerivative("x1"), /*canonical_reorder*/false);
        // xadd_context.getGraph(der1_xac).launchViewer();
        //
        // int der2_xac = xadd_context.reduceProcessXADDLeaf(xadd2,
        // xadd_context.new XADDLeafDerivative("x1"), /*canonical_reorder*/false);
        // xadd_context.getGraph(der2_xac).launchViewer();

        // Elim Example 1: Definite integral over *continuous var* for polynomial leaf case statement
        int int1_xad = xadd_context.computeDefiniteIntegral(xadd1, "x1");
        xadd_context.getGraph(int1_xad).launchViewer();

        int int2_xad = xadd_context.computeDefiniteIntegral(xadd2, "x2");
        xadd_context.getGraph(int2_xad).launchViewer();

        // Elim Example 2: Marginalizing out a *boolean var* (in this case f)
        // (make sure all boolean diagrams are "completed" to include
        // branches for f=true and f=false)
        int restrict_high = xadd_context.opOut(xadd1, xadd_context.getBoolVarIndex("f"), XADD.RESTRICT_HIGH);
        int restrict_low = xadd_context.opOut(xadd1, xadd_context.getBoolVarIndex("f"), XADD.RESTRICT_LOW);
        int marginal = xadd_context.apply(restrict_high, restrict_low, XADD.SUM);
        xadd_context.getGraph(marginal).launchViewer();

        // Note: if you can guarantee every leaf is reached by a true or false path through
        // the variable being summed out, you can use the shortcut operation
        // int marginal = xadd_context.opOut(xadd1, xadd_context.getBoolVarIndex("f"), SUM);

        // Elim Example 3: Integrating out x for [z * \delta(x - y)] for XADDs y and z
        // Note: it is assumed that z contains references to x that will be substituted according to y,
        // y should *not* contain the variable x
        //
        // Here y = xadd_circle(x1,x2,r1,r2), x = k, z = xadd1(k,x1,f)
        xadd1 = TestBuild(xadd_context, "./src/xadd/ex/test1.xadd");

        int y = xadd_context.reduceProcessXADDLeaf(xadd_circle, xadd_context.new DeltaFunctionSubstitution("k", xadd1), /* canonical_reorder */
                true);
        xadd_context.getGraph(y).launchViewer();
        System.out.println(xadd_context.getString(xadd_circle));
        System.out.println("-------------\n" + xadd_context.getString(y));

        System.exit(1);
        xadd_context.getGraph(y).launchViewer();
        System.out.println(xadd_context.getString(xadd_circle));

        // Pause
        System.in.read();

        // *****************TESTING MAX***********
        int xadd4 = TestBuild(xadd_context, "./src/xadd/ex/test4.xadd");
        int xadd5 = TestBuild(xadd_context, "./src/xadd/ex/test5.xadd");
        int xaddrRes = xadd_context.apply(xadd4, xadd5, XADD.MAX);
        Graph gRes = xadd_context.getGraph(xadd_context.reduceLP(xaddrRes));
        gRes.launchViewer();

        int xadd_implied = TestBuild(xadd_context, "./src/xadd/implied.xadd");
        Graph gb = xadd_context.getGraph(xadd_implied);
        gb.launchViewer();
        xadd_implied = xadd_context.reduceLP(xadd_implied);
        Graph gb2 = xadd_context.getGraph(xadd_implied);
        gb2.launchViewer();
        if (true)
            return;

        // **************************************
        System.out.println(">> PROD Operations");
        int xaddr1 = xadd_context.apply(xadd1, xadd2, XADD.PROD);
        g1 = xadd_context.getGraph(xaddr1);
        g1.launchViewer();
        int xaddr2 = xadd_context.opOut(xaddr1, 2, XADD.PROD);
        Graph g2 = xadd_context.getGraph(xaddr2);
        g2.launchViewer();
        System.out.println(">> MAX Operation");
        int xaddr3 = xadd_context.apply(xadd1, xadd2, XADD.MAX);
        Graph g3 = xadd_context.getGraph(xaddr3);
        g3.launchViewer();

        System.out.println(">> Substitutions");
        HashMap<String, ArithExpr> subst = new HashMap<String, ArithExpr>();
        subst.put("k", new DoubleExpr(10d));
        int xaddr4 = xadd_context.substitute(xaddr3, subst);
        Graph g4 = xadd_context.getGraph(xaddr4);
        g4.launchViewer();
        subst.put("x1", new DoubleExpr(0d/* 5d */));
        int xaddr5 = xadd_context.substitute(xaddr3, subst);
        Graph g5 = xadd_context.getGraph(xaddr5);
        g5.launchViewer();
        System.out.println("Vars: " + xadd_context._alOrder);

        HashMap<String, Boolean> bool_assign = new HashMap<String, Boolean>();
        bool_assign.put("f", true);
        bool_assign.put("g", true); // if h instead, eval will be null

        HashMap<String, Double> cont_assign = new HashMap<String, Double>();
        cont_assign.put("k", 0d);
        cont_assign.put("x1", -5d);

        System.out.println(">> Evaluations");
        System.out.println("1 Eval: [" + bool_assign + "], [" + cont_assign + "]" + ": "
                + xadd_context.evaluate(xaddr3, bool_assign, cont_assign));
        cont_assign.put("x1", 10d);
        System.out.println("2 Eval: [" + bool_assign + "], [" + cont_assign + "]" + ": "
                + xadd_context.evaluate(xaddr3, bool_assign, cont_assign));
        cont_assign.put("x2", 7d);
        System.out.println("3 Eval: [" + bool_assign + "], [" + cont_assign + "]" + ": "
                + xadd_context.evaluate(xaddr3, bool_assign, cont_assign));
    }

    public static void TestParse(String s) {
        s = s.substring(1, s.length() - 1);
        CompExpr e = CompExpr.ParseCompExpr(s);
        System.out.println("CompExpr for  '" + s + "': " + e);
        ArithExpr a = ArithExpr.ParseArithExpr(s);
        System.out.println("ArithExpr for '" + s + "': " + a + "\n");
        VarExpr sub = new VarExpr("a");
        ArithExpr a2 = ArithExpr.ParseArithExpr("[c + a]");
        HashMap<String, ArithExpr> subst = new HashMap<String, ArithExpr>();
        subst.put("a", a2);
        if (a != null) {
            System.out.println("EX + EX = " + ArithExpr.op(a, a, ArithOperation.SUM));
            System.out.println("EX - EX = " + ArithExpr.op(a, a, ArithOperation.MINUS));
            System.out.println("EX * EX = " + ArithExpr.op(a, a, ArithOperation.PROD));
            System.out.println("EX / EX = " + ArithExpr.op(a, a, ArithOperation.DIV));
            System.out.println("EX == EX: " + ArithExpr.ParseArithExpr(s).equals(a));
            System.out.println("EX != EX * EX: " + ArithExpr.op(a, a, ArithOperation.PROD).equals(a));
            System.out.println("EX+EX:" + sub + "/" + a2 + ": " + ArithExpr.op(a, a, ArithOperation.SUM).substitute(subst));
        } else if (e != null) {
            System.out.println("EX == EX: " + CompExpr.ParseCompExpr(s).equals(e));
            System.out.println("EX:" + sub + "/" + a2 + ": " + e.substitute(subst));
        }
    }

    public static int TestBuild(XADD xadd_context, String filename) {
        int dd1 = xadd_context.buildCanonicalXADDFromFile(filename);
        //xadd_context.showGraph(dd1, "Parsed Graph: " + filename);
        //xadd_context.showGraph(xadd_context.apply(dd1, dd1, XADD.SUM),  "Parsed Graph, Sum");
        //xadd_context.showGraph(xadd_context.apply(dd1, dd1, XADD.PROD), "Parsed Graph, Prod");
        return dd1;
    }

    public static void TestPolyOps() {
        XADD xadd_context = new XADD();
        //int xadd1 = TestBuild(xadd_context, "src/xadd/test2.xadd");
        int xadd1 = TestBuild(xadd_context, "src/xadd/ex/test7.xadd");
        int xaddr1 = xadd_context.apply(xadd1, xadd1, XADD.SUM);
        xadd_context.getGraph(xaddr1).launchViewer("SUM");
        int xaddr2 = xadd_context.apply(xadd1, xadd1, XADD.MINUS);
        xadd_context.getGraph(xaddr2).launchViewer("MINUS");
        int xaddr3 = xadd_context.apply(xadd1, xadd1, XADD.PROD);
        xadd_context.getGraph(xaddr3).launchViewer("SQUARE");
        int xaddr4 = xadd_context.apply(xaddr3, xaddr3, XADD.PROD);
        xadd_context.getGraph(xaddr4).launchViewer("4 POWER!");
    }

    public static void testVarSubstitute(String[] args) throws Exception {
         // Test XADD substitution and max
        XADD context = new XADD();

        //Simple XADD with abs function
        int dd = context.buildCanonicalXADDFromFile("./src/xadd/ex/test8.xadd");
        context.getGraph(dd).launchViewer();

		HashMap<String,ArithExpr> replace = new HashMap<String, ArithExpr>();
		replace.put("t1", new DoubleExpr(30.0));        
		int sdd = context.substitute(dd,replace);

		context.getGraph(sdd).launchViewer();
        
}

    public static void testLinApprox(String[] args) throws Exception {
        // Test XADD substitution and max
       XADD context = new XADD();

       //Simple XADD with abs function
       int dd = context.buildCanonicalXADDFromFile("./src/xadd/ex/testApprox2.xadd");
       double allow_error = 0.2;
       context.getGraph(dd).launchViewer("Original");

		int approxDD = context.linPruneRel(dd, allow_error);
		context.getGraph(approxDD).launchViewer("approximated");

		int upperApproxDD = context.linUpperPruneRel(dd, allow_error);
		context.getGraph(upperApproxDD).launchViewer("UpperBoundApproximated");
    
    }

    public static void testMinOut(String[] args) throws Exception {
        // Test XADD substitution and max
       XADD context = new XADD();

       //Simple XADD with abs function
       int dd = context.buildCanonicalXADDFromFile("./src/xadd/ex/testMinOut.xadd");
       context.getGraph(dd).launchViewer("Original");

       XADDLeafMinOrMax max = context.new XADDLeafMinOrMax("t4", -100, 100, false/* is_max */, System.out);
       context.reduceProcessXADDLeaf(dd, max, false);
       int minDD = max._runningResult;
    		   
       context.getGraph(minDD).launchViewer("minOutDD");
    
    }
    
    
    public static void testBVarSubs(String[] args) throws Exception {
//        Test XADD substitution and max
        System.out.println("TestXADD: Testing Boolean Vars Substitution");
        
        XADD context = new XADD();
//        Simple XADD with boolean vars
//        Alternative dd:       int oriDD = context.buildCanonicalXADDFromFile("./src/xadd/ex/boolvars.xadd");
       int oriDD = context.buildCanonicalXADDFromFile("./src/xadd/ex/boolxor.xadd");
       Graph g1 = context.getGraph(oriDD);
       g1.launchViewer("Original DD");

//       Substitute Bool Vars
       HashMap<String,Boolean> replace = new HashMap<String, Boolean>();
       replace.put("b1", true);
       replace.put("b2", true);
       replace.put("b3", false);
       int sDD = context.substituteBoolVars(oriDD,replace);
       Graph gS = context.getGraph(sDD);
       gS.launchViewer("After Sub b1->T, b2->T & b3->F");
       System.out.println("Test Finish.");
    }
}
