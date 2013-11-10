package hgm.asve.cnsrv.approxator.fitting;

import hgm.asve.Pair;
import xadd.XADD;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Hadi Afshar.
 * Date: 1/11/13
 * Time: 7:55 PM
 */
public class MeanSquareErrorMeasure implements DivergenceMeasure {
    @Override
    public String measureName() {
        return "MSE";
    }

    @Override
    public double calcDivergenceBetweenApproximatingNodeAndSamples(XADD context, XADD.XADDTNode approxNode, SamplingDB samples) {
        double accumulatedSqErr = 0d;
        for (int i = 0; i < samples.size(); i++) {
            Pair<Map<String, Double>, Double> element = samples.getElement(i);
            Map<String, Double> continuousVarAssign = element.getFirstEntry();
            Double exactTarget = element.getSecondEntry();

            Double eval = context.evaluate(context._hmNode2Int.get(approxNode), new HashMap<String, Boolean>() /*since the leaf does not have boolean vars*/,
                    (HashMap<String, Double>) continuousVarAssign //todo this is really bad, the evaluate() in XADD should accept maps rather than HashMaps and this casting should be removed
            );

            double error = eval - exactTarget;

           /* //for test:
            if (eval ==0 ) {
                if (error !=0) error = Double.POSITIVE_INFINITY;
            } else {
            error = error / eval; //todo: this line is added just for test, can be considered a relative MSE....
            }
            //end test.*/

//            accumulatedSqErr += (Math.abs(error)); // abs error
            accumulatedSqErr += (error * error);   // mean square error
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
