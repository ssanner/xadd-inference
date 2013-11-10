package hgm.asve.cnsrv.approxator.fitting;

import hgm.asve.Pair;
import hgm.asve.XaddPath;
import hgm.asve.cnsrv.approxator.Approximator;
import hgm.asve.cnsrv.approxator.sampler.GridSampler;
import hgm.asve.cnsrv.factor.Factor;
import hgm.asve.cnsrv.factory.ElementaryFactorFactory;
import xadd.XADD;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 23/10/13
 * Time: 2:03 PM
 */
public class CurveFittingBasedXaddApproximator implements Approximator {
    public static boolean ZERO_REGIONS_CAN_BE_MERGED_WITH_NONZERO_REGIONS = false;

    private XADD context = null;
    private CurveFitting<Factor> curveFitting = null;
    private DivergenceMeasure divergenceMeasure;

    //parameters:
    int maxPower;
    int sampleNumPerContinuousVar;
    double regularizationCoefficient;
    double maxAcceptableMeanSquaredErrorPerSiblingMerge;
    int minimumNumberOfNodesToTriggerApproximation;

    public CurveFittingBasedXaddApproximator(XADD context, DivergenceMeasure divergenceMeasure,
                                             int maxPower, int sampleNumPerContinuousVar, double regularizationCoefficient,
                                             double maxAcceptableMeanSquaredErrorPerSiblingMerge,
                                             int minimumNumberOfNodesToTriggerApproximation) {
        this(divergenceMeasure, maxPower, sampleNumPerContinuousVar,
                regularizationCoefficient, maxAcceptableMeanSquaredErrorPerSiblingMerge, minimumNumberOfNodesToTriggerApproximation);

        setupWithContext(context);
    }

    public CurveFittingBasedXaddApproximator(DivergenceMeasure divergenceMeasure,
                                             int maxPower, int sampleNumPerContinuousVar, double regularizationCoefficient,
                                             double maxAcceptableMeanSquaredErrorPerSiblingMerge,
                                             int minimumNumberOfNodesToTriggerApproximation) {
        this.divergenceMeasure = divergenceMeasure;

        this.maxPower = maxPower;
        this.sampleNumPerContinuousVar = sampleNumPerContinuousVar;
        this.regularizationCoefficient = regularizationCoefficient;
        this.maxAcceptableMeanSquaredErrorPerSiblingMerge = maxAcceptableMeanSquaredErrorPerSiblingMerge;
        this.minimumNumberOfNodesToTriggerApproximation = minimumNumberOfNodesToTriggerApproximation;

    }

    @Override
    public void setupWithContext(final XADD context) {
        this.context = context;

        curveFitting = new CurveFitting<Factor>(new ElementaryFactorFactory<Factor>() {
            Factor one = new Factor(context.ONE, context, "ONE");

            @Override
            public Factor one() {
                return one;
            }

            @Override
            public Factor getFactorForMultiplicationOfVars(String[] vars) {
                // It should return an XADDTNode:
                StringBuilder sb = new StringBuilder("([");
                for (String var : vars) {
                    sb.append(var).append("*");
                }

                String str = sb.delete(sb.length() - 1, sb.length()).append("])").toString();
                int id = context.buildCanonicalXADDFromString(str);
                return new Factor(id, context, str);
            }

            @Override
            public double evaluate(Factor factor, Map<String, Double> completeContinuousVariableAssignment) {
                return context.evaluate(factor._xadd, new HashMap<String, Boolean>(), new HashMap<String, Double>(completeContinuousVariableAssignment));
            }
        });
    }

    @Override
    public XADD.XADDNode approximateXadd(XADD.XADDNode root) {
        int rootNodeCount = context.getNodeCount(context._hmNode2Int.get(root));
        if (rootNodeCount < minimumNumberOfNodesToTriggerApproximation) return root;

        RegionSamplingDataBase mappingFromRegionsToSamplesAndTargets =
                null;
        try {
            mappingFromRegionsToSamplesAndTargets = generatePathMappedToSamplesAndTargets(root, sampleNumPerContinuousVar);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

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
            System.out.println("No samples for path: " + pathToCurrentNode + " exists.");
            return null;
        }

        XADD.XADDNode lastPathNode = pathToCurrentNode.getLastNode();
        if (ZERO_REGIONS_CAN_BE_MERGED_WITH_NONZERO_REGIONS || !zeroSampleExistIn(currentPathSamples)) {
            XADD.XADDTNode approximatedNode = singleNodeApproximation(lastPathNode, currentPathSamples, maxPower, regularizationCoefficient);
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

    private boolean zeroSampleExistIn(SamplingDB samples) {
        for (Double target : samples.getTargets()) {
            if (target == 0) return true;
        }
        return false;
    }


    //todo: do not take input parameters from outside...
    public XADD.XADDNode approximateXaddByLeafPowerDecreaseWithoutMerging(XADD.XADDNode rootXadd,
                                                                          int maxPower,
                                                                          int sampleNumPerContinuousVar,
                                                                          double regularizationCoefficient) {
        RegionSamplingDataBase regionSampleTargetMap = generatePathMappedToSamplesAndTargets(rootXadd, sampleNumPerContinuousVar);

        Map<XaddPath, XADD.XADDTNode> pathNewLeafMap = new HashMap<XaddPath, XADD.XADDTNode>(regionSampleTargetMap.size());

        for (Map.Entry<XaddPath, SamplingDB> regionSampleTarget : regionSampleTargetMap.entrySet()) {
            XaddPath region = regionSampleTarget.getKey();
            SamplingDB samplingDB = regionSampleTarget.getValue();

            XADD.XADDTNode approxNode = singleNodeApproximation(region.getLeaf(), samplingDB, /*samples, targets,*/ maxPower, regularizationCoefficient);
            pathNewLeafMap.put(region, approxNode);
        }

        XADD.XADDNode result = substitute(rootXadd, pathNewLeafMap);

        return result;
    }

    private XADD.XADDNode substitute(XADD.XADDNode rootXadd,
                                     Map<XaddPath, XADD.XADDTNode> mappingFromCompletePathsToNewLeafs) {
        return substitute(rootXadd, new ArrayList<XADD.XADDNode>(Arrays.asList(rootXadd)), mappingFromCompletePathsToNewLeafs);
    }

    private XADD.XADDNode substitute(XADD.XADDNode currentNode,
                                     List<XADD.XADDNode> inclusivePathToCurrentNode,
                                     Map<XaddPath, XADD.XADDTNode> mappingFromCompletePathsToNewLeafs) {
        if (currentNode instanceof XADD.XADDTNode) {
            for (XaddPath completePath : mappingFromCompletePathsToNewLeafs.keySet()) {
                if (inclusivePathToCurrentNode.equals(completePath)) {
                    return mappingFromCompletePathsToNewLeafs.get(completePath);
                }
            }
            return currentNode;
        }

        XADD.XADDINode iNode = (XADD.XADDINode) currentNode;
        XADD.XADDNode highChild = context.getExistNode(iNode._high);
        XADD.XADDNode lowChild = context.getExistNode(iNode._low);
        inclusivePathToCurrentNode.add(highChild);
        XADD.XADDNode newHighChild = substitute(highChild, inclusivePathToCurrentNode, mappingFromCompletePathsToNewLeafs);
        inclusivePathToCurrentNode.set(inclusivePathToCurrentNode.size() - 1, lowChild);
        XADD.XADDNode newLowChild = substitute(lowChild, inclusivePathToCurrentNode, mappingFromCompletePathsToNewLeafs);
        inclusivePathToCurrentNode.remove(inclusivePathToCurrentNode.size() - 1);

        //If low and high are the same:
        if (newHighChild.equals(newLowChild)) return newHighChild;

        return context.getExistNode(
                context.getINode(iNode._var /*i.e. expression of the node*/,
                        context._hmNode2Int.get(newLowChild), context._hmNode2Int.get(newHighChild)));
    }

    //public //Map<XaddPath, Pair<List<Map<String, Double>> /*continuous varAssigns*/, List<Double> /*targets*/>>
    public RegionSamplingDataBase generatePathMappedToSamplesAndTargets(
            XADD.XADDNode root,
            int sampleNumPerContinuousVar) {
        Pair<List<String>, List<String>> binaryVarsAndContinuousVars = collectionAllBinaryAndContinuousVars(root);
        List<String> binaryVars = binaryVarsAndContinuousVars.getFirstEntry();
        List<String> continuousVars = binaryVarsAndContinuousVars.getSecondEntry();

        String[] allVariables = new String[binaryVars.size() + continuousVars.size()];
        double[] allMinValues = new double[binaryVars.size() + continuousVars.size()];
        double[] allMaxValues = new double[binaryVars.size() + continuousVars.size()];
        double[] allVarIncVals = new double[binaryVars.size() + continuousVars.size()];

        for (int i = 0; i < binaryVars.size(); i++) {
            allVariables[i] = binaryVars.get(i);
            allMinValues[i] = 0;
            allMaxValues[i] = 1;
            allVarIncVals[i] = 1;
        }
        int offset = binaryVars.size();
        for (int i = 0; i < continuousVars.size(); i++) {
            String var = continuousVars.get(i);
            Double min = context._hmMinVal.get(var);
            Double max = context._hmMaxVal.get(var);
            allVariables[i + offset] = var;
            allMinValues[i + offset] = min;
            allMaxValues[i + offset] = max;
            allVarIncVals[i + offset] = (max - min) / (double) sampleNumPerContinuousVar;
        }

        RegionSamplingDataBase pathInfoDatabase = new RegionSamplingDataBase();
//        Map<XaddPath, Pair<List<Map<String, Double>> /*continuous varAssigns*/, List<Double> /*targets*/>> pathInfoDatabase = new
//                HashMap<XaddPath, Pair<List<Map<String, Double>>, List<Double>>>();


        GridSampler sampler = new GridSampler(allVariables, allMinValues, allMaxValues, allVarIncVals);
        Iterator<double[]> sampleIterator = sampler.getSampleIterator();
        while (sampleIterator.hasNext()) {
            double[] sample = sampleIterator.next();
//            System.out.println("Arrays.toString(sample) = " + Arrays.toString(sample));

            Pair<HashMap<String, Boolean> /*bool_assign*/, HashMap<String, Double> /*con_assign*/> booleanAndContinuousAssignments =
                    refactorToBooleanContinuousAssignmentPair(offset, allVariables, sample);
            HashMap<String, Boolean> booleanAssignment = booleanAndContinuousAssignments.getFirstEntry();
            HashMap<String, Double> continuousAssignment = booleanAndContinuousAssignments.getSecondEntry();
            Pair<XaddPath, Double> activatedPathAndEvaluation =
                    getActivatedPathAndEvaluation(root, booleanAssignment, continuousAssignment);
            XaddPath activatedRegion = activatedPathAndEvaluation.getFirstEntry();
            Double target = activatedPathAndEvaluation.getSecondEntry();

            pathInfoDatabase.addSamplingInfo(activatedRegion, continuousAssignment, target);
        }
        return pathInfoDatabase;
    }

    private Pair<HashMap<String, Boolean>, HashMap<String, Double>> refactorToBooleanContinuousAssignmentPair(int numberOfBooleanVars,
                                                                                                              String[] vars,
                                                                                                              double[] sample) {
        HashMap<String, Boolean> booleanAssign = new HashMap<String, Boolean>(numberOfBooleanVars);
        HashMap<String, Double> continuousAssign = new HashMap<String, Double>(sample.length - numberOfBooleanVars);
        for (int i = 0; i < numberOfBooleanVars; i++) {
            booleanAssign.put(vars[i], sample[i] != 0);
        }
        for (int i = numberOfBooleanVars; i < sample.length; i++) {
            continuousAssign.put(vars[i], sample[i]);
        }
        return new Pair<HashMap<String, Boolean>, HashMap<String, Double>>(booleanAssign, continuousAssign);
    }

    private Pair<List<String> /*binary*/, List<String>> collectionAllBinaryAndContinuousVars(XADD.XADDNode node) {
        List<String> allVars = new ArrayList<String>(node.collectVars());
        List<String> binaryVars = new ArrayList<String>();
        List<String> continuousVars = new ArrayList<String>();
        for (String var : allVars) {
            if (context._alBooleanVars.contains(var)) {
                binaryVars.add(var);
            } else {
                continuousVars.add(var);
            }
        }
        return new Pair<List<String>, List<String>>(binaryVars, continuousVars);
    }


    public Pair<XaddPath, Double> getActivatedPathAndEvaluation(XADD.XADDNode n,
                                                                HashMap<String, Boolean> bool_assign,
                                                                HashMap<String, Double> cont_assign) {
        XaddPath path = new XaddPath(context);

        // Traverse decision diagram until terminal found
        while (n instanceof XADD.XADDINode) {
            path.add(n);
            XADD.XADDINode iNode = (XADD.XADDINode) n;
            XADD.Decision d = context._alOrder.get(iNode._var);
            Boolean branch_high = null;
            if (d instanceof XADD.TautDec)
                branch_high = ((XADD.TautDec) d)._bTautology;
            else if (d instanceof XADD.BoolDec)
                branch_high = bool_assign.get(((XADD.BoolDec) d)._sVarName);
            else if (d instanceof XADD.ExprDec) {
                branch_high = ((XADD.ExprDec) d)._expr.evaluate(cont_assign);
            }

            // Not all required variables were assigned
            if (branch_high == null)
                return null;

            // Advance down to next node
            n = context.getExistNode(branch_high ? iNode._high : iNode._low);
        }

        // Now at a terminal node so evaluate expression
        XADD.XADDTNode t = (XADD.XADDTNode) n;
        path.add(t);
        return new Pair<XaddPath, Double>(path, t._expr.evaluate(cont_assign));
    }


    public XADD.XADDTNode singleNodeApproximation(XADD.XADDNode node,
                                                  SamplingDB samplingDB,
                                                  int maxPower,
                                                  double regularizationCoefficient) {
        HashSet<String> localVars = new HashSet<String>();
        node.collectVars(localVars);


        if (localVars.isEmpty()) {
            if (node instanceof XADD.XADDTNode)
                return (XADD.XADDTNode) node; // a constant leaf cannot be approximated by this method.
            throw new RuntimeException("an unexpected inner node with no variable");
        }

        //Note: only local samples i.e. samples of local vars should be passed otherwise the determinant will be zero!!!!!!!!!!!!!!!!!!!
        List<Factor> basisFunctions = curveFitting.calculateBasisFunctions(maxPower, new ArrayList<String>(localVars));
//        System.out.println("basisFunctions = " + basisFunctions);
//        double[] weights = curveFitting.solveWeights(basisFunctions, pathSamples, regionTargetAssignments, regularizationCoefficient);
        double[] weights = curveFitting.solveWeights(basisFunctions, samplingDB.getSamples(), samplingDB.getTargets(), regularizationCoefficient);

        //summation of products of w_i in basis_i
        int combinationXaddId = context.ZERO;
        for (int i = 0; i < weights.length; i++) {
            int basisId = basisFunctions.get(i)._xadd;
            int wb = context.scalarOp(basisId, weights[i], XADD.PROD);
            combinationXaddId = context.applyInt(combinationXaddId, wb, XADD.SUM);
        }

        return (XADD.XADDTNode) context.getExistNode(combinationXaddId);
    }
}
