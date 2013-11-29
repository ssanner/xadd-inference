package hgm.asve.cnsrv.approxator.regression.measures;

import hgm.asve.Pair;
import hgm.asve.cnsrv.approxator.regression.SamplingDB;
import xadd.XADD;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Hadi Afshar.
 * Date: 1/11/13
 * Time: 7:55 PM
 */
public class ZeroFriendlyMeanSquareErrorMeasure implements DivergenceMeasure {
    public static double MAX_LOSS = 1000d;

    @Override
    public String measureName() {
        return "Zero-MSE";
    }

    @Override
    public double calcDivergenceBetweenApproximatingNodeAndSamples(XADD context, XADD.XADDTNode approxNode, SamplingDB samples) {
        double accumulatedSqErr = 0d;
        for (int i = 0; i < samples.size(); i++) {
            Pair<Map<String, Double>, Double> element = samples.getElement(i);
            Map<String, Double> continuousVarAssign = element.getFirstEntry();
            Double exactTarget = element.getSecondEntry();

            Double eval = context.evaluate(context._hmNode2Int.get(approxNode), new HashMap<String, Boolean>() /*since the leaf does not have boolean vars*/,
                    (HashMap<String, Double>) continuousVarAssign);

            double error;
            if ((!eval.equals(exactTarget)) && (eval == 0 || exactTarget == 0)) {
                error = MAX_LOSS;
            } else error = Math.min((eval - exactTarget) * (eval - exactTarget), MAX_LOSS);

            accumulatedSqErr += (error);   // mean square error
        }

        /**
         * Note: this is only true when sampling is done via a grid.
         * In this case, the mass under each sample_i is (sample_i)/(samples.size).
         * So: (eval - approxTarget)^2 should be replaced with ([eval/samples.size]^2 - [approxTarget/samples.size]^2).
         * TODO modifiy it when a general sampling method is used.
         */
//        accumulatedSqErr /= (double) (samples.size()*samples.size()); //no good! should not be dependent on the number of samples.

        return accumulatedSqErr / (double) samples.size();
    }
}
