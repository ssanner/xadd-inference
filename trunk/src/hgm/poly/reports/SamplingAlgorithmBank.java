package hgm.poly.reports;

import hgm.BayesianDataGenerator;
import hgm.poly.bayesian.*;
import hgm.poly.market.BayesianMarketMakingModel;
import hgm.poly.market.MarketMakingDatabase;
import hgm.poly.pref.*;
import hgm.poly.sampling.SamplerInterface;
import hgm.preference.db.PreferenceDatabase;
import hgm.sampling.SamplingFailureException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hadi Afshar.
 * Date: 11/05/14
 * Time: 12:29 AM
 */
public class SamplingAlgorithmBank {

    public static final String GATED_GIBBS_GENERAL_MMM = "gated.gibbs.general.mmm";
    public static final String TARGETED_GATED_GIBBS_GENERAL_MMM = "targeted.gated.gibbs.general.mmm";
    public static final String REJ_ORIGINAL_MODEL_MMM = "rej.original.mmm";
    public static final String FULL_GIBBS_GENERAL_MMM = "full.gibbs.general.mmm";
    public static final String REJ_GENERAL_MMM = "rej.general.mmm";
    public static final String MH_GENERAL_MMM = "mh.general.mmm";
    public static final String TUNED_MH_GENERAL_MMM = "tuned.mh.general.mmm";

    public static final String GATED_GIBBS_GENERAL_BPPL = "gated.gibbs.general.bppl";
    public static final String REJ_ORIGINAL_MODEL_BPPL = "rej.original.bppl";
    public static final String REJ_GENERAL_BPPL = "rej.general.bppl";
    public static final String MH_GENERAL_BPPL = "mh.general.bppl";
    public static final String MH_GENERAL_BPPL2 = "mh.general.bppl2";
    public static final String TUNED_MH_GENERAL_BPPL = "tuned.mh.general.bppl";
    public static final String FULL_GIBBS_GENERAL_BPPL = "full.gibbs.general.bppl";
    public static final String GATED_GIBBS_CONST_BPPL = "gated.gibbs.const.bppl";
    public static final String TARGETED_GATED_GIBBS_CONST_BPPL = "targeted.gated.gibbs.const.bppl";
    public static final String TARGETED_GATED_GIBBS_GENERAL_BPPL = "targeted.gated.gibbs.general.bppl";
    public static final String FULL_GIBBS_CONST_BPPL = "full.gibbs.const.bppl";

    public static final String SYMBOLIC_GIBBS_CONST_BPPL = "symbolic.gibbs.const.bppl";
    public static final String TESTER_CONST_BPPL = "tester.const.bppl";


    public static List<Db2Sampler> makeDb2Samplers4MarketMakingModel(
            /*double mm_epsilon4starVars,*/ String... algorithmNames) {

        Db2Sampler[] allAlgorithms = new Db2Sampler[]{
                new Db2SamplerMMM(/*mm_epsilon4starVars*/) {
                    @Override
                    public SamplerInterface createSamplerMMM(GeneralBayesianPosteriorHandler posterior) {
                        return new GatedGibbsGeneralBayesianSampler(posterior, null);
                    }

                    @Override
                    public String getName() {
                        return GATED_GIBBS_GENERAL_MMM;
                    }
                },

                //----------------------------------------------------------------------------------------------

                new Db2SamplerMMM() {
                    @Override
                    public SamplerInterface createSamplerMMM(GeneralBayesianPosteriorHandler posterior) {
                        return new TargetedGatedGibbsGeneralBayesianSampler(posterior, null);
                    }

                    @Override
                    public String getName() {
                        return TARGETED_GATED_GIBBS_GENERAL_MMM;
                    }
                },

                //----------------------------------------------------------------------------------------------

                new Db2SamplerMMM() { //e.g. (1-bppl_indicator) noise or 0.8 in MM
                    @Override
                    SamplerInterface createSamplerMMM(GeneralBayesianPosteriorHandler posterior) {
                        return new OriginalModelRejectionBasedGeneralBayesianSampler(posterior, 1,
                                Math.max(BayesianMarketMakingModel.B1, Math.max(BayesianMarketMakingModel.B3, Math.max(BayesianMarketMakingModel.S1, BayesianMarketMakingModel.S3)))); //(e.g. 0.8)
                    }

                    @Override
                    public String getName() {
                        return REJ_ORIGINAL_MODEL_MMM;
                    }
                },

                //----------------------------------------------------------------------------------------------

                new Db2SamplerMMM(/*mm_epsilon4starVars*/) {
                    @Override
                    public SamplerInterface createSamplerMMM(GeneralBayesianPosteriorHandler posterior) {
                        return new FullGibbsGeneralBayesianSampler(posterior, null);
                    }

                    @Override
                    public String getName() {
                        return FULL_GIBBS_GENERAL_MMM;
                    }
                },

                //----------------------------------------------------------------------------------------------

                new Db2SamplerMMM(/*mm_epsilon4starVars*/) {
                    @Override
                    public SamplerInterface createSamplerMMM(GeneralBayesianPosteriorHandler posterior) {
                        return new RejectionBasedGeneralBayesianSampler(posterior, 1.0);
                    }

                    @Override
                    public String getName() {
                        return REJ_GENERAL_MMM;
                    }
                },

                //----------------------------------------------------------------------------------------------

                new Db2SamplerMMM() {
                    @Override
                    public SamplerInterface createSamplerMMM(GeneralBayesianPosteriorHandler posterior) {
                        return new MetropolisHastingGeneralBayesianSampler(posterior, 0.1);
                    }

                    @Override
                    public String getName() {
                        return MH_GENERAL_MMM;
                    }
                },

                //----------------------------------------------------------------------------------------------

                new Db2SamplerMMM() {
                    @Override
                    public SamplerInterface createSamplerMMM(GeneralBayesianPosteriorHandler posterior) {
                        return new SelfTunedMetropolisHastingGeneralBayesianSampler(posterior, 10.0, 100, 1000);//60, 100);
                    }

                    @Override
                    public String getName() {
                        return TUNED_MH_GENERAL_MMM;
                    }
                },
        };

        return chooseFrom(allAlgorithms, algorithmNames);

    }

    //////////////////////////////////////////////////////////////////////////////////////////////////

    public static List<Db2Sampler> makeDb2Samplers4PrefLearningModel(
            final double bppl_indicatorNoise,
            int bppl_const_maxGatingConditionViolation,
            String... algorithmNames) {

        Db2Sampler[] allAlgorithms = new Db2Sampler[]{
                new Db2SamplerBPPLWithGeneralBayesianPosterior(bppl_indicatorNoise) {
                    @Override
                    SamplerInterface createSamplerBPPL(GeneralBayesianPosteriorHandler posterior) {
                        return new GatedGibbsGeneralBayesianSampler(posterior, null);
                    }

                    @Override
                    public String getName() {
                        return GATED_GIBBS_GENERAL_BPPL;
                    }
                },

                //----------------------------------------------------------------------------------------------
                new Db2SamplerBPPLWithGeneralBayesianPosterior(bppl_indicatorNoise) { //e.g. (1-bppl_indicator noise) or 0.8 in MM
                    @Override
                    SamplerInterface createSamplerBPPL(GeneralBayesianPosteriorHandler posterior) {
                        return new OriginalModelRejectionBasedGeneralBayesianSampler(posterior, 1, 1.0-bppl_indicatorNoise);
                    }

                    @Override
                    public String getName() {
                        return REJ_ORIGINAL_MODEL_BPPL;
                    }
                },

                //----------------------------------------------------------------------------------------------

                new Db2SamplerBPPLWithGeneralBayesianPosterior(bppl_indicatorNoise) {
                    @Override
                    SamplerInterface createSamplerBPPL(GeneralBayesianPosteriorHandler posterior) {
                        return new RejectionBasedGeneralBayesianSampler(posterior, 1.0);
                    }

                    @Override
                    public String getName() {
                        return REJ_GENERAL_BPPL;
                    }
                },

                //----------------------------------------------------------------------------------------------

                new Db2SamplerBPPLWithGeneralBayesianPosterior(bppl_indicatorNoise) {
                    @Override
                    SamplerInterface createSamplerBPPL(GeneralBayesianPosteriorHandler posterior) {
                        return new MetropolisHastingGeneralBayesianSampler(posterior, 1.0);//0.3
                    }

                    @Override
                    public String getName() {
                        return MH_GENERAL_BPPL;
                    }
                },

                new Db2SamplerBPPLWithGeneralBayesianPosterior(bppl_indicatorNoise) {
                    @Override
                    SamplerInterface createSamplerBPPL(GeneralBayesianPosteriorHandler posterior) {
                        return new MetropolisHastingGeneralBayesianSampler(posterior, 3.0);
                    }

                    @Override
                    public String getName() {
                        return MH_GENERAL_BPPL2;
                    }
                },

                new Db2SamplerBPPLWithGeneralBayesianPosterior(bppl_indicatorNoise) {
                    @Override
                    SamplerInterface createSamplerBPPL(GeneralBayesianPosteriorHandler posterior) {
                        return new SelfTunedMetropolisHastingGeneralBayesianSampler(posterior, 10.0, 30, 100);
                    }

                    @Override
                    public String getName() {
                        return TUNED_MH_GENERAL_BPPL;
                    }
                },

                //----------------------------------------------------------------------------------------------

                new Db2SamplerBPPLWithGeneralBayesianPosterior(bppl_indicatorNoise) {
                    @Override
                    SamplerInterface createSamplerBPPL(GeneralBayesianPosteriorHandler posterior) {

                        return new FullGibbsGeneralBayesianSampler(posterior, null);
                    }

                    @Override
                    public String getName() {
                        return FULL_GIBBS_GENERAL_BPPL;
                    }
                },

                //----------------------------------------------------------------------------------------------

                new Db2SamplerBPPLWithGeneralBayesianPosterior(bppl_indicatorNoise) {
                    Double[] reuse = null;
                    @Override
                    SamplerInterface createSamplerBPPL(GeneralBayesianPosteriorHandler posterior) {


                        TargetedGatedGibbsGeneralBayesianSampler targetedGatedGibbsGeneralBayesianSampler = new TargetedGatedGibbsGeneralBayesianSampler(posterior, null);
//                        targetedGatedGibbsGeneralBayesianSampler.setReusable(reuse);
                        return targetedGatedGibbsGeneralBayesianSampler;
                    }

                    @Override
                    public String getName() {
                        return TARGETED_GATED_GIBBS_GENERAL_BPPL;
                    }

                    @Override
                    public void setReusableSample(double[] reuse) {
//                        this.reuse = new Double[reuse.length];
//                        for (int i = 0; i < reuse.length; i++) {
//                            this.reuse[i] =reuse[i];
//                        }
                    }
                },

                //----------------------------------------------------------------------------------------------

                new Db2SamplerBPPLWithConstantBayesianPosterior(bppl_indicatorNoise, bppl_const_maxGatingConditionViolation) {

                    @Override
                    SamplerInterface createSamplerBPPL(ConstantBayesianPosteriorHandler posterior) {
                        return GatedGibbsPolytopesSampler.makeSampler(posterior,
                                -BayesianPairwisePreferenceLearningModel.C,
                                BayesianPairwisePreferenceLearningModel.C, null);
                    }

                    @Override
                    public String getName() {
                        return GATED_GIBBS_CONST_BPPL;
                    }
                },

                //----------------------------------------------------------------------------------------------

                new Db2SamplerBPPLWithConstantBayesianPosterior(bppl_indicatorNoise, bppl_const_maxGatingConditionViolation) {
                    @Override
                    SamplerInterface createSamplerBPPL(ConstantBayesianPosteriorHandler posterior) {
                        return TargetedGatedGibbsPolytopesSampler.makeCleverGibbsSampler(posterior,
                                -BayesianPairwisePreferenceLearningModel.C,
                                BayesianPairwisePreferenceLearningModel.C, null);
                    }

                    @Override
                    public String getName() {
                        return TARGETED_GATED_GIBBS_CONST_BPPL;
                    }
                },

                //----------------------------------------------------------------------------------------------


                new Db2Sampler(){
                    @Override
                    public String getName() {
                        return TESTER_CONST_BPPL;
                    }

                    @Override
                    public SamplerInterface createSampler(final BayesianDataGenerator db) {
                        if (!(db instanceof PreferenceDatabase)) throw new RuntimeException("Pref. Db. required...");
//                        final BayesianPairwisePreferenceLearningModel model = new BayesianPairwisePreferenceLearningModel((PreferenceDatabase) db, bppl_indicatorNoise);

//                        return createSamplerBPPL(model);
                        return new SamplerInterface() {
                            @Override
                            public Double[] reusableSample() throws SamplingFailureException {
                                double[] aux = ((PreferenceDatabase) db).getAuxiliaryWeightVector();
                                if (aux == null) throw new RuntimeException();

                                final Double[] auxDouble = new Double[aux.length];
                                for (int i = 0; i < aux.length; i++) {
                                    auxDouble[i] = aux[i];
                                }
                                return auxDouble;
                            }
                        };
                    }

                    @Override
                    public void setReusableSample(double[] reuse) {
                    }

//                    abstract SamplerInterface createSamplerBPPL(BayesianPairwisePreferenceLearningModel model);
                },

                //----------------------------------------------------------------------------------------------

                new Db2SamplerBPPLWithConstantBayesianPosterior(bppl_indicatorNoise, bppl_const_maxGatingConditionViolation) {
                    @Override
                    SamplerInterface createSamplerBPPL(ConstantBayesianPosteriorHandler posterior) {
                        return FullGibbsPolytopesSampler.makeFullGibbsSampler(posterior,
                                -BayesianPairwisePreferenceLearningModel.C,
                                BayesianPairwisePreferenceLearningModel.C, null);
                    }

                    @Override
                    public String getName() {
                        return FULL_GIBBS_CONST_BPPL;
                    }
                },

                //----------------------------------------------------------------------------------------------

                new Db2SamplerBPPLWithConstantBayesianPosterior(bppl_indicatorNoise, bppl_const_maxGatingConditionViolation) {
                    @Override
                    SamplerInterface createSamplerBPPL(ConstantBayesianPosteriorHandler posterior) {
                        return SymbolicGibbsPolytopesSampler.makeSampler(posterior,
                                -BayesianPairwisePreferenceLearningModel.C,
                                BayesianPairwisePreferenceLearningModel.C, null);
                    }

                    @Override
                    public String getName() {
                        return SYMBOLIC_GIBBS_CONST_BPPL;
                    }
                },
        };

        return chooseFrom(allAlgorithms, algorithmNames);

    }

    private static List<Db2Sampler> chooseFrom(Db2Sampler[] allAlgorithms, String[] algorithmNames) {
        List<Db2Sampler> chosenAlgorithms = new ArrayList<Db2Sampler>(algorithmNames.length);
        for (int i = 0; i < algorithmNames.length; i++) {
            String name = algorithmNames[i];
            boolean found = false;
            for (Db2Sampler algorithm : allAlgorithms) {
                if (algorithm.getName().equals(name)) {
                    chosenAlgorithms.add(algorithm);
                    found = true;
                    break;
                }
            }
            if (!found) throw new RuntimeException("Algorithm: " + name + " not found");
        }
        return chosenAlgorithms;
    }
} //end main class

//////////////////////////////////////////////////////////////////////////////////////////////////

// Other classes:

//BPPL:
abstract class Db2SamplerBPPL implements Db2Sampler {
    double indicatorNoise;

    protected Db2SamplerBPPL(double indicatorNoise) {
        this.indicatorNoise = indicatorNoise;
    }

    @Override
    public SamplerInterface createSampler(BayesianDataGenerator db) {
        if (!(db instanceof PreferenceDatabase)) throw new RuntimeException("Pre. Db. required...");
        BayesianPairwisePreferenceLearningModel model = new BayesianPairwisePreferenceLearningModel((PreferenceDatabase) db, indicatorNoise);

        return createSamplerBPPL(model);
    }

    abstract SamplerInterface createSamplerBPPL(BayesianPairwisePreferenceLearningModel model);

    @Override
    public void setReusableSample(double[] reuse) {
    }
}

abstract class Db2SamplerBPPLWithGeneralBayesianPosterior extends Db2SamplerBPPL {

    protected Db2SamplerBPPLWithGeneralBayesianPosterior(double indicatorNoise) {
        super(indicatorNoise);
    }

    @Override
    SamplerInterface createSamplerBPPL(BayesianPairwisePreferenceLearningModel model) {

        // Pr(W | R^{n+1})
        GeneralBayesianPosteriorHandler posterior = model.computeBayesianPosterior();
        return createSamplerBPPL(posterior);
    }

    abstract SamplerInterface createSamplerBPPL(GeneralBayesianPosteriorHandler posterior);
}

abstract class Db2SamplerBPPLWithConstantBayesianPosterior extends Db2SamplerBPPL {
    int maxGatingConditionViolation;

    protected Db2SamplerBPPLWithConstantBayesianPosterior(double indicatorNoise, int maxGatingConditionViolation) {
        super(indicatorNoise);
        this.maxGatingConditionViolation = maxGatingConditionViolation;
    }

    @Override
    public SamplerInterface createSamplerBPPL(BayesianPairwisePreferenceLearningModel model) {
        ConstantBayesianPosteriorHandler posterior = model.computePosteriorWeightVector(maxGatingConditionViolation);
        return createSamplerBPPL(posterior);
    }

    abstract SamplerInterface createSamplerBPPL(ConstantBayesianPosteriorHandler posterior);
}

//MMM

abstract class Db2SamplerMMM implements Db2Sampler {
//    double mm_epsilon_for_star_vars;

    protected Db2SamplerMMM(/*double mm_epsilon_for_star_vars*/) {
//        this.mm_epsilon_for_star_vars = mm_epsilon_for_star_vars;
    }

    @Override
    public SamplerInterface createSampler(BayesianDataGenerator db) {
        if (!(db instanceof MarketMakingDatabase)) throw new RuntimeException("MM. Db. required...");

        BayesianMarketMakingModel model =
                new BayesianMarketMakingModel((MarketMakingDatabase) db/*, mm_epsilon_for_star_vars, "v"*/);

        // Pr(V | R^{n+1})
        GeneralBayesianPosteriorHandler posterior = model.computeBayesianPosterior();
        return createSamplerMMM(posterior);
    }

    @Override
    public void setReusableSample(double[] reuse) {
    }

    abstract SamplerInterface createSamplerMMM(GeneralBayesianPosteriorHandler posterior);
}



