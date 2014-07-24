package hgm.poly.reports;

import hgm.asve.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Hadi Afshar.
 * Date: 19/05/14
 * Time: 11:45 AM
 */
public class AlgorithmDeathLimitKeeper {
    //#Dim and #Data in which (and presumably after which) each algorithm dies:
    private Map<Db2Sampler, Pair<Integer, Integer>> alg2DeathDimData;

    public AlgorithmDeathLimitKeeper(List<Db2Sampler> algorithms) {
        alg2DeathDimData = new HashMap<Db2Sampler, Pair<Integer, Integer>>(algorithms.size());
        for (Db2Sampler alg : algorithms) {
            alg2DeathDimData.put(alg, new Pair<Integer, Integer>(Integer.MAX_VALUE /*dim*/, Integer.MAX_VALUE /*data*/));
        }
    }

    public void recordDeath(Db2Sampler algorithm, int numDims, int numData) {
         alg2DeathDimData.put(algorithm, new Pair<Integer, Integer>(numDims, numData));
    }

    public boolean algorithmWillDie(Db2Sampler algorithm, int numDims, int numData) {
        Pair<Integer, Integer> dimDataDeath = alg2DeathDimData.get(algorithm);
        return (dimDataDeath.getFirstEntry() <= numDims && dimDataDeath.getSecondEntry() <= numData);
    }
}
