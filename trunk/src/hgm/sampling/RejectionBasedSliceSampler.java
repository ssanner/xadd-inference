package hgm.sampling;

import xadd.XADD;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 18/12/13
 * Time: 6:37 PM
 */
@Deprecated
public class RejectionBasedSliceSampler extends XaddSampler {
    protected Double latestTargetValue = null;


    public RejectionBasedSliceSampler(XADD context, XADD.XADDNode root) {
        super(context, root);


    }

    @Override
    public VarAssignment sample() {
            double threshold;
            if (latestTargetValue == null) { // (no sample is taken yet)
                // initialization phase:
                threshold = 0.0;
            } else {
                threshold = randomDoubleUniformBetween(0.0, latestTargetValue); // sample uniformly between 0 and latest target value
            }

            sampleAboveThreshold(threshold);
            return reusableVarAssignment;
    }

    // Choose a starting value for which the target value is positive
    private void sampleAboveThreshold(double threshold) {
        HashMap<String, Boolean> bAssignments = new HashMap<String, Boolean>(bVars.size());
        HashMap<String, Double> cAssignments = new HashMap<String, Double>(cVars.size());
        Double target;
        do {
            for (String cv: cVars) {
                Double min = cVarMins.get(cv);
                Double max = cVarMaxes.get(cv);
                cAssignments.put(cv, randomDoubleUniformBetween(min, max));
            }

            for (String bv: bVars) {
                double random = Math.random();
                bAssignments.put(bv, random>= 0.5);
            }

            target = context.evaluate(rootId, bAssignments, cAssignments);
        } while (target <= threshold); // (rejection phase)  //todo what if everything is rejected ad infinitum?

        reusableVarAssignment = new VarAssignment(bAssignments, cAssignments);
        latestTargetValue = target;
    }


}
