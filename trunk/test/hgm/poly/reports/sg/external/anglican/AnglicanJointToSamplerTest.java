package hgm.poly.reports.sg.external.anglican;

import hgm.asve.Pair;
import hgm.poly.Fraction;
import hgm.poly.PiecewiseExpression;
import hgm.poly.gm.*;
import hgm.poly.reports.sg.ExperimentalGraphicalModels;
import hgm.poly.sampling.SamplerInterface;
import hgm.poly.sampling.SamplingUtils;
import hgm.sampling.SamplingFailureException;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Hadi Afshar.
 * Date: 8/01/15
 * Time: 5:38 AM
 */
public class AnglicanJointToSamplerTest {
    @Test
    public void testMomentumWithAnglican() throws Exception {
        // m_1      v_1   m_2     v_2
        //   \__p_1__/    \__p_2__/
        //       \_____p_t______/

        Double mu1 = 0.1;
        Double mu2 = 2.1;
        Double nu1 = -2.0;
        Double nu2 = 2.0;
        boolean symmetric = false;
        int numSamples = 10000;

        GraphicalModel bn = ExperimentalGraphicalModels.makeCollisionModel(2, mu1, mu2, nu1, nu2, symmetric);

        SymbolicGraphicalModelHandler handler = new SymbolicGraphicalModelHandler();
        Map<String, Double> evidence = new HashMap<String, Double>();
        evidence.put("p_t", 3d);
//        evidence.put("m_1", 2d);
        evidence.put("v_2", 0.2d);
        List<String> query = Arrays.asList("m_1 v_1".split(" "));

        Pair<PiecewiseExpression<Fraction>, List<DeterministicFactor>> jointAndEliminatedStochasticVars =
                handler.makeJointAndEliminatedStochasticVars(bn, query, evidence);
        PiecewiseExpression<Fraction> joint = jointAndEliminatedStochasticVars.getFirstEntry();
        List<DeterministicFactor> eliminatedStochasticVars = jointAndEliminatedStochasticVars.getSecondEntry();
        final RichJointWrapper jointWrapper =
                new RichJointWrapper(joint, eliminatedStochasticVars, query, -10, 10, bn, evidence);
        //Anglican code:
        AnglicanCodeGenerator.DEBUG = true;
        String anglicanCode = AnglicanCodeGenerator.makeAnglicanCollisionModel(2, mu1, mu2, nu1, nu2, symmetric, evidence, null /*unknown noise*/, query);
        jointWrapper.addExtraInfo(AnglicanCodeGenerator.ANGLICAN_CODE_KEY, anglicanCode);


        final SamplerInterface innerSampler =
                new AnglicanJointToSampler(numSamples + 50, 0.1, AnglicanCodeGenerator.AnglicanSamplingMethod.rdb).makeSampler(jointWrapper);


        SamplerInterface queriedVarsSampler = new SamplerInterface() {
            @Override
            public Double[] reusableSample() throws SamplingFailureException {
                return jointWrapper.reusableQueriedVarValues(innerSampler.reusableSample());
            }
        };

/*
        SamplerInterface sampler = handler.makeQuerySampler(bn, query, evidence, -10, 10,
//                FractionalJointBaselineGibbsSampler.makeJointToSampler()
//                FractionalJointRejectionSampler.makeJointToSampler(10)
//                SelfTunedFractionalJointMetropolisHastingSampler.makeJointToSampler(10, 30, 100)
//                FractionalJointMetropolisHastingSampler.makeJointToSampler(10)
//                SymbolicFractionalJointGibbsSampler.makeJointToSampler()
                new AnglicanJointToSampler(1000, 0.1, AnglicanCodeGenerator.AnglicanSamplingMethod.rdb)
        );
*/

//        for (int i = 0; i<10; i++) {
//            Double[] sample = sampler.reusableSample();
//            System.out.println("sample = " + Arrays.toString(sample));
//            double m_1 = sample[0];
//            double v_1 = sample[1];
//        }
//            double m_2 = sample[2];
//            double v_2 = sample[3];
//            System.out.println("(m_1*v_1 + m_2*v_2) = " + (m_1 * v_1 + m_2 * v_2));

       /* for (int i = 0; i < numSamples; i++) {
            Double[] s = sampler.reusableSample();
            System.out.println(i + ". sample = " + Arrays.toString(s));
            Assert.assertTrue(s[1]<s[0]);
        }*/
        String SAMPLES_FILE_PATH = "D:/JAVA/IdeaProjects/proj2/test/hgm/poly/gm/";
        SamplingUtils.save2DSamples(queriedVarsSampler, numSamples, SAMPLES_FILE_PATH + "scatter2D_anglican" + numSamples);

    }

}
