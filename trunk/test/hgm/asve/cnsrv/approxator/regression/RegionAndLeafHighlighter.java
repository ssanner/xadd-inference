package hgm.asve.cnsrv.approxator.regression;

import hgm.utils.vis.XaddVisualizer;
import hgm.asve.XaddPath;
import hgm.asve.cnsrv.factor.Factor;
import xadd.XADD;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Hadi Afshar.
 * Date: 15/11/13
 * Time: 10:56 AM
 */

/**
 * Used for visualization of regions
 */
public class RegionAndLeafHighlighter {
    private XADD.XADDTNode regionCounterNode;
    private XADD context;

    public RegionAndLeafHighlighter(XADD context) {
        this.context = context;
    }

    public XADD.XADDNode getRegionXadd(XADD.XADDNode node) {
        regionCounterNode = (XADD.XADDTNode) context.getExistNode(context.ZERO); // reset counter
        return leafSubstituteToHighlightEachRegion(new XaddPath(Arrays.asList(node), context), false, new HashMap<XADD.XADDTNode, XADD.XADDTNode>());
    }

    public XADD.XADDNode getLeafXadd(XADD.XADDNode node) {
        regionCounterNode = (XADD.XADDTNode) context.getExistNode(context.ZERO); // reset counter
        return leafSubstituteToHighlightEachRegion(new XaddPath(Arrays.asList(node), context), true, new HashMap<XADD.XADDTNode, XADD.XADDTNode>());
    }

    private XADD.XADDNode leafSubstituteToHighlightEachRegion(
            XaddPath pathFromRootToThisXADD,
            boolean countLeavesRatherThanRegions,
            Map<XADD.XADDTNode, XADD.XADDTNode> visitedLeavesToNumberNodeMap) {
        XADD.XADDNode modifiedXADD;


        if (pathFromRootToThisXADD.isComplete()) {  //this node is a leaf, the path represents a region
            if (countLeavesRatherThanRegions) {
                XADD.XADDTNode leaf = pathFromRootToThisXADD.getLeaf();
                XADD.XADDTNode leafNumber = visitedLeavesToNumberNodeMap.get(leaf);
                if (leafNumber == null) {
                    regionCounterNode = increase(regionCounterNode);
                    visitedLeavesToNumberNodeMap.put(leaf, regionCounterNode);
                    return regionCounterNode;
                } else {
                    return leafNumber;
                }

            } else {
                regionCounterNode = increase(regionCounterNode);
                return regionCounterNode;
            }
        }

        XADD.XADDINode iThisXADD = (XADD.XADDINode) pathFromRootToThisXADD.getLastNode();

        pathFromRootToThisXADD.add(context.getExistNode(iThisXADD._low));
        XADD.XADDNode low = leafSubstituteToHighlightEachRegion(pathFromRootToThisXADD, countLeavesRatherThanRegions, visitedLeavesToNumberNodeMap);
        pathFromRootToThisXADD.remove(pathFromRootToThisXADD.size() - 1);

        pathFromRootToThisXADD.add(context.getExistNode(iThisXADD._high));
        XADD.XADDNode high = leafSubstituteToHighlightEachRegion(pathFromRootToThisXADD, countLeavesRatherThanRegions, visitedLeavesToNumberNodeMap);
        pathFromRootToThisXADD.remove(pathFromRootToThisXADD.size() - 1);

        modifiedXADD = context.getExistNode(
                context.getINode(iThisXADD._var, context._hmNode2Int.get(low), context._hmNode2Int.get(high)));
        return modifiedXADD;
    }

    private XADD.XADDTNode increase(XADD.XADDTNode t) {
        return (XADD.XADDTNode) context.getExistNode(context.applyInt(context._hmNode2Int.get(t), context.ONE, XADD.SUM));
    }

    public static void visualizeRegions(Factor factor, String title) {
        //showing leafs:
        XADD context = factor.getContext();
        RegionAndLeafHighlighter leafShow = new RegionAndLeafHighlighter(context);
        XADD.XADDNode regionsXadd = leafShow.getRegionXadd(factor.getNode());
        XaddVisualizer.visualize(regionsXadd, title, context);
    }

    public static void visualizeLeaves(Factor factor, String title) {
        //showing leafs:
        XADD context = factor.getContext();
        RegionAndLeafHighlighter leafShow = new RegionAndLeafHighlighter(context);
        XADD.XADDNode leafHighlighterXadd = leafShow.getLeafXadd(factor.getNode());
        XaddVisualizer.visualize(leafHighlighterXadd, title, context);
    }
}
