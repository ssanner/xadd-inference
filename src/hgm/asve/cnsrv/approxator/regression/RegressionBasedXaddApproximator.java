package hgm.asve.cnsrv.approxator.regression;

import hgm.asve.Pair;
import hgm.asve.XaddPath;
import hgm.asve.cnsrv.approxator.Approximator;
import hgm.asve.cnsrv.approxator.regression.measures.DivergenceMeasure;
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
public abstract class RegressionBasedXaddApproximator implements Approximator {
    protected XADD context = null;
    protected Regression<Factor> regression = null;
    protected DivergenceMeasure divergenceMeasure;

    //parameters:
    protected int maxPower;
    protected int sampleNumPerContinuousVar;
    protected double regularizationCoefficient;

    public RegressionBasedXaddApproximator(XADD context, DivergenceMeasure divergenceMeasure,
                                           int maxPower, int sampleNumPerContinuousVar, double regularizationCoefficient) {
        this(divergenceMeasure, maxPower, sampleNumPerContinuousVar, regularizationCoefficient);

        setupWithContext(context);
    }

    public RegressionBasedXaddApproximator(DivergenceMeasure divergenceMeasure,
                                           int maxPower, int sampleNumPerContinuousVar, double regularizationCoefficient) {
        this.divergenceMeasure = divergenceMeasure;

        this.maxPower = maxPower;
        this.sampleNumPerContinuousVar = sampleNumPerContinuousVar;
        this.regularizationCoefficient = regularizationCoefficient;
    }

    @Override
    public void setupWithContext(final XADD context) {
        this.context = context;

        regression = new Regression<Factor>(new ElementaryFactorFactory<Factor>() {
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
                Double evaluate = null;

                    evaluate = context.evaluate(factor.getXaddId(), new HashMap<String, Boolean>(), new HashMap<String, Double>(completeContinuousVariableAssignment));
                if (evaluate == null) {
                    System.out.println("evaluate = " + evaluate);
                }
                return evaluate;
            }
        });
    }

    @Override
    abstract public XADD.XADDNode approximateXadd(XADD.XADDNode root);


    protected boolean zeroSampleExistIn(SamplingDB samples) {
        for (Double target : samples.getTargets()) {
            if (target == 0) return true;
        }
        return false;
    }

    @Deprecated //only used in tests...
    public XADD.XADDNode approximateXaddByLeafPowerDecreaseWithoutMerging(XADD.XADDNode rootXadd) {
        RegionSamplingDataBase regionSampleTargetMap = generatePathMappedToSamplesAndTargets(rootXadd, sampleNumPerContinuousVar);

        Map<XaddPath, XADD.XADDTNode> pathNewLeafMap = new HashMap<XaddPath, XADD.XADDTNode>(regionSampleTargetMap.size());

        for (Map.Entry<XaddPath, SamplingDB> regionSampleTarget : regionSampleTargetMap.entrySet()) {
            XaddPath region = regionSampleTarget.getKey();
            SamplingDB samplingDB = regionSampleTarget.getValue();


            XADD.XADDTNode node = region.getLeaf();
            HashSet<String> localVars = new HashSet<String>();
            node.collectVars(localVars);
            if (localVars.isEmpty()) {
                return node;
            }
            XADD.XADDTNode approxNode = approximateByRegression(localVars/*region.getLeaf()*/, samplingDB, /*samples, targets,*/ maxPower, regularizationCoefficient);
            pathNewLeafMap.put(region, approxNode);
        }

        XADD.XADDNode result = substitute(rootXadd, pathNewLeafMap, true);

        return result;
    }

    protected XADD.XADDNode substitute(XADD.XADDNode rootXadd,
                                       Map<XaddPath, XADD.XADDTNode> mappingFromCompletePathsToNewLeafs,
                                       boolean removeOtherRegions) {
        return substitute(rootXadd, new XaddPath(Arrays.asList(rootXadd), context), mappingFromCompletePathsToNewLeafs, removeOtherRegions);
    }

    private XADD.XADDNode substitute(XADD.XADDNode currentNode,
                                     XaddPath inclusivePathToCurrentNode,
                                     Map<XaddPath, XADD.XADDTNode> mappingFromCompletePathsToNewLeafs,
                                     boolean removeOtherRegions) {
        if (currentNode instanceof XADD.XADDTNode) {
//            System.out.println(" ........ In: ");
//            System.out.println("mappingFromCompletePathsToNewLeafs.keySet() = ");
//            for (XaddPath pppp : mappingFromCompletePathsToNewLeafs.keySet()) {
//                System.out.println(pppp);
//            }
//            System.out.println("I want to find: " + inclusivePathToCurrentNode);

            for (XaddPath completePath : mappingFromCompletePathsToNewLeafs.keySet()) {
                if (inclusivePathToCurrentNode.equals(completePath)) {
//                    System.out.println("I found completePath = " + completePath);
                    return mappingFromCompletePathsToNewLeafs.get(completePath);
                }
            }
            return removeOtherRegions ? null : currentNode;

            /*System.out.println("I did not find :" + inclusivePathToCurrentNode);
            System.out.println(" ........ In: ");
            System.out.println("mappingFromCompletePathsToNewLeafs.keySet() = ");
            for (XaddPath pppp : mappingFromCompletePathsToNewLeafs.keySet()) {
                System.out.println(pppp);
            }
            System.out.println(" - - - - - - ");
            for (XaddPath completePath : mappingFromCompletePathsToNewLeafs.keySet()) {
                if (completePath.size() == inclusivePathToCurrentNode.size()) {
                    System.out.println("completePath = " + completePath);
                    System.out.println("inclusivePathToCurrentNode = " + inclusivePathToCurrentNode);
                if (inclusivePathToCurrentNode.equals(completePath)) {
//                    System.out.println("I found completePath = " + completePath);
                    return mappingFromCompletePathsToNewLeafs.get(completePath);
                }
                }
            }
            return currentNode;*/
        }

        XADD.XADDINode iNode = (XADD.XADDINode) currentNode;
        XADD.XADDNode highChild = context.getExistNode(iNode._high);
        XADD.XADDNode lowChild = context.getExistNode(iNode._low);
        inclusivePathToCurrentNode.add(highChild);
        XADD.XADDNode newHighChild = substitute(highChild, inclusivePathToCurrentNode, mappingFromCompletePathsToNewLeafs, removeOtherRegions);
        inclusivePathToCurrentNode.set(inclusivePathToCurrentNode.size() - 1, lowChild);
        XADD.XADDNode newLowChild = substitute(lowChild, inclusivePathToCurrentNode, mappingFromCompletePathsToNewLeafs, removeOtherRegions);
        inclusivePathToCurrentNode.remove(inclusivePathToCurrentNode.size() - 1);

        if (newHighChild == null) return newLowChild;
        if (newLowChild == null) return newHighChild;
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


    public XADD.XADDTNode approximateByRegression(Set<String> localVars, //XADD.XADDNode node,
                                                  SamplingDB samplingDB,
                                                  int maxPower,
                                                  double regularizationCoefficient) {

        //Note: only local samples i.e. samples of local vars should be passed otherwise the determinant will be zero!!!!!!!!!!!!!!!!!!!
        List<Factor> basisFunctions = regression.calculateBasisFunctions(maxPower, new ArrayList<String>(localVars));
//        System.out.println("basisFunctions = " + basisFunctions);
//        double[] weights = curveFitting.solveWeights(basisFunctions, pathSamples, regionTargetAssignments, regularizationCoefficient);
        double[] weights = regression.solveWeights(basisFunctions, samplingDB.getSamples(), samplingDB.getTargets(), regularizationCoefficient);

        //summation of products of w_i in basis_i
        int combinationXaddId = context.ZERO;
        for (int i = 0; i < weights.length; i++) {
            int basisId = basisFunctions.get(i).getXaddId();
            int wb = context.scalarOp(basisId, weights[i], XADD.PROD);
            combinationXaddId = context.applyInt(combinationXaddId, wb, XADD.SUM);
        }

        return (XADD.XADDTNode) context.getExistNode(combinationXaddId);
    }
}
