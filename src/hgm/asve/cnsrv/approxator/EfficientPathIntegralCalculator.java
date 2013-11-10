package hgm.asve.cnsrv.approxator;

import hgm.Configurations;
import xadd.ExprLib;
import xadd.XADD;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 14/10/13
 * Time: 4:10 AM
 */

public class EfficientPathIntegralCalculator implements PathIntegralOnLeafFunctionCalculator {
    private XADD context = null;

//    public EfficientPathIntegralCalculator(XADD context) {
//        this.context = context;
//    }

    @Override
    public Map<List<XADD.XADDNode>, Double> calculatePathValueMap(XADD.XADDNode rootNode, XADD context, LeafFunction leafFunction) {
        this.context = context;
        return processForAllPaths(rootNode, new ArrayList<XADD.XADDINode>(), new ArrayList<Boolean>(), leafFunction);
    }

    private Map<List<XADD.XADDNode>, Double> processForAllPaths(XADD.XADDNode lastNode, /*int id, */
                                                                ArrayList<XADD.XADDINode> pathDecisionNodes, ArrayList<Boolean> pathDecisionValues,
                                                                LeafFunction leafFunction) {

        if (lastNode instanceof XADD.XADDTNode) {
            Double leafMass = marginalizeAllPathVarsOnLeaf(pathDecisionNodes, pathDecisionValues, (XADD.XADDTNode) lastNode, leafFunction);

            //todo this linke is hack HACK hack!!!!!!!
//            leafMass = Math.abs(leafMass);

            Map<List<XADD.XADDNode>, Double> map = new HashMap<List<XADD.XADDNode>, Double>();
            List<XADD.XADDNode> completePath = new ArrayList<XADD.XADDNode>(pathDecisionNodes.size() + 1);
            //note: the original path itself is reusable so cloning is a must. The last node (which is a leaf) is also a part of complete path
            completePath.addAll(pathDecisionNodes);
            completePath.add(lastNode);
            map.put(completePath, leafMass);
            return map;
        }

        XADD.XADDINode iNode = (XADD.XADDINode) lastNode;
//       XADD.Decision d = context._alOrder.get(iNode._var);

        //for children the this node itself should be in the path:
        pathDecisionNodes.add(iNode);

        //for low:
        pathDecisionValues.add(Boolean.FALSE);
        Map<List<XADD.XADDNode>, Double> map = processForAllPaths(context.getExistNode(iNode._low), pathDecisionNodes, pathDecisionValues, leafFunction);

        //for high:
        pathDecisionValues.set(pathDecisionValues.size() - 1, Boolean.TRUE);
        Map<List<XADD.XADDNode>, Double> map2 = processForAllPaths(context.getExistNode(iNode._high), pathDecisionNodes, pathDecisionValues, leafFunction);

        //collect entries of maps of both low and high children:
        map.putAll(map2);
//        System.out.println("---------------------------");
//        System.out.println("map = " + map);
//        System.out.println("---------------------------");

        // last decision nodes and values should be removed from the list before the process ends:
        pathDecisionNodes.remove(pathDecisionNodes.size() - 1);
        pathDecisionValues.remove(pathDecisionValues.size() - 1);

        return map;
    }

    private ArrayList<XADD.Decision> getDecisionsOutOfInnerNodes(ArrayList<XADD.XADDINode> innerNodes) {
        ArrayList<XADD.Decision> decisions = new ArrayList<XADD.Decision>(innerNodes.size());
        for (XADD.XADDINode innerNode : innerNodes) {
            decisions.add(context._alOrder.get(innerNode._var));
        }
        return decisions;
    }

    /**
     * It is assumed that no variable exists in leaf that is not in path. As a consequence, the result is a double
     *
     * @param pathDecisionNodes path inner nodes
     * @param decision_values   path decision values
     * @param leafNode          leaf
     */
    public Double marginalizeAllPathVarsOnLeaf(ArrayList<XADD.XADDINode> pathDecisionNodes,
                                               ArrayList<Boolean> decision_values /*, ExprLib.ArithExpr leaf_val*/,
                                               XADD.XADDTNode leafNode,
                                               LeafFunction leafFunction) {
        ArrayList<XADD.Decision> pathDecisions = getDecisionsOutOfInnerNodes(pathDecisionNodes);

        HashSet<String> pathVars = new HashSet<String>();
        for (XADD.XADDINode decision : pathDecisionNodes) {
            decision.collectVars(pathVars);
        }
        if (Configurations.DEBUG_XADD_PATH_MASS) {
            HashSet<String> leafVars = new HashSet<String>();
            leafNode.collectVars(leafVars); //leaf_val.collectVars(leafVars);
            if (!pathVars.containsAll(leafVars)) {
                throw new RuntimeException("there is a variable in leaf variables: " + leafVars + " not in path vars: " + pathVars);
            }
        }

        // Marginalize out every variable occurred in the path:
//        if (pathVars.isEmpty()) {
//            throw new RuntimeException("no variable in path!");
//        }
//        int leafNodeId = context._hmNode2Int.get(leafNode);

        XADD.XADDNode pathMassXaddNode;

        /*if (substituteLeafWithONE) {
            pathMassXaddNode = computeDefiniteConditionalIntegral(context.getExistNode(context.ONE), pathVars, pathDecisions, decision_values);
        } else {
            pathMassXaddNode = computeDefiniteConditionalIntegral(leafNode, pathVars, pathDecisions, decision_values);
        }
*/

        pathMassXaddNode = computeDefiniteConditionalIntegral(leafFunction.func(leafNode, pathDecisionNodes), pathVars, pathDecisions, decision_values);

        if (!(pathMassXaddNode instanceof XADD.XADDTNode)) {
            throw new RuntimeException(pathMassXaddNode + " where vars: " + pathVars + " are marginalized is not a terminal. I do not know what I should do!!");
        }
        XADD.XADDTNode pathMass = (XADD.XADDTNode) pathMassXaddNode;
        Double mass = ((ExprLib.DoubleExpr) pathMass._expr)._dConstVal;
        return mass;
    }

    private XADD.XADDNode computeDefiniteConditionalIntegral(XADD.XADDNode xaddNode, Collection<String> integrationVars,
                                                             ArrayList<XADD.Decision> extraDecisions, ArrayList<Boolean> extraDecisionValues) {
        XADD.XADDNode result = xaddNode;

        if (integrationVars.size() == 0) return result;

        ConditionalIntegrator integrator;
        boolean itsTheFistIntegrationVariable = true;
        for (String integrationVar : integrationVars) {
            if (itsTheFistIntegrationVariable) { //conditions are taken into account only for the first variable since after that, they will be embedded into the resulting xadd
                itsTheFistIntegrationVariable = false;
                integrator = new ConditionalIntegrator(context, integrationVar, extraDecisions, extraDecisionValues);
            } else {
                integrator = new ConditionalIntegrator(context, integrationVar, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
            }
            reduceProcessXADDLeaf(result, integrator, new ArrayList<XADD.Decision>(), new ArrayList<Boolean>());
            result = integrator.getFinalResultXadd();
        }
        return result;
    }

    private int reduceProcessXADDLeaf(XADD.XADDNode n, ConditionalIntegrator conditionalIntegrator,
                                      ArrayList<XADD.Decision> decisions, ArrayList<Boolean> decision_values) {

        Integer ret;
//        XADD.XADDNode n = context.getExistNode(id);

        if (n instanceof XADD.XADDTNode) {
            return conditionalIntegrator.processXADDLeaf(decisions, decision_values,
                    ((XADD.XADDTNode) n)._expr); // Assuming that to have
            // a node id means
            // canonical
        }

        // If its an internal node, check the reduce cache
//        _tempReduceLeafOpKey.set(id, leaf_op.hashCode());
        /*if (leaf_op.isCacheable() && (ret = _hmReduceLeafOpCache.get(_tempReduceLeafOpKey)) != null) {
            // System.out.println("** In cache, returning: " + getString(ret));
            return ret;
        }
*/
        XADD.XADDINode iNode = (XADD.XADDINode) n;
        XADD.Decision d = context._alOrder.get(iNode._var);

        decisions.add(d);
        decision_values.add(Boolean.FALSE);
        int low = reduceProcessXADDLeaf(context.getExistNode(iNode._low), conditionalIntegrator, decisions,
                decision_values);

        decision_values.set(decision_values.size() - 1, Boolean.TRUE);
        int high = reduceProcessXADDLeaf(context.getExistNode(iNode._high), conditionalIntegrator, decisions,
                decision_values);

        decisions.remove(decisions.size() - 1);
        decision_values.remove(decision_values.size() - 1);

        // Standard Reduce: getInode will handle the case of low == high
        ret = context.getINode(iNode._var, low, high);

        // Put return value in cache and return
//        _hmReduceLeafOpCache.put(new IntPair(id, leaf_op.hashCode()), ret);
        return ret;
    }

    /**
     * Same as XADDLeafDefIntegral but with extra conditions passed to it.
     */
    public class ConditionalIntegrator /*extends XADDLeafOperation*/ {
        List<XADD.Decision> extraDecisions;
        List<Boolean> extraDecisionValues;
        XADD.XADDLeafDefIntegral integrator;

        ConditionalIntegrator(XADD context, String integrationVariable, List<XADD.Decision> extraDecisions, List<Boolean> extraDecisionValues) {
            integrator = context.new XADDLeafDefIntegral(integrationVariable);

            this.extraDecisions = extraDecisions;
            this.extraDecisionValues = extraDecisionValues;
        }

        public int processXADDLeaf(ArrayList<XADD.Decision> decisions, ArrayList<Boolean> decision_values, ExprLib.ArithExpr leaf_val) {
            ArrayList<XADD.Decision> extendedDecisionList = new ArrayList<XADD.Decision>(extraDecisions);
            extendedDecisionList.addAll(decisions);

            ArrayList<Boolean> extendedDecisionValueList = new ArrayList<Boolean>(extraDecisionValues);
            extendedDecisionValueList.addAll(decision_values);

            return integrator.processXADDLeaf(extendedDecisionList, extendedDecisionValueList, leaf_val);
        }

        public XADD.XADDNode getFinalResultXadd() {
            return context.getExistNode(integrator._runningSum);
        }
    }
}