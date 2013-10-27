package hgm.asve.cnsrv.approxator.sampler;

import junit.framework.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Created by Hadi Afshar.
 * Date: 23/10/13
 * Time: 9:14 AM
 */
public class GridSamplerTest {
    @Test
    public void testIterator1() throws Exception {
        GridSampler gs = new GridSampler(
                new String[]{"a", "b", "c"},
                new double[]{0, 0, 0},
                new double[]{3, 3, 3},
                new double[]{1, 1, 1});

        int counter = 0;
        Iterator<double[]> sampleIterator = gs.getSampleIterator();
        while (sampleIterator.hasNext()) {
            counter ++;
            double[] next = sampleIterator.next();
        }
        Assert.assertEquals(4*4*4, counter);
    }
    @Test
    public void testIterator2() throws Exception {
        GridSampler gs = new GridSampler(
                new String[]{"a", "b", "c"},
                new double[]{0d, -1d, 0.5d},
                new double[]{3, 2.99, 3.4},
                new double[]{1, 0.5, 0.5});

        Iterator<double[]> sampleIterator = gs.getSampleIterator();
        while (sampleIterator.hasNext()) {
            double[] next = sampleIterator.next();
            System.out.println("next = " + Arrays.toString(next));
        }
    }
}
