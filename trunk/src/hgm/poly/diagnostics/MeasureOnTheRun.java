package hgm.poly.diagnostics;

/**
 * Created by Hadi Afshar.
 * Date: 3/09/14
 * Time: 10:11 PM
 */
public interface MeasureOnTheRun<V> {
    void addNewValue(V value);
    double computeMeasure();
}
