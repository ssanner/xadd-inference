package hgm.poly.pref;

import hgm.sampling.VarAssignment;

/**
 * Created by Hadi Afshar.
 * Date: 16/03/14
 * Time: 5:26 AM
 */
public class GPolyPreferenceLearningPredictorUsingGibbsSampler extends GPolyPreferenceLearningPredictor {
    double minForAllVars;
    double maxForAllVars;

    public GPolyPreferenceLearningPredictorUsingGibbsSampler(double indicatorNoise,
                                                             int numberOfSamples,
                                                             int burnedSamples,
                                                             int maxGateConstraintViolation,
                                                             double minForAllVars, double maxForAllVars) {
        super(indicatorNoise, numberOfSamples, burnedSamples, maxGateConstraintViolation);

        this.minForAllVars = minForAllVars;
        this.maxForAllVars = maxForAllVars;
    }

    @Override
    public GatedGibbsPolytopesSampler makeNewSampler(PolytopesHandler posterior, VarAssignment initAssign) {
        if (initAssign!=null) System.err.println("NOTE: init assign will not be used...");
        return GatedGibbsPolytopesSampler.makeGibbsSampler(posterior,
                minForAllVars,
                maxForAllVars, null);
    }
}
