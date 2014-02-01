package hgm.preference;

import hgm.preference.db.PreferenceDatabase;
import hgm.preference.db.car.CarPreferenceDatabase;
import junit.framework.Assert;
import org.junit.Test;
import xadd.XADD;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 20/12/13
 * Time: 1:00 PM
 */
public class PreferenceLearningTest {
    public static void main(String[] args) {
        PreferenceLearningTest instance = new PreferenceLearningTest();
//        instance.testOnCar1stExperiment();
//        instance.testParametricVsNonParametricExpectedUtility();
        instance.testTimeVsNumConstraintsAndDimensions();

    }

    PreferenceDatabase testDB1 = new PreferenceDatabase() {
        Preference[] prefs = new Preference[]{
                new Preference(1, 2, Choice.FIRST),
                new Preference(1, 3, Choice.FIRST),
                new Preference(2, 3, Choice.FIRST),
        };

        List<Double[]> items = new ArrayList<Double[]>(5);

        {
            items.add(new Double[]{1.0, 0.1});
            items.add(new Double[]{2.0, 0.2});
            items.add(new Double[]{3.0, 0.3});
            items.add(new Double[]{4.0, 0.4});
            items.add(new Double[]{5.0, 0.5});
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
            return Arrays.asList(prefs);
        }

        @Override
        public Double[] getItemAttributeValues(int itemId) {
            return items.get(itemId);
        }

        @Override
        public double[] getAuxiliaryWeightVector() {
            return null;
        }
    }; //end inner class.

    @Test
    public void testBasic() {
        PreferenceLearning learning = new PreferenceLearning(new XADD(), testDB1, 0, "w", 0);

        // Pr(W | R^{n+1})
        XADD.XADDNode utilityWeights = learning.computePosteriorWeightVector(false);

        int maxExpUtilItem = learning.computeItemWithMaxExpectedUtilityVersion1(utilityWeights);
        System.out.println("maxExpUtilItem = " + maxExpUtilItem);
    }

    @Test
    public void testParametricVsNonParametricExpectedUtility() {
        PreferenceLearning learning = new PreferenceLearning(new XADD(), testDB1, 0, "w", 0);
        // Pr(W | R^{n+1})
        XADD.XADDNode utilityWeights = learning.computePosteriorWeightVector(false);

        int maxExpUtilItemCalculatedNonParametrically = learning.computeItemWithMaxExpectedUtilityVersion1(utilityWeights);
        System.out.println("maxExpUtilItemCalculatedNonParametrically = " + maxExpUtilItemCalculatedNonParametrically);

        int maxExpUtilItemCalculatedParametrically = learning.computeItemWithMaxExpectedUtilityVersion2(utilityWeights, "x");
        System.out.println("maxExpUtilItemCalculatedParametrically = " + maxExpUtilItemCalculatedParametrically);

        Assert.assertEquals(maxExpUtilItemCalculatedNonParametrically, maxExpUtilItemCalculatedParametrically);
    }

    @Test
    public void testOnCar1stExperiment() {
        int maxPrefs = 5;
        Set<Integer> advisers = new HashSet<Integer>();
        for (int i = 0; i < 1; i++) {
            advisers.add(i);
        }
        PreferenceDatabase db = CarPreferenceDatabase.fetchCarPreferenceDataBase1stExperiment(advisers);

        XADD context = new XADD();
        PreferenceLearning learning = new PreferenceLearning(context, db, 0, "w", 0);

        // Pr(W | R^{n+1})
        List<Preference> preferenceResponses = db.getPreferenceResponses();
        if (maxPrefs > 0) {
            preferenceResponses = preferenceResponses.subList(0, Math.min(maxPrefs, preferenceResponses.size()));
        }
        XADD.XADDNode utilityWeights = learning.computePosteriorWeightVector(false);
        context.getGraph(context._hmNode2Int.get(utilityWeights)).launchViewer();

        int maxExpUtilItem = learning.computeItemWithMaxExpectedUtilityVersion1(utilityWeights);
        Double[] item = db.getItemAttributeValues(0);
        double itemUtil = learning.expectedItemUtility(item, utilityWeights, true);
        System.out.println("itemUtil = " + itemUtil);

//        System.out.println("maxExpUtilItem = " + maxExpUtilItem);
    }

    /**
     * *********************************************
     * #Dimension and #Constraints vs. time
     * *********************************************
     */
    @Test
    public void testTimeVsNumConstraintsAndDimensions() {
        int numberOfItems = 15;
        for (int numDims = 1; numDims < 5; numDims++) {
            for (int numConstraints = 0; numConstraints<11; numConstraints++){
//        int numDims = 2;// features of each item
//            int numConstraints = 2; //number of (known) preferences
            PreferenceDatabase db = generateDummyPreferenceDatabase(0, 5, numConstraints, numDims, numberOfItems /*number of items*/);

            //

            PreferenceLearning learning = new PreferenceLearning(new XADD(), db, 0, "w", 0);

            long time1 = System.currentTimeMillis();
            // Pr(W | R^{n+1})
            XADD.XADDNode utilityWeights = learning.computePosteriorWeightVector(false);
            int maxExpUtilItem = learning.computeItemWithMaxExpectedUtilityVersion2(utilityWeights, "x");
//            System.out.println("maxExpUtilItem = " + maxExpUtilItem);
            long time2 = System.currentTimeMillis();
            System.out.println("[maxExpUtilItem = " + maxExpUtilItem + "] \t\t #Dim: " + numDims + " \t #Constraints: " + numConstraints + "\t Elapsed time: " + (time2 - time1));
            } // end numConstraints for
        } // end numDim for


    }

    public static PreferenceDatabase generateDummyPreferenceDatabase(final double minAttribBound, final double maxAttribBound, final int preferenceCount, final int attributeCount, final int itemCount) {
        final Random random = new Random();
        if (maxAttribBound < minAttribBound) throw new RuntimeException("bound mismatch");
        return new PreferenceDatabase() {

            List<Preference> preferences = new ArrayList<Preference>(preferenceCount);
            List<Double[]> items = new ArrayList<Double[]>(itemCount);

            {
                for (int itemIndex = 0; itemIndex < itemCount; itemIndex++) {
                    items.add(makeNewItem());
                }
                for (int i = 0; i < preferenceCount; i++) {
                    int prefIndex1 = random.nextInt(itemCount - 1) + 1;
                    int prefIndex2 = random.nextInt(prefIndex1);
                    preferences.add(new Preference(prefIndex1, prefIndex2, Choice.FIRST));
                }
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
                return null;
            }
        }; //end inner class.

    }
}
