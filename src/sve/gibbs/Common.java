package sve.gibbs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import xadd.XADD;

public class Common {

    public static String RESULT_PATH = "./src/prefs/results/";
    static String OUTPUT_FILEPATH = "./src/prefs/results/output_$.csv";
    static String PROBABILITIES_PATH = "./src/prefs/results/p_$.csv";
    static BufferedWriter _XADD_WRITER;
    static BufferedWriter _RESULTS_WRITER;
    static final boolean WRITE_XADDS = false;

    static void init() {
        try {
            String s = getDateTime();
            if (WRITE_XADDS)
                _XADD_WRITER = new BufferedWriter(new FileWriter("./src/prefs/results/xadd_output" + s + ".txt"));
            _RESULTS_WRITER = new BufferedWriter(new FileWriter("./src/prefs/results/results_" + s + ".txt"));
            OUTPUT_FILEPATH = OUTPUT_FILEPATH.replace("$", s);
            PROBABILITIES_PATH = PROBABILITIES_PATH.replace("$", "");
            System.out.println(getDateTime());
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
    }

    static int getJoint(Collection<Integer> xadds, XADD context) {
        int xadd = -1;
        for (int val : xadds) {
            if (xadd == -1) {
                xadd = val;
            } else {
                xadd = context.apply(xadd, val, XADD.PROD);
            }
        }

        return xadd;
    }

    static int integrate(String var, int xadd, XADD x, String high) {
        if (!(high == null || high.isEmpty())) {
            // We want to integrate with a restriction on high
            ArrayList l = new ArrayList();
            l.add(high);
            l.add(new ArrayList(Arrays.asList(new String[]{"[1.0]"})));
            l.add(new ArrayList(Arrays.asList(new String[]{"[0.0]"})));
            int constraint = x.buildCanonicalXADD(l);
            xadd = x.apply(xadd, constraint, XADD.PROD);
        }

        int bool_var_index = x.getBoolVarIndex(var);
        if (bool_var_index > 0) {
            // Sum out boolean variable
            int restrict_high = x.opOut(xadd, bool_var_index, XADD.RESTRICT_HIGH);
            int restrict_low = x.opOut(xadd, bool_var_index, XADD.RESTRICT_LOW);
            xadd = x.apply(restrict_high, restrict_low, XADD.SUM);
        } else {
            // Integrate out continuous variable
            xadd = x.computeDefiniteIntegral(xadd, var);
        }

        // xadd = x.reduce(xadd);
        // xadd = x.reduceLP(xadd);
        return xadd;
    }

    static void close() {
        try {
            if (_XADD_WRITER != null)
                _XADD_WRITER.close();

            if (_RESULTS_WRITER != null)
                _RESULTS_WRITER.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getDateTime() {
        Calendar c = Calendar.getInstance();
        return c.get(Calendar.YEAR) + "-" + c.get(Calendar.MONTH) + "-" + c.get(Calendar.DAY_OF_MONTH) + "-"
                + c.get(Calendar.HOUR) + "-" + c.get(Calendar.MINUTE) + "-" + c.get(Calendar.SECOND);
    }

    private static String writeTmpLikelihoods(String path, HashMap<String, ArrayList<Integer>> var2exp) {
        return writeTmpLikelihoods(path, var2exp, null);
    }

    public static String writeTmpLikelihoods(String path, HashMap<String, ArrayList<Integer>> var2exp, Double epsilon) {
        String tmpFileName = path.replace(".xadd", "_" + "tmp.xadd");
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            BufferedWriter bw = new BufferedWriter(new FileWriter(tmpFileName));
            String line = null;
            int idx;

            while ((line = br.readLine()) != null) {
                line = extractSumLine(var2exp, line, epsilon);

                bw.write(line + "\n");
            }

            bw.close();
            br.close();
        } catch (Exception ex) {
            System.err.println(ex.toString());
        }

        return tmpFileName;
    }

    static String extractSumLine(HashMap<String, ArrayList<Integer>> var2exp, String line) {
        return extractSumLine(var2exp, line, null);
    }

    static String extractSumLine(HashMap<String, ArrayList<Integer>> var2exp, String line, Double epsilon) {
        int idx;
        while ((idx = line.indexOf("sum")) != -1) {
            String subst = line.substring(idx);
            int start = subst.indexOf("(");
            int end = subst.indexOf(")");
            String pattern = subst.substring(start + 1, end);
            String newSubst = "(";
            for (String k : var2exp.keySet()) {
                for (int i = 0; i < var2exp.get(k).size(); i++) {
                    newSubst += pattern.replace(k, Integer.toString(var2exp.get(k).get(i)));
                    newSubst += i + 1 < var2exp.get(k).size() ? " + " : "";
                }
            }

            line = line.substring(0, idx) + newSubst + line.substring(idx + end);
        }
        if (epsilon != null) {
            line = line.replace("epsilon", epsilon.toString());
        }

        return line;
    }

    public static void println(String s, BufferedWriter bw) {
        try {
            bw.append(s + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void println(String[] s) {
        for (int i = 0; i < s.length; i++) {
            System.out.print(s[i]);
            if (i < s.length - 1) {
                System.out.print(",");
            }
        }
        System.out.println();
    }

    public static void println(String s) {
        System.out.println(s);
    }

    public static void println(Map m) {
        for (Object key : m.keySet()) {
            System.out.println(key + " > " + m.get(key));
        }
    }

    public static void println(Map m, BufferedWriter bw) {
        for (Object key : m.keySet()) {
            println(key + " > " + m.get(key), bw);
        }
    }

    public static void println(double[] d) {
        for (double dd : d) {
            System.out.println(dd);
        }
    }

    private static void println(HashMap<String, HashMap<Integer, String>> all, int lineCount, String filename) {
        try {
            String[] keys = all.keySet().toArray(new String[]{});
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename));

            for (int i = 0; i < keys.length; i++) {
                bw.write(keys[i]);
                if (i < keys.length - 1) {
                    bw.write(",");
                }
            }
            bw.write("\n");
            for (int l = 0; l < lineCount; l++) {
                for (int i = 0; i < keys.length; i++) {
                    HashMap<Integer, String> hm = all.get(keys[i]);
                    if (hm.containsKey(l)) {
                        bw.write(hm.get(l));
                    }
                    if (i < keys.length - 1) {
                        bw.write(",");
                    }
                }
                bw.write("\n");
            }
            bw.flush();
            bw.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Generate a range of values from start to end
     *
     * @param start
     * @param end
     * @param step
     * @return
     */
    static double[] generateRange(double start, double end, double step) {
        double range[] = new double[Double.valueOf((end - start) / step).intValue() + 1];
        int i = 0;
        for (double d = start; i < range.length; d += step) {
            range[i++] = d;
        }
        range[range.length - 1] = end;
        return range;
    }

    public static void flush() {
        try {
            if (Common.WRITE_XADDS && _XADD_WRITER != null)
                _XADD_WRITER.flush();

            if (_RESULTS_WRITER != null)
                _RESULTS_WRITER.flush();
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
    }

    public static void parseResults(String filename) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            String line;
            HashMap<String, HashMap<Integer, String>> all = new HashMap<String, HashMap<Integer, String>>();
            boolean wait_for_close = false;
            int line_number = 0;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("---") || wait_for_close) {
                    if (wait_for_close && line.startsWith("---")) {
                        line_number++;
                        wait_for_close = false;
                    } else {
                        if (!all.containsKey("1")) {
                            all.put("1", new HashMap<Integer, String>());
                        }
                        if (!all.get("1").containsKey(line_number)) {
                            all.get("1").put(line_number, "");
                        } else {
                            all.get("1").put(line_number, all.get("1").get(line_number) + "\n" + line);
                        }
                        wait_for_close = true;
                    }
                    continue;
                }

                if (line.startsWith("Time to")) {
                    if (line.contains("@"))
                        continue;

                    String[] s = line.split(":");
                    String key = s[0];
                    int idx = key.indexOf("for user");
                    if (idx > -1) {
                        key = key.substring(0, idx);
                    }
                    if (!all.containsKey(key)) {
                        all.put(key, new HashMap<Integer, String>());
                    }
                    all.get(key).put(line_number, s[1]);
                    // line_number++;
                    continue;
                }

                if (line.startsWith("# prefs")) {
                    String[] s = line.split(":");
                    String key = s[0].replace("#", "");
                    if (!all.containsKey(key)) {
                        all.put(key, new HashMap<Integer, String>());
                    }
                    all.get(key).put(line_number, s[s.length - 1]);
                    // line_number++;
                    continue;
                }

                if (line.contains("finished")) {
                    String[] s = line.split(":");
                    if (!all.containsKey("user")) {
                        all.put("user", new HashMap<Integer, String>());
                    }
                    all.get("user").put(line_number, s[0]);

                    if (!all.containsKey("epsilon")) {
                        all.put("epsilon", new HashMap<Integer, String>());
                    }
                    all.get("epsilon").put(line_number, s[1]);

                    if (!all.containsKey("experiment")) {
                        all.put("experiment", new HashMap<Integer, String>());
                    }
                    all.get("experiment").put(line_number, s[2]);

                    line_number++;
                    continue;
                }
            }

            println(all, line_number, filename.replace(".txt", ".csv"));

        } catch (Exception ex) {

        }
    }

    public static void main(String[] args) {
        Common.parseResults("./results/rresults_2012-10-10-6-13-33.txt"); // results_2012-10-13-6-43-46.txt // results_2012-10-13-3-12-3.txt
    }
}
