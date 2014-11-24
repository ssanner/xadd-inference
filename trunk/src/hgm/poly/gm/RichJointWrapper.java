package hgm.poly.gm;

import hgm.poly.Fraction;
import hgm.poly.PiecewiseExpression;

import java.util.List;

/**
 * Created by Hadi Afshar.
 * Date: 8/09/14
 * Time: 11:28 PM
 * Includes more information
 */
public class RichJointWrapper extends JointWrapper{
    private List<DeterministicFactor> eliminatedStochasticVarFactors;
    private List<String> queryVars;

    public RichJointWrapper(PiecewiseExpression<Fraction> joint, List<DeterministicFactor> eliminatedStochasticVarFactors, List<String> queryVars, double minVarLimit, double maxVarLimit) {
        super(joint, minVarLimit, maxVarLimit);
        this.eliminatedStochasticVarFactors = eliminatedStochasticVarFactors;
        this.queryVars = queryVars;
    }

    public List<DeterministicFactor> eliminatedStochasticVarFactors() {
        return eliminatedStochasticVarFactors;
    }

    public List<String> getQueryVars() {
        return queryVars;
    }

    @Override
    public int getAppropriateSampleVectorSize() {
        return queryVars.size();
    }
}
