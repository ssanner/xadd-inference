package hgm.preference.predict;

import hgm.asve.Pair;
import hgm.preference.Choice;
import hgm.preference.PreferenceLearning;
import hgm.preference.db.PreferenceDatabase;
import hgm.preference.predict.PreferenceLearningPredictor;
import hgm.sampling.Sampler;
import hgm.sampling.VarAssignment;
import hgm.sampling.gibbs.GibbsSamplerWithCDFsPerSample;
import xadd.XADD;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
* Created by Hadi Afshar.
* Date: 2/02/14
* Time: 5:32 AM
*/
public class PolytopePrefLearningPredictor implements PreferenceLearningPredictor {
  boolean DEBUG_MODE = true;
    private double indicatorNoise;
    private boolean reduceLP;
    private int numberOfSamples;
    private double relativeLeafValueBelowWhichRegionsAreTrimmed;
    private double epsilon;

    private List<Double[]> takenSamples;

    public PolytopePrefLearningPredictor(double indicatorNoise,
                                  boolean reduceLP,
                                  int numberOfSamples,
                                  double relativeLeafValueBelowWhichRegionsAreTrimmed,
                                  double epsilon) {
        this.indicatorNoise = indicatorNoise;
        this.reduceLP = reduceLP;
        this.numberOfSamples = numberOfSamples;
        this.relativeLeafValueBelowWhichRegionsAreTrimmed = relativeLeafValueBelowWhichRegionsAreTrimmed;
        this.epsilon = epsilon;
    }

    @Override
    public Info learnToPredict(PreferenceDatabase trainingDatabase) {
        Info info = new Info();

        XADD context = new XADD();
        PreferenceLearning learning = new PreferenceLearning(context, trainingDatabase, indicatorNoise, "w", epsilon);

        long time1start = System.currentTimeMillis();
        // Pr(W | R^{n+1})
        XADD.XADDNode posterior = learning.computePosteriorWeightVector(reduceLP, relativeLeafValueBelowWhichRegionsAreTrimmed);
        fixVarLimits(context, posterior, -PreferenceLearning.C, PreferenceLearning.C); //todo: do something better...

        info.add("#posteriorNodes", (double) posterior.collectNodes().size());

        long time2posteriorCalculated = System.currentTimeMillis();
        info.add(new Pair<String, Double>("T:posterior", (double) time2posteriorCalculated - time1start));

        //extra reduction phase.... long time3posteriorReduced = System.currentTimeMillis();

        Sampler sampler = new GibbsSamplerWithCDFsPerSample(context, posterior, learning.generateAWeightVectorHighlyProbablePosteriorly());

        takenSamples = new ArrayList<Double[]>(numberOfSamples);

        for (int i = 0; i < numberOfSamples; i++) {
            VarAssignment assign = sampler.sample();

            if (DEBUG_MODE) {
                Double eval = context.evaluate(context._hmNode2Int.get(posterior), assign.getBooleanVarAssign(), assign.getContinuousVarAssign());
                if (eval == null || eval <= 0.0) {
                    throw new RuntimeException("eval" + eval + "had to be > 0!");
                }
            }

            Double[] cAssign = assign.getContinuousVarAssignAsArray("w");
            takenSamples.add(cAssign);
        }
        long time5samplesTaken = System.currentTimeMillis();
        info.add(new Pair<String, Double>("T:sampling", (double) time5samplesTaken - time2posteriorCalculated));
        return info;
    }

    @Override
    public Choice predictPreferenceChoice(Double[] a, Double[] b) {
        Choice predictedChoice;

        int timesAIsGreaterThanB = 0;
        int timesBIsGreaterThanA = 0;
        int timesAEqualsB = 0;

        for (Double[] sampledW : takenSamples) {
            double utilA = util(a, sampledW);
            double utilB = util(b, sampledW);
            if (utilA - utilB > 0) {
                timesAIsGreaterThanB++;
            } else if (utilB - utilA > 0) {
                timesBIsGreaterThanA++;
            } else timesAEqualsB++;
        }

        double maxCount = Math.max(timesAEqualsB, Math.max(timesAIsGreaterThanB, timesBIsGreaterThanA));
        if (maxCount == timesAIsGreaterThanB) predictedChoice = Choice.FIRST;
        else if (maxCount == timesBIsGreaterThanA) predictedChoice = Choice.SECOND;
        else predictedChoice = Choice.EQUAL;

        return predictedChoice;
    }

    //sum_i a_i.b_i
    private double util(Double[] a, Double[] b) {
        double u = 0d;
        if (a.length != b.length) throw new RuntimeException("size mismatch");
        for (int i = 0; i < a.length; i++) {
            u += (a[i] * b[i]);
        }
        return u;
    }

    private void fixVarLimits(XADD context, XADD.XADDNode root, double varMin, double varMax) {
        HashSet<String> vars = root.collectVars();
        for (String var : vars) {
            context._hmMinVal.put(var, varMin);
            context._hmMaxVal.put(var, varMax);
        }
    }
}
