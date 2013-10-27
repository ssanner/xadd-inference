package hgm.asve.cnsrv.approxator.fitting;

import hgm.asve.cnsrv.factor.Factor;
import hgm.asve.cnsrv.factor.MockFactor;
import hgm.asve.cnsrv.factory.BaselineXaddFactorFactory;
import hgm.asve.cnsrv.factory.MockFactorFactory;
import hgm.utils.Function;
import hgm.utils.vis.GeneralVisualizationUtils;
import junit.framework.Assert;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.junit.Test;
import xadd.XADD;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 21/10/13
 * Time: 7:56 AM
 */
public class CurveFittingTest {
    public static void main(String[] args) throws Exception {
        CurveFittingTest instance = new CurveFittingTest();
//        instance.testFittingWithMock();
        instance.testFittingWithXaddFactors();
    }

    @Test
    public void nestedLoopsTest() {
        Assert.assertEquals("[[0]]", CurveFitting.toStringNestedLoop(CurveFitting.nestedLoops(1, 1)));
        Assert.assertEquals("[[0], [1], [2]]", CurveFitting.toStringNestedLoop(CurveFitting.nestedLoops(1, 3)));
        Assert.assertEquals("[[0, 0, 0]]", CurveFitting.toStringNestedLoop(CurveFitting.nestedLoops(3, 1)));
        Assert.assertEquals("[[0, 0, 0], [0, 0, 1], [0, 1, 1], [1, 1, 1]]", CurveFitting.toStringNestedLoop(CurveFitting.nestedLoops(3, 2)));
        Assert.assertEquals("[[0, 0], [0, 1], [0, 2], [0, 3], [1, 1], [1, 2], [1, 3], [2, 2], [2, 3], [3, 3]]", CurveFitting.toStringNestedLoop(CurveFitting.nestedLoops(2, 4)));
        Assert.assertEquals("[[0, 0, 0], [0, 0, 1], [0, 0, 2], [0, 1, 1], [0, 1, 2], [0, 2, 2], [1, 1, 1], [1, 1, 2], [1, 2, 2], [2, 2, 2]]",
                CurveFitting.toStringNestedLoop(CurveFitting.nestedLoops(3, 3)));
    }

    @Test
    public void testCalculateBasisFunctions() throws Exception {
        CurveFitting<MockFactor> curveFitting = new CurveFitting<MockFactor>(new MockFactorFactory());
        List<MockFactor> basisFunctions = curveFitting.calculateBasisFunctions(3, Arrays.asList("x", "y", "z"));
        Assert.assertEquals("[ONE, x, y, z, x.x, x.y, x.z, y.y, y.z, z.z, x.x.x, x.x.y, x.x.z, x.y.y, x.y.z, x.z.z, y.y.y, y.y.z, y.z.z, z.z.z]",
                basisFunctions.toString());
    }

    @Test
    public void testFittingWithMock() throws Exception {
        double lowerBound = -100d;
        double higherBound = 100d;
        int basisFunctionsMaxPower = 2;
        double regularizationCoefficient = 0.1;

        UniformRealDistribution distribution = new UniformRealDistribution(lowerBound, higherBound);
        int numSamples = 1000;
        List<String> variables = Arrays.asList(MockFactor.X, MockFactor.Y);
        Function target = new Function(variables) {
            @Override
            public double func(double... effectiveVars) {
                double x = effectiveVars[0];
                double y = effectiveVars[1];
                return x * x * x * x + (y + 3) * (y + 3);
            }
        };

        GeneralVisualizationUtils.plot(target, lowerBound, higherBound, "exact");

        final MockFactorFactory mockFactory = new MockFactorFactory();
        CurveFitting<MockFactor> fitting = new CurveFitting<MockFactor>(mockFactory);
        final List<MockFactor> basisFunctions = fitting.calculateBasisFunctions(basisFunctionsMaxPower, variables);
        List<Map<String, Double>> sampleVarAssignments = new ArrayList<Map<String, Double>>(numSamples);
        List<Double> targetVarAssignments = new ArrayList<Double>(numSamples);
        for (int i = 0; i < numSamples; i++) {
            Map<String, Double> sampleAssignment = new HashMap<String, Double>(variables.size());
            for (String var : variables) {
                double value = distribution.sample();
                sampleAssignment.put(var, value);
            }
            sampleVarAssignments.add(sampleAssignment);
            targetVarAssignments.add(target.calcTarget(sampleAssignment));
        }
//        RealMatrix designMatrix = fitting.designMatrix(basisFunctions, sampleVarAssignments);
//        System.out.println("designMatrix = " + designMatrix);
//        System.out.println("fitting.moorePenrosePseudoInverse() = " + fitting.moorePenrosePseudoInverse(designMatrix));

        final double[] weights = fitting.solveWeights(basisFunctions, sampleVarAssignments, targetVarAssignments, regularizationCoefficient);
        Assert.assertEquals(basisFunctions.size(), weights.length);
        for (int i = 0; i < weights.length; i++) {
            System.out.println("basisFunctions = " + basisFunctions.get(i) + " W: " + weights[i]);
        }

        Function estimatedFunction = new Function(variables) { //although it depends on other vars as well, they are not plottable

            @Override
            public double func(double... effectiveVars) {
                //reconstruct the map!
                Map<String, Double> varAssign = new HashMap<String, Double>(2);
                String[] varNames = getEffectiveVarNames();
                for (int i=0; i< varNames.length; i++) {
                    varAssign.put(varNames[i], effectiveVars[i]);
                }

                double totalValue = 0;
                for (int i=0; i<basisFunctions.size(); i++){
                    MockFactor basis = basisFunctions.get(i);
                    double basisValue = mockFactory.evaluate(basis, varAssign);
                    totalValue += (weights[i] * basisValue);
                }
                return totalValue;

            }
        };

        GeneralVisualizationUtils.plot(estimatedFunction, lowerBound, higherBound, "Approx");
    }

    @Test
    public void testFittingWithXaddFactors() throws Exception {
        double lowerBound = -100d;
        double higherBound = 100d;
        int basisFunctionsMaxPower = 2;
        double regularizationCoefficient = 0.1;

        UniformRealDistribution distribution = new UniformRealDistribution(lowerBound, higherBound);
        int numSamples = 1000;
        List<String> variables = Arrays.asList("x", "y");
        final BaselineXaddFactorFactory factory = new BaselineXaddFactorFactory(new XADD());


        Function target = new Function(variables) {
            @Override
            public double func(double... effectiveVars) {
                double x = effectiveVars[0];
                double y = effectiveVars[1];
                return x * x * x * x + (y + 3) * (y + 3);
            }
        };

        GeneralVisualizationUtils.plot(target, lowerBound, higherBound, "exact");

        CurveFitting<Factor> fitting = new CurveFitting<Factor>(factory);
        final List<Factor> basisFunctions = fitting.calculateBasisFunctions(basisFunctionsMaxPower, variables);
        List<Map<String, Double>> sampleVarAssignments = new ArrayList<Map<String, Double>>(numSamples);
        List<Double> targetVarAssignments = new ArrayList<Double>(numSamples);
        for (int i = 0; i < numSamples; i++) {
            Map<String, Double> sampleAssignment = new HashMap<String, Double>(variables.size());
            for (String var : variables) {
                double value = distribution.sample();
                sampleAssignment.put(var, value);
            }
            sampleVarAssignments.add(sampleAssignment);
            targetVarAssignments.add(target.calcTarget(sampleAssignment));
        }
//        RealMatrix designMatrix = fitting.designMatrix(basisFunctions, sampleVarAssignments);
//        System.out.println("designMatrix = " + designMatrix);
//        System.out.println("fitting.moorePenrosePseudoInverse() = " + fitting.moorePenrosePseudoInverse(designMatrix));

        final double[] weights = fitting.solveWeights(basisFunctions, sampleVarAssignments, targetVarAssignments, regularizationCoefficient);
        Assert.assertEquals(basisFunctions.size(), weights.length);
        for (int i = 0; i < weights.length; i++) {
            System.out.println("basisFunctions = " + basisFunctions.get(i) + " W: " + weights[i]);
        }

        Function estimatedFunction = new Function(variables) { //although it depends on other vars as well, they are not plottable

            @Override
            public double func(double... effectiveVars) {
                //reconstruct the map!
                Map<String, Double> varAssign = new HashMap<String, Double>(2);
                String[] varNames = getEffectiveVarNames();
                for (int i=0; i< varNames.length; i++) {
                    varAssign.put(varNames[i], effectiveVars[i]);
                }

                double totalValue = 0;
                for (int i=0; i<basisFunctions.size(); i++){
                    Factor basis = basisFunctions.get(i);
                    double basisValue = factory.evaluate(basis, varAssign);
                    totalValue += (weights[i] * basisValue);
                }
                return totalValue;

            }
        };

        GeneralVisualizationUtils.plot(estimatedFunction, lowerBound, higherBound, "Approx");
    }


}
