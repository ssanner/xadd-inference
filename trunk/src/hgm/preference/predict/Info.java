package hgm.preference.predict;

import hgm.asve.Pair;

import java.util.ArrayList;

/**
 * Created by Hadi Afshar.
 * Date: 2/02/14
 * Time: 7:08 AM
 */
public class Info extends ArrayList<Pair<String, Double>> {
    public void add(String info, Double quantity) {
       add(new Pair<String, Double>(info, quantity));
    }
}
