package sveApprox;

import xadd.ExprLib;
import xadd.XADD;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
* Created by Hadi M Afshar
* Date: 13/09/13
* Time: 8:55 PM
*/
public class HFactor {
    private XADDFactory factory;
    private Set<String> scopeVars;
    private String factorString;
    private int nodeId; //hook of the density node in the factory

    HFactor(XADDFactory factory, String factorString, int nodeId) {
        this.factory = factory;
        this.factorString = factorString;
        this.nodeId = nodeId;

        scopeVars = factory.collectScopeVars(this);
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
        return factory.getNodeString(this);
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

    public HFactor normalize() {
        HFactor normalizationFactor = this;
        for (String var : getScopeVars()) {
            normalizationFactor = factory.definiteIntegral(normalizationFactor, var);
        }

        double norm = factory.evaluate(normalizationFactor, new ArrayList<VariableValue>());
        if(XADDFactory.DEBUG) {
            System.out.println("normalizing factor = " + (1.00d / norm));
        }
        return factory.scalarMultiply(this, 1.00d / norm);
    }

}
