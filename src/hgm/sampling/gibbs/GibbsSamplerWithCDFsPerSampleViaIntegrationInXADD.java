package hgm.sampling.gibbs;

import hgm.sampling.XaddSampler;
import hgm.sampling.SamplingFailureException;
import hgm.sampling.VarAssignment;
import xadd.ExprLib;
import xadd.XADD;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Hadi Afshar.
 * Date: 3/01/14
 * Time: 9:37 PM
 */

/**
 * This is a an implementation of Gibbs sampler that calculates CDF per sample
 */
@Deprecated //replaced by GibbsSamplerWithCDFsPerSample. Now may only be used for (performance) test...
public class GibbsSamplerWithCDFsPerSampleViaIntegrationInXADD extends XaddSampler {
    public static final double SAMPLE_ACCURACY = 1E-6;
    public static final int MAX_ITERATIONS_TO_APPROX_F_INVERSE = 20;
    public static final int MAX_INITIAL_SAMPLING_TRIAL = 10000; //if the function is not positive, (initial) sample cannot be taken
    private List<String> allVars;
    private VarAssignment initialSample;

    @Deprecated
    public GibbsSamplerWithCDFsPerSampleViaIntegrationInXADD(XADD context, XADD.XADDNode root) {
        this(context, root, null);
    }

    //Warning: This constructor performs XADD flushing
    public GibbsSamplerWithCDFsPerSampleViaIntegrationInXADD(XADD context, XADD.XADDNode root, VarAssignment initialSample) {
        super(context, root);

        allVars = new ArrayList<String>(bVars);
        allVars.addAll(cVars);

        //NOTE: Flushes everything except the given root:
        context.addSpecialNode(context._hmNode2Int.get(root));

        this.initialSample = initialSample;

        if (initialSample != null) {
            Double initSampleValue = context.evaluate(context._hmNode2Int.get(root), initialSample.getBooleanVarAssign(), initialSample.getContinuousVarAssign());
            if (initSampleValue <= 0.0)
                throw new SamplingFailureException("valuation of the initial sample is not positive: " + initSampleValue);
        }
//        else {
//            System.err.println("Warning! NULL initial sample. The sampler tries to sample itself by rejection sampling....");
//        }

    }

    /**
     * @return sample in a REUSABLE assignment
     * @throws hgm.sampling.SamplingFailureException
     * Warning: flushes...
     */
    @Override
    public VarAssignment sample() throws SamplingFailureException {
        if (super.reusableVarAssignment == null) { // (no sample is taken yet)
            super.reusableVarAssignment = takeInitialSample();// initialization phase:
        } else {
            for (String bVar : bVars) {
                throw new RuntimeException("does not work with boolean variables YET....");
//                sampleSingleVar(bVar, reusableVarAssignment);
            }
            for (String cVar : cVars) {
                sampleSingleContinuousVar(cVar, reusableVarAssignment);
                context.flushCaches();
            }
        }
        return reusableVarAssignment;
    }

    /**
     * uniformly sample each variable in the interval between its min and max values and reject the produced sample if its probability is not positive...
     */
    private VarAssignment takeInitialSample() throws SamplingFailureException {
        if (initialSample != null) return initialSample;

//        System.err.println("No initial sample passed to Gibbs sampler. Rejection sampling is used instead...");

        int failureCount = 0;


        HashMap<String, Boolean> boolAssign = new HashMap<String, Boolean>(bVars.size());
        HashMap<String, Double> contAssign = new HashMap<String, Double>(cVars.size());

        Double targetValue;
        do {
            if (failureCount++ > MAX_INITIAL_SAMPLING_TRIAL)
                throw new SamplingFailureException("Unable to take initial sample");
            for (String bVar : bVars) {
                boolAssign.put(bVar, super.randomBoolean());
            }
            for (String cVar : cVars) {
                //todo I think minVal and maxVal are only used for visualization (not sure) and maybe should not used here... (should directly be fed to the class?)
                Double minVarValue = context._hmMinVal.get(cVar);
                Double maxVarValue = context._hmMaxVal.get(cVar);
                if (maxVarValue == null) throw new RuntimeException("The max of scope of var " + cVar + " is unknown");
                if (minVarValue == null) throw new RuntimeException("The min of scope of var " + cVar + " is unknown");
                contAssign.put(cVar, super.randomDoubleUniformBetween(minVarValue, maxVarValue));
            }
            targetValue = context.evaluate(rootId, boolAssign, contAssign);
        } while (targetValue <= 0.0); // a valid sample is found

        return new VarAssignment(boolAssign, contAssign);
    }

    /**
     * @param varToBeSampled    var to be sampled from
     * @param reusableVarAssign current assignment. the specified variable will be updated in this assignment
     */
    private void sampleSingleContinuousVar(String varToBeSampled, VarAssignment reusableVarAssign) {
//        XADD.XADDNode varCDF = varCdfMap.get(varToBeSampled);

        XADD.XADDNode varCDF = makeCumulativeDistributionFunction(root, varToBeSampled, reusableVarAssign);
        int cdfId = context._hmNode2Int.get(varCDF);


        //todo working with these maps is not recommended... Take min and max in constructor....
        Double maxVarValue = context._hmMaxVal.get(varToBeSampled);
        Double minVarValue = context._hmMinVal.get(varToBeSampled);

        //to calculate F(infinity):
        reusableVarAssign.assignExistingContinuousVariable(varToBeSampled, maxVarValue + 0.1 /*just to be exclusive*/); //since it is reusable

        // inverse of cdfInfinity is the normalization factor:
        Double cdfInfinity = context.evaluate(cdfId,
                reusableVarAssignment.getBooleanVarAssign(), reusableVarAssign.getContinuousVarAssign());

        double s = randomDoubleUniformBetween(0.0, cdfInfinity);
        //now: F^{-1}(X=s) should be computed:
        //Binary search algorithm:
        double low = minVarValue;
        double high = maxVarValue;
        double approxS;
        double average;

        //NOTE: the algorithm only works since there is no delta (i.e. no discontinuity in the CDF)
        //however, counter is added in case XADD rounding causes problems
        int counter = 0;
        do {
            average = (high + low) / 2.0;
            reusableVarAssign.assignExistingContinuousVariable(varToBeSampled, average);  //here the sample is stored
            approxS = context.evaluate(cdfId,
                    reusableVarAssign.getBooleanVarAssign(), reusableVarAssign.getContinuousVarAssign());
            if (approxS < s) {
                low = average;
            } else {
                high = average;
            }
        } while ((Math.abs(approxS - s) > SAMPLE_ACCURACY) && counter++ < MAX_ITERATIONS_TO_APPROX_F_INVERSE);
    }


    //NOTE: only works with continuous vars...
    // returns int_{w=-infty}^{var} (func[var->w]dw) for instantiated function
    public XADD.XADDNode makeCumulativeDistributionFunction(XADD.XADDNode func, String var, VarAssignment currentVarAssign) {
        //1. Make a uni-dimensional function where except 'var', all variables are instantiated due to the current var. assign.:
        HashMap<String, Double> continuousVarAssign = currentVarAssign.getContinuousVarAssign();
        HashMap<String, ExprLib.ArithExpr> substitution = new HashMap<String, ExprLib.ArithExpr>(Math.max(0, continuousVarAssign.size() - 1)); //since one var is remained untouched...
        for (Map.Entry<String, Double> cVarValue : continuousVarAssign.entrySet()) {
            String cVar = cVarValue.getKey();
            if (!cVar.equals(var)) { //var should remain untouched!
                Double cAssign = cVarValue.getValue();
                substitution.put(cVar, new ExprLib.DoubleExpr(cAssign));
            }
        }

        int instantiatedXaddNodId = context.substitute(context._hmNode2Int.get(func), substitution);
        instantiatedXaddNodId = context.reduceLP(instantiatedXaddNodId);

        // a pruned uni-var functions should be integrated easily:
        XADD.XADDNode uniVarFunc = context._hmInt2Node.get(instantiatedXaddNodId);

        //2. first integrate with an unspecified upper bound 't' and then replace 't' with 'var':
        if (allVars.contains("t")) throw new RuntimeException("a temporary variable already exist...");

        XADD.XADDNode unspecifiedBoundedNode = integrateWithUpperBound(uniVarFunc, var, "t");

        XADD.XADDNode cdf = replaceVar(unspecifiedBoundedNode, "t", var); // t is replaced here with

        return cdf;
    }

    // integral_{-infinity}^{upperBound} func d_var
    public XADD.XADDNode integrateWithUpperBound(XADD.XADDNode func, String var, String upperBound) {
        String indicatorStr = "([" + var + "<" + upperBound + "] ([1]) ([0]))"; //TODO: THIS SHOULD BE DONE ONLY ONCE....
        int indicatorId = context.buildCanonicalXADDFromString(indicatorStr);
        int boundedFuncId = context.apply(context._hmNode2Int.get(func), indicatorId, XADD.PROD);

        int integratedBoundedFuncId;
        //todo this code is copied from Gibbs class. Test it...
        if (context._hsBooleanVars.contains(var)) {
            Integer bool_var_index = context.getBoolVarIndex(var);
            // Sum out boolean variable
            int restrict_high = context.opOut(boundedFuncId, bool_var_index, XADD.RESTRICT_HIGH);
            int restrict_low = context.opOut(boundedFuncId, bool_var_index, XADD.RESTRICT_LOW);
            integratedBoundedFuncId = context.apply(restrict_high, restrict_low, XADD.SUM);
        } else {
            // Integrate out continuous variable
            integratedBoundedFuncId = context.computeDefiniteIntegral(boundedFuncId, var);
        }
        XADD.XADDNode result = context.getExistNode(integratedBoundedFuncId);
        return result;
    }

    private XADD.XADDNode replaceVar(XADD.XADDNode node, String varToBeReplaced, String replacingVar) {
        HashMap<String, ExprLib.ArithExpr> subst = new HashMap<String, ExprLib.ArithExpr>();
        ExprLib.ArithExpr a = new ExprLib.VarExpr(replacingVar);
        subst.put(varToBeReplaced, a);
        return context.getExistNode(context.substitute(context._hmNode2Int.get(node), subst));
    }

}
