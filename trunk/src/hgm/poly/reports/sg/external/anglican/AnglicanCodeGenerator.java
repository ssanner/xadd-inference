package hgm.poly.reports.sg.external.anglican;

import hgm.poly.reports.sg.external.ExternalMhSampleBank;

import java.io.*;
import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 28/11/14
 * Time: 12:00 AM
 */
public class AnglicanCodeGenerator {

    //todo this field is general and should not be in Anglican (Stan uses is as well...)
    public static final String EXTERNAL_MODEL_NOISE_PARAM = "externalNoiseParam";

    public static final String ANGLICAN_CODE_KEY = "anglican.code.key";

    public static enum AnglicanSamplingMethod {smc, rdb, pgibbs, cascade, ardb} // ascade & ardb don't work

    public static boolean DEBUG = false;

    static final String ANGLICAN_JAR_NAME = "anglican-fe051d1fcdaa4b5355114c530eefb0f4c983f0b4-master.jar";
    static String ANGLICAN_DEFAULT_JAR_PATH = "E:\\WORK\\Anglican\\";

//    public static ExternalMhSampleBank runAnglicanCode(String anglicanJarPath, String anglicanCode, int numSamples) throws InterruptedException, IOException {
//        return runAnglicanCode(anglicanJarPath, anglicanCode, numSamples, AnglicanSamplingMethod.pgibbs);
//    }

    public static ExternalMhSampleBank runAnglicanCode(String anglicanJarPath, String anglicanCode, final int numSamples, AnglicanSamplingMethod method) throws InterruptedException, IOException {
        Random rand = new Random(System.currentTimeMillis());
        // 1. persist the given anglican code in a file:
        PrintStream ps = new PrintStream(new FileOutputStream(anglicanJarPath + "/model_code.txt"));
        ps.print(anglicanCode);
        ps.close();

        // 2. run the code:
        String command = "java -jar " + anglicanJarPath + ANGLICAN_JAR_NAME + " -s " + anglicanJarPath + File.separator + "model_code.txt -n " + numSamples + " -m " + method + " -r " + rand.nextInt(); //r for random seed
        System.out.println("command = " + command);
        final Process p = Runtime.getRuntime().exec(
                command);

        final ExternalMhSampleBank bank = new ExternalMhSampleBank();


        new Thread(new Runnable() {
            int numParsedLines = 0; // for debugging

            @Override
            public void run() {
                BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                String prevLine = "";

                long prevTime = System.nanoTime();
                try {
                    while ((line = input.readLine()) != null) {
                        numParsedLines++;
                        if (!line.equals(prevLine)) {
                            //new sample:
                            if (DEBUG) System.out.println(line + "*");
                            Map<String, Double> sampleAssignment = parseAnglicanListValuation(line);
//                            System.out.println("sampleAssignment = " + sampleAssignment);
                            bank.addNewParticle(sampleAssignment, System.nanoTime() - prevTime);
                        } else {
                            bank.addInstanceOfPreviousParticle(System.nanoTime() - prevTime);
                        }
                        prevTime = System.nanoTime();
                        prevLine = line;
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                if (numParsedLines != numSamples)
                    throw new RuntimeException("num. parsed lines: " + numParsedLines + " != num. desired samples: " + numSamples);

            }
        }).start();

        p.waitFor();

        return bank;
    }

    public static String makeAnglicanCollisionModel(int n /*num colliding objects*/,
                                                    Double muAlpha, Double muBeta,
                                                    Double nuAlpha, Double nuBeta,
                                                    boolean symmetric,
                                                    Map<String, Double> evidence,
                                                    Double observationNoise, //null if not known
                                                    List<String> queryVars) {
        //               (?)
        // m_1      v_1 ---->  v_n     m_2
        //   \__p_1__/        \__p_n__/
        //       \______p_t______/

        StringBuilder sbAnglican = new StringBuilder();
        StringBuilder sbTotalMomentum = new StringBuilder();
        String end = System.lineSeparator();
        for (int i = 0; i < n; i++) {
            sbAnglican.append("[assume m_").append(i + 1).append(" (uniform-continuous ").append(muAlpha).append(" ").append(muBeta).append(")]").append(end);

            if (symmetric) {
                sbAnglican.append("[assume v_").append(i + 1).append(" (uniform-continuous ").append(nuAlpha).append(" ").append(nuBeta).append(")]").append(end);
            } else {
                sbAnglican.append(i == 0 ?
                        ("[assume v_" + (i + 1) + " (uniform-continuous " + nuAlpha + " " + nuBeta + ")]" + end)
                        :
                        ("[assume v_" + (i + 1) + " (uniform-continuous " + nuAlpha + " v_" + i + ")]" + end));
            }

            if (i == 0) {
                sbTotalMomentum.append("(* m_1 v_1)");
            } else {
                sbTotalMomentum.insert(0, "(+ ");
                sbTotalMomentum.append(" (* m_").append(i + 1).append(" v_").append(i + 1).append("))");
            }
        }

        sbAnglican.append("[assume p_t ").append(sbTotalMomentum).append("]").append(end);

        String observationNoiseStr = observationNoise == null ?
                EXTERNAL_MODEL_NOISE_PARAM : observationNoise.toString();
        for (String observedVar : evidence.keySet()) {
            // a stupid hack to find deterministic variables
            if (observedVar.startsWith("p")) {
            sbAnglican.append("[observe (normal ").append(observedVar).append(" ").append(observationNoiseStr).append(") ").
                    append(evidence.get(observedVar)).append("]").append(end);
            } else {
//                sbAnglican.append("[observe ").append(observedVar).append(" ").append(evidence.get(observedVar)).append("]").append(end); //todo why it does not work???
                sbAnglican.append("[observe (normal ").append(observedVar).append(" ").append("0.01").append(") ").append(evidence.get(observedVar)).append("]").append(end);     //todo if this is a must then noise parameter should be a parameter....
            }
        }

        sbAnglican.append("[predict (list");
        for (String queryVar : queryVars) {
            sbAnglican.append(" ").append(queryVar);
        }
        sbAnglican.append(")]");

        return sbAnglican.toString();

    }

    public static String makeAnglicanFermentationModel(int n, double alpha, double beta, Map<String, Double> evidence, Double observationNoise //null if not known
            , List<String> queryVars) {
/*
        // l_1 --> l_2 --> ... --> l_n
        //  |       |            |
        //  \_______\____________\____q //average l_i

        String[] vars = new String[n + 1];
        for (int i = 0; i < n; i++) {
            vars[i] = "l_" + (i + 1);     // lactose at time step i
        }
        vars[n] = "q";


        PiecewiseExpression<Fraction>[] lactoseFs = new PiecewiseExpression[n];

        String averagePH = "";
        for (int i = 0; i < n; i++) {
            lactoseFs[i] = i == 0 ?
                    dBank.createUniformDistributionFraction("l_1", minLactoseAlpha.toString(), maxInitialLactoseBeta.toString())
                    : dBank.createUniformDistributionFraction("l_" + (i + 1), minLactoseAlpha.toString(), "l_" + i + "^(1)");
            averagePH += ("l_" + (i + 1) + "^(1) +");
        }
        averagePH = averagePH.substring(0, averagePH.length() - 1); //removing last "+"

        Fraction averagePhF = factory.makeFraction(averagePH, "" + n); // [l_1^(1) + ... + l_n^(1)]/[n]

        for (int i = 0; i < n; i++) {
            bn.addFactor(new StochasticVAFactor("l_" + (i + 1), lactoseFs[i]));
        }

        bn.addFactor(new DeterministicFactor("q", averagePhF));
        return bn;
 */
        String end = System.lineSeparator();
        StringBuilder sbAnglican = new StringBuilder("[assume l_1 (uniform-continuous " + alpha + " " + beta + ")]" + end);
        StringBuilder sbSumL = new StringBuilder("l_1");
        for (int i = 1; i < n; i++) {
                sbAnglican.append("[assume l_").append(i + 1).append(" (uniform-continuous ").
                        append(alpha).append(" l_").append(i).append(")]").append(end);
                sbSumL.insert(0, "(+ ");
                sbSumL.append(" l_").append(i + 1).append(")");

        }

        sbAnglican.append("[assume q (/ ").append(sbSumL).append(" ").append(n).append(")]").append(end); //average

        String observationNoiseStr = observationNoise == null ?
                EXTERNAL_MODEL_NOISE_PARAM : observationNoise.toString();
        for (String observedVar : evidence.keySet()) {
            sbAnglican.append("[observe (normal ").append(observedVar).append(" ").append(observationNoiseStr).append(") ").
                    append(evidence.get(observedVar)).append("]").append(end);
        }

        sbAnglican.append("[predict (list");
        for (String queryVar : queryVars) {
            sbAnglican.append(" ").append(queryVar);
        }
        sbAnglican.append(")]");

        return sbAnglican.toString();

    }

    public static String makeAnglicanResistorModel(int n, double resistMinVarLimit, double resistMaxVarLimit,
                                                   Map<String, Double> evidence, Double observationNoise, //null if not known
                                                   List<String> queryVars) {
        // r_1 ... r_n           g_i = 1/r_i
        //  \      /
        //    -------r_t
        StringBuilder sbAnglican = new StringBuilder();
        StringBuilder sbSumG = new StringBuilder("g_1"); //to be: g_1 + ... g_n
        String end = System.lineSeparator();
        for (int i = 0; i < n; i++) {
            sbAnglican.append("[assume r_").append(i + 1).append(" (uniform-continuous ").append(resistMinVarLimit).append(" ").append(resistMaxVarLimit).append(")]").append(end);
            sbAnglican.append("[assume g_").append(i + 1).append(" (/ 1 r_").append(i + 1).append(")]").append(end);

            if (i > 0) {
                sbSumG.insert(0, "(+ ");
                sbSumG.append(" g_").append(i + 1).append(")");
            }
        }

        sbAnglican.append("[assume g_t ").append(sbSumG).append("]").append(end);
        sbAnglican.append("[assume r_t (/ 1 g_t)]").append(end);

        String observationNoiseStr = observationNoise == null ?
                EXTERNAL_MODEL_NOISE_PARAM : observationNoise.toString();
        for (String observedVar : evidence.keySet()) {
            sbAnglican.append("[observe (normal ").append(observedVar).append(" ").append(observationNoiseStr).append(") ").
                    append(evidence.get(observedVar)).append("]").append(end);
        }

        sbAnglican.append("[predict (list");
        for (String queryVar : queryVars) {
            sbAnglican.append(" ").append(queryVar);
        }
        sbAnglican.append(")]");

        return sbAnglican.toString();
    }

    ///////////////////////
    //e.g. "(list a b),(110 -18)"
    public static Map<String, Double> parseAnglicanListValuation(String line) {
        String[] split = line.split(",");
        if (split.length != 2) throw new AnglicanParsingException("cannot parse: '" + Arrays.toString(split) + "'");
        String varsStr = split[0].trim();
        if (!varsStr.startsWith("(list")) throw new AnglicanParsingException("cannot parse: '" + varsStr + "'");
        if (!varsStr.endsWith(")")) throw new AnglicanParsingException("cannot parse: '" + varsStr + "'");
        varsStr = varsStr.substring("(list".length(), varsStr.length() - 1).trim();
        String[] vars = varsStr.split(" ");

        String valuesStr = split[1].trim();
        if (!valuesStr.startsWith("(")) throw new AnglicanParsingException("cannot parse: '" + valuesStr + "'");
        if (!valuesStr.endsWith(")")) throw new AnglicanParsingException("cannot parse: '" + valuesStr + "'");
        valuesStr = valuesStr.substring(1, valuesStr.length() - 1).trim();
        String[] values = valuesStr.split(" ");

        int n = vars.length;
        if (n != values.length)
            throw new AnglicanParsingException("size mismatch! between " + Arrays.toString(vars) + " and " + Arrays.toString(values));

        Map<String, Double> varValues = new HashMap<String, Double>(n);
        for (int i = 0; i < n; i++) {
            varValues.put(vars[i], Double.valueOf(values[i]));
        }

        return varValues;

    }
}

/*
public class AnglicanParser {
    public static void main(String[] args) throws IOException {
        parsePredictedList("E:/WORK/Anglican/out.txt");
    }

    private static void parsePredictedList(String anglicanOutputFile) throws IOException {
        ExternalMhSampleBank sampleBank = new ExternalMhSampleBank();
        BufferedReader br = new BufferedReader(new FileReader(anglicanOutputFile));
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                Map<String, Double> varValue = AnglicanCodeGenerator.parseAnglicanListValuation(line);
                sampleBank.addNewParticle(varValue);
                System.out.println("varValue = " + varValue);

                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            String everything = sb.toString();
            System.out.println("everything = " + everything);
        } finally {
            br.close();
        }

        System.out.println("sampleBank = " + sampleBank);
    }


}

*/