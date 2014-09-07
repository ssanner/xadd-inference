package hgm.poly.reports.sg;

import hgm.poly.Fraction;
import hgm.poly.PiecewiseExpression;
import hgm.poly.PolynomialFactory;
import hgm.poly.gm.*;
import hgm.poly.sampling.SamplerInterface;
import hgm.poly.sampling.frac.*;

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
@Deprecated
public class SymbolicGibbsTesterForErrorVsTimeAnalysis {

    public static final String REPORT_PATH = "E:/REPORT_PATH_AAAI15/symmetric/";

    public static void main(String[] args) throws IOException {
        SymbolicGibbsTesterForErrorVsTimeAnalysis instance = new SymbolicGibbsTesterForErrorVsTimeAnalysis();
//        instance.symmetricBowlDistributionProblemTest();
        instance.symmetricCollisionDistributionProblemTest();
    }

    public void symmetricCollisionDistributionProblemTest() throws IOException {
        System.out.println("REPORT_PATH = " + REPORT_PATH);

        List<JointToSampler> samplerMakersToBeTested = Arrays.asList(
//                FractionalJointBaselineGibbsSampler.makeJointToSampler(),
                FractionalJointRejectionSampler.makeJointToSampler(1),
                FractionalJointSymbolicGibbsSampler.makeJointToSampler(),
                FractionalJointMetropolisHastingSampler.makeJointToSampler(5.0)
//                ,FractionalJointSelfTunedMetropolisHastingSampler.makeJointToSampler(10, 100, 100)
        );
//        int[] numParams = {3};
        int samplingTimeMillis = 1000 * 5;
        int approxNumTimePointsForWhichErrIsPersisted = 1000;
        int numRuns = 1;
        int burnedSamples = 50;

        int numCollidingObjects = 14;

        double muNuBeta = 0.5; //todo
        double observedPt = muNuBeta*muNuBeta*numCollidingObjects/5d;
        JointWrapper jointWrapper = makeSymmetricCollisionJointWrapper(numCollidingObjects, muNuBeta, observedPt);
        System.out.println("jointWrapper = " + jointWrapper.joint);
        double[] groundTruthMean = new double[numCollidingObjects*2]; //since for each object, velocity and mass are stochastic

//        Arrays.fill(groundTruthMean, Math.sqrt(observedPt/(double)numCollidingObjects));
        Arrays.fill(groundTruthMean, 0);

        System.out.println("groundTruthMean = " + Arrays.toString(groundTruthMean));


        testSamplersErrorVsTime(jointWrapper,
                groundTruthMean,
                samplerMakersToBeTested,
                samplingTimeMillis,
                approxNumTimePointsForWhichErrIsPersisted, numRuns, burnedSamples, REPORT_PATH);

        System.out.println(" That was all the folk. ");
    }

    public void symmetricBowlDistributionProblemTest() throws IOException {
        System.out.println("REPORT_PATH = " + REPORT_PATH);

        List<JointToSampler> samplerMakersToBeTested = Arrays.asList(
                FractionalJointSymbolicGibbsSampler.makeJointToSampler(),
                FractionalJointBaselineGibbsSampler.makeJointToSampler(),
                FractionalJointRejectionSampler.makeJointToSampler(1),
                FractionalJointMetropolisHastingSampler.makeJointToSampler(5.0),
                FractionalJointSelfTunedMetropolisHastingSampler.makeJointToSampler(10, 100, 100)
        );
//        int[] numParams = {3};
        int samplingTime = 1000 * 10;
        int approxNumTimePointsForWhichErrIsPersisted = 33;
        int numRuns = 2;
        int burnedSamples = 50;

        int numVars = 2;
        int radius = 1;
        int halfPower = 1;
        JointWrapper jointWrapper = makeSymmetricBowlJointWrapper(numVars, radius, halfPower);
        double[] groundTruthMean = new double[numVars];
        System.out.println("groundTruthMean = " + Arrays.toString(groundTruthMean));


        testSamplersErrorVsTime(jointWrapper,
                groundTruthMean,
                samplerMakersToBeTested,
                samplingTime,
                approxNumTimePointsForWhichErrIsPersisted, numRuns, burnedSamples, REPORT_PATH);

        System.out.println(" That was all the folk. ");

    }

    private JointWrapper makeSymmetricCollisionJointWrapper(int n /*num colliding objects*/,
                                                            Double muNuBeta, Double observedTotalMomentum) {
        if (observedTotalMomentum >= muNuBeta*muNuBeta*n) throw new RuntimeException("observed total momentum must be < " + muNuBeta*muNuBeta*n);
        if (observedTotalMomentum <= 0) throw new RuntimeException("observed total momentum must be positive");

        // both m_i and v_i ~ U(0, muNuBeta)

        // m_1      v_1   v_n     m_2
        //   \__p_1__/    \__p_n__/
        //       \_____p_t____/


        String[] vars = new String[3 * n + 3];
        for (int i = 0; i < n; i++) {
            vars[3 * i] = "m_" + (i + 1);     //mass of i-th object
            vars[3 * i + 1] = "v_" + (i + 1); //velocity of i-th object
            vars[3 * i + 2] = "p_" + (i + 1); //momentum of i-th object
        }
        vars[3 * n] = "m_t"; //total mass
        vars[3 * n + 1] = "v_t"; //total velocity
        vars[3 * n + 2] = "p_t"; //total momentum


        PolynomialFactory factory = new PolynomialFactory(vars);
        Distributions dBank = new Distributions(factory);

        BayesNetGraphicalModel bn = new BayesNetGraphicalModel();

        PiecewiseExpression<Fraction>[] massesF = new PiecewiseExpression[n];
        PiecewiseExpression<Fraction>[] velocitiesF = new PiecewiseExpression[n];
        Fraction[] momentaF = new Fraction[n];
        String totalMomentumFormula = "";
        String totalMassFormula = "";

//        Double muNuAlpha = 0d; //todo
        Double muNuAlpha = -muNuBeta; //todo

        for (int i = 0; i < n; i++) {
            massesF[i] = dBank.createUniformDistributionFraction("m_" + (i + 1), muNuAlpha.toString(), muNuBeta.toString());
            velocitiesF[i] = dBank.createUniformDistributionFraction("v_" + (i+1), muNuAlpha.toString(), muNuBeta.toString());
            momentaF[i] = factory.makeFraction("m_" + (i + 1) + "^(1) * v_" + (i + 1) + "^(1)");
            totalMomentumFormula += ("p_" + (i + 1) + "^(1) +");
            totalMassFormula += ("m_" + (i + 1) + "^(1) +");
        }
        totalMomentumFormula = totalMomentumFormula.substring(0, totalMomentumFormula.length() - 1); //removing last "+"
        totalMassFormula = totalMassFormula.substring(0, totalMassFormula.length() - 1); //removing last "+"

        Fraction ptF = factory.makeFraction(totalMomentumFormula); // m_1^(1)*v_1^(1) + m_2^(1)*v_2^(1) + ...
        Fraction mtF = factory.makeFraction(totalMassFormula);
        Fraction vtF = factory.makeFraction("[p_t^(1)]/[m_t^(1)]");

        for (int i = 0; i < n; i++) {
            bn.addFactor(new StochasticVAFactor("m_" + (i + 1), massesF[i])); //mass
            bn.addFactor(new StochasticVAFactor("v_" + (i + 1), velocitiesF[i])); //mass 2
            bn.addFactor(new DeterministicFactor("p_" + (i + 1), momentaF[i])); //momentum
        }

        bn.addFactor(new DeterministicFactor("p_t", ptF)); //total momentum
        bn.addFactor(new DeterministicFactor("m_t", mtF)); //total mass (after collision)
        bn.addFactor(new DeterministicFactor("v_t", vtF)); //total velocity (after collision)

        Map<String, Double> evidence = new HashMap<String, Double>();
        evidence.put("p_t", observedTotalMomentum);

        SymbolicGraphicalModelHandler handler = new SymbolicGraphicalModelHandler();
        List<String> queryVars = new ArrayList<String>();
        queryVars.addAll(Arrays.asList(PolynomialFactory.makeIndexedVars("v", 1, n)));
        queryVars.addAll(Arrays.asList(PolynomialFactory.makeIndexedVars("m", 1, n)));
        PiecewiseExpression<Fraction> joint = handler.makeJoint(bn, queryVars, evidence);

        return new JointWrapper(joint, muNuAlpha - 0.1, muNuBeta + 0.1);
    }

    private JointWrapper makeSymmetricBowlJointWrapper(int numVars, double radius, int halfPower) {
        String[] vars = PolynomialFactory.makeIndexedVars("x", 1, numVars);
        PolynomialFactory factory = new PolynomialFactory(vars);
        Distributions dBank = new Distributions(factory);

        PiecewiseExpression<Fraction> joint = dBank.createBowlDistributionFraction(vars, radius, halfPower);
        return new JointWrapper(joint, -radius -0.0001, radius+0.0001);
    }

    private void testSamplersErrorVsTime(
            JointWrapper jointWrapper,
            double[] groundTruthMeans,
            List<JointToSampler> samplerMakersToBeTested,
            long desiredSamplingTime,
            int approxNumTimePointsForWhichErrIsPersisted,
            int numRuns,
            int burnedSamples, //used for both tester and tested algorithms
            String outputDirectoryPath) throws IOException {

        //Analysis of tested algorithms:
        for (JointToSampler samplerMaker : samplerMakersToBeTested) {
            ErrVsTimeInfo errVsTimeInfo = meansAndStdErrorsForErrVsTimeTest(numRuns,
                    groundTruthMeans,
                    jointWrapper,
                    samplerMaker,
                    burnedSamples,
                    desiredSamplingTime,
                    approxNumTimePointsForWhichErrIsPersisted);
            errVsTimeInfo.persistMeanStdErrForTimePoints(outputDirectoryPath, groundTruthMeans.length/*param*/, samplerMaker.getName(), desiredSamplingTime);

            System.out.println(samplerMaker.getName());
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public ErrVsTimeInfo meansAndStdErrorsForErrVsTimeTest(
            int numRunsPerAlgorithm,
            double[] groundTruthMeanVector,
            JointWrapper jointWrapper,
            JointToSampler samplerMaker,
            int burnedSamples,
            long minDesiredSamplingTimeMillis,
            int approxNumTimePointsForWhichErrIsPersisted) {

        double rootNumRuns = Math.sqrt(numRunsPerAlgorithm);

        List<Long> hallmarkTimeStampsInNano = null;
        List<Double> meanErrForTimes = null;
        double[] exp2ErrForTimes = null;

        for (int runNum = 0; runNum < numRunsPerAlgorithm; runNum++) {
            System.out.println("ALG: " + samplerMaker.getName() + " -- ITR. = " + runNum);

            Err4times err4times = errorVsTime(groundTruthMeanVector, jointWrapper, samplerMaker, burnedSamples,
                    minDesiredSamplingTimeMillis, approxNumTimePointsForWhichErrIsPersisted);

            //means and std-errors for "errors in time stamps":
            List<Long> recordedTimePointsInNano = err4times.recordedTimePointsInNano;
            List<Double> errVsTimes = err4times.errVsTimes;
            if (hallmarkTimeStampsInNano == null) {
                hallmarkTimeStampsInNano = recordedTimePointsInNano; //so the times points of the first algorithm-run are the hall marks...
                meanErrForTimes = errVsTimes; //means of a single elements = same single elements\
                exp2ErrForTimes = new double[meanErrForTimes.size()];
                for (int i = 0; i < errVsTimes.size(); i++) {
                    Double errVsTime = errVsTimes.get(i);
                    exp2ErrForTimes[i] = errVsTime * errVsTime;
                }
            } else if (!recordedTimePointsInNano.isEmpty()) { //E[X], E[X^2] of 'means vs Times' should be updated
                int index2 = 0;
                for (int index1 = 0; index1 < hallmarkTimeStampsInNano.size(); index1++) {
                    Long hallMarkTime = hallmarkTimeStampsInNano.get(index1);
                    for (int i = index2; i < recordedTimePointsInNano.size(); i++) {
                        Long newT1 = recordedTimePointsInNano.get(i);
                        Long newT2 = (i == recordedTimePointsInNano.size() - 1) ? newT1 : recordedTimePointsInNano.get(i + 1);
                        long deltaT1 = Math.abs(newT1 - hallMarkTime);
                        long deltaT2 = Math.abs(newT2 - hallMarkTime);
                        if (deltaT1 <= deltaT2) {
                            index2 = i; //so that next time search is started from here
                            break;
                        }
                    }
//                    long nearestTime = recordedTimePointsInNano.get(index2);
                    Double errAssociatedWithNearestTime = errVsTimes.get(index2);
                    meanErrForTimes.set(index1, ((meanErrForTimes.get(index1) * runNum) + errAssociatedWithNearestTime) / (double) (runNum + 1));//mean is updated with the closest new time
                    exp2ErrForTimes[index1] = ((exp2ErrForTimes[index1] * runNum) + errAssociatedWithNearestTime * errAssociatedWithNearestTime) / (double) (runNum + 1);
                }
            }

        } //end alg. run num.

        Double[] stdErrs4Times = new Double[meanErrForTimes.size()];
        for (int i = 0; i < meanErrForTimes.size(); i++) {
            double mean = meanErrForTimes.get(i);
            stdErrs4Times[i] = Math.sqrt(exp2ErrForTimes[i] - mean * mean) / rootNumRuns;
        }

        return new ErrVsTimeInfo(meanErrForTimes, Arrays.asList(stdErrs4Times), hallmarkTimeStampsInNano, numRunsPerAlgorithm);

        /*    //mean and stdErr for all Iterations
            double mean = 0d;  //E[X]
            double ex2 = 0;  //E[X^2]
            for (int fId = 0; fId < numFs; fId++) {
                double x = runningErrorPerF[fId];
                mean += (x / (double) numFs);
                ex2 += (x * x / (double) numFs);
            }
            double stdErr = root(ex2 - mean * mean) / rootNumRuns;
*/

    }

    public Err4times errorVsTime(
            double[] groundTruthMeanVector, //of size #dims
            JointWrapper db,
            JointToSampler samplerMaker,
            int burnedSamples,
            long desiredSamplingTimeMillis,
            int approxNumTimePointsForWhichErrIsPersisted) {

        //burned samples:
        SamplerInterface sampler = samplerMaker.makeSampler(db.joint, db.minVarLimit, db.maxVarLimit);
        for (int i = 0; i < burnedSamples; i++) {
            sampler.reusableSample(); //discard samples...
        }

        double[] runningAccumulatedSample = null; //since maybe I do not know the dim yet...(if no burned sample is taken)

        List<Long> recordedTimePointsInNano = new ArrayList<Long>(approxNumTimePointsForWhichErrIsPersisted);
        List<Double> errVsTimes = new ArrayList<Double>(approxNumTimePointsForWhichErrIsPersisted);

        //trying to take the desired number of samples...
        long absoluteStartTimeMillis = System.currentTimeMillis();
        long absoluteStartTimeNanos = System.nanoTime();

        int savedTimePoints = 0;
        int takenSamples = 0;

        boolean samplingPerformedInIntendedTimeSuccessfully;

        for (; ; ) {
            Double[] sample = sampler.reusableSample();
            sample = pruneNulls(sample);
            takenSamples++;

            if (runningAccumulatedSample == null) {
                runningAccumulatedSample = new double[sample.length];
            }

            for (int i = 0; i < sample.length; i++) {
                runningAccumulatedSample[i] = runningAccumulatedSample[i] + sample[i];
            }

            //sum_i (absolute difference of (average of taken_sample_i and ground_truth_i)):
            double runErr = 0;  //running error
            for (int i = 0; i < sample.length; i++) {
                runErr += Math.abs((runningAccumulatedSample[i] / (double) takenSamples) - groundTruthMeanVector[i]);
            }
            runErr /= (double) sample.length;

            //samples vs. time:
            long nanosFromStart = System.nanoTime() - absoluteStartTimeNanos;
            if ((nanosFromStart >= (double) (((long) savedTimePoints) * desiredSamplingTimeMillis * 1000000) / (double) (approxNumTimePointsForWhichErrIsPersisted + 1))
                    && nanosFromStart <= desiredSamplingTimeMillis * 1000000) {
                savedTimePoints++;
                errVsTimes.add(runErr);
                recordedTimePointsInNano.add(System.nanoTime() - absoluteStartTimeNanos);
            }

            //successful termination:
            if (System.currentTimeMillis() - absoluteStartTimeMillis >= desiredSamplingTimeMillis // && savedTimePoints >= approxNumTimePointsForWhichErrIsPersisted
                    ) {
//                samplingPerformedInIntendedTimeSuccessfully = true;
                System.out.println("successfull after " + takenSamples + " samples.");
                break;
            }

        }//end for loop

        return new Err4times(errVsTimes, recordedTimePointsInNano);
    }

    class Err4times {
        List<Double> errVsTimes;
        List<Long> recordedTimePointsInNano;

        Err4times(List<Double> errVsTimes, List<Long> recordedTimePointsInNano) {
            this.errVsTimes = errVsTimes;
            this.recordedTimePointsInNano = recordedTimePointsInNano;
        }
    }


    class ErrVsTimeInfo {
        //calculated info:
        List<Double> means4TimePoints;
        List<Double> stdErrs4TimePoints;
        List<Long> recordedTimePointsInNano;

        int numIterationsForEachAlgorithm;

        public ErrVsTimeInfo(List<Double> meanOfErrorVsTime,
                             List<Double> stdErrOfErrorVsTime,
                             List<Long> recordedTimePointsInNano,
                             int numIterationsForEachAlgorithm) {
            this.numIterationsForEachAlgorithm = numIterationsForEachAlgorithm;

            this.means4TimePoints = meanOfErrorVsTime;
            this.stdErrs4TimePoints = stdErrOfErrorVsTime;
            this.recordedTimePointsInNano = recordedTimePointsInNano;

            if ((means4TimePoints.size() != stdErrs4TimePoints.size()) || (means4TimePoints.size() != this.recordedTimePointsInNano.size()))
                throw new RuntimeException("size mismatch");

        }

        private String generalInfo(int param, String algorithmName/*, int maxAllowedSamplingTime*/) {
            return "param" + param + "-itr" + numIterationsForEachAlgorithm /*+ "-maxT" + maxAllowedSamplingTime */ + "-" + algorithmName;
        }

        public void persistMeanStdErrForTimePoints(String path, int numParamDims, String algorithmName, long maxAllowedSamplingTime) throws FileNotFoundException {
            String outputFileName = path + generalInfo(numParamDims, algorithmName/*, maxAllowedSamplingTime*/) + "-times";// + means4TimePoints.size();

            PrintStream ps = new PrintStream(new FileOutputStream(outputFileName));

            for (int i = 0; i < means4TimePoints.size(); i++) {
                //          #index      #time.point(ms)         #mean       #stdErr
                ps.println((i + 1) + "\t" + recordedTimePointsInNano.get(i) /*/ 1000000*/ + "\t" + means4TimePoints.get(i) + "\t" + stdErrs4TimePoints.get(i));
            }

            ps.close();
        }
    }//end class Stat info

/*
    private class TotalTimeKeeper {
        String outputDirectoryPath;
        Map<String*/
/*algorithm*//*
, SortedSet<Pair<Integer */
/*data*//*
, Long */
/*totalTime*//*
>>> alg2dataTime;
        Map<String*/
/*algorithm*//*
, SortedSet<Pair<Integer */
/*dims*//*
, Long */
/*totalTime*//*
>>> alg2dimsTime;

        public TotalTimeKeeper(JointToSampler[] samplers, String outputDirectoryPath) {
            this.outputDirectoryPath = outputDirectoryPath;
            alg2dataTime = new HashMap<String, SortedSet<Pair<Integer, Long>>>(samplers.length);
            alg2dimsTime = new HashMap<String, SortedSet<Pair<Integer, Long>>>(samplers.length);
            for (JointToSampler sampler : samplers) {
                alg2dataTime.put(sampler.getName(), new TreeSet<Pair<Integer, Long>>());
            }
        }

        int[] dimsArray;
        String[] samplerNames;
        Map<String */
/*alg*//*
, Long*/
/*time*//*
>[] dimIndexDataIndexAlgTime;

        public TotalTimeKeeper(List<JointToSampler> samplers, int[] dimsArray*/
/*, int[] dataArray*//*
, String outputDirectoryPath) {
            this.outputDirectoryPath = outputDirectoryPath;
            this.dimsArray = new int[dimsArray.length];
            System.arraycopy(dimsArray, 0, this.dimsArray, 0, dimsArray.length);
            Arrays.sort(this.dimsArray);
            dimIndexDataIndexAlgTime = new HashMap[dimsArray.length];//[dataArray.length];
            this.samplerNames = new String[samplers.size()];
            for (int i = 0; i < samplers.size(); i++) {
                this.samplerNames[i] = samplers.get(i).getName();
            }
            for (int i = 0; i < dimsArray.length; i++) {
                dimIndexDataIndexAlgTime[i] = new HashMap<String, Long>(samplers.size());
            }
        }

        public void persist(int param, String samplerName, Long timeMillis) throws FileNotFoundException {
            int dimIndex = Arrays.binarySearch(dimsArray, param);
            if (dimIndexDataIndexAlgTime[dimIndex].put(samplerName, timeMillis) != null) {
                System.err.println("for dim: " + param + ", " + samplerName + " already exists! and will be replaced");
                System.out.println("dimsArray = " + Arrays.toString(dimsArray));
            }
            persistDataFix(samplerName*/
/*, dataIndex*//*
);
        }

        private void persistDataFix(String samplerName*/
/*, int dataIndex*//*
) throws FileNotFoundException {
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
    }

*/
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public double[] computeGroundTruthMean(
            JointWrapper jointWrapper, JointToSampler samplerMaker,
            final int numDesiredSamples,
            long maxWaitingTimeForTakingDesiredSamplesMillis //in ms
    ) {
        double[] mean = null;
        int numTakenSamples = 0;

        ///////////////////////////////////////////////////////////////////////
        SamplerInterface sampler = samplerMaker.makeSampler(jointWrapper.joint, jointWrapper.minVarLimit, jointWrapper.maxVarLimit);
        ///////////////////////////////////////////////////////////////////////

        long t1 = System.currentTimeMillis();
        for (int sampleCount = 0; sampleCount < numDesiredSamples; sampleCount++) {
            Double[] sample = sampler.reusableSample();
            sample = pruneNulls(sample);

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

    private Double[] pruneNulls(Double[] sample) {
        int nonNullEntryCount = 0;
        for (Double s : sample) {
            if (s != null) nonNullEntryCount++;
        }
        Double[] nonNullEntries = new Double[nonNullEntryCount];
        int i = 0;
        for (Double s : sample) {
            if (s != null) {
                nonNullEntries[i++] = s;
            }
        }
        return nonNullEntries;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    //none symmetric
    public GraphicalModel makeCollisionModel(int n /*num colliding objects*/,
                                             Double muAlpha, Double muBeta,
                                             Double nuAlpha, Double nuBeta) {
//                                                 double minVarLimit, double maxVarLimit,
//                                                 JointToSampler jointToSampler) {
        // m_1      v_1   v_n     m_2
        //   \__p_1__/    \__p_n__/
        //       \_____p_t____/


        String[] vars = new String[3 * n + 3];
        for (int i = 0; i < n; i++) {
            vars[3 * i] = "m_" + (i + 1);     //mass of i-th object
            vars[3 * i + 1] = "v_" + (i + 1); //velocity of i-th object
            vars[3 * i + 2] = "p_" + (i + 1); //momentum of i-th object
        }
        vars[3 * n] = "m_t"; //total mass
        vars[3 * n + 1] = "v_t"; //total velocity
        vars[3 * n + 2] = "p_t"; //total momentum


        PolynomialFactory factory = new PolynomialFactory(vars);
        Distributions dBank = new Distributions(factory);

        BayesNetGraphicalModel bn = new BayesNetGraphicalModel();

        PiecewiseExpression<Fraction>[] massesF = new PiecewiseExpression[n];
        PiecewiseExpression<Fraction>[] velocitiesF = new PiecewiseExpression[n];
        Fraction[] momentaF = new Fraction[n];
        String totalMomentumFormula = "";
        String totalMassFormula = "";
        for (int i = 0; i < n; i++) {
            massesF[i] = dBank.createUniformDistributionFraction("m_" + (i + 1), muAlpha.toString(), muBeta.toString());
            velocitiesF[i] = i == 0 ? dBank.createUniformDistributionFraction("v_1", nuAlpha.toString(), nuBeta.toString())
                    : dBank.createUniformDistributionFraction("v_" + (i + 1), nuAlpha.toString(), "v_" + i + "^(1)");
            momentaF[i] = factory.makeFraction("m_" + (i + 1) + "^(1) * v_" + (i + 1) + "^(1)");
            totalMomentumFormula += ("p_" + (i + 1) + "^(1) +");
            totalMassFormula += ("m_" + (i + 1) + "^(1) +");
        }
        totalMomentumFormula = totalMomentumFormula.substring(0, totalMomentumFormula.length() - 1); //removing last "+"
        totalMassFormula = totalMassFormula.substring(0, totalMassFormula.length() - 1); //removing last "+"

        Fraction ptF = factory.makeFraction(totalMomentumFormula); // m_1^(1)*v_1^(1) + m_2^(1)*v_2^(1) + ...
        Fraction mtF = factory.makeFraction(totalMassFormula);
        Fraction vtF = factory.makeFraction("[p_t^(1)]/[m_t^(1)]");

        for (int i = 0; i < n; i++) {
            bn.addFactor(new StochasticVAFactor("m_" + (i + 1), massesF[i])); //mass
            bn.addFactor(new StochasticVAFactor("v_" + (i + 1), velocitiesF[i])); //mass 2
            bn.addFactor(new DeterministicFactor("p_" + (i + 1), momentaF[i])); //momentum
        }

        bn.addFactor(new DeterministicFactor("p_t", ptF)); //total momentum
        bn.addFactor(new DeterministicFactor("m_t", mtF)); //total mass (after collision)
        bn.addFactor(new DeterministicFactor("v_t", vtF)); //total velocity (after collision)
        return bn;


//        SamplerInterface sampler = handler.makeSampler(bn, ("v_1 v_" + (n-1)).split(" "), //todo what about this?
//                evidence, minVarLimit, maxVarLimit, jointToSampler
//                FractionalJointBaselineGibbsSampler.makeJointToSampler()
//                FractionalJointRejectionSampler.makeJointToSampler(1)
//                SelfTunedFractionalJointMetropolisHastingSampler.makeJointToSampler(10, 30, 100)
//                FractionalJointMetropolisHastingSampler.makeJointToSampler(10)
//                SymbolicFractionalJointGibbsSampler.makeJointToSampler()
//        );

    }

    private class JointWrapper {
        PiecewiseExpression<Fraction> joint;
        double minVarLimit;
        double maxVarLimit;

        private JointWrapper(PiecewiseExpression<Fraction> joint, double minVarLimit, double maxVarLimit) {
            this.joint = joint;
            this.minVarLimit = minVarLimit;
            this.maxVarLimit = maxVarLimit;
        }
    }
}

