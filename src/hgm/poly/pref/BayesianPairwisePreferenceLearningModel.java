package hgm.poly.pref;

import hgm.poly.*;
import hgm.poly.bayesian.BayesianModel;
import hgm.poly.bayesian.GeneralBayesianPosteriorHandler;
import hgm.preference.Preference;
import hgm.preference.db.PreferenceDatabase;
import hgm.sampling.VarAssignment;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Hadi Afshar.
 * Date: 19/12/13
 * Time: 8:36 PM
 */
public class BayesianPairwisePreferenceLearningModel extends BayesianModel<Preference> {

    //todo: when you what to change check which classes (e.g. SlackGibbsPolytopeSampler) use it.
    /*
    Prior weight of Ws is considered uniform in [-C, C]
     */
    @Deprecated
    public static final double C = 10.0; //todo range of uniform distributions should be decided in a more flexible way.

    private double indicatorNoise;

    public BayesianPairwisePreferenceLearningModel(PreferenceDatabase db, double indicatorNoise) {     //todo: I think indicator noise should go up to database
        super(db);

        if (indicatorNoise > 0.5 || indicatorNoise < 0.0)
            System.err.println("indicator noise should be in [0, 0.5] to make sense.");
        this.indicatorNoise = indicatorNoise;

    }

    @Deprecated
    public ConstantBayesianPosteriorHandler computePosteriorWeightVector(int maxGatingConditionViolation) {     //todo go up
        return computePrefPosteriorWeightVector(((PreferenceDatabase) db).getObservedDataPoints(), maxGatingConditionViolation); //todo do not cast...
    }

    @Deprecated
    //Pr(W | R^n)
    private ConstantBayesianPosteriorHandler computePrefPosteriorWeightVector(List<Preference> preferenceResponses,
                                                              int maxGatingConditionViolation) {

        PiecewisePolynomial priorPP = prior.getPrior();
        List<ConstrainedPolynomial> priorCases = priorPP.getCases();
        if (priorCases.size() != 1) throw new RuntimeException("this old fashioned method works only with a prior with a single case");
        ConstrainedPolynomial priorOnlyCase = priorCases.get(0);
//        ConstrainedPolynomial prior = computePrior();
        ConstantBayesianPosteriorHandler gatedPolytopes = new ConstantBayesianPosteriorHandler(factory, priorOnlyCase, indicatorNoise, maxGatingConditionViolation);

        for (Preference prefResponse : preferenceResponses) {
            // Pr(q_{ab} | W):
            Polynomial likelihoodPositiveConstraint = computePreferenceLikelihoodPositiveConstraintGivenUtilityWeights((PreferenceDatabase)db, prefResponse);
            gatedPolytopes.addPositiveConstraint(likelihoodPositiveConstraint);
        }

        return gatedPolytopes;
    }

//    public GeneralBayesianPosteriorHandler computeBayesianPosterior() {
//        GeneralBayesianPosteriorHandler posterior = new GeneralBayesianPosteriorHandler(prior.getPrior());
//        PreferenceDatabase db = (PreferenceDatabase)this.db;
//        for (Preference prefResponse : db.getObservedDataPoints()) {
//            Pr(q_{ab} | W):
//            posterior.addLikelihood(likelihood);
//        }
//        return posterior;
//    }

    @Override
    protected PiecewisePolynomial computeLikelihoodGivenValueVector(Preference response) {
        Polynomial likelihoodPositiveConstraint = computePreferenceLikelihoodPositiveConstraintGivenUtilityWeights((PreferenceDatabase)db, response);  //I wish I could avoid casting
        Polynomial posPoly = factory.makePolynomial(Double.toString(1 - indicatorNoise));
        Polynomial negPoly = factory.makePolynomial(Double.toString(indicatorNoise));
        ConstrainedPolynomial posCase = new ConstrainedPolynomial(posPoly, Arrays.asList(likelihoodPositiveConstraint));

        Polynomial likelihoodNegativeConstraint = likelihoodPositiveConstraint.clone();
        likelihoodNegativeConstraint.multiplyScalarInThis(-1);
        ConstrainedPolynomial negCase = new ConstrainedPolynomial(negPoly, Arrays.asList(likelihoodNegativeConstraint));

        return new PiecewisePolynomial(posCase, negCase);
    }

    //--------------------------------------------------------------------------

   /* private PiecewisePolynomial computePrior() {
        int numAttribs = factory.numberOfVars();
        //1. prior: pr(W)
        String[] constraints = new String[numAttribs * 2];
        for (int i = 0; i < factory.numberOfVars(); i++) {
            String w_i = weightVectorName + "_" + i;
            constraints[2 * i] = w_i + "^(1) + " + C + ">0";
            constraints[2 * i + 1] = "-1*" + w_i + "^(1) + " + C + ">0";
        }

        return new PiecewisePolynomial(Arrays.asList(factory.makeConstrainedPolynomial("1", constraints)));
    }*/



    // Pr(q_{ab} | W)
    // returns u(a|w) - u(b|w) if a > b and u(b|w) - u(a|w) if a < b
    private Polynomial computePreferenceLikelihoodPositiveConstraintGivenUtilityWeights(PreferenceDatabase db, Preference preference/*, String weightVectorName, int numAttribs*/) {
        int itemId1 = preference.getItemId1();
        int itemId2 = preference.getItemId2();
        Double[] item1Attribs = db.getItemAttributeValues(itemId1);
        Double[] item2Attribs = db.getItemAttributeValues(itemId2);


//        String u1Str = itemUtilityStr(weightVectorName, item1Attribs);
//        String u2Str = itemUtilityStr(weightVectorName, item2Attribs);
        String u1Str = itemUtilityStr(item1Attribs);
        String u2Str = itemUtilityStr(item2Attribs);

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
    private String itemUtilityStr(/*String weightVectorName,*/ Double[] itemAttributes) {
        StringBuilder sumW_iXx_i = new StringBuilder();
        for (int i = 0; i < itemAttributes.length; i++) {
            String w_iXx_i = itemAttributes[i] + "*" + vars[i] + "^(1)"; //weightVectorName + "_" + i + "^(1)";
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
    @Deprecated //should not be used //todo remove
    private VarAssignment generateAWeightVectorHighlyProbablePosteriorly() {
        PreferenceDatabase db = (PreferenceDatabase)this.db;

        double[] auxiliaryWeightVector = db.getAuxiliaryWeightVector();
        if (auxiliaryWeightVector == null) return null;

        Integer dim = auxiliaryWeightVector.length;
        HashMap<String, Double> continuousVarAssign = new HashMap<String, Double>(dim);
        for (int i = 0; i < dim; i++) {
            continuousVarAssign.put(vars[i], auxiliaryWeightVector[i]);
        }

        return new VarAssignment(new HashMap<String, Boolean>(), continuousVarAssign);
    }
}
