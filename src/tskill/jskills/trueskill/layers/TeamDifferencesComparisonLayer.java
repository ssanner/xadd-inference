package tskill.jskills.trueskill.layers;

import tskill.jskills.GameInfo;
import tskill.jskills.factorgraphs.DefaultVariable;
import tskill.jskills.factorgraphs.Variable;
import tskill.jskills.numerics.GaussianDistribution;
import tskill.jskills.trueskill.DrawMargin;
import tskill.jskills.trueskill.TrueSkillFactorGraph;
import tskill.jskills.trueskill.factors.GaussianFactor;
import tskill.jskills.trueskill.factors.GaussianGreaterThanFactor;
import tskill.jskills.trueskill.factors.GaussianWithinFactor;

public class TeamDifferencesComparisonLayer extends
    TrueSkillFactorGraphLayer<Variable<GaussianDistribution>, GaussianFactor, DefaultVariable<GaussianDistribution>>
{
    private final double _Epsilon;
    private final int[] _TeamRanks;

    public TeamDifferencesComparisonLayer(TrueSkillFactorGraph parentGraph, int[] teamRanks)
    {
        super(parentGraph);
        _TeamRanks = teamRanks;
        GameInfo gameInfo = ParentFactorGraph.getGameInfo();
        _Epsilon = DrawMargin.GetDrawMarginFromDrawProbability(gameInfo.getDrawProbability(), gameInfo.getBeta());
    }

    @Override
    public void BuildLayer()
    {
        for (int i = 0; i < getInputVariablesGroups().size(); i++)
        {
            boolean isDraw = (_TeamRanks[i] == _TeamRanks[i + 1]);
            Variable<GaussianDistribution> teamDifference = getInputVariablesGroups().get(i).get(0);

            GaussianFactor factor =
                isDraw
                    ? (GaussianFactor) new GaussianWithinFactor(_Epsilon, teamDifference)
                    : new GaussianGreaterThanFactor(_Epsilon, teamDifference);

            AddLayerFactor(factor);
        }
    }
}