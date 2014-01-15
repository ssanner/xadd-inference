package sve;

import graph.Graph;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import sve.GraphicalModel.Factor;

public class Sampling {

    private static class Distribution {
        public enum DistributionType {
            Normal, Uniform, Triangular, Boolean
        }

        ;

        public DistributionType dist_type;
        public String params;
        public double coef;
        public Distribution[] trueDist;
        public Distribution[] falseDist;

        public String toString() {
            return coef + " " + dist_type.toString() + " (" + params + ")";
        }
    }

    public static class Samples {
        ArrayList<HashMap<String, Double>> samples;
        HashMap<String, Distribution[]> varDists;
        double[] likelihoodWeights;
    }

    private static class Stats {
        public HashMap<String, Double> mean;
        public HashMap<String, Double> std;
        public HashMap<String, Double> se;

        public String toString() {
            return "Mean: " + Sampling.toString(mean) + "\nStd: " + Sampling.toString(std) + "\nStE: "
                    + Sampling.toString(se);
        }
    }

    private static String toString(Map m) {
        if (m == null)
            return "";

        StringBuilder sb = new StringBuilder();
        for (Object obj : m.keySet()) {
            sb.append("[" + obj.toString() + ": " + m.get(obj) + "] ");
        }

        return sb.toString();
    }

    private HashMap<String, Distribution[]> _var_distributions;
    private static Random _RANDOM = new Random();
    private ArrayList<String> _alVariableOrder = null;

    public Sampling(GraphicalModel gm) {
        _var_distributions = new HashMap<String, Distribution[]>();
        Set<Entry<String, ArrayList>> variable_val = gm._hmVar2cptTemplate.entrySet();
        for (Entry<String, ArrayList> e : variable_val) {
            if (gm._alBVarsTemplate.contains(e.getKey()) || gm._alBVarsTemplate.contains(e.getKey().replace("'", ""))
                    || gm._alBVarsTemplate.contains(e.getKey().replace("1", "i"))) {
                Distribution d = new Distribution();
                d.dist_type = sve.Sampling.Distribution.DistributionType.Boolean;
                d.coef = 1.;
                ArrayList a = (ArrayList) e.getValue();
                d.params = a.size() == 1 ? a.get(0).toString() : ((ArrayList) a.get(1)).get(0).toString() + ","
                        + ((ArrayList) a.get(2)).get(0).toString();
                d.params = d.params.replace("[", "").replace("]", "");
                _var_distributions.put(e.getKey(), new Distribution[]{d});
            } else {
                _var_distributions.put(e.getKey(), parseDistribution(e.getValue()));
            }
        }
        Graph g = new Graph(/* directed */true, false, true, false);
        g.setBottomToTop(false);
        g.setMultiEdges(false);
        for (Factor f : gm._alFactors)
            g.addAllUniLinks(f._vars, f._vars);

        _alVariableOrder = (ArrayList<String>) g.computeBestOrder();
    }

    private static Distribution[] parseDistribution(List value) {
        ArrayList<Distribution> dst = new ArrayList<Sampling.Distribution>();
        // for (Object token : value) {
        if (value.size() == 1) {
            String t = value.get(0).toString();
            String coef = "";
            Distribution d = null;
            for (int i = 0; i < t.length(); ) {
                char c = t.charAt(i);
                if (c == 'N') {
                    d = new Distribution();
                    d.dist_type = sve.Sampling.Distribution.DistributionType.Normal;

                } else if (c == 'U') {
                    d = new Distribution();
                    d.dist_type = sve.Sampling.Distribution.DistributionType.Uniform;

                } else if (c == 'T') {
                    d = new Distribution();
                    d.dist_type = sve.Sampling.Distribution.DistributionType.Triangular;

                } else if (Character.isDigit(c) || c == '.') {
                    coef = coef + c;

                } else if (c == '[') {
                    System.out.println(t);
                    // d = new Distribution();
                    // d.dist_type =
                    // sve.Sampling.Distribution.DistributionType.Boolean;
                    // t = t.replace("[", "(");
                    // t = t.replace("]", ")");
                }

                if (d != null) {
                    d.coef = (coef.length() == 0 ? 1 : Double.parseDouble(coef));
                    int indx1, indx2;
                    String t1 = t.substring(i);
                    indx1 = t1.indexOf("(");
                    indx2 = t1.indexOf(")");
                    d.params = t1.substring(indx1 + 1, indx2);
                    i = i + (indx2 - indx1);
                    dst.add(d);
                    System.out.println(d);
                    d = null;
                    coef = "";
                }
                i++;
            }
        } else {
            Distribution d = new Distribution();
            d.dist_type = sve.Sampling.Distribution.DistributionType.Boolean;
            d.params = value.get(0).toString();
            d.coef = 1;
            d.trueDist = parseDistribution(Arrays.asList(value.get(1)));
            d.falseDist = parseDistribution(Arrays.asList(value.get(2)));
            dst.add(d);
        }
        // }

        return dst.toArray(new Distribution[]{});
    }

    public Samples generateSamples(final int n, Query q) {
        Samples ss = new Samples();
        ss.varDists = new HashMap<String, Sampling.Distribution[]>();
        for (String v : _alVariableOrder) {
            System.out.println(v);
            if (_var_distributions.containsKey(v)) {
                ss.varDists.put(v, _var_distributions.get(v));
            } else {
                for (String s : q._hmVar2Expansion.keySet()) {
                    int idx = v.indexOf("_");
                    String newVar = v.substring(0, idx + 1) + s;
                    int num = Integer.parseInt(v.substring(idx + 1));
                    if (_var_distributions.containsKey(newVar) || _var_distributions.containsKey(newVar + "'")) {
                        if (_var_distributions.containsKey(newVar + "'")) {
                            newVar = newVar + "'";
                            Distribution[] dists = _var_distributions.get(newVar).clone();
                            formatDistribution(s + "'", num, dists);
                            num--;
                        }
                        Distribution[] dists = _var_distributions.get(newVar).clone();
                        formatDistribution(s, num, dists);
                        // for (Distribution dd : dists) {
                        // dd.params = dd.params.replace("_" + s, "_" + num);
                        // }
                        ss.varDists.put(v, dists);
                        break;
                    }
                }
                // System.out.println(v + " " + _var_distributions.get(v));
            }
            // for (String s : q._hmVar2Expansion.keySet()) {
            // if (v.contains("_" + s)) {
            // ArrayList val = q._hmVar2Expansion.get(s);
            // for (int i = 0; i < val.size(); i++) {
            // Distribution[] d = _var_distributions.get(v).clone();
            // for (Distribution dd : d) {
            // dd.params = dd.params.replace("_" + s, "_" + val.get(i));
            // }
            //
            // ss.varDists.put(v.replace("_" + s, "_" + val.get(i)), d);
            // }
            // }
            // }
            // if (!v.contains("_")) {
            // ss.varDists.put(v, _var_distributions.get(v));
            // }
        }

        ss.samples = new ArrayList<HashMap<String, Double>>();
        for (int i = 0; i < n; i++) {
            ss.samples.add(sample(ss.varDists));
        }

        Stats stat = generateStats(ss);
        System.out.println(stat);

        writeSamples(ss);

        likelihoodWeighting(ss, q);

        return ss;
    }

    private Stats generateStats(Samples ss) {
        Stats stats = new Stats();
        stats.mean = new HashMap<String, Double>();
        for (HashMap<String, Double> hm : ss.samples) {
            for (String key : hm.keySet()) {
                double val = hm.get(key);
                if (stats.mean.containsKey(key)) {
                    val += stats.mean.get(key);
                }
                stats.mean.put(key, val);
            }
        }
        for (String key : stats.mean.keySet()) {
            stats.mean.put(key, stats.mean.get(key) / (double) ss.samples.size());
        }

        stats.std = new HashMap<String, Double>();
        for (HashMap<String, Double> hm : ss.samples) {
            for (String key : hm.keySet()) {
                double val = hm.get(key) - stats.mean.get(key);
                val = val * val;
                if (stats.std.containsKey(key)) {
                    val += stats.std.get(key);
                }
                stats.std.put(key, val);
            }
        }
        for (String key : stats.std.keySet()) {
            stats.std.put(key, Math.sqrt(stats.std.get(key) / (double) ss.samples.size()));
        }

        stats.se = new HashMap<String, Double>();
        for (String key : stats.std.keySet()) {
            stats.se.put(key, 1.96 * stats.std.get(key) / Math.sqrt(ss.samples.size()));
        }

        return stats;
    }

    private void writeSamples(Samples ss) {
        PrintStream p;
        try {
            p = new PrintStream(new File("./out.csv"));
            for (HashMap<String, Double> hm : ss.samples) {
                int i = 0;
                for (Double d : hm.values()) {
                    p.print(d);
                    i++;
                    if (i < hm.size()) {
                        p.print(",");
                    }
                }
                p.println();
            }
            p.flush();
            p.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void formatDistribution(String s, int num, Distribution[] dists) {
        if (dists == null)
            return;

        for (Distribution dd : dists) {
            dd.params = dd.params.replace("_" + s, "_" + num);
            formatDistribution(s, num, dd.trueDist);
            formatDistribution(s, num, dd.falseDist);
        }
    }

    public double likelihoodWeightExpectation(Samples s, String[] queryVar) {
        // double[] w = likelihoodWeighting(s);
        double[] w = s.likelihoodWeights;
        double z = 0, zn = 0;
        for (int i = 0; i < w.length; i++) {
            if (w[i] >= 0) {
                Double d = s.samples.get(i).get(queryVar[0]);
                if (d == null) {
                    d = s.samples.get(i).get(queryVar[0] + "'");
                }
                z += Math.abs(w[i]) * d;
                zn += Math.abs(w[i]);
            }
        }

        return z / zn;
    }

    public double likelihoodWeightProbability(Samples s) {
        // double[] w = likelihoodWeighting(s, q);
        double[] w = s.likelihoodWeights;
        double z = 0, zn = 0;
        for (int i = 0; i < w.length; i++) {
            if (w[i] >= 0) {
                z += Math.abs(w[i]);
                zn += Math.abs(w[i]);
            } else {
                zn += 1.;
            }
        }

        return z / zn;
    }

    private double[] likelihoodWeighting(Samples s, Query q) {
        s.likelihoodWeights = new double[s.samples.size()];
        final double epsilon = .5;

        for (int i = 0; i < s.samples.size(); i++) {
            s.likelihoodWeights[i] = 1;
            HashMap<String, Double> x = s.samples.get(i);
            boolean changed = false;
            for (String xi_key : x.keySet()) {
                if ((q._hmCVarAssign.containsKey(xi_key) && Math.abs(x.get(xi_key) - q._hmCVarAssign.get(xi_key)) < epsilon)
                        || ((q._hmBVarAssign.containsKey(xi_key) && (x.get(xi_key) > 0 == Boolean
                        .parseBoolean(q._hmBVarAssign.get(xi_key).toString()))) || (q._hmBVarAssign
                        .containsKey(xi_key.replace("'", "")) && (x.get(xi_key) > 0 == Boolean
                        .parseBoolean(q._hmBVarAssign.get(xi_key.replace("'", "")).toString()))))) {
                    s.likelihoodWeights[i] = Math.abs(s.likelihoodWeights[i])
                            * evaluate(xi_key, s.varDists, x.get(xi_key), x); // .get(xi_key)
                    changed = true;
                } else {
                    s.likelihoodWeights[i] = changed ? s.likelihoodWeights[i] : -1;
                }
            }
        }

        return s.likelihoodWeights;
    }

    private double evaluate(String var, HashMap<String, Distribution[]> var_dists, double x,
                            HashMap<String, Double> vals) {
        double s = 0;
        for (Distribution d : var_dists.get(var)) {
            double[] params = correctParams(var, d.params.replace(var, Double.toString(x)).split(","), vals, var_dists);
            if (d.dist_type == sve.Sampling.Distribution.DistributionType.Normal) {
                s += evaluateNormal(params[0], params[1], params[2]);
            } else if (d.dist_type == sve.Sampling.Distribution.DistributionType.Uniform) {
                s += evaluateUniform(params[2], params[3]);
            } else if (d.dist_type == sve.Sampling.Distribution.DistributionType.Triangular) {
                s += evaluateTriangular(params[0], params[1], params[2], params[3]);
            } else if (d.dist_type == sve.Sampling.Distribution.DistributionType.Boolean) {
                s += params.length == 1 ? evaluateBoolean(x, params[0]) : evaluateBoolean(x, params[0], params[1]);
            }

        }
        return s;
    }

    private static double evaluateBoolean(double x, double d) {
        return x > .5 ? d : 1 - d;
    }

    private static double evaluateBoolean(double x, double a, double b) {
        return x > .5 ? a : b;
    }

    private static double evaluateTriangular(double x, double a, double c, double b) {
        if (x >= a && x < c) {
            return 2. * (x - a) / ((b - a) * (c - a));
        } else if (x == c) {
            return 2. / (b - a);
        } else if (x <= b && x > c) {
            return 2. * (b - x) / ((b - a) * (b - c));
        }

        return 0;
    }

    private static double evaluateUniform(double a, double b) {
        return 1. / (b - a);
    }

    private static double evaluateNormal(double x, double mu, double var) {
        return (1. / Math.sqrt(var * var * 2. * Math.PI)) * Math.exp(-(x - mu) * (x - mu) / (2. * var * var));

    }

    private static HashMap<String, Double> sample(HashMap<String, Distribution[]> var_dists) {
        HashMap<String, Double> hm = new HashMap<String, Double>();
        for (String s : var_dists.keySet()) {
            sample(s, hm, var_dists);
        }

        return hm;
    }

    private static Double sample(final String var_name, HashMap<String, Double> evidence_vals,
                                 HashMap<String, Distribution[]> var_dists) {
        Distribution[] distributions = var_dists.get(var_name);
        if (distributions == null || distributions.length == 0) {
            distributions = var_dists.get(var_name + "'");
        }

        double val = sample(var_name, evidence_vals, var_dists, distributions);
        return val;
    }

    private static double sample(final String var_name, HashMap<String, Double> evidence_vals,
                                 HashMap<String, Distribution[]> var_dists, Distribution[] distributions) {
        double component = _RANDOM.nextDouble();
        int componentIdx = -1;
        double s = 0;
        for (int i = 0; i < distributions.length; i++) {
            s += distributions[i].coef;
            if (component < s) {
                componentIdx = i;
                break;
            }
        }
        double val = 0.;
        double[] params = correctParams(var_name, distributions[componentIdx].params.split(","), evidence_vals,
                var_dists);
        if (distributions[componentIdx].dist_type == sve.Sampling.Distribution.DistributionType.Normal) {
            val = randomNormal(params[1], params[2]);
            // val = randomNormal2(params[1], params[3]);
        } else if (distributions[componentIdx].dist_type == sve.Sampling.Distribution.DistributionType.Uniform) {
            val = randomUniform(params[1], params[3] - params[2]);
        } else if (distributions[componentIdx].dist_type == sve.Sampling.Distribution.DistributionType.Triangular) {
            val = randomTriangular(params[1], params[1] - params[2], params[1] + params[3]);
        } else if (distributions[componentIdx].dist_type == sve.Sampling.Distribution.DistributionType.Boolean) {
            if (distributions[componentIdx].trueDist == null) {
                val = randomUniform(0, 1);
            } else {
                val = evaluateBoolean(params[0], 1, 0);
                if (val == 1) {
                    val = sample(var_name, evidence_vals, var_dists, distributions[componentIdx].trueDist);
                } else {
                    val = sample(var_name, evidence_vals, var_dists, distributions[componentIdx].falseDist);
                }
            }
        }

        // System.out.println("var_name:" + var_name + ", val: " + val +
        // ", evidence: " + evidence_vals);

        evidence_vals.put(var_name, val);
        return val;
    }

    private static double[] correctParams(String var_name, String[] params, HashMap<String, Double> evidence_vals,
                                          HashMap<String, Distribution[]> var_dists) {
        double[] d = new double[params.length];
        if (Character.isDigit(params[0].charAt(0))) {
            d[0] = Double.parseDouble(params[0]);
        } else if (Character.isLetter(params[0].charAt(0))) {
            if (evidence_vals != null && evidence_vals.containsKey(params[0])) {
                d[0] = evidence_vals.get(params[0]);
            } else if (params.length == 1) {
                double val = sample(params[0], evidence_vals, var_dists);
                d[0] = val;
            }
        }
        for (int i = 1; i < params.length; i++) {
            if (evidence_vals != null && evidence_vals.containsKey(params[i])) {
                d[i] = evidence_vals.get(params[i]);
            } else {
                if (!params[i].equalsIgnoreCase(var_name) && !Character.isDigit(params[i].charAt(0))) {
                    double val = sample(params[i], evidence_vals, var_dists);
                    d[i] = val;
                } else {
                    try {
                        d[i] = Double.parseDouble(params[i]);
                    } catch (Exception e) {
                    }
                }
            }
        }
        return d;
    }

    public static double randomNormal(double mu, double var) {
        return mu + var * _RANDOM.nextGaussian();
    }

    public static double randomNormal2(double mu, double width) {
        double var = Math.abs(normal2IntFunc(mu + width, width, mu) - normal2IntFunc(mu - width, width, mu));

        return mu + var * _RANDOM.nextGaussian();
    }

    private static double normal2IntFunc(double x, double w, double mu) {
        return 3.
                / (4. * Math.pow(w, 3))
                * (.33 * Math.pow(x, 3) * (w * w - 6 * mu * mu) + mu * x * x * (2. * mu * mu - w * w) - mu * mu * x
                * (mu * mu - w * w) + mu * Math.pow(x, 4) - .2 * Math.pow(x, 5));
    }

    // private static double normal2IntFunc(double x, double w, double mu) {
    // return /*
    // * 1. / (60. * Math.pow(w, 3)) *
    // */
    // (x * (-45. * Math.pow(mu, 4) + 90. * Math.pow(mu, 3) * x + 20. *
    // Math.pow(w,5) * x * x - 9. * Math.pow(x, 4) + 30. * mu
    // * mu * (2 * Math.pow(w,5) - 3 * x * x) + mu * (-60. * Math.pow(w,5) * x +
    // 45. * Math.pow(x, 3))))
    // / (60. * Math.pow(w, 3));
    // }

    public static double randomUniform(double mu, double var) {
        return mu + var * _RANDOM.nextDouble();
    }

    public static double randomTriangular(double mu, double a, double b) {
        // from: http://www.worldscibooks.com/etextbook/5720/5720_chap1.pdf
        // page: 8
        double u = _RANDOM.nextDouble();
        double val = 0;
        if (u < ((mu - a) / (b - a))) {
            val = a + Math.sqrt(u * (mu - a) * (b - a));
        } else {
            val = b - Math.sqrt((1 - u) * (b - mu) * (b - a));
        }

        // System.out.println(val);

        return val;
    }
}
