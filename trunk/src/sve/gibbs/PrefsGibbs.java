package sve.gibbs;

import java.io.BufferedReader;
import java.io.FileReader;
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
import xadd.ExprLib.DoubleExpr;
import xadd.XADD.XADDNode;
import camdp.HierarchicalParser;

public class PrefsGibbs extends Gibbs {
    private int _utility;

    public PrefsGibbs(String filename, String utilitpath, PreferenceDataset ds,
                      boolean evaluate_exact) {
        super(filename, utilitpath, ds, evaluate_exact);
    }

    public PrefsGibbs(String filename, String likelighoodPath,
                      String utilityPath, PreferenceDataset ds, boolean evaluate_exact) {
        super(filename, likelighoodPath, utilityPath, ds, evaluate_exact);
    }

    private int _exactposterior_utility = -1;

    protected void init(String filename, String likelighoodPath,
                        String utilityPath, PreferenceDataset ds, boolean evaluate_exact) {
        super.init(filename, likelighoodPath, utilityPath, ds, evaluate_exact);

        try {
            // load the utility function
            ArrayList l = new ArrayList();
            BufferedReader br = new BufferedReader(new FileReader(utilityPath));
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            l.add(Common.extractSumLine(_var2exp, sb.toString()));
            _utility = _gm._context.buildCanonicalXADD(l);

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    protected void exactInfer() {
        int temp = _gm._context.apply(_exactposterior, _utility, XADD.PROD);
        for (String key : _varList.keySet()) {
            int t = _gm._context.getNodeCount(temp);
            _node_count_exact += t;
            temp = integrate(key, temp);
            if (t > _max_node_count_exact)
                _max_node_count_exact = t;
        }
        _node_count_exact += _gm._context.getNodeCount(temp);
        _exactposterior_utility = temp;
    }

    /**
     * Fill the subst parameter containing the parameters that has to be
     * replaced
     *
     * @param subst : The parameter in which the subsitute values are written
     * @param vals  : Values to be used, i.e. features
     * @param var   : Variable that has to be substituted
     */
    private void fillPreferenceSubst(HashMap<String, ArithExpr> subst,
                                     Double[] vals, String var) {
        for (int i = 1; i <= _varList.size(); i++) {
            subst.put(var + "_" + i, new DoubleExpr(vals[i - 1]));
        }
    }

    /**
     * Substitute the preferences and its item values values in the given xadd:
     * utility or likelihoods
     *
     * @param xadd : xadd to be replaced in
     * @param vals : preference values ... user, item1, item2 values
     * @return
     */
    private int assignPreferenceValues(int xadd, int[] vals) {
        HashMap<String, ArithExpr> subst = new HashMap<String, ArithExpr>();

        fillPreferenceSubst(subst, _prefDs.getItem(vals[1]), "x"); // item1
        fillPreferenceSubst(subst, _prefDs.getItem(vals[2]), "y"); // item2

        xadd = this._gm._context.substitute(xadd, subst);
        return xadd;
    }

    protected Boolean testOneExact(int[] p) {
        double eu1_exact = exactExpectedUtility(_prefDs.getItem(p[1]));
        double eu2_exact = exactExpectedUtility(_prefDs.getItem(p[2]));

        if ((eu1_exact > eu2_exact)
                || (eu1_exact == eu2_exact && _r.nextBoolean()))
            return true;

        return false;
    }

    protected Boolean testOne(int[] p) {
        if (_userId < 0 || p[0] != _userId)
            return null;

        double eu1 = expectedUtility(_prefDs.getItem(p[1]));
        double eu2 = expectedUtility(_prefDs.getItem(p[2]));

        if ((eu1 > eu2) || (eu1 == eu2 && _r.nextBoolean()))
            return true;

        return false;
    }

    /**
     * For a given item, computes its expected utility based on the samples
     * generated in infer function
     *
     * @param item
     * @return
     */
    private double expectedUtility(Double[] item) {
        int sum = -1;
        HashMap<String, ArithExpr> subst = new HashMap<String, ArithExpr>();
        fillPreferenceSubst(subst, item, "x"); // item1
        int u = this._gm._context.substitute(_utility, subst);
        subst.clear();
        for (int i = 0; i < SAMPLE_SIZE; i++) {
            // HashMap<String, ArithExpr> subst = new HashMap<String,
            // XADD.ArithExpr>();
            for (String key : _samples.keySet()) {
                subst.put(key, new DoubleExpr(_samples.get(key).get(i)));
            }
            int temp = _gm._context.substitute(u, subst);
            if (sum < 1) {
                sum = temp;
            } else {
                sum = _gm._context.apply(sum, temp, XADD.SUM);
            }
            subst.clear();
        }

        double res = ((DoubleExpr) ((XADD.XADDTNode) _gm._context.getNode(sum))._expr)._dConstVal
                / (double) SAMPLE_SIZE;
        // Common.println(Double.toString(res));

        return res;
    }

    /**
     * For a given item, computes its exact expected utility
     *
     * @param item
     * @return
     */
    private double exactExpectedUtility(Double[] item) {
        HashMap<String, ArithExpr> subst = new HashMap<String, ArithExpr>();
        fillPreferenceSubst(subst, item, "x"); // item1
        int temp = this._gm._context.substitute(_exactposterior_utility, subst);
        // int u = this._gm._context.substitute(_utility, subst);
        // int temp = _gm._context.apply(_exactposterior, u, XADD.PROD);
        // for (String key : _samples.keySet()) {
        // int t = _gm._context.getNodeCount(temp);
        // _node_count_exact += t;
        // temp = integrate(key, temp);
        // if (t > _max_node_count_exact)
        // _max_node_count_exact = t;
        // }

        return ((DoubleExpr) ((XADD.XADDTNode) _gm._context.getNode(temp))._expr)._dConstVal;
    }

    @Override
    protected int assignValues(int likelihood, int[] vals) {
        if (_userId < 0 || vals[0] != _userId)
            return -1;
        return assignPreferenceValues(likelihood, vals);
    }

    @Override
    protected int noVariablesVarExp() {
        return _prefDs.itemsDimension();
    }

    public static Args getArgs(int no_items) {
        Args arg = new Args();
        arg.dsName = "car_sp60.1"; // "car_sp60.1" "car10"; //
        // "simple_synthetic5"; // "sushi10"; //
        // "simple_synthetic5";
        arg.items = "./src/prefs/data/x_" + arg.dsName + ".csv"; // x_car10 //
        // -uniform
        arg.users = "./src/prefs/data/u_" + arg.dsName + ".csv"; // u_car10
        arg.train = "./src/prefs/data/pref_" + arg.dsName + ".csv"; // "./src/prefs/data/pref_sushi10.csv";
        // //
        arg.test = "./src/prefs/data/pref_" + arg.dsName + "_test.csv"; // _test//
        arg.epsilon = Common.generateRange(0, 0, 1);
        arg.dsTrain = new PreferenceDataset(arg.items, arg.users, arg.train);
        arg.dsTest = new PreferenceDataset(arg.test);
        arg.likelihood = new String[]{"./src/prefs/models/deltaLikelihood.xadd"}; // ,
        // "./src/prefs/deltaMMLikelihood.xadd"
        // };
        arg.prior = new String[]{"./src/prefs/models/TriangularConst.gm"}; // "./src/prefs/UniformConst.gm"
        // };
        // //
        // ,
        // ,
        // arg.dsTrain.setPreferenceLimit(5);
        arg.dsTrain.selectItemSubset(no_items, 1., 1, 2);
        arg.dsTest.selectItemSubset(0, 1., 1, 2);

        return arg;
    }

    static void main() {
        TreeMap<String, Double> percentage = new TreeMap<String, Double>();
        double sum = 0;

        Common.init();
        for (int no_items = 3; no_items <= 20; no_items += 2) {
            Args arg = getArgs(no_items);
            boolean evaluate_exact;
            if (no_items < 5) {
                evaluate_exact = true;
            } else {
                evaluate_exact = false;
            }
            for (String l : arg.likelihood) {
                for (String p : arg.prior) {
                    for (int u = 0; u < 1 /* arg.dsTrain.getUsersCount() */; u++) {
                        PrefsGibbs g = new PrefsGibbs(p, l,
                                "./src/prefs/models/utility.xadd", arg.dsTrain,
                                evaluate_exact);
                        g.prepareInference(u);
                        for (double e : arg.epsilon) {
                            for (int i = 0; i < arg.no_experiments; i++) {
                                g.infer(e);
                                Common.println(">> user: " + u + ", epsilon: "
                                        + Double.toString(e) + ", experiment: "
                                        + (i + 1) + " is starting");
                                Common.println(">> user: " + u + ", epsilon: "
                                        + Double.toString(e) + ", experiment: "
                                        + (i + 1) + " is starting",
                                        Common._RESULTS_WRITER);
                                double d = g.test(arg.dsTest);
                                Common.println("-------");
                                Common.println("-------",
                                        Common._RESULTS_WRITER);
                                percentage.put(u + ":" + Double.toString(e)
                                        + ":" + i, d);
                                sum += d;
                            }
                        }
                    }
                }
            }
            Common.println("");
            Common.flush();
        }

        Common.close();
        Common.println(percentage);
        Common.println("average: " + sum / (double) percentage.size());
    }

    /**
     * Main
     *
     * @param args
     */
    public static void main(String[] args) {
        main();
        // GraphicalModel gm = new GraphicalModel("./src/prefs/NormalConst.gm");
        // HashMap<String, ArrayList<Integer>> var2expansion = new
        // HashMap<String, ArrayList<Integer>>();
        // var2expansion.put("i", new ArrayList<Integer>());
        // var2expansion.get("i").add(1);
        //
        // gm.instantiateGMTemplate(var2expansion);
        // int a = gm.getXADD(gm._hsVariables.toArray(new String[] {})[0],
        // false);
        // gm._context.getGraph(a).launchViewer();
    }

}
