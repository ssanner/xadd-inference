package hgm.asve.cnsrv.approxator.regression.measures;

import hgm.asve.cnsrv.approxator.regression.SamplingDB;
import xadd.XADD;

/**
 * Created by Hadi Afshar.
 * Date: 1/11/13
 * Time: 7:51 PM
 */
public interface DivergenceMeasure {
    public String measureName();

    public double calcDivergenceBetweenApproximatingNodeAndSamples(XADD context, XADD.XADDTNode approxNode, SamplingDB samples);
}
