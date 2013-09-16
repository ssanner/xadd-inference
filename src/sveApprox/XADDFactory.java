package sveApprox;

import xadd.ExprLib;
import xadd.XADD;
import xadd.XADDUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by Hadi M Afshar
 * Date: 11/09/13
 * Time: 12:22 AM
 * <p/>
 * A A Singleton Wrapper on xadd.XADD class.
 */
public class XADDFactory {
    public static final boolean DEBUG = true;

    private XADD context;

    public XADDFactory() {
        context = new XADD();
    }

    public void putContinuousVariable(String varName, double minValue, double maxValue) {
        context._hmMinVal.put(varName, minValue);
        context._hmMaxVal.put(varName, maxValue);

        //todo: maybe they should be added to the _hsContinuousVars as well... but then what is _alContinuousVars....
    }

    public Double getMinValue(String varName) {
        return context._hmMinVal.get(varName);
    }

    public Double getMaxValue(String varName) {
        return context._hmMaxVal.get(varName);

    }



    public HFactor substitute(HFactor factor, HashMap<String, ExprLib.ArithExpr> valueAssignment) {
        if (valueAssignment.isEmpty()) return factor;

        int newNodeId = context.substitute(factor.getNodeId(), valueAssignment);
        return new HFactor(this, "[[" + factor + "|" + valueAssignment + "]]", newNodeId);
    }

    public HFactor putNewFactorWithContinuousVars(/*String varName, double minValue, double maxValue, */String function) {
        ArrayList l = new ArrayList(1);
        l.add("[" + function + "]");
        int cptId = context.buildCanonicalXADD(l);
        return new HFactor(this,/*varName, */function, cptId);
    }

    public HFactor multiply(Iterable<HFactor> factors) {
        int productNodeId = context.ONE;
        for (HFactor f : factors)
            productNodeId = context.applyInt(productNodeId, f.getNodeId(), XADD.PROD);
        return new HFactor(this, "[[PROD{" + factors.toString() + "}]]", productNodeId);
    }

    public HFactor definiteIntegral(HFactor factor, String varName) {
        int integralNodeId = context.computeDefiniteIntegral(factor.getNodeId(), varName);
        return new HFactor(this, "[[Integral{" + factor + "}d"+ varName + "]]", integralNodeId);
    }

    public Set<String> collectScopeVars(HFactor factor) {
        return context.collectVars(factor.getNodeId());
    }

    public String getNodeString(HFactor factor) {
        //return factory.context.getString(nodeId);
        XADD.XADDNode root = context.getExistNode(factor.getNodeId());
        return root.toString(true);
    }

    public Double evaluate(HFactor factor, Iterable<VariableValue> variableValues) {
        //todo: now only continuous variables supported
        HashMap<String, Double> continuousMap = new HashMap<String, Double>();
        for (VariableValue v : variableValues) {
            continuousMap.put(v.getVariable(), Double.valueOf(v.getValue()));
        }

        return context.evaluate(factor.getNodeId(), new HashMap<String, Boolean>(), continuousMap);
    }

    public HFactor scalarMultiply(HFactor f, double c) {
        return new HFactor(this, "[" + c + " TIMES " + f.toString() + "]", context.scalarOp(f.getNodeId(), c, XADD.PROD));
    }

    //This function should not be used "ideally" since XADDFactory is meant to wrap all features of context
//    @Deprecated
//    public XADD getContext() {
//        return context;
//    }

    //TODO visualization should IDEALLY be transmitted to another class....
    public void visualize1DFactor(HFactor factor, String title) {
        if (factor.getScopeVars().size() != 1) throw new RuntimeException("only one variable expected!");
        if (factor.getFactory() != this) throw new RuntimeException("I just draw my own factors");

        String var = factor.getScopeVars().iterator().next();

        double min_val = getMinValue(var);
        double max_val = getMaxValue(var);

        XADDUtils.PlotXADD(context, factor.getNodeId(), min_val, 0.1d, max_val, var, title);
//        double integral = XADDUtils.TestNormalize(factor.getFactory().getContext(), factor.getNodeId(), var);
//        if (Math.abs(integral - 1d) > 0.001d)
//            System.err.println("WARNING: distribition does not integrate out to 1: " + integral);
    }

}
