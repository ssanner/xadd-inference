package hgm.sampling;

import xadd.XADD;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 3/01/14
 * Time: 11:02 PM
 */
public abstract class Sampler {
    private Random random = new Random();
    protected XADD context;
    protected XADD.XADDNode root;
    protected int rootId;
    protected List<String> cVars = new ArrayList<String>();
    protected List<String> bVars = new ArrayList<String>();
    protected Map<String, Double> cVarMins;
    protected Map<String, Double> cVarMaxes;
    protected VarAssignment reusableVarAssignment = null;


    public Sampler(XADD context, XADD.XADDNode root) {
        this.context = context;
        rootId = context._hmNode2Int.get(root);
        this.root = root;
        cVarMaxes = new HashMap<String, Double>(cVars.size());
        cVarMins = new HashMap<String, Double>(cVars.size());

        HashSet<String> allVars = root.collectVars();
        for (String var : allVars) {
            if (context._hsBooleanVars.contains(var)) {
                bVars.add(var);
            } else cVars.add(var);
        }

        for (String var : cVars) {
            cVarMins.put(var, context._hmMinVal.get(var)); //todo check how XADD works for min and max of boolean variables...
            cVarMaxes.put(var, context._hmMaxVal.get(var));
        }
    }

    /**
     * @return sample variable vector in the root XADD
     */
    public abstract VarAssignment sample() throws SamplingFailureException;

    protected double randomDoubleUniformBetween(double min, double max) {
        return random.nextDouble() * (max - min) + min;
    }

    protected boolean randomBoolean() {
        return random.nextBoolean();
    }
}
