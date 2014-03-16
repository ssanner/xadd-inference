package hgm.utils;

import com.jmatio.io.MatFileReader;
import hgm.preference.Choice;
import hgm.preference.Preference;
import hgm.preference.db.PreferenceDatabase;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.linear.RealMatrix;

import com.jmatio.io.MatFileWriter;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLDouble;

// By Ehsan 
public class Utils {

    private static String run_stamp = null;
    private static String run_stamp_path = null;

    public static String runStamp() {
        if (run_stamp == null) {
            Calendar cal = Calendar.getInstance();
            run_stamp = Integer.toString(cal.get(Calendar.YEAR))
                    + String.format("%2d", cal.get(Calendar.MONTH)).replace(" ", "0")
                    + String.format("%2d", cal.get(Calendar.DATE)).replace(" ", "0") + "_"
                    + String.format("%2d", cal.get(Calendar.HOUR_OF_DAY)).replace(" ", "0")
                    + String.format("%2d", cal.get(Calendar.MINUTE)).replace(" ", "0")
                    + String.format("%2d", cal.get(Calendar.SECOND)).replace(" ", "0").intern();
        }
        return run_stamp;
    }

    public static String runStampPath() {
        if (run_stamp_path == null) {
            run_stamp_path = "results/" + runStamp();

            File f = new File(run_stamp_path);
            if (!f.exists()) {
                f.mkdir();
            }
        }
        return run_stamp_path;
    }

    public static void writeMat(String filename, ArrayList<MLArray> list) {
        MatFileWriter mfw = new MatFileWriter();
        try {
            mfw.write(runStampPath() + File.separator + filename, list);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeMat(String filename, MLDouble m) {
        ArrayList<MLArray> list = new ArrayList<MLArray>();
        list.add(m);
        writeMat(filename, list);
    }

    public static void writeMat(String filename, String name, double[] d) {
        MLDouble m = toMLDouble(name, d);
        ArrayList<MLArray> list = new ArrayList<MLArray>();

        list.add(m);
        writeMat(filename, list);
    }

    public static void writeMat(String filename, HashMap<String, double[][]> data) {
        ArrayList<MLArray> list = new ArrayList<MLArray>();
        for (String key : data.keySet()) {
            MLDouble m = toMLDouble(key, data.get(key));
            list.add(m);
        }

        writeMat(filename, list);
    }

    public static void writeMatWithExactFileName(String filename, HashMap<String, double[][]> data) throws IOException {
        ArrayList<MLArray> list = new ArrayList<MLArray>();
        for (String key : data.keySet()) {
            MLDouble m = toMLDouble(key, data.get(key));
            list.add(m);
        }

//        writeMat(filename, list);

        MatFileWriter mfw = new MatFileWriter();
        mfw.write(filename, list);
    }

    public static void writeMatMap(String filename, HashMap<String, ArrayList<Double>> map) {
        ArrayList<MLArray> list = new ArrayList<MLArray>();
        for (String key : map.keySet()) {
            MLDouble m = toMLDouble(key, map.get(key));
            list.add(m);
        }

        writeMat(filename, list);
    }

    public static MLDouble toMLDouble(String name, double[] val) {
        MLDouble d = new MLDouble(name, new int[]{1, val.length});

        for (int j = 0; j < val.length; j++) {
            d.set(val[j], 0, j);
        }

        return d;
    }

    public static MLDouble toMLDouble(String name, List<Double> val) {
        MLDouble d = new MLDouble(name, new int[]{1, val.size()});

        for (int j = 0; j < val.size(); j++) {
            d.set(val.get(j), 0, j);
        }

        return d;
    }

    public static MLDouble toMLDouble(String name, double[][] val) {
        int n = 0, m = val.length;
        if (m > 0)
            n = val[0].length;
        else {
            System.err.println("Number of columns is zero.");
            return null;
        }
        MLDouble d = new MLDouble(name, new int[]{m, n});

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                d.set(val[i][j], i, j);
            }
        }

        return d;
    }

    public static double[] toDouble(Double[] val) {
        double[] t = new double[val.length];
        for (int i = 0; i < t.length; i++) {
            t[i] = val[i];
        }
        return t;
    }

    private static double[][] toDouble(MLDouble m) {
        double[][] d = new double[m.getM()][m.getN()];

        for (int i = 0; i < m.getM(); i++) {
            for (int j = 0; j < m.getN(); j++) {
                d[i][j] = m.get(i, j);
            }
        }

        return d;
    }

/*
    public static void save(final PreferenceDatabase prData, final String filename) {
        HashMap<String, double[][]> map = new HashMap<String, double[][]>();
//        map.put(AUXILIARY_WEIGHT_VECTOR, new double[][]{prData.getAuxiliaryWeightVector()});
        map.put(NUMBER_OF_ATTRIBUTES, new double[][]{{prData.getNumberOfAttributes()}});
        map.put(NUMBER_OF_ITEMS, new double[][]{{prData.getNumberOfItems()}});

        ArrayList<Integer> visitedItemIds = new ArrayList<Integer>();
        ArrayList<double[]> itemAtt = new ArrayList<double[]>();
        double[][] pref = new double[prData.getPreferenceResponses().size()][3];

        for (int i = 0; i < prData.getPreferenceResponses().size(); i++) {
            int itemId1 = prData.getPreferenceResponses().get(i).getItemId1();
            int itemId2 = prData.getPreferenceResponses().get(i).getItemId2();

            if (!visitedItemIds.contains(itemId1)) {
                itemAtt.add(toDouble(prData.getItemAttributeValues(itemId1)));
            }

            if (!visitedItemIds.contains(itemId2)) {
                itemAtt.add(toDouble(prData.getItemAttributeValues(itemId2)));
            }

            pref[i] = new double[]{itemId1, itemId2, prData.getPreferenceResponses().get(i).getPreferenceChoice().ordinal()};
        }
        map.put(ITEM_ATTRIBUTE_VALUES, itemAtt.toArray(new double[][]{{}}));
        map.put(PREFERENCE_RESPONSES, pref);

        writeMat(filename, map);
    }
*/


    //// Read:
    public static Map<String, double[][]> readMat(String file) {
        Map<String, double[][]> map = new HashMap<String, double[][]>();
        try {
            File f = new File(file);

            if (f.exists() == false) {
                System.out.println("File " + f.getCanonicalPath() + " does not exist ...");
                return null;
            }
            MatFileReader mfr = new MatFileReader(file);
            Map<String, MLArray> a = mfr.getContent();

            for (String k : a.keySet()) {
                map.put(k, toDouble((MLDouble) a.get(k)));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return map;
    }


}
