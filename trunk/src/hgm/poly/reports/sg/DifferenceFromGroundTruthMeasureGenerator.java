package hgm.poly.reports.sg;

import hgm.poly.diagnostics.MeasureOnTheRun;
import hgm.poly.gm.RichJointWrapper;

public interface DifferenceFromGroundTruthMeasureGenerator {
    public void initialize(RichJointWrapper jointWrapper);

    //must be called after init.
    public MeasureOnTheRun<Double[]> generateMeasure();


}
