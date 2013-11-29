package hgm.asve.cnsrv.approxator.regression;

import hgm.asve.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Hadi Afshar.
 * Date: 27/10/13
 * Time: 6:28 PM
 */
public class SamplingDB {  //todo, keeping the list of variables for each valuation is costly. Instead, a tabular approach with a map interface is required.
    private List<Map<String, Double>> continuousVarAssignSamples;
    private List<Double> targetValues;

    public SamplingDB() {
        continuousVarAssignSamples = new ArrayList<Map<String, Double>>();
        targetValues = new ArrayList<Double>();
    }

    public void addSamplingInfo(HashMap<String, Double> continuousAssignment, Double target) {
        continuousVarAssignSamples.add(continuousAssignment);  //todo: [IMPORTANT] what if the current sample already exists in the region DB? does it end in 0 determinant?
        //todo: What happens if two samples have same assignments and different targets? in particular this may happen if two regions differ in a decision based on a boolean variable. They collapse on each other(?)
        targetValues.add(target);
    }

    public List<Map<String, Double>> getSamples() {
        return continuousVarAssignSamples;
    }

    public List<Double> getTargets() {
        return targetValues;
    }

    public int size() {
        return targetValues.size(); //which is equal to assign. samples size.
    }

    public Pair<Map<String, Double>, Double> getElement(int index) {
        return new Pair<Map<String, Double>, Double>(continuousVarAssignSamples.get(index), targetValues.get(index));
    }

    public void addAllSamplingInfo(SamplingDB otherDB) {
        this.continuousVarAssignSamples.addAll(otherDB.continuousVarAssignSamples);
        this.targetValues.addAll(otherDB.targetValues);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SamplingDB: <");
        for (int i = 0; i < size(); i++) {
            sb.append(continuousVarAssignSamples.get(i)).append(": ").append(targetValues.get(i)).append("\n");
        }
        sb.append(">");
        return sb.toString();
    }

    public boolean isEmpty() {
        return targetValues.isEmpty();
    }

    public static SamplingDB unionOfSamplingDBs(SamplingDB... dbs) {
        SamplingDB union = new SamplingDB();
        for (SamplingDB db : dbs) {
            union.addAllSamplingInfo(db);
        }
        return union;
    }
}
