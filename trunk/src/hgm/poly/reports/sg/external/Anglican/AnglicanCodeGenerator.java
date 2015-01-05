package hgm.poly.reports.sg.external.Anglican;

import hgm.poly.reports.sg.external.ExternalMhSampleBank;

import java.io.*;
import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 28/11/14
 * Time: 12:00 AM
 */
public class AnglicanCodeGenerator {

    public static final String ANGLICAN_NOISE_PARAM = "anglicanNoiseParam";
    public static final String ANGLICAN_CODE_KEY = "anglican.code.key";

    public static enum AnglicanSamplingMethod {smc, rdb, ardb, pgibbs, cascade}

    public static boolean DEBUG = false;

    static final String ANGLICAN_JAR_NAME = "anglican-fe051d1fcdaa4b5355114c530eefb0f4c983f0b4-master.jar";
    static String ANGLICAN_DEFAULT_JAR_PATH = "E:\\WORK\\Anglican\\";

    public static ExternalMhSampleBank runAnglicanCode(String anglicanJarPath, String anglicanCode, int numSamples) throws InterruptedException, IOException {
        return runAnglicanCode(anglicanJarPath, anglicanCode, numSamples, AnglicanSamplingMethod.pgibbs);

    }
    public static ExternalMhSampleBank runAnglicanCode(String anglicanJarPath, String anglicanCode, final int numSamples, AnglicanSamplingMethod method) throws InterruptedException, IOException {
        // 1. persist the given anglican code in a file:
        PrintStream ps = new PrintStream(new FileOutputStream(anglicanJarPath + "/model_code.txt"));
        ps.print(anglicanCode);
        ps.close();

        // 2. run the code:
        final Process p = Runtime.getRuntime().exec(
                "java -jar " + anglicanJarPath + ANGLICAN_JAR_NAME + " -s " + anglicanJarPath + File.separator + "model_code.txt -n " + numSamples + " -m " + method);

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

                if (numParsedLines != numSamples) throw new RuntimeException("num. parsed lines: " + numParsedLines + " != num. desired samples: " + numSamples);

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
                ANGLICAN_NOISE_PARAM : observationNoise.toString();
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
        if (split.length != 2) throw new AnglicanParsingException("cannot parse: '" + split + "'");
        String varsStr = split[0].trim();
        if (!varsStr.startsWith("(list")) throw new AnglicanParsingException("cannot parse: '" + varsStr + "'");
        if (!varsStr.endsWith(")")) throw new AnglicanParsingException("cannot parse: '" + varsStr + "'");
        varsStr = varsStr.substring("(list".length(), varsStr.length()-1).trim();
        String[] vars = varsStr.split(" ");

        String valuesStr = split[1].trim();
        if (!valuesStr.startsWith("(")) throw new AnglicanParsingException("cannot parse: '" + valuesStr + "'");
        if (!valuesStr.endsWith(")")) throw new AnglicanParsingException("cannot parse: '" + valuesStr + "'");
        valuesStr = valuesStr.substring(1, valuesStr.length()-1).trim();
        String[] values = valuesStr.split(" ");

        int n = vars.length;
        if (n != values.length)
            throw new AnglicanParsingException("size mismatch! between " + Arrays.toString(vars) + " and " + Arrays.toString(values));

        Map<String, Double> varValues = new HashMap<String, Double>(n);
        for (int i=0; i< n; i++) {
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