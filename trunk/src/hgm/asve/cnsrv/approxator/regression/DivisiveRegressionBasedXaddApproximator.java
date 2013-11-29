package hgm.asve.cnsrv.approxator.regression;

import hgm.asve.XaddPath;
import hgm.asve.cnsrv.approxator.regression.measures.DivergenceMeasure;
import xadd.XADD;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Created by Hadi Afshar.
 * Date: 13/11/13
 * Time: 9:15 PM
 */
public class DivisiveRegressionBasedXaddApproximator extends RegressionBasedXaddApproximator {
    public static boolean ZERO_REGIONS_CAN_BE_MERGED_WITH_NONZERO_REGIONS = false;

    protected double maxAcceptableMeanSquaredErrorPerSiblingMerge;
    protected int minimumNumberOfNodesToTriggerApproximation;

    public DivisiveRegressionBasedXaddApproximator(XADD context,
                                                   DivergenceMeasure divergenceMeasure,
                                                   int maxPower,
                                                   int sampleNumPerContinuousVar,
                                                   double regularizationCoefficient,
                                                   double maxAcceptableMeanSquaredErrorPerSiblingMerge,
                                                   int minimumNumberOfNodesToTriggerApproximation) {
        super(context, divergenceMeasure, maxPower, sampleNumPerContinuousVar, regularizationCoefficient);

        this.maxAcceptableMeanSquaredErrorPerSiblingMerge = maxAcceptableMeanSquaredErrorPerSiblingMerge;
        this.minimumNumberOfNodesToTriggerApproximation = minimumNumberOfNodesToTriggerApproximation;
    }

    public DivisiveRegressionBasedXaddApproximator(DivergenceMeasure divergenceMeasure,
                                                   int maxPower,
                                                   int sampleNumPerContinuousVar,
                                                   double regularizationCoefficient,
                                                   double maxAcceptableMeanSquaredErrorPerSiblingMerge,
                                                   int minimumNumberOfNodesToTriggerApproximation) {
        super(divergenceMeasure, maxPower, sampleNumPerContinuousVar, regularizationCoefficient);

        this.maxAcceptableMeanSquaredErrorPerSiblingMerge = maxAcceptableMeanSquaredErrorPerSiblingMerge;
        this.minimumNumberOfNodesToTriggerApproximation = minimumNumberOfNodesToTriggerApproximation;
    }

    @Override
    public XADD.XADDNode approximateXadd(XADD.XADDNode root) {
        int rootNodeCount = context.getNodeCount(context._hmNode2Int.get(root));
        if (rootNodeCount < minimumNumberOfNodesToTriggerApproximation) return root;

        RegionSamplingDataBase mappingFromRegionsToSamplesAndTargets =
                generatePathMappedToSamplesAndTargets(root, sampleNumPerContinuousVar);

        return approximateXadd(new XaddPath(Arrays.asList(root), context), mappingFromRegionsToSamplesAndTargets,
                maxPower, regularizationCoefficient,
                maxAcceptableMeanSquaredErrorPerSiblingMerge);
    }

    private XADD.XADDNode approximateXadd(XaddPath pathToCurrentNode, //inclusive
                                          RegionSamplingDataBase mappingFromRegionsToSamplesAndTargets,
                                          int maxPower, double regularizationCoefficient,
                                          double maxAcceptableMeanSquaredErrorPerSiblingMerge) {

        SamplingDB currentPathSamples = mappingFromRegionsToSamplesAndTargets.getAccumulatedSamplingInfo(pathToCurrentNode);
        if (currentPathSamples.isEmpty()) {
            //there is no sampling data for this path.
//            System.out.println("No samples for path: " + pathToCurrentNode + " exists.");
            return null;
        }

        XADD.XADDNode lastPathNode = pathToCurrentNode.getLastNode();
        if (ZERO_REGIONS_CAN_BE_MERGED_WITH_NONZERO_REGIONS || !zeroSampleExistIn(currentPathSamples)) {


            HashSet<String> localVars = new HashSet<String>();
            lastPathNode.collectVars(localVars);
            if (localVars.isEmpty()) {
                if (lastPathNode instanceof XADD.XADDTNode)
                    return (XADD.XADDTNode) lastPathNode; // a constant leaf cannot be approximated by this method.
                throw new RuntimeException("an unexpected inner node with no variable");
            }
            XADD.XADDTNode approximatedNode = approximateByRegression(localVars/*lastPathNode*/, currentPathSamples, maxPower, regularizationCoefficient);
            double mse = divergenceMeasure.calcDivergenceBetweenApproximatingNodeAndSamples(context, approximatedNode, currentPathSamples);
//            System.out.println(divergenceMeasure.measureName() + " = " + mse);

            if (mse <= maxAcceptableMeanSquaredErrorPerSiblingMerge) return approximatedNode;

        }

        if (lastPathNode instanceof XADD.XADDTNode)
            return lastPathNode;//approximatedNode; //todo: should I approximate the leaf in any expense?//lastPathNode; //approximation is impossible

        XADD.XADDINode iNode = (XADD.XADDINode) lastPathNode;
        XADD.XADDNode low = context._hmInt2Node.get(iNode._low);
        XADD.XADDNode high = context._hmInt2Node.get(iNode._high);

        pathToCurrentNode.add(low);
        XADD.XADDNode approxLow = approximateXadd(pathToCurrentNode,
                mappingFromRegionsToSamplesAndTargets, maxPower, regularizationCoefficient, maxAcceptableMeanSquaredErrorPerSiblingMerge);

        pathToCurrentNode.setLastNodeTo(high);
        XADD.XADDNode approxHigh = approximateXadd(pathToCurrentNode,
                mappingFromRegionsToSamplesAndTargets, maxPower, regularizationCoefficient, maxAcceptableMeanSquaredErrorPerSiblingMerge);
        pathToCurrentNode.removeLastNode();

        if (approxLow == null) {
            //todo: But what if the current node contains Boolean variables, should I assign values to them? I should check....
            //todo: what if the decision leading to the chosen child implies that a continuous variable have a particular value?
            return approxHigh;
        }

        if (approxHigh == null) {
            return approxLow; //todo: same concerns...
        }

        Integer approxLowId = context._hmNode2Int.get(approxLow);
        Integer approxHighId = context._hmNode2Int.get(approxHigh);
        if (approxLowId.equals(approxHighId)) {
            if (ZERO_REGIONS_CAN_BE_MERGED_WITH_NONZERO_REGIONS)
                throw new RuntimeException("how is it that the parent cannot be approximated but the children's approximation leads to the same stuff?");
            else {
                System.out.print("Parent of these two nodes could not be approximated: approxHigh = " + approxHigh + " and ");
                System.out.println("approxLow = " + approxLow);
                return approxHigh;
            }

        }
        return context.getExistNode(context.getINode(iNode._var, approxLowId, approxHighId));
    }
}
