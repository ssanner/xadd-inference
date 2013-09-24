package hgm.asve.factor;

import hgm.Configurations;
import hgm.InstantiatedVariable;
import hgm.Variable;
import hgm.asve.factory.XADDFactory;
import xadd.ExprLib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
* Created by Hadi M Afshar
* Date: 13/09/13
* Time: 8:55 PM
*/
public class XADDFactor implements IFactor{
    private XADDFactory _factory;
    private Set<Variable> _scopeVars;
    private String _factorString;
    private int _nodeId; //hook of the xadd node in the factory

    public XADDFactor(XADDFactory factory, String factorString, int nodeId) {
        this._factory = factory;
        this._factorString = factorString;
        this._nodeId = nodeId;

        _scopeVars = factory.collectScopeVars(this);
    }

    @Override
    public Variable getAssociatedVar() {
        return null;  //todo
    }

    public int getNodeId() {
        return _nodeId;
    }

    @Override
    public Set<Variable> getScopeVars() {
        return _scopeVars;
    }

    public XADDFactory getFactory() {
        return _factory;
    }

    @Override
    public String toString() {
        return _factorString;
    }

    public String getXADDNodeString() {
        return _factory.getNodeString(this);
    }

    public XADDFactor instantiate(Set<InstantiatedVariable> valueAssignment) {
        HashMap<String, ExprLib.ArithExpr> relevantValueAssignment = new HashMap<String, ExprLib.ArithExpr>();
        for (InstantiatedVariable instantiatedVariable : valueAssignment) {
            if (_scopeVars.contains(instantiatedVariable.getVariable())) {
                relevantValueAssignment.put(instantiatedVariable.getVariable().getName(), new ExprLib.DoubleExpr(
                        Double.valueOf(instantiatedVariable.getValue()))); //TODO: note that this (and many other things) only work for continuousVariables
            }
        }

        return _factory.substitute(this, relevantValueAssignment);
    }

    public XADDFactor normalize() {
        XADDFactor normalizationFactor = this;
        for (Variable var : getScopeVars()) {
            normalizationFactor = _factory.marginalize(normalizationFactor, var);
        }

        double norm = _factory.evaluate(normalizationFactor, new ArrayList<InstantiatedVariable>());
        if(Configurations.DEBUG) {
            System.out.println("normalizing factor = " + (1.00d / norm));
        }
        return _factory.scalarMultiply(this, 1.00d / norm);
    }

}
