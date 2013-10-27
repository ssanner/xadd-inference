package hgm.asve.cnsrv.factor;

import hgm.asve.cnsrv.factory.BaselineXaddFactorFactory;
import hgm.asve.cnsrv.factory.ModelBasedXaddFactorFactory;
import xadd.XADDUtils;

import java.util.Iterator;

/**
 * Created by Hadi Afshar.
 * Date: 10/10/13
 * Time: 5:55 PM
 */
public class FactorVisualizer {
    private BaselineXaddFactorFactory factory;

    public FactorVisualizer(BaselineXaddFactorFactory factory) {
        this.factory = factory;
    }

    //*******************************************************************************************
    public void visualizeFactor(Factor factor, String title){
        /*if (title == null) {
            title = factory.getQuery().getQueryVariables()
                    + " | " + factory.getQuery().getBooleanInstantiatedEvidence()
                    + ", " + factory.getQuery().getContinuousInstantiatedEvidence();
        }*/

        if (factor.getScopeVars().size() == 1) {
            visualize1DFactor(factor, title);
//            ExportData(norm_result, q._sFilename + ".txt"); //todo do this as well...
        } else if (factor.getScopeVars().size() == 2) {
            visualize2DFactor(factor, title);
//            Export3DData(norm_result, q._sFilename);   //todo...
        }

    }


    private void visualize1DFactor(Factor factor, String title) {
        if (factor.getScopeVars().size() != 1) throw new RuntimeException("only one variable expected!");

        String var = factor.getScopeVars().iterator().next();

        double min_val = factory.getMinValue(var);
        double max_val = factory.getMaxValue(var);

//        System.out.println("min_val = " + min_val);
//        System.out.println("max_val = " + max_val);

        XADDUtils.PlotXADD(factory.getContext(), factor._xadd, min_val, 0.1d, max_val, var, title);
//        double integral = XADDUtils.TestNormalize(factor.getFactory().getContext(), factor.getNodeId(), var);
//        if (Math.abs(integral - 1d) > 0.001d)
//            System.err.println("WARNING: distribition does not integrate out to 1: " + integral);
    }

    private void visualize2DFactor(Factor factor, String title) {

        Iterator<String> iterator = factor.getScopeVars().iterator();
        String varX = iterator.next();
        String varY = iterator.next();
        double min_val_x = factory.getContext()._hmMinVal.get(varX);
        double max_val_x = factory.getContext()._hmMaxVal.get(varX);
        double min_val_y = factory.getContext()._hmMinVal.get(varY);
        double max_val_y = factory.getContext()._hmMaxVal.get(varY);
        XADDUtils.Plot3DSurfXADD(factor._localContext, factor._xadd,
                min_val_x, 0.5d, max_val_x,
                min_val_y, 0.5d, max_val_y,
                varX, varY, title);
    }

//*********************************************************

}
