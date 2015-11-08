package hgm.poly.reports.sg.journal;

import hgm.asve.Pair;
import hgm.poly.Fraction;
import hgm.poly.Function;
import hgm.poly.PiecewiseExpression;
import hgm.poly.bayesian.AbstractGeneralBayesianGibbsSampler;
import hgm.poly.gm.DeterministicFactor;
import hgm.poly.gm.RichJointWrapper;
import hgm.poly.gm.SymbolicGraphicalModelHandler;
import hgm.poly.integral.OneDimFunction;
import hgm.poly.sampling.SamplerInterface;
import hgm.poly.sampling.frac.FractionalJointBaselineGibbsSampler;
import hgm.poly.vis.FunctionVisualizer;
import hgm.sampling.VarAssignment;
import org.junit.Test;

import java.util.*;

/**
 * Created by Hadi M Afshar.
 * Date: 10/22/15
 * Time: 9:13 PM
 */
public class RobotLocalizationModelTest {
    public static void main(String[] args) {
        RobotLocalizationModelTest instance = new RobotLocalizationModelTest();
        instance.testLikelihoodVisualization();
        instance.testPosteriorVisualization();
        instance.testSampling();

    }

    @Test
    public void testLikelihoodVisualization(){
        final RobotLocalizationModel model = new RobotLocalizationModel(1);
        final PiecewiseExpression<Fraction> density = model.makeMixture("x_0");
        System.out.println("density = " + density);


        final int varId = density.getFactory().getVarIndex("x_0");
        final Double[] varValues = new Double[density.getFactory().getAllVars().length];
        Arrays.fill(varValues, 2d); //actually just "d" should be instantiated



        FunctionVisualizer.visualize(new Function() {


            @Override
            public double evaluate(VarAssignment fullVarAssign) {
                varValues[varId] = fullVarAssign.getContinuousVar("x_0");
                return density.evaluate(varValues);
            }

            @Override
            public String[] collectContinuousVars() {
                return new String[]{"x_0"};
            }
        }, -15, 35, 0.01, "likelihood");
    }

    @Test
    public void testPosteriorVisualization(){
        int param = 2;
        RobotLocalizationModel bn = new RobotLocalizationModel(param);
        SymbolicGraphicalModelHandler handler = new SymbolicGraphicalModelHandler();
        Map<String, Double> evidence = new HashMap<String, Double>();

        //localization evidence: randomly choose evidence between 0 and 10. TODO is this a good strategy?


      evidence.put("x_0" , 3.1d);
      evidence.put("x_1" , 9.1d);
//      evidence.put("x_2" , 8d);


        List<String> query = new ArrayList<String>();
        for (int i = 0; i < param; i++) {
            query.add("d"); //todo is this good?
        }

        Pair<PiecewiseExpression<Fraction>, List<DeterministicFactor>> jointAndEliminatedStochasticVars =
                handler.makeJointAndEliminatedStochasticVars(bn, query, evidence);
        final PiecewiseExpression<Fraction> joint = jointAndEliminatedStochasticVars.getFirstEntry();
        List<DeterministicFactor> eliminatedStochasticVars = jointAndEliminatedStochasticVars.getSecondEntry();
        if (!eliminatedStochasticVars.isEmpty()) throw new RuntimeException("how possible?");
        double envelopeSafeZone = 0;//0.001;
        RichJointWrapper jointWrapper =
                new RichJointWrapper(joint, eliminatedStochasticVars, query, 0, 10, bn, evidence);
        System.out.println("jointWrapper.getAppropriateSampleVectorSize() = " + jointWrapper.getAppropriateSampleVectorSize());
        System.out.println("jointWrapper.getJoint().getScopeVars() = " + jointWrapper.getJoint().getScopeVars());
        System.out.println("jointWrapper.getJoint() = " + jointWrapper.getJoint());


        final int dIndex = joint.getFactory().getVarIndex("d");
        final Double[] assign = new Double[joint.getFactory().getAllVars().length];
        FunctionVisualizer.visualize(new OneDimFunction() {
            @Override
            public double eval(double var) {
                assign[dIndex] = var;
                return joint.evaluate(assign);
            }
        }, -5d, 15d, 0.05, "Posterior");
    }

    @Test
    public void testSampling(){
        int param = 1;
        RobotLocalizationModel bn = new RobotLocalizationModel(param);
        SymbolicGraphicalModelHandler handler = new SymbolicGraphicalModelHandler();
        Map<String, Double> evidence = new HashMap<String, Double>();

        //localization evidence: randomly choose evidence between 0 and 10. TODO is this a good strategy?

        evidence.put("x_0", 5d);
//        for (int p = 0; p < param; p++) {
//            evidence.put("x_"+p , AbstractGeneralBayesianGibbsSampler.randomDoubleUniformBetween(0, 10));
//        }

        List<String> query = new ArrayList<String>();
        for (int i = 0; i < param; i++) {
            query.add("d"); //todo is this good?
        }

        Pair<PiecewiseExpression<Fraction>, List<DeterministicFactor>> jointAndEliminatedStochasticVars =
                handler.makeJointAndEliminatedStochasticVars(bn, query, evidence);
        final PiecewiseExpression<Fraction> joint = jointAndEliminatedStochasticVars.getFirstEntry();
        List<DeterministicFactor> eliminatedStochasticVars = jointAndEliminatedStochasticVars.getSecondEntry();
        if (!eliminatedStochasticVars.isEmpty()) throw new RuntimeException("how possible?");
        double envelopeSafeZone = 0;//0.001;
        RichJointWrapper jointWrapper =
                new RichJointWrapper(joint, eliminatedStochasticVars, query, 0, 10, bn, evidence);
        System.out.println("jointWrapper.getAppropriateSampleVectorSize() = " + jointWrapper.getAppropriateSampleVectorSize());
        System.out.println("jointWrapper.getJoint().getScopeVars() = " + jointWrapper.getJoint().getScopeVars());
        System.out.println("jointWrapper.getJoint() = " + jointWrapper.getJoint());


        final int dIndex = joint.getFactory().getVarIndex("d");
        final Double[] assign = new Double[joint.getFactory().getAllVars().length];
        FunctionVisualizer.visualize(new OneDimFunction() {
            @Override
            public double eval(double var) {
                assign[dIndex] = var;
                return joint.evaluate(assign);
            }
        }, -5d, 15d, 0.05, "pstrr");

        SamplerInterface sampler = FractionalJointBaselineGibbsSampler.makeSampler(joint, 0, 10);
        for (int i=0; i<100; i++) {
            Double[] s = sampler.reusableSample();
            System.out.println("s = " + Arrays.toString(s));

        }
    }
}
