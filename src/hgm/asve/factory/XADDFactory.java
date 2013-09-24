package hgm.asve.factory;

import hgm.InstantiatedVariable;
import hgm.Variable;
import hgm.asve.factor.XADDFactor;
import xadd.ExprLib;
import xadd.XADD;
import xadd.XADDUtils;

import java.util.*;

/**
 * Created by Hadi M Afshar
 * Date: 11/09/13
 * Time: 12:22 AM
 * <p/>
 * A A Singleton Wrapper on xadd.XADD class.
 */
public class XADDFactory implements FactorFactory<XADDFactor>{

    private XADD _context;

    public XADDFactory() {
        _context = new XADD();
    }

    public void putContinuousVariable(Variable var, double minValue, double maxValue) {
        _context._hmMinVal.put(var.getName(), minValue);
        _context._hmMaxVal.put(var.getName(), maxValue);

        //todo: maybe they should be added to the _hsContinuousVars as well... but then what is _alContinuousVars....
    }

    public Double getMinValue(String varName) {
        return _context._hmMinVal.get(varName);
    }

    public Double getMaxValue(String varName) {
        return _context._hmMaxVal.get(varName);

    }

    public XADDFactor substitute(XADDFactor factor, HashMap<String, ExprLib.ArithExpr> valueAssignment) {
        if (valueAssignment.isEmpty()) return factor;

        int newNodeId = _context.substitute(factor.getNodeId(), valueAssignment);
        return new XADDFactor(this, "[[" + factor + "|" + valueAssignment + "]]", newNodeId);
    }

    public XADDFactor putNewFactorWithContinuousVars(/*String varName, double minValue, double maxValue, */String function) {
        ArrayList l = new ArrayList(1);
        l.add("[" + function + "]");
        int cptId = _context.buildCanonicalXADD(l);
        return new XADDFactor(this,/*varName, */function, cptId);
    }

    @Override
    public XADDFactor multiply(Collection<XADDFactor> factors) {
        int productNodeId = _context.ONE;
        for (XADDFactor f : factors)
            productNodeId = _context.applyInt(productNodeId, f.getNodeId(), XADD.PROD);
        return new XADDFactor(this, "[[PROD{" + factors.toString() + "}]]", productNodeId);
    }

    @Override
    public XADDFactor marginalize(XADDFactor factor, Variable variable) {
        int integralNodeId = _context.computeDefiniteIntegral(factor.getNodeId(), variable.getName());
        return new XADDFactor(this, "[[Integral{" + factor + "}d"+ variable + "]]", integralNodeId);
    }

    @Override
    public XADDFactor approximate(XADDFactor factor) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Set<Variable> collectScopeVars(XADDFactor factor) {
        //TODO if context works with variables then this wrapping process is not needed
        Set<String> varNames = _context.collectVars(factor.getNodeId());
        Set<Variable> vars = new HashSet<Variable>(varNames.size());
        for (String varName : varNames) {
             vars.add(new Variable(varName));
        }
        return vars;
    }

    public String getNodeString(XADDFactor factor) {
        //return factory.context.getString(nodeId);
        XADD.XADDNode root = _context.getExistNode(factor.getNodeId());
        return root.toString(true);
    }

    public Double evaluate(XADDFactor factor, Iterable<InstantiatedVariable> variableValues) {
        //todo: now only continuous variables supported
        HashMap<String, Double> continuousMap = new HashMap<String, Double>();
        for (InstantiatedVariable v : variableValues) {
            continuousMap.put(v.getVariable().getName(), Double.valueOf(v.getValue()));
        }

        return _context.evaluate(factor.getNodeId(), new HashMap<String, Boolean>(), continuousMap);
    }

    public XADDFactor scalarMultiply(XADDFactor f, double c) {
        return new XADDFactor(this, "[" + c + " TIMES " + f.toString() + "]", _context.scalarOp(f.getNodeId(), c, XADD.PROD));
    }

    //This function should not be used "ideally" since XADDFactory is meant to wrap all features of context
//    @Deprecated
//    public XADD getContext() {
//        return context;
//    }

    //TODO visualization should IDEALLY be transmitted to another class....
    public void visualize1DFactor(XADDFactor factor, String title) {
        if (factor.getScopeVars().size() != 1) throw new RuntimeException("only one variable expected!");
        if (factor.getFactory() != this) throw new RuntimeException("I just draw my own factors");

        Variable var = factor.getScopeVars().iterator().next();

        double min_val = getMinValue(var.getName());
        double max_val = getMaxValue(var.getName());

        XADDUtils.PlotXADD(_context, factor.getNodeId(), min_val, 0.1d, max_val, var.getName(), title);
//        double integral = XADDUtils.TestNormalize(factor.getFactory().getContext(), factor.getNodeId(), var);
//        if (Math.abs(integral - 1d) > 0.001d)
//            System.err.println("WARNING: distribition does not integrate out to 1: " + integral);
    }

}
