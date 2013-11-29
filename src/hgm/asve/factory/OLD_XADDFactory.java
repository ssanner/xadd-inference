package hgm.asve.factory;

import hgm.InstantiatedVariable;
import hgm.Variable;
import hgm.asve.cnsrv.approxator.EfficientPathIntegralCalculator;
import hgm.asve.cnsrv.approxator.MassThresholdXaddApproximator;
import hgm.asve.factor.OLD_XADDFactor;
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
@Deprecated
public class OLD_XADDFactory implements OLD_FactorFactory<OLD_XADDFactor> {

    private XADD _context;
    private double _massThreshold;
    private double _volumeThreshold;

    public OLD_XADDFactory(double approximationMassThreshold, double approximationVolumeThreshold) {
        _context = new XADD();
        _massThreshold = approximationMassThreshold;
        _volumeThreshold = approximationVolumeThreshold;
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

    public OLD_XADDFactor substitute(OLD_XADDFactor factor, HashMap<String, ExprLib.ArithExpr> valueAssignment) {
        if (valueAssignment.isEmpty()) return factor;

        int newNodeId = _context.substitute(factor.getNodeId(), valueAssignment);
        return new OLD_XADDFactor(this, factor.getAssociatedVar(), "[[" + factor + "|" + valueAssignment + "]]", newNodeId);
    }

    public OLD_XADDFactor putNewFactorWithContinuousAssociatedVar(Variable associatedVar, /*String varName, double minValue, double maxValue, */String function) {
        //make sure the associated variable is registered:
//todo: do something with this:
//        if ((_context._hmMinVal.get(associatedVar)==null) || (_context._hmMaxVal.get(associatedVar) == null)) {
//            throw new RuntimeException("unregistered variable");
//        }

        ArrayList l = new ArrayList(1);
        l.add("[" + function + "]");
        int cptId = _context.buildCanonicalXADD(l);
        return new OLD_XADDFactor(this, associatedVar, function, cptId);
    }

    @Override
    public OLD_XADDFactor multiply(Collection<OLD_XADDFactor> factors) {
        int productNodeId = _context.ONE;
        for (OLD_XADDFactor f : factors)
            productNodeId = _context.applyInt(productNodeId, f.getNodeId(), XADD.PROD);
        return new OLD_XADDFactor(this, null, "[[PROD{" + factors.toString() + "}]]", productNodeId);
    }

    @Override
    public OLD_XADDFactor marginalize(OLD_XADDFactor factor, Variable variable) {
        int integralNodeId = _context.computeDefiniteIntegral(factor.getNodeId(), variable.getName());
        return new OLD_XADDFactor(this, null, "[[Marginalize{" + factor + "}d" + variable + "]]", integralNodeId);
    }

    @Override
    public OLD_XADDFactor approximate(OLD_XADDFactor factor) {
        MassThresholdXaddApproximator approximator = new MassThresholdXaddApproximator(_context, new EfficientPathIntegralCalculator(), _massThreshold, _volumeThreshold);
        int approximatedNodeId =
                _context._hmNode2Int.get(approximator.approximateXadd(_context._hmInt2Node.get(factor.getNodeId())));//_context.approximateXADD(factor.getNodeId(), _massThreshold, _volumeThreshold);
        return new OLD_XADDFactor(this, null, "[[Approx{" + factor + "}]]", approximatedNodeId);
    }

    public Set<Variable> collectScopeVars(OLD_XADDFactor factor) {
        //TODO if context works with variables then this wrapping process is not needed
        Set<String> varNames = _context.collectVars(factor.getNodeId());
        Set<Variable> vars = new HashSet<Variable>(varNames.size());
        for (String varName : varNames) {
            vars.add(new Variable(varName));
        }
        return vars;
    }

    public String getNodeString(OLD_XADDFactor factor) {
        //return factory.context.getString(nodeId);
        XADD.XADDNode root = _context.getExistNode(factor.getNodeId());
        return root.toString(true);
    }

    public Double evaluate(OLD_XADDFactor factor, Iterable<InstantiatedVariable> variableValues) {
        //todo: now only continuous variables are supported
        HashMap<String, Double> continuousMap = new HashMap<String, Double>();
        for (InstantiatedVariable v : variableValues) {
            continuousMap.put(v.getVariable().getName(), Double.valueOf(v.getValue()));
        }

        return _context.evaluate(factor.getNodeId(), new HashMap<String, Boolean>(), continuousMap);
    }

    public OLD_XADDFactor scalarMultiply(OLD_XADDFactor f, double c) {
        return new OLD_XADDFactor(this, null, "[" + c + " TIMES " + f.toString() + "]", _context.scalarOp(f.getNodeId(), c, XADD.PROD));
    }

    //TODO visualization should IDEALLY be carried to another class....
    public void visualize1DFactor(OLD_XADDFactor factor, String title) {
        if (factor.getScopeVars().size() != 1) throw new RuntimeException("only one variable expected!");
        if (factor.getFactory() != this) throw new RuntimeException("I can just draw my own factors");

        Variable var = factor.getScopeVars().iterator().next();

        double min_val = getMinValue(var.getName());
        double max_val = getMaxValue(var.getName());

        XADDUtils.PlotXADD(_context, factor.getNodeId(), min_val, 0.1d, max_val, var.getName(), title);
//        double integral = XADDUtils.TestNormalize(factor.getFactory().getContext(), factor.getNodeId(), var);
//        if (Math.abs(integral - 1d) > 0.001d)
//            System.err.println("WARNING: distribition does not integrate out to 1: " + integral);
    }

}
