package hgm.poly.diagnostics;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Hadi Afshar.
 * Date: 4/09/14
 * Time: 7:21 PM
 */
public class MultiArrayMultiStatFlexibleIndex {
    private List<Long> hallmarkTimeStampsInNano = null;
//    private List<Double> meanErrForTimes = null;
//    private double[] exp2ErrForTimes = null;
    MultiArrayMultiStatistics innerMultiMeasure;

    public MultiArrayMultiStatFlexibleIndex(int upperBoundOnTimePointsNum /*numTimePoints*/,
                                            int numRunsPerAlgorithm) {
        innerMultiMeasure = new MultiArrayMultiStatistics(upperBoundOnTimePointsNum, numRunsPerAlgorithm);
    }

    public List<Long> getHallmarkTimeStampsInNano() {
        return hallmarkTimeStampsInNano;
    }

    public void addNewValue(List<Long> recordedTimePointsInNano, List<Double> errVsTimes) {
        if (hallmarkTimeStampsInNano == null) {
            hallmarkTimeStampsInNano = recordedTimePointsInNano; //so the times points of the first algorithm-run are the hall marks...
//            meanErrForTimes = new ArrayList<Double>(errVsTimes); //means of a single elements = same single elements\
//            exp2ErrForTimes = new double[meanErrForTimes.size()];
//            for (int i = 0; i < errVsTimes.size(); i++) {
//                Double errVsTime = errVsTimes.get(i);
//                exp2ErrForTimes[i] = errVsTime * errVsTime;
//            }
            innerMultiMeasure.addNewValue(errVsTimes);
        } else if (!recordedTimePointsInNano.isEmpty()) { //E[X], E[X^2] of 'means vs Times' should be updated
            Double[] newDataWithAdjustedIndex = new Double[hallmarkTimeStampsInNano.size()];
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
                Double errAssociatedWithNearestTime = errVsTimes.get(index2);
                newDataWithAdjustedIndex[index1] = errAssociatedWithNearestTime;
//                meanErrForTimes.set(index1, ((meanErrForTimes.get(index1) * runNum) + errAssociatedWithNearestTime) / (double) (runNum + 1));//mean is updated with the closest new time
//                exp2ErrForTimes[index1] = ((exp2ErrForTimes[index1] * runNum) + errAssociatedWithNearestTime * errAssociatedWithNearestTime) / (double) (runNum + 1);
            }
            innerMultiMeasure.addNewValue(Arrays.asList(newDataWithAdjustedIndex));
        }   else throw new RuntimeException("what is going on?");
    }

    //////////////////////////////////////////////////////////////////

    public List<Double> computeMean() {
       return innerMultiMeasure.computeMean();
    }

    public List<Double> computeCorrectedVariance() {
       return innerMultiMeasure.computeCorrectedVariance();
    }

    public List<Double> computeVariance() {
        return innerMultiMeasure.computeVariance();
    }


    public List<Double> computeCorrectedStdErr() {
        return innerMultiMeasure.computeCorrectedStdErr();
    }

    public int numberOfChains() {
        return innerMultiMeasure.numberOfChains();
    }
}
