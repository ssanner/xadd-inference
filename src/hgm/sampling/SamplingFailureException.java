package hgm.sampling;

/**
 * Created by Hadi Afshar.
 * Date: 11/01/14
 * Time: 2:08 PM
 */
public class SamplingFailureException extends  RuntimeException {
    public SamplingFailureException(String message) {
        super(message);
    }

    public SamplingFailureException() {
        super();
    }

    public SamplingFailureException(Exception e) {
        super(e);
    }
}
