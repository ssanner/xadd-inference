package sve.gibbs;

import java.util.HashMap;
import java.util.TreeMap;

import xadd.XADD;
import xadd.ExprLib;

public class ScoreGibbs extends Gibbs {

    public ScoreGibbs(String filename, String likelighoodPath, PreferenceDataset ds, boolean evaluate_exact) {
        super(filename, likelighoodPath, "", ds, evaluate_exact);
    }

    public void prepareInference(int userID) {
        _varList.remove("o_" + (userID + 1));
        super.prepareInference(userID);
    }

    @Override
    protected int assignValues(int xadd, int[] vals) {
        HashMap<String, ExprLib.ArithExpr> subst = new HashMap<String, ExprLib.ArithExpr>();
        if (vals[0] == _userId) {
            subst.put("s", new ExprLib.DoubleExpr(vals[2] - vals[3]));
            subst.put("o_i", new ExprLib.VarExpr("o_" + (vals[1] + 1)));
        } else if (vals[1] == _userId) {
            subst.put("s", new ExprLib.DoubleExpr(vals[3] - vals[2]));
            subst.put("o_i", new ExprLib.VarExpr("o_" + (vals[0] + 1)));
        } else {
            return -1;
        }
        xadd = this._gm._context.substitute(xadd, subst);
        return xadd;
    }

    @Override
    protected Boolean testOneExact(int[] vals) {
        if (_userId < 0 || vals[0] != _userId)
            return null;

        int temp = _exactposterior;
        _node_count_exact = 0;
        for (String key : _samples.keySet()) {
            int t = _gm._context.getNodeCount(temp);
            _node_count_exact += t;
            temp = integrate(key, temp);
            if (t > _max_node_count_exact)
                _max_node_count = t;
        }

        double s = ((ExprLib.DoubleExpr) ((XADD.XADDTNode) _gm._context.getNode(temp))._expr)._dConstVal;

        System.out.println(s + "," + vals[2] + "," + vals[3]);

        return s > 0;
    }

    @Override
    protected Boolean testOne(int[] vals) {
        if (_userId < 0 || vals[0] != _userId)
            return null;

        double s = 0;
        double dd = 0;
        for (int i = 0; i < SAMPLE_SIZE; i++) {
            for (String key : _samples.keySet()) {
                String d = key.substring(key.indexOf("_") + 1);
                try {
                    dd = Double.parseDouble(d);
                } catch (Exception ex) {
                    continue;
                }
                // if (dd == vals[0]) {
                // s += _samples.get(key).get(i);
                // }
                if ((dd == vals[1] && vals[0] == _userId) || (dd == vals[0] && vals[1] == _userId)) {
                    s += _samples.get(key).get(i) - _samples.get("d").get(i);
                }
            }
        }

        s = s / SAMPLE_SIZE;

        System.out.println(s + "," + vals[2] + "," + vals[3]);

        return s > 0;
    }

    @Override
    protected int noVariablesVarExp() {
        return _prefDs.getItemsCount();
    }

    public static Args getArgs(int no_items) {
        Args arg = new Args();
        arg.dsName = "premier_scores"; // "sushi10"; // "simple_synthetic5";
        arg.items = "./src/prefs/data/x_premier.csv";
        arg.users = "./src/prefs/data/u_premier.csv";
        arg.train = "./src/prefs/data/premier_train.csv"; // "./src/prefs/data/pref_sushi10.csv"; //
        arg.test = "./src/prefs/data/premier_test.csv"; // _test//
        arg.dsTrain = new PreferenceDataset(arg.items, arg.users, arg.train);
        arg.dsTest = new PreferenceDataset(arg.test);
        arg.likelihood = new String[]{"./src/prefs/models/ScoreLikelihood.xadd"}; // , "./src/prefs/deltaMMLikelihood.xadd" };
        arg.prior = new String[]{"./src/prefs/models/TriangularDoubleScoreConst.gm"}; // "./src/prefs/UniformConst.gm" }; // , ,
        // arg.dsTrain.setPreferenceLimit(3);
        arg.dsTrain.selectItemSubset(no_items, .2, 0, 1);
        arg.dsTest.selectItemSubset(0, 0., 0, 1);

        return arg;
    }

    public static void main(String[] args) {
        Args arg = getArgs(4);

        TreeMap<String, Double> percentage = new TreeMap<String, Double>();
        double sum = 0;

        boolean evaluate_exact = false;

        Common.init();
        for (String l : arg.likelihood) {
            for (String p : arg.prior) {
                for (int u = 0; u < 1 /* arg.dsTrain.getUsersCount() */; u++) {
                    ScoreGibbs g = new ScoreGibbs(p, l, arg.dsTrain, evaluate_exact);
                    g.prepareInference(u);
                    double e = 0;
                    for (int i = 0; i < arg.no_experiments; i++) {
                        g.infer(e);
                        Common.println(">> user: " + u + ", epsilon: " + Double.toString(e) + ", experiment: " + (i + 1)
                                + " is starting");
                        Common.println(">> user: " + u + ", epsilon: " + Double.toString(e) + ", experiment: " + (i + 1)
                                + " is starting", Common._RESULTS_WRITER);
                        double d = g.test(arg.dsTest);
                        Common.println("-------");
                        Common.println("-------", Common._RESULTS_WRITER);
                        percentage.put(u + ":" + Double.toString(e) + ":" + i, d);
                        sum += d;
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

}
