package hgm.asve.cnsrv.infer;

import hgm.asve.cnsrv.approxator.regression.DivisiveRegressionBasedXaddApproximator;
import hgm.asve.cnsrv.approxator.regression.measures.MeanSquareErrorMeasure;
import hgm.asve.cnsrv.factor.Factor;
import hgm.asve.cnsrv.factory.ModelBasedXaddFactorFactory;
import hgm.asve.cnsrv.gm.FBQuery;

/**
 * Created by Hadi Afshar.
 * Date: 11/11/13
 * Time: 2:17 AM
 */
public class LazyApproxSveInferenceEngineTest {
    public static void main(String[] args) throws Exception {
        LazyApproxSveInferenceEngineTest instance = new LazyApproxSveInferenceEngineTest();
        instance.testLazyVsNonLazy("./src/sve/radar.gm", "./src/sve/radar.query.4", true);
    }

    public void testLazyVsNonLazy(String gmFile, String qFile, Boolean exclusivelyUseAncestorsOfQueryAndEvidenceFactors) throws Exception {
        FBQuery q = new FBQuery(qFile);
        ModelBasedXaddFactorFactory factory = ModelBasedXaddFactorFactory.newInstance(gmFile, q,
                new DivisiveRegressionBasedXaddApproximator(new MeanSquareErrorMeasure(), 2, 100, 0.01, 0.00002, 5)
//                Approximator.DUMMY_APPROXIMATOR
        );


        ExactSveInferenceEngine exact = new ExactSveInferenceEngine(factory);
        LazyApproxSveInferenceEngine approx1 = new LazyApproxSveInferenceEngine(factory, 1);
        ApproxSveInferenceEngine approx2 = new ApproxSveInferenceEngine(factory);


        Factor exactF = exact.infer(null, exclusivelyUseAncestorsOfQueryAndEvidenceFactors);
        Factor approxF1 = approx1.infer();

        Factor approxF2 = approx2.infer();

        System.out.println("----------- F I N A L   F A C T O R S --------------");
        System.out.println("approx1.getRecords().getFinalResultRecord() = " + approx1.getRecords().getFinalResultRecord());
        System.out.println("approx2.getRecords().getFinalResultRecord() = " + approx2.getRecords().getFinalResultRecord());
        System.out.println("----------- . . . . . . . . . . . . .---------------");

        factory.getVisualizer().visualizeFactor(exactF, "exactResult");
        factory.getVisualizer().visualizeFactor(approxF1, "approx1");
        factory.getVisualizer().visualizeFactor(approxF2, "approx2");
    }

}
