package hgm.preference.db;

import hgm.preference.Preference;

import java.util.ArrayList;
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

            if (xW1 > xW2) preferences.add(new Preference(itemIndex1, itemIndex2, Preference.Choice.FIRST));
            else if (xW1 < xW2) preferences.add(new Preference(itemIndex1, itemIndex2, Preference.Choice.SECOND));
            else preferences.add(new Preference(itemIndex1, itemIndex2, Preference.Choice.EQUAL));
        }
    }

    //return summation w_i*x_i
    private double utility(Double[] itemVector, double[] weightVector) {
        assert itemVector.length == weightVector.length;
        double result = 0;
        for (int i = 0; i < itemVector.length; i++) {
            result += itemVector[i] * weightVector[i];
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

    private Double[] makeNewItem() {
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
}
