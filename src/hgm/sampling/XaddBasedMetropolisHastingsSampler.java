package hgm.sampling;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;

import xadd.XADD;
import xadd.XADD.XADDNode;

@Deprecated
public class XaddBasedMetropolisHastingsSampler extends XaddSampler {

	private static Random			_randomGenerator	= new Random();
	private HashMap<String, Double>	_lastPoint;
	private int						_iteration			= 0;
	private VarAssignment			_initialSample;
	private int						_leapSize			= 10;

	public XaddBasedMetropolisHastingsSampler(XADD context, XADDNode root) {
		super(context, root);

		init(null, 1);
	}

	private void init(VarAssignment initialSample, final int leapsize) {
		_initialSample = null;
		_leapSize = leapsize;
	}

	public XaddBasedMetropolisHastingsSampler(XADD context, XADD.XADDNode root, VarAssignment initialSample) {
		super(context, root);

		init(initialSample, 1);
	}

	public XaddBasedMetropolisHastingsSampler(XADD context, XADD.XADDNode root, VarAssignment initialSample, int leapSize) {
		super(context, root);

		init(initialSample, leapSize);
	}

	@Override
	public VarAssignment sample() throws SamplingFailureException {
		if (super.reusableVarAssignment == null) { // (no sample is taken yet)
			if (_initialSample == null) {
				super.reusableVarAssignment = takeInitialSample(super.context, super.rootId, super.cVars, super.bVars);// initialization phase:
			} else {
				super.reusableVarAssignment = _initialSample;
			}
			_lastPoint = (java.util.HashMap<String, Double>) reusableVarAssignment.getContinuousVarAssign().clone();

			if (reusableVarAssignment.getBooleanVarAssign().size() > 0) {
				throw new SamplingFailureException("There are boolean variables that are not handled here.");
			}
		} else {
			generateSample();
		}

		return reusableVarAssignment;
	}

	public static VarAssignment takeInitialSample(XADD context, int rootId, List<String> cVars, List<String> bVars) {
		HashMap<String, Boolean> boolAssign = new HashMap<String, Boolean>(bVars.size());
		HashMap<String, Double> contAssign = new HashMap<String, Double>(cVars.size());

		for (String bVar : bVars) {
			boolAssign.put(bVar, XaddSampler.randomBoolean());
		}

		do { // generate a sample that does not have zero probability
			for (String cVar : cVars) {
				Double minVarValue = context._hmMinVal.get(cVar);
				Double maxVarValue = context._hmMaxVal.get(cVar);
				if (maxVarValue == null)
					throw new RuntimeException("The max of scope of var " + cVar + " is unknown");
				if (minVarValue == null)
					throw new RuntimeException("The min of scope of var " + cVar + " is unknown");

				contAssign.put(cVar, XaddSampler.randomDoubleUniformBetween(minVarValue, maxVarValue));
			}
		} while (context.evaluate(rootId, null, contAssign) == null || context.evaluate(rootId, null, contAssign) == 0);

		return new VarAssignment(boolAssign, contAssign);
	}

	// assumes the lastpoint is already initialized ....
	public void generateSample() {
		final double transitionVariance = 1.;

		HashMap<String, Double> w = _lastPoint;
		_iteration++;

		double a, u, pw = -1, pwp = -1;

		for (int i = 0; i < _leapSize; i++) {
			HashMap<String, Double> wp = transit(w, transitionVariance);
			pwp = logProbability(wp);

			if (pw == -1) {
				pw = logProbability(w);
			}
			a = Math.exp(logRatio(pw, pwp));
			u = _randomGenerator.nextDouble();
			if (a > 0 && u <= a) {
				copyTo(wp, w);
				_lastPoint = w;
				pw = pwp;
			}
		}

		for (String key : w.keySet()) {
			super.reusableVarAssignment.getContinuousVarAssign().put(key, w.get(key));
		}
	}

	private void copyTo(final HashMap<String, Double> from, HashMap<String, Double> to) {
		for (String key : from.keySet()) {
			to.put(key, from.get(key));
		}
	}

	private double logProbability(HashMap<String, Double> w) {
		return Math.log(super.context.evaluate(super.rootId, null, w));
	}

	private double logRatio(double pw, double pwp) {
		return pwp - pw;
	}

	private static HashMap<String, Double> transit(HashMap<String, Double> w, double variance) {
		HashMap<String, Double> wp = new HashMap<String, Double>();
		for (String key : w.keySet()) {
			wp.put(key, w.get(key) + variance * _randomGenerator.nextGaussian());
		}

		return wp;
	}

	private static Double[] getDoubleArray(HashMap<String, Double> a) {
		return a.values().toArray(new Double[] {});
		/* double[] d = new double[a.size()]; int i = 0; for (String var : a.keySet()) { d[i++] = a.get(var); }
		 * 
		 * return d; */
	}

	private void toVarAssignment(VarAssignment v, java.util.Hashtable<String, Double> a) {
		for (String key : v.getContinuousVarAssign().keySet()) {
			v.getContinuousVarAssign().put(key, a.get(key));
		}
	}

	private void MapToTable(Hashtable<String, Double> table, HashMap<String, Double> map) {
		table.clear();
		for (String key : map.keySet()) {
			table.put(key, map.get(key));
		}
	}

	private void tableToMap(HashMap<String, Double> map, Hashtable<String, Double> table) {
		map.clear();
		for (String key : table.keySet()) {
			map.put(key, table.get(key));
		}
	}

}
