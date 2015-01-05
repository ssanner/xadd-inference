package hgm.poly.reports.sg;

import hgm.asve.Pair;
import hgm.poly.Fraction;
import hgm.poly.PiecewiseExpression;
import hgm.poly.PolynomialFactory;
import hgm.poly.gm.*;
import hgm.poly.sampling.SamplerInterface;
import hgm.poly.sampling.SamplingUtils;
import hgm.poly.sampling.frac.FractionalJointRejectionSampler;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 1/12/14
 * Time: 3:05 PM
 */
public class EliminatedVarCompleterSamplerMakerTest {
    @Test
    public void test1(){
        GraphicalModel gm =
                ExperimentalGraphicalModels.makeCollisionModel(2, 0d, 3d, 0d, 3d, true);

        SymbolicGraphicalModelHandler handler = new SymbolicGraphicalModelHandler();
        Map<String, Double> evidence = new HashMap<String, Double>();

        evidence.put("p_t", 3d);        //todo...    ???
                evidence.put("m_1", 2d);
        evidence.put("v_2", 0.2d);

        List<String> query = new ArrayList<String>();//Arrays.asList("v_1", "m_1", "m_2"));
        query.add("v_2");
        query.add("p_t");
        query.add("v_t");
        query.add("m_t");

        System.out.println("evidence = " + evidence);
        System.out.println("query = " + query);

        Pair<PiecewiseExpression<Fraction>, List<DeterministicFactor>> jointAndEliminatedStochasticVars =
                handler.makeJointAndEliminatedStochasticVars(gm, query, evidence);
        PiecewiseExpression<Fraction> joint = jointAndEliminatedStochasticVars.getFirstEntry();
        List<DeterministicFactor> eliminatedStochasticVars = jointAndEliminatedStochasticVars.getSecondEntry();
        RichJointWrapper jointWrapper =
                new RichJointWrapper(joint, eliminatedStochasticVars, query, -10, 10, gm, evidence);
        System.out.println("jointWrapper.getAppropriateSampleVectorSize() = " + jointWrapper.getAppropriateSampleVectorSize());
//                System.out.println("jointWrapper.eliminatedStochasticVarFactors() = " + jointWrapper.eliminatedStochasticVarFactors());
        System.out.println("jointWrapper.getJoint().getScopeVars() = " + jointWrapper.getJoint().getScopeVars());
        System.out.println("jointWrapper.getJoint() = " + jointWrapper.getJoint());
        System.out.println("jointWrapper.getJoint().getScopeVars() = " + jointWrapper.getJoint().getScopeVars());
        System.out.println("jointWrapper.getJoint().getFactory().getAllVars() = " + Arrays.toString(jointWrapper.getJoint().getFactory().getAllVars()));


        JointToSampler innerJointToSampler = FractionalJointRejectionSampler.makeJointToSampler(1.0);
        SamplerInterface innerSampler = innerJointToSampler.makeSampler(jointWrapper);
        for (int i = 0; i < 20; i++) {
            Double[] s = innerSampler.reusableSample();
            System.out.println("s = " + Arrays.toString(s));
        }
        JointToSampler outerJointToSampler = new EliminatedVarCompleterSamplerMaker(innerJointToSampler);
        SamplerInterface outerSampler = outerJointToSampler.makeSampler(jointWrapper);
        for (int i = 0; i < 20; i++) {
            Double[] outerSample = outerSampler.reusableSample();
            System.out.println("outerSampler.reusableSample() = " + Arrays.toString(outerSample));
            System.out.println("jointWrapper.reusableQueriedVarValues(outerSample) = " + Arrays.toString(jointWrapper.reusableQueriedVarValues(outerSample)));

        }
    }

    @Test
    public void testExponentialProblem() throws FileNotFoundException {
        GraphicalModel gm = ExperimentalGraphicalModels.makeExponentialRelationshipModel(5, 0d, 1d, 0d, 1d);
        SymbolicGraphicalModelHandler handler = new SymbolicGraphicalModelHandler();
        Map<String, Double> evidence = new HashMap<String, Double>();
        evidence.put("z", 0d); //pr(z|x,y) = delta[y - x^n]

        List<String> query = new ArrayList<String>();
        query.add("x");
        query.add("y");

        Pair<PiecewiseExpression<Fraction>, List<DeterministicFactor>> jointAndEliminatedStochasticVars =
                handler.makeJointAndEliminatedStochasticVars(gm, query, evidence);
        PiecewiseExpression<Fraction> joint = jointAndEliminatedStochasticVars.getFirstEntry();
        List<DeterministicFactor> eliminatedStochasticVars = jointAndEliminatedStochasticVars.getSecondEntry();
        RichJointWrapper jointWrapper =
                new RichJointWrapper(joint, eliminatedStochasticVars, query, -2, 2, gm, evidence);

        JointToSampler innerJointToSampler = FractionalJointRejectionSampler.makeJointToSampler(1.0);
//        SamplerInterface innerSampler = innerJointToSampler.makeSampler(jointWrapper);
        JointToSampler outerJointToSampler = new EliminatedVarCompleterSamplerMaker(innerJointToSampler);
        SamplerInterface outerSampler = outerJointToSampler.makeSampler(jointWrapper);

        int numSamples = 10000;
        String SAMPLES_FILE_PATH = "D:/JAVA/IdeaProjects/proj2/test/hgm/poly/gm/";
        SamplingUtils.save2DSamples(outerSampler, numSamples, SAMPLES_FILE_PATH + "scatter2D");

//        for (int i = 0; i < 20; i++) {
//            Double[] outerSample = outerSampler.reusableSample();
//            System.out.println("jointWrapper.reusableQueriedVarValues(outerSample) = " + Arrays.toString(jointWrapper.reusableQueriedVarValues(outerSample)));
//        }
    }

    //todo this and most other test should be moved to Symbolic GM Handler model test...
    @Test
    public void testOnMultiplicativeDeterministicRelation() throws FileNotFoundException {
        // z = x*y
        GraphicalModel gm = makeMultiplicativeRelationModel(0d, 1d, 0d, 1d);
        SymbolicGraphicalModelHandler handler = new SymbolicGraphicalModelHandler();
        Map<String, Double> evidence = new HashMap<String, Double>();
        evidence.put("z", 0.3d); //pr(z|x,y) = delta[z - ab]

        List<String> query = new ArrayList<String>();
        query.add("x");
        query.add("y");

        Pair<PiecewiseExpression<Fraction>, List<DeterministicFactor>> jointAndEliminatedStochasticVars =
                handler.makeJointAndEliminatedStochasticVars(gm, query, evidence);
        PiecewiseExpression<Fraction> joint = jointAndEliminatedStochasticVars.getFirstEntry();
        System.out.println("joint = " + joint);
        List<DeterministicFactor> eliminatedStochasticVars = jointAndEliminatedStochasticVars.getSecondEntry();
        RichJointWrapper jointWrapper =
                new RichJointWrapper(joint, eliminatedStochasticVars, query, -2, 2, gm, evidence);

        JointToSampler innerJointToSampler = FractionalJointRejectionSampler.makeJointToSampler(20.0);
        JointToSampler outerJointToSampler = new EliminatedVarCompleterSamplerMaker(innerJointToSampler);
        SamplerInterface outerSampler = outerJointToSampler.makeSampler(jointWrapper);

        int numSamples = 1000;
        String SAMPLES_FILE_PATH = "D:/JAVA/IdeaProjects/proj2/test/hgm/poly/gm/";
        SamplingUtils.save2DSamples(outerSampler, numSamples, SAMPLES_FILE_PATH + "scatter2D");

//        for (int i = 0; i < 20; i++) {
//            Double[] outerSample = outerSampler.reusableSample();
//            System.out.println("jointWrapper.reusableQueriedVarValues(outerSample) = " + Arrays.toString(jointWrapper.reusableQueriedVarValues(outerSample)));
//        }
    }


    //z = xy
    public static GraphicalModel makeMultiplicativeRelationModel(Double xL, Double xH,
                                                                  Double yL, Double yH){
        String[] vars = new String[]{"x", "y", "z"};
        PolynomialFactory factory = new PolynomialFactory(vars);
        Distributions dBank = new Distributions(factory);
        BayesNetGraphicalModel bn = new BayesNetGraphicalModel();

        PiecewiseExpression<Fraction> fX = dBank.createUniformDistributionFraction("x", xL.toString(), xH.toString());
        PiecewiseExpression<Fraction> fY = dBank.createUniformDistributionFraction("y", yL.toString(), yH.toString());
        Fraction zF = factory.makeFraction("x^(1) * y^(1)");  //pr(z|x,y)  = delta[z - x*y] [potential constant]
        bn.addFactor(new StochasticVAFactor("x", fX));
        bn.addFactor(new StochasticVAFactor("y", fY));
        bn.addFactor(new DeterministicFactor("z", zF));

        return bn;

    }

}
