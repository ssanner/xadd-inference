package hgm.asve.cnsrv.approxator.fitting;

import hgm.asve.Pair;
import hgm.asve.XaddPath;
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
public class CurveFittingBasedXaddApproximator {
    private XADD context;
    private CurveFitting<Factor> curveFitting;

    public CurveFittingBasedXaddApproximator(final XADD context) {
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
                System.out.println("str = " + str);
                int id = context.buildCanonicalXADDFromString(str);
                return new Factor(id, context, str);
            }

            @Override
            public double evaluate(Factor factor, Map<String, Double> completeContinuousVariableAssignment) {
                return context.evaluate(factor._xadd, new HashMap<String, Boolean>(), new HashMap<String, Double>(completeContinuousVariableAssignment));
            }
        });
    }

    public XADD.XADDNode approximateXaddByLeafPowerDecrease(XADD.XADDNode rootXadd,
                                                            int maxPower,
                                                            int sampleNumPerContinuousVar,
                                                            double regularizationCoefficient) {
        Map<XaddPath, Pair<List<Map<String, Double>>, List<Double>>> regionSampleTargetMap = generatePathMappedToSamplesAndTargets(rootXadd, sampleNumPerContinuousVar);

        Map<XaddPath, XADD.XADDTNode> pathNewLeafMap = new HashMap<XaddPath, XADD.XADDTNode>(regionSampleTargetMap.size());

        for (Map.Entry<XaddPath, Pair<List<Map<String, Double>>, List<Double>>> regionSampleTarget : regionSampleTargetMap.entrySet()) {
            XaddPath region = regionSampleTarget.getKey();
            List<Map<String, Double>> samples = regionSampleTarget.getValue().getFirstEntry();
            List<Double> targets = regionSampleTarget.getValue().getSecondEntry();

            XADD.XADDTNode approxNode = leafApproximation(region.getLeaf(), samples, targets, maxPower, regularizationCoefficient);
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

    public Map<XaddPath, Pair<List<Map<String, Double>> /*continuous varAssigns*/, List<Double> /*targets*/>> generatePathMappedToSamplesAndTargets(
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

        //todo instead of such complicated structure you need a DB object
        Map<XaddPath, Pair<List<Map<String, Double>> /*continuous varAssigns*/, List<Double> /*targets*/>> pathInfoDatabase = new
                HashMap<XaddPath, Pair<List<Map<String, Double>>, List<Double>>>();
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

            Pair<List<Map<String, Double>>, List<Double>> regionInfoDB = pathInfoDatabase.get(activatedRegion);
            if (regionInfoDB == null) {
                regionInfoDB = new Pair<List<Map<String, Double>>, List<Double>>(new ArrayList<Map<String, Double>>(), new ArrayList<Double>());
                pathInfoDatabase.put(activatedRegion, regionInfoDB);
            }
            List<Map<String, Double>> currentAssignments = regionInfoDB.getFirstEntry(); //todo: what if the current sample already exists in the region DB? does it end in 0 determinant?
            List<Double> currentTargets = regionInfoDB.getSecondEntry();
            currentAssignments.add(continuousAssignment);
            currentTargets.add(target);
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


    public XADD.XADDTNode leafApproximation(XADD.XADDTNode leaf,
                                            List<Map<String, Double>> pathSamples,
                                            List<Double> regionTargetAssignments,
                                            int maxPower,
                                            double regularizationCoefficient) {
        assert pathSamples.size() == regionTargetAssignments.size();

        HashSet<String> localVars = new HashSet<String>();
        leaf.collectVars(localVars);


        if (localVars.isEmpty()) return leaf; // a constant leaf cannot be approximated by this method.

        //Note: only local samples i.e. samples of local vars should be passed otherwise the determinant will be zero!!!!!!!!!!!!!!!!!!!
        List<Factor> basisFunctions = curveFitting.calculateBasisFunctions(maxPower, new ArrayList<String>(localVars));
        System.out.println("basisFunctions = " + basisFunctions);
        double[] weights = curveFitting.solveWeights(basisFunctions, pathSamples, regionTargetAssignments, regularizationCoefficient);

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
