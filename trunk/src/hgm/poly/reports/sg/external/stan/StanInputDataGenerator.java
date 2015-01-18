package hgm.poly.reports.sg.external.stan;

import hgm.poly.reports.sg.external.anglican.AnglicanCodeGenerator;

import java.util.List;
import java.util.Map;

/**
 * Created by Hadi Afshar.
 * Date: 16/01/15
 * Time: 1:50 PM
 */
public class StanInputDataGenerator {

    /**
     * This data string should be placed in a folder where the following model exists:
     *
     * data {
     *      int<lower=0> N;
     *      real alpha; // lower bound
     *      real beta;  // upper bound
     *      real noise;
     *      real ap; // approx p_t
     * }
     * parameters{
     *      vector<lower=0,upper=3>[N] m;
     *      vector<lower=0,upper=3>[N] v;
     * }
     * model{
     *      real s;
     *      s <- 0;
     *      for (n in 1:N) {
     *          m[n] ~ uniform(alpha, beta);
     *          v[n] ~ uniform(alpha, beta);
     *          s <- s + m[n]*v[n];
     *      }
     *      ap ~ normal(s, noise);
     * }
     *
     * If the file name is MY_FOLDER/MY_FILE.stan then an executable file should be made out of it:
     *      make MY_FOLDER/MY_FILE.exe
     * from the cmdstan home folder. what is remained is to run:
     *      cd MY_FOLDER
     *      MY_FILE.exe sample data file=MY_FILE.data.R
     * where MY_FILE.data.R contains the data generated in this method:
     *
     */
    public static String makeStanCollisionInput(int n /*num colliding objects (param)*/,
                                                double muAlpha, double muBeta,
                                                double nuAlpha, double nuBeta,
                                                boolean symmetric,
                                                Map<String, Double> evidence) {



        //               (?)
        // m_1      v_1 ---->  v_n     m_2
        //   \__p_1__/        \__p_n__/
        //       \______p_t______/
        //
        // Example:
        // N <- 2
        // alpha <- 0.2
        // beta <- 2.2
        // noise <- 0.01
        // ap <- 3
        String end = System.lineSeparator();

        if (muAlpha != nuAlpha) throw new RuntimeException("at present time only muAlpha = nuAlpha is supported");
        if (muBeta != nuBeta) throw new RuntimeException("at present time only muBeta  = nuBeta  is supported");

        Double ap = evidence.get("p_t");
        if (evidence.size() != 1 || ap == null)
            throw new RuntimeException("at present only p_t is accepted as evidence");

        String s = "N <- " + n + end +
                "alpha <- " + muAlpha + end +
                "beta <- " + muBeta + end +
                "noise <- " + AnglicanCodeGenerator.EXTERNAL_MODEL_NOISE_PARAM + end +
                "ap <- " + ap + end;

        return s;
    }

}