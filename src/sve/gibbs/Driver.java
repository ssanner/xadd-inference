package sve.gibbs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Driver {

    static enum Experiments {
        prefs, score
    }

    ;

    private static Args getArgs(Experiments ex, int no_items) {
        switch (ex) {
            case prefs:
                return PrefsGibbs.getArgs(no_items);

            case score:
                return ScoreGibbs.getArgs(no_items);
        }

        return null;
    }

    private static Gibbs getInstance(Experiments ex, String p, String l,
                                     Args arg, boolean evaluate_exact) {
        switch (ex) {
            case prefs:
                return new PrefsGibbs(p, l, "./src/prefs/models/utility.xadd",
                        arg.dsTrain, evaluate_exact);

            case score:
                return new ScoreGibbs(p, l, arg.dsTrain, evaluate_exact);
        }

        return null;
    }

    private static class RunInstance implements Callable<String>, Runnable {

        private Gibbs g;
        private Args arg;
        private Double sum;
        private TreeMap<String, Double> percentage;
        private int u;
        private String str = "";

        @Override
        public void run() {

            System.gc();

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
                    String s = g._prefDs.getPreferenceFilename() + ":" + u
                            + ":" + Double.toString(e) + ":" + i;
                    str = str + "\n" + s;
                    percentage.put(str, d);
                    sum += d;
                }
            }

        }

        @Override
        public String call() throws Exception {
            run();
            Common.println(str);

            return str;
        }

    }

    public static void main(String[] args) {
        TreeMap<String, Double> percentage = new TreeMap<String, Double>();
        Double sum = new Double(0);
        Gibbs g = null;
        Common.init();
        ExecutorService executor = Executors.newFixedThreadPool(4);
        ArrayList<RunInstance> threads = new ArrayList<RunInstance>();

        for (Experiments ex : Experiments.values()) {
            // Experiments ex = Experiments.score; {
            for (int no_items = 4; no_items <= 20; no_items += 2) {
                Args arg = getArgs(ex, no_items);
                boolean evaluate_exact;
                if (no_items < 8) {
                    evaluate_exact = true;
                } else {
                    evaluate_exact = false;
                }
                for (String l : arg.likelihood) {
                    for (String p : arg.prior) {
                        for (int u = 0; u < 1 /* arg.dsTrain.getUsersCount() */; u++) {
                            g = getInstance(ex, p, l, arg, evaluate_exact);
                            RunInstance r = new RunInstance();
                            r.arg = arg;
                            r.g = g;
                            r.percentage = percentage;
                            r.sum = sum;

                            threads.add(r);
                        }
                    }
                }
                Common.println("");
                Common.flush();
            }
        }

        try {
            executor.invokeAll(threads, 2, TimeUnit.HOURS);
            executor.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Common.close();
        Common.println(percentage);
        Common.println("average: " + sum / (double) percentage.size());
    }
}
