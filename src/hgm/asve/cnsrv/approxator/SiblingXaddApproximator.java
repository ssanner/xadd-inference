package hgm.asve.cnsrv.approxator;

import xadd.XADD;
import xadd.XADDUtils;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 16/10/13
 * Time: 6:01 AM
 */
public class SiblingXaddApproximator implements Approximator{
    private XADD context;
    private PathIntegralOnLeafFunctionCalculator pathValueCalculator;

    int maxDesiredNumberOfNodes;
    double siblingDiffThreshold;

    public SiblingXaddApproximator(PathIntegralOnLeafFunctionCalculator pathValueCalculator,
                                   int maxDesiredNumberOfNodes, double siblingDiffThreshold) {
        this.pathValueCalculator = pathValueCalculator;

        this.maxDesiredNumberOfNodes = maxDesiredNumberOfNodes;
        this.siblingDiffThreshold = siblingDiffThreshold;
    }

    public SiblingXaddApproximator(XADD context, PathIntegralOnLeafFunctionCalculator pathValueCalculator,
                                   int maxDesiredNumberOfNodes, double siblingDiffThreshold) {

        this (pathValueCalculator, maxDesiredNumberOfNodes, siblingDiffThreshold);
        setupWithContext(context);
    }

    @Override
    public void setupWithContext(XADD context) {
        this.context = context;
    }

    @Override
    public XADD.XADDNode approximateXadd(XADD.XADDNode rootXadd/*, int maxDesiredNumberOfNodes, double siblingDiffThreshold*/) {
        XADD.XADDNode node = rootXadd;

        int i = 0;
        for (; ; ) {
            if (context.getNodeCount(context._hmNode2Int.get(node)) <= maxDesiredNumberOfNodes) break;
            XADD.XADDNode approxNode = approximationIteration(node, siblingDiffThreshold);
            if (approxNode == node) break;

            node = approxNode;

            System.out.println("approximation iteration: " + i++);
//            context.getGraph(context._hmNode2Int.get(approxNode)).launchViewer();
//            XADDUtils.PlotXADD(context, context._hmNode2Int.get(approxNode), -2, 0.01, 15, "x", "sibling approximator");
        }

        return node;
    }

    //Important note: approximation is based on the paths from root not on the nodes
    public XADD.XADDNode approximationIteration(XADD.XADDNode rootXadd, double siblingDiffThreshold) {
        //todo 1: since this map is used in each iteration, its results should be cached
        //todo 2: check that values are always positive. If not figure out why. If there is no solution figure out if abs() should be used.
        Map<List<XADD.XADDNode>, Double> pathValueMap =
                pathValueCalculator.calculatePathValueMap(rootXadd, context, LeafFunction.squaredErrorWithSibling(context));

        List<Map.Entry<List<XADD.XADDNode>, Double>> sortedPathValues =
                new ArrayList<Map.Entry<List<XADD.XADDNode>, Double>>(pathValueMap.entrySet());
        if (sortedPathValues.isEmpty()) return rootXadd; // no approximation possible.

        // sorting is necessary so that the sibling that its omission produces less loss is processed first.
        Collections.sort(sortedPathValues, new Comparator<Map.Entry<List<XADD.XADDNode>, Double>>() {
            @Override
            public int compare(Map.Entry<List<XADD.XADDNode>, Double> e1, Map.Entry<List<XADD.XADDNode>, Double> e2) {
                if (e1.getValue() < e2.getValue()) return -1;
                if (e1.getValue() > e2.getValue()) return 1;
                return 0;
            }
        });

        //throw away paths with values more than threshold:
        for (int i = 0; i < sortedPathValues.size(); i++) {
            Map.Entry<List<XADD.XADDNode>, Double> pathValue = sortedPathValues.get(i);
            if (pathValue.getValue() > siblingDiffThreshold) {
                sortedPathValues = sortedPathValues.subList(0, i);
                break;
            }
        }

        if (sortedPathValues.isEmpty()) return rootXadd;

        //throw aways paths attached to siblings whom the other sibling is also chosen with less score:
        List<List<XADD.XADDNode>> listOfCompletePathsToBeApproximated = new ArrayList<List<XADD.XADDNode>>();
        Set<List<XADD.XADDNode>> setOfParentsOfPathsToBeApproximated = new HashSet<List<XADD.XADDNode>>(); //parent of a path has one node less in it
        for (Map.Entry<List<XADD.XADDNode>, Double> pathValue : sortedPathValues) {
            List<XADD.XADDNode> path = pathValue.getKey();
            List<XADD.XADDNode> pathParent = path.subList(0, path.size() - 1);
            if (setOfParentsOfPathsToBeApproximated.contains(pathParent)) continue; //the other sibling has less score.
            setOfParentsOfPathsToBeApproximated.add(pathParent);
            listOfCompletePathsToBeApproximated.add(path);
        }

        List<XADD.XADDNode> completePath = new ArrayList<XADD.XADDNode>();
        completePath.add(rootXadd);
        return leafSubstituteWithSibling(completePath, listOfCompletePathsToBeApproximated);


    }

    protected XADD.XADDNode leafSubstituteWithSibling(List<XADD.XADDNode> pathFromRootToThisXADD, //inclusive
                                                      Collection<List<XADD.XADDNode>> completePathsToLeavesToBeMergedWithTheirSiblings) {
        XADD.XADDNode modifiedXADD;

        XADD.XADDNode thisXADD = pathFromRootToThisXADD.get(pathFromRootToThisXADD.size() - 1);

        if (thisXADD instanceof XADD.XADDTNode) {
            if (completePathsToLeavesToBeMergedWithTheirSiblings.contains(pathFromRootToThisXADD))
                return sibling(pathFromRootToThisXADD);
            else
                return thisXADD;
        }

        XADD.XADDINode iThisXADD = (XADD.XADDINode) thisXADD;

        pathFromRootToThisXADD.add(context.getExistNode(iThisXADD._low));
        XADD.XADDNode low = leafSubstituteWithSibling(pathFromRootToThisXADD, completePathsToLeavesToBeMergedWithTheirSiblings);
        pathFromRootToThisXADD.remove(pathFromRootToThisXADD.size() - 1);

        pathFromRootToThisXADD.add(context.getExistNode(iThisXADD._high));
        XADD.XADDNode high = leafSubstituteWithSibling(pathFromRootToThisXADD, completePathsToLeavesToBeMergedWithTheirSiblings);
        pathFromRootToThisXADD.remove(pathFromRootToThisXADD.size() - 1);

        //If low and high are the same:
        if (low.equals(high)) return high;

        modifiedXADD = context.getExistNode(
                context.getINode(iThisXADD._var, context._hmNode2Int.get(low), context._hmNode2Int.get(high)));

        return modifiedXADD;
    }

    private XADD.XADDNode sibling(List<XADD.XADDNode> completePathToNode) {
        XADD.XADDNode node = completePathToNode.get(completePathToNode.size() - 1);
        XADD.XADDINode parent = (XADD.XADDINode) completePathToNode.get(completePathToNode.size() - 2);
        Integer nodeId = context._hmNode2Int.get(node);

        Integer siblingId = (parent._high == nodeId) ? parent._low : parent._high;
        return context._hmInt2Node.get(siblingId);

    }
}
