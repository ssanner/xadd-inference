package hgm.sampling.gibbs.integral;

/**
 * Created by Hadi Afshar.
 * Date: 22/01/14
 * Time: 4:09 PM
 */
public class Interval {
    protected Double lowBound;
    protected Double highBound;

    public Interval(Double lowBound, Double highBound) {
        this.lowBound = lowBound;
        this.highBound = highBound;
    }

    void setLowBound(Double lowBound) {
        this.lowBound = lowBound;
    }

    void setHighBound(Double highBound) {
        this.highBound = highBound;
    }

    Double getLowBound() {
        return lowBound;
    }

    Double getHighBound() {
        return highBound;
    }

    public void imposeMoreRestriction(Double low, Double high) {
        if (low != null && low> this.lowBound) {  //NULL means not set...
            this.lowBound = low;
        }

        if (high != null && high < this.highBound) {
            this.highBound = high;
        }
    }

    @SuppressWarnings("CloneDoesntCallSuperClone, CloneDoesntDeclareCloneNotSupportedException")
    @Override
    public Interval clone() {
        return new Interval(lowBound, highBound);
    }

    @Override
    public String toString() {
        return "[" + lowBound  +", " + highBound + ']';
    }

    public boolean isFeasible() {
        return lowBound == null || highBound == null || highBound > lowBound;    //even equality is not accepted...
    }


}
