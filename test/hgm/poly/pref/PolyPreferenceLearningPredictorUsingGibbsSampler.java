package hgm.poly.pref;

import hgm.sampling.VarAssignment;

/**
 * Created by Hadi Afshar.
 * Date: 16/03/14
 * Time: 5:26 AM
 */
public class PolyPreferenceLearningPredictorUsingGibbsSampler extends PolyPreferenceLearningPredictor {
    double minForAllVars;
    double maxForAllVars;

    public PolyPreferenceLearningPredictorUsingGibbsSampler(double indicatorNoise,
                                                            int numberOfSamples,
                                                            int burnedSamples,
                                                            int maxGateConstraintViolation,
                                                            double minForAllVars, double maxForAllVars) {
        super(indicatorNoise, numberOfSamples, burnedSamples, maxGateConstraintViolation);

        this.minForAllVars = minForAllVars;
        this.maxForAllVars = maxForAllVars;
    }

    @Override
    public GatedPolytopesSampler makeNewSampler(GatedPolytopesHandler posterior, VarAssignment initAssign) {
        if (initAssign!=null) System.err.println("NOTE: init assign will not be used...");
        return GatedPolytopesSampler.makeGibbsSampler( posterior,
                minForAllVars,
                maxForAllVars, null);
    }
}
