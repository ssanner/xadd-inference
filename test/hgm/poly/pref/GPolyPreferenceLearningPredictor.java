package hgm.poly.pref;

import hgm.asve.Pair;
import hgm.poly.vis.FunctionVisualizer;
import hgm.preference.Choice;
import hgm.preference.db.PreferenceDatabase;
import hgm.preference.predict.Info;
import hgm.preference.predict.PreferenceLearningPredictor;
import hgm.sampling.SamplingFailureException;
import hgm.sampling.VarAssignment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hadi Afshar.
 * Date: 16/03/14
 * Time: 3:04 AM
 */
public abstract class GPolyPreferenceLearningPredictor implements PreferenceLearningPredictor {
    boolean DEBUG_MODE = false;
    private double indicatorNoise;
    private int numberOfSamples;

    private List<Double[]> takenSamples;
    private int burnedSamples;
    private int maxGateConstraintViolation;

    public GPolyPreferenceLearningPredictor(double indicatorNoise,
                                            int numberOfSamples,
                                            int burnedSamples,
                                            int maxGateConstraintViolation) {
        this.indicatorNoise = indicatorNoise;
        this.numberOfSamples = numberOfSamples;
        this.burnedSamples =burnedSamples;
        this.maxGateConstraintViolation = maxGateConstraintViolation;
    }

    @Override
    public Info learnToPredict(PreferenceDatabase trainingDatabase) {
        Info info = new Info();

        BayesianPairwisePreferenceLearningModel learning = new BayesianPairwisePreferenceLearningModel(trainingDatabase, indicatorNoise/*, "w"*/);

        long time1start = System.currentTimeMillis();
        // Pr(W | R^{n+1})
        ConstantBayesianPosteriorHandler posterior = learning.computePosteriorWeightVector(maxGateConstraintViolation);

        if (DEBUG_MODE) {
            FunctionVisualizer.visualize(posterior, -50, 50, 0.1, "posterior");
        }

        long time2posteriorCalculated = System.currentTimeMillis();
        info.add(new Pair<String, Double>("T:posterior", (double) time2posteriorCalculated - time1start));

        //extra reduction phase.... long time3posteriorReduced = System.currentTimeMillis();

        GatedGibbsPolytopesSampler sampler = makeNewSampler(posterior, null);//learning.generateAWeightVectorHighlyProbablePosteriorly());

        takenSamples = new ArrayList<Double[]>(numberOfSamples);

        for (int i = 0; i < numberOfSamples; i++) {
            Double[] assign = sampler.reusableSample();

            if (DEBUG_MODE) {
                Double eval = posterior.evaluate(assign);
                if (eval == null || eval <= 0.0) {
                    throw new RuntimeException("eval" + eval + "had to be > 0!");
                }
            }

            takenSamples.add(assign);
        }

        long time5samplesTaken = System.currentTimeMillis();
        info.add(new Pair<String, Double>("T:sampling", (double) time5samplesTaken - time2posteriorCalculated));
        return info;
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

    public abstract GatedGibbsPolytopesSampler makeNewSampler(ConstantBayesianPosteriorHandler posterior, VarAssignment assignment);
    /*
    i.e. GatedPolytopesSampler sampler = GatedPolytopesSampler.makeGibbsSampler(
                minForAllVars,
                maxForAllVars, null);
    */

    @Override
    public Choice predictPreferenceChoice(Double[] a, Double[] b) {
        return predictPreferenceChoice(a, b, burnedSamples, takenSamples.size() - burnedSamples);  //note that 100 samples are burnt...
    }

    public Choice predictPreferenceChoice(Double[] a, Double[] b, int numberOfBurnedSamples, int numberOfSamplesTakenIntoAccount) {
        if (numberOfSamplesTakenIntoAccount + numberOfBurnedSamples > takenSamples.size()) throw new SamplingFailureException(
                "Out of bound exception: #Burned= " + numberOfBurnedSamples + "\t#effective samples= " + numberOfSamplesTakenIntoAccount + "\t exceeds: " + takenSamples.size());
        Choice predictedChoice;

        int timesAIsGreaterThanB = 0;
        int timesBIsGreaterThanA = 0;
        int timesAEqualsB = 0;

        for (int i = 0; i < numberOfSamplesTakenIntoAccount; i++) {
            Double[] sampledW = takenSamples.get(i + numberOfBurnedSamples); //I take the samples backward to increase the effect of sample burning
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

    @Override
    public double probabilityOfFirstItemBeingPreferredOverSecond(Double[] a, Double[] b) {
        return  probabilityOfFirstItemBeingPreferredOverSecond(a, b, burnedSamples, takenSamples.size() - burnedSamples);  //note that 100 samples are burnt...
    }

    public double probabilityOfFirstItemBeingPreferredOverSecond(Double[] a, Double[] b, int numberOfBurnedSamples, int numberOfSamplesTakenIntoAccount) {
        if (numberOfSamplesTakenIntoAccount + numberOfBurnedSamples > takenSamples.size()) throw new SamplingFailureException(
                "Out of bound exception: #Burned= " + numberOfBurnedSamples + "\t#effective samples= " + numberOfSamplesTakenIntoAccount + "\t exceeds: " + takenSamples.size());

        double n = 0;
        double sumProb = 0;

        for (int i = 0; i < numberOfSamplesTakenIntoAccount; i++) {
            n++;

            Double[] sampledW = takenSamples.get(i + numberOfBurnedSamples); //I take the samples backward to increase the effect of sample burning
            double utilA = util(a, sampledW);
            double utilB = util(b, sampledW);

            if (utilA == 0 && utilB==0) {
                System.err.println("both utils 0 in XADD.poly.P.L.Predict");
                sumProb += 0.5;
            } else {
                sumProb += (utilA/(utilA + utilB));
            }
        }

        return sumProb/n;
    }
}
