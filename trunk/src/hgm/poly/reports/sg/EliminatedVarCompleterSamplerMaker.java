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
 *
 * In the presence of deterministic evidence some vars are omitted that should be recovered...
 */
public class EliminatedVarCompleterSamplerMaker implements JointToSampler{
    private JointToSampler innerJointToSampler;
    public EliminatedVarCompleterSamplerMaker(JointToSampler innerJointToSampler) {
        this.innerJointToSampler = innerJointToSampler;
    }

    @Override
    public SamplerInterface makeSampler(JointWrapper jointWrapper) {
        if (!(jointWrapper instanceof RichJointWrapper)) {
            throw new RuntimeException("RichJointWrapper expected");
        }
        RichJointWrapper richJW = (RichJointWrapper) jointWrapper;
        SymbolicGraphicalModelHandler handler = new SymbolicGraphicalModelHandler();
        return handler.makeCompletedSampler(richJW, innerJointToSampler);//(richJW.getJoint(), richJW.getEliminatedStochasticVarFactors(), /*richJW.getQueryVars(),*/ richJW.getMinLimitForAllVars(), richJW.getMaxLimitForAllVars(), innerJointToSampler);
    }

    @Override
    public String getName() {
        return innerJointToSampler.getName();
    }
}
