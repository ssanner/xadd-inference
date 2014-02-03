package hgm.sampling;

import java.util.HashMap;

import xadd.XADD;
import xadd.XADD.XADDNode;

public class RejectionSampler extends Sampler {

	private VarAssignment	_initialSample;
	private int				_M;

	public RejectionSampler(XADD context, XADDNode root) {
		super(context, root);

		init(null);
	}

	public RejectionSampler(XADD context, XADD.XADDNode root, VarAssignment initialSample) {
		super(context, root);

		init(initialSample);
	}

	public RejectionSampler(XADD context, XADD.XADDNode root, VarAssignment initialSample, int leapSize) {
		super(context, root);

		init(initialSample, leapSize);
	}

	private void init(VarAssignment initialSample) {
		init(initialSample, 100);
	}

	private void init(VarAssignment initialSample, final int M) {
		_initialSample = null;
		_M = M;
	}

	@Override
	public VarAssignment sample() throws SamplingFailureException {
		if (super.reusableVarAssignment == null) { // (no sample is taken yet)
			if (_initialSample == null) {
				super.reusableVarAssignment = MetropolisHastingsSampler.takeInitialSample(super.context, super.rootId,
                        super.cVars, super.bVars);// initialization phase:
			} else {
				super.reusableVarAssignment = _initialSample;
			}

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
		do {
			g = 1;
			for (String cVar : cVars) {
				min = context._hmMinVal.get(cVar);
				max = context._hmMaxVal.get(cVar);
				sample.put(cVar, Sampler.randomDoubleUniformBetween(min, max));
				g = (max - min) * g;
			}

			g = 1.0 / g; // probability
			p = super.context.evaluate(super.rootId, null, sample);
			u = Sampler.randomDoubleUniformBetween(0, 1);
			if (u < p / (_M * g)) {
				accepted = true;
			}
		} while (!accepted);

		for (String key : sample.keySet()) {
			super.reusableVarAssignment.getContinuousVarAssign().put(key, sample.get(key));
		}
	}
}
