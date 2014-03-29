package hgm.preference.db;

import hgm.preference.Choice;
import hgm.preference.Preference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Created by Hadi Afshar.
 * Date: 13/01/14
 * Time: 4:02 PM
 */
public class DummyFeasiblePreferenceDatabase implements PreferenceDatabase {
    private double minAttribBound;
    private double maxAttribBound;
    private int attributeCount;
    private Random random;
    private List<Preference> preferences;
    private List<Double[]> items;
    double[] auxiliaryWeightVector;

    public DummyFeasiblePreferenceDatabase(double minWeightBound, double maxWeightBound,
                                           double minAttribBound, double maxAttribBound, int preferenceCount, int attributeCount, int itemCount) {

        this.minAttribBound = minAttribBound;
        this.maxAttribBound = maxAttribBound;
        this.attributeCount = attributeCount;
        items = new ArrayList<Double[]>(itemCount);
        preferences = new ArrayList<Preference>(preferenceCount);
        random = new Random();

        // making items:
        for (int itemIndex = 0; itemIndex < itemCount; itemIndex++) {
            items.add(makeNewItem());
        }

        auxiliaryWeightVector = makeAuxiliaryWeightVector(minWeightBound, maxWeightBound, attributeCount);

        //making preferences;
        for (int i = 0; i < preferenceCount; i++) {
            int itemIndex1 = random.nextInt(itemCount);
            int itemIndex2;
            do {
                itemIndex2 = random.nextInt(itemCount);
            } while (itemIndex2 == itemIndex1);
            double xW1 = utility(items.get(itemIndex1), auxiliaryWeightVector);
            double xW2 = utility(items.get(itemIndex2), auxiliaryWeightVector);

            if (xW1 > xW2) preferences.add(new Preference(itemIndex1, itemIndex2, Choice.FIRST));
            else if (xW1 < xW2) preferences.add(new Preference(itemIndex1, itemIndex2, Choice.SECOND));
            else preferences.add(new Preference(itemIndex1, itemIndex2, Choice.EQUAL));
        }
    }

    public List<Double[]> getItems() {
        return items;
    }

    //return summation w_i*x_i
    private double utility(Double[] itemVector, double[] weightVector) {
        assert itemVector.length == weightVector.length;
        double result = 0d;
        for (int i = 0; i < itemVector.length; i++) {
            result += (itemVector[i] * weightVector[i]);
        }
        return result;
    }

    private double[] makeAuxiliaryWeightVector(double minWeightBound, double maxWeightBound, int attributeCount) {
        double[] v = new double[attributeCount];
        for (int i = 0; i < v.length; i++) {
            v[i] = random.nextDouble() * (maxWeightBound - minWeightBound) + minWeightBound;
        }
        return v;
    }

    public Double[] makeNewItem() {
        Double[] item = new Double[attributeCount];
        for (int i = 0; i < attributeCount; i++) {
            item[i] = random.nextDouble() * (maxAttribBound - minAttribBound) + minAttribBound;
        }
        return item;
    }

    @Override
    public int getNumberOfAttributes() {
        return items.get(0).length;
    }

    @Override
    public int getNumberOfItems() {
        return items.size();
    }

    @Override
    public List<Preference> getPreferenceResponses() {
        return preferences;
    }

    @Override
    public Double[] getItemAttributeValues(int itemId) {
        return items.get(itemId);
    }

    @Override
    public double[] getAuxiliaryWeightVector() {
        return auxiliaryWeightVector;
    }

    @Deprecated
    //for debugging:
    public void test() {
//        System.out.println("auxiliaryWeightVector = " + Arrays.toString(auxiliaryWeightVector));
        for (Preference pref : preferences) {
            Integer aId = pref.getItemId1();
            Integer bId = pref.getItemId2();
            Double[] a = getItemAttributeValues(aId);
            Double[] b = getItemAttributeValues(bId);

            double utilA = utility(a, auxiliaryWeightVector);
            double utilB = utility(b, auxiliaryWeightVector);
            if (utilA - utilB > 0) {
                if (!pref.getPreferenceChoice().equals(Choice.FIRST))
                    throw new RuntimeException("not >  !!!");
            } else if (utilB - utilA > 0) {
                if (!pref.getPreferenceChoice().equals(Choice.SECOND))
                    throw new RuntimeException("not <  !!!");
            } else {
                if (!pref.getPreferenceChoice().equals(Choice.EQUAL))
                    throw new RuntimeException("not =  !!!");
            }
        }
    }
}
