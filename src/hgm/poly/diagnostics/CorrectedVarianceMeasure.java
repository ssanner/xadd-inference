package hgm.poly.diagnostics;

/**
 * Created by Hadi Afshar.
 * Date: 3/09/14
 * Time: 10:24 PM
 */
public class CorrectedVarianceMeasure implements MeasureOnTheRun<Double[]> {

    double esm;  //expectation of sample means E[X]
    double esm2; // expectation of the square of sample means E[X^2]
    int sampleLength;

    int takenSamples = 0;

    public CorrectedVarianceMeasure() {
    }

    @Override
    public void addNewValue(Double[] sample) {
        if (takenSamples == 0) {
            sampleLength = sample.length;
            esm = 0;
            esm2 = 0;
        }

        takenSamples++;

        if (sample.length != sampleLength) throw new RuntimeException("size mismatch");

        double sm = arrMean(sample); //sample mean

//        esm = (esm * (takenSamples - 1) + sm) / (double) takenSamples;
//        esm2 = (esm2 * (takenSamples - 1) + sm*sm) / (double) takenSamples;

        esm += sm;
        esm2 += (sm * sm);

    }

    @Override
    public double computeMeasure() { //E[X^2] - E[X]^2
        double cesm = esm / ((double) takenSamples); //
        double cesm2 = (esm2) / ((double) takenSamples);
        double v = cesm2 - cesm * cesm;
        if (takenSamples < 2) {
            return v;
        }
        return (v * takenSamples) / ((double) takenSamples - 1.0);     //NOTE: (Bessel's correction) it is the corrected version by replacing n with n-1.
    }

    public double arrMean(Double[] arr) {
        double mean = 0.0;
        for (Double anArr : arr) {
            mean += anArr;
        }
        mean = mean / (double) arr.length;
        return mean;
    }


}
