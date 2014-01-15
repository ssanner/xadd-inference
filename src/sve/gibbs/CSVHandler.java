package sve.gibbs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import javax.lang.model.type.TypeVisitor;

public class CSVHandler {

    private static <T> void parseStringArray(T[] a, String[] s) {
        for (int i = 0; i < s.length; i++) {
            if (a instanceof String[]) {
                a[i] = (T) s[i];
            } else if (a instanceof Double[]) {
                a[i] = (T) ((Double) Double.parseDouble(s[i]));
            } else if (a instanceof Integer[]) {
                a[i] = (T) ((Integer) Integer.parseInt(s[i]));
            }
        }
    }

    public static ArrayList<Double[]> readcsvDouble(String filename) {
        ArrayList<Double[]> a = new ArrayList<Double[]>();

        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = br.readLine()) != null) {
                String[] s = line.split(",");
                Double[] t = new Double[s.length];
                parseStringArray(t, s);
                a.add(t);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return a;
    }

    public static ArrayList<String[]> readcsv(String filename) {
        ArrayList<String[]> a = new ArrayList<String[]>();

        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = br.readLine()) != null) {
                String[] s = line.split(",");
                a.add(s);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return a;
    }

    public static void writecsv(String filename, ArrayList<String[]> a) {
        writecsv(filename, a, false);
    }

    public static void writecsv(String filename, ArrayList<String[]> a, boolean append) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename, append));
            for (String[] s : a) {
                for (int i = 0; i < s.length; i++) {
                    bw.write(s[i]);
                    if (i < s.length - 1) {
                        bw.write(",");
                    }
                }
                bw.write("\n");
            }
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writecsv(String filename, double[] a) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
            for (double d : a) {
                bw.write(d + "\n");
            }

            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writecsv(String filename, HashMap<String, ArrayList<Double>> a) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
            // we assume all the columns are of equal length
            int length = -2;
            int i = 0;
            while (length == -2 || i < length) {
                for (String key : a.keySet()) {
                    if (length == -2) {
                        length = a.get(key).size();
                    }
                    bw.write(a.get(key).get(i).toString());
                    if (i < length - 1) {
                        bw.write(",");
                    }
                }
                if (length == -2) {
                    length = -1;
                } else
                    bw.write("\n");
                i++;
            }

            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
