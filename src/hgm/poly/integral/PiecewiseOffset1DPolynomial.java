package hgm.poly.integral;

import hgm.poly.Polynomial;
import hgm.poly.PolynomialException;
import hgm.poly.PolynomialFactory;
import hgm.sampling.gibbs.integral.Interval;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hadi Afshar.
 * Date: 27/02/14
 * Time: 12:19 AM
 *
 * A one dimensional function that in different intervals is associated to different offset values, suitable for representation of !D integral
 */
public class PiecewiseOffset1DPolynomial implements OneDimFunction{
    Polynomial integral;
    Double[] varAssign;
    List<Interval> intervals;
    List<Double> offsets;
    int varIndex;


    public PiecewiseOffset1DPolynomial(Polynomial indefIntegral, int varIndex) {
        this.integral = indefIntegral;
        this.varIndex = varIndex;

        PolynomialFactory factory = integral.getFactory();
        varAssign = new Double[factory.getAllVars().length];

        intervals = new ArrayList<Interval>();
        offsets = new ArrayList<Double>();

//        varIndex = factory.getVarIndex(integrationVar);
//        varAssign[varIndex] = interval.getHighBound();
//        maxValue = integral.evaluate(varAssign);
    }


    //todo: not only the y value but the last x located in an interval can also be returned leading to faster CDF calculation
    @Override
    public double eval(double varValue) {
//        if (varValue < interval.getLowBound()) return 0d;
//        if (varValue > interval.getHighBound()) return maxValue;
//        varAssign[varIndex] = varValue;
//        return integral.evaluate(varAssign);
        if (intervals.isEmpty()) return 0;

        for (int i = 0; i < intervals.size(); i++) {
            Interval interval = intervals.get(i);
            if (interval.getHighBound() >= varValue) {
                if (interval.getLowBound() <= varValue) {
                    //in the interval:
                    varAssign[varIndex] = varValue;
                    return integral.evaluate(varAssign) + offsets.get(i);
                } else {
                    //between two intervals (or before all intervals):
                    if (i==0) {
                        //before all intervals:
                        return 0d;
                    } else {
                        //return last value of the prev. interval
                        varAssign[varIndex] = intervals.get(i-1).getHighBound();
                        return integral.evaluate(varAssign) + offsets.get(i-1);
                    }
                }
            }
        }

        //todo: max value may be cached
        varAssign[varIndex] = intervals.get(intervals.size()-1).getHighBound();
        return integral.evaluate(varAssign) + offsets.get(intervals.size()-1);
    }


    public void addIntervalAndOffset(Interval interval, double offset) {
        if (!intervals.isEmpty() && intervals.get(intervals.size()-1).getHighBound() > interval.getLowBound()) throw new PolynomialException("interval order violated"); //debug

        intervals.add(interval);
        offsets.add(offset);
    }

    public List<Interval> getIntervals() {
        return intervals;
    }
}
