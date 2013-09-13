package sveApprox;

import xadd.ExprLib;
import xadd.XADD;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by Hadi M Afshar
 * Date: 11/09/13
 * Time: 12:22 AM
 * <p/>
 * A A Singleton Wrapper on xadd.XADD class.
 */
public class XADDFactory {
    private XADD context;

    public XADDFactory() {
        context = new XADD();
    }

    public void putContinuousVariable(String varName, double minValue, double maxValue) {
        context._hmMinVal.put(varName, minValue);
        context._hmMaxVal.put(varName, maxValue);

        //todo: maybe they should be added to the _hsContinuousVars as well... but then what is _alContinuousVars....
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
        return new HFactor(this, "[[Integral{" + factor + "}]]", integralNodeId);
    }

    public Set<String> collectScopeVars(HFactor factor) {
        return context.collectVars(factor.getNodeId());
    }

    public String getNodeString(HFactor factor) {
        //return factory.context.getString(nodeId);
        XADD.XADDNode root = context.getExistNode(factor.getNodeId());
        return root.toString(true);
    }
}
