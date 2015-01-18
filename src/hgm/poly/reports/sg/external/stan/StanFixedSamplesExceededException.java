package hgm.poly.reports.sg.external.stan;

/**
 * Created by Hadi Afshar.
 * Date: 18/01/15
 * Time: 2:57 AM
 */
public class StanFixedSamplesExceededException extends StanParsingException {
    public StanFixedSamplesExceededException(String msg) {
        super(msg);
    }

    public StanFixedSamplesExceededException(Exception e) {
        super(e);
    }
}
