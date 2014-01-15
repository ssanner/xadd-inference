package sve.gibbs;

import java.util.TreeMap;

public class Args {
    public final int no_experiments = 1;
    public boolean evaluate_exact = true;

    public String dsName = "simple_synthetic5";                                        // "sushi10"; // "simple_synthetic5";
    public String items = "./src/prefs/data/x_" + dsName + ".csv";            // x_car10 // -uniform
    public String users = "./src/prefs/data/u_" + dsName + ".csv";            // u_car10
    public String train = "./src/prefs/data/pref_" + dsName + ".csv";        // "./src/prefs/data/pref_sushi10.csv"; //
    // pref_simple_synthetic15
    public String test = "./src/prefs/data/pref_" + dsName + "_test.csv"; // _test//
    // "./src/prefs/data/pref_sushi10_test.csv"; //
    // pref_simple_synthetic15_test

    // System.setProperty("java.library.path", System.getProperty("java.library.path") + ":./lib/lpsolve55j.jar");
    // System.out.println(System.getProperty("java.library.path"));
    public double[] epsilon = new double[]{1};                                        // Common.generateRange(0, 0, 1);

    public PreferenceDataset dsTrain = new PreferenceDataset(items, users, train);
    public PreferenceDataset dsTest = new PreferenceDataset(test);

    public String[] likelihood = {"./src/prefs/models/deltaLikelihood.xadd"};    // , "./src/prefs/deltaMMLikelihood.xadd" };
    public String[] prior = {"./src/prefs/models/TriangularConst.gm"};        // "./src/prefs/UniformConst.gm" }; // , ,
    // "./src/prefs/NormalConst.gm" };

}
