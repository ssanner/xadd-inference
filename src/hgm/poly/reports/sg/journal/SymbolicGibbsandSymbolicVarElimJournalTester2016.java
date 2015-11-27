package hgm.poly.reports.sg.journal;

import hgm.asve.Pair;
import hgm.poly.Fraction;
import hgm.poly.PiecewiseExpression;
import hgm.poly.bayesian.AbstractGeneralBayesianGibbsSampler;
import hgm.poly.diagnostics.AbsDifferenceMeasure;
import hgm.poly.diagnostics.MeasureOnTheRun;
import hgm.poly.diagnostics.MultiArrayMultiStatistics;
import hgm.poly.gm.*;
import hgm.poly.reports.sg.*;
import hgm.poly.reports.sg.external.anglican.AnglicanCodeGenerator;
import hgm.poly.reports.sg.external.anglican.AnglicanJointToSampler;
import hgm.poly.reports.sg.external.stan.StanInputDataGenerator;
import hgm.poly.reports.sg.external.stan.StanJointToSampler;
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
public class SymbolicGibbsandSymbolicVarElimJournalTester2016 {

    public static final String REPORT_PATH_COLLISION_ANALYSIS = "E:/REPORT_PATH_ICML15/collision/";
    public static final String REPORT_PATH_FERMENTATION_ANALYSIS = "E:/REPORT_PATH_ICML15/fermentation/";
    public static final String REPORT_PATH_CIRCUITS_ANALYSIS = "E:/REPORT_PATH_ICML15/circuits/";
    public static final String REPORT_PATH_CONDUCTANCE_ANALYSIS = "E:/REPORT_PATH_ICML15/conductance/";
    public static final String REPORT_PATH_VISUAL_COLLISION_ANALYSIS = "E:/REPORT_PATH_ICML15/visual_collision/";

    public static final String REPORT_PATH_ROBOT = "C:/REPORTS/robot/";
    public static final String REPORT_PATH_CRF = "C:/REPORTS/crf/";
    public static final String REPORT_PATH_DBN = "C:/REPORTS/dbn/";

    public static void main(String[] args) throws IOException {
        SymbolicGibbsandSymbolicVarElimJournalTester2016 instance = new SymbolicGibbsandSymbolicVarElimJournalTester2016();
//        instance.collisionICML2015Test(true, REPORT_PATH_COLLISION_ANALYSIS);
//        instance.fermentationICML2015Test(REPORT_PATH_FERMENTATION_ANALYSIS);
//        instance.circuitAAAI2015Test();
//        instance.conductanceICML2015Test(REPORT_PATH_CONDUCTANCE_ANALYSIS);
//        instance.robotLocalizationJournalTest(REPORT_PATH_ROBOT); //never worked more than size 3!
//        instance.visualCollisionICML2015Test(true, REPORT_PATH_VISUAL_COLLISION_ANALYSIS);
//        instance.crfJournalTest(REPORT_PATH_CRF);
        instance.dbnJournalTest(REPORT_PATH_DBN);
    }

    //___________________________________________________________________________


    public void dbnJournalTest(String reportPath) throws IOException {
        System.out.println("REPORT_PATH_DBN_ANALYSIS = " + reportPath);

        final Double locationLowerBound = 0.0;
        final Double locationUpperBound = 10.0;

        int[] numParams = {3};//{3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30};//{2, 3};
        int numMinDesiredSamples = 100;//1000; //100;
        int maxWaitingTimeForTakingDesiredSamples = 1000 * 60 * 3;//1000 * 60 * 2;//1000*60*5;//1000 * 20;
        int minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis = 1000 * 60*1 * 3;//1000 * 5;//1000*60;//1000 * 5;
        int approxNumTimePointsForWhichErrIsPersisted = 100;//33;
        int numRuns = 1;//10;//20;//2;
        int burnedSamples = 10;//100;//50;
        double goldenErrThreshold = 0.1;//0.045;//0.02;////0.2;


        List<JointToSampler> samplerMakersToBeTested = new ArrayList<JointToSampler>();
//        samplerMakersToBeTested.add(new EliminatedVarCompleterSamplerMaker(FractionalJointMetropolisHastingSampler.makeJointToSampler(0.1)));
        samplerMakersToBeTested.add(new DifferenceSampler(
                FractionalJointSymbolicGibbsSampler.makeJointToSampler(),
                FractionalJointSymbolicGibbsSampler.makeJointToSampler()
                ));
        samplerMakersToBeTested.add(new DifferenceSampler(
                FractionalJointBaselineGibbsSampler.makeJointToSampler(),
                FractionalJointBaselineGibbsSampler.makeJointToSampler()
                )); //...
        samplerMakersToBeTested.add(new DifferenceSampler(
                FractionalJointSelfTunedMetropolisHastingSampler.makeJointToSampler(0.1, 200, 100),
                FractionalJointSelfTunedMetropolisHastingSampler.makeJointToSampler(0.1, 200, 100)
                ));
        samplerMakersToBeTested.add(new DifferenceSampler(
                FractionalJointHMC.makeJointToSampler(0.1, 20),
                FractionalJointHMC.makeJointToSampler(0.1, 20)
                ));
        /*JointToSampler cleverPramDependentRejectionSampler = new JointToSampler() {
            @Override
            public SamplerInterface makeSampler(JointWrapper jointWrapper) {
                SortedSet<String> scopeVars = jointWrapper.getJoint().getScopeVars();
                int param = scopeVars.size();
                return FractionalJointRejectionSampler.makeJointToSampler(Math.pow(0.9, param*param)*//*Math.pow(0.45, param) * 0.1*//*).makeSampler(jointWrapper); //just because I need param to make envelop
            }

            @Override
            public String getName() {
                return "rej";
            }
        };
        samplerMakersToBeTested.add(new EliminatedVarCompleterSamplerMaker(cleverPramDependentRejectionSampler));
*/

//                FractionalJointRejectionSampler.makeJointToSampler(Math.pow(locationUpperBound, 1.0 * max(numParams)))));
//        samplerMakersToBeTested.add(new AnglicanJointToSampler(10000, 0.1, AnglicanCodeGenerator.AnglicanSamplingMethod.rdb));
//        samplerMakersToBeTested.add(new AnglicanJointToSampler(10000, 0.1, AnglicanCodeGenerator.AnglicanSamplingMethod.smc));
//        samplerMakersToBeTested.add(new AnglicanJointToSampler(10000, 0.1, AnglicanCodeGenerator.AnglicanSamplingMethod.pgibbs));
//        samplerMakersToBeTested.add(new StanJointToSampler(0.02));

        Param2JointWrapper modelParam2Joint = new Param2JointWrapper() {

            @Override
            public JointWrapper makeJointWrapper(int param) {

                //square lattice
                System.out.println("param = " + param);
                TrackingDynamicBayesianNetwork model = new TrackingDynamicBayesianNetwork(param);


                // 1. some evidence and some query....
//                SymbolicGraphicalModelHandler handler = new SymbolicGraphicalModelHandler();

                //NOTE: var indexes starts from 1 here:
                Map<String, Double> evidence = new HashMap<String, Double>();
                evidence.put("o_"+param, 3.0); //todo what should be the evidence?

                //todo all opposite nodes that are different (not repeated) and not evidence...
                //because evidence is symmetric, so the expected value fo the symmetric queries should converge to a same value.
                List<String> query = new ArrayList<String>();
                for (int i=0; i<param; i++){
                    query.add("x_"+ (i+1));
                }

                System.out.println("evidence = " + evidence);
                System.out.println("query = " + query);


                //------------  M A K E   J O I N T  ---------------
                MultiFactorJoint joint = model.makeJoint(query, evidence);
                //---------------------------------------------------------------------------------------

                double envelopeSafeZone = 0;//0.001;
                RichJointWrapper jointWrapper =
                        new RichJointWrapper(joint, new ArrayList<DeterministicFactor>(),
                                query, locationLowerBound - envelopeSafeZone, locationUpperBound + envelopeSafeZone,
                                model, evidence);
                System.out.println("jointWrapper.getAppropriateSampleVectorSize() = " + jointWrapper.getAppropriateSampleVectorSize());
                System.out.println("jointWrapper.getJoint().getScopeVars() = " + jointWrapper.getJoint().getScopeVars());
//                System.out.println("jointWrapper.getJoint() = " + jointWrapper.getJoint());

                //Anglican code:
//                String anglicanCode = AnglicanCodeGenerator.makeAnglicanResistorModel(param, locationLowerBound, locationUpperBound, evidence, null , query);
//                jointWrapper.addExtraInfo(AnglicanCodeGenerator.ANGLICAN_CODE_KEY, anglicanCode);
                // Stan code:
//                String stanInput = StanInputDataGenerator.makeStanResistorInput(param, locationLowerBound, locationUpperBound, evidence);
//                jointWrapper.addExtraInfo(StanJointToSampler.STAN_INPUT_CONTENT_KEY, stanInput);
//                jointWrapper.addExtraInfo(StanJointToSampler.STAN_MODEL_FILE_KEY, StanJointToSampler.STAN_RESISTOR_MODEL);


                return jointWrapper;
            }
        };


        testSamplersPerformanceWrtParameterTimeAndSampleCount(modelParam2Joint,
                new DifferenceFromTrueMeanVectorMeasureGenerator(0.0),
                samplerMakersToBeTested, numParams, numMinDesiredSamples,
                maxWaitingTimeForTakingDesiredSamples,
                minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis,
                approxNumTimePointsForWhichErrIsPersisted, numRuns,
                burnedSamples, reportPath, goldenErrThreshold);

        System.out.println(" That was all the folk for DBN problem with params " + Arrays.toString(numParams) + ".");


    }


    //___________________________________________________________________________

    public void crfJournalTest(String reportPath) throws IOException {
        System.out.println("REPORT_PATH_ROBOT_CRF_ANALYSIS = " + reportPath);

        //todo

        //tester is commented since symmetry is used.
        //The following 3 values are used only used if the Ground truth values are not known:
//        final JointToSampler testerSampleMaker = new EliminatedVarCompleterSamplerMaker(FractionalJointRejectionSampler.makeJointToSampler(1.0));
//        final int numSamplesFromTesterToSimulateTrueDistribution = 170000;//100000;
//        final int maxWaitingTimeForTesterToSimulateMillis = 1000 * 60 * 10;//1000 * 60 * 10;

        final Double locationLowerBound = 0.0;
        final Double locationUpperBound = 10.0;
//        final Double rMean = ???(w1 * locationUpperBound + w2 * locationLowerBound) / (w1 + w2);

        int[] numParams = {3003};//{3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30};//{2, 3};
        int numMinDesiredSamples = 100;//1000; //100;
        int maxWaitingTimeForTakingDesiredSamples = 1000 * 60 * 3;//1000 * 60 * 2;//1000*60*5;//1000 * 20;
        int minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis = 1000 * 60*1 * 3;//1000 * 5;//1000*60;//1000 * 5;
        int approxNumTimePointsForWhichErrIsPersisted = 100;//33;
        int numRuns = 1;//10;//20;//2;
        int burnedSamples = 10;//100;//50;
        double goldenErrThreshold = 0.1;//0.045;//0.02;////0.2;


        List<JointToSampler> samplerMakersToBeTested = new ArrayList<JointToSampler>();
//        samplerMakersToBeTested.add(new EliminatedVarCompleterSamplerMaker(FractionalJointMetropolisHastingSampler.makeJointToSampler(0.1)));
        samplerMakersToBeTested.add((FractionalJointSymbolicGibbsSampler.makeJointToSampler()));
        samplerMakersToBeTested.add((FractionalJointBaselineGibbsSampler.makeJointToSampler())); //...
        samplerMakersToBeTested.add((FractionalJointSelfTunedMetropolisHastingSampler.makeJointToSampler(0.1, 200, 100)));
        samplerMakersToBeTested.add((FractionalJointHMC.makeJointToSampler(0.1, 20)));
        /*JointToSampler cleverPramDependentRejectionSampler = new JointToSampler() {
            @Override
            public SamplerInterface makeSampler(JointWrapper jointWrapper) {
                SortedSet<String> scopeVars = jointWrapper.getJoint().getScopeVars();
                int param = scopeVars.size();
                return FractionalJointRejectionSampler.makeJointToSampler(Math.pow(0.9, param*param)*//*Math.pow(0.45, param) * 0.1*//*).makeSampler(jointWrapper); //just because I need param to make envelop
            }

            @Override
            public String getName() {
                return "rej";
            }
        };
        samplerMakersToBeTested.add(new EliminatedVarCompleterSamplerMaker(cleverPramDependentRejectionSampler));
*/

//                FractionalJointRejectionSampler.makeJointToSampler(Math.pow(locationUpperBound, 1.0 * max(numParams)))));
//        samplerMakersToBeTested.add(new AnglicanJointToSampler(10000, 0.1, AnglicanCodeGenerator.AnglicanSamplingMethod.rdb));
//        samplerMakersToBeTested.add(new AnglicanJointToSampler(10000, 0.1, AnglicanCodeGenerator.AnglicanSamplingMethod.smc));
//        samplerMakersToBeTested.add(new AnglicanJointToSampler(10000, 0.1, AnglicanCodeGenerator.AnglicanSamplingMethod.pgibbs));
//        samplerMakersToBeTested.add(new StanJointToSampler(0.02));

        Param2JointWrapper modelParam2Joint = new Param2JointWrapper() {

            @Override
            public JointWrapper makeJointWrapper(int param) {

                //square lattice
                int nX = param / 1000;
                int nY = param % 1000;
                System.out.println("[nX = " + nX + "\t nY = " + nY + "]");
                ConditionalRandomFieldModel model = new ConditionalRandomFieldModel(nX, nY);


                // 1. some evidence and some query....
//                SymbolicGraphicalModelHandler handler = new SymbolicGraphicalModelHandler();
                Map<String, Double> evidence = new HashMap<String, Double>();
                //randomly observed half of the nodes:
                int numAllNodes = model.getNumCRFieldNodes();
                int numEvidencePairs = numAllNodes / 4; //so half of the nodes would be evidenced
                List<Integer> remainedNodeIds = new ArrayList<Integer>(numAllNodes);
                for (int i = 0; i < numAllNodes; i++) {
                    remainedNodeIds.add(i);
                }
                for (int j = 0; j < numEvidencePairs; j++) {
                    Integer aRandomNodeId = remainedNodeIds.get(AbstractGeneralBayesianGibbsSampler.getRandom().nextInt(remainedNodeIds.size()));
                    double evidenceValue = AbstractGeneralBayesianGibbsSampler.randomDoubleUniformBetween(0, 10);
                    evidence.put("y_" + aRandomNodeId, evidenceValue);  //y_i are the observation nodes corresponding hidden x_i, though they are not in the factory due to implementation...
                    remainedNodeIds.remove(aRandomNodeId);

                    Integer otherNodeId = model.oppositeNodeId(aRandomNodeId); //symmetric evidence
                    if (!otherNodeId.equals(aRandomNodeId)) {
                        remainedNodeIds.remove(otherNodeId);
                        evidence.put("y_" + otherNodeId, evidenceValue);
                    }

                    //now remove null elements:


                }
                //todo all opposite nodes that are different (not repeated) and not evidence...
                //because evidence is symmetric, so the expected value fo the symmetric queries should converge to a same value.
                List<String> query = new ArrayList<String>();

                remainedNodeIds = new ArrayList<Integer>();//empty/refill
                for (int i = 0; i < numAllNodes; i++) {
                    remainedNodeIds.add(i);
                }
                Set<Integer> alreadyProcessedIds = new HashSet<Integer>(); //already sorted out..
                for (Integer id : remainedNodeIds) {
                    if (!alreadyProcessedIds.contains(id)) {
                        Integer oppositeId = model.oppositeNodeId(id);

                        alreadyProcessedIds.add(id);
                        alreadyProcessedIds.add(oppositeId);

                        if (!id.equals(oppositeId)){
                            query.add(model.varName(id));
                            query.add(model.varName(oppositeId));
                        }
                    }
                }



//                query.add("x_0");
//                query.add("x_" + model.oppositeNodeId(0));

//                evidence = new HashMap<String, Double>();  System.err.println("EVIDENCE removed for DEBUG!!!");
                System.out.println("evidence = " + evidence);
                System.out.println("query = " + query);


                MultiFactorJoint joint = model.makeJoint(query, evidence);

//                System.out.println("_________________________________________");
//                System.out.println("joint = " + joint);
//                System.out.println("_________________________________________");

                double envelopeSafeZone = 0;//0.001;
                RichJointWrapper jointWrapper =
                        new RichJointWrapper(joint, new ArrayList<DeterministicFactor>(),
                                query, locationLowerBound - envelopeSafeZone, locationUpperBound + envelopeSafeZone,
                                model, evidence);
                System.out.println("jointWrapper.getAppropriateSampleVectorSize() = " + jointWrapper.getAppropriateSampleVectorSize());
                System.out.println("jointWrapper.getJoint().getScopeVars() = " + jointWrapper.getJoint().getScopeVars());
//                System.out.println("jointWrapper.getJoint() = " + jointWrapper.getJoint());

                //Anglican code:
//                String anglicanCode = AnglicanCodeGenerator.makeAnglicanResistorModel(param, locationLowerBound, locationUpperBound, evidence, null , query);
//                jointWrapper.addExtraInfo(AnglicanCodeGenerator.ANGLICAN_CODE_KEY, anglicanCode);
                // Stan code:
//                String stanInput = StanInputDataGenerator.makeStanResistorInput(param, locationLowerBound, locationUpperBound, evidence);
//                jointWrapper.addExtraInfo(StanJointToSampler.STAN_INPUT_CONTENT_KEY, stanInput);
//                jointWrapper.addExtraInfo(StanJointToSampler.STAN_MODEL_FILE_KEY, StanJointToSampler.STAN_RESISTOR_MODEL);


                return jointWrapper;
            }
        };


        testSamplersPerformanceWrtParameterTimeAndSampleCount(modelParam2Joint,
                new QueryPairsEqualMeasureGenerator(),
                samplerMakersToBeTested, numParams, numMinDesiredSamples,
                maxWaitingTimeForTakingDesiredSamples,
                minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis,
                approxNumTimePointsForWhichErrIsPersisted, numRuns,
                burnedSamples, reportPath, goldenErrThreshold);

        System.out.println(" That was all the folk for CRF problem with params " + Arrays.toString(numParams) + ".");


    }

//___________________________________________________________________________



    public void robotLocalizationJournalTest(String reportPath) throws IOException {
        System.out.println("REPORT_PATH_ROBOT_LOCALIZATION_ANALYSIS = " + reportPath);

        //todo

        //tester is commented since symmetry is used.
        //The following 3 values are used only used if the Ground truth values are not known:
//        final JointToSampler testerSampleMaker = new EliminatedVarCompleterSamplerMaker(FractionalJointRejectionSampler.makeJointToSampler(1.0));
//        final int numSamplesFromTesterToSimulateTrueDistribution = 170000;//100000;
//        final int maxWaitingTimeForTesterToSimulateMillis = 1000 * 60 * 10;//1000 * 60 * 10;

        final Double locationLowerBound = 0.0;
        final Double locationUpperBound = 10.0;
//        final Double rMean = ???(w1 * locationUpperBound + w2 * locationLowerBound) / (w1 + w2);

        int[] numParams = {3, 4, 5};//{3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30};//{2, 3};
        int numMinDesiredSamples = 200;//1000; //100;
        int maxWaitingTimeForTakingDesiredSamples = 1000 * 2;//1000 * 60 * 2;//1000*60*5;//1000 * 20;
        int minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis = 1000 * 2;//1000 * 5;//1000*60;//1000 * 5;
        int approxNumTimePointsForWhichErrIsPersisted = 100;//33;
        int numRuns = 1;//10;//20;//2;
        int burnedSamples = 100;//100;//50;
        double goldenErrThreshold = 0.045;//0.02;////0.2;


        List<JointToSampler> samplerMakersToBeTested = new ArrayList<JointToSampler>();
//        samplerMakersToBeTested.add(new EliminatedVarCompleterSamplerMaker(FractionalJointMetropolisHastingSampler.makeJointToSampler(0.1)));
        samplerMakersToBeTested.add(new EliminatedVarCompleterSamplerMaker(FractionalJointSelfTunedMetropolisHastingSampler.makeJointToSampler(0.1, 200, 100)));
        samplerMakersToBeTested.add(new EliminatedVarCompleterSamplerMaker(FractionalJointHMC.makeJointToSampler(0.1, 20)));
        samplerMakersToBeTested.add(new EliminatedVarCompleterSamplerMaker(FractionalJointBaselineGibbsSampler.makeJointToSampler())); //...
        samplerMakersToBeTested.add(new EliminatedVarCompleterSamplerMaker(FractionalJointSymbolicGibbsSampler.makeJointToSampler()));
        JointToSampler cleverPramDependentRejectionSampler = new JointToSampler() {
            @Override
            public SamplerInterface makeSampler(JointWrapper jointWrapper) {
                SortedSet<String> scopeVars = jointWrapper.getJoint().getScopeVars();
                int param = scopeVars.size();
                return FractionalJointRejectionSampler.makeJointToSampler(Math.pow(0.45, param) * 0.1).makeSampler(jointWrapper); //just because I need param to make envelop
            }

            @Override
            public String getName() {
                return "rej";
            }
        };
        samplerMakersToBeTested.add(new EliminatedVarCompleterSamplerMaker(cleverPramDependentRejectionSampler));


//                FractionalJointRejectionSampler.makeJointToSampler(Math.pow(locationUpperBound, 1.0 * max(numParams)))));
//        samplerMakersToBeTested.add(new AnglicanJointToSampler(10000, 0.1, AnglicanCodeGenerator.AnglicanSamplingMethod.rdb));
//        samplerMakersToBeTested.add(new AnglicanJointToSampler(10000, 0.1, AnglicanCodeGenerator.AnglicanSamplingMethod.smc));
//        samplerMakersToBeTested.add(new AnglicanJointToSampler(10000, 0.1, AnglicanCodeGenerator.AnglicanSamplingMethod.pgibbs));
//        samplerMakersToBeTested.add(new StanJointToSampler(0.02));

        Param2JointWrapper modelParam2Joint = new Param2JointWrapper() {

            @Override
            public JointWrapper makeJointWrapper(int param) {

                GraphicalModel bn = new RobotLocalizationModel(param);//todo

                SymbolicGraphicalModelHandler handler = new SymbolicGraphicalModelHandler();
                Map<String, Double> evidence = new HashMap<String, Double>();

                //localization evidence: randomly choose evidence between 0 and 10. TODO is this a good strategy?
                for (int p = 0; p < param; p++) {
                    evidence.put("x_" + p, AbstractGeneralBayesianGibbsSampler.randomDoubleUniformBetween(0, 10));
                }

                List<String> query = new ArrayList<String>();
                for (int i = 0; i < param; i++) {
                    query.add("d"); //todo is this good?
                }

                Pair<PiecewiseExpression<Fraction>, List<DeterministicFactor>> jointAndEliminatedStochasticVars =
                        handler.makeJointAndEliminatedStochasticVars(bn, query, evidence);
                PiecewiseExpression<Fraction> joint = jointAndEliminatedStochasticVars.getFirstEntry();
                List<DeterministicFactor> eliminatedStochasticVars = jointAndEliminatedStochasticVars.getSecondEntry();
                if (!eliminatedStochasticVars.isEmpty()) throw new RuntimeException("how possible?");
                double envelopeSafeZone = 0;//0.001;
                RichJointWrapper jointWrapper =
                        new RichJointWrapper(joint, eliminatedStochasticVars, query, locationLowerBound - envelopeSafeZone, locationUpperBound + envelopeSafeZone, bn, evidence);
                System.out.println("jointWrapper.getAppropriateSampleVectorSize() = " + jointWrapper.getAppropriateSampleVectorSize());
                System.out.println("jointWrapper.getJoint().getScopeVars() = " + jointWrapper.getJoint().getScopeVars());
                System.out.println("jointWrapper.getJoint() = " + jointWrapper.getJoint());

                //Anglican code:
//                String anglicanCode = AnglicanCodeGenerator.makeAnglicanResistorModel(param, locationLowerBound, locationUpperBound, evidence, null , query);
//                jointWrapper.addExtraInfo(AnglicanCodeGenerator.ANGLICAN_CODE_KEY, anglicanCode);
                // Stan code:
//                String stanInput = StanInputDataGenerator.makeStanResistorInput(param, locationLowerBound, locationUpperBound, evidence);
//                jointWrapper.addExtraInfo(StanJointToSampler.STAN_INPUT_CONTENT_KEY, stanInput);
//                jointWrapper.addExtraInfo(StanJointToSampler.STAN_MODEL_FILE_KEY, StanJointToSampler.STAN_RESISTOR_MODEL);


                return jointWrapper;
            }
        };


        testSamplersPerformanceWrtParameterTimeAndSampleCount(modelParam2Joint,
                new DifferenceFromTrueMeanVectorMeasureGenerator(cleverPramDependentRejectionSampler/*FractionalJointRejectionSampler.makeJointToSampler(2)*/, 10000, 100000), // todo if I compute the rMean by SVE then a vector should be passed . . .
                samplerMakersToBeTested, numParams, numMinDesiredSamples,
                maxWaitingTimeForTakingDesiredSamples,
                minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis,
                approxNumTimePointsForWhichErrIsPersisted, numRuns,
                burnedSamples, reportPath, goldenErrThreshold);

        System.out.println(" That was all the folk for conductance circuit problem with params " + Arrays.toString(numParams) + ".");


    }


    //___________________________________________________________________________


    public void conductanceICML2015Test(String reportPath) throws IOException {
        System.out.println("REPORT_PATH_CONDUCTANCE_ANALYSIS = " + reportPath);

        //tester is commented since symmetry is used.
        //The following 3 values are used only used if the Ground truth values are not known:
//        final JointToSampler testerSampleMaker = new EliminatedVarCompleterSamplerMaker(FractionalJointRejectionSampler.makeJointToSampler(1.0));
//        final int numSamplesFromTesterToSimulateTrueDistribution = 170000;//100000;
//        final int maxWaitingTimeForTesterToSimulateMillis = 1000 * 60 * 10;//1000 * 60 * 10;

        final Double resistorLowerBound = 9.5;
        final Double resistorUpperBound = 10.5;
        double w1 = 2.0;
        double w2 = 1.0;
        final Double rMean = (w1 * resistorUpperBound + w2 * resistorLowerBound) / (w1 + w2);

        int[] numParams = {2, 3, 4, 5, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 32, 34};//{3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30};//{2, 3};
        int numMinDesiredSamples = 200;//1000; //100;
        int maxWaitingTimeForTakingDesiredSamples = 1000 * 2;//1000 * 60 * 2;//1000*60*5;//1000 * 20;
        int minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis = 1000 * 2;//1000 * 5;//1000*60;//1000 * 5;
        int approxNumTimePointsForWhichErrIsPersisted = 100;//33;
        int numRuns = 15;//10;//20;//2;
        int burnedSamples = 200;//100;//50;
        double goldenErrThreshold = 0.045;//0.02;////0.2;


        List<JointToSampler> samplerMakersToBeTested = new ArrayList<JointToSampler>();
        samplerMakersToBeTested.add(new EliminatedVarCompleterSamplerMaker(FractionalJointSymbolicGibbsSampler.makeJointToSampler()));
        samplerMakersToBeTested.add(new EliminatedVarCompleterSamplerMaker(FractionalJointBaselineGibbsSampler.makeJointToSampler())); //...
//        samplerMakersToBeTested.add(new EliminatedVarCompleterSamplerMaker(FractionalJointMetropolisHastingSampler.makeJointToSampler(0.1)));
        samplerMakersToBeTested.add(new EliminatedVarCompleterSamplerMaker(FractionalJointSelfTunedMetropolisHastingSampler.makeJointToSampler(0.1, 200, 100)));
        samplerMakersToBeTested.add(new EliminatedVarCompleterSamplerMaker(
                new JointToSampler() {
                    @Override
                    public SamplerInterface makeSampler(JointWrapper jointWrapper) {
                        SortedSet<String> scopeVars = jointWrapper.getJoint().getScopeVars();
//                        System.out.println("scopeVars = " + scopeVars);
                        int param = scopeVars.size();
                        return FractionalJointRejectionSampler.makeJointToSampler(Math.pow(resistorUpperBound, 2.0 * param + 2)).makeSampler(jointWrapper); //just because I need param to make envelop
                    }

                    @Override
                    public String getName() {
                        return "rej";
                    }
                }));
//                FractionalJointRejectionSampler.makeJointToSampler(Math.pow(resistorUpperBound, 1.0 * max(numParams)))));
//        samplerMakersToBeTested.add(new AnglicanJointToSampler(10000, 0.1, AnglicanCodeGenerator.AnglicanSamplingMethod.rdb));
        samplerMakersToBeTested.add(new AnglicanJointToSampler(10000, 0.1, AnglicanCodeGenerator.AnglicanSamplingMethod.smc));
        samplerMakersToBeTested.add(new AnglicanJointToSampler(10000, 0.1, AnglicanCodeGenerator.AnglicanSamplingMethod.pgibbs));
        samplerMakersToBeTested.add(new StanJointToSampler(0.02));

        Param2JointWrapper conductanceModelParam2Joint = new Param2JointWrapper() {

            @Override
            public JointWrapper makeJointWrapper(int param) {
                double conductMinVarLimit = 1.0 / resistorUpperBound;//5;//lowerBound-0.1;
                double conductMaxVarLimit = 1.0 / resistorLowerBound;//15;//upperBound+0.1;

                GraphicalModel bn =
                        ExperimentalGraphicalModels.makeCircuitConductanceModel(param, resistorLowerBound, resistorUpperBound);

                SymbolicGraphicalModelHandler handler = new SymbolicGraphicalModelHandler();
                Map<String, Double> evidence = new HashMap<String, Double>();


                double gMean = 1.0 / rMean;
                evidence.put("g_t", gMean * param);

                List<String> query = new ArrayList<String>();
                for (int i = 0; i < param; i++) {
                    if (!evidence.keySet().contains("r_" + (i + 1))) query.add("r_" + (i + 1));
                }

                Pair<PiecewiseExpression<Fraction>, List<DeterministicFactor>> jointAndEliminatedStochasticVars =
                        handler.makeJointAndEliminatedStochasticVars(bn, query, evidence);
                PiecewiseExpression<Fraction> joint = jointAndEliminatedStochasticVars.getFirstEntry();
                List<DeterministicFactor> eliminatedStochasticVars = jointAndEliminatedStochasticVars.getSecondEntry();
                double envelopeSafeZone = 0;//0.001;
                RichJointWrapper jointWrapper =
                        new RichJointWrapper(joint, eliminatedStochasticVars, query, conductMinVarLimit - envelopeSafeZone, conductMaxVarLimit + envelopeSafeZone, bn, evidence);
                System.out.println("jointWrapper.getAppropriateSampleVectorSize() = " + jointWrapper.getAppropriateSampleVectorSize());
                System.out.println("jointWrapper.getJoint().getScopeVars() = " + jointWrapper.getJoint().getScopeVars());
                System.out.println("jointWrapper.getJoint() = " + jointWrapper.getJoint());

                //Anglican code:
                String anglicanCode = AnglicanCodeGenerator.makeAnglicanResistorModel(param, resistorLowerBound, resistorUpperBound, evidence, null /*unknown noise*/, query);
                jointWrapper.addExtraInfo(AnglicanCodeGenerator.ANGLICAN_CODE_KEY, anglicanCode);

                // Stan code:
                String stanInput = StanInputDataGenerator.makeStanResistorInput(param, resistorLowerBound, resistorUpperBound, evidence);
                jointWrapper.addExtraInfo(StanJointToSampler.STAN_INPUT_CONTENT_KEY, stanInput);
                jointWrapper.addExtraInfo(StanJointToSampler.STAN_MODEL_FILE_KEY, StanJointToSampler.STAN_RESISTOR_MODEL);


                return jointWrapper;
            }
        };


        testSamplersPerformanceWrtParameterTimeAndSampleCount(conductanceModelParam2Joint,
                new DifferenceFromTrueMeanVectorMeasureGenerator(rMean) {
                    @Override //hack to allow Ground Truth p_t has its own value...
                    public void initialize(RichJointWrapper jointWrapper) {
                        super.initialize(jointWrapper);
                        int r_tQueryIndex = jointWrapper.getQueryVars().indexOf("r_t");
                        if (r_tQueryIndex == -1) {
                            System.err.println("WARNING: we expected that r_t be a query var...QVs=" + jointWrapper.getQueryVars() + " but it is not. So, the Ground truth is not changed.");
                        } else {
                            Double r_t = 1.0 / jointWrapper.getEvidence().get("g_t");
                            if (groundTruthMeans.length != jointWrapper.getQueryVars().size())
                                throw new RuntimeException("EliminatedVarCompleterSamplerMaker expected...");
                            this.groundTruthMeans[r_tQueryIndex] = r_t;
                        }
                        System.out.println("Now known groundTruthMeans=" + Arrays.toString(groundTruthMeans));
                    }
                },
                samplerMakersToBeTested, numParams, numMinDesiredSamples,
                maxWaitingTimeForTakingDesiredSamples,
                minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis,
                approxNumTimePointsForWhichErrIsPersisted, numRuns,
                burnedSamples, reportPath, goldenErrThreshold);

        System.out.println(" That was all the folk for conductance circuit problem with params " + Arrays.toString(numParams) + ".");

    }

    public static int max(int[] ar) {
        int max = ar[0];
        for (int i = 1; i < ar.length; i++) {
            max = Math.max(max, ar[i]);
        }
        return max;
    }

    //--------------------------------------------------------


    public void collisionICML2015Test(final boolean symmetric, String path) throws IOException {
        System.out.println("REPORT PATH COLLISION ANALYSIS = " + path);

        //tester is commented since symmetry is used.

        //The following 3 values are used only used if the Ground truth values are not known:
        final JointToSampler testerSampleMaker = new EliminatedVarCompleterSamplerMaker(FractionalJointRejectionSampler.makeJointToSampler(1.0));
//                FractionalJointSymbolicGibbsSampler.makeJointToSampler();
        final int numSamplesFromTesterToSimulateTrueDistribution = 170000;//100000;
        final int maxWaitingTimeForTesterToSimulateMillis = 1000 * 60 * 10;//1000 * 60 * 10;

        double anglicanNoise = 0.1;
        double stanNoise = 0.05;
        int anglicanBankCapacity = 10000;
        List<JointToSampler> samplerMakersToBeTested = new ArrayList<JointToSampler>();

        samplerMakersToBeTested.add(new EliminatedVarCompleterSamplerMaker(FractionalJointSymbolicGibbsSampler.makeJointToSampler()));
        samplerMakersToBeTested.add(new EliminatedVarCompleterSamplerMaker(FractionalJointBaselineGibbsSampler.makeJointToSampler())); //...
//        samplerMakersToBeTested.add(new EliminatedVarCompleterSamplerMaker(FractionalJointMetropolisHastingSampler.makeJointToSampler(2.0)));
        samplerMakersToBeTested.add(new EliminatedVarCompleterSamplerMaker(FractionalJointSelfTunedMetropolisHastingSampler.makeJointToSampler(0.1, 200, 50)));
        samplerMakersToBeTested.add(new EliminatedVarCompleterSamplerMaker(FractionalJointRejectionSampler.makeJointToSampler(1)));
//        samplerMakersToBeTested.add(new AnglicanJointToSampler(anglicanBankCapacity, anglicanNoise, AnglicanCodeGenerator.AnglicanSamplingMethod.rdb));
        samplerMakersToBeTested.add(new AnglicanJointToSampler(anglicanBankCapacity, anglicanNoise, AnglicanCodeGenerator.AnglicanSamplingMethod.smc));
//        samplerMakersToBeTested.add(new AnglicanJointToSampler(anglicanBankCapacity, angloStanNoise, AnglicanCodeGenerator.AnglicanSamplingMethod.pgibbs));
        samplerMakersToBeTested.add(new StanJointToSampler(stanNoise));

//produces errors...        samplerMakersToBeTested.add(new AnglicanJointToSampler(10000, 0.2, AnglicanCodeGenerator.AnglicanSamplingMethod.ardb));
//produces errors...        samplerMakersToBeTested.add(new AnglicanJointToSampler(10000, 0.2, AnglicanCodeGenerator.AnglicanSamplingMethod.cascade));


        int[] numParams = {2, 4, 5, 6, 8, 10, 12, 14, 15, 16, 18, 20, 22, 24, 26, 28, 30};//{2, 3};
        int numMinDesiredSamples = 20;//1000; //100;
        int maxWaitingTimeForTakingDesiredSamples = 1000 * 2;//1000 * 60 * 2;//1000*60*5;//1000 * 20;
        int minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis = 1000 * 2;//1000 * 5;//1000*60;//1000 * 5;
        int approxNumTimePointsForWhichErrIsPersisted = 100;//33;
        int numRuns = 15;//20;//2;
        int burnedSamples = 100;//50;
        double goldenErrThreshold = 0.3;

        final double mom = 1.5; //will be multiplied in the number of objects to generate the total momentum
        Param2JointWrapper collisionModelParam2Joint = new Param2JointWrapper() {

            @Override
            public JointWrapper makeJointWrapper(int param) {
                Double muAlpha = 0.2;//-2.2;//0.2;
                Double muBeta = 2.2;
                Double nuAlpha = muAlpha; //-2.0;
                Double nuBeta = muBeta;// 2.0;
                double minVarLimit = 0.2;
                double maxVarLimit = 2.2;

                GraphicalModel bn =
                        ExperimentalGraphicalModels.makeCollisionModel(param, muAlpha, muBeta, nuAlpha, nuBeta, symmetric);//paramDataCount2DataGenerator.createJointGenerator(param);

                SymbolicGraphicalModelHandler handler = new SymbolicGraphicalModelHandler();
                Map<String, Double> evidence = new HashMap<String, Double>();


                evidence.put("p_t", mom * param);
//                evidence.put("m_1", 2d);
//                evidence.put("v_2", 0.2d);

//                List<String> query = Arrays.asList("v_1", "v_" + (param - 1));
                List<String> query = new ArrayList<String>();
                for (int i = 0; i < param; i++) {
                    if (!evidence.keySet().contains("v_" + (i + 1))) query.add("v_" + (i + 1));
                    if (!evidence.keySet().contains("m_" + (i + 1))) query.add("m_" + (i + 1)); //todo add again
                }
//                query.add("p_1");
//                query.add("p_t");
//                query.add("m_1");
//                query.add("v_1");

//                query.add("p_t");


//                PiecewiseExpression<Fraction> joint = handler.makeJoint(bn, query, evidence);
//                JointWrapper jointWrapper = new JointWrapper(joint, minVarLimit, maxVarLimit);
//                System.out.println("jointWrapper.getJoint().getScopeVars() = " + jointWrapper.getJoint().getScopeVars());

                Pair<PiecewiseExpression<Fraction>, List<DeterministicFactor>> jointAndEliminatedStochasticVars =
                        handler.makeJointAndEliminatedStochasticVars(bn, query, evidence);
                PiecewiseExpression<Fraction> joint = jointAndEliminatedStochasticVars.getFirstEntry();
                List<DeterministicFactor> eliminatedStochasticVars = jointAndEliminatedStochasticVars.getSecondEntry();
                RichJointWrapper jointWrapper =
                        new RichJointWrapper(joint, eliminatedStochasticVars, query, minVarLimit, maxVarLimit, bn, evidence);
                System.out.println("jointWrapper.getAppropriateSampleVectorSize() = " + jointWrapper.getAppropriateSampleVectorSize());
//                System.out.println("jointWrapper.eliminatedStochasticVarFactors() = " + jointWrapper.eliminatedStochasticVarFactors());
                System.out.println("jointWrapper.getJoint().getScopeVars() = " + jointWrapper.getJoint().getScopeVars());
                System.out.println("jointWrapper.getJoint() = " + jointWrapper.getJoint());

                // Anglican code:
                String anglicanCode = AnglicanCodeGenerator.makeAnglicanCollisionModel(param, muAlpha, muBeta, nuAlpha, nuBeta, symmetric, evidence, null /*unknown noise*/, query);
                jointWrapper.addExtraInfo(AnglicanCodeGenerator.ANGLICAN_CODE_KEY, anglicanCode);

                // Stan code:
                String stanInput = StanInputDataGenerator.makeStanCollisionInput(param, muAlpha, muBeta, nuAlpha, nuBeta, symmetric, evidence);
                jointWrapper.addExtraInfo(StanJointToSampler.STAN_INPUT_CONTENT_KEY, stanInput);
                jointWrapper.addExtraInfo(StanJointToSampler.STAN_MODEL_FILE_KEY, StanJointToSampler.STAN_COLLISION_MODEL);

                return jointWrapper;
            }
        };


        testSamplersPerformanceWrtParameterTimeAndSampleCount(collisionModelParam2Joint,
                new DifferenceFromTrueMeanVectorMeasureGenerator(Math.sqrt(mom)) {
                    @Override //hack to allow Ground Truth p_t has its own value...
                    public void initialize(RichJointWrapper jointWrapper) {
                        super.initialize(jointWrapper);
                        int p_tQueryIndex = jointWrapper.getQueryVars().indexOf("p_t");
                        if (p_tQueryIndex == -1) {
                            System.err.println("WARNING: p_t is not a query var. Ground truth not modified.... (we expected that p_t be a query var...QVs=" + jointWrapper.getQueryVars() + ")");

                        } else {
                            Double p_t = jointWrapper.getEvidence().get("p_t");
                            if (groundTruthMeans.length != jointWrapper.getQueryVars().size())
                                throw new RuntimeException("EliminatedVarCompleterSamplerMaker expected...");
                            this.groundTruthMeans[p_tQueryIndex] = p_t; //NOTE only works if p_t is the last param
                            System.out.println("Now known groundTruthMeans=" + Arrays.toString(groundTruthMeans));
                        }
                    }
                },//(1.2d), //known true mean of all vars is 0!
//                  new DifferenceFromTrueMeanVector(testerSampleMaker,
//                          numSamplesFromTesterToSimulateTrueDistribution, maxWaitingTimeForTesterToSimulateMillis),
//                new DifferenceFromSymmetricVector(),
//                null), //testerSampleMaker,
                samplerMakersToBeTested, numParams, numMinDesiredSamples,
                maxWaitingTimeForTakingDesiredSamples,
                minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis,
                approxNumTimePointsForWhichErrIsPersisted, numRuns,
                burnedSamples, path, goldenErrThreshold);

        System.out.println(" That was all the folk for symmetric collision problem. ");
    }

    /*************************************************************************************************************/
    /*************************************************************************************************************/
    /*************************************************************************************************************/
    /*************************************************************************************************************/
    /**
     * *********************************************************************************************************
     */



    /*public void visualCollisionICML2015Test(String path) throws IOException {
        System.out.println("REPORT PATH for VISUAL COLLISION ANALYSIS = " + path);

        final boolean symmetric = false;

        //tester is commented since symmetry is used.

        //The following 3 values are used only used if the Ground truth values are not known:
        final JointToSampler testerSampleMaker = new EliminatedVarCompleterSamplerMaker(FractionalJointRejectionSampler.makeJointToSampler(1.0));
//                FractionalJointSymbolicGibbsSampler.makeJointToSampler();
        final int numSamplesFromTesterToSimulateTrueDistribution = 170000;//100000;
        final int maxWaitingTimeForTesterToSimulateMillis = 1000 * 60 * 10;//1000 * 60 * 10;

        double anglicanNoise = 0.1;
        double stanNoise = 0.05;
        int anglicanBankCapacity = 10000;
        List<JointToSampler> samplerMakersToBeTested = new ArrayList<JointToSampler>();
//        samplerMakersToBeTested.add(new EliminatedVarCompleterSamplerMaker(FractionalJointSymbolicGibbsSampler.makeJointToSampler()));
//        samplerMakersToBeTested.add(new EliminatedVarCompleterSamplerMaker(FractionalJointBaselineGibbsSampler.makeJointToSampler())); //...
//        samplerMakersToBeTested.add(new EliminatedVarCompleterSamplerMaker(FractionalJointSelfTunedMetropolisHastingSampler.makeJointToSampler(0.1, 200, 50)));
//        samplerMakersToBeTested.add(new EliminatedVarCompleterSamplerMaker(FractionalJointRejectionSampler.makeJointToSampler(10)));
////        samplerMakersToBeTested.add(new AnglicanJointToSampler(anglicanBankCapacity, anglicanNoise, AnglicanCodeGenerator.AnglicanSamplingMethod.rdb));
//        samplerMakersToBeTested.add(new AnglicanJointToSampler(anglicanBankCapacity, anglicanNoise, AnglicanCodeGenerator.AnglicanSamplingMethod.smc));
////        samplerMakersToBeTested.add(new AnglicanJointToSampler(anglicanBankCapacity, angloStanNoise, AnglicanCodeGenerator.AnglicanSamplingMethod.pgibbs));
////        samplerMakersToBeTested.add(new StanJointToSampler(stanNoise));
        samplerMakersToBeTested.add(new JointToSampler() {
            @Override
            public SamplerInterface makeSampler(JointWrapper jointWrapper) { //a hack!
                return StanJointToSampler.makeFactoryCompatibleSampler("collision_asymetric_model", "asymetric.data.R", "asym.out.csv", jointWrapper.getJoint().getFactory());
            }

            @Override
            public String getName() {
                return "stan.hmc";  //To change body of implemented methods use File | Settings | File Templates.
            }
        });

//produces errors...        samplerMakersToBeTested.add(new AnglicanJointToSampler(10000, 0.2, AnglicanCodeGenerator.AnglicanSamplingMethod.ardb));
//produces errors...        samplerMakersToBeTested.add(new AnglicanJointToSampler(10000, 0.2, AnglicanCodeGenerator.AnglicanSamplingMethod.cascade));

        int[] numParams = {2};
        int numMinDesiredSamples = 20;//1000; //100;
        int maxWaitingTimeForTakingDesiredSamples = 1000 * 2;//1000 * 60 * 2;//1000*60*5;//1000 * 20;
        int minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis = 1000 * 2;//1000 * 5;//1000*60;//1000 * 5;
        int approxNumTimePointsForWhichErrIsPersisted = 100;//33;
        int numRuns = 1;//10;//20;//2;
        int burnedSamples = 100;//50;
        double goldenErrThreshold = 0.3;

        final double mom = 1.5; //will be multiplied in the number of objects to generate the total momentum
        Param2JointWrapper collisionModelParam2Joint = new Param2JointWrapper() {

            @Override
            public JointWrapper makeJointWrapper(int param) {
                Double muAlpha = 0.2;//-2.2;//0.2;
                Double muBeta = 2.2;
                Double nuAlpha = muAlpha; //-2.0;
                Double nuBeta = muBeta;// 2.0;
                double minVarLimit = 0.2;
                double maxVarLimit = 2.2;

                GraphicalModel bn =
                        ExperimentalGraphicalModels.makeCollisionModel(param, muAlpha, muBeta, nuAlpha, nuBeta, symmetric);//paramDataCount2DataGenerator.createJointGenerator(param);

                SymbolicGraphicalModelHandler handler = new SymbolicGraphicalModelHandler();
                Map<String, Double> evidence = new HashMap<String, Double>();


                evidence.put("p_t", mom * param);
//                evidence.put("m_1", 2d);
//                evidence.put("v_2", 0.2d);

//                List<String> query = Arrays.asList("v_1", "v_" + (param - 1));
                List<String> query = new ArrayList<String>();
                for (int i = 0; i < param; i++) {
                    if (!evidence.keySet().contains("v_" + (i + 1))) query.add("v_" + (i + 1));
                    if (!evidence.keySet().contains("m_" + (i + 1))) query.add("m_" + (i + 1)); //todo add again
                }
//                query.add("p_1");
//                query.add("p_t");
//                query.add("m_1");
//                query.add("v_1");

//                query.add("p_t");


//                PiecewiseExpression<Fraction> joint = handler.makeJoint(bn, query, evidence);
//                JointWrapper jointWrapper = new JointWrapper(joint, minVarLimit, maxVarLimit);
//                System.out.println("jointWrapper.getJoint().getScopeVars() = " + jointWrapper.getJoint().getScopeVars());

                Pair<PiecewiseExpression<Fraction>, List<DeterministicFactor>> jointAndEliminatedStochasticVars =
                        handler.makeJointAndEliminatedStochasticVars(bn, query, evidence);
                PiecewiseExpression<Fraction> joint = jointAndEliminatedStochasticVars.getFirstEntry();
                List<DeterministicFactor> eliminatedStochasticVars = jointAndEliminatedStochasticVars.getSecondEntry();
                RichJointWrapper jointWrapper =
                        new RichJointWrapper(joint, eliminatedStochasticVars, query, minVarLimit, maxVarLimit, bn, evidence);
                System.out.println("jointWrapper.getAppropriateSampleVectorSize() = " + jointWrapper.getAppropriateSampleVectorSize());
//                System.out.println("jointWrapper.eliminatedStochasticVarFactors() = " + jointWrapper.eliminatedStochasticVarFactors());
                System.out.println("jointWrapper.getJoint().getScopeVars() = " + jointWrapper.getJoint().getScopeVars());
                System.out.println("jointWrapper.getJoint() = " + jointWrapper.getJoint());

                // Anglican code:
                String anglicanCode = AnglicanCodeGenerator.makeAnglicanCollisionModel(param, muAlpha, muBeta, nuAlpha, nuBeta, symmetric, evidence, null *//*unknown noise*//*, query);
                jointWrapper.addExtraInfo(AnglicanCodeGenerator.ANGLICAN_CODE_KEY, anglicanCode);

                // Stan code:
//                String stanInput = StanInputDataGenerator.makeStanCollisionInput(param, muAlpha, muBeta, nuAlpha, nuBeta, symmetric, evidence);
//                jointWrapper.addExtraInfo(StanJointToSampler.STAN_INPUT_CONTENT_KEY, stanInput);
//                jointWrapper.addExtraInfo(StanJointToSampler.STAN_MODEL_FILE_KEY, StanJointToSampler.STAN_COLLISION_MODEL);

                return jointWrapper;
            }
        };


        testSamplersPerformanceWrtParameterTimeAndSampleCount(collisionModelParam2Joint,
                new DifferenceFromTrueMeanVectorMeasureGenerator(Math.sqrt(mom)) {
                    @Override //hack to allow Ground Truth p_t has its own value...
                    public void initialize(RichJointWrapper jointWrapper) {
                        super.initialize(jointWrapper);
                        int p_tQueryIndex = jointWrapper.getQueryVars().indexOf("p_t");
                        if (p_tQueryIndex == -1) {
                            System.err.println("WARNING: p_t is not a query var. Ground truth not modified.... (we expected that p_t be a query var...QVs=" + jointWrapper.getQueryVars() + ")");

                        } else {
                            Double p_t = jointWrapper.getEvidence().get("p_t");
                            if (groundTruthMeans.length != jointWrapper.getQueryVars().size())
                                throw new RuntimeException("EliminatedVarCompleterSamplerMaker expected...");
                            this.groundTruthMeans[p_tQueryIndex] = p_t; //NOTE only works if p_t is the last param
                            System.out.println("Now known groundTruthMeans=" + Arrays.toString(groundTruthMeans));
                        }
                    }
                },//(1.2d), //known true mean of all vars is 0!
//                  new DifferenceFromTrueMeanVector(testerSampleMaker,
//                          numSamplesFromTesterToSimulateTrueDistribution, maxWaitingTimeForTesterToSimulateMillis),
//                new DifferenceFromSymmetricVector(),
//                null), //testerSampleMaker,
                samplerMakersToBeTested, numParams, numMinDesiredSamples,
                maxWaitingTimeForTakingDesiredSamples,
                minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis,
                approxNumTimePointsForWhichErrIsPersisted, numRuns,
                burnedSamples, path, goldenErrThreshold);

        System.out.println(" That was all the folk for visual collision problem. ");
    }*/
    public void fermentationICML2015Test(String path) throws IOException {
        System.out.println("REPORT PATH FERMENTATION ANALYSIS = " + path);

//        JointToSampler testerSampleMaker =
//                FractionalJointRejectionSampler.makeJointToSampler(1.0);
//                new SelectedQuerySampler(FractionalJointSymbolicGibbsSampler.makeJointToSampler());
//                FractionalJointMetropolisHastingSampler.makeJointToSampler(4.0);
//                new SelectedQuerySampler(FractionalJointBaselineGibbsSampler.makeJointToSampler());

//        int numSamplesFromTesterToSimulateTrueDistribution = 1000000;//1000;
//        int maxWaitingTimeForTesterToSimulateMillis = 1000 * 60 * 60 * 5;//1000 * 60 * 1;
        List<JointToSampler> samplerMakersToBeTested = new ArrayList<JointToSampler>();
//        samplerMakersToBeTested.add(new DifferenceSampler(
//                new EliminatedVarCompleterSamplerMaker(FractionalJointSymbolicGibbsSampler.makeJointToSampler()),
//                new EliminatedVarCompleterSamplerMaker(FractionalJointSymbolicGibbsSampler.makeJointToSampler())));
//        samplerMakersToBeTested.add(new DifferenceSampler(
//                new EliminatedVarCompleterSamplerMaker(FractionalJointBaselineGibbsSampler.makeJointToSampler()),
//                new EliminatedVarCompleterSamplerMaker(FractionalJointBaselineGibbsSampler.makeJointToSampler())));
//        samplerMakersToBeTested.add(new DifferenceSampler(
//                new EliminatedVarCompleterSamplerMaker(FractionalJointMetropolisHastingSampler.makeJointToSampler(0.1)),
//                new EliminatedVarCompleterSamplerMaker(FractionalJointMetropolisHastingSampler.makeJointToSampler(0.1))));
//        samplerMakersToBeTested.add(new DifferenceSampler(
//                new EliminatedVarCompleterSamplerMaker(FractionalJointSelfTunedMetropolisHastingSampler.makeJointToSampler(0.5, 20, 10)),
//                new EliminatedVarCompleterSamplerMaker(FractionalJointSelfTunedMetropolisHastingSampler.makeJointToSampler(0.5, 20, 10))));
//        int maxAnglicanNumberOfTakenSamples = 10000;
//        double anglicanNoise = 0.2;
//        samplerMakersToBeTested.add(new DifferenceSampler(
//                new AnglicanJointToSampler(maxAnglicanNumberOfTakenSamples, anglicanNoise, AnglicanCodeGenerator.AnglicanSamplingMethod.rdb),
//                new AnglicanJointToSampler(maxAnglicanNumberOfTakenSamples, anglicanNoise, AnglicanCodeGenerator.AnglicanSamplingMethod.rdb)));
//        samplerMakersToBeTested.add(new DifferenceSampler(
//                new AnglicanJointToSampler(maxAnglicanNumberOfTakenSamples, anglicanNoise, AnglicanCodeGenerator.AnglicanSamplingMethod.smc),
//                new AnglicanJointToSampler(maxAnglicanNumberOfTakenSamples, anglicanNoise, AnglicanCodeGenerator.AnglicanSamplingMethod.smc)));
//        samplerMakersToBeTested.add(new DifferenceSampler(
//                new AnglicanJointToSampler(maxAnglicanNumberOfTakenSamples, anglicanNoise, AnglicanCodeGenerator.AnglicanSamplingMethod.pgibbs),
//                new AnglicanJointToSampler(maxAnglicanNumberOfTakenSamples, anglicanNoise, AnglicanCodeGenerator.AnglicanSamplingMethod.pgibbs)));
        samplerMakersToBeTested.add(new DifferenceSampler(
                new StanJointToSampler(0.2, "1"),
                new StanJointToSampler(0.2, "2")));
        int[] numParams = {3, 4, 5, 6, 7, 8, 9, 10};//, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20};
        int numMinDesiredSamples = 200;//1000;
        int maxWaitingTimeForTakingDesiredSamples = 1000 * 5;//60 * 2;//1000 * 20;
        int minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis = 1000 * 5;//5;//1000 * 5;
        int approxNumTimePointsForWhichErrIsPersisted = 100;//33;
        int numRuns = 2;
        int burnedSamples = 0;//100;//10;
        double goldenErrThreshold = 0.02;


        Param2JointWrapper fermentationModelParam2Joint = new Param2JointWrapper() {

            @Override
            public JointWrapper makeJointWrapper(int param) {
//                Double muAlpha = 0.2;
//                Double muBeta = 2.2;
//                Double nuAlpha = -2.0;
//                Double nuBeta = 2.0;

                double alpha = 0d;
                double beta = 1d;
                double minVarLimit = alpha;
                double maxVarLimit = beta;

                GraphicalModel bn =
//                        makeFermentationModel(param, 1d, 0.1, 12d);
                        ExperimentalGraphicalModels.makeSimplifiedFermentationModel(param, alpha, beta);//0.1, 12d);


                SymbolicGraphicalModelHandler handler = new SymbolicGraphicalModelHandler();
                Map<String, Double> evidence = new HashMap<String, Double>();

                evidence.put("q", 0.2);

                List<String> query = Arrays.asList("l_1", "l_" + (param));
//                PiecewiseExpression<Fraction> joint = handler.makeJoint(bn, query, evidence);
                Pair<PiecewiseExpression<Fraction>, List<DeterministicFactor>> jointAndEliminatedStochasticVars =
                        handler.makeJointAndEliminatedStochasticVars(bn, query, evidence);
                PiecewiseExpression<Fraction> joint = jointAndEliminatedStochasticVars.getFirstEntry();
                List<DeterministicFactor> eliminatedStochasticVars = jointAndEliminatedStochasticVars.getSecondEntry();

                RichJointWrapper jointWrapper = new RichJointWrapper(joint, eliminatedStochasticVars, query, minVarLimit, maxVarLimit, bn, evidence);
                System.out.println("jointWrapper.getJoint() = " + jointWrapper.getJoint());

                //Anglican code:
                String anglicanCode = AnglicanCodeGenerator.makeAnglicanFermentationModel(param, alpha, beta, evidence, null /*unknown noise*/, query);
                jointWrapper.addExtraInfo(AnglicanCodeGenerator.ANGLICAN_CODE_KEY, anglicanCode);

                // Stan code:
                String stanInput = StanInputDataGenerator.makeStanFermentationInput(param, alpha, beta, evidence);
                jointWrapper.addExtraInfo(StanJointToSampler.STAN_INPUT_CONTENT_KEY, stanInput);
                jointWrapper.addExtraInfo(StanJointToSampler.STAN_MODEL_FILE_KEY, StanJointToSampler.STAN_FERMENTATION_MODEL);

                return jointWrapper;
            }
        };


        testSamplersPerformanceWrtParameterTimeAndSampleCount(fermentationModelParam2Joint,
                new DifferenceFromTrueMeanVectorMeasureGenerator(0d),
                samplerMakersToBeTested, numParams, numMinDesiredSamples,
                maxWaitingTimeForTakingDesiredSamples,
                minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis,
                approxNumTimePointsForWhichErrIsPersisted, numRuns, burnedSamples, path, goldenErrThreshold);

        System.out.println(" That was all the folk for fermentation. ");

    }

    private void testSamplersPerformanceWrtParameterTimeAndSampleCount(
            Param2JointWrapper paramToJointWrapper, //model
            DifferenceFromGroundTruthMeasureGenerator differenceMetricsGenerator,
//            Double knownGroundTruthMeansOfAllVariables, //null if unknown
//            JointToSampler testerSampleMaker,  //used as baseline (if known ground truth is null)
            //int numSamplesFromTesterToSimulateTrueDistribution, // (used only if ground truth means are not given)
//            long maxWaitingTimeForTesterToSimulateMillis,
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

            RichJointWrapper jointWrapper = (RichJointWrapper) paramToJointWrapper.makeJointWrapper(param);
            //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

//            MeasureOnTheRun<Double[]> groundTruthDefMeasure = differenceMetricsGenerator.generate(jointWrapper, numSamplesFromTesterToSimulateTrueDistribution, maxWaitingTimeForTesterToSimulateMillis);
            differenceMetricsGenerator.initialize(jointWrapper);//, numSamplesFromTesterToSimulateTrueDistribution, maxWaitingTimeForTesterToSimulateMillis);

            //Analysis of tested algorithms:
            for (JointToSampler samplerMaker : samplerMakersToBeTested) {
//                if (!testedAlgsDeathKeeper.algorithmWillDie(samplerMaker, param, numObservedDataPoints)) {
                try {
                    MultiMCMCChainAnalysis multiMCMCChainAnalysis = new MultiMCMCChainAnalysis(numRuns,
                            differenceMetricsGenerator, //groundTruthMeans,
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


//                    System.out.println(samplerMaker.getName() + ".timeN/GOLD = " + multiMCMCChainAnalysis.averageTimeToAccomplishOrGolden/*totalProcessTimeMillis*/ + "\t\tsamples=" + multiMCMCChainAnalysis.numberOfFirstSamples());//.means4FirstSamples.size());

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
        Map<String /*alg.*/, Map<Integer /*param*/, Double /*gold*/>> alg2paramGoldMeanMap;
        Map<String /*alg.*/, Map<Integer /*param*/, Double /*gold*/>> alg2paramGoldStdErrMap;

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

            if (paramToMeanMap.put(param, goldMean) != null)
                throw new RuntimeException("double entry for param " + param);
            if (paramToStdErrMap.put(param, goldStdErr) != null)
                throw new RuntimeException("double entry for param " + param);

            persistInner(samplerName, fileSuffix, paramToMeanMap, paramToStdErrMap);
        }

        private void persistInner(String samplerName, String fileSuffix, Map<Integer, Double> param2Means, Map<Integer, Double> param2StdErrs) throws FileNotFoundException {
            String outputFileName = this.outputDirectoryPath + samplerName + "-" + fileSuffix;

            PrintStream ps = new PrintStream(new FileOutputStream(outputFileName));

            SortedSet<Integer> params = new TreeSet<Integer>(param2Means.keySet());
            int i = 1;
            for (Integer param : params) {
                ps.println((i++) + "\t" + param + "\t" + param2Means.get(param) + "\t" + param2StdErrs.get(param));
            }

            ps.close();
        }

    }

  /*  private class TotalTimeKeeper {
        String outputDirectoryPath;
        Map<String*//*algorithm*//*, SortedSet<Pair<Integer *//*dims*//*, Long *//*totalTime*//*>>> alg2dimsTime;
//        Map<String*//*algorithm*//*, SortedSet<Pair<Integer *//*data*//*, Long *//*totalTime*//*>>> alg2dataTime;

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
        Map<String *//*alg*//*, Long*//*time*//*>[] dimIndexDataIndexAlgTime;

        public TotalTimeKeeper(List<JointToSampler> samplers, int[] dimsArray*//*, int[] dataArray*//*, String outputDirectoryPath) {
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
            persistDataFix(samplerName*//*, dataIndex*//*);
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

        private void persistDataFix(String samplerName*//*, int dataIndex*//*) throws FileNotFoundException {
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

    }*/


    private interface Param2JointWrapper {
        JointWrapper makeJointWrapper(int param);
    }


    //.................................


    public void visualCollisionICML2015Test(final boolean symmetric, String path) throws IOException {
        System.out.println("REPORT PATH COLLISION ANALYSIS = " + path);

        //tester is commented since symmetry is used.

        //The following 3 values are used only used if the Ground truth values are not known:
        final JointToSampler testerSampleMaker = new EliminatedVarCompleterSamplerMaker(FractionalJointRejectionSampler.makeJointToSampler(1.0));
//                FractionalJointSymbolicGibbsSampler.makeJointToSampler();
        final int numSamplesFromTesterToSimulateTrueDistribution = 200000;//100000;
        final int maxWaitingTimeForTesterToSimulateMillis = 1000 * 60 * 10;//1000 * 60 * 10;

//        double anglicanNoise = 0.1;
//        double stanNoise = 0.05;
        final int anglicanBankCapacity = 10000;
        List<JointToSampler> samplerMakersToBeTested = new ArrayList<JointToSampler>();
        samplerMakersToBeTested.add(new EliminatedVarCompleterSamplerMaker(FractionalJointSymbolicGibbsSampler.makeJointToSampler()));
        samplerMakersToBeTested.add(new EliminatedVarCompleterSamplerMaker(FractionalJointBaselineGibbsSampler.makeJointToSampler())); //...
//        samplerMakersToBeTested.add(new EliminatedVarCompleterSamplerMaker(FractionalJointMetropolisHastingSampler.makeJointToSampler(2.0)));
        samplerMakersToBeTested.add(new EliminatedVarCompleterSamplerMaker(FractionalJointSelfTunedMetropolisHastingSampler.makeJointToSampler(0.1, 200, 50)));
        samplerMakersToBeTested.add(new EliminatedVarCompleterSamplerMaker(FractionalJointRejectionSampler.makeJointToSampler(1.0)));
//        samplerMakersToBeTested.add(new AnglicanJointToSampler(anglicanBankCapacity, anglicanNoise, AnglicanCodeGenerator.AnglicanSamplingMethod.rdb));
        samplerMakersToBeTested.add(new JointToSampler() {
            double n = 0.2;

            @Override
            public SamplerInterface makeSampler(JointWrapper jointWrapper) {
                return new AnglicanJointToSampler(anglicanBankCapacity, n, AnglicanCodeGenerator.AnglicanSamplingMethod.smc).makeSampler(jointWrapper);
            }

            @Override
            public String getName() {
                return "smc1";
            }
        });
        samplerMakersToBeTested.add(new JointToSampler() {
            double n = 0.01;

            @Override
            public SamplerInterface makeSampler(JointWrapper jointWrapper) {
                return new AnglicanJointToSampler(anglicanBankCapacity, n, AnglicanCodeGenerator.AnglicanSamplingMethod.smc).makeSampler(jointWrapper);
            }

            @Override
            public String getName() {
                return "smc2";
            }
        });
//        samplerMakersToBeTested.add(new AnglicanJointToSampler(anglicanBankCapacity, angloStanNoise, AnglicanCodeGenerator.AnglicanSamplingMethod.pgibbs));
        samplerMakersToBeTested.add(new JointToSampler() {
            double n = 0.005;

            @Override
            public SamplerInterface makeSampler(JointWrapper jointWrapper) {
                return new StanJointToSampler(n).makeSampler(jointWrapper);
            }

            @Override
            public String getName() {
                return "stan1";
            }
        });
        samplerMakersToBeTested.add(new JointToSampler() {
            double n = 0.1;

            @Override
            public SamplerInterface makeSampler(JointWrapper jointWrapper) {
                return new StanJointToSampler(n).makeSampler(jointWrapper);
            }

            @Override
            public String getName() {
                return "stan2";
            }
        });
//produces errors...        samplerMakersToBeTested.add(new AnglicanJointToSampler(10000, 0.2, AnglicanCodeGenerator.AnglicanSamplingMethod.ardb));
//produces errors...        samplerMakersToBeTested.add(new AnglicanJointToSampler(10000, 0.2, AnglicanCodeGenerator.AnglicanSamplingMethod.cascade));

        int[] numParams = {2};
        int numMinDesiredSamples = 20;//1000; //100;
        int maxWaitingTimeForTakingDesiredSamples = 1000 / 3;//1000 * 60 * 2;//1000*60*5;//1000 * 20;
        int minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis = 1000 * 2;//1000 * 5;//1000*60;//1000 * 5;
        int approxNumTimePointsForWhichErrIsPersisted = 100;//33;
        int numRuns = 15;//10;//20;//2;
        int burnedSamples = 100;//50;
        double goldenErrThreshold = 0.3;

        final double mom = 1.5; //will be multiplied in the number of objects to generate the total momentum
        Param2JointWrapper collisionModelParam2Joint = new Param2JointWrapper() {

            @Override
            public JointWrapper makeJointWrapper(int param) {
                Double muAlpha = 0.2;//-2.2;//0.2;
                Double muBeta = 2.2;
                Double nuAlpha = muAlpha; //-2.0;
                Double nuBeta = muBeta;// 2.0;
                double minVarLimit = 0.2;
                double maxVarLimit = 2.2;

                GraphicalModel bn =
                        ExperimentalGraphicalModels.makeCollisionModel(param, muAlpha, muBeta, nuAlpha, nuBeta, symmetric);//paramDataCount2DataGenerator.createJointGenerator(param);

                SymbolicGraphicalModelHandler handler = new SymbolicGraphicalModelHandler();
                Map<String, Double> evidence = new HashMap<String, Double>();


                evidence.put("p_t", mom * param);
//                evidence.put("m_1", 2d);
//                evidence.put("v_2", 0.2d);

//                List<String> query = Arrays.asList("v_1", "v_" + (param - 1));
                List<String> query = new ArrayList<String>();
                for (int i = 0; i < param; i++) {
                    if (!evidence.keySet().contains("v_" + (i + 1))) query.add("v_" + (i + 1));
                    if (!evidence.keySet().contains("m_" + (i + 1))) query.add("m_" + (i + 1)); //todo add again
                }
//                query.add("p_1");
//                query.add("p_t");
//                query.add("m_1");
//                query.add("v_1");

//                query.add("p_t");


//                PiecewiseExpression<Fraction> joint = handler.makeJoint(bn, query, evidence);
//                JointWrapper jointWrapper = new JointWrapper(joint, minVarLimit, maxVarLimit);
//                System.out.println("jointWrapper.getJoint().getScopeVars() = " + jointWrapper.getJoint().getScopeVars());

                Pair<PiecewiseExpression<Fraction>, List<DeterministicFactor>> jointAndEliminatedStochasticVars =
                        handler.makeJointAndEliminatedStochasticVars(bn, query, evidence);
                PiecewiseExpression<Fraction> joint = jointAndEliminatedStochasticVars.getFirstEntry();
                List<DeterministicFactor> eliminatedStochasticVars = jointAndEliminatedStochasticVars.getSecondEntry();
                RichJointWrapper jointWrapper =
                        new RichJointWrapper(joint, eliminatedStochasticVars, query, minVarLimit, maxVarLimit, bn, evidence);
                System.out.println("jointWrapper.getAppropriateSampleVectorSize() = " + jointWrapper.getAppropriateSampleVectorSize());
//                System.out.println("jointWrapper.eliminatedStochasticVarFactors() = " + jointWrapper.eliminatedStochasticVarFactors());
                System.out.println("jointWrapper.getJoint().getScopeVars() = " + jointWrapper.getJoint().getScopeVars());
                System.out.println("jointWrapper.getJoint() = " + jointWrapper.getJoint());

                // Anglican code:
                String anglicanCode = AnglicanCodeGenerator.makeAnglicanCollisionModel(param, muAlpha, muBeta, nuAlpha, nuBeta, symmetric, evidence, null /*unknown noise*/, query);
                jointWrapper.addExtraInfo(AnglicanCodeGenerator.ANGLICAN_CODE_KEY, anglicanCode);

                // Stan code:
                String stanInput = StanInputDataGenerator.makeStanCollisionInput(param, muAlpha, muBeta, nuAlpha, nuBeta, symmetric, evidence);
                jointWrapper.addExtraInfo(StanJointToSampler.STAN_INPUT_CONTENT_KEY, stanInput);
                jointWrapper.addExtraInfo(StanJointToSampler.STAN_MODEL_FILE_KEY, StanJointToSampler.STAN_COLLISION_MODEL);

                return jointWrapper;
            }
        };


        testSamplersPerformanceWrtParameterTimeAndSampleCount(collisionModelParam2Joint,
                new DifferenceFromTrueMeanVectorMeasureGenerator(Math.sqrt(mom)) {
                    @Override //hack to allow Ground Truth p_t has its own value...
                    public void initialize(RichJointWrapper jointWrapper) {
                        super.initialize(jointWrapper);
                        int p_tQueryIndex = jointWrapper.getQueryVars().indexOf("p_t");
                        if (p_tQueryIndex == -1) {
                            System.err.println("WARNING: p_t is not a query var. Ground truth not modified.... (we expected that p_t be a query var...QVs=" + jointWrapper.getQueryVars() + ")");

                        } else {
                            Double p_t = jointWrapper.getEvidence().get("p_t");
                            if (groundTruthMeans.length != jointWrapper.getQueryVars().size())
                                throw new RuntimeException("EliminatedVarCompleterSamplerMaker expected...");
                            this.groundTruthMeans[p_tQueryIndex] = p_t; //NOTE only works if p_t is the last param
                            System.out.println("Now known groundTruthMeans=" + Arrays.toString(groundTruthMeans));
                        }
                    }
                },//(1.2d), //known true mean of all vars is 0!
//                  new DifferenceFromTrueMeanVector(testerSampleMaker,
//                          numSamplesFromTesterToSimulateTrueDistribution, maxWaitingTimeForTesterToSimulateMillis),
//                new DifferenceFromSymmetricVector(),
//                null), //testerSampleMaker,
                samplerMakersToBeTested, numParams, numMinDesiredSamples,
                maxWaitingTimeForTakingDesiredSamples,
                minDesiredSamplingTimeRegardlessOfNumTakenSamplesMillis,
                approxNumTimePointsForWhichErrIsPersisted, numRuns,
                burnedSamples, path, goldenErrThreshold);

        System.out.println(" That was all the folk for VISUAL collision problem. ");
    }

}     //end main class

//If ground truth is not known, but we know that the measure is symmetric and all entries should have sample mean...
/*@Deprecated
        //does not produce meaningful results... Danna why... //todo fix or delete
class DifferenceFromSymmetricVector implements DifferenceFromGroundTruthMeasureGenerator {
    Integer sampleLength = null;

    @Override
    public void initialize(RichJointWrapper jointWrapper) {
        sampleLength = jointWrapper.getAppropriateSampleVectorSize();
    }

    @Override
    public MeasureOnTheRun<Double[]> generateMeasure() {

//        final Integer sampleLength = jointWrapper.getAppropriateSampleVectorSize();
        final double[] runningAccumulatedSample = new double[sampleLength];

        return new MeasureOnTheRun<Double[]>() {
            Integer takenSamples = 0;

            @Override
            public void addNewValue(Double[] sample) {
                if (sample.length != sampleLength)
                    throw new RuntimeException("size mismatch. sample=" + Arrays.toString(sample) + " but expected sample length =" + sampleLength);
                for (int i = 0; i < sampleLength; i++) {
                    runningAccumulatedSample[i] = runningAccumulatedSample[i] + sample[i];
                }
                takenSamples++;
            }

            @Override
            public double computeMeasure() {

//                System.out.println("runningAccumulatedSample = " + Arrays.toString(runningAccumulatedSample));

                double mean = 0;  //running error
                for (double d : runningAccumulatedSample) {
                    mean += d;
                }
                mean /= (double) takenSamples;

                double err = 0;
                for (int i = 0; i < sampleLength; i++) {
                    err += Math.abs((runningAccumulatedSample[i] / (double) takenSamples) - mean);
                }
                err /= (double) sampleLength;
                return err;
            }

        };
    }
}*/

//absolute difference from the mean or calculated mean vector.
class DifferenceFromTrueMeanVectorMeasureGenerator implements DifferenceFromGroundTruthMeasureGenerator {
    boolean initialized = false;
    Double knownGroundTruthMeansOfAllVariables = null;
    JointToSampler testerSampleMaker = null;

    public DifferenceFromTrueMeanVectorMeasureGenerator(Double knownGroundTruthMeansOfAllVariables) {
        this.knownGroundTruthMeansOfAllVariables = knownGroundTruthMeansOfAllVariables;
    }

    Integer numSamplesFromTesterToSimulateTrueDistribution = null; // (used only if ground truth means are not given)
    Long maxWaitingTimeForTesterToSimulateMillis = null; // (used only if ground truth means are not given)

    public DifferenceFromTrueMeanVectorMeasureGenerator(JointToSampler testerSampleMaker,
                                                        int numSamplesFromTesterToSimulateTrueDistribution,
                                                        long maxWaitingTimeForTesterToSimulateMillis) {
        this.testerSampleMaker = testerSampleMaker;
        this.numSamplesFromTesterToSimulateTrueDistribution = numSamplesFromTesterToSimulateTrueDistribution;
        this.maxWaitingTimeForTesterToSimulateMillis = maxWaitingTimeForTesterToSimulateMillis;
    }

    double[] groundTruthMeans = null;

    @Override
    public void initialize(RichJointWrapper jointWrapper) {
//                           int numSamplesFromTesterToSimulateTrueDistribution,
//                           long maxWaitingTimeForTesterToSimulateMillis) {
        if (knownGroundTruthMeansOfAllVariables != null) {
            groundTruthMeans = new double[jointWrapper.getAppropriateSampleVectorSize()];//[jointWrapper.getJoint().getScopeVars().size()];
            Arrays.fill(groundTruthMeans, knownGroundTruthMeansOfAllVariables);
            System.out.println("known groundTruthMeans = " + Arrays.toString(groundTruthMeans));
        } else { //ground truth mean is not given...
//            try {
            groundTruthMeans = computeGroundTruthMean(jointWrapper,
                    testerSampleMaker,
                    numSamplesFromTesterToSimulateTrueDistribution, maxWaitingTimeForTesterToSimulateMillis);
            System.out.println("{{{groundTruthMeans = " + Arrays.toString(groundTruthMeans) + "}}}");
//            } catch (Exception e) {
//                e.printStackTrace();
//                System.err.println("NO GROUND TRUTH TAKEN! TERMINATED....");
//                continue;
//            }
        }

        initialized = true;
    }

    @Override
    public MeasureOnTheRun<Double[]> generateMeasure() {
        if (!initialized) throw new RuntimeException("not initialized");

        return new AbsDifferenceMeasure(groundTruthMeans);

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public double[] computeGroundTruthMean(
            RichJointWrapper jointWrapper, JointToSampler samplerMaker,
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
            sample = jointWrapper.reusableQueriedVarValues(sample); //SingleMCMCChainAnalysis.pruneNulls(sample);

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
}

//______________________________________________

//when receiving a sample [q_11, q_12, ...q_n1, q_n2] it assumes that the expected value of each q_i1 and q_i2 should be equal
class QueryPairsEqualMeasureGenerator implements DifferenceFromGroundTruthMeasureGenerator {
    int sampleLength = -1;

    @Override
    public void initialize(RichJointWrapper jointWrapper) {
        sampleLength = jointWrapper.getAppropriateSampleVectorSize();
        if (sampleLength % 2 != 0)
            throw new RuntimeException("even number of query vars expelcted so that every consequent pairs are expected to share a same expected value");
    }

    @Override
    public MeasureOnTheRun<Double[]> generateMeasure() {
        return new MeasureOnTheRun<Double[]>() {
            double[] runningAccumulatedSample = new double[sampleLength];
            int takenSamples = 0;


            @Override
            public void addNewValue(Double[] sample) {
                if (sample.length != sampleLength)
                    throw new RuntimeException("size mismatch. sample=" + Arrays.toString(sample) + " but the given query sample length=" + sampleLength);

                for (int i = 0; i < sampleLength; i++) {
                    runningAccumulatedSample[i] = runningAccumulatedSample[i] + sample[i];
                }

                takenSamples++;
            }


            @Override
            public double computeMeasure() { //worst absolute difference of (consecutive expected values)
                Double runWorstErr = null;  //running worst error
                for (int i = 0; i < sampleLength / 2; i++) {
                    double oneExpectedQ = runningAccumulatedSample[2 * i] / (double) takenSamples;
                    double otherExpectedQ = runningAccumulatedSample[2 * i + 1] / (double) takenSamples;
                    double thisErr = Math.abs(oneExpectedQ - otherExpectedQ);
                    if (runWorstErr == null) {
                        runWorstErr = thisErr;       //for first entry
                    }else if (thisErr > runWorstErr){
                        runWorstErr = thisErr;
                    }
                }
                runWorstErr /= (double) (sampleLength / 2);
                return runWorstErr;
            }
        };//end measure
    }
}





