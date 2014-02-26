package hgm.reports.urgent;

import hgm.asve.Pair;
import hgm.sampling.gibbs.integral.Interval;
import xadd.ExprLib;
import xadd.XADD;

import java.util.*;
import xadd.ExprLib.*;
import xadd.XADD.*;

/**
 * Created by Hadi Afshar.
 * Date: 4/02/14
 * Time: 7:32 PM
 */
// this was written for aaai but never completed.... //todo take the useful parts and delete...
@Deprecated
public class NewIdea {
/*
    public static void main(String[] args) {
        NewIdea ni = new NewIdea();
//        ni.t1();
        ni.testXaddConstIntegral();
    }

    class LeafInInterval {

        double lowBound;
        double highBound;
        double leaf;

        LeafInInterval(double lowBound, double highBound, double leaf) {
            this.lowBound = lowBound;
            this.highBound = highBound;
            this.leaf = leaf;
        }

        @Override
        public String toString() {
            return leaf + " : [" + lowBound + ", " + highBound + "]";
        }

    }


    class PolyIterator {
        List<LeafInInterval> data = new ArrayList<LeafInInterval>();
        int i = 0;

        PolyIterator(LeafInInterval... data) {
            this.data = Arrays.asList(data);
        }

        boolean hasNext() {
            return i < data.size();
        }

        LeafInInterval next() {
            return data.get(i++);
        }

        int size() {
            return data.size();
        }

        */
/*double integral(int i) {
            return data.get(i).leaf * (data.get(i).highBound - data.get(i).lowBound);
        }*//*


        */
/*double maxLeaf() {
            double max = Double.NEGATIVE_INFINITY;
            for (LeafInInterval d : data) {
                max = Math.max(max, d.leaf);
            }
            return max;
        }
*//*

        double maxHB() {
            double max = Double.NEGATIVE_INFINITY;
            for (LeafInInterval d : data) {
                max = Math.max(max, d.highBound);
            }
            return max;
        }

        double minLB() {
            double min = Double.POSITIVE_INFINITY;
            for (LeafInInterval d : data) {
                min = Math.min(min, d.lowBound);
            }
            return min;
        }

        double totalInterval() {
            return maxHB() - minLB();
        }

    }

    PolyIterator db;

    {
        db = new PolyIterator(
                new LeafInInterval(0d, 1d, 80),
                new LeafInInterval(1d, 3d, 40),
                new LeafInInterval(3d, 7d, 40)
        );
    }

    Random random = new Random();

    private void t1() {

//        Just compute an upper bound for Z_1 as max-leaf-val*(UB_global-LB_global)
//        then for i>2: Z_i = Z_{i-1} - (max-leaf-val)*(UB_i-LB_i) + (actual-leaf-val)*(UB_i-LB_i)

        int[] counts = new int[3];
        for (int i = 0; i < 1000000; i++) {
            double u = random.nextDouble();
            Integer sampledId = sample2(u, 3);
//            System.out.println("itr.data.get(sampleId(i)) = " + itr.data.get(sampledId));
            counts[sampledId]++;
        }

        System.out.println("Arrays.toString(counts) = " + Arrays.toString(counts));

    }

    private int sample2(double u, int K */
/*number of regions*//*
) {
//        System.out.println(" db.totalInterval() = " + db.totalInterval());
//        System.out.println("............");
        double[] z = new double[db.size()];

        double[] m = new double[K];//masses
        double[] v = new double[K];//values
        double[] p = new double[K];//estimated error while calc. normalized mass m_i / Z_i
        for (int i = 0; i < K; i++) {
            LeafInInterval segment = db.data.get(i);
            v[i] = segment.leaf; //costly...
            m[i] = (v[i]) * (segment.highBound - segment.lowBound);

            if (i == 0) {
                z[i] = db.totalInterval() * db.data.get(0).leaf;
            } else {
                z[i] = z[i - 1] - (v[i - 1] - v[i]) * (db.totalInterval() - segment.lowBound); //assuming bound starts at 0!!!!!!
            }

//            System.out.println("z["+i+"] = " + z[i]);

            for (int j = 0; j <= i; j++) {
                p[j] = m[j] / z[i] - (j < i ? m[j] / z[i - 1] : 0);
                if (u < p[j]) return j;
                u = u - p[j];
            }
        }
        throw new RuntimeException();
    }

     ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void testXaddConstIntegral() {
        XADD context = new XADD();
        XADD.XADDNode n = context.getExistNode(context.buildCanonicalXADDFromString("([1*x + 2*y > 5] ([1*x -1*y <2] ([0]) ([10]))([100]))"));//"([x+y>5] ([x-y <2] ([0]) ([10]))([100]))"));
        System.out.println("n.collectVars() = " + n.collectVars());
//        XaddVisualizer.visualize(n, -10, 10, 0.5, "", context);

        Map<XADD.XADDNode, Double> node2maxLeaf = new HashMap<XADD.XADDNode, Double>();
        populateNode2MaxLeaf(n, node2maxLeaf);

        System.out.println("node2maxLeaf = " + node2maxLeaf);

        //....
        Map<String, Double> continuousVarAssign = new HashMap<String, Double>();
        continuousVarAssign.put("x", 0d);
        continuousVarAssign.put("y", 0d);

        String integrationVar = "x";

        HashMap<String, ExprLib.ArithExpr> substitution = new HashMap<String, ExprLib.ArithExpr>();

        for (Map.Entry<String, Double> cVarValue : continuousVarAssign.entrySet()) {
            String cVar = cVarValue.getKey();
            if (!cVar.equals(integrationVar)) { //var should be excluded!
                Double cAssign = cVarValue.getValue();
                substitution.put(cVar, new ExprLib.DoubleExpr(cAssign));
            }
        }

        HashMap<XADDNode, Interval> node2interval = new HashMap<XADDNode, Interval>();//important
        Set<XADDNode> nodesWithLowChildBlocked = new HashSet<XADDNode>();
        Set<XADDNode> nodesWithHighChildBlocked = new HashSet<XADDNode>();
        ArrayList<Interval> chosens = new ArrayList<Interval>();
        substituteAndFindBestPathOneDimXadd(n, true, node2maxLeaf, chosens, node2interval, substitution, context, null, nodesWithLowChildBlocked, nodesWithHighChildBlocked);

        System.out.println("node2interval = " + node2interval);

    }

    //returns the leaf value
    private double substituteAndFindBestPathOneDimXadd(XADDNode node,
                                                       boolean iAmAHighChild,
                                                       Map<XADDNode, Double> node2maxLeaf,
                                                       List<Interval> chosens,
                                                       Map<XADDNode, Interval> node2interval,
                                                       HashMap<String, ExprLib.ArithExpr> subst, XADD context,
                                                       XADDNode father,
                                                       Set<XADDNode> nodesWithLowChildBlocked,
                                                       Set<XADDNode> nodesWithHighChildBlocked) {

        if (node instanceof XADDTNode) {
            //this path should not be visited in the future...
            if (iAmAHighChild) {
                nodesWithHighChildBlocked.add(father);
            } else {
                nodesWithLowChildBlocked.add(father);
            }
            return ((DoubleExpr)(((XADDTNode)node)._expr))._dConstVal;
        }

        XADDINode iNode = (XADDINode)node;
        Interval interval= node2interval.get(node);
        if (interval ==null) { //no interval is calc. for it yet
            interval = createInterval(iNode, subst, context);
            node2interval.put(node, interval);
        }


        chosens.add(interval);

        XADDNode lowChild = ((XADDINode) node).getLowChild();
        Double lowMax = node2maxLeaf.get(lowChild); //assuming they are set...

        XADDNode highChild = ((XADDINode) node).getHighChild();
        Double highMax = node2maxLeaf.get(highChild);
        if (lowMax>highMax) {  //low child will be followed
            //todo
            interval.set(false);
            return substituteAndFindBestPathOneDimXadd(lowChild, false, node2maxLeaf, chosens, node2interval, subst, context);
        }
        else {  //high child will be followed
            interval.set(true);
            return substituteAndFindBestPathOneDimXadd(highChild, true, node2maxLeaf, chosens, node2interval, subst, context);
        }
    }

    private Interval createInterval(XADDINode iNode, HashMap<String, ArithExpr> subst, XADD context) {
        XADD.Decision decision = context._alOrder.get(iNode._var);

        Interval thisDecisionBounds;

        //substitution:
//        if (!(decision instanceof ExprDec)) {
//            throw new SamplingFailureException("is not implemented for boolean case YET. Error in substitution of decision: " + decision);
//        }
        ExprLib.CompExpr comparableExpression = ((ExprDec) decision)._expr;
        comparableExpression = comparableExpression.substitute(subst);
        Expr canonExpr = comparableExpression.makeCanonical(); //todo needed????
//        System.out.println(comparableExpression + " ==canon=> " + canonExpr);
//        if (canonExpr instanceof CompExpr) {
            thisDecisionBounds = fetchComparableExprBounds((CompExpr) canonExpr);
//        } else throw new SamplingFailureException("Expression: " + canonExpr + "cannot be parsed...");
      return thisDecisionBounds;
    }

    private Interval fetchComparableExprBounds(CompExpr comparableExpression) {
        Interval interval = new Interval(null, null);


        //todo Only for debug...
        if (!comparableExpression._rhs.equals(ExprLib.ZERO)) {
            throw new RuntimeException("processXADDLeaf: Expected RHS = 0 for '" + comparableExpression + "'");
        }

        if (comparableExpression._type != ExprLib.CompOperation.GT && comparableExpression._type != ExprLib.CompOperation.GT_EQ)
            throw new RuntimeException("Not implemented for Comparable operation '" + comparableExpression._type + "' in " + comparableExpression);

        // I expect patterns like "(1 + (-0.2 * x))" or "(-0.2 * x)" in the LHS:
        ExprLib.ArithExpr lhs = comparableExpression._lhs;
        if (lhs instanceof DoubleExpr) {
            double c = ((DoubleExpr) lhs)._dConstVal;
            if (c>0 || (c>=0 && comparableExpression._type == ExprLib.CompOperation.GT_EQ)) {
                return new Interval(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY); //always true
            } else {
                return new Interval(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY); //always false
            }
        } else if (!(lhs instanceof ExprLib.OperExpr)) {
            throw new RuntimeException(lhs + " is not an operation...");
        }

        double a, b = 0.0d; // to have (b + a * x)
        ExprLib.OperExpr op = (ExprLib.OperExpr) lhs;
        ArrayList<ExprLib.ArithExpr> terms;
        switch (op._type) {
            case SUM:
                //like: (1 + (-0.2 * x)) [bias + coeff * var]
                terms = op._terms;
                if (terms.size() != 2) throw new RuntimeException("cannot parse: " + terms);
                ExprLib.DoubleExpr bias = (ExprLib.DoubleExpr) terms.get(0);
                b = bias._dConstVal;
                if (!(terms.get(1) instanceof ExprLib.OperExpr)) {
                    throw new RuntimeException("Operation expression was expected in the second argument of: " + op);
                }

                op = ((ExprLib.OperExpr) terms.get(1));
                // after this the PROD CASE should be run as well...
            case PROD:
                //like: (-0.2 * x)  [coeff * var]
                terms = op._terms;
                if (terms.size() != 2) throw new RuntimeException("cannot parse: " + terms);
                ExprLib.DoubleExpr coefficient = (ExprLib.DoubleExpr) terms.get(0);
                a = coefficient._dConstVal;
                if (!(terms.get(1) instanceof ExprLib.VarExpr)) {
                    throw new RuntimeException("Variable was expected as the second param. of: " + op);
                }
                break;

            default:
                throw new RuntimeException("cannot parse Operation " + op);
        }

//        System.out.println("a = " + a);
//        System.out.println("b = " + b);

        double bound = -b / a;

        boolean varIsGreaterThanBound = (a > 0);

        if (varIsGreaterThanBound) interval.setLowBound(bound);
        else interval.setHighBound(bound);

        return interval;
    }


    */
/*
     * returns the last calculated max-leaf
     *//*

    private double populateNode2MaxLeaf(XADD.XADDNode n, Map<XADD.XADDNode, Double> node2maxLeaf) {
        if (n instanceof XADD.XADDTNode) {
            double c = ((DoubleExpr) (((XADDTNode) n)._expr))._dConstVal;
            node2maxLeaf.put(n, c);
            return c;
        }

        XADD.XADDINode iNode = (XADD.XADDINode) n;
        double lowMax=populateNode2MaxLeaf(iNode.getLowChild(), node2maxLeaf);
        double highMax=populateNode2MaxLeaf(iNode.getHighChild(), node2maxLeaf);
        double c = Math.max(lowMax, highMax);
        node2maxLeaf.put(n, c);
        return c;
    }


    */
/*HashMap<String, ExprLib.ArithExpr> substitution = new HashMap<String, ExprLib.ArithExpr>(); //since the int. var. is not added to it
    class ConstInterval {
        double c;
        double lb;
        double ub;
    }
    ConstInterval carrier = new ConstInterval();
//    Map<XADDNode, Interval> node2interval = new HashMap<XADDNode, Interval>();
    public double calcMass(XADD.XADDNode node, String integrationVar, HashMap<String, Double> continuousVarAssign, Map<String, Double> node2maxLeaf) {
        //Exclude the integration var from the assignment and replace doubles with expressions
        substitution.clear();

        for (Map.Entry<String, Double> cVarValue : continuousVarAssign.entrySet()) {
            String cVar = cVarValue.getKey();
            if (!cVar.equals(integrationVar)) { //var should be excluded!
                Double cAssign = cVarValue.getValue();
                substitution.put(cVar, new ExprLib.DoubleExpr(cAssign));
            }
        }

        node2interval.clear();
        findBestPathOneDimXadd(node, node2maxLeaf, node2interval);

        return result;
    }*//*


  */
/*  public List<PolynomialInAnInterval> substituteAndConvertToPiecewisePolynomial(XADD.XADDNode node, HashMap<String, ArithExpr> subst,
                                                                                  Interval inheritedInterval) {

        if (node instanceof XADD.XADDTNode) {
            List<PolynomialInAnInterval> results = new ArrayList<PolynomialInAnInterval>();
            results.add(new PolynomialInAnInterval(inheritedInterval.getLowBound(), inheritedInterval.getHighBound(), ((XADD.XADDTNode) node)._expr.substitute(subst)));
            return results;
        }

        XADD.XADDINode iNode = (XADD.XADDINode) node;
        XADD.Decision decision = context._alOrder.get(iNode._var);

        Interval thisDecisionBounds;

        //substitution:
        if (!(decision instanceof ExprDec)) {
            throw new SamplingFailureException("is not implemented for boolean case YET. Error in substitution of decision: " + decision);
        }
        ExprLib.CompExpr comparableExpression = ((ExprDec) decision)._expr;
        comparableExpression = comparableExpression.substitute(subst);
        Expr canonExpr = comparableExpression.makeCanonical();
//        System.out.println(comparableExpression + " ==canon=> " + canonExpr);
        if (canonExpr instanceof CompExpr) {
            thisDecisionBounds = fetchComparableExprBounds((CompExpr) canonExpr);
        } else throw new SamplingFailureException("Expression: " + canonExpr + "cannot be parsed...");
//            d = new ExprDec(comp);
//            var = getVarIndex(d, true);
//        if (d instanceof BoolDec) {
        // This part is not tested hence commented...
*//*
*/
/*
            // System.out.println(((BoolDec)d)._sVarName + ": " + subst);
            ExprLib.VarExpr sub = (ExprLib.VarExpr) subst.get(((BoolDec) d)._sVarName);
            if (sub != null) {
                // There is a substitution for this BoolDec... get new var index
                var = getVarIndex(new BoolDec(sub._sVarName), false);
            }
*//*
*/
/*

        Interval lowInterval = inheritedInterval.clone();
        lowInterval.imposeMoreRestriction(thisDecisionBounds.highBound, thisDecisionBounds.lowBound); // they are swapped for the low child, note that one bound is NULL...

        List<PolynomialInAnInterval> lowDataList = null;
        if (lowInterval.isFeasible()) {
            lowDataList = substituteAndConvertToPiecewisePolynomial(iNode.getLowChild(), subst, lowInterval);
//            results.addAll(lowDataList);
        }
//        List<PolynomialInAnInterval> lowDataList = computePiecewisePolynomial(iNode.getLowChild());
//        for (PolynomialInAnInterval lowData : lowDataList) {
//            lowData.imposeMoreRestriction(thisDecisionBounds.getHighBound(), thisDecisionBounds.getLowBound()); //low and high bounds are swapped for the low child...
//        }

        inheritedInterval.imposeMoreRestriction(thisDecisionBounds.getLowBound(), thisDecisionBounds.highBound); //to be passed to the high child...
        List<PolynomialInAnInterval> highDataList = null;
        if (inheritedInterval.isFeasible()) {
            highDataList = substituteAndConvertToPiecewisePolynomial(iNode.getHighChild(), subst, inheritedInterval);
//            results.addAll(highDataList);
        }
//        for (PolynomialInAnInterval highData : highDataList) {
//            highData.imposeMoreRestriction(thisDecisionBounds.getLowBound(), thisDecisionBounds.getHighBound()); //low and high bounds are swapped for the low child...
//        }

        if (lowDataList == null) return highDataList;
        if (highDataList == null) return lowDataList;

        //sorting:
        if (lowDataList.get(0).getLowBound() < highDataList.get(0).getLowBound()) {
            lowDataList.addAll(highDataList);
            return lowDataList;
        } else {
            highDataList.addAll(lowDataList);
            return highDataList;
        }
    }
    *//*


    class Wrapper {
        XADD context;
        XADDNode root;
//        Map<XADDNode, List<XADDNode>> node2itsHighFathers; //if B is the high child of A, then A is a high father of B
//        Map<XADDNode, List<XADDNode>> node2itsLowFathers;
        Map<XADDNode, List<XADDNode>> node2fathers;
        TreeSet<XADDTNode> leaves;

        Wrapper(XADD context, XADDNode root) {
            this.context = context;
            this.root = root;

            node2fathers = new HashMap<XADDNode, List<XADDNode>>();

            leaves = new TreeSet<XADDTNode>(new Comparator<XADDTNode>() {
                @Override
                public int compare(XADDTNode o1, XADDTNode o2) {
                    double d1 = ((DoubleExpr)o1._expr)._dConstVal;
                    double d2 = ((DoubleExpr)o2._expr)._dConstVal;
                    if (d1>d2) return -1;
                    if (d1<d2) return 1;
                    if (o1.hashCode()>o2.hashCode()) return -1;
                    if (o1.hashCode()<o2.hashCode()) return 1;
                    return 0;
                }
            });
            init(root, node2fathers, null);
        }

        private void init(XADDNode node, Map<XADDNode, List<XADDNode>> node2fathers, XADDNode newFather) {
            List<XADDNode> fathers = node2fathers.get(node);
            if (fathers == null) {
                fathers = new ArrayList<XADDNode>();
                node2fathers.put(node, fathers);
            }
            if (newFather != null) {
                fathers.add(newFather);
            }

            if (node instanceof XADDTNode) {
                leaves.add((XADDTNode)node);
                return;
            }

            XADDINode iNode = (XADDINode)node;

            init(iNode.getHighChild(), node2fathers, node);
            init(iNode.getLowChild(), node2fathers, node);
        }

        public Iterator<Pair<ArrayList<Decision> */
/*decisions*//*
, ArrayList<Boolean> */
/*decision_values*//*
>> pathsToLeaf(XADDTNode leaf){
            Set<XADDNode> activatedNodes = new HashSet<XADDNode>();
            populateNodeActiveMap(leaf, activatedNodes);
            return new Iterator<Pair<java.util.ArrayList<Decision>, java.util.ArrayList<Boolean>>>() {
                @Override
                public boolean hasNext() {
                    return false;  //To change body of implemented methods use File | Settings | File Templates.
                }

                @Override
                public Pair<ArrayList<Decision>, ArrayList<Boolean>> next() {
                    return null;  //To change body of implemented methods use File | Settings | File Templates.
                }

                @Override
                public void remove() {
                    throw new RuntimeException("not implemented");
                }
            };


        }

        private void populateNodeActiveMap(XADDNode node, Set<XADDNode> actives) {
            actives.add(node);

            List<XADDNode> fathers = node2fathers.get(node);
            for (XADDNode father : fathers) {
                actives.add(father);
            }

        }

//        ArrayList<Decision> decisions, ArrayList<Boolean> decision_values


    }


*/

}
