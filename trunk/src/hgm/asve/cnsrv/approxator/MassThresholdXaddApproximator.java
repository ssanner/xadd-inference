package hgm.asve.cnsrv.approxator;

import xadd.ExprLib;
import xadd.XADD;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 9/10/13
 * Time: 12:00 PM
 */

public class MassThresholdXaddApproximator implements Approximator{
    private XADD context = null;
    private Map<List<XADD.XADDNode>, Double> pathMassMap;
    private Map<List<XADD.XADDNode>, Double> pathVolumeMap;
//    private int rootXaddId;

    private PathIntegralOnLeafFunctionCalculator pathValueCalculator;

    private double massThreshold;
    private double volumeThreshold;

    public MassThresholdXaddApproximator(PathIntegralOnLeafFunctionCalculator pathValueCalculator,
                                         double massThreshold, double volumeThreshold) {
        this.pathValueCalculator = pathValueCalculator;
        this.massThreshold = massThreshold;
        this.volumeThreshold = volumeThreshold;
    }

    public MassThresholdXaddApproximator(XADD context,
                                         PathIntegralOnLeafFunctionCalculator pathValueCalculator,
                                         double massThreshold, double volumeThreshold) {

        this(pathValueCalculator, massThreshold, volumeThreshold);

        setupWithContext(context);
    }

    @Override
    public void setupWithContext(XADD context) {
        this.context = context;
    }

    @Override
    public XADD.XADDNode approximateXadd(XADD.XADDNode rootXadd) {
//        XADD.XADDNode rootXadd = context.getExistNode(rootXaddId);
        pathMassMap = pathValueCalculator.calculatePathValueMap(rootXadd, context, LeafFunction.identityFunction());
        pathVolumeMap = pathValueCalculator.calculatePathValueMap(rootXadd, context, LeafFunction.oneFunction(context));

        return context._hmInt2Node.get(approximateXADD(context._hmNode2Int.get(rootXadd)));
    }

    //todo use nodes rather than ids everywhere...
    private int approximateXADD(int rootId/*double massThreshold, double volumeThreshold*/) {
//        int rootId = this.rootXaddId;

        while (context.getNode(rootId) instanceof XADD.XADDINode) {
            System.out.println("---*---");
            int newRootId = mergeNodes(rootId, massThreshold, volumeThreshold);
            if (newRootId == rootId) break;
            rootId = newRootId;
        }

        return rootId;
    }

    class CompletePath {
        List<XADD.XADDNode> path;
        double volume;

        CompletePath(List<XADD.XADDNode> path, double volume) {
            this.path = path;
            this.volume = volume;
        }
    }

    /**
     * @param rootXaddNodeId  input xadd node id
     * @param massThreshold   mass threshold
     * @param volumeThreshold volume threshold
     * @return node id of the xadd in which paths with mass and volume less than specified thresholds are merged.
     */
    public int mergeNodes(int rootXaddNodeId, double massThreshold, double volumeThreshold) {

        Set<CompletePath> chosenCompletePathsToBeMerged = new HashSet<CompletePath>();

        Double totalMass = 0d;
        Double totalVolume = 0d;
        for (List<XADD.XADDNode> path : pathMassMap.keySet()) {
            Double mass = pathMassMap.get(path);
            Double volume = pathVolumeMap.get(path);

            if (mass > 0 && mass <= massThreshold && volume <= volumeThreshold) {
                chosenCompletePathsToBeMerged.add(new CompletePath(path, volume));

                totalMass += mass;
                totalVolume += volume;
            }
        }

        if (chosenCompletePathsToBeMerged.size() < 2)
            return rootXaddNodeId; //nothing is merged.

        double averageMass = (totalVolume <= 0) ? 0 : (totalMass / totalVolume);
//            if (DEBUG_XADD_PATH_MASS) System.out.println("averageMass = " + averageMass);

        int returnedXaddNodeId;
        int substitutingTermNodeId = context.getTermNode(new ExprLib.DoubleExpr(averageMass));

        List<XADD.XADDNode> completePathFromRoot = new ArrayList<XADD.XADDNode>();
        completePathFromRoot.add(context.getExistNode(rootXaddNodeId)); // the root itself
        returnedXaddNodeId = context._hmNode2Int.get(
                leafSubstituteXADD(completePathFromRoot,
                        chosenCompletePathsToBeMerged,
                        (XADD.XADDTNode) context.getExistNode(substitutingTermNodeId)));

        return returnedXaddNodeId;
    }


    /**
     * @param pathFromRootToThisXADD The XADD node and its ancestors
     * @param completePathsToLeavesToBeSubstituted
     *                               list of terminals that should be replaced
     * @param replacingTerminal      replacing terminal
     * @return id of the resulting xadd, NULL if constraints (i.e. the conjunction of path decisions) is not satisfiable.
     */
    public XADD.XADDNode leafSubstituteXADD(List<XADD.XADDNode> pathFromRootToThisXADD, //including this XADD
                                            Set<CompletePath> completePathsToLeavesToBeSubstituted,
                                            XADD.XADDTNode replacingTerminal) {
        XADD.XADDNode modifiedXADD;

        XADD.XADDNode thisXADD = pathFromRootToThisXADD.get(pathFromRootToThisXADD.size() - 1);

        if (thisXADD instanceof XADD.XADDTNode) {
            for (CompletePath completePath : completePathsToLeavesToBeSubstituted) {
                if (completePath.path.equals(pathFromRootToThisXADD)) {//(completePath.getLeaf().equals(thisXADD) && completePath.getPathInternalNodes().equals(pathFromRootToThisXADD)) {
                    return completePath.volume <= 0 ? null : replacingTerminal; //non-positive path volume means the path is not satisfiable
                }
            }

            return thisXADD;
        }


        XADD.XADDINode iThisXADD = (XADD.XADDINode) thisXADD;

        //path from root to the children of the current node, contains the current node itself:
//        pathFromRootToThisXADD.add(iThisXADD);


        pathFromRootToThisXADD.add(context.getExistNode(iThisXADD._low));
        XADD.XADDNode low = leafSubstituteXADD(pathFromRootToThisXADD,
                completePathsToLeavesToBeSubstituted, replacingTerminal);
        pathFromRootToThisXADD.remove(pathFromRootToThisXADD.size() - 1);

        pathFromRootToThisXADD.add(context.getExistNode(iThisXADD._high));
        XADD.XADDNode high = leafSubstituteXADD(pathFromRootToThisXADD,
                completePathsToLeavesToBeSubstituted, replacingTerminal);
        pathFromRootToThisXADD.remove(pathFromRootToThisXADD.size() - 1);

        //if a child is impossible, return the other child (omit the current "decision")
        if (low == null) return high;
        if (high == null) return low;

        //If low and high are the same:
        if (low.equals(high)) return high;


        //TODO: I wonder what is the use of ids for xadd nodes (and expressions).
        //TODO: instead, I think we can always pass the XADDNodes to methods (rather than their ids)
        //TODO: For objects, java is "call by reference" i.e. pointers of objects are passed to methods which is not so different from using integers.
        //TODO: The benefits will be:
        // 1. we do not need to use _hmNode2Int and _hmInt2Node maps (so things become faster), garbage collection can be done automatically...
        // 2. due to type checking, ids of nodes and expressions and other integer values cannot be used interchangeably so it is safer
        // 3. directly working with objects is more readable, mush easier to trace and debug.
        modifiedXADD = context.getExistNode(
                context.getINode(iThisXADD._var /*i.e. expression of the node*/,
                        context._hmNode2Int.get(low), context._hmNode2Int.get(high)));

        return modifiedXADD;
    }
}