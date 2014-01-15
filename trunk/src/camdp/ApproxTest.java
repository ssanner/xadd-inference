package camdp;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

import camdp.CAMDP;

public class ApproxTest {

    public static PrintStream makeLog(String filename, double error, String dir) {
        String[] f1 = filename.split("\\.");
        String[] longname = (f1[0]).split("/");
        String name = longname[longname.length - 1];
        String approx = String.format("%03d", Math.round(1000 * error));
        String logName = dir + "/" + name + "_" + approx + ".log";
        try {
            return new PrintStream(new FileOutputStream(logName));
        } catch (Exception e) {
            System.err.println("Could not create Log '" + logName + "' in '" + dir + "' to produce output files.");
            System.exit(1);
        }
        return null;
    }

    public static void usage() {
        System.out.println("Usage: 4 to 6 params:\n AproxTest filename logdir iter Napprox [ApproxStepSize 2D or 3D]\n");
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        if (args.length < 4 || args.length > 7) {
            usage();
            System.exit(1);
        }

        // Parse problem filename
        String filename = args[0];
        String resultsDir = args[1];
        boolean printToFile = true; //log format: iter, nodes, branches, time, max, totTime, Mem
        if (resultsDir.toUpperCase().compareTo("STD_OUT") == 0) {
            printToFile = false;
        }
        PrintStream out = System.out;

        // Parse iterations
        int iter = -1;
        int nApproxSteps = -1;
        double approxStepSize = 0.1d;
        int valuePlot = 0;
        try {
            iter = Integer.parseInt(args[2]);
            nApproxSteps = Integer.parseInt(args[3]);
            if (args.length >= 5) approxStepSize = Double.parseDouble(args[4]);
            if (args.length >= 6) valuePlot = Integer.parseInt(args[5]);
            if ((nApproxSteps) * approxStepSize > 1) System.err.format("\nIllegal approx step %f or number %d\n",
                    approxStepSize, nApproxSteps);
            ;
        } catch (NumberFormatException nfe) {
            System.err.println("\nIllegal iteration, approx or display values\n");
            System.exit(1);
        }

        CAMDP camdp = new CAMDP(filename);
        if (valuePlot == 2)
            camdp.DISPLAY_2D = true;
        if (valuePlot == 3)
            camdp.DISPLAY_3D = true;

        for (int approx = 0; approx <= nApproxSteps; approx++) {
            double error = approx * approxStepSize;
            System.out.format("Solving %s with %d iter, %f error\n", filename, iter, error);
            if (printToFile) out = makeLog(filename, error, resultsDir);
            camdp.setApproxTest(error, out, false);
            try {
                int iter_used = camdp.solve(iter);
            } catch (OutOfMemoryError e) {
                System.err.println("Catch blow up!");
                e.printStackTrace();
            }
            camdp.flushCaches(true);
            out.close();
        }
        System.out.println("Approx Test, Over!");
        System.exit(0);
    }
}
