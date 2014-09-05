package hgm.poly.diagnostics;

/**
 * Created by Hadi Afshar.
 * Date: 3/09/14
 * Time: 10:24 PM
 */
public class AbsDifferenceMeasure implements MeasureOnTheRun<Double[]> {

    double[] groundTruthMeanVector;
    double[] runningAccumulatedSample;
    int sampleLength;

    int takenSamples = 0;

    public AbsDifferenceMeasure(double[] groundTruth) {
        this.groundTruthMeanVector = groundTruth;
        this.runningAccumulatedSample = new double[groundTruth.length];
        sampleLength = groundTruth.length;
    }

    @Override
    public void addNewValue(Double[] sample) {
        if (sample.length != sampleLength) throw new RuntimeException("size mismatch");

        for (int i = 0; i < sampleLength; i++) {
            runningAccumulatedSample[i] = runningAccumulatedSample[i] + sample[i];
        }

        takenSamples++;
    }


    @Override
    public double computeMeasure() { //sum_i (absolute difference of (average of taken_sample_i and ground_truth_i)):
           double runErr = 0;  //running error
           for (int i = 0; i < sampleLength; i++) {
                runErr += Math.abs((runningAccumulatedSample[i] / (double) takenSamples) - groundTruthMeanVector[i]);
            }
            runErr /= (double) sampleLength;
        return runErr;
    }


}
