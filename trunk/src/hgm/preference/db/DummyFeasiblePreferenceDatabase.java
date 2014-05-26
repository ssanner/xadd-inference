package hgm.preference.db;

import hgm.poly.bayesian.PriorHandler;
import hgm.preference.Choice;
import hgm.preference.Preference;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Hadi Afshar.
 * Date: 13/01/14
 * Time: 4:02 PM
 */
public class DummyFeasiblePreferenceDatabase extends PreferenceDatabase {
    private double minAttribBound;
    private double maxAttribBound;
    //    private int attributeCount;  is dim = num. parameters
    private Random random;
    private List<Preference> preferences;
    private List<Double[]> items;
    double[] auxiliaryWeightVector;

    public DummyFeasiblePreferenceDatabase(double minAttribBound, double maxAttribBound,
                                           int preferenceCount, //num. observed data
                                           PriorHandler priorOnAttribWeights,
                                           int itemCount) {
        this(minAttribBound, maxAttribBound, preferenceCount, priorOnAttribWeights, itemCount, 0.0);
    }

    public DummyFeasiblePreferenceDatabase(double minAttribBound, double maxAttribBound,
                                           int preferenceCount, //num. observed data
                                           PriorHandler priorOnAttribWeights,
                                           int itemCount, double noise) {
        super(priorOnAttribWeights);

        this.minAttribBound = minAttribBound;
        this.maxAttribBound = maxAttribBound;
//        this.attributeCount = attributeCount;
        items = new ArrayList<Double[]>(itemCount);
        preferences = new ArrayList<Preference>(preferenceCount);
        random = new Random();

        // making items:
        for (int itemIndex = 0; itemIndex < itemCount; itemIndex++) {
            items.add(makeNewItem());
        }

        auxiliaryWeightVector = makeAuxiliaryWeightVector();

        //making preferences;
        for (int i = 0; i < preferenceCount; i++) {
            int itemIndex1 = random.nextInt(itemCount);
            int itemIndex2;
            do {
                itemIndex2 = random.nextInt(itemCount);
            } while (itemIndex2 == itemIndex1);
            double xW1 = utility(items.get(itemIndex1), auxiliaryWeightVector);
            double xW2 = utility(items.get(itemIndex2), auxiliaryWeightVector);

            Preference preference;
            if (xW1 > xW2) preference = new Preference(itemIndex1, itemIndex2, Choice.FIRST);
            else if (xW1 < xW2) preference = new Preference(itemIndex1, itemIndex2, Choice.SECOND);
            else preference = new Preference(itemIndex1, itemIndex2, Choice.EQUAL);

            //flip with probability = noise.
            if (random.nextDouble()< noise) {
                preference.flipChoice();
            }


            preferences.add(preference);
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

    private double[] makeAuxiliaryWeightVector() {
        double[] lowerBoundsPerDim = prior.getLowerBoundsPerDim();
        double[] upperBoundsPerDim = prior.getUpperBoundsPerDim();

        double[] v = new double[this.getNumberOfParameters()];
        for (int i = 0; i < v.length; i++) {
            double minWeightBound = lowerBoundsPerDim[i];
            double maxWeightBound = upperBoundsPerDim[i];
            v[i] = random.nextDouble() * (maxWeightBound - minWeightBound) + minWeightBound;
        }
        return v;
    }

    public Double[] makeNewItem() {
        Double[] item = new Double[this.getNumberOfParameters()];
        for (int i = 0; i < item.length; i++) {
            item[i] = random.nextDouble() * (maxAttribBound - minAttribBound) + minAttribBound;
        }
        return item;
    }

    @Override
    public int getNumberOfParameters() {
        return this.prior.getFactory().numberOfVars();//items.get(0).length;
    }

    @Override
    public int getNumberOfItems() {
        return items.size();
    }

    @Override
    public List<Preference> getObservedDataPoints() {
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
