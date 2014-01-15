package sve;


import org.junit.Test;

/**
 * Created by Hadi Afshar
 * Date: 15/09/13
 * Time: 7:58 PM
 */
public class SVETest {
    public static void main(String[] args) {
        SVETest s = new SVETest();
        s.testOnQUERY();
    }

    @Test
    public void testOnQUERY() {
        runSve("./src/sve/competition.gm", "./src/sve/competition.query.1");
//        runSve("./src/sve/tracking.gm", "./src/sve/tracking.query.1");
//        runSve("./src/sve/radar.gm", "./src/sve/radar.query.4");
    }

    public void runSve(String gmFileName, String queryFileName) {
        GraphicalModel gm = new GraphicalModel(gmFileName);
        SVE sve = new SVE(gm);
        GraphicalModel.Factor result1 = sve.infer(new Query(queryFileName));
    }

}
