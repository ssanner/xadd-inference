package hgm.asve.cnsrv.approxator;

import xadd.ExprLib;
import xadd.XADD;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 9/10/13
 * Time: 12:00 PM
 */

public class RobustMassThresholdXaddApproximator implements Approximator {
    private XADD context = null;
    private Map<List<XADD.XADDNode>, Double> pathMassMap;
    private Map<List<XADD.XADDNode>, Double> pathVolumeMap;

    private PathIntegralOnLeafFunctionCalculator pathValueCalculator;

    private int desiredNumberOfLeaves;

    public RobustMassThresholdXaddApproximator(PathIntegralOnLeafFunctionCalculator pathValueCalculator,
                                               int desiredNumberOfLeaves) {
        this.pathValueCalculator = pathValueCalculator;
        this.desiredNumberOfLeaves = desiredNumberOfLeaves;
    }

    public RobustMassThresholdXaddApproximator(XADD context,
                                               PathIntegralOnLeafFunctionCalculator pathValueCalculator,
                                               int desiredNumberOfLeaves) {

        this(pathValueCalculator, desiredNumberOfLeaves);

        setupWithContext(context);
    }

    @Override
    public void setupWithContext(XADD context) {
        this.context = context;
    }

    @Override
    public XADD.XADDNode approximateXadd(XADD.XADDNode rootXadd) {
        pathMassMap = pathValueCalculator.calculatePathValueMap(rootXadd, context, LeafFunction.identityFunction());
        pathVolumeMap = pathValueCalculator.calculatePathValueMap(rootXadd, context, LeafFunction.oneFunction(context));

//        return context._hmInt2Node.get(approximateXADD(context._hmNode2Int.get(rootXadd)));
        return mergeNodes(rootXadd, desiredNumberOfLeaves);
    }

    class CompletePath {
        List<XADD.XADDNode> path;
        double volume;

        CompletePath(List<XADD.XADDNode> path, double volume) {
            this.path = path;
            this.volume = volume;
        }
    }

    public XADD.XADDNode mergeNodes(XADD.XADDNode rootXaddNode, int desiredNumberOfLeaves) {

        List<List<XADD.XADDNode>> sortedPaths = new ArrayList<List<XADD.XADDNode>>(pathMassMap.keySet());
        Collections.sort(sortedPaths, new Comparator<List<XADD.XADDNode>>() {
            @Override
            public int compare(List<XADD.XADDNode> p1, List<XADD.XADDNode> p2) {
                Double mass1 = pathMassMap.get(p1);
                Double mass2 = pathMassMap.get(p2);

                if (mass1 <= 0) mass1 = Double.MAX_VALUE; // 0 regions should not be merged
                if (mass2 <= 0) mass2 = Double.MAX_VALUE; // 0 regions should not be merged

                if (mass1 < mass2) return -1;
                if (mass1 > mass2) return 1;
                return 0;
            }
        });

        Set<CompletePath> chosenCompletePathsToBeMerged = new HashSet<CompletePath>();

//        int numberOfNonMassivePaths = Math.max(0, sortedPaths.size() - desiredNumberOfLeaves);
//        if (numberOfNonMassivePaths < 2 ) return rootXaddNode;

        System.out.println("sortedPaths size = " + sortedPaths.size());
//        System.out.println("numberOfNonMassivePaths = " + numberOfNonMassivePaths);

        //for test
        for (int i = 0; i < sortedPaths.size(); i++) {
            List<XADD.XADDNode> path = sortedPaths.get(i);
            Double m = pathMassMap.get(path);
            System.out.println("m = " + m);
        }


        Set<XADD.XADDTNode> encounteredLeaves = new HashSet<XADD.XADDTNode>();
        double totalMass = 0;
        double totalVolume = 0;
        for (int i = 0; i < sortedPaths.size(); i++) {
            List<XADD.XADDNode> nonMassivePath = sortedPaths.get(i);
            Double mass = pathMassMap.get(nonMassivePath);
            Double volume = pathVolumeMap.get(nonMassivePath);

            if (mass <= 0) break;
            if (volume > Integer.MAX_VALUE) break;

            totalMass += mass;
            System.out.println("mass = " + mass);
            totalVolume += volume;
            System.out.println("volume = " + volume);
            chosenCompletePathsToBeMerged.add(new CompletePath(nonMassivePath, volume));
            encounteredLeaves.add((XADD.XADDTNode) nonMassivePath.get(nonMassivePath.size() - 1));
            if (encounteredLeaves.size() >= desiredNumberOfLeaves) break;
            System.out.println("encounteredLeaves.size = " + encounteredLeaves.size());
        }

        if (encounteredLeaves.size() < desiredNumberOfLeaves) return rootXaddNode;


/*
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
            return rootXaddNode; //nothing is merged.
*/

        double averageMass = (totalVolume <= 0) ? 0 : (totalMass / totalVolume);
//            if (DEBUG_XADD_PATH_MASS) System.out.println("averageMass = " + averageMass);

        XADD.XADDNode returnedXaddNode;
        XADD.XADDTNode substitutingTermNode = (XADD.XADDTNode) context.getExistNode(context.getTermNode(new ExprLib.DoubleExpr(averageMass)));

        List<XADD.XADDNode> completePathFromRoot = new ArrayList<XADD.XADDNode>();
        completePathFromRoot.add(rootXaddNode); // the root itself
        returnedXaddNode = leafSubstituteXADD(completePathFromRoot,
                chosenCompletePathsToBeMerged,
                substitutingTermNode);

        return returnedXaddNode;
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