package hgm.preference.db;

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
public class DummyMultiSeedPreferenceDatabase implements PreferenceDatabase {
    private double minAttribBound;
    private double maxAttribBound;
    private int attributeCount;
    private Random random;
    private List<Preference> preferences;
    private List<Double[]> items;
    private List<double[]> auxiliaryWeightVectors; //seeds

    public DummyMultiSeedPreferenceDatabase(double minWeightBound, double maxWeightBound,
                                            double minAttribBound, double maxAttribBound, int preferenceCount, int attributeCount, int itemCount, int seedCount) {

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


        //making seeds:
        auxiliaryWeightVectors = new ArrayList<double[]>(seedCount);
        for (int i=0; i<seedCount; i++) {
            auxiliaryWeightVectors.add(makeAuxiliaryWeightVector(minWeightBound, maxWeightBound, attributeCount));
        }

        //making preferences;
        for (int i = 0; i < preferenceCount; i++) {
            int itemIndex1 = random.nextInt(itemCount);
            int itemIndex2;
            do {
                itemIndex2 = random.nextInt(itemCount);
            } while (itemIndex2 == itemIndex1);
            double[] auxiliaryWeightVector = auxiliaryWeightVectors.get(i % seedCount); // pick seeds in round
            double xW1 = utility(items.get(itemIndex1), auxiliaryWeightVector);
            double xW2 = utility(items.get(itemIndex2), auxiliaryWeightVector);

            if (xW1 > xW2) preferences.add(new Preference(itemIndex1, itemIndex2, Choice.FIRST));
            else if (xW1 < xW2) preferences.add(new Preference(itemIndex1, itemIndex2, Choice.SECOND));
            else preferences.add(new Preference(itemIndex1, itemIndex2, Choice.EQUAL));
        }
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
        return auxiliaryWeightVectors.get(0); //just a seed
    }

}

/**
 * Does not work in its current form
 * #Samples:1000	 relLeafThreshold:0.81	 indicator noise:0.9
 polytope               dim:constraint = loss
 [poly:]	 averageLoss(4:1) = 0.405		[true.skill:]	 averageLoss(4:1) = 0.549
 [poly:]	 averageLoss(4:2) = 0.41		[true.skill:]	 averageLoss(4:2) = 0.35
 [poly:]	 averageLoss(4:3) = 0.405		[true.skill:]	 averageLoss(4:3) = 0.437
 [poly:]	 averageLoss(4:4) = 0.464		[true.skill:]	 averageLoss(4:4) = 0.387
 [poly:]	 averageLoss(4:5) = 0.416		[true.skill:]	 averageLoss(4:5) = 0.415
 [poly:]	 averageLoss(4:6) = 0.184		[true.skill:]	 averageLoss(4:6) = 0.243
 [poly:]	 averageLoss(4:7) = 0.368		[true.skill:]	 averageLoss(4:7) = 0.4
 [poly:]	 averageLoss(4:8) = 0.411		[true.skill:]	 averageLoss(4:8) = 0.421
 [poly:]	 averageLoss(4:9) = 0.259		[true.skill:]	 averageLoss(4:9) = 0.299
 [poly:]	 averageLoss(4:10) = 0.364		[true.skill:]	 averageLoss(4:10) = 0.354
 [poly:]	 averageLoss(4:11) = 0.262		[true.skill:]	 averageLoss(4:11) = 0.25
 [poly:]	 averageLoss(4:12) = 0.355		[true.skill:]	 averageLoss(4:12) = 0.28
 [poly:]	 averageLoss(4:13) = 0.396		[true.skill:]	 averageLoss(4:13) = 0.244
 [poly:]	 averageLoss(4:14) = 0.418		[true.skill:]	 averageLoss(4:14) = 0.35
 [poly:]	 averageLoss(4:15) = 0.135		[true.skill:]	 averageLoss(4:15) = 0.138
 [poly:]	 averageLoss(4:16) = 0.431		[true.skill:]	 averageLoss(4:16) = 0.26
 [poly:]	 averageLoss(4:17) = 0.429		[true.skill:]	 averageLoss(4:17) = 0.446
 [poly:]	 averageLoss(4:18) = 0.182		[true.skill:]	 averageLoss(4:18) = 0.17
 [poly:]	 averageLoss(4:19) = 0.284		[true.skill:]	 averageLoss(4:19) = 0.172
 [poly:]	 averageLoss(4:20) = 0.301		[true.skill:]	 averageLoss(4:20) = 0.222
 [poly:]	 averageLoss(4:21) = 0.243		[true.skill:]	 averageLoss(4:21) = 0.155
 [poly:]	 averageLoss(4:22) = 0.507		[true.skill:]	 averageLoss(4:22) = 0.287
 [poly:]	 averageLoss(4:23) = 0.601		[true.skill:]	 averageLoss(4:23) = 0.321
 [poly:]	 averageLoss(4:24) = 0.26		[true.skill:]	 averageLoss(4:24) = 0.109
 [poly:]	 averageLoss(4:25) = 0.583		[true.skill:]	 averageLoss(4:25) = 0.171
 [poly:]	 averageLoss(4:26) = 0.222		[true.skill:]	 averageLoss(4:26) = 0.248
 [poly:]	 averageLoss(4:27) = 0.303		[true.skill:]	 averageLoss(4:27) = 0.162
 [poly:]	 averageLoss(4:28) = 0.472		[true.skill:]	 averageLoss(4:28) = 0.362
 [poly:]	 averageLoss(4:29) = 0.5		[true.skill:]	 averageLoss(4:29) = 0.354
 [poly:]	 averageLoss(4:30) = 0.542		[true.skill:]	 averageLoss(4:30) = 0.472
 [poly:]	 averageLoss(4:31) = 0.603		[true.skill:]	 averageLoss(4:31) = 0.435
 [poly:]	 averageLoss(4:32) = 0.515		[true.skill:]	 averageLoss(4:32) = 0.336
 [poly:]	 averageLoss(4:33) = 0.49		[true.skill:]	 averageLoss(4:33) = 0.186
 [poly:]	 averageLoss(4:34) = 0.47		[true.skill:]	 averageLoss(4:34) = 0.471
 [poly:]	 averageLoss(4:35) = 0.531		[true.skill:]	 averageLoss(4:35) = 0.365
 [poly:]	 averageLoss(4:36) = 0.612		[true.skill:]	 averageLoss(4:36) = 0.264
 [poly:]	 averageLoss(4:37) = 0.631		[true.skill:]	 averageLoss(4:37) = 0.346
 [poly:]	 averageLoss(4:38) = 0.595		[true.skill:]	 averageLoss(4:38) = 0.131
 [poly:]	 averageLoss(4:39) = 0.632		[true.skill:]	 averageLoss(4:39) = 0.272

 **/