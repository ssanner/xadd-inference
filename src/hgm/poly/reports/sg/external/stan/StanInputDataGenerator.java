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
    static final String end = System.lineSeparator();

    /**
     * This data string should be placed in a folder where the following model exists:
     * <p/>
     * data {
     * int<lower=0> N;
     * real alpha; // lower bound
     * real beta;  // upper bound
     * real noise;
     * real ap; // approx p_t
     * }
     * parameters{
     * vector<lower=0,upper=3>[N] m;
     * vector<lower=0,upper=3>[N] v;
     * }
     * model{
     * real s;
     * s <- 0;
     * for (n in 1:N) {
     * m[n] ~ uniform(alpha, beta);
     * v[n] ~ uniform(alpha, beta);
     * s <- s + m[n]*v[n];
     * }
     * ap ~ normal(s, noise);
     * }
     * <p/>
     * If the file name is MY_FOLDER/MY_FILE.stan then an executable file should be made out of it:
     * make MY_FOLDER/MY_FILE.exe
     * from the cmdstan home folder. what is remained is to run:
     * cd MY_FOLDER
     * MY_FILE.exe sample data file=MY_FILE.data.R
     * where MY_FILE.data.R contains the data generated in this method:
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

        if (!symmetric) throw new RuntimeException("at the moment, only symmetric is supported");
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

    /**
     * The data text associated with this model is as follows and should be placed in the folder of examples
     * (as a .stan file) and then be 'made' from the CMD-STAN home folder.
     * <p/>
     * <p/>
     * data {
     * int<lower=0> N; // N resistors
     * real alpha; // resistor's distr. lower bound
     * real beta;  // resistor's distr. upper bound
     * real noise;
     * real ag; // approx g_t (the only evidence)
     * }
     * parameters{
     * vector<lower=alpha,upper=beta>[N] r;
     * }
     * model{
     * real g; //real g_t = sum_i 1/r_i
     * g <- 0;
     * for (n in 1:N) {
     * r[n] ~ uniform(alpha, beta);
     * g <- g + 1/r[n];
     * }
     * ag ~ normal(g, noise);
     * }
     */
    public static String makeStanResistorInput(int n /*num. resistors*/,
                                               Double alpha /*resistorLowerBound*/,
                                               Double beta  /*resistorUpperBound*/,
                                               Map<String, Double> evidence) {
        // Example:
        // N <- 5
        // alpha <- 9.5
        // beta <- 10.5
        // noise <- 0.2
        // ag <- 0.205

        Double ag = evidence.get("g_t");
        if (evidence.size() != 1 || ag == null)
            throw new RuntimeException("at present only g_t is accepted as evidence");

        return "N <- " + n + end +
                "alpha <- " + alpha + end +
                "beta <- " + beta + end +
                "noise <- " + AnglicanCodeGenerator.EXTERNAL_MODEL_NOISE_PARAM + end +
                "ag <- " + ag + end;

    }

    /*
    data {
	int<lower=1> N; // N resistors
	real alpha; // resistor's distr. lower bound
	real beta;  // resistor's distr. upper bound
	real noise;
	real aq; // approx q (ph) (the only evidence)
    }
    parameters{
	    vector<lower=alpha,upper=beta>[N] l; //todo... (should bounds be fixed??)
    }
    model{
	real q; //real q = average of l_i
	l[1] ~ uniform(alpha, beta);
	q <- l[1];
	for (n in 2:N) {
		l[n] ~ uniform(l[n-1], beta);
		q <- q + l[n];
	}
	q <- q/N; //making average
	aq ~ normal(q, noise);
}
     */
    public static String makeStanFermentationInput(int n, double alpha, double beta, Map<String, Double> evidence) {
       /*
        N <- 4
        alpha <- 0
        beta <- 1.0
        noise <- 0.2
        aq <- 0.2
        */
        Double aq = evidence.get("q");
        if (evidence.size() != 1 || aq == null)
            throw new RuntimeException("at present only q is accepted as evidence");

        return "N <- " + n + end +
                "alpha <- " + alpha + end +
                "beta <- " + beta + end +
                "noise <- " + AnglicanCodeGenerator.EXTERNAL_MODEL_NOISE_PARAM + end +
                "aq <- " + aq + end;

    }
}