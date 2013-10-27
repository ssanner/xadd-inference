package hgm.asve.cnsrv.factory;

import hgm.asve.cnsrv.approxator.EfficientPathIntegralCalculator;
import hgm.asve.cnsrv.approxator.PathIntegralOnLeafFunctionCalculator;
import hgm.asve.cnsrv.approxator.SiblingXaddApproximator;
import hgm.asve.cnsrv.factor.Factor;
import hgm.asve.cnsrv.gm.FBQuery;
import xadd.XADD;

/**
 * Created by Hadi Afshar.
 * Date: 18/10/13
 * Time: 1:18 AM
 */
public class ModelBasedXaddFactorFactoryWithSiblingApprox extends ModelBasedXaddFactorFactory {
    public ModelBasedXaddFactorFactoryWithSiblingApprox(String modelFileName, FBQuery instantiatingQuery) {
        super(modelFileName, instantiatingQuery);
    }

    //todo: I think the approximator and its parameters should eventually be fed to the system from beyond it
    @Override
    public Factor approximate(Factor factor, double maxDesiredNumberOfNodes, double siblingDifThreshold) {
        return approximateSibling(factor, maxDesiredNumberOfNodes, siblingDifThreshold,
                new EfficientPathIntegralCalculator(context)
        );
    }

    public Factor approximateSibling(Factor factor, double maxDesiredNumberOfNodes, double siblingDifThreshold, PathIntegralOnLeafFunctionCalculator calculator) {
        SiblingXaddApproximator approximator = new SiblingXaddApproximator(context, calculator);
        XADD.XADDNode approxNode = approximator.approximateXADD(context._hmInt2Node.get(factor._xadd), (int)Math.round(maxDesiredNumberOfNodes), siblingDifThreshold);
        int approxId = context._hmNode2Int.get(approxNode);
        return new Factor(approxId, context, "~" + factor.getHelpingText());
    }

}
