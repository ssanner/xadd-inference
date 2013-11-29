package hgm.asve.cnsrv.approxator.regression;

import hgm.XaddVisualizer;
import hgm.asve.cnsrv.approxator.Approximator;
import hgm.asve.cnsrv.approxator.regression.measures.ZeroFriendlyMeanSquareErrorMeasure;
import hgm.asve.cnsrv.factor.Factor;
import hgm.asve.cnsrv.factory.ModelBasedXaddFactorFactory;
import hgm.asve.cnsrv.gm.FBQuery;
import hgm.asve.cnsrv.infer.ExactSveInferenceEngine;
import xadd.XADD;

/**
 * Created by Hadi Afshar.
 * Date: 14/11/13
 * Time: 8:40 AM
 */
public class AgglomerativeRegressionBasedXaddApproximatorTest {
    public static void main(String[] args) {
        AgglomerativeRegressionBasedXaddApproximatorTest instance = new AgglomerativeRegressionBasedXaddApproximatorTest();
        instance.testAgglomerativeApproximation("./src/sve/radar.gm", "./src/sve/radar.query.3");
    }

    //they should produce same results for high acceptable errors:
    public void testAgglomerativeApproximation(String gmFile, String qFile) {
        FBQuery q = new FBQuery(qFile);
        ModelBasedXaddFactorFactory factory = ModelBasedXaddFactorFactory.newInstance(gmFile, q, Approximator.DUMMY_APPROXIMATOR);

        ExactSveInferenceEngine exact = new ExactSveInferenceEngine(factory);
        Factor exactResultF = exact.infer(true);

        //todo tempo
        factory.getVisualizer().visualizeFactor(exactResultF, "exact");

        int maxPower = 2;
        int sampleNumPerContinuousVar = 130;//30;
        double regularizationCoefficient = 0.01;
        int maxNumRegions = 4;//40;
        RegressionBasedXaddApproximator approx = new AgglomerativeRegressionBasedXaddApproximator(factory.getContext(),
                new ZeroFriendlyMeanSquareErrorMeasure(),
//                new KLDivergenceMeasure(),
//                new MeanSquareErrorMeasure(),
                maxPower, sampleNumPerContinuousVar, regularizationCoefficient, maxNumRegions);
        XADD.XADDNode approxRoot = approx.approximateXadd(factory.getContext()._hmInt2Node.get(exactResultF.getXaddId()));
        XaddVisualizer.visualize(approxRoot, "approx", factory.getContext());

        System.out.println("approxRoot = " + approxRoot);

//        factory.getContext().getGraph(factory.getContext()._hmNode2Int.get(approxRoot)).launchViewer();

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


        //test ends...........................................
        //end test on Mock.

        //showing leafs:
        RegionAndLeafHighlighter.visualizeRegions(exactResultF, "exactRegions");
        RegionAndLeafHighlighter leafShow = new RegionAndLeafHighlighter(context);
//        XADD.XADDNode exactRegionsXadd = leafShow.getRegionXadd(context._hmInt2Node.get(exactResultF.getXaddId()));
//        XaddVisualizer.visualize(exactRegionsXadd, "exactRegions", factory.getContext());

        XADD.XADDNode approxRegionsXadd = leafShow.getRegionXadd(approxRoot);
        XaddVisualizer.visualize(approxRegionsXadd, "approxRegions", factory.getContext());

    }
}
