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
        s.testOnQUERY1();
    }
    @Test
    public void testOnQUERY1(){
        GraphicalModel gm = new GraphicalModel("./src/sve/tracking.gm");
        SVE sve = new SVE(gm);
        GraphicalModel.Factor result1 = sve.infer(new Query("./src/sve/tracking.query.1"));

    }

}
