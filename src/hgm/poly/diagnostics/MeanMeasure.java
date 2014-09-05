package hgm.poly.diagnostics;

/**
 * Created by Hadi Afshar.
 * Date: 3/09/14
 * Time: 10:24 PM
 */
public class MeanMeasure implements MeasureOnTheRun<Double[]> {

    double[] runningAccumulatedSample;
    int sampleLength;

    int takenSamples = 0;

    public MeanMeasure() {
    }

    @Override
    public void addNewValue(Double[] sample) {
        if (takenSamples == 0) {
            sampleLength = sample.length;
            runningAccumulatedSample = new double[sampleLength];
        }

        if (sample.length != sampleLength) throw new RuntimeException("size mismatch");

        for (int i = 0; i < sampleLength; i++) {
            runningAccumulatedSample[i] = runningAccumulatedSample[i] + sample[i];
        }

        takenSamples++;
    }


    @Override
    public double computeMeasure() { //mean till now
           double rnMean = 0;  //running error
           for (int i = 0; i < sampleLength; i++) {
                rnMean += Math.abs((runningAccumulatedSample[i] / (double) takenSamples));
            }
            rnMean /= (double) sampleLength;
        return rnMean;
    }


}
