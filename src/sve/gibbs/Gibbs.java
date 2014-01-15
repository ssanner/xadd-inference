package sve.gibbs;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import sve.GraphicalModel;
import sve.GraphicalModel.Factor;
import xadd.XADD;
import xadd.ExprLib.ArithExpr;
import xadd.ExprLib.VarExpr;
import xadd.ExprLib.DoubleExpr;
import xadd.XADD.XADDNode;
import camdp.HierarchicalParser;

/**
 * @author eabbasnejad
 */
public abstract class Gibbs {

    protected static final String ORIGINAL = "_original";
    /**
     * Static values are set here
     */
    protected static final int SAMPLES_TO_IGNORE = 100;
    protected static final int SAMPLE_SIZE = 100;
    protected int _userId = -1;
    /**
     * Field values are set here
     */
    protected GraphicalModel _gm;
    protected Random _r = new Random();
    protected SortedMap<String, Integer> _varList = new TreeMap<String, Integer>();
    protected HashMap<String, ArrayList<Integer>> _var2exp = new HashMap<String, ArrayList<Integer>>();
    protected HashMap<String, Integer> _numer = new HashMap<String, Integer>(),
            _denom = new HashMap<String, Integer>();
    HashMap<String, ArrayList<Double>> _samples;
    protected PreferenceDataset _prefDs;

    protected int _likelihood;
    protected int _exactposterior;
    protected boolean _evaluate_exact;

    protected int _originalexact_posterior;
    protected int _node_count;
    protected int _node_count_exact;
    protected int _max_node_count;
    protected int _max_node_count_exact;
    private long _inference_time = 0;
    private long _inference_time_exact = 0;

    protected abstract int assignValues(int likelihood, int[] vals);

    protected abstract Boolean testOneExact(int[] vals);

    protected abstract Boolean testOne(int[] vals);

    protected abstract int noVariablesVarExp();

    /**
     * Constructor
     *
     * @param filename       : Name of the file containing the graphical model for the
     *                       prior
     * @param utilitpath     : Path to the file containing the definition of the utility
     *                       function
     * @param ds             : Dataset of the preferences contaning items, users,
     *                       preferences. Prefrences contains user|item1|item2 meaning that
     *                       item1 is prefered to item2 for the user
     * @param evaluate_exact : If the results are needed to be compared to the exact
     *                       expected utility this value has to be set to true so that the
     *                       true posterior is computed during infer function
     */
    public Gibbs(String filename, String utilitpath, PreferenceDataset ds,
                 boolean evaluate_exact) {
        init(filename, null, utilitpath, ds, evaluate_exact);
    }

    /**
     * Constructor
     *
     * @param filename       : Name of the file containing the graphical model for the
     *                       prior
     * @param utilitpath     : Path to the file containing the definition of the utility
     *                       function
     * @param ds             : Dataset of the preferences contaning items, users,
     *                       preferences. Prefrences contains user|item1|item2 meaning that
     *                       item1 is prefered to item2 for the user
     * @param evaluate_exact : If the results are needed to be compared to the exact
     *                       expected utility this value has to be set to true so that the
     *                       true posterior is computed during infer function
     */
    public Gibbs(String filename, String likelighoodPath, String utilityPath,
                 PreferenceDataset ds, boolean evaluate_exact) {
        init(filename, likelighoodPath, utilityPath, ds, evaluate_exact);
    }

    /**
     * Constructor calls this function. Same param values are sent here.
     *
     * @param filename
     * @param likelighoodPath
     * @param utilityPath
     * @param ds
     * @param evaluate_exact
     */
    protected void init(String filename, String likelighoodPath,
                        String utilityPath, PreferenceDataset ds, boolean evaluate_exact) {
        // Load the file, make the changes, update the param values, and
        // instantiate the GraphicalModel param
        _evaluate_exact = evaluate_exact;
        _gm = new GraphicalModel(filename);

        _prefDs = ds;
        _var2exp.put("i", new ArrayList<Integer>());
        for (int i = 1; i <= noVariablesVarExp(); i++) {
            _var2exp.get("i").add(i);
        }

        _gm.instantiateGMTemplate(_var2exp);

        // loading likelihood
        if (likelighoodPath != null) {
            // _epsilon = epsilon;
            String tmp = Common.writeTmpLikelihoods(likelighoodPath, _var2exp,
                    null);
            ArrayList l = HierarchicalParser.ParseFile(tmp);
            _likelihood = _gm._context.buildCanonicalXADD(l);
            // _gm._context.getGraph(likelihood).launchViewer();
        }
        int xadd = -1;
        try {
            // some comments for the log
            Common.println(
                    "------------------------------------------------------",
                    Common._RESULTS_WRITER);
            Common.println(Common.getDateTime(), Common._RESULTS_WRITER);
            Common.println("Prior path: " + filename, Common._RESULTS_WRITER);
            Common.println("Likelihood path: " + likelighoodPath,
                    Common._RESULTS_WRITER);
            Common.println("Utility path: " + utilityPath,
                    Common._RESULTS_WRITER);
            Common.println(
                    "Compute exact posterior: "
                            + Boolean.toString(_evaluate_exact),
                    Common._RESULTS_WRITER);
            Common.println("Sample size: " + SAMPLE_SIZE,
                    Common._RESULTS_WRITER);
            Common.println("Samples to ignore : " + SAMPLES_TO_IGNORE,
                    Common._RESULTS_WRITER);
            Common.println("Preference dataset: " + ds.getPreferenceFilepath(),
                    Common._RESULTS_WRITER);
            Common.println(" # items : " + ds.getItemsCount(),
                    Common._RESULTS_WRITER);
            Common.println(" # preferences : " + ds.getPreferencesCount(),
                    Common._RESULTS_WRITER);
            Common.println(
                    "------------------------------------------------------",
                    Common._RESULTS_WRITER);
            Common.flush();

            // select the factors for each variable
            for (String var : _gm._hsVariables) {
                for (Factor f : findCasesContains(var, _gm._alFactors)) {
                    xadd = f._xadd;
                    _varList.put(var, xadd);

                    if (Common.WRITE_XADDS)
                        Common.println(
                                var + ": After\n"
                                        + _gm._context.getString(xadd),
                                Common._XADD_WRITER);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getXADD(String var) {
        return _gm.getXADD(var, _gm._alBVarsTemplate.contains(var));
    }

    /**
     * Computes the joint probability of the samples
     *
     * @return
     */
    public double[] probabilities() {
        int xadd = Common.getJoint(_varList.values(), _gm._context);
        /*
		 * for (String key : _varList.keySet()) { if (xadd == -1) { xadd =
		 * _varList.get(key); } else { xadd = _gm._context.apply(xadd,
		 * _varList.get(key), XADD.PROD); } }
		 */

        double[] p = new double[SAMPLE_SIZE];
        HashMap<String, Double> assign = new HashMap<String, Double>();
        for (int i = 0; i < SAMPLE_SIZE; i++) {
            for (String key : _samples.keySet()) {
                assign.put(key, _samples.get(key).get(i));
            }
            p[i] = _gm._context.evaluate(xadd, null, assign);
        }

        CSVHandler.writecsv(Common.PROBABILITIES_PATH, p);

        return p;
    }

    public void prepareInference(int userID) {
        _userId = userID;
        SortedMap<String, Integer> pw = new TreeMap<String, Integer>();
        _inference_time_exact = _inference_time = System.currentTimeMillis();

        int prior = -1;
        if (_evaluate_exact) {
            _inference_time_exact = System.currentTimeMillis();
            prior = Common.getJoint(_varList.values(), _gm._context);
            _exactposterior = prior;
            _inference_time_exact = (System.currentTimeMillis() - _inference_time_exact);
            Common.println("Time to calculate prior: " + _inference_time_exact,
                    Common._RESULTS_WRITER);
        } else
            _exactposterior = -1;

        for (String key : _varList.keySet()) {
            pw.put(key, _varList.get(key)); // prior
        }

        long tmp_time = 0;
        int count = 0;
        for (int i = 0; i < _prefDs.getPreferencesCount(); i++) {
            // Common.println(" >> " + i);
            int l = assignValues(_likelihood, _prefDs.getPreference(i));
            if (l <= 0)
                continue;

            count++;
            if (_evaluate_exact) {
                long t = System.currentTimeMillis();
                _exactposterior = _gm._context.apply(_exactposterior, l,
                        XADD.PROD);
                _originalexact_posterior = _exactposterior;
                tmp_time += System.currentTimeMillis() - t;
            }

            // for the variables in the likelihood the product has to be made
            for (String key : _gm._context.collectVars(l)) { // _varList.keySet())
                // { //
                if (pw.containsKey(key)) {
                    int a = _gm._context.apply(pw.get(key), l, XADD.PROD);
                    pw.put(key, a);
                }
            }
        }

        _inference_time = System.currentTimeMillis() - _inference_time
                - tmp_time;
        _inference_time_exact = _inference_time_exact + tmp_time;
        Common.println("Time to apply likelihood for user " + _userId + ": "
                + _inference_time + " for " + count + " likelihoods");
        Common.println("Time to apply likelihood for user " + _userId + ": "
                + _inference_time + " for " + count + " likelihoods",
                Common._RESULTS_WRITER);

        for (String key : _varList.keySet()) {
            int a = pw.get(key);
            try {
                // a = _gm._context.reduceLP(a);
            } catch (Exception ex) {
            }
            pw.put(key, a);
        }

        // Let's load the xadds into a new one to save space if possible
        // This part only copies the current _gm._context into a new one
		/*
		 * println("** Preferences **", _XADD_WRITER); XADD x = new XADD(); for
		 * (String key : _varList.keySet()) { println(key + ":", _XADD_WRITER);
		 * String s = _gm._context.getString(pw.get(key), true, false); s =
		 * s.substring(1, s.length() - 2); println(s, _XADD_WRITER);
		 * _varList.put(key,
		 * x.reduce(x.buildCanonicalXADD(HierarchicalParser.ParseString(s)))); }
		 * 
		 * _gm._context = x;
		 */
        _varList = pw;

        Common.flush();

        // build the numerator and denominator for the CDF
        if (_numer.isEmpty() || _denom.isEmpty()) {
            Common.println("building cdf ...");
            buildCDF();
        }

        if (_evaluate_exact) {
            _node_count_exact = 0; // _gm._context.getNodeCount(_exactposterior);
            _max_node_count_exact = 0; // _node_count_exact;
            Common.println("Time to compute exact posterior: "
                    + _inference_time_exact, Common._RESULTS_WRITER);
        }
    }

    /**
     * Main inference (generating samples + exact posterior) is performed here
     */
    public void infer(Double epsilon) {
        _samples = new HashMap<String, ArrayList<Double>>();

        // _node_count = 0;
        // _max_node_count = 0;
        HashSet<XADDNode> tempHash = null;

        for (String key : _varList.keySet()) {
            HashMap<String, ArithExpr> subst = new HashMap<String, ArithExpr>();
            subst.put("epsilon", new DoubleExpr(epsilon));
            int a;
            a = _numer.get(key + ORIGINAL);
            a = _gm._context.substitute(a, subst);
            _numer.put(key, a);
            a = _denom.get(key + ORIGINAL);
            a = _gm._context.substitute(a, subst);
            _denom.put(key, a);
            // ---- node number evaluation starts
            XADDNode root = _gm._context._hmInt2Node.get(_numer.get(key));
            HashSet<XADDNode> t = root.collectNodes();
            if (tempHash == null)
                tempHash = t;
            else
                tempHash.addAll(t);

            if (t.size() > _max_node_count)
                _max_node_count = t.size();
            // ---- node number evaluation ends
            if (_evaluate_exact)
                _exactposterior = _gm._context.substitute(
                        _originalexact_posterior, subst);
        }
        _node_count = tempHash.size();

        // Generate samples from the current posteior and the xadd
        _inference_time += System.currentTimeMillis();
        generateSamples(SAMPLE_SIZE + SAMPLES_TO_IGNORE, false);
        _inference_time = (System.currentTimeMillis() - _inference_time);

        // keep the last sampleSize
        // if (i % 2 == 0) {
        for (String key : _samples.keySet()) {
            List<Double> d = _samples.get(key).subList(
                    Math.max(0, _samples.get(key).size() - SAMPLE_SIZE - 1),
                    _samples.get(key).size() - 1);
            _samples.put(key, new ArrayList<Double>());
            _samples.get(key).addAll(d);
        }
        // }

        CSVHandler.writecsv(Common.OUTPUT_FILEPATH.replace(".csv", "1.csv"),
                _samples);

        if (_evaluate_exact) {
            long t = System.currentTimeMillis();
            exactInfer();
            _inference_time_exact += (System.currentTimeMillis() - t);
        }

    }

    /**
     * The samples are generated here.
     *
     * @param sampleSize
     * @param append
     */
    private void generateSamples(int sampleSize, boolean append) {
        HashMap<String, ArithExpr> subst = new HashMap<String, ArithExpr>();

        try {
            for (String var : _varList.keySet()) {
                if (!_samples.containsKey(var)) {
                    _samples.put(var, new ArrayList<Double>());
                    _samples.get(var).add(0D);
                }
                subst.put(var, new DoubleExpr(0));
            }

            long time = System.currentTimeMillis();
            // the number of samples we want to generate
            ArrayList<String[]> ar = new ArrayList<String[]>();
            int counter = 0;
            String[] s = new String[_varList.keySet().size()];
            for (int i = 0; i < sampleSize; i++) {
                counter = 0;
                // System.out.println(i);
                for (String var : _varList.keySet()) {
                    // int num = assignVars(var, subst, _numer.get(var));
                    // int denum = assignVars(var, subst, _denom.get(var));
                    // double d = sample(var, num, denum);
                    double d = sample(var, _numer.get(var), _denom.get(var),
                            subst);
                    _samples.get(var).add(d);
                    subst.put(var, new DoubleExpr(d));
                    s[counter++] = Double.toString(d);
                }
                ar.add(s);
            }
            Common.println("Time to generate " + sampleSize + " samples: "
                    + (System.currentTimeMillis() - time),
                    Common._RESULTS_WRITER);
            CSVHandler.writecsv(Common.OUTPUT_FILEPATH, ar, append);
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace();
            System.exit(0);
        }
    }

    protected void exactInfer() {
        int temp = _exactposterior;
        for (String key : _varList.keySet()) {
            int t = _gm._context.getNodeCount(temp);
            _node_count_exact += t;
            temp = integrate(key, temp);
            if (t > _max_node_count_exact)
                _max_node_count_exact = t;
        }
        _node_count_exact += _gm._context.getNodeCount(temp);
        _exactposterior = temp;
    }

    /**
     * Build the cdf
     */
    private void buildCDF() {
        int temp;
        _numer.clear();
        _denom.clear();
        long time = System.currentTimeMillis();

        for (String var : _varList.keySet()) {
            Common.println(var);
            if (Common.WRITE_XADDS)
                Common.println(var, Common._XADD_WRITER);

            temp = _varList.get(var); // The xadd (cases) containing the
            // variable var

            if (Common.WRITE_XADDS)
                Common.println(_gm._context.getString(temp),
                        Common._XADD_WRITER);

            // Integrate the product of likelihood and the prior bounded on top
            // by var
            // So, first integrate with respect to a dummy variable t and then
            // replace
            // t with var
            int aa = integrate(var, temp, "[" + var + " < t" + "]");

            if (Common.WRITE_XADDS)
                // NOTE: Can now export XADDs if required for reading in later (exportXADDToFile)
                Common.println("After integratation of numerator:\n"
                        + _gm._context.getString(aa, true),
                        Common._XADD_WRITER);

            int numerator = replaceVar(aa, "t", var); // t is replaced here with
            // var
            _numer.put(var + ORIGINAL, numerator); // numerator is stored in the
            // hashmap

            if (Common.WRITE_XADDS)
                Common.println("After var replace in numerator:\n"
                        + _gm._context.getString(numerator, true),
                        Common._XADD_WRITER);

            int denominator = integrate(var, temp); // computing denominator
            // t is replaced here with the maximum ....
            // So it is not required to integrate again
            // int denominator = replaceVar(aa, "t", Double.toString(-1 *
            // ((DoubleExpr)
            // XADD.NEG_INFINITE)._dConstVal));
            _denom.put(var + ORIGINAL, denominator);

            if (Common.WRITE_XADDS)
                Common.println("After integration in denominator:\n"
                        + _gm._context.getString(denominator, true),
                        Common._XADD_WRITER);

        }

        Common.println("Time to build CDF: "
                + (System.currentTimeMillis() - time), Common._RESULTS_WRITER);
    }

    private void evaluate(int xadd, String var) {
        ArrayList<String[]> ar = new ArrayList<String[]>();
        String[] s;
        HashMap<String, Boolean> bool_assign = new HashMap<String, Boolean>();
        HashMap<String, Double> cont_assign = new HashMap<String, Double>();
        DecimalFormat df = new DecimalFormat("#.##");
        for (double x = -4; x < 4; x += .5) {
            s = new String[2];
            cont_assign.put(var, x);
            s[0] = df.format(x);
            Double d = _gm._context.evaluate(xadd, bool_assign, cont_assign);
            if (d != null) {
                s[1] = df.format(d);
                ar.add(s);
            }
        }
        CSVHandler.writecsv("./src/prefs/gaussian.csv", ar);
    }

    /**
     * For sampling, removes the given variable from the subst, substitutes it
     * in the xadd and return the new xadd
     *
     * @param var
     * @param subst
     * @param xadd
     * @return
     */
    private int assignVars(String var, HashMap<String, ArithExpr> subst,
                           int xadd) {
        // HashMap<String, ArithExpr> subst = new HashMap<String, ArithExpr>();
		/*
		 * subst.remove(var); double val = 0; for (String v : _samples.keySet())
		 * { if (!v.equals(var)) { // if (i - 1 < 0) { // val = 0; // } else {
		 * // val = samples.get(v).get(i - 1); // } val =
		 * _samples.get(v).get(i); subst.put(v, new XADD.DoubleExpr(val)); } }
		 * 
		 * // println(_gm._context.getString(xadd));
		 */
        HashMap<String, ArithExpr> s = (HashMap<String, ArithExpr>) subst
                .clone();
        s.remove(var);
        xadd = _gm._context.substitute(xadd, s);
        return xadd;
    }

    private int replaceVar(int node, String var1, String var) {
        HashMap<String, ArithExpr> subst = new HashMap<String, ArithExpr>();
        ArithExpr a = new VarExpr(var);
        subst.put(var1, a);
        return _gm._context.substitute(node, subst);
    }

    /**
     * Generates sample by binary search in the XADD Simply generate a uniform,
     * evaluate the function, adjust boundary for the function it sampled from
     *
     * @param num   : numerator of the CDF
     * @param denom : Denominator for the CDF
     */
    private double sample(String var, Integer num, Integer denom,
                          HashMap<String, ArithExpr> subst) {
        float u = _r.nextFloat();
        double high = 1, low = 0;
        HashMap<String, Double> assignment = new HashMap<String, Double>();

        for (String k : subst.keySet()) {
            if (!k.matches(var))
                assignment.put(k, ((DoubleExpr) subst.get(k))._dConstVal);
        }

        double val, d = _gm._context.evaluate(denom, null, assignment); // ((DoubleExpr)
        // ((XADDTNode)
        // _gm._context.getNode(denom))._expr)._dConstVal;

        int iteration = 0;
        while ((high - low > 1E-6 && low < high) && iteration++ < 500) { // The
            // value
            // to
            // be
            // adjusted
            assignment.put(var, (high + low) / 2);
            val = _gm._context.evaluate(num, null, assignment);
            val = val / d;

            if (val > u) {
                high = assignment.get(var);
            } else {
                low = assignment.get(var);
            }
        }

        return assignment.get(var);
    }

    /**
     * Integrate
     *
     * @param var
     * @param xadd
     * @return
     */
    protected int integrate(String var, int xadd) {
        return integrate(var, xadd, null);
    }

    /**
     * Integrate
     *
     * @param var
     * @param xadd
     * @param high
     * @return
     */
    private int integrate(String var, int xadd, String high) {
        // XADD x = new XADD();
        // String s = _gm._context.getString(xadd, true, false);
        // s = s.substring(1, s.length() - 2);
        // xadd = x.buildCanonicalXADD(HierarchicalParser.ParseString(s));
        // xadd = integrate(var, xadd, x, null);
        // s = x.getString(xadd, true, false);
        // s = s.substring(1, s.length() - 2);
        // xadd =
        // _gm._context.buildCanonicalXADD(HierarchicalParser.ParseString(s));

        return Common.integrate(var, xadd, _gm._context, high);
    }

    /**
     * Find factors containing the variable of choice
     *
     * @param var
     * @param factors
     * @return
     */
    private ArrayList<Factor> findCasesContains(String var,
                                                ArrayList<Factor> factors) {
        ArrayList<Factor> a = new ArrayList<Factor>();

        for (Factor f : factors) {
            if (f._vars.contains(var)) {
                a.add(f);
            }
        }

        return a;
    }

    /**
     * Generates some statistical values on the generated samples
     *
     * @return
     */
    protected HashMap<String, Double> samplesStat() {
        HashMap<String, Double> av = new HashMap<String, Double>();
        BufferedWriter bw;
        try {
            bw = new BufferedWriter(new FileWriter("var.csv"));

            for (String key : _samples.keySet()) {
                for (int i = 0; i < SAMPLE_SIZE; i++) {
                    if (av.containsKey(key)) {
                        av.put(key, av.get(key) + _samples.get(key).get(i));
                    } else {
                        av.put(key, _samples.get(key).get(i));
                    }
                }
                av.put(key, av.get(key) / (double) SAMPLE_SIZE);
            }

            for (String k : _samples.keySet()) {
                String varKey = k + "_var";
                for (int i = 0; i < SAMPLE_SIZE; i++) {
                    if (!av.containsKey(varKey)) {
                        av.put(varKey, 0D);
                    }
                    av.put(varKey,
                            av.get(varKey)
                                    + Math.pow(
                                    _samples.get(k).get(i) - av.get(k),
                                    2));
                    bw.append(varKey + "," + _samples.get(k).get(i) + ","
                            + Math.pow(_samples.get(k).get(i) - av.get(k), 2)
                            + ", " + av.get(varKey) + "\n");
                }
                av.put(varKey, av.get(varKey) / (double) SAMPLE_SIZE);
            }
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return av;
    }

    /**
     * Run test for the given dataset, i.e. compute expected utilities and
     * compare them with the true values and report the accuracy
     *
     * @param ds
     * @return
     */
    public double test(PreferenceDataset ds) {
        int correct = 0, wrong = 0;
        int correct_exact = 0, wrong_exact = 0, count = 0;
        Random r = new Random();
        double eu1, eu2, eu1_exact, eu2_exact, expected_time = 0, expected_exact_time = 0;
        long time;
        for (int i = 0; i < ds.getPreferencesCount(); i++) {
            int[] p = ds.getPreference(i);

            time = System.currentTimeMillis();
            Boolean b = testOne(p);
            if (b == null)
                continue;
            if (b)
                correct++;
            else
                wrong++;
            expected_time += (System.currentTimeMillis() - time);
            count++;

            if (_evaluate_exact) {
                time = System.currentTimeMillis();
                b = testOneExact(p);
                if (b == null)
                    continue;
                if (b)
                    correct_exact++;
                else
                    wrong_exact++;
                expected_exact_time += (System.currentTimeMillis() - time);
            }
        }

        Common.println("Time to evaluate one expected utility for user "
                + _userId + ": " + expected_time / (double) count,
                Common._RESULTS_WRITER);

        expected_time = expected_time / (double) count;
        expected_exact_time = expected_exact_time / (double) count;

        double percentage = (double) correct / (double) count;

        String s = samplesStat().toString();
        Common.println(s);
        Common.println(s, Common._RESULTS_WRITER);

        Common.println("user ID: " + _userId);

        s = "# prefs: " + count + ", # correct: " + correct + ", # wrong: "
                + wrong + " : " + percentage * 100. + "%, expected_time: "
                + expected_time + "%, inference_time: " + _inference_time
                + ", no_nodes: " + _node_count + ", max_no_nodes: "
                + _max_node_count;
        Common.println(s);
        Common.println(s, Common._RESULTS_WRITER);

        if (_evaluate_exact) {
            Common.println("Time to evaluate one expected utility for user "
                    + _userId + ": " + (expected_exact_time / (double) count),
                    Common._RESULTS_WRITER);

            s = "# prefs_exact: " + count + ", # correct_exact: "
                    + correct_exact + ", # wrong_exact: " + wrong_exact + " : "
                    + (double) correct_exact / (double) count * 100.
                    + "%, expected_time: " + expected_exact_time
                    + "%, inference_time_exact: " + _inference_time_exact
                    + ", no_exact_nodes: " + _node_count_exact
                    + ", max_no_exact_nodes: " + _max_node_count_exact;
            Common.println(s);
            Common.println(s, Common._RESULTS_WRITER);
        }

        Common.flush();

        return percentage;
    }

}
