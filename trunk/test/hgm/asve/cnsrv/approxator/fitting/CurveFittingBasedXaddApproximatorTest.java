package hgm.asve.cnsrv.approxator.fitting;

import hgm.DebugIntegral.IntegralTest;
import hgm.asve.XaddPath;
import hgm.asve.cnsrv.approxator.Approximator;
import hgm.asve.cnsrv.factor.Factor;
import hgm.asve.cnsrv.factor.MockFactor;
import hgm.asve.cnsrv.factory.MockFactorFactory;
import hgm.asve.cnsrv.factory.ModelBasedXaddFactorFactory;
import hgm.asve.cnsrv.gm.FBQuery;
import hgm.asve.cnsrv.infer.ExactSveInferenceEngine;
import hgm.utils.Function;
import hgm.utils.vis.GeneralVisualizationUtils;
import junit.framework.Assert;
import org.junit.Test;
import xadd.XADD;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 25/10/13
 * Time: 11:20 AM
 */
public class CurveFittingBasedXaddApproximatorTest {

    public static void main(String[] args) throws Exception {
        CurveFittingBasedXaddApproximatorTest instance = new CurveFittingBasedXaddApproximatorTest();
        instance.testCurveFittingBasedXaddApproxVsMockFitting("./src/sve/radar.gm", "./src/sve/radar.query.3");
//        instance.testCurveFittingBasedXaddApproxVsMockFitting("./src/sve/radar.gm", "./src/sve/radar.query.4");
//        instance.testXaddApproximationByLeafMergeOnExactSVE("./src/sve/radar.gm", "./src/sve/radar.query.4");
//        instance.testBasicXaddApproximateByMerge();
//        instance.testLeafPowerDecrease();
//        instance.testLeafPowerDecreaseOnExactSVE("./src/sve/radar.gm", "./src/sve/radar.query.4");
    }

    @Test
    public void testBasicXaddApproximateByMerge() throws Exception {
        final int MAX_POWER = 1;
        final int SAMPLE_NUM_PER_CONTINUOUS_VAR = 40;
        final double REGULARIZATION_COEFFICIENT = 0.1;
        final double MAX_ACCEPTABLE_MEAN_SQUARED_ERROR_PER_SIBLING_MERGE = 0.1;//Double.MAX_VALUE;//1000000000d;
        final int TRIGGER = 2;

        XADD context = new XADD();
        int rootId = context.buildCanonicalXADDFromString("" +
                "([(1 * x) < -18] " +
                "   ([0]) " +
                "   ([(1 * x) > 3]" +
                "       ([(x + y) > 10] ([2.0]) ([0])) " +
                "       ([1 * y < -3] " +
                "           ([0.01 * y*y])" +
                "           ([1 * y > 3] " +
                "               ([0.005*x*x + 0.01*y*y + 0.05*y])" +
                "               ([0.03*x + 0.03*y + 1.0])" +
                "           )" +
                "       )" +
                "   )" +
                ")");
        context._hmMinVal.put("x", -10d);
        context._hmMinVal.put("y", -10d);
        context._hmMaxVal.put("x", 10d);
        context._hmMaxVal.put("y", 10d);
        XADD.XADDNode root = context._hmInt2Node.get(rootId);
        context.getGraph(rootId).launchViewer("root");
        IntegralTest.visualize(root, "root", context);

        CurveFittingBasedXaddApproximator approx = new CurveFittingBasedXaddApproximator(context, new MeanSquareErrorMeasure(),
                MAX_POWER, SAMPLE_NUM_PER_CONTINUOUS_VAR, REGULARIZATION_COEFFICIENT, MAX_ACCEPTABLE_MEAN_SQUARED_ERROR_PER_SIBLING_MERGE,
                TRIGGER);
        XADD.XADDNode approxRoot = approx.approximateXadd(root);

//        context.getGraph(context._hmNode2Int.get(approxRoot)).launchViewer("approx node");
        IntegralTest.visualize(approxRoot, "approxRoot", context);
    }

    @Test
    public void testLeafPowerDecrease() throws Exception {
        final int MAX_POWER = 2;
        final int SAMPLE_NUM_PER_CONTINUOUS_VAR = 4;
        final double REGULARIZATION_COEFFICIENT = 0.000001;
        final int TRIGGER = 2;

        XADD context = new XADD();
        int rootId = context.buildCanonicalXADDFromString("" +
                "([(1 * x) < -5] " +
                "   ([0]) " +
                "   ([(1 * x) > 5]" +
                "       ([0]) " +
                "       ([1 * y < -5] " +
                "           ([0])" +
                "           ([1 * y > 5] " +
                "               ([0])" +
                "               ([x*x*x*x*2 + x*x + x*y*y*y + 0.69])" +
                "           )" +
                "       )" +
                "   )" +
                ")");
        context._hmMinVal.put("x", -10d);
        context._hmMinVal.put("y", -10d);
        context._hmMaxVal.put("x", 10d);
        context._hmMaxVal.put("y", 10d);
        XADD.XADDNode root = context._hmInt2Node.get(rootId);
//        context.getGraph(rootId).launchViewer("root");
        IntegralTest.visualize(root, "root", context);

        CurveFittingBasedXaddApproximator approx = new CurveFittingBasedXaddApproximator(context, new MeanSquareErrorMeasure(),
                MAX_POWER, SAMPLE_NUM_PER_CONTINUOUS_VAR, REGULARIZATION_COEFFICIENT, -1 /*this parameter is not used*/, TRIGGER);
        XADD.XADDNode approxRoot = approx.approximateXaddByLeafPowerDecreaseWithoutMerging(root, MAX_POWER, SAMPLE_NUM_PER_CONTINUOUS_VAR, REGULARIZATION_COEFFICIENT);

        context.getGraph(context._hmNode2Int.get(approxRoot)).launchViewer("approx node");
        IntegralTest.visualize(approxRoot, "approxRoot", context);
    }

    public void testLeafPowerDecreaseOnExactSVE(String gmFile, String qFile) {
        FBQuery q = new FBQuery(qFile);
        ModelBasedXaddFactorFactory factory = ModelBasedXaddFactorFactory.newInstance(gmFile, q, null /*no approximator*/);

        ExactSveInferenceEngine exact = new ExactSveInferenceEngine(factory);
        Factor exactResultF = exact.infer(true);
        factory.getVisualizer().visualizeFactor(exactResultF, "exact");

//        CurveFittingBasedXaddApproximator approx = new CurveFittingBasedXaddApproximator(factory.getContext(), new MeanSquareErrorMeasure());
        CurveFittingBasedXaddApproximator approx = new CurveFittingBasedXaddApproximator(factory.getContext(), new MeanSquareErrorMeasure(),
                2, 800/*SAMPLE_NUM_PER_CONTINUOUS_VAR*/, 0.1 /*REGULARIZATION_COEFFICIENT*/, -1 /*this parameter is not used*/, 1/*trigger*/);
        XADD.XADDNode approxRoot = approx.approximateXaddByLeafPowerDecreaseWithoutMerging(factory.getContext()._hmInt2Node.get(exactResultF._xadd), 2, 800, 0.1);
        IntegralTest.visualize(approxRoot, "approx", factory.getContext());
    }

    public void testXaddApproximationByLeafMergeOnExactSVE(String gmFile, String qFile) {
        FBQuery q = new FBQuery(qFile);
        ModelBasedXaddFactorFactory factory = ModelBasedXaddFactorFactory.newInstance(gmFile, q, null /*approximator*/);

        ExactSveInferenceEngine exact = new ExactSveInferenceEngine(factory);
        Factor exactResultF = exact.infer(true);
        factory.getVisualizer().visualizeFactor(exactResultF, "exact");

        int maxPower = 5;
        int sampleNumPerContinuousVar = 10;
        double regularizationCoefficient = 0.00;
        double maxAcceptableMeanSquaredErrorPerSiblingMerge = 0.0017;
        int trigger = 30;
        CurveFittingBasedXaddApproximator approx = new CurveFittingBasedXaddApproximator(factory.getContext(), new MeanSquareErrorMeasure(),
                maxPower, sampleNumPerContinuousVar, regularizationCoefficient, maxAcceptableMeanSquaredErrorPerSiblingMerge, trigger);
        XADD.XADDNode approxRoot = approx.approximateXadd(factory.getContext()._hmInt2Node.get(exactResultF._xadd));
        IntegralTest.visualize(approxRoot, "approx", factory.getContext());

        System.out.println("=============================================");
        XADD context = factory.getContext();
        int exactNodeCount = context.getNodeCount(exactResultF._xadd);
        System.out.println("exactNodeCount = " + exactNodeCount);
        int exactLeafCount = context.getLeafCount(exactResultF._xadd);
        System.out.println("exactLeafCount = " + exactLeafCount);
        System.out.println("=============================================");
        Integer approxRootId = context._hmNode2Int.get(approxRoot);
        System.out.println("context.getNodeCount(approxRootId) = " + context.getNodeCount(approxRootId));
        System.out.println("context.getLeafCount(approxRootId) = " + context.getLeafCount(approxRootId));
    }

    //they should produce same results for high acceptable errors:
    public void testCurveFittingBasedXaddApproxVsMockFitting(String gmFile, String qFile) {
        FBQuery q = new FBQuery(qFile);
        ModelBasedXaddFactorFactory factory = ModelBasedXaddFactorFactory.newInstance(gmFile, q, Approximator.DUMMY_APPROXIMATOR /*todo no approximator (?)*/);

        ExactSveInferenceEngine exact = new ExactSveInferenceEngine(factory);
        Factor exactResultF = exact.infer(true);
        factory.getVisualizer().visualizeFactor(exactResultF, "exact");

        int maxPower = 3;
        int sampleNumPerContinuousVar = 340;//30;
        double regularizationCoefficient = 0.01;
        double maxAcceptableMeanSquaredErrorPerSiblingMerge = 0.00000017;
        int trigger = 50;
        CurveFittingBasedXaddApproximator approx = new CurveFittingBasedXaddApproximator(factory.getContext(),
//                new KLDivergenceMeasure(),
                new MeanSquareErrorMeasure(),
                maxPower, sampleNumPerContinuousVar, regularizationCoefficient, maxAcceptableMeanSquaredErrorPerSiblingMerge,
                trigger);
        XADD.XADDNode approxRoot = approx.approximateXadd(factory.getContext()._hmInt2Node.get(exactResultF._xadd));
        IntegralTest.visualize(approxRoot, "approx", factory.getContext());

        System.out.println("=============================================");
        XADD context = factory.getContext();
        int exactNodeCount = context.getNodeCount(exactResultF._xadd);
        System.out.println("exactNodeCount = " + exactNodeCount);
        int exactLeafCount = context.getLeafCount(exactResultF._xadd);
        System.out.println("exactLeafCount = " + exactLeafCount);
        System.out.println("=============================================");
        Integer approxRootId = context._hmNode2Int.get(approxRoot);
        System.out.println("context.getNodeCount(approxRootId) = " + context.getNodeCount(approxRootId));
        System.out.println("context.getLeafCount(approxRootId) = " + context.getLeafCount(approxRootId));

        //now test on Mock:
        //just for test:.......................................
        XADD.XADDNode root = context.getExistNode(exactResultF._xadd);
        RegionSamplingDataBase mappingFromRegionsToSamplesAndTargets =
                approx.generatePathMappedToSamplesAndTargets(root, sampleNumPerContinuousVar);

        final SamplingDB currentPathSamples = mappingFromRegionsToSamplesAndTargets.getAccumulatedSamplingInfo(new XaddPath(Arrays.asList(root), context));
        final ArrayList<String> rootVars = new ArrayList<String>(root.collectVars());
        System.out.println("rootVars = " + rootVars);

//        final List<Factor> basisFunctions = curveFitting.calculateBasisFunctions(maxPower, rootVars);
//        System.out.println("basisFunctions = " + basisFunctions);
        final List<Map<String, Double>> samples = currentPathSamples.getSamples();
        final List<Double> targets = currentPathSamples.getTargets();
//        final double[] weights = curveFitting.solveWeights(basisFunctions, samples, targets, regularizationCoefficient);

        double lowerBound = context._hmMinVal.get(rootVars.get(0));
        double higherBound = context._hmMaxVal.get(rootVars.get(0));
        Function target = new Function(rootVars) {
            @Override
            public double func(double... effectiveVars) {
                List<Integer> sampleIndexes = new ArrayList<Integer>();
                for (int i = 0; i < samples.size(); i++) {
                    Map<String, Double> sample = samples.get(i);
                    if (sample.get(rootVars.get(0)).equals(effectiveVars[0]) &&
                            sample.get(rootVars.get(1)).equals(effectiveVars[1])) {
                        sampleIndexes.add(i);
                    }
                }
                if (sampleIndexes.size() == 0) return 0;//todo this is not right
                if (sampleIndexes.size() != 1) throw new RuntimeException("only one sample expected!" + sampleIndexes);
                return targets.get(sampleIndexes.get(0));
            }
        };

        GeneralVisualizationUtils.plot(target, lowerBound, higherBound, "some SAMPLES");

        final MockFactorFactory mockFactory = new MockFactorFactory();
        CurveFitting<MockFactor> fitting = new CurveFitting<MockFactor>(mockFactory);
        final List<MockFactor> basisFunctions2 = fitting.calculateBasisFunctions(maxPower, rootVars);
        final double[] weights = fitting.solveWeights(basisFunctions2, samples, targets, regularizationCoefficient);
        Assert.assertEquals(basisFunctions2.size(), weights.length);
        for (int i = 0; i < weights.length; i++) {
            System.out.println("basisFunctions = " + basisFunctions2.get(i) + " W: " + weights[i]);
        }

        Function estimatedFunction = new Function(rootVars) { //although it depends on other vars as well, they are not plottable

            @Override
            public double func(double... effectiveVars) {
                //reconstruct the map!
                Map<String, Double> varAssign = new HashMap<String, Double>(2);
                String[] varNames = getEffectiveVarNames();
                for (int i = 0; i < varNames.length; i++) {
                    varAssign.put(varNames[i], effectiveVars[i]);
                }
                double totalValue = 0;
                for (int i = 0; i < basisFunctions2.size(); i++) {
                    MockFactor basis = basisFunctions2.get(i);
                    double basisValue = mockFactory.evaluate(basis, varAssign);
                    totalValue += (weights[i] * basisValue);
                }
                return totalValue;

            }
        };
        GeneralVisualizationUtils.plot(estimatedFunction, lowerBound, higherBound, "Approx MOCK");

        //test ends...........................................
        //end test on Mock.

        //showing leafs:
        LeafHighlighter leafShow = new LeafHighlighter(context);
        XADD.XADDNode exactRegionsXadd = leafShow.getRegionXadd(context._hmInt2Node.get(exactResultF._xadd));
        IntegralTest.visualize(exactRegionsXadd, "exactRegions", factory.getContext());
        XADD.XADDNode approxRegionsXadd = leafShow.getRegionXadd(approxRoot);
        IntegralTest.visualize(approxRegionsXadd, "approxRegions", factory.getContext());

    }

    //todo: this class (and visualizer in the integrator class) might go to a util class...
    public class LeafHighlighter {
        private XADD.XADDTNode regionCounterNode;
        private XADD context;

        public LeafHighlighter(XADD context) {
            this.context = context;
        }

        public XADD.XADDNode getRegionXadd(XADD.XADDNode node) {
            regionCounterNode = (XADD.XADDTNode) context.getExistNode(context.ZERO);
            return leafSubstituteToHighlightEachRegion(new XaddPath(Arrays.asList(node), context));
        }

        private XADD.XADDNode leafSubstituteToHighlightEachRegion(XaddPath pathFromRootToThisXADD) {
            XADD.XADDNode modifiedXADD;


            if (pathFromRootToThisXADD.isComplete()) {  //this node is a leaf, the path represents a region
                regionCounterNode = increase(regionCounterNode);
                return regionCounterNode;
            }

            XADD.XADDINode iThisXADD = (XADD.XADDINode) pathFromRootToThisXADD.getLastNode();

            pathFromRootToThisXADD.add(context.getExistNode(iThisXADD._low));
            XADD.XADDNode low = leafSubstituteToHighlightEachRegion(pathFromRootToThisXADD);
            pathFromRootToThisXADD.remove(pathFromRootToThisXADD.size() - 1);

            pathFromRootToThisXADD.add(context.getExistNode(iThisXADD._high));
            XADD.XADDNode high = leafSubstituteToHighlightEachRegion(pathFromRootToThisXADD);
            pathFromRootToThisXADD.remove(pathFromRootToThisXADD.size() - 1);

            modifiedXADD = context.getExistNode(
                    context.getINode(iThisXADD._var, context._hmNode2Int.get(low), context._hmNode2Int.get(high)));
            return modifiedXADD;
        }

        private XADD.XADDTNode increase(XADD.XADDTNode t) {
            return (XADD.XADDTNode) context.getExistNode(context.applyInt(context._hmNode2Int.get(t), context.ONE, XADD.SUM));
        }
    }
}
