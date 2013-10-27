package hgm;

/**
 * Created by Hadi Afshar.
 * Date: 30/09/13
 * Time: 11:45 PM
 */
@Deprecated
public class ContinuousVariable extends Variable {
    private double minValue;
    private double maxValue;

    public ContinuousVariable(String name, double minValue, double maxValue) {
        super(name);
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public double getMinValue() {
        return minValue;
    }

    public double getMaxValue() {
        return maxValue;
    }
}
