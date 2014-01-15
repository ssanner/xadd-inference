package ml;

import java.text.DecimalFormat;


/**
 * Automatically generate data, run Loss01Simplex and Loss01BB
 * and compare the result. Stop when the result is different.
 *
 * @author Tan T. Nguyen
 */

public class Test {

    public static void main(String[] args) {
        String dataFileName = "./src/ml/data_test.txt";
        final int maxTests = 1000;
        final int nPoints = 16;

//		int diff = 0;

        for (int i = 0; i < maxTests; i++) {

            DataGenerator dg = new DataGenerator(nPoints, 2, dataFileName);
            Loss01BB ls = new Loss01BB(dataFileName);

            if (ls.getLossSimplex() > ls.getLoss()) {
                System.out.println("Test #" + i + ": lossBB= " + ls.getLoss()
                        + " < lossSimplex= " + ls.getLossSimplex());
                Visualizer viz = new Visualizer(dataFileName, 0, 1);
                viz.pack();
                viz.setVisible(true);
                viz.updateW(ls.getWeightsSimplex());
                viz.updateW(ls.getWeights());
                break;
//				diff++;
            } else
                System.out.println("Test #" + i + ": lossBB = lossSimplex.");
        }
//		System.out.println("Number of diff: " + diff + " / " + maxTests);
    }

}
