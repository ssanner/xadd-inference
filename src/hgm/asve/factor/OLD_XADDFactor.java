package hgm.asve.factor;

import hgm.Configurations;
import hgm.InstantiatedVariable;
import hgm.Variable;
import hgm.asve.factory.OLD_XADDFactory;
import xadd.ExprLib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by Hadi M Afshar
 * Date: 13/09/13
 * Time: 8:55 PM
 */
@Deprecated
public class OLD_XADDFactor implements OLD_IFactor {
    private OLD_XADDFactory _factory;
    private Set<Variable> _scopeVars;
    private String _factorString;
    private int _nodeId; //hook of the xadd node in the factory
    private Variable _associatedVar;

    public OLD_XADDFactor(OLD_XADDFactory factory, Variable associatedVar, String factorString, int nodeId) {
        this._factory = factory;
        this._factorString = factorString;
        this._nodeId = nodeId;
        this._associatedVar = associatedVar;

        _scopeVars = factory.collectScopeVars(this);

        //test:
        if (associatedVar != null && !_scopeVars.contains(associatedVar))
            throw new RuntimeException("associated factor not in scope!");
    }

    @Override
    public Variable getAssociatedVar() {
        return _associatedVar;
    }

    public int getNodeId() {
        return _nodeId;
    }

    @Override
    public Set<Variable> getScopeVars() {
        return _scopeVars;
    }

    public OLD_XADDFactory getFactory() {
        return _factory;
    }

    @Override
    public String toString() {
        return _factorString;
    }

    public String getXADDNodeString() {
        return _factory.getNodeString(this);
    }

    public OLD_XADDFactor instantiate(Set<InstantiatedVariable> valueAssignment) {
        HashMap<String, ExprLib.ArithExpr> relevantValueAssignment = new HashMap<String, ExprLib.ArithExpr>();
        for (InstantiatedVariable instantiatedVariable : valueAssignment) {
            if (_scopeVars.contains(instantiatedVariable.getVariable())) {
                relevantValueAssignment.put(instantiatedVariable.getVariable().getName(), new ExprLib.DoubleExpr(
                        Double.valueOf(instantiatedVariable.getValue()))); //TODO: note that this (and many other things) only work for continuousVariables
            }
        }

        return _factory.substitute(this, relevantValueAssignment);
    }

    public OLD_XADDFactor normalize() {
        OLD_XADDFactor normalizationFactor = this;
        for (Variable var : getScopeVars()) {
            normalizationFactor = _factory.marginalize(normalizationFactor, var);
        }

        double norm = _factory.evaluate(normalizationFactor, new ArrayList<InstantiatedVariable>());
        if (Configurations.DEBUG) {
            System.out.println("normalizing factor = " + (1.00d / norm));
        }
        return _factory.scalarMultiply(this, 1.00d / norm);
    }

}
