package hgm.poly.reports.sg;

import hgm.asve.Pair;
import hgm.poly.Fraction;
import hgm.poly.PiecewiseExpression;
import hgm.poly.diagnostics.MultiArrayMultiStatistics;
import hgm.poly.gm.*;
import hgm.poly.sampling.SamplerInterface;
import hgm.poly.sampling.frac.*;
import hgm.sampling.SamplingFailureException;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 18/08/14
 * Time: 7:24 PM
 */
@Deprecated //todo delete
public class NewSymbolicGibbsAAAI2015Tester {

   /* public static final String REPORT_PATH_COLLISION_ANALYSIS = "E:/REPORT_PATH_AAAI15/collision/";
    public static final String REPORT_PATH_FERMENTATION_ANALYSIS = "E:/REPORT_PATH_AAAI15/fermentation/";
    public static final String REPORT_PATH_CIRCUITS_ANALYSIS = "E:/REPORT_PATH_AAAI15/circuits/";

    public static void main(String[] args) throws IOException {
        NewSymbolicGibbsAAAI2015Tester instance = new NewSymbolicGibbsAAAI2015Tester();
//        instance.collisionAAAI2015Test(true);
        instance.fermentationAAAI2015Test();
//        instance.circuitAAAI2015Test();
    }

    public void circuitAAAI2015Test() throws IOException {
        System.out.println("REPORT_PATH_CIRCUITS_ANALYSIS = " + REPORT_PATH_CIRCUITS_ANALYSIS);

//        JointToSampler testerSampleMaker =
//                FractionalJointRejectionSampler.makeJointToSampler(1.0);
//                FractionalJointSymbolicGibbsSampler.makeJointToSampler();
//        int numSamplesFromTesterToSimulateTrueDistribution = 170000;//100000;
//        int maxWaitingTimeForTesterToSimulateMillis = 1000 * 60*10;//1000 * 60 * 10;
        List<JointToSampler> samplerMakersToBeTested = Arrays.asList(
                FractionalJointSymbolicGibbsSampler.makeJointToSampler()
                , FractionalJointBaselineGibbsSampler.makeJointToSampler(),
                FractionalJointRejectionSampler.makeJointToSampler(1.1)
                ,FractionalJointMetropolisHastingSampler.makeJointToSampler(5.0),
                FractionalJointSelfTunedMetropolisHastingSampler.makeJointToSampler(10, 20, 10)
        );
        int[] numParams = {8, 9, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30};//{2, 6, 10, 14, 18, 25, 30};//{2, 3};
        int numMinDesiredSamples = 200;//1000; //100;
        int maxWaitingTimeForTakingDesiredSamples = 1000*60*2;//1000 * 60 * 2;//1000*60*5;//1000 * 20;
        int minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis = 1000 * 5;//1000*60;//1000 * 5;
        int approxNumTimePointsForWhichErrIsPersisted = 100;//33;
        int numRuns = 15;//10;//20;//2;
        int burnedSamples = 200;//50;
        double goldenErrThreshold = 0.05;//0.02;////0.2;

        final Double lowerBound = 9.5;
        final Double upperBound = 10.5;

        Param2JointWrapper collisionModelParam2Joint = new Param2JointWrapper() {

            @Override
            public JointWrapper makeJointWrapper(int param) {

                double minVarLimit = lowerBound;//5;//lowerBound-0.1;
                double maxVarLimit = upperBound;//15;//upperBound+0.1;

                GraphicalModel bn =
                        ExperimentalGraphicalModels.makeCircuitModel(param, lowerBound, upperBound);

                SymbolicGraphicalModelHandler handler = new SymbolicGraphicalModelHandler();
                Map<String, Double> evidence = new HashMap<String, Double>();

                evidence.put("R_t", (0.5*(upperBound + lowerBound))/(double)param);        //todo...    ???

                List<String> query = Arrays.asList("R_1", "R_" + (param));
                PiecewiseExpression<Fraction> joint = handler.makeJoint(bn, query, evidence);
                JointWrapper jointWrapper = new JointWrapper(joint, minVarLimit, maxVarLimit);
                return jointWrapper;
            }
        };


        testSamplersPerformanceWrtParameterTimeAndSampleCount(collisionModelParam2Joint,
                0.5*(upperBound + lowerBound),//10d,
                null, //testerSampleMaker,
                -1, //                numSamplesFromTesterToSimulateTrueDistribution,
                -1, // maxWaitingTimeForTesterToSimulateMillis,
                samplerMakersToBeTested, numParams, numMinDesiredSamples,
                maxWaitingTimeForTakingDesiredSamples,
                minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis,
                approxNumTimePointsForWhichErrIsPersisted, numRuns, burnedSamples, REPORT_PATH_CIRCUITS_ANALYSIS, goldenErrThreshold);

        System.out.println(" That was all the folk for circuits problem. ");

    }

    public void fermentationAAAI2015Test() throws IOException {
        System.out.println("REPORT_PATH_FERMENTATION_ANALYSIS = " + REPORT_PATH_FERMENTATION_ANALYSIS);

//        JointToSampler testerSampleMaker =
//                FractionalJointRejectionSampler.makeJointToSampler(1.0);
//                new SelectedQuerySampler(FractionalJointSymbolicGibbsSampler.makeJointToSampler());
//                FractionalJointMetropolisHastingSampler.makeJointToSampler(4.0);
//                new SelectedQuerySampler(FractionalJointBaselineGibbsSampler.makeJointToSampler());

//        int numSamplesFromTesterToSimulateTrueDistribution = 1000000;//1000;
//        int maxWaitingTimeForTesterToSimulateMillis = 1000 * 60 * 60 * 5;//1000 * 60 * 1;
        List<JointToSampler> samplerMakersToBeTested = new ArrayList<JointToSampler>();
        samplerMakersToBeTested.add(new DifferenceSampler(
                new QuerySelectorSamplerMaker(FractionalJointSymbolicGibbsSampler.makeJointToSampler()),
                new QuerySelectorSamplerMaker(FractionalJointSymbolicGibbsSampler.makeJointToSampler())));
        samplerMakersToBeTested.add(new DifferenceSampler(
                new QuerySelectorSamplerMaker(FractionalJointBaselineGibbsSampler.makeJointToSampler()),
                new QuerySelectorSamplerMaker(FractionalJointBaselineGibbsSampler.makeJointToSampler())));
//        samplerMakersToBeTested.add(new SelectedQuerySampler(FractionalJointRejectionSampler.makeJointToSampler(1)));
        samplerMakersToBeTested.add(new DifferenceSampler(
                new QuerySelectorSamplerMaker(FractionalJointMetropolisHastingSampler.makeJointToSampler(5.0)),
                new QuerySelectorSamplerMaker(FractionalJointMetropolisHastingSampler.makeJointToSampler(5.0))));
        samplerMakersToBeTested.add(new DifferenceSampler(
                new QuerySelectorSamplerMaker(FractionalJointSelfTunedMetropolisHastingSampler.makeJointToSampler(10, 20, 10)),
                new QuerySelectorSamplerMaker(FractionalJointSelfTunedMetropolisHastingSampler.makeJointToSampler(10, 20, 10))));

        int[] numParams = {3,4, 5, 6, 7, 8, 9, 10, 11, 12 ,13, 14, 15, 16, 17, 18, 19, 20};
        int numMinDesiredSamples = 200;//1000;
        int maxWaitingTimeForTakingDesiredSamples = 1000 * 60 * 2;//1000 * 20;
        int minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis = 1000*5;//1000 * 5;
        int approxNumTimePointsForWhichErrIsPersisted = 100;//33;
        int numRuns = 10;
        int burnedSamples = 100;//10;
        double goldenErrThreshold = 0.02;


        Param2JointWrapper fermentationModelParam2Joint = new Param2JointWrapper() {

            @Override
            public JointWrapper makeJointWrapper(int param) {
//                Double muAlpha = 0.2;
//                Double muBeta = 2.2;
//                Double nuAlpha = -2.0;
//                Double nuBeta = 2.0;
                double minVarLimit = 0d;
                double maxVarLimit = 1d;

                GraphicalModel bn =
//                        makeFermentationModel(param, 1d, 0.1, 12d);
                        ExperimentalGraphicalModels.makeSimplifiedFermentationModel(param, 0d, 1d);//0.1, 12d);


                SymbolicGraphicalModelHandler handler = new SymbolicGraphicalModelHandler();
                Map<String, Double> evidence = new HashMap<String, Double>();

                evidence.put("q", 0.2);

                List<String> query = Arrays.asList("l_1", "l_" + (param));
//                PiecewiseExpression<Fraction> joint = handler.makeJoint(bn, query, evidence);
                Pair<PiecewiseExpression<Fraction>, List<DeterministicFactor>> jointAndEliminatedStochasticVars =
                        handler.makeJointAndEliminatedStochasticVars(bn, query, evidence);
                PiecewiseExpression<Fraction> joint = jointAndEliminatedStochasticVars.getFirstEntry();
                List<DeterministicFactor> eliminatedStochasticVars = jointAndEliminatedStochasticVars.getSecondEntry();

                JointWrapper jointWrapper = new RichJointWrapper(joint, eliminatedStochasticVars, query, minVarLimit, maxVarLimit, bn, evidence);

                return jointWrapper;
            }
        };


        testSamplersPerformanceWrtParameterTimeAndSampleCount(fermentationModelParam2Joint,
                0d,//null,
                null, //testerSampleMaker,
                -1,//numSamplesFromTesterToSimulateTrueDistribution,
                -1,//maxWaitingTimeForTesterToSimulateMillis,
                samplerMakersToBeTested, numParams, numMinDesiredSamples,
                maxWaitingTimeForTakingDesiredSamples,
                minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis,
                approxNumTimePointsForWhichErrIsPersisted, numRuns, burnedSamples, REPORT_PATH_FERMENTATION_ANALYSIS, goldenErrThreshold);

        System.out.println(" That was all the folk for fermentation. ");

    }

    //////////////////////////////////////////////////////////////////////////
    @Deprecated
    public void nonSymmetricCollisionAAAI2015Test() throws IOException {
        System.out.println("REPORT_PATH_COLLISION_ANALYSIS = " + REPORT_PATH_COLLISION_ANALYSIS);

        JointToSampler testerSampleMaker =
                FractionalJointRejectionSampler.makeJointToSampler(1.0);
//                FractionalJointSymbolicGibbsSampler.makeJointToSampler();
        int numSamplesFromTesterToSimulateTrueDistribution = 1000;//100000;
        int maxWaitingTimeForTesterToSimulateMillis = 1000 * 60 * 1;//1000 * 60 * 10;
        List<JointToSampler> samplerMakersToBeTested = Arrays.asList(
                FractionalJointSymbolicGibbsSampler.makeJointToSampler(),
                FractionalJointBaselineGibbsSampler.makeJointToSampler(),
                FractionalJointRejectionSampler.makeJointToSampler(1),
                FractionalJointMetropolisHastingSampler.makeJointToSampler(5.0),
                FractionalJointSelfTunedMetropolisHastingSampler.makeJointToSampler(10, 20, 10)
        );
        int[] numParams = {2};//{2, 3};
        int numMinDesiredSamples = 1000;//1000; //100;
        int maxWaitingTimeForTakingDesiredSamples = 1000 * 30;//1000*60*5;//1000 * 20;
        int minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis = 1000 * 25;//1000*60;//1000 * 5;
        int approxNumTimePointsForWhichErrIsPersisted = 100;//33;
        int numRuns = 2;//20;//2;
        int burnedSamples = 100;//50;
        int goldenErrThreshold = 0;

        Param2JointWrapper collisionModelParam2Joint = new Param2JointWrapper() {
            @Override
            public JointWrapper makeJointWrapper(int param) {
                Double muAlpha = 0.2;
                Double muBeta = 2.2;
                Double nuAlpha = -2.0;
                Double nuBeta = 2.0;
                double minVarLimit = -2.3;
                double maxVarLimit = 2.3;

                GraphicalModel bn =
                        ExperimentalGraphicalModels.makeCollisionModel(param, muAlpha, muBeta, nuAlpha, nuBeta, false);//paramDataCount2DataGenerator.createJointGenerator(param);

                SymbolicGraphicalModelHandler handler = new SymbolicGraphicalModelHandler();
                Map<String, Double> evidence = new HashMap<String, Double>();

                evidence.put("p_t", 3d);        //todo...    ???
                evidence.put("m_1", 2d);
//        evidence.put("v_2", 0.2d);

                List<String> query = Arrays.asList("v_1", "v_" + (param - 1));
                PiecewiseExpression<Fraction> joint = handler.makeJoint(bn, query, evidence);
                JointWrapper jointWrapper = new JointWrapper(joint, minVarLimit, maxVarLimit);
                return jointWrapper;
            }
        };


        testSamplersPerformanceWrtParameterTimeAndSampleCount(collisionModelParam2Joint,
                null,
                testerSampleMaker,
                numSamplesFromTesterToSimulateTrueDistribution,
                maxWaitingTimeForTesterToSimulateMillis,
                samplerMakersToBeTested, numParams, numMinDesiredSamples,
                maxWaitingTimeForTakingDesiredSamples,
                minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis,
                approxNumTimePointsForWhichErrIsPersisted, numRuns, burnedSamples, REPORT_PATH_COLLISION_ANALYSIS, goldenErrThreshold);

        System.out.println(" That was all the folk. ");

    }

    //////////////////////////////////////////////////////////////////////////

    public void collisionAAAI2015Test(final boolean symmetric) throws IOException {
        System.out.println("REPORT_PATH_COLLISION_ANALYSIS = " + REPORT_PATH_COLLISION_ANALYSIS);

        JointToSampler testerSampleMaker =
                FractionalJointRejectionSampler.makeJointToSampler(1.0);
//                FractionalJointSymbolicGibbsSampler.makeJointToSampler();
//        int numSamplesFromTesterToSimulateTrueDistribution = 170000;//100000;
//        int maxWaitingTimeForTesterToSimulateMillis = 1000 * 60*10;//1000 * 60 * 10;
        List<JointToSampler> samplerMakersToBeTested = Arrays.asList(
                FractionalJointSymbolicGibbsSampler.makeJointToSampler(),
                FractionalJointBaselineGibbsSampler.makeJointToSampler(),
                FractionalJointRejectionSampler.makeJointToSampler(1),
                FractionalJointMetropolisHastingSampler.makeJointToSampler(5.0),
                FractionalJointSelfTunedMetropolisHastingSampler.makeJointToSampler(10, 20, 10)
        );
        int[] numParams = {3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30};//{2, 3};
        int numMinDesiredSamples = 200;//1000; //100;
        int maxWaitingTimeForTakingDesiredSamples = 1000 * 60 * 2;//1000*60*5;//1000 * 20;
        int minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis = 1000 * 5;//1000*60;//1000 * 5;
        int approxNumTimePointsForWhichErrIsPersisted = 100;//33;
        int numRuns = 10;//20;//2;
        int burnedSamples = 100;//50;
        double goldenErrThreshold = 0.2;


        Param2JointWrapper collisionModelParam2Joint = new Param2JointWrapper() {

            @Override
            public JointWrapper makeJointWrapper(int param) {
                Double muAlpha = -2.2;//0.2;
                Double muBeta = 2.2;
                Double nuAlpha = -2.0;
                Double nuBeta = 2.0;
                double minVarLimit = -2.3;
                double maxVarLimit = 2.3;

                GraphicalModel bn =
                        ExperimentalGraphicalModels.makeCollisionModel(param, muAlpha, muBeta, nuAlpha, nuBeta, symmetric);//paramDataCount2DataGenerator.createJointGenerator(param);

                SymbolicGraphicalModelHandler handler = new SymbolicGraphicalModelHandler();
                Map<String, Double> evidence = new HashMap<String, Double>();

                evidence.put("p_t", 3d);        //todo...    ???
//                evidence.put("m_1", 2d);
//        evidence.put("v_2", 0.2d);

                List<String> query = Arrays.asList("v_1", "v_" + (param - 1));    //todo should it be param rather than param - 1 ????
                PiecewiseExpression<Fraction> joint = handler.makeJoint(bn, query, evidence);
                JointWrapper jointWrapper = new JointWrapper(joint, minVarLimit, maxVarLimit);
                return jointWrapper;
            }
        };


        testSamplersPerformanceWrtParameterTimeAndSampleCount(collisionModelParam2Joint, 0d,
                testerSampleMaker,
                -1, //                numSamplesFromTesterToSimulateTrueDistribution,
                -1, // maxWaitingTimeForTesterToSimulateMillis,
                samplerMakersToBeTested, numParams, numMinDesiredSamples,
                maxWaitingTimeForTakingDesiredSamples,
                minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis,
                approxNumTimePointsForWhichErrIsPersisted, numRuns, burnedSamples, REPORT_PATH_COLLISION_ANALYSIS, goldenErrThreshold);

        System.out.println(" That was all the folk for symmetric collision problem. ");

    }

    private void testSamplersPerformanceWrtParameterTimeAndSampleCount(
            Param2JointWrapper paramToJointWrapper, //model
            Double knownGroundTruthMeansOfAllVariables, //null if unknown
            JointToSampler testerSampleMaker,  //used as baseline (if known ground truth is null)
            int numSamplesFromTesterToSimulateTrueDistribution, // (used only if ground truth means are not given)
            long maxWaitingTimeForTesterToSimulateMillis,
            List<JointToSampler> samplerMakersToBeTested,
            int[] numParams, // number of states in a dynamic model, or number of objects in collision model, ..., parameter space dimension
            int numMinDesiredSamples,     //used for error vs. #samples diagram
            long maxWaitingTimeForTakingDesiredSamples, //if sampling takes more than this, sampling would be terminated without an answer
            long minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis, //used for error vs. time diagram
            int approxNumTimePointsForWhichErrIsPersisted,
            int numRuns,
            int burnedSamples, //used for both tester and tested algorithms
            String outputDirectoryPath,
            double goldenErrThreshold) throws IOException {


//        TotalTimeKeeper timeKeeper = new TotalTimeKeeper(samplerMakersToBeTested, numParams, outputDirectoryPath);
//        AlgorithmDeathLimitKeeper testedAlgsDeathKeeper = new AlgorithmDeathLimitKeeper(samplerMakersToBeTested);  todo un-comment if necessary

        GoldenTimesKeeper timeToPassGoldErrKeeper = new GoldenTimesKeeper(samplerMakersToBeTested, outputDirectoryPath);
        GoldenTimesKeeper timeToTakeGoldSamplesKeeper = new GoldenTimesKeeper(samplerMakersToBeTested, outputDirectoryPath);

        for (int param : numParams) {
            System.out.println(".......\nparam = " + param);

            //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

            JointWrapper jointWrapper = paramToJointWrapper.makeJointWrapper(param);
            //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

            double[] groundTruthMeans;
            if (knownGroundTruthMeansOfAllVariables != null) {
                groundTruthMeans = new double[jointWrapper.getAppropriateSampleVectorSize()];//[jointWrapper.getJoint().getScopeVars().size()];
                Arrays.fill(groundTruthMeans, knownGroundTruthMeansOfAllVariables);
                System.out.println("known groundTruthMeans = " + Arrays.toString(groundTruthMeans));
            } else { //ground truth mean is not given...
                try {
                    groundTruthMeans = computeGroundTruthMean(jointWrapper, testerSampleMaker,
                            numSamplesFromTesterToSimulateTrueDistribution, maxWaitingTimeForTesterToSimulateMillis);
                    System.out.println("{{{groundTruthMeans = " + Arrays.toString(groundTruthMeans) + "}}}");
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("NO GROUND TRUTH TAKEN! TERMINATED....");
                    continue;
                }
            }

            //Analysis of tested algorithms:
            for (JointToSampler samplerMaker : samplerMakersToBeTested) {
//                if (!testedAlgsDeathKeeper.algorithmWillDie(samplerMaker, param, numObservedDataPoints)) {
                try {
                    MultiMCMCChainAnalysis multiMCMCChainAnalysis = new MultiMCMCChainAnalysis(numRuns,
                            groundTruthMeans,
                            jointWrapper,
                            samplerMaker,
                            burnedSamples,
                            numMinDesiredSamples,
                            maxWaitingTimeForTakingDesiredSamples,
                            minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis,
                            approxNumTimePointsForWhichErrIsPersisted,
                            goldenErrThreshold);
                    multiMCMCChainAnalysis.persistMeanStdErrForFirstTakenErrSamples(outputDirectoryPath, param, samplerMaker.getName(), numMinDesiredSamples);
                    multiMCMCChainAnalysis.persistMeanStdErrForTimePoints(outputDirectoryPath, param, samplerMaker.getName(), maxWaitingTimeForTakingDesiredSamples);
//                statInfo.persistGelmanRubinDiagnostics(outputDirectoryPath, param, samplerMaker.getName());
                    multiMCMCChainAnalysis.persistNumTakenSamplesForTimePoints(outputDirectoryPath, param, samplerMaker.getName());
//                multiMCMCChainAnalysis.persistAutoCorrelationForFirstTakenErrSamples(outputDirectoryPath, param, samplerMaker.getName(), numMinDesiredSamples);

//                if (statInfo.timeToTakeFirstSamplesOrGoldenTime != null) { //did not die        //todo!!!!!!!!!!!!!!!!!!!!
//                    timeKeeper.persist(param, samplerMaker.getName(), statInfo.timeToTakeFirstSamplesOrGoldenTime);
//                } else { //died
//                    throw new RuntimeException("died");
//                    testedAlgsDeathKeeper.recordDeath(samplerMaker, param); //todo uncomment if necessary
//                }

                    MultiArrayMultiStatistics timeToPassGoldenErrStat = multiMCMCChainAnalysis.getTimeToPassGoldenErrStat();
                    if (timeToPassGoldenErrStat != null) {
                        timeToPassGoldErrKeeper.persist(samplerMaker.getName(), "toPassErrThr", param,
                                timeToPassGoldenErrStat.computeMean().get(0),
                                timeToPassGoldenErrStat.computeCorrectedStdErr().get(0)); //we know that if it is not null it contains exactly one entry
                    }

                    MultiArrayMultiStatistics timeToTakeGoldenSamplesStat = multiMCMCChainAnalysis.getTimeToTakeGoldenSamplesStat();
                    if (timeToTakeGoldenSamplesStat != null) {
                        timeToTakeGoldSamplesKeeper.persist(samplerMaker.getName(), "toTake100samples", param,
                                timeToTakeGoldenSamplesStat.computeMean().get(0),
                                timeToTakeGoldenSamplesStat.computeCorrectedStdErr().get(0)); //we know that if it is not null it contains exactly one entry
                    }



//                    System.out.println(samplerMaker.getName() + ".timeN/GOLD = " + multiMCMCChainAnalysis.averageTimeToAccomplishOrGolden*//*totalProcessTimeMillis*//* + "\t\tsamples=" + multiMCMCChainAnalysis.numberOfFirstSamples());//.means4FirstSamples.size());

//                } else {
//                    System.err.println(samplerMaker.getName() + " skipped...");
//                }
                } catch (SamplingFailureException e) {
                    e.printStackTrace();
                    System.err.println("Sampling using " + samplerMaker.getName() + " with param " + param + " terminated UNSUCCESSFULLY without saving any info.");
                }
            }  //end sampler
//                FunctionVisualizer.visualize(stdErr, 0, numSamples, 1, statType + " #dim:" + numDims + " #cnstrnt:" + numConstraints);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private class GoldenTimesKeeper {
        String outputDirectoryPath;
        Map<String *//*alg.*//*, Map<Integer *//*param*//*, Double *//*gold*//*>> alg2paramGoldMeanMap;
        Map<String *//*alg.*//*, Map<Integer *//*param*//*, Double *//*gold*//*>> alg2paramGoldStdErrMap;

        public GoldenTimesKeeper(List<JointToSampler> samplers, String outputDirectoryPath) {
            this.outputDirectoryPath = outputDirectoryPath;

            alg2paramGoldMeanMap = new HashMap<String, Map<Integer, Double>>(samplers.size());
            alg2paramGoldStdErrMap = new HashMap<String, Map<Integer, Double>>(samplers.size());

            for (JointToSampler sampler : samplers) {
                alg2paramGoldMeanMap.put(sampler.getName(), new HashMap<Integer, Double>());
                alg2paramGoldStdErrMap.put(sampler.getName(), new HashMap<Integer, Double>());
            }
        }

        public void persist(String samplerName, String fileSuffix, Integer param, Double goldMean, Double goldStdErr) throws FileNotFoundException {
            Map<Integer, Double> paramToMeanMap = alg2paramGoldMeanMap.get(samplerName);
            Map<Integer, Double> paramToStdErrMap = alg2paramGoldStdErrMap.get(samplerName);

            if (paramToMeanMap.put(param, goldMean)!= null) throw new RuntimeException("double entry for param " + param);
            if (paramToStdErrMap.put(param, goldStdErr)!= null) throw new RuntimeException("double entry for param " + param);

            persistInner(samplerName, fileSuffix, paramToMeanMap, paramToStdErrMap);
        }

        private void persistInner(String samplerName, String fileSuffix, Map<Integer, Double> param2Means, Map<Integer, Double> param2StdErrs) throws FileNotFoundException {
            String outputFileName = this.outputDirectoryPath + samplerName + "-" + fileSuffix;

            PrintStream ps = new PrintStream(new FileOutputStream(outputFileName));

            SortedSet<Integer> params = new TreeSet<Integer>(param2Means.keySet());
            int i=1;
            for (Integer param : params) {
                ps.println((i++) + "\t" + param + "\t" + param2Means.get(param) + "\t" + param2StdErrs.get(param));
            }

            ps.close();
        }

    }

  *//*  private class TotalTimeKeeper {
        String outputDirectoryPath;
        Map<String*//**//*algorithm*//**//*, SortedSet<Pair<Integer *//**//*dims*//**//*, Long *//**//*totalTime*//**//*>>> alg2dimsTime;
//        Map<String*//**//*algorithm*//**//*, SortedSet<Pair<Integer *//**//*data*//**//*, Long *//**//*totalTime*//**//*>>> alg2dataTime;

        public TotalTimeKeeper(JointToSampler[] samplers, String outputDirectoryPath) {
            this.outputDirectoryPath = outputDirectoryPath;

            alg2dimsTime = new HashMap<String, SortedSet<Pair<Integer, Long>>>(samplers.length);
//            alg2dataTime = new HashMap<String, SortedSet<Pair<Integer, Long>>>(samplers.length);

//            for (JointToSampler sampler : samplers) {
//                alg2dataTime.put(sampler.getName(), new TreeSet<Pair<Integer, Long>>());
//            }
        }

        int[] dimsArray;
        //        int[] dataArray;
        String[] samplerNames;
        Map<String *//**//*alg*//**//*, Long*//**//*time*//**//*>[] dimIndexDataIndexAlgTime;

        public TotalTimeKeeper(List<JointToSampler> samplers, int[] dimsArray*//**//*, int[] dataArray*//**//*, String outputDirectoryPath) {
            this.outputDirectoryPath = outputDirectoryPath;

            this.dimsArray = new int[dimsArray.length];
//            this.dataArray = new int[dataArray.length];
            System.arraycopy(dimsArray, 0, this.dimsArray, 0, dimsArray.length);
//            System.arraycopy(dataArray, 0, this.dataArray, 0, dataArray.length);
            Arrays.sort(this.dimsArray);
//            Arrays.sort(this.dataArray);

            dimIndexDataIndexAlgTime = new HashMap[dimsArray.length];//[dataArray.length];

            this.samplerNames = new String[samplers.size()];
            for (int i = 0; i < samplers.size(); i++) {
                this.samplerNames[i] = samplers.get(i).getName();
            }

            for (int i = 0; i < dimsArray.length; i++) {
//                for (int j = 0; j < dataArray.length; j++) {
                dimIndexDataIndexAlgTime[i] = new HashMap<String, Long>(samplers.size());
//                }
            }
        }

        public void persist(int param, String samplerName, Long timeMillis) throws FileNotFoundException {
            int dimIndex = Arrays.binarySearch(dimsArray, param);
//            int dataIndex = Arrays.binarySearch(dataArray, data);
            if (dimIndexDataIndexAlgTime[dimIndex].put(samplerName, timeMillis) != null) {
                System.err.println("for dim: " + param + ", " + samplerName + " already exists! and will be replaced");
                System.out.println("dimsArray = " + Arrays.toString(dimsArray));
//                System.out.println("dataArray = " + Arrays.toString(dataArray));
            }
//            persistDimFix(samplerName, dimIndex);
            persistDataFix(samplerName*//**//*, dataIndex*//**//*);
        }

//        private void persistDimFix(String samplerName, int dimIndex) throws FileNotFoundException {
//            int dim = dimsArray[dimIndex];
//
//            List<Integer> dataList = new ArrayList<Integer>();
//            List<Long> timeList = new ArrayList<Long>();
//            for (int dataIndex = 0; dataIndex < dataArray.length; dataIndex++) {
//                Long time = dimIndexDataIndexAlgTime[dimIndex][dataIndex].get(samplerName);
//                if (time == null) continue;
//                int data = dataArray[dataIndex];
//                dataList.add(data);
//                timeList.add(time);
//            }
//            persistOneEntryFixed(samplerName, "dim", dim, dataList, timeList);
//        }

        private void persistDataFix(String samplerName*//**//*, int dataIndex*//**//*) throws FileNotFoundException {
//            int data = dataArray[dataIndex];

            List<Integer> dimList = new ArrayList<Integer>();
            List<Long> timeList = new ArrayList<Long>();
            for (int dimIndex = 0; dimIndex < dimsArray.length; dimIndex++) {
                Long time = dimIndexDataIndexAlgTime[dimIndex].get(samplerName);
                if (time == null) continue;
                int dim = dimsArray[dimIndex];
                dimList.add(dim);
                timeList.add(time);
            }
            persistInner(samplerName, dimList, timeList);
        }

        private void persistInner(String samplerName, List<Integer> elementList, List<Long> timeList) throws FileNotFoundException {
            String outputFileName = this.outputDirectoryPath + "-" + samplerName;

            PrintStream ps = new PrintStream(new FileOutputStream(outputFileName));

            int n = elementList.size();
            if (n != timeList.size()) throw new RuntimeException("size mismatch");
            for (int i = 0; i < n; i++) {
                ps.println(elementList.get(i) + "\t" + timeList.get(i));
            }

            ps.close();
        }

    }*//*

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public double[] computeGroundTruthMean(
            JointWrapper jointWrapper, JointToSampler samplerMaker,
            final int numDesiredSamples,
            long maxWaitingTimeForTakingDesiredSamplesMillis //in ms
    ) {
        double[] mean = null;
        int numTakenSamples = 0;

        ///////////////////////////////////////////////////////////////////////
        SamplerInterface sampler = samplerMaker.makeSampler(jointWrapper);//(jointWrapper.joint, jointWrapper.minVarLimit, jointWrapper.maxVarLimit);
        ///////////////////////////////////////////////////////////////////////

        long t1 = System.currentTimeMillis();
        for (int sampleCount = 0; sampleCount < numDesiredSamples; sampleCount++) {
            Double[] sample = sampler.reusableSample();
            sample = SingleMCMCChainAnalysis.pruneNulls(sample);

            if (mean == null) {
                mean = new double[sample.length];
            }

            for (int j = 0; j < mean.length; j++) {
                mean[j] = mean[j] + sample[j];
            }
            numTakenSamples++;
            if (System.currentTimeMillis() - t1 > maxWaitingTimeForTakingDesiredSamplesMillis) break;
        }

        System.out.println("num. Ground. Truth. TakenSamples = " + numTakenSamples + "\t in time <= " + maxWaitingTimeForTakingDesiredSamplesMillis + "(ms)");

        for (int j = 0; j < mean.length; j++) {
            mean[j] = mean[j] / (double) numTakenSamples;
        }

        return mean;
    }



    private interface Param2JointWrapper {
        JointWrapper makeJointWrapper(int param);
    }*/
}

