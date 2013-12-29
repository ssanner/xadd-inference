package hgm.sampling;

import hgm.asve.Pair;
import xadd.XADD;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 18/12/13
 * Time: 6:37 PM
 */
public class RejectionBasedSliceSampling {
    private XADD context;
    private XADD.XADDNode root;
    private int rootId;
    private List<String> cVars = new ArrayList<String>();
    private List<String> bVars = new ArrayList<String>();
    private Map<String, Double> cVarMins;
    private Map<String, Double> cVarMaxes;

    private VarAssignment latestVarAssign;
    private Double latestTargetValue = null;



    public RejectionBasedSliceSampling(XADD context, XADD.XADDNode root) {
        this.context = context;
        this.root = root;
        rootId = context._hmNode2Int.get(root);

        HashSet<String> allVars = root.collectVars();
        for (String var : allVars) {
            if (context._hsBooleanVars.contains(var)) {
                bVars.add(var);
            } else cVars.add(var);
        }

        cVarMins = new HashMap<String, Double>(cVars.size());
        cVarMaxes = new HashMap<String, Double>(cVars.size());
        for (String var : cVars) {
            cVarMins.put(var, context._hmMinVal.get(var)); //todo check how XADD works for min and max of boolean variables...
            cVarMaxes.put(var, context._hmMaxVal.get(var));
        }
    }

    /**
     * @return sample of each variable in an XADD
     */
    public VarAssignment sample() {
        double threshold;
        if (latestTargetValue == null) { // (no sample is taken yet)
            // initialization phase:
            threshold = 0.0;
        } else {
            threshold = sampleUniformlyBetween(0.0, latestTargetValue); // sample uniformly between 0 and latest target value
        }

        sampleAboveThreshold(threshold);
        return latestVarAssign;
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
                cAssignments.put(cv, sampleUniformlyBetween(min, max));
            }

            for (String bv: bVars) {
                double random = Math.random();
                bAssignments.put(bv, random>= 0.5);
            }

            target = context.evaluate(rootId, bAssignments, cAssignments);
        } while (target <= threshold); // (rejection phase)  //todo what if everything is rejected ad infinitum?

        latestVarAssign = new VarAssignment(bAssignments, cAssignments);
        latestTargetValue = target;
    }

    private Double sampleUniformlyBetween(Double min, Double max) {
        return Math.random() * (max - min) + min;
    }


}
