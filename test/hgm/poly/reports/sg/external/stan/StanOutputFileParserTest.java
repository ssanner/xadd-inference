package hgm.poly.reports.sg.external.stan;

import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Created by Hadi Afshar.
 * Date: 13/01/15
 * Time: 4:25 PM
 */
public class StanOutputFileParserTest {
    @Test
    public void testCSV() throws IOException {
        StanOutputFileParser parser = new StanOutputFileParser("D:\\JAVA\\IdeaProjects\\proj2\\test\\hgm\\poly\\reports\\sg\\output.csv");
        Iterator<Double[]> it = parser.lowLevelStanSampleIterator();
        int count = 0;
        while (it.hasNext()) {
            Double[] next = it.next();
            System.out.println("next = " + Arrays.toString(next));
            count++;
        }

        System.out.println("count = " + count);
    }

    /*@Test
    public void testRunStanCode() throws Exception {
        String code = "data {" + StanCodeGenerator.END +
                "int<lower=0> N;" + StanCodeGenerator.END +
                "int<lower=0,upper=1> y[N];" + StanCodeGenerator.END +
                "}" + StanCodeGenerator.END +
                "parameters {" + StanCodeGenerator.END +
                "real<lower=0,upper=1> theta;" + StanCodeGenerator.END +
                "}" + StanCodeGenerator.END +
                "model {" + StanCodeGenerator.END +
                "theta ~ beta(1,1);" + StanCodeGenerator.END +
                "for (n in 1:N)" + StanCodeGenerator.END +
                "y[n] ~ bernoulli(theta);" + StanCodeGenerator.END +
                "}";

        List<Pair<String, String>> filesAndCodes = new ArrayList<Pair<String, String>>();
        filesAndCodes.add(new Pair<String, String>("ooo", code));
        StanCodeGenerator.stanExecutableModelBatchMaker(filesAndCodes);
    }


    public static void test2() throws IOException, InterruptedException {

        String s2 = "cmd /c xxx.bat";//"make xxx/bernoulli.exe";//"cmd /c start make";
//        System.out.println("s = " + s2);
        final Process p = Runtime.getRuntime().exec(
                s2, new String[]{}, new File("E:\\WORK\\Stan\\cmdstan"));

        InputStream is = p.getInputStream();
        int i = 0;
        while ((i = is.read()) != -1) {
            System.out.print((char) i);
        }

        InputStream errorStream = p.getErrorStream();
        i = 0;
        while ((i = errorStream.read()) != -1) {
            System.out.print((char) i);
        }


        p.waitFor();

    }
*/



}
