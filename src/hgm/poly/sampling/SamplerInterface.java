package hgm.poly.sampling;

import hgm.sampling.SamplingFailureException;

/**
 * Created by Hadi Afshar.
 * Date: 12/03/14
 * Time: 8:21 AM
 */
public interface SamplerInterface {
    public static final int MAX_WAITING_MILLIS_TO_TAKE_INITIAL_SAMPLE = 1000 * 60;

    /**
     *
     * @return  a (possibly and often) REUSABLE sample. For storing needs to be cloned.
     * @throws SamplingFailureException
     */
    Double[] reusableSample() throws SamplingFailureException;
}
