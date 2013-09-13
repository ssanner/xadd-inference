package hadiPractice;

/**
 * Created by Hadi M Afshar
 * Date: 9/09/13
 * Time: 8:17 PM
 */
@Deprecated //Not used ....
public class ContinuousVariable {
    private String name;
    private double minValue;
    private double maxValue;
    private double value;
//    private boolean isBoolean;

    public ContinuousVariable(String name, double value/*, boolean aBoolean*/) {
        this.name = name;
        this.value = value;
//        isBoolean = aBoolean;
    }

    public double getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "ContinuousVariable{" +
                "value='" + value + '\'' +
                '}';
    }

}
