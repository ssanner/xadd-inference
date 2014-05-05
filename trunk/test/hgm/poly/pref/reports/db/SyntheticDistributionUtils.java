package hgm.poly.pref.reports.db;

import hgm.poly.pref.GPolyPreferenceLearning;
import hgm.poly.pref.GatedGibbsPolytopesSampler;
import hgm.poly.pref.PosteriorHandler;
import hgm.poly.vis.FunctionVisualizer;
import hgm.poly.sampling.SamplingUtils;
import hgm.preference.Choice;
import hgm.preference.Preference;
import hgm.preference.db.DummyFeasiblePreferenceDatabase;
import hgm.preference.db.PreferenceDatabase;
import hgm.utils.Utils;

import java.io.*;
import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 13/03/14
 * Time: 4:59 AM
 */
public class SyntheticDistributionUtils {
    public static final String AUXILIARY_WEIGHT_VECTOR = "AuxiliaryWeightVector";
    public static final String NUMBER_OF_ATTRIBUTES = "NumberOfAttributes";
    public static final String NUMBER_OF_ITEMS = "NumberOfItems";
    public static final String ITEM_ATTRIBUTE_VALUES = "ItemAttributeValues";
    public static final String PREFERENCE_RESPONSES = "PreferenceResponses";
    //returns taken time...

    public static String DBS_FOLDER = "./test/hgm/poly/pref/reports/db/data/";
    public static boolean VISUALIZE = false;

    public static PreferenceDatabase fetchOrGenerateTrainTestPreferenceDbDistribution(String prior,
                                                                                      double minWeightBound, double maxWeightBound,
                                                                                      double minAttribBound, double maxAttribBound,
                                                                                      int attributeCount, int itemCount, int trainingPreferenceCount, int testPreferenceCount,
                                                                                      int numSamplesToEstimateRealDistribution,
                                                                                      double indicatorNoise,
                                                                                      int maxGatingConditionViolation,
                                                                                      boolean regenerateEvenIfExists) throws IOException {
        SyntheticDistributionUtils instance = new SyntheticDistributionUtils();
        String signature = instance.createSignature(prior, minWeightBound, maxWeightBound,
                minAttribBound, maxAttribBound, trainingPreferenceCount, testPreferenceCount, attributeCount, itemCount);

        String databasePath = DBS_FOLDER + signature;
        File f = new File(databasePath);
        if (f.exists() && !regenerateEvenIfExists) {
            return instance.loadPrefDb(databasePath, prior,
                    minWeightBound, maxWeightBound,
                    minAttribBound, maxAttribBound,
                    trainingPreferenceCount, testPreferenceCount, attributeCount, itemCount);
        } else {
            //generate:
            DummyFeasiblePreferenceDatabase trainDummyDb = new DummyFeasiblePreferenceDatabase(minWeightBound, maxWeightBound,
                    minAttribBound, maxAttribBound, trainingPreferenceCount, attributeCount, itemCount);

            String samplesFile = databasePath + "_samples";
            instance.generateAndPersistPiecewiseDistributionSamples(trainDummyDb,
                    numSamplesToEstimateRealDistribution,
                    samplesFile,
                    indicatorNoise,
                    maxGatingConditionViolation, minWeightBound, maxWeightBound);
            PreferenceDatabase testDb = instance.generateTrainingTestPreferenceDatabaseGivenSamples(testPreferenceCount, samplesFile, trainDummyDb);
            instance.savePrefDb(testDb, databasePath);
            return testDb;
        }
   }
    private double generateAndPersistPiecewiseDistributionSamples(PreferenceDatabase trainingDb,
                                                                  int numSamples, String outputFileAddress,
                                                                  double indicatorNoise,
                                                                  int maxGatingConditionViolation,
                                                                  double minForAllVars,
                                                                  double maxForAllVars) throws FileNotFoundException {

        //todo what about prior?
        GPolyPreferenceLearning learning = new GPolyPreferenceLearning(trainingDb, indicatorNoise, "w");

        // Pr(W | R^{n+1})
        PosteriorHandler posterior = learning.computePosteriorWeightVector(maxGatingConditionViolation);

        if (VISUALIZE) FunctionVisualizer.visualize(posterior, -10, 10, 0.1, "posterior");

        //now I sample from it:
        GatedGibbsPolytopesSampler sampler = GatedGibbsPolytopesSampler.makeSampler(posterior, //todo rejection based sampling should be used instead...
                minForAllVars/*-PolyPreferenceLearning.C*/,
                maxForAllVars/*PolyPreferenceLearning.C*/, null);

        long t1 = System.currentTimeMillis();
        SamplingUtils.saveSamples(sampler, numSamples, outputFileAddress);
        long t2 = System.currentTimeMillis();
        return t2 - t1;
    }

    private PreferenceDatabase generateTrainingTestPreferenceDatabaseGivenSamples(//final double minAttribBound, final double maxAttribBound,
                                                                                  final int testPreferenceCount,// final int itemCount,
                                                                                  final String trainingDistributionSamplesAddress,
                                                                                  final DummyFeasiblePreferenceDatabase trainingDB) throws IOException {


        return new PreferenceDatabase() {
            private int attributeCount;
            private Random random;
            private List<Preference> preferences;
            private List<Double[]> items;
            {
                attributeCount = calcAttributeCount();
                items = trainingDB.getItems();
                preferences = new ArrayList<Preference>(trainingDB.getPreferenceResponses().size() + testPreferenceCount);
                preferences.addAll(trainingDB.getPreferenceResponses()); //training + test
                random = new Random();

                // making items:
//                for (int itemIndex = 0; itemIndex < itemCount; itemIndex++) {
//                    items.add(makeNewItem());
//                }

                //making TEST preferences;
                for (int i = 0; i < testPreferenceCount; i++) {
                    int itemIndex1 = random.nextInt(items.size());
                    int itemIndex2;
                    do {
                        itemIndex2 = random.nextInt(items.size());
                    } while (itemIndex2 == itemIndex1);

                    Choice choice;
                    if (firstItemHasToBePreferred(items.get(itemIndex1), items.get(itemIndex2))) {
                        choice = Choice.FIRST;
                    } else {
                        choice = Choice.SECOND;
                    }

                    preferences.add(shuffledPreference(itemIndex1, itemIndex2, choice));
                }
            }

            private int calcAttributeCount() throws IOException {
                BufferedReader br = new BufferedReader(new FileReader(trainingDistributionSamplesAddress));

                String line = br.readLine();
                String[] ws = line.split("\\s+");//split on white space
//                System.out.println("line: " + line + " -> ws = " + Arrays.toString(ws));
                return ws.length;
            }

            private Preference shuffledPreference(int itemId1, int itemId2, Choice choice) {
                if (random.nextBoolean()) {
                    return new Preference(itemId1, itemId2, choice);
                }

                Choice flippedChoice;
                switch (choice) {
                    case FIRST:
                        flippedChoice = Choice.SECOND;
                        break;
                    case SECOND:
                        flippedChoice = Choice.FIRST;
                        break;
                    case EQUAL:
                        flippedChoice = Choice.EQUAL;
                        break;
                    default:
                        throw new RuntimeException("unexpected choice");
                }
                return new Preference(itemId2, itemId1, flippedChoice);
            }


//            private Double[] makeNewItem() {
//                Double[] item = new Double[attributeCount];
//                for (int i = 0; i < attributeCount; i++) {
//                    item[i] = random.nextDouble() * (maxAttribBound - minAttribBound) + minAttribBound;
//                }
//                return item;
//            }

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

            boolean firstItemHasToBePreferred(Double[] item1, Double[] item2) throws IOException {
                int n = item1.length;
                if (n != item2.length) throw new RuntimeException("size mismatch");

                double itemUtil1 = 0d;
                double itemUtil2 = 0d;


                BufferedReader br = new BufferedReader(new FileReader(trainingDistributionSamplesAddress));

                String line;

                for (; ; ) {
                    line = br.readLine();
                    if (line == null) break;
                    String[] ws = line.split("\\s+");//split on white space
                    if (ws.length != n) throw new RuntimeException("size mismatch");
                    for (int i = 0; i < n; i++) {
                        double w = Double.valueOf(ws[i]);
                        itemUtil1 += (w * item1[i]);
                        itemUtil2 += (w * item2[i]);
                    }

                }

                br.close();

                return (itemUtil1 >= itemUtil2);

            }

        };
    }


    public static void savePrefDb(final PreferenceDatabase prData, final String filename) {
        HashMap<String, double[][]> map = new HashMap<String, double[][]>();
//        map.put(AUXILIARY_WEIGHT_VECTOR, new double[][]{prData.getAuxiliaryWeightVector()});
        map.put(NUMBER_OF_ATTRIBUTES, new double[][]{{prData.getNumberOfAttributes()}});
        map.put(NUMBER_OF_ITEMS, new double[][]{{prData.getNumberOfItems()}});

        double[][] itemAttribsArray = new double[prData.getNumberOfItems()][];
        for (int i = 0; i < prData.getNumberOfItems(); i++) {
            Double[] attribs = prData.getItemAttributeValues(i);
            itemAttribsArray[i] = Utils.toDouble(attribs);
        }
        map.put(ITEM_ATTRIBUTE_VALUES, itemAttribsArray);


//        ArrayList<Integer> visitedItemIds = new ArrayList<Integer>();
//        ArrayList<double[]> itemAtt = new ArrayList<double[]>();
        double[][] pref = new double[prData.getPreferenceResponses().size()][3];

        for (int i = 0; i < prData.getPreferenceResponses().size(); i++) {
            int itemId1 = prData.getPreferenceResponses().get(i).getItemId1();
            int itemId2 = prData.getPreferenceResponses().get(i).getItemId2();

            pref[i] = new double[]{itemId1, itemId2, prData.getPreferenceResponses().get(i).getPreferenceChoice().ordinal()};
        }
        map.put(PREFERENCE_RESPONSES, pref);

//        System.out.println("filename = " + filename);

        try {
            Utils.writeMatWithExactFileName(filename, map);    //todo
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public static PreferenceDatabase loadPrefDb(String databasesPath,
                                                String prior,
                                                double minWeightBound, double maxWeightBound,
                                                double minAttribBound, double maxAttribBound,
                                                int numTrainingConstraints,
                                                int numberOfTestConstraints,
                                                final int attributeCount, final int itemCount) {
        String signature = createSignature(prior, minWeightBound, maxWeightBound, minAttribBound, maxAttribBound,
                numTrainingConstraints, numberOfTestConstraints, attributeCount, itemCount);
        Map<String, double[][]> map = Utils.readMat(databasesPath);//todo
        if (map == null) {
            throw new RuntimeException("no data");
        } else { //convert data to pref:
            int loadedAttributeCount = (int) map.get(NUMBER_OF_ATTRIBUTES)[0][0];
            if (loadedAttributeCount != attributeCount)
                throw new RuntimeException("Loaded #attribs: " + loadedAttributeCount + " != expected #attribs: " + attributeCount);

            int loadedItemCount = (int) map.get(NUMBER_OF_ITEMS)[0][0];
            if (loadedItemCount != itemCount)
                throw new RuntimeException("Loaded #items: " + loadedItemCount + " != expected #items: " + itemCount);

            double[][] itemAtt = map.get(ITEM_ATTRIBUTE_VALUES);
            final List<Double[]> parsedItemAttribs = new ArrayList<Double[]>(itemAtt.length);
            for (double[] item : itemAtt) {
                Double[] pItem = new Double[item.length];
                for (int j = 0; j < item.length; j++) {
                    pItem[j] = item[j];
                }
                parsedItemAttribs.add(pItem);
            }

            double[][] prefRespArrays = map.get(PREFERENCE_RESPONSES);
            final List<Preference> parsedPrefs = new ArrayList<Preference>();
            for (double[] prefRespAr : prefRespArrays) {
                if (prefRespAr.length != 3) throw new RuntimeException("parsing error!");
                Preference pref = new Preference((int) prefRespAr[0], (int) prefRespAr[1], Choice.values()[(int) prefRespAr[2]]);
                parsedPrefs.add(pref);
            }


            return new PreferenceDatabase() {
                @Override
                public int getNumberOfAttributes() {
                    return attributeCount;
                }

                @Override
                public int getNumberOfItems() {
                    return itemCount;
                }

                @Override
                public List<Preference> getPreferenceResponses() {
                    return parsedPrefs;
                }

                @Override
                public Double[] getItemAttributeValues(int itemId) {
                    return parsedItemAttribs.get(itemId);
                }

                @Override
                public double[] getAuxiliaryWeightVector() {
                    return null;
                }
            };
        }
    }

    private static String createSignature(String prior,
                                          double minWeightBound, double maxWeightBound,
                                          double minAttribBound, double maxAttribBound,
                                          int numTrainingConstraints,
                                          int numTestConstraints,
                                          int attributeCount, int itemCount) {
        return "db_Train" + numTrainingConstraints + "_Test" +
                numTestConstraints + "_Dim" +
                attributeCount + "_" + itemCount + "item_" +
                (Math.abs(prior.hashCode() + new Double(minWeightBound * 1000 + maxWeightBound * 100 + minAttribBound * 10 + maxAttribBound * 1).hashCode()) % 100000);
    }

    public static void main(String[] args) throws IOException {
        //test:
        SyntheticDistributionUtils.VISUALIZE = true;
        PreferenceDatabase testDatabase = SyntheticDistributionUtils.fetchOrGenerateTrainTestPreferenceDbDistribution("1", -10, 10, -5, 5, 2, 100, 15, 100, 1000, 0.2, 8, true);
    }
}
