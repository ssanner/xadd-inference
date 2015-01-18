package hgm.poly.reports.sg.external.stan;

import hgm.sampling.SamplingFailureException;

/**
 * Created by Hadi Afshar.
 * Date: 16/01/15
 * Time: 8:46 AM
 */
public class StanParsingException extends SamplingFailureException {
    public StanParsingException(String msg) {
        super(msg);
    }

    public StanParsingException(Exception e) {
        super(e);
    }
}