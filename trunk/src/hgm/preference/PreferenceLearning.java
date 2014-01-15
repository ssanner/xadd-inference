package hgm.preference;

import hgm.preference.db.PreferenceDatabase;
import hgm.sampling.VarAssignment;
import xadd.XADD;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Hadi Afshar.
 * Date: 19/12/13
 * Time: 8:36 PM
 */
public class PreferenceLearning {
    /*
    Prior weight of Ws is considered uniform in [-C, C]
     */
    public static final double C = 10.0; //todo range of uniform distributions should be decided in a more flexible way.
    public static final double EPSILON = 0.01; // deciding when preference should be considered as "equality"
    DecimalFormat decimalFormat = new DecimalFormat("##.#######"); //used to convert doubles to strings //todo discuss the effect of this approximation with Scott...
//    public static final double INDICATOR_NOISE = 0.1;
    private  double indicatorNoise;

    private XADD context;
    private PreferenceDatabase db;

    private String weightVectorName;

    public PreferenceLearning(XADD context, PreferenceDatabase db, double indicatorNoise, String weightVectorName) {
        this.context = context;
        this.db = db;
        this.weightVectorName = weightVectorName;

        if (indicatorNoise>0.5 || indicatorNoise<0.0) System.err.println("indicator noise should be in [0, 0.5] to make sense.");
        this.indicatorNoise = indicatorNoise;
    }


    public int computeItemWithMaxExpectedUtilityVersion1(XADD.XADDNode utilityWeights/*, String weightVectorName*/) {
        // in this version, the expression "int_w Pr(w|R^n)[\sum_d=1^D (w_d x_d)] dw"  is calculated for each X=[x_0, ...x_{D-1}] separately
        int chosenItemId = -1;
        double maxUtil = Double.NEGATIVE_INFINITY;
        for (int itemId = 0; itemId<db.numberOfItems(); itemId++) {
            Double[] item = db.getItemAttributeValues(itemId);
            double itemUtil = expectedItemUtility(item, utilityWeights, /*weightVectorName,*/ false);
            System.out.println("itemUtil of item #" + itemId + ": " + itemUtil);
            if (itemUtil>maxUtil) {
                maxUtil = itemUtil;
                chosenItemId = itemId;
            }
        }
        return chosenItemId;
    }

    /**
     *
     * @param utilityWeights Pr(w|R^n)
//     * @param weightVectorName w
     * @param itemAttributesVectorName x
     * @return argmax_x {int_w Pr(w|R^n)[sum_d=1:D w_d.x_d]}
     */
    public int computeItemWithMaxExpectedUtilityVersion2(XADD.XADDNode utilityWeights /*posterior*/,
                                                        /* String weightVectorName,*/ String itemAttributesVectorName) {
        // in this version, the expression "int_w Pr(w|R^n)[\sum_d=1^D (w_d x_d)] dw"  is calculated parametrically
        // so both W=[w_0, ..., w_{D-1}] and X=[x_0, ..., x_{D-1}] are parameters...
        XADD.XADDNode parametricExpectedUtil = parametricExpectedItemUtility(utilityWeights, /*weightVectorName,*/
                itemAttributesVectorName, db.getNumberOfAttributes(), false);
        int paramExpectedUtilNodeId = context._hmNode2Int.get(parametricExpectedUtil);

        int chosenItemId = -1;
        double maxUtil = Double.NEGATIVE_INFINITY;
        HashMap<String, Double> assignment = new HashMap<String, Double>(db.getNumberOfAttributes());
        for (int itemId = 0; itemId<db.numberOfItems(); itemId++) {

            //make a an assignment out of item attributes:
            Double[] item = db.getItemAttributeValues(itemId);
            for (int i =0; i<item.length; i++) {
                assignment.put(itemAttributesVectorName+"_"+i, item[i]);    //x_i <-> item[i]
            }


            //todo: IMPORTANT: Only works for the case where nothing is boolean.....
            double itemExpectedUtil = context.evaluate(paramExpectedUtilNodeId, null, assignment);
//            System.out.println("Expected itemUtil of item #" + itemId + ": " + itemExpectedUtil);
            if (itemExpectedUtil>maxUtil) {
                maxUtil = itemExpectedUtil;
                chosenItemId = itemId;
            }
        }
        return chosenItemId;
    }

    public XADD.XADDNode parametricExpectedItemUtility(XADD.XADDNode utilityWeights /*posterior*/,
                                                       /*String weightVectorName,*/ String itemAttributesVectorName,
                                                       int vectorDimension, boolean plotGraph) {
        String itemUtilStr = "([" + parametricItemUtility(weightVectorName, itemAttributesVectorName, vectorDimension) + "])";
//        System.out.println("itemUtilStr = " + itemUtilStr);
        XADD.XADDNode itemUtil = context.getExistNode(context.buildCanonicalXADDFromString(itemUtilStr));
        XADD.XADDNode expectedItemUtilForGivenW = multiply(utilityWeights, itemUtil); //P(W|R^n)[sum(w_d . x_d)] (W not marginalized yet)

        if (plotGraph) context.getGraph(context._hmNode2Int.get(expectedItemUtilForGivenW)).launchViewer();

        //marginalize all w_i:
        int expectedItemUtilNodeId = context._hmNode2Int.get(expectedItemUtilForGivenW);
        for (int i=0; i<vectorDimension; i++) {
            String w_i = weightVectorName + "_" + i;
            expectedItemUtilNodeId = context.computeDefiniteIntegral(expectedItemUtilNodeId, w_i);
        }
        return context._hmInt2Node.get(expectedItemUtilNodeId);
    }

    public double expectedItemUtility(Double[] item, XADD.XADDNode utilityWeights/*, String weightVectorName*/, boolean plotGraph) {
        String itemUtilStr = "([" + itemUtility(weightVectorName, item) + "])";
//        System.out.println("itemUtilStr = " + itemUtilStr);
        XADD.XADDNode itemUtil = context.getExistNode(context.buildCanonicalXADDFromString(itemUtilStr));
        XADD.XADDNode expectedItemUtilForGivenW = multiply(utilityWeights, itemUtil); //P(W|R^n)[sum(w_d . x_d)] (W not marginalized yet)

        if (plotGraph) context.getGraph(context._hmNode2Int.get(expectedItemUtilForGivenW)).launchViewer();

        //marginalize all w_i:
        int expectedItemUtilNodeId = context._hmNode2Int.get(expectedItemUtilForGivenW);
        for (int i=0; i<item.length; i++) {
            String w_i = weightVectorName + "_" + i;
            expectedItemUtilNodeId = context.computeDefiniteIntegral(expectedItemUtilNodeId, w_i);
        }
        return context.evaluate(expectedItemUtilNodeId, new HashMap<String, Boolean>() /*EMPTY_BOOL*/, new HashMap<String, Double>()/*EMPTY_DOUBLE*/);
    }


    /**
     *
     * @param reduceXadd whether to use LP-Reduce or not
     * Warning: this method flushes everything in XADD except itself.
     * @return Pr(W | R^n)
     */
    public XADD.XADDNode computePosteriorWeightVector(boolean reduceXadd) {
        XADD.XADDNode posterior = computeProbabilityOfWeightVector(db.getPreferenceResponses(), reduceXadd);

        context.addSpecialNode(context._hmNode2Int.get(posterior));
        context.flushCaches();

        return posterior;
    }

    //Pr(W | R^n)
    private XADD.XADDNode computeProbabilityOfWeightVector(List<Preference> preferenceAnswers/*, String weightVectorName*/, boolean reduceXadd) {
        Integer numAttribs = db.getNumberOfAttributes();
        XADD.XADDNode[] pWeights = new XADD.XADDNode[numAttribs];

        if (preferenceAnswers.isEmpty()) {
            // uniform distribution between -C and C:
            for (int i=0; i<pWeights.length; i++) {
                String w_i = weightVectorName + "_" + i;
                pWeights[i] = uniformDistribution(w_i, -C, C);

                //todo it seems something is wrong with the following lines...
                //note: for using LP solver, I think I need to assign min/max values to to each var (?)
//                if (context._hmMinVal.get(w_i) == null) context._hmMinVal.put(w_i, -C - 1); //or? can I simply put -C and then get read of the uniform distribution?//todo check
//                if (context._hmMaxVal.get(w_i) == null) context._hmMaxVal.put(w_i, C + 1); //or? ...
            }
            return multiply(pWeights);  //Pr(W)
        }

        //Pr(W | R^n)
        XADD.XADDNode prior = computeProbabilityOfWeightVector(preferenceAnswers.subList(0, preferenceAnswers.size() - 1), reduceXadd); //Pr(W|R^n)
        Preference lastResponse = preferenceAnswers.get(preferenceAnswers.size() - 1); //q_{ab}
        // Pr(q_{ab} | W):
        XADD.XADDNode likelihood = computePreferenceLikelihoodGivenUtilityWeights(lastResponse/*, weightVectorName*/, numAttribs);

        XADD.XADDNode multiply = multiply(likelihood, prior);

//        Integer size1 = multiply.collectNodes().size();
        if (reduceXadd) {
            XADD.XADDNode reducedMult = simplify(multiply);
            multiply = reducedMult;
        }
        return multiply;
    }

    private XADD.XADDNode simplify(XADD.XADDNode node) {
        return context._hmInt2Node.get(context.reduceLP(context._hmNode2Int.get(node)));
    }

    private XADD.XADDNode uniformDistribution(String var, double min, double max) {
        double u = 1.0 / (max -min);
        String s = "([" + var + "<" + min + "] ([0]) ([" + var + "<" + max + "] ([" + u + "]) ([0])))";
        int nodeId = context.buildCanonicalXADDFromString(s);
        return context.getExistNode(nodeId);
    }

    private XADD.XADDNode indicator(String condition) { //noisy indicator
        String s = "([" + condition + "] ([" + (1-indicatorNoise) + "]) ([" + (0 + indicatorNoise) + "]))";
        int nodeId = context.buildCanonicalXADDFromString(s);
        return context.getExistNode(nodeId);
    }

    // Pr(q_{ab} | W)
    private XADD.XADDNode computePreferenceLikelihoodGivenUtilityWeights(Preference preference/*, String weightVectorName*/, int numAttribs) {
        int itemId1 = preference.getItemId1();
        int itemId2 = preference.getItemId2();
        Double[] item1Attribs = db.getItemAttributeValues(itemId1);
        Double[] item2Attribs = db.getItemAttributeValues(itemId2);


        String u1Str = itemUtility(weightVectorName, item1Attribs);
        String u2Str = itemUtility(weightVectorName, item2Attribs);
        switch (preference.getPreferenceChoice()) {
            case FIRST:
                return indicator("(" + u1Str + ") - (" + u2Str + ") > " + EPSILON);
            case SECOND:
                return indicator("(" + u2Str + ") - (" + u1Str + ") > " + EPSILON);
            case EQUAL:
                return indicator("(" + u1Str + ") - (" + u2Str + ") <= " + EPSILON);
            default:
                throw new RuntimeException("no valid preference");
        }
    }

    //sum(x_i . w_i) where i = 0 to numAttribs - 1
    private String itemUtility(String weightVectorName, Double[] itemAttributes) {
        StringBuilder sumW_iXx_i = new StringBuilder();
        for (int i = 0; i < itemAttributes.length; i++) {
            String w_iXx_i = doubleToString(itemAttributes[i]) + "*" + weightVectorName + "_" + i;
            sumW_iXx_i.append(w_iXx_i).append("+");
        }
        sumW_iXx_i.deleteCharAt(sumW_iXx_i.length() - 1); //delete last '+'
        return sumW_iXx_i.toString();
    }

    // I do not want to use toString() method since it may generate strings like: 5.788825006847187E-4 which cannot be parsed afterwards.
    private String doubleToString(Double d) {
        return decimalFormat.format(d);
    }

    //sum(x_i . w_i) where i = 0 to (dimension - 1)
    private String parametricItemUtility(String weightVectorName, String itemAttributesName, int dimension) {
        StringBuilder sumW_iXx_i = new StringBuilder();
        for (int i = 0; i < dimension; i++) {
            String w_iXx_i = itemAttributesName + "_" + i + "*" + weightVectorName + "_" + i;
            sumW_iXx_i.append(w_iXx_i).append("+");
        }
        sumW_iXx_i.deleteCharAt(sumW_iXx_i.length() - 1); //delete last '+'
        return sumW_iXx_i.toString();
    }

    private XADD.XADDNode multiply(XADD.XADDNode... nodes) {
        int mult_xadd = context.ONE;
        for (XADD.XADDNode n : nodes) {
            mult_xadd = context.applyInt(mult_xadd, context._hmNode2Int.get(n), XADD.PROD);
        }

        return context.getExistNode(mult_xadd);
    }

    /**
     *
     * @return an assignment for which the value of posterior is positive (and large), null if not known.
     * This can be used as the initial sample in samplers...
     */
    public VarAssignment generateAWeightVectorHighlyProbablePosteriorly() {

        double[] auxiliaryWeightVector = db.getAuxiliaryWeightVector();
        if (auxiliaryWeightVector == null) return null;

        Integer dim = auxiliaryWeightVector.length;
        HashMap<String, Double> continuousVarAssign = new HashMap<String, Double>(dim);
        for(int i=0; i< dim; i++) {
            continuousVarAssign.put(weightVectorName + "_" + i, auxiliaryWeightVector[i]);
        }

        return new VarAssignment(new HashMap<String, Boolean>(), continuousVarAssign);
    }
}
