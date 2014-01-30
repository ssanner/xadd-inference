package hgm.sampling.gibbs.report;

import hgm.asve.cnsrv.approxator.LeafThresholdXaddApproximator;
import hgm.preference.Preference;
import hgm.preference.PreferenceLearning;
import hgm.preference.PreferenceLearningTest;
import hgm.preference.db.DummyFeasiblePreferenceDatabase;
import hgm.preference.db.PreferenceDatabase;
import hgm.sampling.Sampler;
import hgm.sampling.SamplingFailureException;
import hgm.sampling.VarAssignment;
import hgm.sampling.gibbs.GibbsSampler;
import hgm.sampling.gibbs.GibbsSamplerWithCDFsPerSample;
import hgm.utils.vis.XaddVisualizer;
import junit.framework.Assert;
import org.junit.Test;
import xadd.XADD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Created by Hadi Afshar.
 * Date: 11/01/14
 * Time: 10:42 AM
 */
public class ReportGibbsSamplerWithCDFsPerSampleForPreferenceLearning {

    public static void main(String[] args) {
        ReportGibbsSamplerWithCDFsPerSampleForPreferenceLearning instance = new ReportGibbsSamplerWithCDFsPerSampleForPreferenceLearning();
//        instance.basicTest();
        instance.dummyFeasibleTest();

    }

    PreferenceDatabase testDB1 = new PreferenceDatabase() {
        Preference[] prefs = new Preference[]{
                new Preference(1, 2, Preference.Choice.FIRST),
                new Preference(1, 3, Preference.Choice.FIRST),
//                new Preference(2, 3, Preference.Choice.FIRST),
        };

        List<Double[]> items = new ArrayList<Double[]>(5);

        {
            items.add(new Double[]{0.0, 1.0});
            items.add(new Double[]{25.0, 4.5});
            items.add(new Double[]{30.0, 9.0});
            items.add(new Double[]{30.0, 16.0});
            items.add(new Double[]{40.0, 25.0});
        }

        @Override
        public int getNumberOfAttributes() {
            return items.get(0).length;
        }

        @Override
        public int getNumberOfItems() {
            return items.size();
        }

        @Override
        public List<Preference> getPreferenceResponses() {
            return Arrays.asList(prefs);
        }

        @Override
        public Double[] getItemAttributeValues(int itemId) {
            return items.get(itemId);
        }

        @Override
        public double[] getAuxiliaryWeightVector() {
            return null;
        }
    }; //end inner class.

    @Test
    public void basicTest() {
        XADD context = new XADD();
        PreferenceLearning learning = new PreferenceLearning(context, testDB1, 0.2, "w");

        // Pr(W | R^{n+1})
        XADD.XADDNode utilityWeights = learning.computePosteriorWeightVector(false);
        fixVarLimits(context, utilityWeights, -30d, 60d);

//        context.getGraph(context._hmNode2Int.get(utilityWeights)).launchViewer("test");
//        XaddVisualizer.visualize(utilityWeights, "test", context);


        //now I sample from it:
        Sampler sampler = new GibbsSamplerWithCDFsPerSample(context, utilityWeights);
        long t1 = System.currentTimeMillis();
        for (int i = 1; /*i < 10000*/ ; i++) {
            try {
                VarAssignment assign = sampler.sample();
            } catch (Exception e) {
                e.printStackTrace();
                sampler = new GibbsSamplerWithCDFsPerSample(context, utilityWeights);
            }
//            System.out.println("t = " + assign);
            if (i % 1000 == 0) {
                long t2 = System.currentTimeMillis();
                System.out.println("Time for taking 1000 samples: " + (t2 - t1) + "\tcontext._alOrder.size() = " + context._alOrder.size());
                t1 = t2;
            }
        }

    }


    @Test
    public void dummyFeasibleTest() {
        int numberOfItems = 200; // shouldn't have any significant effect (?) unless if its too small, dummy items will be repeated...
        int minDim = 4;
        int maxDim = 12;
        int minNumConstraints = 30;
        int maxNumConstraints = 50;
        double indicatorNoise = 0.1;
        boolean reduceLP = true;
        int numberOfSamplesFromEachDatabase = 1000;
        double relativeLeafValueBelowWhichRegionsAreTrimmed = 0.0001;

        System.out.println("#Dims|#Cnstrnts|Time:Posterior|#Nodes:posterior|Time:sampling(" + numberOfSamplesFromEachDatabase + "taken)");
        for (int numDims = minDim; numDims <= maxDim; numDims++) {
            for (int numConstraints = minNumConstraints; numConstraints <= maxNumConstraints; numConstraints++) {

                PreferenceDatabase db =
                        new DummyFeasiblePreferenceDatabase(-PreferenceLearning.C, PreferenceLearning.C, 0, 5, numConstraints, numDims, numberOfItems /*number of items*/);

                XADD context = new XADD();
                PreferenceLearning learning = new PreferenceLearning(context, db, indicatorNoise, "w");

                long time1start = System.currentTimeMillis();
                // Pr(W | R^{n+1})
                XADD.XADDNode posterior = learning.computePosteriorWeightVector(reduceLP, relativeLeafValueBelowWhichRegionsAreTrimmed);
                int posteriorNodeCount = posterior.collectNodes().size();
                long time2posteriorCalculated = System.currentTimeMillis();


                //This should only be used for visualization (???), not in sampling ?
                fixVarLimits(context, posterior, -10d, 10d);

                long time3posteriorReduced = System.currentTimeMillis();

                Sampler sampler = new GibbsSamplerWithCDFsPerSample(context, posterior, learning.generateAWeightVectorHighlyProbablePosteriorly());
//                long time4samplerInitialized = System.currentTimeMillis();

                for (int i = 0; i < numberOfSamplesFromEachDatabase; i++) {
                    VarAssignment assign = sampler.sample();
                    Double eval = context.evaluate(context._hmNode2Int.get(posterior), assign.getBooleanVarAssign(), assign.getContinuousVarAssign());
                    if (eval == null || eval <= 0.0) System.out.println("eval = " + eval);
                    Assert.assertTrue(eval > 0.0);
                }
                long time5samplesTaken = System.currentTimeMillis();

                // #Dims \t\t #Constraints \t\t time for 1. Posterior calc \t\t posterior nodes \t\t Elapsed time for 3. w_i - CDF calculated: \t\t Elapsed time for 4. sampling:
                System.out.println(numDims + " \t\t " + numConstraints + "\t\t\t" + (time2posteriorCalculated - time1start) + "\t\t (" + posteriorNodeCount +
                        ")\t\t" + //(time4samplerInitialized - time3posteriorReduced) +
                        "\t\t " + (time5samplesTaken - time3posteriorReduced));


            } // end numConstraints for
        } // end numDim for

    }

    private void fixVarLimits(XADD context, XADD.XADDNode root, double varMin, double varMax) {
        HashSet<String> vars = root.collectVars();
        for (String var : vars) {
            context._hmMinVal.put(var, varMin);
            context._hmMaxVal.put(var, varMax);
        }
    }

    /*abstract class FunctionStat {
        private int sampleCounter;
        private double valueSum;
        private double sqValueSum;

        protected FunctionStat() {
            reset();
        }

        public void addSample(VarAssignment assignment) {
            sampleCounter++;
            double v = value(assignment);
            valueSum += v;
            sqValueSum += v * v;
        }

        abstract double value(VarAssignment valuation);

        public double getMean() {
            return valueSum / (double) sampleCounter;
        }

        public double getVariance() {
            return sqValueSum / (double) sampleCounter;
        }

        public double getStandardErrorOfTheMean() {
            return Math.sqrt(getVariance()) / Math.sqrt(sampleCounter);
        }

        public void reset() {
            sampleCounter = 0;
            valueSum = 0d;
            sqValueSum = 0d;
        }
    }*/


}
