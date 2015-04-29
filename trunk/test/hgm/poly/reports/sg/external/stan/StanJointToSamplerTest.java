package hgm.poly.reports.sg.external.stan;

import hgm.asve.Pair;
import hgm.poly.Fraction;
import hgm.poly.PiecewiseExpression;
import hgm.poly.gm.DeterministicFactor;
import hgm.poly.gm.GraphicalModel;
import hgm.poly.gm.RichJointWrapper;
import hgm.poly.gm.SymbolicGraphicalModelHandler;
import hgm.poly.reports.sg.ExperimentalGraphicalModels;
import hgm.poly.reports.sg.external.anglican.AnglicanCodeGenerator;
import hgm.poly.reports.sg.external.anglican.AnglicanJointToSampler;
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
 * Date: 22/01/15
 * Time: 4:32 PM
 */
public class StanJointToSamplerTest {
    @Test
    public void testMomentumWithStan() throws Exception {
        // m_1      v_1   m_2     v_2
        //   \__p_1__/    \__p_2__/
        //       \_____p_t______/

        int numSamples = 10000;

//        final RichJointWrapper jointWrapper =
//                new RichJointWrapper(joint, eliminatedStochasticVars, query, -10, 10, bn, evidence);
        //Anglican code:
//        String anglicanCode = AnglicanCodeGenerator.makeAnglicanCollisionModel(2, mu1, mu2, nu1, nu2, symmetric, evidence, null /*unknown noise*/, query);
//        jointWrapper.addExtraInfo(AnglicanCodeGenerator.ANGLICAN_CODE_KEY, anglicanCode);


        final SamplerInterface queriedVarsSampler = StanJointToSampler.makeSampler("collision_asymetric_model", "asymetric.data.R", "asym.out.csv");


//        SamplerInterface queriedVarsSampler = new SamplerInterface() {
//            @Override
//            public Double[] reusableSample() throws SamplingFailureException {
//                return jointWrapper.reusableQueriedVarValues(innerSampler.reusableSample());
//            }
//        };

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
        SamplingUtils.save2DSamples(queriedVarsSampler, numSamples, SAMPLES_FILE_PATH + "scatter2D_stan" + numSamples);

    }
}
