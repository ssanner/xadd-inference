package hgm.asve.cnsrv.approxator.regression.measures;

import hgm.asve.Pair;
import hgm.asve.cnsrv.approxator.regression.SamplingDB;
import xadd.XADD;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Hadi Afshar.
 * Date: 1/11/13
 * Time: 8:31 PM
 */
public class KLDivergenceMeasure implements DivergenceMeasure {
    @Override
    public String measureName() {
        return "KL-Divergence";
    }

    @Override
    public double calcDivergenceBetweenApproximatingNodeAndSamples(XADD context, XADD.XADDTNode approxNode, SamplingDB samples) {
        double klSum = 0d;
        double n = (double) samples.size();

        for (int i = 0; i < n; i++) {
            Pair<Map<String, Double>, Double> element = samples.getElement(i);
            Map<String, Double> continuousVarAssign = element.getFirstEntry();
            Double target = element.getSecondEntry();

            Double evalApprox = context.evaluate(context._hmNode2Int.get(approxNode), new HashMap<String, Boolean>() /*since the leaf does not have boolean vars*/,
                    (HashMap<String, Double>) continuousVarAssign //todo this is really bad, the evaluate() in XADD should accept maps rather than HashMaps and this casting should be removed
            );

            double p = target;// / n;  // mass
            double q = evalApprox;// / n;     // mass
            // KL divergence of approximation (q) from the exact value (p):
            if (p < 0d) {
                p = 0d; //although such a thing should never happen in theory
            }

            if (q < 0d) return Double.MAX_VALUE; // approximation should always be positive

            if (q == 0d) {
                if (p != 0d) return Double.MAX_VALUE; // such a thing should not happen in theory but it does...
                //else nothing added to klSum since 0ln0 = 0
            } else {
                //q != 0:
                klSum += Math.abs(Math.log(p / q) * p); //todo: I think abs is needed is not it?
            }
        }

        return klSum / n;
    }
}
