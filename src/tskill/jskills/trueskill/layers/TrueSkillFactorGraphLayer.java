package tskill.jskills.trueskill.layers;

import tskill.jskills.factorgraphs.Factor;
import tskill.jskills.factorgraphs.FactorGraphLayer;
import tskill.jskills.factorgraphs.Variable;
import tskill.jskills.numerics.GaussianDistribution;
import tskill.jskills.trueskill.TrueSkillFactorGraph;

public abstract class TrueSkillFactorGraphLayer<TInputVariable extends Variable<GaussianDistribution>, 
                                                TFactor extends Factor<GaussianDistribution>,
                                                TOutputVariable extends Variable<GaussianDistribution>>
    extends FactorGraphLayer
            <TrueSkillFactorGraph, GaussianDistribution, Variable<GaussianDistribution>, TInputVariable,
            TFactor, TOutputVariable> 
{
    public TrueSkillFactorGraphLayer(TrueSkillFactorGraph parentGraph)
    {
        super(parentGraph);
    }
}