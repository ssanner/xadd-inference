package hgm.asve.cnsrv.factory;

import hgm.asve.cnsrv.approxator.*;
import hgm.asve.cnsrv.factor.Factor;
import hgm.asve.cnsrv.factor.FactorVisualizer;
import xadd.XADD;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 22/10/13
 * Time: 6:31 PM
 */
public class BaselineXaddFactorFactory implements FactorFactory<Factor> {
    private XADD context;
    protected FactorVisualizer visualizer;
    private Set<Factor> permanentFactors;

    protected Approximator approximator;

    private Factor one;
    private Factor zero; //pos/neg infinite may be needed as well...


    public BaselineXaddFactorFactory(XADD context, Approximator approximator) {
        this.context = context;
        this.approximator = approximator;

        one = new Factor(context.ONE, context, "ONE");
        zero = new Factor(context.ZERO, context, "ZERO");

        // instantiate default permanent factors:
        permanentFactors = new HashSet<Factor>();
        permanentFactors.add(one);
        permanentFactors.add(zero);

        visualizer = new FactorVisualizer(this);
    }

    public void makePermanent(Collection<Factor> factors) {
        permanentFactors.addAll(factors);
    }

    public void flushFactorsExcept(Collection<Factor> specialFactors) {
        context.clearSpecialNodes();

        //permanent factors remain untouched:
        for (Factor f : permanentFactors) {
            Integer xaddId = f._xadd;
            context.addSpecialNode(xaddId);
        }

        //special factors remain untouched:
        for (Factor f : specialFactors) {
            Integer xaddId = f._xadd;
            context.addSpecialNode(xaddId);
        }

        context.flushCaches();
    }

    @Override
    public Factor multiply(Collection<Factor> factors) {
        int mult_xadd = context.ONE;
        String text = "(";
        for (Factor f : factors) {
            mult_xadd = context.applyInt(mult_xadd, f._xadd, XADD.PROD);
            text += (f.getHelpingText() + ".");
        }
        return new Factor(mult_xadd, context, text.substring(0, text.length() - 1) + ")");
    }

    public Factor subtract(Factor f1, Factor f2) {
        int subXaddId = context.applyInt(f1._xadd, f2._xadd, XADD.MINUS);
        return new Factor(subXaddId, context, "MINUS(" + f1.getHelpingText() + ", " + f2.getHelpingText() + ")");
    }

    public Factor power(Factor factor, int pow) {
        List<Factor> fs = new ArrayList<Factor>(pow);
        for (int i = 0; i < pow; i++) {
            fs.add(factor);
        }
        return multiply(fs);
    }

    public double meanSquaredError(Factor f1, Factor f2, boolean debug) {
//        XADD.HADI_DEBUG = true;
        Factor diff = subtract(f1, f2);  //visualizer.visualizeFactor(diff, "diff");
        Factor dif2 = power(diff, 2); //visualizer.visualizeFactor(dif2,"dif2");
        List<String> scopeVars = new ArrayList<String>(dif2.getScopeVars());
        if (debug) debugMSE(dif2, scopeVars);

        Factor mseFactor = dif2;
        for (String var : scopeVars) {
//            getVisualizer().visualizeFactor(mseFactor, "before marginalization");
            System.out.println("marginalizing var = " + var);
            mseFactor = marginalize(mseFactor, var);
//            visualizer.visualizeFactor(mseFactor, "after marginalizing" + var);
        }
        return valueOfOneConstantFactor(mseFactor);
    }

    private void debugMSE(Factor factor, List<String> vars) {
        //debugging to see if in all partitions, the integration over any variable is a positive function of other variables:
        if (vars.size() != 2) {
            System.err.println("debugMSE is not implemented for this case");
            return;
        }

        String var1 = vars.get(0);
        String var2 = vars.get(1);
        int bool_var1_index = context.getBoolVarIndex(var1);
        int bool_var2_index = context.getBoolVarIndex(var2);
        if (bool_var1_index > 0 || bool_var2_index >0) {
            System.err.println("Not covered this case");
            return;
        }

        //Xadd of the leaf of each path (constraint on the path decisions) is returned:
        PathToXaddExpansionBasedIntegralCalculator calc = new PathToXaddExpansionBasedIntegralCalculator();
        Map<List<XADD.XADDNode>, XADD.XADDNode> pathToXaddMap = calc.calculatePathToXaddMapping(context.getExistNode(factor._xadd), LeafFunction.identityFunction());
        System.out.println("#paths: = " + pathToXaddMap.size());

        //now: I marginalize var1 on each path xadd:
        Map<List<XADD.XADDNode>, XADD.XADDNode> pathToVar1MarginalizedXaddMap = new HashMap<List<XADD.XADDNode>, XADD.XADDNode>();
        for (List<XADD.XADDNode> path : pathToXaddMap.keySet()) {
            XADD.XADDNode pathXadd = pathToXaddMap.get(path);
            int marginalizedVar1NodeId = context.computeDefiniteIntegral(context._hmNode2Int.get(pathXadd), var1);
            pathToVar1MarginalizedXaddMap.put(path, context.getExistNode(marginalizedVar1NodeId));
//            visualizer.visualizeFactor(new Factor(marginalizedVar1NodeId, context, "..."), "a factor");
        }

        //now: I calculate the mass of each path-xadd if the second variable is also marginalized:
        Map<List<XADD.XADDNode>, Double> pathToBothVarsMarginalizedXaddMap = new HashMap<List<XADD.XADDNode>, Double>();
        for (List<XADD.XADDNode> path : pathToVar1MarginalizedXaddMap.keySet()) {
            XADD.XADDNode v1Marginal = pathToVar1MarginalizedXaddMap.get(path);
            int marginalizedBothVarsNodeId = context.computeDefiniteIntegral(context._hmNode2Int.get(v1Marginal), var2);
            Factor marginalizedBothVarsFactor = new Factor(marginalizedBothVarsNodeId,context, "?");
            pathToBothVarsMarginalizedXaddMap.put(path, this.valueOfOneConstantFactor(marginalizedBothVarsFactor));
//            visualizer.visualizeFactor(new Factor(marginalizedVar1NodeId, context, "..."), "a factor");
        }

        System.out.println("pathToBothVarsMarginalizedXaddMap.values() = " + pathToBothVarsMarginalizedXaddMap.values());

        double valueWithMaxAbs = 0.0d;
        List<XADD.XADDNode> chosenPath = null;
        for (Map.Entry<List<XADD.XADDNode>, Double> e : pathToBothVarsMarginalizedXaddMap.entrySet()) {
            List<XADD.XADDNode> path = e.getKey();
            Double mass = e.getValue();
            if (Math.abs(mass) > Math.abs(valueWithMaxAbs)) {
                valueWithMaxAbs = mass;
                chosenPath = path;
            }
        }

        System.out.println("valueWithMaxAbs = " + valueWithMaxAbs);
        XADD.XADDNode chosenXadd = pathToXaddMap.get(chosenPath);
        System.out.println("chosenXadd = " + chosenXadd);
        Factor chosenFactor = new Factor(context._hmNode2Int.get(chosenXadd), context, "?");
        visualizer.visualizeFactor(chosenFactor, "Bug");

        //make sure its integral is negative:
        Factor marginFactor = chosenFactor;
        for (String var : chosenFactor.getScopeVars()) {
            getVisualizer().visualizeFactor(marginFactor, "before marginalization");
            System.out.println("marginalizing var = " + var);
            marginFactor = marginalize(marginFactor, var);
        }
        System.out.println("this.valueOfOneConstantFactor(marginFactor) = " + this.valueOfOneConstantFactor(marginFactor));

//            return new Factor(xadd_marginal, context, "{" + f.getHelpingText() + "|!" + variable + "}");

        //end marginalize
//            visualizer.visualizeFactor(mseFactor, "after marginalizing" + var);


    }

    @Override
    public Factor marginalize(Factor factor, String variable) {
        // Take appropriate action based on whether var is boolean or continuous
//        int bool_var_index = context._alBooleanVars.contains(variable)//context.getBoolVarIndex(variable);
        int xadd_marginal = -1;
        if (context._alBooleanVars.contains(variable)){//(bool_var_index > 0) {
            int bool_var_index = context.getBoolVarIndex(variable); //todo I am not even sure about this, check it....
            // Sum out boolean variable
            int restrict_high = context.opOut(factor._xadd, bool_var_index, XADD.RESTRICT_HIGH);
            int restrict_low = context.opOut(factor._xadd, bool_var_index, XADD.RESTRICT_LOW);
            xadd_marginal = context.apply(restrict_high, restrict_low, XADD.SUM);
        } else {
            // Integrate out continuous variable
            xadd_marginal = context.computeDefiniteIntegral(factor._xadd, variable);
        }
        return new Factor(xadd_marginal, context, "{" + factor.getHelpingText() + "|!" + variable + "}");
    }

/*
    @Override
    public Factor approximate(Factor factor){double massThreshold, double volumeThreshold) {
        return approximate2(factor, massThreshold, volumeThreshold,
//////                new PathToXaddExpansionBasedMassCalculator(context)
                new EfficientPathIntegralCalculator(context)
        );
    }

    public Factor approximate2(Factor factor, double massThreshold, double volumeThreshold, PathIntegralOnLeafFunctionCalculator calculator) {
        MassThresholdXaddApproximator approximator = new MassThresholdXaddApproximator(context, factor._xadd, calculator);
        int approxId = approximator.approximateXADD(massThreshold, volumeThreshold);
        return new Factor(approxId, context, "~" + factor.getHelpingText());
    }
*/

    @Deprecated
    @Override
    public Factor approximate(Factor factor) {
//        MassThresholdXaddApproximator approximator = new MassThresholdXaddApproximator(context, new EfficientPathIntegralCalculator(context), massThreshold, volumeThreshold);
        XADD.XADDNode approxNode = approximator.approximateXadd(context.getExistNode(factor._xadd));
        return new Factor(context._hmNode2Int.get(approxNode), context, "~" + factor.getHelpingText());
    }

    @Override
    public Factor approximateMultiply(Collection<Factor> factors) {
        //todo maybe the factors should be sorted due to their size in the beginning...
        int mult_xadd = context.ONE;
        String text = "(";
        for (Factor f : factors) {
            mult_xadd = context.applyInt(mult_xadd, f._xadd, XADD.PROD);

//            Factor ____originalFactor = new Factor(mult_xadd, context, "");
//            visualizer.visualizeFactor(____originalFactor, "exact");
//            System.out.println("...... exact factor.toString() = " + ____originalFactor.toString());

            XADD.XADDNode multNode = approximator.approximateXadd(context.getExistNode(mult_xadd));
            mult_xadd = context._hmNode2Int.get(multNode);

//            Factor ____newFactor = new Factor(mult_xadd, context, "");
//            visualizer.visualizeFactor(____newFactor, "approx");
//            System.out.println("..........APPROX f.toString() = " + ____newFactor.toString());

            text += (f.getHelpingText() + ".");
        }
        return new Factor(mult_xadd, context, text.substring(0, text.length() - 1) + ")");

    }

    public double valueOfOneConstantFactor(Factor constFactor) {
        return context.evaluate(constFactor._xadd, new HashMap<String, Boolean>() /*EMPTY_BOOL*/, new HashMap<String, Double>()/*EMPTY_DOUBLE*/);
    }

    public Factor normalize(Factor f) {
        int xadd_norm = f._xadd;
        for (String var : f._vars)
            xadd_norm = context.computeDefiniteIntegral(xadd_norm, var);
        double norm = context.evaluate(xadd_norm, new HashMap<String, Boolean>() /*EMPTY_BOOL*/, new HashMap<String, Double>()/*EMPTY_DOUBLE*/);
        xadd_norm = context.scalarOp(f._xadd, 1d / norm, XADD.PROD);
        return new Factor(xadd_norm, context, "Norm" + f.getHelpingText());
    }

    public FactorVisualizer getVisualizer() {
        return visualizer;
    }

    public XADD getContext() {
        return context;
    }

    @Override
    public Factor one() {
        return  one;//new Factor(context.ONE, context, "ONE");
    }

    public Factor zero() {
        return zero;
    }

    @Override
    public Factor getFactorForMultiplicationOfVars(String[] vars) {
        // It should return an XADDTNode:
        StringBuilder sb = new StringBuilder("([");
        for (String var : vars) {
            sb.append(var).append("*");
        }

        String str = sb.delete(sb.length() - 1, sb.length()).append("])").toString();
//        System.out.println("str = " + str);
        int id = context.buildCanonicalXADDFromString(str);
        return new Factor(id, context, str);
    }

    @Override
    public double evaluate(Factor factor, Map<String, Double> completeContinuousVariableAssignment) {
        return context.evaluate(factor._xadd, new HashMap<String, Boolean>(), new HashMap<String, Double>(completeContinuousVariableAssignment));
    }

    public Double getMinValue(String varName) {
        return context._hmMinVal.get(varName);
    }

    public Double getMaxValue(String varName) {
        return context._hmMaxVal.get(varName);

    }

    public double klDivergence(Factor p, Factor q, int numberOfSamples) {
        Set<String> pVars = p.getScopeVars();
        Set<String> qVars = q.getScopeVars();
        if (pVars.size() != 1)
            throw new RuntimeException("KL divergence cannot be computed since one-variable factor P expected");
        if (qVars.size() != 1)
            throw new RuntimeException("KL divergence cannot be computed since one-variable factor Q expected");

        String x = pVars.iterator().next();
        if (!qVars.iterator().next().equals(x)) throw new RuntimeException("same variable in P and Q expected");

        double lowBound = getMinValue(x);
        double highBound = getMaxValue(x);

        double epsilon = (highBound - lowBound) / (double) numberOfSamples;
        double sum = 0d;
        Double pSample;
        Double qSample;
        HashMap<String, Double> continuousAssignment = new HashMap<String, Double>(1);
        HashMap<String, Boolean> emptyBooleanAssignment = new HashMap<String, Boolean>(0);
        for (Double i = lowBound; i < highBound; i += epsilon) {
            continuousAssignment.clear();
            continuousAssignment.put(x, i);
            pSample = context.evaluate(p._xadd, emptyBooleanAssignment, continuousAssignment);
            qSample = context.evaluate(q._xadd, emptyBooleanAssignment, continuousAssignment);
            sum += Math.log(pSample / (double) qSample) * pSample;
        }

        return sum / (double) numberOfSamples;
    }

}
