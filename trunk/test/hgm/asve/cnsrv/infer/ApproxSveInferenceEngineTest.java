package hgm.asve.cnsrv.infer;

import hgm.asve.cnsrv.approxator.EfficientPathIntegralCalculator;
import hgm.asve.cnsrv.approxator.MassThresholdXaddApproximator;
import hgm.asve.cnsrv.approxator.SiblingXaddApproximator;
import hgm.asve.cnsrv.factory.ModelBasedXaddFactorFactory;
import hgm.asve.cnsrv.gm.FBQuery;
import hgm.asve.cnsrv.factor.Factor;
import org.junit.Test;
import sve.GraphicalModel;
import sve.Query;
import sve.SVE;

/**
 * Created by Hadi Afshar.
 * Date: 6/10/13
 * Time: 11:32 PM
 */
public class ApproxSveInferenceEngineTest {

    public static void main(String[] args) {
        ApproxSveInferenceEngineTest instance = new ApproxSveInferenceEngineTest();

//NOTE: "radar.query3" does not work (even with the old SVE)!

        //NOTE: MSE = -6.763313647249394!!!!    -6.770624969513447!
        instance.testMassThresholdApproxVsExactSVE("./src/sve/radar.gm", "./src/sve/radar.query.4", 0/*0.04*/, Double.POSITIVE_INFINITY);
//        instance.testSiblingApproximation("./src/sve/radar.gm", "./src/sve/radar.query.4", 5, 0.01, 1);

    }

    @Test
    public void testMassThresholdApproxVsExactSVE(String gmFile, String qFile,
                                                  double massThreshold, double volumeThreshold) {
        Boolean doApproxAndExactInference = true;
        Boolean doOldSVE = false;
//        Boolean doExactInference = true;

        FBQuery q = new FBQuery(qFile);
        ModelBasedXaddFactorFactory factory = ModelBasedXaddFactorFactory.newInstance(gmFile, q,
                new MassThresholdXaddApproximator(new EfficientPathIntegralCalculator(), massThreshold, volumeThreshold));

        Records approxRecords = null;

        Factor approxResultF = null;//inferHack(Arrays.asList("x_1", "x_2", "b_1", "o_1"));
        if (doApproxAndExactInference) {
//            LazyApproxSveInferenceEngine approx = new LazyApproxSveInferenceEngine(factory, numberOfFactorsLeadingToJoint); //, massThreshold, volumeThreshold);
            ApproxSveInferenceEngine approx = new ApproxSveInferenceEngine(factory); //, massThreshold, volumeThreshold);

//        Factor approxResultF = approx.infer();
            approxResultF = approx.infer();

//            int leaves = factory.countLeafCount(approxResultF);
//            System.out.println("leaves = " + leaves);
//            System.out.println("resultF = " + approxResultF);

            //todo
            factory.getVisualizer().visualizeFactor(approxResultF, ("approx normalized result"));


            approxRecords = approx.getRecords();
            System.out.println("approx records = " + approxRecords);

        }

////////////////////////////////////

        if (doOldSVE) {

            //SVE:
            GraphicalModel gm = new GraphicalModel(gmFile);
            SVE sve = new SVE(gm);


            Query q1 = new Query(qFile);
            GraphicalModel.Factor sveResultF = sve.infer(q1);
//        XADDUtils.PlotXADD(_context, factor.getNodeId(), min_val, 0.1d, max_val, var.getName(), title);
            SVE.Visualize1DFactor(sveResultF, "SVE tracking");
        }

////////////////////////////////////

        if (doApproxAndExactInference) {

            System.out.println("+++++++++++++++++++++++++++++++++++++++++++++");

//        FBXADDFactorFactory factory2 = new FBXADDFactorFactory(gmFile, -1, -1);
            ExactSveInferenceEngine exact = new ExactSveInferenceEngine(factory);
            Factor exactResultF = exact.infer(approxRecords.desiredVariableEliminationOrder, true);
//            int exactLeaves = factory.countLeafCount(exactResultF);
//            System.out.println("exactLeaves = " + exactLeaves);

//            if (exactResultF.getScopeVars().size() == 1) {


            //todo recent
            factory.getVisualizer().visualizeFactor(exactResultF, "exact II");

//            }

//            System.out.println("exact.getRecords() = " + exact.getRecords());

//            System.out.println("approxResultF.txt = " + approxResultF.getHelpingText());
//            System.out.println("exactResultF.txt = " + exactResultF.getHelpingText());

            Records exactRecords = exact.getRecords();
            System.out.println("exactRecords = " + exactRecords);

            System.out.println("----------- F I N A L   F A C T O R S --------------");
            System.out.println("approxRecords.getFinalResultRecord() = " + approxRecords.getFinalResultRecord());
            System.out.println("exactRecords.getFinalResultRecord() = " + exactRecords.getFinalResultRecord());
            System.out.println("----------- . . . . . . . . . . . . .---------------");


            double mES = factory.meanSquaredError(approxResultF, exactResultF, false);
            System.out.println("mES = " + mES);

//            try {
//                double klDiv = factory.klDivergence(approxResultF, exactResultF, 1000);
//                System.out.println("klDiv = " + klDiv);
//            } catch (Exception e) {
//                System.err.println(e);
//            }

        }

    }


    @Deprecated    // Sibling approximation is not used any more
    private void testSiblingApproximation(String gmFile, String qFile,
                                          int maxDesiredNumberOfNodes, double siblingDifThreshold, int numberOfFactorsLeadingToJoint) {

        FBQuery q = new FBQuery(qFile);
//        ModelBasedXaddFactorFactoryWithSiblingApprox factory = new ModelBasedXaddFactorFactoryWithSiblingApprox(gmFile, q);
        ModelBasedXaddFactorFactory factory = ModelBasedXaddFactorFactory.newInstance(gmFile, q, new SiblingXaddApproximator(new EfficientPathIntegralCalculator(),
                maxDesiredNumberOfNodes, siblingDifThreshold));

        Records approxRecords;
        Factor approxResultF;
        ApproxSveInferenceEngine approx = new ApproxSveInferenceEngine(factory); //, maxDesiredNumberOfNodes, siblingDifThreshold);

        approxResultF = approx.infer();
        factory.getVisualizer().visualizeFactor(approxResultF, ("approx normalized result"));

        approxRecords = approx.getRecords();
        System.out.println("approx records = " + approxRecords);


        System.out.println("+++++++++++++++++++++++++++++++++++++++++++++");

        ExactSveInferenceEngine exact = new ExactSveInferenceEngine(factory);
        Factor exactResultF = exact.infer(approxRecords.desiredVariableEliminationOrder, true);
        factory.getVisualizer().visualizeFactor(exactResultF, "exact II");
        Records exactRecords = exact.getRecords();
        System.out.println("exactRecords = " + exactRecords);

        System.out.println("----------- F I N A L   F A C T O R S --------------");
        System.out.println("approxRecords.getFinalResultRecord() = " + approxRecords.getFinalResultRecord());
        System.out.println("exactRecords.getFinalResultRecord() = " + exactRecords.getFinalResultRecord());
        System.out.println("----------- . . . . . . . . . . . . .---------------");


        double mES = factory.meanSquaredError(approxResultF, exactResultF, false);
        System.out.println("mES = " + mES);

    }

}


