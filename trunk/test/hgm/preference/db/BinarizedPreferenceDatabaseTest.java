package hgm.preference.db;


import hgm.preference.db.car.CarPreferenceDatabase;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Hadi Afshar.
 * Date: 29/01/14
 * Time: 7:03 AM
 */
public class BinarizedPreferenceDatabaseTest {
    @Test
    public void test1() {
        Set<Integer> userIds = new HashSet<Integer>();
        userIds.add(0);

        CarPreferenceDatabase carDb = CarPreferenceDatabase.fetchCarPreferenceDataBase1stExperiment(userIds);

        for (int i=0; i<carDb.getNumberOfItems(); i++) {
            System.out.println("car.Db.get(" + i + ") = " + Arrays.toString(carDb.getItemAttributeValues(i)));
        }

        BinarizedPreferenceDatabase binDb = new BinarizedPreferenceDatabase(carDb);
        for (int i=0;i<binDb.getNumberOfItems(); i++) {
            System.out.println("bin.car.Db.get(" + i + ") = " + Arrays.toString(binDb.getItemAttributeValues(i)));
        }
    }
}
