package hgm.asve;

import java.util.Arrays;

/**
 * Created by Hadi Afshar.
 * Date: 23/10/13
 * Time: 6:59 PM
 */
//Note: Scott has a similar class but since I am not sure about its Hash, I use this.
public class Pair<K, V> {
    private K entry1;
    private V entry2;

    public Pair(K entry1, V entry2) {
        this.entry1 = entry1;
        this.entry2 = entry2;
    }

    public K getFirstEntry() {
        return entry1;
    }

    public V getSecondEntry() {
        return entry2;
    }

    @Override
    public String toString() {
        String s1 = (entry1 instanceof Object[]) ? Arrays.toString((Object[])entry1) : entry1.toString();
        String s2 = (entry2 instanceof Object[]) ? Arrays.toString((Object[])entry2) : entry2.toString();
        return "<" + s1 + ", " + s2 + ">";
    }
}
