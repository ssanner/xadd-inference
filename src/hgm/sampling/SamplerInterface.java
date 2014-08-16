package hgm.sampling;

/**
 * Created by Hadi Afshar.
 * Date: 27/02/14
 * Time: 1:59 AM
 */
@Deprecated
public interface SamplerInterface {
    public VarAssignment sample() throws SamplingFailureException;
}
