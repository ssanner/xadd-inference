package hgm.poly.reports.sg.external.Anglican;

import hgm.asve.Pair;
import hgm.poly.reports.sg.external.ExternalMhSampleBank;
import hgm.poly.sampling.SamplerInterface;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Hadi Afshar.
 * Date: 28/11/14
 * Time: 12:01 AM
 */
public class AnglicanCodeGeneratorTest {

    @Test
    public void testMakeAnglicanCollisionModel() throws Exception {
        Map<String, Double> evidence=new HashMap<String, Double>();
        evidence.put("p_t", 3d);        //todo...    ???
        evidence.put("v_2", 0.2d);
//        evidence.put("m_1", 2d);
        String s = AnglicanCodeGenerator.makeAnglicanCollisionModel(2, 0.1d, 2.1d, -2d, 2d, false, evidence, 0.1, Arrays.asList("m_1", "v_1"));
//        System.out.println("s = \n" + s);

        ExternalMhSampleBank bank = AnglicanCodeGenerator.runAnglicanCode(AnglicanCodeGenerator.ANGLICAN_DEFAULT_JAR_PATH, s, 20000, AnglicanCodeGenerator.AnglicanSamplingMethod.rdb);//pgibbs, smc, rdb
        System.out.println("bank = " + bank);
        /*while (bank.hasNext()) {
            Pair<Double[],Long> next = bank.next();
            System.out.println("next = " + next);
        }*/

        String SAMPLES_FILE_PATH = "D:/JAVA/IdeaProjects/proj2/test/hgm/poly/gm/";
        persistBank(bank, SAMPLES_FILE_PATH + "scatter2D.txt");
    }

    @Test
    public void functionalTest() throws IOException, InterruptedException {
        String s = "" +
                "[assume x (uniform-continuous 0.0 1.0)]\n" +
                "[assume y (uniform-continuous 0.0 1.0)]\n" +
                "[assume p (- (* x (* x (* x x))) y)]\n" +
                "[observe (normal p 0.1) 0]\n" +
                "[predict (list x y)]";

        ExternalMhSampleBank bank = AnglicanCodeGenerator.runAnglicanCode(AnglicanCodeGenerator.ANGLICAN_DEFAULT_JAR_PATH, s, 1000, AnglicanCodeGenerator.AnglicanSamplingMethod.smc);//pgibbs, smc, rdb
//        System.out.println("bank = " + bank);

        String SAMPLES_FILE_PATH = "D:/JAVA/IdeaProjects/proj2/test/hgm/poly/gm/";
        persistBank(bank, SAMPLES_FILE_PATH + "scatter2D.txt");



    }

    private void persistBank(ExternalMhSampleBank bank, String outputFileName) throws FileNotFoundException {
        PrintStream ps;

        ps = new PrintStream(new FileOutputStream(outputFileName));
        while (bank.hasNext()){
            Pair<Double[],Long> next = bank.next();
            Double[] sample = next.getFirstEntry();

            StringBuilder sb = new StringBuilder();
            for (Double sampledVar : sample) {
                sb.append(sampledVar + "\t");
            }
            ps.println(sb.toString());
//            System.out.println("sample = " + Arrays.toString(sample));
        }

        ps.close();
    }

}
