package hgm.poly.reports.sg;

import hgm.poly.gm.JointToSampler;
import hgm.poly.gm.JointWrapper;
import hgm.poly.gm.RichJointWrapper;
import hgm.poly.gm.SymbolicGraphicalModelHandler;
import hgm.poly.sampling.SamplerInterface;

/**
 * Created by Hadi Afshar.
 * Date: 8/09/14
 * Time: 11:21 PM
 */
public class QuerySelecterSampler implements JointToSampler{
    private JointToSampler innerJointToSampler;
    public QuerySelecterSampler(JointToSampler innerJointToSampler) {
        this.innerJointToSampler = innerJointToSampler;
    }

    @Override
    public SamplerInterface makeSampler(JointWrapper jointWrapper) {
        if (!(jointWrapper instanceof RichJointWrapper)) {
            throw new RuntimeException("RichJointWrapper expected");
        }
        RichJointWrapper richJW = (RichJointWrapper) jointWrapper;
        SymbolicGraphicalModelHandler handler = new SymbolicGraphicalModelHandler();
        return handler.makeSampler(richJW.getJoint(), richJW.eliminatedStochasticVarFactors(), richJW.getQueryVars(),
                richJW.getMinLimitForAllVars(), richJW.getMaxLimitForAllVars(), innerJointToSampler);
    }

    @Override
    public String getName() {
        return innerJointToSampler.getName();
    }
}
