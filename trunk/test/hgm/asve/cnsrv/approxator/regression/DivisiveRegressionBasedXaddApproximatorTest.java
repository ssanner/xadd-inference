package hgm.asve.cnsrv.approxator.regression;

import hgm.utils.vis.XaddVisualizer;
import hgm.asve.XaddPath;
import hgm.asve.cnsrv.approxator.Approximator;
import hgm.asve.cnsrv.approxator.regression.measures.MeanSquareErrorMeasure;
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
 * Date: 13/11/13
 * Time: 11:50 PM
 */
public class DivisiveRegressionBasedXaddApproximatorTest {

    public static void main(String[] args) {
        DivisiveRegressionBasedXaddApproximatorTest instance = new DivisiveRegressionBasedXaddApproximatorTest();
        instance.testCurveFittingBasedXaddApproxVsMockFitting("./src/sve/radar.gm", "./src/sve/radar.query.3");
//        instance.testCurveFittingBasedXaddApproxVsMockFitting("./src/sve/radar.gm", "./src/sve/radar.query.4");
//        instance.testXaddApproximationByLeafMergeOnExactSVE("./src/sve/radar.gm", "./src/sve/radar.query.4");
    }

    @Test
    public void testCurveFittingBasedXaddApproxVsMockFitting1() {
        testCurveFittingBasedXaddApproxVsMockFitting("./src/sve/radar.gm", "./src/sve/radar.query.3");
    }

    @Test
    public void testCurveFittingBasedXaddApproxVsMockFitting2() {
        testCurveFittingBasedXaddApproxVsMockFitting("./src/sve/radar.gm", "./src/sve/radar.query.4");
    }

    @Test
    public void testXaddApproximationByLeafMergeOnExactSVE1() {
        testXaddApproximationByLeafMergeOnExactSVE("./src/sve/radar.gm", "./src/sve/radar.query.4");
    }

    public void testXaddApproximationByLeafMergeOnExactSVE(String gmFile, String qFile) {
        FBQuery q = new FBQuery(qFile);
        ModelBasedXaddFactorFactory factory = ModelBasedXaddFactorFactory.newInstance(gmFile, q, Approximator.DUMMY_APPROXIMATOR);

        ExactSveInferenceEngine exact = new ExactSveInferenceEngine(factory);
        Factor exactResultF = exact.infer(true);
        factory.getVisualizer().visualizeFactor(exactResultF, "exact");

        int maxPower = 5;
        int sampleNumPerContinuousVar = 10;
        double regularizationCoefficient = 0.00;
        double maxAcceptableMeanSquaredErrorPerSiblingMerge = 0.0017;
        int trigger = 30;
        RegressionBasedXaddApproximator approx = new DivisiveRegressionBasedXaddApproximator(factory.getContext(), new MeanSquareErrorMeasure(),
                maxPower, sampleNumPerContinuousVar, regularizationCoefficient, maxAcceptableMeanSquaredErrorPerSiblingMerge, trigger);
        XADD.XADDNode approxRoot = approx.approximateXadd(factory.getContext()._hmInt2Node.get(exactResultF.getXaddId()));
        XaddVisualizer.visualize(approxRoot, "approx", factory.getContext());

        System.out.println("=============================================");
        XADD context = factory.getContext();
        int exactNodeCount = context.getNodeCount(exactResultF.getXaddId());
        System.out.println("exactNodeCount = " + exactNodeCount);
        int exactLeafCount = context.getLeafCount(exactResultF.getXaddId());
        System.out.println("exactLeafCount = " + exactLeafCount);
        System.out.println("=============================================");
        Integer approxRootId = context._hmNode2Int.get(approxRoot);
        System.out.println("context.getNodeCount(approxRootId) = " + context.getNodeCount(approxRootId));
        System.out.println("context.getLeafCount(approxRootId) = " + context.getLeafCount(approxRootId));
    }

    //they should produce same results for high acceptable errors:
    public void testCurveFittingBasedXaddApproxVsMockFitting(String gmFile, String qFile) {
        FBQuery q = new FBQuery(qFile);
        ModelBasedXaddFactorFactory factory = ModelBasedXaddFactorFactory.newInstance(gmFile, q, Approximator.DUMMY_APPROXIMATOR /*no approximator (?)*/);

        ExactSveInferenceEngine exact = new ExactSveInferenceEngine(factory);
        Factor exactResultF = exact.infer(true);
        factory.getVisualizer().visualizeFactor(exactResultF, "exact");

        int maxPower = 3;
        int sampleNumPerContinuousVar = 340;//30;
        double regularizationCoefficient = 0.01;
        double maxAcceptableMeanSquaredErrorPerSiblingMerge = 0.00000017;
        int trigger = 50;
        RegressionBasedXaddApproximator approx = new DivisiveRegressionBasedXaddApproximator(factory.getContext(),
//                new KLDivergenceMeasure(),
                new MeanSquareErrorMeasure(),
                maxPower, sampleNumPerContinuousVar, regularizationCoefficient, maxAcceptableMeanSquaredErrorPerSiblingMerge,
                trigger);
        XADD.XADDNode approxRoot = approx.approximateXadd(factory.getContext()._hmInt2Node.get(exactResultF.getXaddId()));
        XaddVisualizer.visualize(approxRoot, "approx", factory.getContext());

        System.out.println("=============================================");
        XADD context = factory.getContext();
        int exactNodeCount = context.getNodeCount(exactResultF.getXaddId());
        System.out.println("exactNodeCount = " + exactNodeCount);
        int exactLeafCount = context.getLeafCount(exactResultF.getXaddId());
        System.out.println("exactLeafCount = " + exactLeafCount);
        System.out.println("=============================================");
        Integer approxRootId = context._hmNode2Int.get(approxRoot);
        System.out.println("context.getNodeCount(approxRootId) = " + context.getNodeCount(approxRootId));
        System.out.println("context.getLeafCount(approxRootId) = " + context.getLeafCount(approxRootId));

        boolean testOnMock = false;
        if (testOnMock) {
            //now test on Mock:
            //just for test:.......................................
            XADD.XADDNode root = context.getExistNode(exactResultF.getXaddId());
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
                    if (sampleIndexes.size() != 1)
                        throw new RuntimeException("only one sample expected!" + sampleIndexes);
                    return targets.get(sampleIndexes.get(0));
                }
            };

            GeneralVisualizationUtils.plot(target, lowerBound, higherBound, "some SAMPLES");

            final MockFactorFactory mockFactory = new MockFactorFactory();
            Regression<MockFactor> fitting = new Regression<MockFactor>(mockFactory);
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
        } //end test ib mock

        //showing leafs:
        RegionAndLeafHighlighter leafShow = new RegionAndLeafHighlighter(context);
        XADD.XADDNode exactRegionsXadd = leafShow.getRegionXadd(context._hmInt2Node.get(exactResultF.getXaddId()));
        XaddVisualizer.visualize(exactRegionsXadd, "exactRegions", factory.getContext());
        XADD.XADDNode approxRegionsXadd = leafShow.getRegionXadd(approxRoot);
        XaddVisualizer.visualize(approxRegionsXadd, "approxRegions", factory.getContext());

    }

}
