package hgm.preference;

import hgm.preference.db.Preference;
import hgm.preference.db.PreferenceDataBase;
import xadd.XADD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Hadi Afshar.
 * Date: 19/12/13
 * Time: 8:36 PM
 */
public class Learning {
    public static final double C = 10; //todo range of uniform distributions should be decided in a more flexible way.
    public static final double EPSILON = 0.01;
    public static final double INDICATOR_NOISE = 0.1;

    private XADD context;
    private PreferenceDataBase db;

    public Learning(XADD context, PreferenceDataBase db) {
        this.context = context;
        this.db = db;
    }


    public int computeItemWithMaxExpectedUtility(XADD.XADDNode utilityWeights, String weightVectorName) {
        int chosenItemId = -1;
        double maxUtil = Double.NEGATIVE_INFINITY;
        for (int itemId = 0; itemId<db.numberOfItems(); itemId++) {
            Double[] item = db.getItemAttributeValues(itemId);
            double itemUtil = expectedItemUtility(item, utilityWeights, weightVectorName, false);
            System.out.println("itemUtil of item #" + itemId + ": " + itemUtil);
            if (itemUtil>maxUtil) {
                maxUtil = itemUtil;
                chosenItemId = itemId;
            }
        }
        return chosenItemId;
    }

    public double expectedItemUtility(Double[] item, XADD.XADDNode utilityWeights, String weightVectorName, boolean plotGraph) {
        String itemUtilStr = "([" + itemUtility(weightVectorName, item) + "])";
        System.out.println("itemUtilStr = " + itemUtilStr);
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


    //Pr(W)
    public XADD.XADDNode computeProbabilityOfWeightVector(int numAttribs, List<Preference> preferenceAnswers, String weightVectorName) {
        XADD.XADDNode[] pWeights = new XADD.XADDNode[numAttribs];

        if (preferenceAnswers.isEmpty()) {
            // uniform distribution between -C and C:
            for (int i=0; i<pWeights.length; i++) {
                pWeights[i] = uniformDistribution(weightVectorName + "_" + i, -C, C);
            }
            return multiply(pWeights);  //Pr(W)
        }

        //Pr(W | R^n)
        XADD.XADDNode prior = computeProbabilityOfWeightVector(numAttribs, preferenceAnswers.subList(0, preferenceAnswers.size() - 1), weightVectorName); //Pr(W|R^n)
        Preference lastResponse = preferenceAnswers.get(preferenceAnswers.size() - 1); //q_{ab}
        // Pr(q_{ab} | W):
        XADD.XADDNode likelihood = computePreferenceLikelihoodGivenUtilityWeights(lastResponse, weightVectorName, numAttribs);

        return multiply(likelihood, prior);
    }

    private XADD.XADDNode uniformDistribution(String var, double min, double max) {
        double u = 1.0 / (max -min);
        String s = "([" + var + "<" + min + "] ([0]) ([" + var + "<" + max + "] ([" + u + "]) ([0])))";
        int nodeId = context.buildCanonicalXADDFromString(s);
        return context.getExistNode(nodeId);
    }

    private XADD.XADDNode indicator(String condition) { //noisy indicator
        String s = "([" + condition + "] ([" + (1-INDICATOR_NOISE) + "]) ([" + (0 + INDICATOR_NOISE) + "]))";
        int nodeId = context.buildCanonicalXADDFromString(s);
        return context.getExistNode(nodeId);
    }

    // Pr(q_{ab} | W)
    private XADD.XADDNode computePreferenceLikelihoodGivenUtilityWeights(Preference preference, String weightVectorName, int numAttribs) {
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
            String w_iXx_i = itemAttributes[i] + "*" + weightVectorName + "_" + i;
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
}
