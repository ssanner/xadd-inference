package hgm.poly.pref;

import hgm.poly.sampling.SamplingUtils;
import hgm.poly.vis.FunctionVisualizer;
import hgm.preference.Choice;
import hgm.preference.Preference;
import hgm.preference.db.PreferenceDatabase;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Hadi Afshar.
 * Date: 12/03/14
 * Time: 5:53 AM
 */
public class GPolyPreferenceLearningTest {
    public static String SAMPLES_FILE_PATH = "./test/hgm/poly/sampling/";//scatter2D.txt";
    public static void main(String[] args) throws FileNotFoundException {
        GPolyPreferenceLearningTest instance = new GPolyPreferenceLearningTest();
        instance.basicTest();
    }


    PreferenceDatabase testDB1 = new PreferenceDatabase() {
        Preference[] prefs = new Preference[]{
                new Preference(1, 2, Choice.FIRST),
                new Preference(1, 3, Choice.FIRST),
                new Preference(2, 3, Choice.FIRST),
        };

        List<Double[]> items = new ArrayList<Double[]>(5);

        {
            items.add(new Double[]{0.0, 1.0});
            items.add(new Double[]{25.0, 4.5});
            items.add(new Double[]{30.0, 9.0});
            items.add(new Double[]{30.0, 16.0});
            items.add(new Double[]{40.0, 25.0});
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
    public void basicTest() throws FileNotFoundException {

        GPolyPreferenceLearning learning = new GPolyPreferenceLearning(testDB1, 0.1, "w");

        // Pr(W | R^{n+1})
        PolytopesHandler posterior = learning.computePosteriorWeightVector(Integer.MAX_VALUE);
//        fixVarLimits(context, utilityWeights, -30d, 60d);

        FunctionVisualizer.visualize(posterior, -30d, 60d, 0.5, "Poly");


        //now I sample from it:
        GatedGibbsPolytopesSampler sampler = GatedGibbsPolytopesSampler.makeGibbsSampler(posterior, -GPolyPreferenceLearning.C - 10, GPolyPreferenceLearning.C + 10, new Double[]{-5.771840329479172, 7.1312683349054});
        GatedGibbsPolytopesSampler.DEBUG = false;
        for (int i = 0; i < 2; i++) {
            Double[] assign = sampler.sample();
            System.out.println("t = " + Arrays.toString(assign));
        }

        long t1 = System.currentTimeMillis();
        GatedGibbsPolytopesSampler.DEBUG = false;
        SamplingUtils.save2DSamples(sampler, 10000, SAMPLES_FILE_PATH + "scatterGibbs");
        long t2 = System.currentTimeMillis();
        System.out.println("Time: "+ (t2-t1) + " \t That was all the folk!");
    }

    @Test
    public void basicTestClever() throws FileNotFoundException {

        GPolyPreferenceLearning learning = new GPolyPreferenceLearning(testDB1, 0.1, "w");

        // Pr(W | R^{n+1})
        PolytopesHandler posterior = learning.computePosteriorWeightVector(Integer.MAX_VALUE);
//        fixVarLimits(context, utilityWeights, -30d, 60d);

        FunctionVisualizer.visualize(posterior, -30d, 60d, 0.5, "Poly");


        //now I sample from it:
        CleverGatedGibbsPolytopesSampler sampler = CleverGatedGibbsPolytopesSampler.makeCleverGibbsSampler(posterior, -GPolyPreferenceLearning.C - 10, GPolyPreferenceLearning.C + 10, new Double[]{-5.771840329479172, 7.1312683349054});
        CleverGatedGibbsPolytopesSampler.DEBUG = false;
        for (int i = 0; i < 2; i++) {
            Double[] assign = sampler.sample();
            System.out.println("t = " + Arrays.toString(assign));
        }

        long t1 = System.currentTimeMillis();
        GatedGibbsPolytopesSampler.DEBUG = false;
        SamplingUtils.save2DSamples(sampler, 10000, SAMPLES_FILE_PATH + "scatterGibbs");
        long t2 = System.currentTimeMillis();
        System.out.println("Time: "+ (t2-t1) + " \t That was all the folk!");
    }

}
