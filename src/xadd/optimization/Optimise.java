package xadd.optimization;

import graph.Graph;
import xadd.ExprLib;
import xadd.XADD;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;

public class Optimise {

    /**
     *
     * @param context
     * @param xaddID
     * @param constraintsMap
     * @param optimiser
     * @return
     */
    public static double optimisePaths(XADD context, Integer xaddID, HashMap<Integer, String> constraintsMap,
                                       IOptimisationTechnique optimiser) {

        double lowM = XADD.DEFAULT_LOWER_BOUND;;
        double highM = XADD.DEFAULT_LOWER_BOUND;;

        XADD.XADDNode rootNode = context.getExistNode(xaddID);
        if(constraintsMap == null) {
            constraintsMap = new HashMap<Integer, String>();
        }

        if(rootNode instanceof XADD.XADDTNode) {
            XADD.XADDTNode tNode = (XADD.XADDTNode) rootNode;

            // Extract the ExprLib.ArithExpr contained in the node
            ExprLib.ArithExpr expression = tNode._expr;

            // If the expression is a DoubleExpr, then return the _dConstVal and skip
            // evaluating the mathematical program
            if(expression instanceof ExprLib.DoubleExpr) {
                return ((ExprLib.DoubleExpr) expression)._dConstVal;
            }

            HashSet<String> lowerBounds = new HashSet<String>();
            HashSet<String> upperBounds = new HashSet<String>();

            for(Map.Entry<String, Double> entry : context._hmMinVal.entrySet()) {
                String var = entry.getKey();

                upperBounds.add(var + " < " + entry.getValue().toString());
                lowerBounds.add(var + " > " + context._hmMaxVal.get(var));
            }

            return optimiser.run(expression.toString(), constraintsMap.values(), lowerBounds, upperBounds);
        } else {
            XADD.XADDINode iNode = (XADD.XADDINode) rootNode;

            int iNodeVar = iNode._var;
            String iNodeDecisionStr = iNode.getDecision().toString();

            int iNodeLow = iNode._low;
            int iNodeHigh = iNode._high;

            if (context._alOrder.get(iNodeVar) instanceof XADD.ExprDec) {

                // Comparison Expression Decision
//                XADD.ExprDec exprDec = (XADD.ExprDec) context._alOrder.get(iNodeVar);

                // Low branch
                constraintsMap.put(iNodeVar, "-1 * " + iNodeDecisionStr);
                lowM = Optimise.optimisePaths(context, iNodeLow, constraintsMap, optimiser);
                constraintsMap.remove(iNodeVar);

                // High branch
                constraintsMap.put(iNodeVar, iNodeDecisionStr);
                highM = Optimise.optimisePaths(context, iNodeHigh, constraintsMap, optimiser);
                constraintsMap.remove(iNodeVar);

            } else {
                // TODO: Raise Exception?
            }

        }

        return Math.max(lowM, highM);
    }

    public static void main(String[] args) {
        XADD xadd_context = new XADD();

        int ixadd = xadd_context.buildCanonicalXADDFromFile("./src/xadd/optimization/test6.xadd");
        Graph g1 = xadd_context.getGraph(ixadd);
        g1.launchViewer();

        HashSet<String> objective = new HashSet<String>();
        HashMap<Integer, String> constraintsMap = new HashMap<Integer, String>();
        HashSet<String> constraints = new HashSet<String>();
        HashSet<String> bounds = new HashSet<String>();

        double optimalValue = Optimise.optimisePaths(xadd_context, ixadd, null, new MATLABNonLinear());
    }
}
