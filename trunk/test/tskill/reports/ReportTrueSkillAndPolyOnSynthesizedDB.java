package tskill.reports;

import hgm.preference.Preference;
import hgm.preference.PreferenceLearning;
import hgm.preference.db.DummyFeasiblePreferenceDatabase;
import hgm.preference.db.PreferenceDatabase;
import hgm.sampling.Sampler;
import hgm.sampling.VarAssignment;
import hgm.sampling.gibbs.GibbsSamplerWithCDFsPerSample;
import org.junit.Test;
import xadd.XADD;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 30/01/14
 * Time: 12:58 AM
 */
public class ReportTrueSkillAndPolyOnSynthesizedDB {
    @Test
    public void test1() {

    }

    public static void main(String[] args) {
//        ReportGibbsSamplerWithCDFsPerSampleForPreferenceLearning instance = new ReportGibbsSamplerWithCDFsPerSampleForPreferenceLearning();
//        instance.basicTest();
//        instance.dummyFeasibleTest();

    }

    /*PreferenceDatabase testDB1 = new PreferenceDatabase() {
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
        for (int i = 1; *//*i < 10000*//* ; i++) {
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
    */


    @Test
    public void dummyFeasiblePolyTest() {
        int numberOfItems = 500; // shouldn't have any significant effect (?) unless if its too small, dummy items will be repeated...
        int minDim = 2;
        int maxDim = 2;
        int minNumConstraints = 1;
        int maxNumConstraints = 30;
        double indicatorNoise = 0.0;
        boolean reduceLP = true;
        int numberOfSamplesFromEachDatabase = 10000;
        double relativeLeafValueBelowWhichRegionsAreTrimmed = -0.0001;
        int numberOfTestComparisonsPerDatabase = 5;
        Random random = new Random();

        System.out.println("#Dims|#Cnstrnts|Time:Posterior|#Nodes:posterior|Time:sampling(" + numberOfSamplesFromEachDatabase + "taken)");
        for (int numDims = minDim; numDims <= maxDim; numDims++) {
            for (int numConstraints = minNumConstraints; numConstraints <= maxNumConstraints; numConstraints++) {

                PreferenceDatabase completeDatabase =
                        new DummyFeasiblePreferenceDatabase(-PreferenceLearning.C, PreferenceLearning.C, 0d, 5d,
                                numConstraints + numberOfTestComparisonsPerDatabase /*more preferences used for testing*/,
                                numDims, numberOfItems /*number of items*/);

                PreferenceDatabase db = completeDatabase;//new PartialPreferenceDatabase(completeDatabase, numConstraints);

                XADD context = new XADD();
                PreferenceLearning learning = new PreferenceLearning(context, db, indicatorNoise, "w");

                long time1start = System.currentTimeMillis();
                // Pr(W | R^{n+1})
                XADD.XADDNode posterior = learning.computePosteriorWeightVector(reduceLP, relativeLeafValueBelowWhichRegionsAreTrimmed);
                fixVarLimits(context, posterior, -PreferenceLearning.C - 5, PreferenceLearning.C + 5);

//                XaddVisualizer.visualize(posterior, "W", context);

                int posteriorNodeCount = posterior.collectNodes().size();
                long time2posteriorCalculated = System.currentTimeMillis();


                //This should only be used for visualization (???), not in sampling ?

                long time3posteriorReduced = System.currentTimeMillis();

                Sampler sampler = new GibbsSamplerWithCDFsPerSample(context, posterior, learning.generateAWeightVectorHighlyProbablePosteriorly());

                List<Double[]> takenSamples = new ArrayList<Double[]>(numberOfSamplesFromEachDatabase);

                for (int i = 0; i < numberOfSamplesFromEachDatabase; i++) {
                    VarAssignment assign = sampler.sample();
                    Double eval = context.evaluate(context._hmNode2Int.get(posterior), assign.getBooleanVarAssign(), assign.getContinuousVarAssign());
                    if (eval == null || eval <= 0.0) System.err.println("eval = " + eval);
//                    Assert.assertTrue(eval > 0.0); todo .....................
                    takenSamples.add(assign.getContinuousVarAssignAsArray());
                }
                long time5samplesTaken = System.currentTimeMillis();

//                System.out.println("utilA = " + utilA + "\tutilB = " + utilB);

                double EPSILON = 0d;
                int timesAIsGreaterThanB = 0;
                int timesBIsGreaterThanA = 0;
                int timesAEqualsB = 0;

                int numLosses = 0;

                for (int testCounter = 0; testCounter < numberOfTestComparisonsPerDatabase; testCounter++) {
                    Preference testPref = completeDatabase.getPreferenceResponses().get(numConstraints + testCounter);
                    Integer aId = testPref.getItemId1();
                    Integer bId = testPref.getItemId2();
                    Double[] a = completeDatabase.getItemAttributeValues(aId);
                    Double[] b = completeDatabase.getItemAttributeValues(bId);

                    for (Double[] sampledW : takenSamples) {
//                        System.out.println("sampledW = " + Arrays.toString(sampledW));

                        double utilA = util(a, sampledW);
                        double utilB = util(b, sampledW);
                        if (utilA - utilB > EPSILON) {
                            timesAIsGreaterThanB++;
                        } else if (utilB - utilA > EPSILON) {
                            timesBIsGreaterThanA++;
                        } else timesAEqualsB++;
                    }

                    double maxCount = Math.max(timesAEqualsB, Math.max(timesAIsGreaterThanB, timesBIsGreaterThanA));
                    Preference.Choice predictedChoice;
                    if (maxCount == timesAIsGreaterThanB) predictedChoice = Preference.Choice.FIRST;
                    else if (maxCount == timesBIsGreaterThanA) predictedChoice = Preference.Choice.SECOND;
                    else predictedChoice = Preference.Choice.EQUAL;

                    if (!predictedChoice.equals(testPref.getPreferenceChoice())) {
                        System.out.println("predictedChoice = " + predictedChoice);
                        System.out.println("testPref = " + testPref.getPreferenceChoice());
                        numLosses++;

                    }

                }//end test for

                double averageLoss = numLosses / (double) numberOfTestComparisonsPerDatabase;
                System.out.println("averageLoss(" + numConstraints + ") = " + averageLoss);


                long time6testsDone = System.currentTimeMillis();

                // #Dims \t\t #Constraints \t\t time for 1. Posterior calc \t\t posterior nodes \t\t Elapsed time for 3. w_i - CDF calculated: \t\t Elapsed time for 4. sampling:
                System.out.println(numDims + " \t\t " + numConstraints + "\t\t\t" + (time2posteriorCalculated - time1start) + "\t\t (" + posteriorNodeCount +
                        ")\t\t" + //(time4samplerInitialized - time3posteriorReduced) +
                        "\t\t " + (time5samplesTaken - time3posteriorReduced) +
                        "\t\t" + (time6testsDone - time5samplesTaken));


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

    //sum_i a_i.b_i
    private double util(Double[] a, Double[] b) {
        double u = 0d;
        if (a.length != b.length) throw new RuntimeException("size mismatch");
        for (int i = 0; i < a.length; i++) {
            u += a[i] * b[i];
        }
        return u;
    }

}
