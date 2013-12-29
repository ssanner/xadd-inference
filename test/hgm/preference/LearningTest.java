package hgm.preference;

import hgm.preference.db.Preference;
import hgm.preference.db.PreferenceDataBase;
import hgm.preference.db.car.CarPreferenceDataBase;
import org.junit.Test;
import xadd.XADD;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 20/12/13
 * Time: 1:00 PM
 */
public class LearningTest {
    public static void main(String[] args) {
        LearningTest instance = new LearningTest();
        instance.testOnCar1stExperiment();

    }

    @Test
    public void testBasic() {
        PreferenceDataBase db = new PreferenceDataBase() {
            Preference[] prefs = new Preference[] {
                    new Preference(1, 2, Preference.Choice.FIRST),
                    new Preference(1, 3, Preference.Choice.FIRST),
                    new Preference(2, 3, Preference.Choice.FIRST),
            };

            List<Double[]> items = new ArrayList<Double[]>(5);
            {
                items.add(new Double[]{1.0, 0.1});
                items.add(new Double[]{2.0, 0.2});
                items.add(new Double[]{3.0, 0.3});
                items.add(new Double[]{4.0, 0.4});
                items.add(new Double[]{5.0, 0.5});
            }

            /*
            @Override
            public String[] getAttributes() {
                return new String[]{"color", "size"};
            }
            */

            @Override
            public int getNumberOfAttributes() {
                return items.get(0).length;
            }

            @Override
            public int numberOfItems() {
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
        }; //end inner class.

        Learning learning = new Learning(new XADD(), db);

        // Pr(W | R^{n+1})
        XADD.XADDNode utilityWeights = learning.computeProbabilityOfWeightVector(db.getNumberOfAttributes(), db.getPreferenceResponses(), "w");

        int maxExpUtilItem = learning.computeItemWithMaxExpectedUtility(utilityWeights, "w");
        System.out.println("maxExpUtilItem = " + maxExpUtilItem);
    }

    @Test
    public void testOnCar1stExperiment() {
        int maxPrefs = 5;
        Set<Integer> advisers = new HashSet<Integer>();
        for (int i=0; i<1; i++) {
            advisers.add(i);
        }
        PreferenceDataBase db = CarPreferenceDataBase.fetchCarPreferenceDataBase1stExperiment(advisers);

        XADD context = new XADD();
        Learning learning = new Learning(context, db);

        // Pr(W | R^{n+1})
        List<Preference> preferenceResponses = db.getPreferenceResponses();
        if (maxPrefs > 0) {
            preferenceResponses = preferenceResponses.subList(0, Math.min(maxPrefs, preferenceResponses.size()));
        }
        XADD.XADDNode utilityWeights = learning.computeProbabilityOfWeightVector(db.getNumberOfAttributes(), preferenceResponses, "w");
        context.getGraph(context._hmNode2Int.get(utilityWeights)).launchViewer();

        int maxExpUtilItem = learning.computeItemWithMaxExpectedUtility(utilityWeights, "w");
            Double[] item = db.getItemAttributeValues(0);
            double itemUtil = learning.expectedItemUtility(item, utilityWeights, "w", true);
        System.out.println("itemUtil = " + itemUtil);

//        System.out.println("maxExpUtilItem = " + maxExpUtilItem);
    }
}
