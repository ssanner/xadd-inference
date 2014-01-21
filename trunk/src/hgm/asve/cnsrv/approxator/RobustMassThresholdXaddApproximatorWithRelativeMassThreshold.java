package hgm.asve.cnsrv.approxator;

import xadd.ExprLib;
import xadd.XADD;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 9/10/13
 * Time: 12:00 PM
 */

/**
 * It prunes the regions with mass below a given ratio of the total mass of the given node
 */
public class RobustMassThresholdXaddApproximatorWithRelativeMassThreshold implements Approximator {
    private XADD context = null;
    //todo make these local...
    private Map<List<XADD.XADDNode>, Double> regionMassMap;
    private Map<List<XADD.XADDNode>, Double> regionVolumeMap;

    private PathIntegralOnLeafFunctionCalculator pathValueCalculator;

    /**
     * Regions with (positive) mass below this threshold are merged to a single node corresponding the mass of all of them.
     * This mass is relative (to the total function mass) and makes sense when it is between 0 and 1.
     * Note that the original function should not be necessarily normalized. But mass of each region should be non-negative.
     **/
    private double relativeMassThreshold;

    public RobustMassThresholdXaddApproximatorWithRelativeMassThreshold(PathIntegralOnLeafFunctionCalculator pathValueCalculator,
                                                                        double relativeMassThreshold) {
        this.pathValueCalculator = pathValueCalculator;
        this.relativeMassThreshold = relativeMassThreshold;

        if (relativeMassThreshold < 0.0) throw new RuntimeException("Illegal relative mass threshold: " + relativeMassThreshold);
        if (relativeMassThreshold > 1.0) throw new RuntimeException("Illegal relative mass threshold: " + relativeMassThreshold);
    }

    public RobustMassThresholdXaddApproximatorWithRelativeMassThreshold(XADD context,
                                                                        PathIntegralOnLeafFunctionCalculator pathValueCalculator,
                                                                        double relativeMassThreshold) {

        this(pathValueCalculator, relativeMassThreshold);

        setupWithContext(context);
    }

    @Override
    public void setupWithContext(XADD context) {
        this.context = context;
    }

    @Override
    public XADD.XADDNode approximateXadd(XADD.XADDNode rootXadd) {
        //todo: if it is a bottle neck, then both mass and volume should be calculated simultaneously....
        regionMassMap = pathValueCalculator.calculatePathValueMap(rootXadd, context, LeafFunction.identityFunction());
        regionVolumeMap = pathValueCalculator.calculatePathValueMap(rootXadd, context, LeafFunction.oneFunction(context));

        return mergeNodes(rootXadd, relativeMassThreshold);
    }


    private XADD.XADDNode mergeNodes(XADD.XADDNode rootXaddNode, double relativeMassThreshold) {
        //for normalization:
        double totalRootNodeMass = 0.0d;
        Set<List<XADD.XADDNode>> regions = regionMassMap.keySet();
        for (List<XADD.XADDNode> region : regions) {
            Double regionMass = regionMassMap.get(region);
            if (regionMass < 0.0) throw new RuntimeException("negative mass " + regionMass + " in region " + region);
            if (regionMass>Double.MAX_VALUE) throw new RuntimeException("Mass: " + regionMass + " in region " + region);
            totalRootNodeMass += regionMass;
        }

        double absoluteMassThreshold = relativeMassThreshold * totalRootNodeMass;

        double totalMergedMass = 0.0d;
        double totalMergedVolume = 0.0d;
        Set<List<XADD.XADDNode>> regionsToBeMerged = new HashSet<List<XADD.XADDNode>>();
        Set<List<XADD.XADDNode>> infeasibleRegions = new HashSet<List<XADD.XADDNode>>();

        for (List<XADD.XADDNode> region : regions) {
            Double regionMass = regionMassMap.get(region);
            Double regionVolume = regionVolumeMap.get(region);

            if (regionVolume <= 0.0) {
                //infeasible region:
                infeasibleRegions.add(region);
            } else if (regionMass > 0.0 && regionMass < absoluteMassThreshold) {
                totalMergedMass += regionMass;
                if (regionVolume <= 0.0) throw new RuntimeException("volume of a region with positive mass is not positive!!!!");
                totalMergedVolume += regionVolume;
                regionsToBeMerged.add(region);
            }

            totalRootNodeMass += regionMass;
        }

        if (infeasibleRegions.isEmpty() && regionsToBeMerged.size()<2) return rootXaddNode;


        double averageMassOfRegionsToBeMerged = totalMergedMass / totalMergedVolume;

        XADD.XADDTNode mergeSubstitutingTermNode = (XADD.XADDTNode) context.getExistNode(context.getTermNode(new ExprLib.DoubleExpr(averageMassOfRegionsToBeMerged)));

        List<XADD.XADDNode> completePathFromRoot = new ArrayList<XADD.XADDNode>();
        completePathFromRoot.add(rootXaddNode); // the root itself
        XADD.XADDNode returnedXaddNode;
        returnedXaddNode = leafSubstituteXADD(completePathFromRoot,
                regionsToBeMerged,
                mergeSubstitutingTermNode,
                infeasibleRegions);

        return returnedXaddNode;
    }


    /**
     * @param pathFromRootToThisXADD The XADD node and its ancestors
     * @param regionsToBeMerged
     *                               list of terminals that should be replaced
     * @param mergerTerminal      replacing terminal
     * @return resulting xadd, NULL if constraints (i.e. the conjunction of path decisions) is not satisfiable.
     */
    public XADD.XADDNode leafSubstituteXADD(List<XADD.XADDNode> pathFromRootToThisXADD, //including this XADD
                                            Set<List<XADD.XADDNode>> regionsToBeMerged,
                                            XADD.XADDTNode mergerTerminal,
                                            Set<List<XADD.XADDNode>> infeasibleRegions) {

        XADD.XADDNode thisXADD = pathFromRootToThisXADD.get(pathFromRootToThisXADD.size() - 1);

        if (thisXADD instanceof XADD.XADDTNode) {
            if (infeasibleRegions.contains(pathFromRootToThisXADD)) {
                return null; // NULL means infeasible
            }

            if (regionsToBeMerged.contains(pathFromRootToThisXADD)) {
                return mergerTerminal;
            }

            return thisXADD;
        }

        XADD.XADDINode iThisXADD = (XADD.XADDINode) thisXADD;

        pathFromRootToThisXADD.add(context.getExistNode(iThisXADD._low));
        XADD.XADDNode low = leafSubstituteXADD(pathFromRootToThisXADD,
                regionsToBeMerged, mergerTerminal, infeasibleRegions);
        pathFromRootToThisXADD.remove(pathFromRootToThisXADD.size() - 1);

        pathFromRootToThisXADD.add(context.getExistNode(iThisXADD._high));
        XADD.XADDNode high = leafSubstituteXADD(pathFromRootToThisXADD,
                regionsToBeMerged, mergerTerminal, infeasibleRegions);
        pathFromRootToThisXADD.remove(pathFromRootToThisXADD.size() - 1);

        //if a child is impossible, return the other child (omit the current "decision")
        if (low == null) return high;
        if (high == null) return low;

        //If low and high are the same:
        if (low.equals(high)) return high;

        XADD.XADDNode modifiedXADD;
        modifiedXADD = context.getExistNode(
                context.getINode(iThisXADD._var /*i.e. expression of the node*/,
                        context._hmNode2Int.get(low), context._hmNode2Int.get(high)));

        return modifiedXADD;
    }
}