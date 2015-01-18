package hgm.poly.reports.sg;

import hgm.poly.gm.JointToSampler;
import hgm.poly.gm.JointWrapper;
import hgm.poly.sampling.SamplerInterface;
import hgm.sampling.SamplingFailureException;

import java.util.Arrays;

/**
 * Created by Hadi Afshar.
 * Date: 9/09/14
 * Time: 3:31 AM
 */
public class DifferenceSampler implements JointToSampler {
    JointToSampler inner1;
    JointToSampler inner2;

    public DifferenceSampler(JointToSampler inner1, JointToSampler inner2) {
        this.inner1 = inner1;
        this.inner2 = inner2;
    }

    @Override
    public SamplerInterface makeSampler(JointWrapper jointWrapper) {
        final SamplerInterface sampler1 = inner1.makeSampler(jointWrapper);
        final SamplerInterface sampler2 = inner2.makeSampler(jointWrapper);
        return new SamplerInterface() {
            @Override
            public Double[] reusableSample() throws SamplingFailureException {
                return difference(sampler1.reusableSample(), sampler2.reusableSample());
            }

            Double[] difference(Double[] d1, Double[] d2) {
                if (d1.length != d2.length) throw new SamplingFailureException("size mismatch");
                Double[] diff = new Double[d1.length];
                for (int i = 0; i < d1.length; i++) {
                    //they should be simultaneously null or number
                    if ((d1[i] == null) ^ (d2[i] == null)) throw new RuntimeException("cannot take the difference of these arrays: " +
                            Arrays.toString(d1) + " and " + Arrays.toString(d2));
                    diff[i] = d1[i] == null ?
                            null :
                            d1[i] - d2[i];
                }
                return diff;
            }
        };
    }

    @Override
    public String getName() {
        if (!inner1.getName().equals(inner2.getName())) throw new RuntimeException("inner should be the same");
        return inner1.getName();
    }


}
