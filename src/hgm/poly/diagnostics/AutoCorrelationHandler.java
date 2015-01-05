package hgm.poly.diagnostics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Hadi Afshar.
 * Date: 6/09/14
 * Time: 5:16 AM
 */
public class AutoCorrelationHandler {
    int maxWindowSize;
    List<Double[]> samples = new ArrayList<Double[]>();
    List<double[]> samplesAutoCovarianceInfo = new ArrayList<double[]>();
    double[] groundTruth; //ground truth mean
    int n; //number of points for which auto correlation is going to be calculated

    public AutoCorrelationHandler(double[] groundTruth, int maxWindowSize, int n) {
        this.groundTruth = groundTruth;
        this.maxWindowSize = maxWindowSize;

        this.n = n;
    }

    public void addNewValue(Double[] reusableSample) {
        Double[] sample = reusableSample.clone();
        if (sample.length != groundTruth.length) throw new RuntimeException("size mismatch");
        if (samples.size() > n + maxWindowSize) return; //the result for the first n samples is independent of other samples

        samples.add(sample);
        samplesAutoCovarianceInfo.add(new double[sample.length]);

        int currIndex = samples.size()-1;
        for (int i=Math.max(0, currIndex - maxWindowSize); i<currIndex; i++) {
             addToThisMinusGT(samplesAutoCovarianceInfo.get(i), sample);
        }
    }

    private void addToThisMinusGT(double[] ref, Double[] a) {
        for (int i = 0; i < ref.length; i++) {
             ref[i] += (a[i] - groundTruth[i]);
        }
    }

    double computeSummationOfAutoCorrelations(int n){
        if (n>samples.size()) throw new RuntimeException("n=" + n + "exceeds " + samples.size());

        double[] denominators = new double[groundTruth.length];
        double[] numerators = new double[groundTruth.length];
        for (int i=0; i<n; i++) {
            Double[] x_i = samples.get(i);
            double[] z_i = samplesAutoCovarianceInfo.get(i); //sum_{i=1}^{n-k}(x_{i+k} - \bar{x})
            for (int j=0; j<groundTruth.length; j++){
                double c = x_i[j] - groundTruth[j];
                denominators[j] = denominators[j] + c*c;
                numerators[j] = numerators[j] + c*z_i[j];
            }
        }

        double rho = 0;  //sum of all ro_k
        for (int j=0; j<groundTruth.length; j++){
            if (denominators[j] == 0) rho += 1; //0/0 = 1 (?)
            else {
              rho += (numerators[j]/denominators[j]);
            }
        }
        return rho;
    }

    public List<Double> computeEffectiveSampleSize() {
        Double[] result = new Double[n];
//        if (n>samples.size()) throw new RuntimeException("n=" + n + "exceeds " + samples.size());
        for (int nn=1;nn<=n;nn++){
            result[nn-1] = nn/(1 + 2*computeSummationOfAutoCorrelations(nn));
        }

        System.out.println("result = " + Arrays.toString(result));

        return Arrays.asList(result);
    }
}
