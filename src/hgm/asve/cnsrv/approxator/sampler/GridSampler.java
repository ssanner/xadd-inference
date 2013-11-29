package hgm.asve.cnsrv.approxator.sampler;

import java.util.Iterator;

/**
 * Created by Hadi Afshar.
 * Date: 23/10/13
 * Time: 7:38 AM
 */
public class GridSampler {
    String[] cVars;  //todo what should I do with boolean vars?
    double[] cVarMinValues;
    double[] cVarMaxValues;
    double[] cVarGridIncValues;

    int n;
    double[] firstOutput;

    public GridSampler(String[] cVars, double[] cVarMinValues, double[] cVarMaxValues, double[] cVarGridIncValues) {
        this.cVars = cVars;
        this.cVarMinValues = cVarMinValues;
        this.cVarMaxValues = cVarMaxValues;
        this.cVarGridIncValues = cVarGridIncValues;

        n = cVars.length;
        assert cVarMaxValues.length == n;
        assert cVarMinValues.length == n;
        assert cVarGridIncValues.length == n;

        firstOutput = new double[n];
        System.arraycopy(cVarMinValues, 0, firstOutput, 0, n);
    }

    public Iterator<double[]> getSampleIterator() {
        return new Iterator<double[]>() {

            double[] nextOutput = firstOutput.clone();

            @Override
            public boolean hasNext() {
                return nextOutput != null;
            }

            @Override
            public double[] next() {
                double[] currentOutput = nextOutput.clone();

                int index = nextOutput.length - 1;
                try {
                    increaseEntry(index, nextOutput);
                } catch (IndexOutOfBoundsException e) {
                    nextOutput = null;
                }

                return currentOutput;
            }

            private void increaseEntry(int index, double[] data) {
                if (index == -1) {
                    throw new IndexOutOfBoundsException();
                }

                data[index] += cVarGridIncValues[index];

                if (data[index] > cVarMaxValues[index]) {
                    data[index] = cVarMinValues[index];
                    increaseEntry(index - 1, data);
                }
            }

            @Override
            public void remove() {
                throw new RuntimeException("not implemented");
            }
        };
    }
}
