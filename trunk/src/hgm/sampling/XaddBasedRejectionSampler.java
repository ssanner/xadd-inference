package hgm.sampling;

import hgm.utils.Utils;

import java.util.HashMap;

import xadd.XADD;
import xadd.XADD.XADDNode;

//this class is wrong...
@Deprecated
public class XaddBasedRejectionSampler extends XaddSampler {

	private VarAssignment			_initialSample;
	private double					_M;
	private HashMap<String, Double>	_lastPoint;

	public XaddBasedRejectionSampler(XADD context, XADDNode root) {
		super(context, root);

		init(null);
	}

	public XaddBasedRejectionSampler(XADD context, XADD.XADDNode root, VarAssignment initialSample) {
		super(context, root);

		init(initialSample);
	}

	public XaddBasedRejectionSampler(XADD context, XADD.XADDNode root, VarAssignment initialSample, double M) {
		super(context, root);

		init(initialSample, M);
	}

	private void init(VarAssignment initialSample) {
		double min, max, g = 1;
		for (String cVar : cVars) {
			min = context._hmMinVal.get(cVar);
			max = context._hmMaxVal.get(cVar);
			g = (max - min) * g;
		}
		init(initialSample, g + 10);
	}

	private void init(VarAssignment initialSample, final double M) {
		_initialSample = null;
		_M = M;
	}

	@Override
	public VarAssignment sample() throws SamplingFailureException {
		if (super.reusableVarAssignment == null) { // (no sample is taken yet)
			if (_initialSample == null) {
				for (String cVar : cVars) {

				}
				super.reusableVarAssignment = XaddBasedMetropolisHastingsSampler.takeInitialSample(super.context,
                        super.rootId,
                        super.cVars, super.bVars);// initialization phase:
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

	private void generateSample() {
		// the other distribution is assumed to be uniform

		double min, max, p, u, g;
		HashMap<String, Double> sample = new HashMap<String, Double>();
		boolean accepted = false;
		for (int i = 0; i < 1000 && !accepted; i++) { // TODO: rejection is changed to have few tries
			g = 1;
			for (String cVar : cVars) {
				min = context._hmMinVal.get(cVar);
				max = context._hmMaxVal.get(cVar);
				sample.put(cVar, XaddSampler.randomDoubleUniformBetween(min, max));
				g = (max - min) * g;
			}

			g = 1.0 / g; // probability
			p = super.context.evaluate(super.rootId, null, sample);
			u = XaddSampler.randomDoubleUniformBetween(0, 1);
			if (u < (p / (_M * g))) {
				accepted = true;
			}
		}

		if (!accepted)
			sample = _lastPoint;
		else
			_noAccepted++;

		for (String key : sample.keySet()) {
			super.reusableVarAssignment.getContinuousVarAssign().put(key, sample.get(key));
		}
	}

	private int			_noAccepted	= 0;
	private static int	counter		= 1;

	public void finish() {
		Utils.writeMat("rejection_accepted" + (counter++) + ".mat", "noAccepted", new double[] { (double) _noAccepted });
	}
}
