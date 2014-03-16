package hgm.poly.sampling;

import hgm.sampling.SamplingFailureException;

/**
 * Created by Hadi Afshar.
 * Date: 12/03/14
 * Time: 8:21 AM
 */
public interface SamplerInterface {
    Double[] sample() throws SamplingFailureException;
}
