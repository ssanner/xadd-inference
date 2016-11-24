package xadd.optimization;

import xadd.ExprLib;
import xadd.XADD;

import java.util.*;

public class Optimise {

    private static HashSet<IOptimisationTechnique> optimisers;

    /**
     *
     * @param optimiser
     */
    public static void RegisterOptimisationMethod(IOptimisationTechnique optimiser) {

        if(Optimise.optimisers == null) {
            Optimise.optimisers = new HashSet<IOptimisationTechnique>();
        }

        Optimise.optimisers.add(optimiser);
    }

    /**
     *
     * @param objective
     * @param constraints
     * @param lowerBounds
     * @param upperBounds
     * @return
     */
//    private static HashMap<IOptimisationTechnique, Double> RunOptimisationMethod(String objective, Set<String> variables,
//                                             Collection<String> constraints, Collection<String> lowerBounds,
//                                                                                 Collection<String> upperBounds) {
//
//        HashMap<IOptimisationTechnique, Double> resultsMap = new HashMap<IOptimisationTechnique, Double>();
//
//        double optimalValue;
//        for(IOptimisationTechnique optimiser : Optimise.optimisers) {
//
//            optimalValue = optimiser.run(objective, variables, constraints, lowerBounds, upperBounds);
//            resultsMap.put(optimiser, optimalValue);
//
//            System.out.println("optimalValue: " + optimalValue);
//        }
//
//        return resultsMap;
//    }

    /**
     *
     * @param objective
     * @param constraints
     * @param lowerBounds
     * @param upperBounds
     * @return
     */
    private static Double RunOptimisationMethod(String objective, Set<String> variables,
                                                 Collection<String> constraints, Collection<String> lowerBounds,
                                                                                 Collection<String> upperBounds) {

        Double optimalValue = null;
        for(IOptimisationTechnique optimiser : Optimise.optimisers) {

            optimalValue = optimiser.run(objective, variables, constraints, lowerBounds, upperBounds);
            System.out.println("optimalValue: " + optimalValue);
        }

        return optimalValue;
    }

    /**
     *
     * @param context
     * @param xaddID
     * @param varSet
     * @param constraintsMap
     * @return
     */
    public static double optimisePaths(XADD context, Integer xaddID, HashSet<String> varSet,
                                       HashMap<Integer, String> constraintsMap) {

        double lowM = XADD.DEFAULT_LOWER_BOUND;;
        double highM = XADD.DEFAULT_LOWER_BOUND;;

        XADD.XADDNode rootNode = context.getExistNode(xaddID);
        if(constraintsMap == null) {
            constraintsMap = new HashMap<Integer, String>();
        }

        if(varSet == null) {
            varSet = new HashSet<String>();
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
//            HashMap<IOptimisationTechnique, Double> resultsMap;

            // Convert the lower and upper bounds into "canonical" form i.e. c(x) > 0
            for(Map.Entry<String, Double> entry : context._hmMinVal.entrySet()) {
                String var = entry.getKey();

                lowerBounds.add(var + " - " + " (" + entry.getValue().toString() + ")");
                upperBounds.add("-1 * (" + var + " - " + context._hmMaxVal.get(var) + ")");
            }

            HashSet<String> vars = new HashSet<String>();
            expression.collectVars(vars);
            expression.collectVars(varSet);

//            resultsMap = Optimise.RunOptimisationMethod(expression.toString(), varSet, constraintsMap.values(),
//                    lowerBounds, upperBounds);

            return Optimise.RunOptimisationMethod(expression.toString(), varSet, constraintsMap.values(),
                    lowerBounds, upperBounds);
        } else {
            XADD.XADDINode iNode = (XADD.XADDINode) rootNode;

            int iNodeVar = iNode._var;
            XADD.Decision iNodeDecision = iNode.getDecision();
            ExprLib.CompExpr compExpr = ((XADD.ExprDec) iNodeDecision)._expr;
            String iNodeDecisionStr = null;

            // Convert the comparison expression in the iNodeDecision into canonical form i.e. c(x) > 0
            ExprLib.Expr tmp;
            if(compExpr.isGreater()) {
                tmp = compExpr.makeCanonical();
                iNodeDecisionStr = ((ExprLib.CompExpr) tmp)._lhs.toString();
            }

            compExpr.collectVars(varSet);

            int iNodeLow = iNode._low;
            int iNodeHigh = iNode._high;

            if (context._alOrder.get(iNodeVar) instanceof XADD.ExprDec) {

                // Low branch
                constraintsMap.put(iNodeVar, "-1 * " + iNodeDecisionStr);
                lowM = Optimise.optimisePaths(context, iNodeLow, varSet, constraintsMap);
                constraintsMap.remove(iNodeVar);

                // High branch
                constraintsMap.put(iNodeVar, iNodeDecisionStr);
                highM = Optimise.optimisePaths(context, iNodeHigh, varSet, constraintsMap);
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

        // Register the MATLABNonLinear optimiser with the class
        Optimise.RegisterOptimisationMethod(new MATLABNonLinear());
        double optimalValue = Optimise.optimisePaths(xadd_context, ixadd, null, null);
    }
}
