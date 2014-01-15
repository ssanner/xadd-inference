package sve.gibbs;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.security.interfaces.DSAKey;
import java.util.HashMap;
import java.util.TreeMap;

import xadd.XADD;
import xadd.ExprLib.DoubleExpr;
import xadd.ExprLib.VarExpr;

public class UniformGibbs extends Gibbs {

    public UniformGibbs(String filename, String likelihoodPath,
                        PreferenceDataset ds, boolean evaluate_exact) {
        super(filename, likelihoodPath, "", ds, evaluate_exact);
    }

    @Override
    protected int assignValues(int likelihood, int[] vals) {
        return 0;
    }

    @Override
    protected Boolean testOneExact(int[] vals) {
        return null;
    }

    @Override
    protected Boolean testOne(int[] vals) {
        return null;
    }

    @Override
    protected int noVariablesVarExp() {
        return 1;
    }

    public double test(PreferenceDataset ds) {
        return -1;
    }

    public void test() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < SAMPLE_SIZE; i++) {
            sb.append(_samples.get("s").get(i).toString() + "\n");
        }
        String s = Common.getDateTime();
        try {
            Common.println(sb.toString(), new BufferedWriter(new FileWriter(
                    Common.RESULT_PATH + "uniform_samples" + s + ".csv")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (_evaluate_exact) {
            sb = new StringBuilder();
            int temp = integrate("l", _exactposterior);
            temp = integrate("u", temp);
            HashMap<String, Double> subst = new HashMap<String, Double>();
            for (double i = 0; i < 2; i += .1) {
                subst.put("s", i);
                sb.append(_gm._context.evaluate(temp, null, subst).toString()
                        + "\n");
            }
            try {
                Common.println(sb.toString(), new BufferedWriter(
                        new FileWriter(Common.RESULT_PATH
                                + "uniform_samples_exact" + s + ".csv")));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Args getArgs() {
        Args arg = new Args();
        arg.dsName = "uniform"; // "sushi10"; // "simple_synthetic5";
        arg.items = "./src/prefs/data/x_" + arg.dsName + ".csv";
        arg.users = "./src/prefs/data/u_" + arg.dsName + ".csv";
        arg.train = "./src/prefs/data/" + arg.dsName + "_train.csv"; // "./src/prefs/data/pref_sushi10.csv";
        // //
        arg.test = "./src/prefs/data/" + arg.dsName + "_test.csv"; // _test//
        arg.dsTrain = new PreferenceDataset();
        arg.dsTest = new PreferenceDataset();
        arg.likelihood = new String[]{"./src/prefs/models/UniformLikelihood.xadd"}; // ,
        // "./src/prefs/deltaMMLikelihood.xadd"
        // };
        arg.prior = new String[]{"./src/prefs/models/UniformPrior.gm"}; // "./src/prefs/UniformConst.gm"
        // };
        // //
        // ,
        // ,
        arg.dsTrain.setPreferenceLimit(5);

        return arg;
    }

    public static void main(String[] args) {
        Args arg = getArgs();

        TreeMap<String, Double> percentage = new TreeMap<String, Double>();
        double sum = 0;

        boolean evaluate_exact = false;

        Common.init();
        UniformGibbs g = new UniformGibbs(arg.prior[0], arg.likelihood[0],
                arg.dsTrain, evaluate_exact);

        for (int u = 0; u < arg.dsTrain.getUsersCount(); u++) {
            g.prepareInference(u);
            for (double e : arg.epsilon) {
                for (int i = 0; i < arg.no_experiments; i++) {
                    g.infer(e);
                    Common.println(">> user: " + u + ", epsilon: "
                            + Double.toString(e) + ", experiment: " + (i + 1)
                            + " is starting");
                    Common.println(
                            ">> user: " + u + ", epsilon: "
                                    + Double.toString(e) + ", experiment: "
                                    + (i + 1) + " is starting",
                            Common._RESULTS_WRITER);
                    double d = g.test(arg.dsTest);
                    Common.println("-------");
                    Common.println("-------", Common._RESULTS_WRITER);
                    percentage.put(u + ":" + Double.toString(e) + ":" + i, d);
                    sum += d;
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
