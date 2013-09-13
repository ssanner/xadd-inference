package hadiPractice;

import sve.GraphicalModel;
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
        return new HFactor("[[" + factor.factorString + "|" + valueAssignment + "]]", newNodeId);
    }

    public HFactor putNewFactorWithContinuousVars(/*String varName, double minValue, double maxValue, */String function) {
        ArrayList l = new ArrayList(1);
        l.add("[" + function + "]");
        int cptId = context.buildCanonicalXADD(l);
        return new HFactor(/*varName, */function, cptId);
    }

    public HFactor multiply(Iterable<HFactor> factors) {
        int productNodeId = context.ONE;
        for (HFactor f : factors)
            productNodeId = context.applyInt(productNodeId, f.getNodeId(), XADD.PROD);
        return new HFactor("[[PROD{" + factors.toString() + "}]]", productNodeId);
    }

    public HFactor definiteIntegral(HFactor factor, String varName) {
        int integralNodeId = context.computeDefiniteIntegral(factor.getNodeId(), varName);
        return new HFactor("[[Integral{" + factor + "}]]", integralNodeId);
    }

    public class HFactor {
        private XADDFactory factory = XADDFactory.this;
        private Set<String> scopeVars;
        private String factorString;
        private int nodeId; //hook of the density node in the factory

        protected HFactor(String factorString, int nodeId) {
            this.factorString = factorString;
            this.nodeId = nodeId;

            scopeVars = factory.context.collectVars(nodeId);
        }

        public int getNodeId() {
            return nodeId;
        }

        public Set<String> getScopeVars() {
            return scopeVars;
        }

        public XADDFactory getFactory() {
            return factory;
        }

        @Override
        public String toString() {
            return factorString;
        }

        public String getXADDNodeString() {
            //return factory.context.getString(nodeId);
            XADD.XADDNode root = context.getExistNode(nodeId);
            return root.toString(true);
        }

        public HFactor instantiate(Set<VariableValue> valueAssignment) {
            HashMap<String, ExprLib.ArithExpr> relevantValueAssignment = new HashMap<String, ExprLib.ArithExpr>();
            for (VariableValue variableValue : valueAssignment) {
                if (scopeVars.contains(variableValue.getVariable())) {
                    relevantValueAssignment.put(variableValue.getVariable(), new ExprLib.DoubleExpr(
                            Double.valueOf(variableValue.getValue()))); //TODO: note that this (and many other things) only work for continuousVariables
                }
            }

            return factory.substitute(this, relevantValueAssignment);
        }
    }   //end inner class.
}
