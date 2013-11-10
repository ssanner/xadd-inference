package hgm.asve.cnsrv.infer;

import hgm.asve.cnsrv.approxator.Approximator;
import hgm.asve.cnsrv.factor.Factor;
import hgm.asve.cnsrv.factory.ModelBasedXaddFactorFactory;
import hgm.asve.cnsrv.gm.FBQuery;
import org.junit.Test;
import sve.GraphicalModel;
import sve.Query;
import sve.SVE;
import xadd.XADD;

/**
 * Created by Hadi Afshar.
 * Date: 3/11/13
 * Time: 7:06 PM
 */
public class ExactSveInferenceEngineTest {
    public static void main(String[] args) throws Exception {
        ExactSveInferenceEngineTest instance = new ExactSveInferenceEngineTest();
        instance.testExactSveOnHugeXadd("./src/sve/radar.gm", "./src/sve/radar.query.4", true);
//        instance.testExactSveOnHugeXadd("./src/sve/radar.gm", "./test/hgm/asve/cnsrv/infer/radar.query.100", true);

//        instance.testExactSveOnHugeXadd("./src/sve/radar.gm", "./src/sve/radar.query.3", true);  //Interestingly, with "true" works perfect and with false generates java.lang.OutOfMemoryError: GC overhead limit exceeded
        //todo mention the above observation is your report.

//        instance.testExactVersusSVE("./src/sve/test2.gm", "./src/sve/test.query"); // OK
//        instance.testExactVersusSVE("./src/sve/test.gm", "./src/sve/test.query"); // OK
//        instance.testExactVersusSVE("./src/sve/radar.gm", "./src/sve/radar.query.1"); // OK
//        instance.testExactVersusSVE("./src/sve/radar.gm", "./src/sve/radar.query.2"); // OK
//        instance.testExactVersusSVE("./src/sve/radar.gm", "./src/sve/radar.query.3"); // java.lang.OutOfMemoryError: GC overhead limit exceeded  (due to the old SVE)
//        instance.testExactVersusSVE("./src/sve/radar.gm", "./src/sve/radar.query.4"); // OK
//        instance.testExactVersusSVE("./src/sve/radar.gm", "./src/sve/radar.query.5"); // OK
    }

    @Test
    public void testExactVersusSVE(String gmFile, String qFile) {

        //OLD SVE:
        System.out.println(". OLD SVE:");
        GraphicalModel gm = new GraphicalModel(gmFile);
        SVE oldSVE = new SVE(gm);
        Query oldQuery = new Query(qFile);
        GraphicalModel.Factor oldSveResultF = oldSVE.infer(oldQuery);
        //(unfortunately) old SVE draw the visualization itself so there is no reason to draw it again.

//        XADDUtils.PlotXADD(_context, factor.getNodeId(), min_val, 0.1d, max_val, var.getName(), title);
//        SVE.Visualize1DFactor(sveResultF, "SVE tracking");

        //NEW SVE:

        System.out.println(". NEW SVE:");

//        FBXADDFactorFactory factory2 = new FBXADDFactorFactory(gmFile, -1, -1);
        FBQuery q = new FBQuery(qFile);
        ModelBasedXaddFactorFactory factory = ModelBasedXaddFactorFactory.newInstance(gmFile, q, null /*I think no approximator is needed here*/);

        ExactSveInferenceEngine exact = new ExactSveInferenceEngine(factory);
        Factor exactResultF = exact.infer(null, true);
//            int exactLeaves = factory.countLeafCount(exactResultF);
//            System.out.println("exactLeaves = " + exactLeaves);

//            if (exactResultF.getScopeVars().size() == 1) {
//            factory.getVisualizer().visualizeFactor(exactResultF, "exact II");

//            }

//            System.out.println("exact.getRecords() = " + exact.getRecords());

//            System.out.println("approxResultF.txt = " + approxResultF.getHelpingText());
//            System.out.println("exactResultF.txt = " + exactResultF.getHelpingText());

        XADD oldContext = oldSveResultF._localContext;
        int oldNodeCount = oldContext.getNodeCount(oldSveResultF._xadd);
        int oldLeafCount = oldContext.getLeafCount(oldSveResultF._xadd);
        int oldPathCount = oldContext.getBranchCount(oldSveResultF._xadd);


        Records exactRecords = exact.getRecords();
        System.out.println("exactRecords = " + exactRecords);

        System.out.println("----------- F I N A L   F A C T O R S --------------");
        System.out.printf("old SVE final Result Record = \t\t\t\t\t\t\t\t\t\t\t [#Node: %d][#Leaf: %d][#path: %d]\n",oldNodeCount, oldLeafCount, oldPathCount);
        System.out.println("exactRecords.getFinalResultRecord() = " + exactRecords.getFinalResultRecord());
        System.out.println("----------- . . . . . . . . . . . . .---------------");

        factory.getVisualizer().visualizeFactor(exactResultF, "exactResult");

        //todo assert that node, leaf and path count of the old and new SVE are equal...

//        double mES = factory.meanSquaredError(approxResultF, exactResultF);
//            System.out.println("mES = " + mES);
    }

    public void testExactSveOnHugeXadd(String gmFile, String qFile, Boolean exclusivelyUseAncestorsOfQueryAndEvidenceFactors) throws Exception {
        System.out.println(". NEW SVE:");
        FBQuery q = new FBQuery(qFile);
        ModelBasedXaddFactorFactory factory = ModelBasedXaddFactorFactory.newInstance(gmFile, q,
                Approximator.DUMMY_APPROXIMATOR
        );

        ExactSveInferenceEngine exact = new ExactSveInferenceEngine(factory);
        Factor exactResultF = exact.infer(null, exclusivelyUseAncestorsOfQueryAndEvidenceFactors);


        Records exactRecords = exact.getRecords();
        System.out.println("exactRecords = " + exactRecords);

        System.out.println("----------- F I N A L   F A C T O R S --------------");
        System.out.println("exactRecords.getFinalResultRecord() = " + exactRecords.getFinalResultRecord());
        System.out.println("----------- . . . . . . . . . . . . .---------------");

        factory.getVisualizer().visualizeFactor(exactResultF, "exactResult");
    }
}
