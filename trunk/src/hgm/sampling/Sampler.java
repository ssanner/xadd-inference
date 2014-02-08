package hgm.sampling;

import xadd.XADD;

import java.util.*;

/**
 * Created by Hadi Afshar. Date: 3/01/14 Time: 11:02 PM
 */
public abstract class Sampler {

	private static Random			random					= new Random();
	protected XADD					context;
	protected XADD.XADDNode			root;
	protected int					rootId;
	protected List<String>			cVars					= new ArrayList<String>();
	protected List<String>			bVars					= new ArrayList<String>();
	protected Map<String, Double>	cVarMins;
	protected Map<String, Double>	cVarMaxes;
	protected VarAssignment			reusableVarAssignment	= null;

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
			} else
				cVars.add(var);
		}

		// todo: I think these parameters are only used for visualization and should not be used ....
		for (String var : cVars) {
			Double min = context._hmMinVal.get(var);
			Double max = context._hmMaxVal.get(var);
			if (min == null)
				throw new SamplingFailureException("No min value for sampling from variable " + var);
			if (max == null)
				throw new SamplingFailureException("No max value for sampling from variable " + var);
			cVarMins.put(var, min); // todo check how XADD works for min and max of boolean variables...
			cVarMaxes.put(var, max);
		}
	}

	/**
	 * @return sample variable vector in the root XADD
	 */
	public abstract VarAssignment sample() throws SamplingFailureException;

	protected static double randomDoubleUniformBetween(double min, double max) {
		return random.nextDouble() * (max - min) + min;
	}

	protected static boolean randomBoolean() {
		return random.nextBoolean();
	}

	public void finish() {

	}
}
