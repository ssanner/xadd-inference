package sve.gibbs;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import camdp.HierarchicalParser;
import camdp.TokenStream;
import camdp.TokenStreamException;

import util.Pair;
import xadd.XADD;

public class UniformPreference {

    public static void main(String[] args) {
        try {
            PrintStream ps = new PrintStream("./uniform_preference_16.csv");
            for (int dim = 2; dim <= 10; dim += 1) {
                for (int c = 1; c <= 10; c += 1) {
                    double t = run(c, dim);
                    ps.println(c + "," + dim + "," + t);
                    ps.flush();
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static double run(int constraints, int dim) {
        double[] interior = new double[dim];
        for (int i = 0; i < dim; i++) {
            interior[i] = 0.;
        }

        Constraint[] c = generateConstraints(constraints, interior);

        // for (int i = 0; i < c.length; i++) {
        // System.out.println(i + ": " + c[i].toString());
        // }

        String xStr = getXADDString(c);
        System.out.println(xStr);

        TokenStream ts = new TokenStream();
        int constraints_xadd = -1;
        Calendar cal = Calendar.getInstance();
        try {
            ts.openFromStringContent(xStr);
            XADD xadd = new XADD();
            ArrayList parsed_str = HierarchicalParser.ParseFileInt(ts, 0);
            constraints_xadd = xadd.buildCanonicalXADD(parsed_str);

            ArrayList<String> ar = new ArrayList<String>();
            ar.add("[" + generateLine(dim).toString() + "]");
            int utility = xadd.buildCanonicalXADD(ar);
            int product = xadd.apply(constraints_xadd, utility, XADD.PROD);

            String[] vars = Line.getVars(dim);
            for (int i = 0; i < vars.length; i++) {
                int x = xadd.computeDefiniteIntegral(product, vars[i]);
                // xadd.getNode(x).toString();
                // xadd.reduceLP(x, Arrays.asList(Arrays.copyOfRange(vars, i+1,
                // vars.length)));
                xadd.reduceLP(x);
                product = x;
            }
            // xadd.getGraph(product).launchViewer();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Calendar.getInstance().getTimeInMillis() - cal.getTimeInMillis();
    }

    private final static long SEED = 110;
    private final static int MAX_COEF_RANDOM_LINE = 100;
    private static Random rand = new Random(SEED);

    private static double generateCoef() {
        return (double) rand.nextInt(MAX_COEF_RANDOM_LINE) * rand.nextDouble()
                * (rand.nextBoolean() ? 1. : -1.);
    }

    private static Line generateLine(int dim) {
        double[] c = new double[dim];
        for (int i = 0; i < dim; i++) {
            c[i] = generateCoef();
        }
        return new Line(c);
    }

    private static Constraint[] generateConstraints(int count,
                                                    double[] interior_point) {
        Constraint[] c = new Constraint[count];
        final int dim = interior_point.length;
        double[] xs = Arrays.copyOfRange(interior_point, 0, dim - 1);
        for (int i = 0; i < count; i++) {
            Line line = generateLine(dim);
            boolean gt = line.eval(xs) > interior_point[dim - 1];
            c[i] = new Constraint(line, gt);
        }

        return c;
    }

    private static String getXADDString(Constraint[] c) {
        StringBuilder sb = new StringBuilder();

        sb.append("[" + c[0].toString() + "] \n");
        for (int i = 1; i < c.length; i++) {
            sb.append(getTab(i) + " ( [" + c[i].toString() + "] \n");
        }

        sb.append(getTab(c.length + 1) + " ( [1.0] ) ( [0.0] ) ");

        for (int i = c.length - 1; i >= 1; i--) {
            sb.append(getTab(i) + " \n ) ( [0.0] ) ");
        }

        return sb.toString();
    }

    private static String getTab(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append("   ");
        }
        return sb.toString();
    }
}
