package hgm.sampling.gibbs.integral;

/**
 * Created by Hadi Afshar.
 * Date: 22/01/14
 * Time: 4:09 PM
 */
public class Interval implements Comparable{
    protected Double lowBound;
    protected Double highBound;

    public Interval(Double lowBound, Double highBound) {
        this.lowBound = lowBound;
        this.highBound = highBound;
    }

    public void setLowBound(Double lowBound) {
        this.lowBound = lowBound;
    }

    public void setHighBound(Double highBound) {
        this.highBound = highBound;
    }

    public Double getLowBound() {
        return lowBound;
    }

    public Double getHighBound() {
        return highBound;
    }

    public void imposeMoreRestriction(Double low, Double high) {
        if (low != null && low > this.lowBound) {  //NULL means not set...
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
        return "[" + lowBound + ", " + highBound + ']';
    }

    public boolean isFeasible() {
        return lowBound == null || highBound == null || highBound > lowBound;    //even equality is not accepted...
    }


    boolean trueOrFalse;

    public void set(boolean trueOrFalse) {
        this.trueOrFalse = trueOrFalse;
    }


    @Override
    public int compareTo(Object o) {
        Interval that = (Interval) o;

        if (this.lowBound.equals(that.lowBound) && this.highBound.equals(that.highBound)) return 0;

        if (this.lowBound < that.lowBound) {
            if (this.highBound <= that.lowBound) return -1;
        }

        if (that.lowBound < this.lowBound) {
            if (that.highBound <= this.lowBound) return 1;
        }

        throw new RuntimeException("cannot compare " + this + " with " + that);
    }
}
