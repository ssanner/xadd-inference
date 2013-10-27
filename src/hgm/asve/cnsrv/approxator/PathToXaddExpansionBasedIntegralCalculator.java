package hgm.asve.cnsrv.approxator;

import xadd.ExprLib;
import xadd.XADD;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 14/10/13
 * Time: 10:44 PM
 */
//TODO the use of this class is not justified except for testing and even for that the IMPORTANT todo of the main method should be addressed
// This class should be used just for test
public class PathToXaddExpansionBasedIntegralCalculator implements PathIntegralOnLeafFunctionCalculator {
    private XADD context;

    public PathToXaddExpansionBasedIntegralCalculator(XADD context) {
        this.context = context;
    }

    /**
     * @param node Input xadd node id
     * @return a map from each complete path in the input xadd to an xadd jin which all leaves are zero except the
     *         leaves attached to that complete path.
     *         Note that paths are stored from root to leaf.
     */
    public Map<List<XADD.XADDNode>, XADD.XADDNode> calculatePathToXaddMapping(XADD.XADDNode node, LeafFunction leafFunction) {

        if (node instanceof XADD.XADDTNode) {
            Map<List<XADD.XADDNode>, XADD.XADDNode> pathXaddMap = new HashMap<List<XADD.XADDNode>, XADD.XADDNode>(1);

            List<XADD.XADDNode> path = Arrays.asList(node);
            /*if (truncateLeafToOne) {
                pathXadd.put(path, context.getExistNode(context.ONE));
            } else {
                pathXadd.put(path, node);
            }
*/
            //todo: IMPORTANT! This method only works for leaf functions that do not depend on the ancestors of the leaf!
            pathXaddMap.put(path, leafFunction.func((XADD.XADDTNode) node, null/*new ArrayList<XADD.XADDINode>()*//*no ancestors*/));

            return pathXaddMap;
        }

        XADD.XADDINode iNode = (XADD.XADDINode) node;
        Map<List<XADD.XADDNode>, XADD.XADDNode> highMap = calculatePathToXaddMapping(context.getExistNode(iNode._high), leafFunction);
        Map<List<XADD.XADDNode>, XADD.XADDNode> lowMap = calculatePathToXaddMapping(context.getExistNode(iNode._low), leafFunction);


        Map<List<XADD.XADDNode>, XADD.XADDNode> map = new HashMap<List<XADD.XADDNode>, XADD.XADDNode>(highMap.size() + lowMap.size());

        //for high:
        for (Map.Entry<List<XADD.XADDNode>, XADD.XADDNode> highEntry : highMap.entrySet()) {
            List<XADD.XADDNode> path = highEntry.getKey();
            XADD.XADDNode highMultiplexerNode = highEntry.getValue();

            // path should be from root to leaf:
            List<XADD.XADDNode> newPath = new ArrayList<XADD.XADDNode>(path.size() + 1);
            newPath.add(iNode);
            newPath.addAll(path);
//            path.add(0, iNode); //todo check that this does not produce a bug
            XADD.XADDNode newMultiplexerXadd = context.getExistNode(context.getINode(iNode._var, context.ZERO, context._hmNode2Int.get(highMultiplexerNode)));
            map.put(newPath, newMultiplexerXadd);
        }

        //for low:
        for (Map.Entry<List<XADD.XADDNode>, XADD.XADDNode> lowEntry : lowMap.entrySet()) {
            List<XADD.XADDNode> path = lowEntry.getKey();
            XADD.XADDNode lowMultiplexerNode = lowEntry.getValue();

            List<XADD.XADDNode> newPath = new ArrayList<XADD.XADDNode>(path.size() + 1);
            newPath.add(iNode);
            newPath.addAll(path);
//            path.add(0, iNode); //todo same concern...
            XADD.XADDNode newMultiplexerXadd = context.getExistNode(context.getINode(iNode._var, context._hmNode2Int.get(lowMultiplexerNode), context.ZERO));
            map.put(newPath, newMultiplexerXadd);
        }

//        for (XADD.XADDNode xaddNode : map.values()) {
//            context.getGraph(context._hmNode2Int.get(xaddNode)).launchViewer();
//        }


        return map;
    }


    @Override
    public Map<List<XADD.XADDNode>, Double> calculatePathValueMap(XADD.XADDNode rootNode, LeafFunction leafFunction) {
        Map<List<XADD.XADDNode>, XADD.XADDNode> pathToXaddExpansion = calculatePathToXaddMapping(rootNode, leafFunction);
        return calculatePathMassMapFromPathExpansion(pathToXaddExpansion);
    }

    private Map<List<XADD.XADDNode>, Double> calculatePathMassMapFromPathExpansion(Map<List<XADD.XADDNode>, XADD.XADDNode> pathMultiplexer) {
        Map<List<XADD.XADDNode>, Double> map = new HashMap<List<XADD.XADDNode>, Double>(pathMultiplexer.size());
        for (List<XADD.XADDNode> path : pathMultiplexer.keySet()) {
            XADD.XADDNode node = pathMultiplexer.get(path);
            int nodeId = context._hmNode2Int.get(node);

            HashSet<String> vars = node.collectVars();

            int massNodeId = nodeId; // in case their is no variable in the node, it is its own mass

            for (String var : vars) {
                //todo: this only works for continuous vars...
                massNodeId = context.computeDefiniteIntegral(massNodeId, var);
            }

            XADD.XADDTNode mass = (XADD.XADDTNode) context.getExistNode(massNodeId);
            map.put(path, ((ExprLib.DoubleExpr) mass._expr)._dConstVal);
        }

        return map;
    }
}
