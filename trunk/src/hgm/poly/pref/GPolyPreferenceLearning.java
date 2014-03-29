package hgm.poly.pref;

import hgm.poly.ConstrainedPolynomial;
import hgm.poly.Polynomial;
import hgm.poly.PolynomialFactory;
import hgm.preference.Preference;
import hgm.preference.db.PreferenceDatabase;
import hgm.sampling.VarAssignment;

import java.util.HashMap;
import java.util.List;

/**
 * Created by Hadi Afshar.
 * Date: 19/12/13
 * Time: 8:36 PM
 */
public class GPolyPreferenceLearning {

    //todo: when you what to change check which classes (e.g. SlackGibbsPolytopeSampler) use it.
    /*
    Prior weight of Ws is considered uniform in [-C, C]
     */
    public static final double C = 10.0; //todo range of uniform distributions should be decided in a more flexible way.

    //    public static double EPSILON = 0;//0.01; // deciding when preference should be considered as "equality"
//    public static final int NUM_POSTERIOR_CALC_ITERATIONS_LEADING_TO_FLUSHING = 130; // the side effect is that the xadd should become permanent. SEEMS USELESS...

    private double indicatorNoise;

    private PreferenceDatabase db;

    private String weightVectorName;

//    private double epsilon;
    private PolynomialFactory factory;

    public GPolyPreferenceLearning(PreferenceDatabase db, double indicatorNoise, String weightVectorName) {

        this.db = db;
        this.weightVectorName = weightVectorName;

        if (indicatorNoise>0.5 || indicatorNoise<0.0) System.err.println("indicator noise should be in [0, 0.5] to make sense.");
//        if (indicatorNoise >= 1 || indicatorNoise < 0.0) System.err.println("indicator noise should be in [0, 1) to make sense.");
        this.indicatorNoise = indicatorNoise;

        int numAttribs = db.getNumberOfAttributes();
        String[] vars = new String[numAttribs];
        for (int i = 0; i < vars.length; i++) {
            String w_i = weightVectorName + "_" + i;
            vars[i] = w_i;
        }

        factory = new PolynomialFactory(vars);


    }

    public PolynomialFactory getFactory() {
        return factory;
    }

    /* public int computeItemWithMaxExpectedUtilityVersion1(XADD.XADDNode utilityWeights*//*, String weightVectorName*//*) {
        // in this version, the expression "int_w Pr(w|R^n)[\sum_d=1^D (w_d x_d)] dw"  is calculated for each X=[x_0, ...x_{D-1}] separately
        int chosenItemId = -1;
        double maxUtil = Double.NEGATIVE_INFINITY;
        for (int itemId = 0; itemId < db.getNumberOfItems(); itemId++) {
            Double[] item = db.getItemAttributeValues(itemId);
            double itemUtil = expectedItemUtility(item, utilityWeights, *//*weightVectorName,*//* false);
            System.out.println("itemUtil of item #" + itemId + ": " + itemUtil);
            if (itemUtil > maxUtil) {
                maxUtil = itemUtil;
                chosenItemId = itemId;
            }
        }
        return chosenItemId;
    }*/

   /* *//**
     *//*
    public int computeItemWithMaxExpectedUtilityVersion2(XADD.XADDNode utilityWeights *//*posterior*//*,
                                                        *//* String weightVectorName,*//* String itemAttributesVectorName) {
        // in this version, the expression "int_w Pr(w|R^n)[\sum_d=1^D (w_d x_d)] dw"  is calculated parametrically
        // so both W=[w_0, ..., w_{D-1}] and X=[x_0, ..., x_{D-1}] are parameters...
        XADD.XADDNode parametricExpectedUtil = parametricExpectedItemUtility(utilityWeights, *//*weightVectorName,*//*
                itemAttributesVectorName, db.getNumberOfAttributes(), false);
        int paramExpectedUtilNodeId = context._hmNode2Int.get(parametricExpectedUtil);

        int chosenItemId = -1;
        double maxUtil = Double.NEGATIVE_INFINITY;
        HashMap<String, Double> assignment = new HashMap<String, Double>(db.getNumberOfAttributes());
        for (int itemId = 0; itemId < db.getNumberOfItems(); itemId++) {

            //make a an assignment out of item attributes:
            Double[] item = db.getItemAttributeValues(itemId);
            for (int i = 0; i < item.length; i++) {
                assignment.put(itemAttributesVectorName + "_" + i, item[i]);    //x_i <-> item[i]
            }


            //todo: IMPORTANT: Only works for the case where nothing is boolean.....
            double itemExpectedUtil = context.evaluate(paramExpectedUtilNodeId, null, assignment);
//            System.out.println("Expected itemUtil of item #" + itemId + ": " + itemExpectedUtil);
            if (itemExpectedUtil > maxUtil) {
                maxUtil = itemExpectedUtil;
                chosenItemId = itemId;
            }
        }
        return chosenItemId;
    }*/

/*
    public XADD.XADDNode parametricExpectedItemUtility(XADD.XADDNode utilityWeights */
/*posterior*//*
,
                                                       */
/*String weightVectorName,*//*
 String itemAttributesVectorName,
                                                       int vectorDimension, boolean plotGraph) {
        String itemUtilStr = "([" + parametricItemUtility(weightVectorName, itemAttributesVectorName, vectorDimension) + "])";
        XADD.XADDNode itemUtil = context.getExistNode(context.buildCanonicalXADDFromString(itemUtilStr));
        XADD.XADDNode expectedItemUtilForGivenW = multiply(utilityWeights, itemUtil); //P(W|R^n)[sum(w_d . x_d)] (W not marginalized yet)

        if (plotGraph) context.getGraph(context._hmNode2Int.get(expectedItemUtilForGivenW)).launchViewer();

        //marginalize all w_i:
        int expectedItemUtilNodeId = context._hmNode2Int.get(expectedItemUtilForGivenW);
        for (int i = 0; i < vectorDimension; i++) {
            String w_i = weightVectorName + "_" + i;
            expectedItemUtilNodeId = context.computeDefiniteIntegral(expectedItemUtilNodeId, w_i);
        }
        return context._hmInt2Node.get(expectedItemUtilNodeId);
    }
*/

  /*  public double expectedItemUtility(Double[] item, XADD.XADDNode utilityWeights*//*, String weightVectorName*//*, boolean plotGraph) {
        String itemUtilStr = "([" + itemUtilityStr(weightVectorName, item) + "])";
//        System.out.println("itemUtilStr = " + itemUtilStr);
        XADD.XADDNode itemUtil = context.getExistNode(context.buildCanonicalXADDFromString(itemUtilStr));
        XADD.XADDNode expectedItemUtilForGivenW = multiply(utilityWeights, itemUtil); //P(W|R^n)[sum(w_d . x_d)] (W not marginalized yet)

        if (plotGraph) context.getGraph(context._hmNode2Int.get(expectedItemUtilForGivenW)).launchViewer();

        //marginalize all w_i:
        int expectedItemUtilNodeId = context._hmNode2Int.get(expectedItemUtilForGivenW);
        for (int i = 0; i < item.length; i++) {
            String w_i = weightVectorName + "_" + i;
            expectedItemUtilNodeId = context.computeDefiniteIntegral(expectedItemUtilNodeId, w_i);
        }
        return context.evaluate(expectedItemUtilNodeId, new HashMap<String, Boolean>() *//*EMPTY_BOOL*//*, new HashMap<String, Double>()*//*EMPTY_DOUBLE*//*);
    }*/


    public PolytopesHandler computePosteriorWeightVector(int maxGatingConditionViolation){
    return computePrefPosteriorWeightVector(db.getPreferenceResponses(), maxGatingConditionViolation);
    }

    //Pr(W | R^n)
    private PolytopesHandler computePrefPosteriorWeightVector(List<Preference> preferenceResponses,
                                                           int maxGatingConditionViolation) {


        int numAttribs = factory.numberOfVars(); //db.getNumberOfAttributes();
//        XADD.XADDNode[] pWeights = new XADD.XADDNode[numAttribs];

        //1. prior: pr(W)
        String[] constraints = new String[numAttribs*2];
        for (int i = 0; i < factory.numberOfVars(); i++) {
            String w_i = weightVectorName + "_" + i;
            constraints[2*i] = w_i + "^(1) + " + C + ">0";
            constraints[2*i + 1] = "-1*" + w_i + "^(1) + " + C + ">0";
        }

        ConstrainedPolynomial prior = factory.makeConstrainedPolynomial("1", constraints);
        PolytopesHandler gatedPolytopes = new PolytopesHandler(factory, prior, indicatorNoise, maxGatingConditionViolation);

        for (Preference prefResponse : preferenceResponses) {
            // Pr(q_{ab} | W):
            Polynomial likelihoodPositiveConstraint = computePreferenceLikelihoodPositiveConstraintGivenUtilityWeights(prefResponse);
            gatedPolytopes.addPositiveConstraint(likelihoodPositiveConstraint);
        }

        return gatedPolytopes;
    }




    // Pr(q_{ab} | W)
    // returns u(a|w) - u(b|w) if a > b and u(b|w) - u(a|w) if a < b
    private Polynomial computePreferenceLikelihoodPositiveConstraintGivenUtilityWeights(Preference preference/*, String weightVectorName, int numAttribs*/) {
        int itemId1 = preference.getItemId1();
        int itemId2 = preference.getItemId2();
        Double[] item1Attribs = db.getItemAttributeValues(itemId1);
        Double[] item2Attribs = db.getItemAttributeValues(itemId2);


        String u1Str = itemUtilityStr(weightVectorName, item1Attribs);
        String u2Str = itemUtilityStr(weightVectorName, item2Attribs);

        Polynomial u1;
        Polynomial u2;

        switch (preference.getPreferenceChoice()) {
            case FIRST:
                u1 = factory.makePolynomial(u1Str);
                u2 = factory.makePolynomial(u2Str);
                break;
//                return u1.sindicator("(" + u1Str + ") - (" + u2Str + ") > " + epsilon, indicatorNoise);
            case SECOND:
                u1 = factory.makePolynomial(u2Str);
                u2 = factory.makePolynomial(u1Str);
                break;
//                return indicator("(" + u2Str + ") - (" + u1Str + ") > " + epsilon, indicatorNoise);
//            case EQUAL:
//                return indicator("(" + u1Str + ") - (" + u2Str + ") <= " + epsilon, indicatorNoise);
            default:
                throw new RuntimeException("no valid preference");
        }

        u2.multiplyScalarInThis(-1);
        u1.addToThis(u2);
        return u1;


    }

    //sum(x_i . w_i) where i = 0 to numAttribs - 1
    private String itemUtilityStr(String weightVectorName, Double[] itemAttributes) {
        StringBuilder sumW_iXx_i = new StringBuilder();
        for (int i = 0; i < itemAttributes.length; i++) {
            String w_iXx_i = itemAttributes[i] + "*" + weightVectorName + "_" + i + "^(1)";
            sumW_iXx_i.append(w_iXx_i).append("+");
        }
        sumW_iXx_i.deleteCharAt(sumW_iXx_i.length() - 1); //delete last '+'
        return sumW_iXx_i.toString();
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

    /**
     * @return an assignment for which the value of posterior is positive (and large), null if not known.
     *         This can be used as the initial sample in samplers...
     */
    public VarAssignment generateAWeightVectorHighlyProbablePosteriorly() {

        double[] auxiliaryWeightVector = db.getAuxiliaryWeightVector();
        if (auxiliaryWeightVector == null) return null;

        Integer dim = auxiliaryWeightVector.length;
        HashMap<String, Double> continuousVarAssign = new HashMap<String, Double>(dim);
        for (int i = 0; i < dim; i++) {
            continuousVarAssign.put(weightVectorName + "_" + i, auxiliaryWeightVector[i]);
        }

        return new VarAssignment(new HashMap<String, Boolean>(), continuousVarAssign);
    }
}
