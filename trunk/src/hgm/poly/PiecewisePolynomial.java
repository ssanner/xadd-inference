package hgm.poly;

import hgm.poly.Polynomial;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 26/02/14
 * Time: 8:07 PM
 */
public class PiecewisePolynomial {

    /**
     * A single entry map to set the value of the var for the purpose of expression evaluation
     */
    protected Map<List<Polynomial> /*constraints*/, Polynomial> constraints2valuesMap = new HashMap<List<Polynomial>, Polynomial>();

    // the lists should be mutually exclusive otherwise things might not work well...
    public void put(List<Polynomial> constrains, Polynomial value) {
        constraints2valuesMap.put(constrains, value);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<List<Polynomial>, Polynomial> aCase : constraints2valuesMap.entrySet()) {
            sb.append("\t").append(aCase).append("\n");
        }
        return sb.toString();
    }

    public double evaluate(Double[] assign) {
        for (Map.Entry<List<Polynomial>, Polynomial> aCase : constraints2valuesMap.entrySet()) {

            boolean allConstraintsSatisfied = true;
            for (Polynomial constraint : aCase.getKey()) {
                if (constraint.evaluate(assign) <= 0) {
                    allConstraintsSatisfied = false;
                    break;
                }
            }

            if (allConstraintsSatisfied) {
                return aCase.getValue().evaluate(assign);
            }

        }

        return 0d; //if no case sentence is satisfied then 0 is returned by default
    }
    /*
        Polynomial integral;
    Double[] vars;
    Interval interval;
    double maxValue;
    int varIndex;


    OneDimIntegralFunc(Polynomial integral, Interval interval, String integrationVar) {

        this.integral = integral;
        this.interval = interval;

        PolynomialFactory factory = integral.getFactory();
        vars = new Double[factory.getAllVars().length];
//        Arrays.fill(vars, null);

        varIndex = factory.getVarIndex(integrationVar);
        vars[varIndex] = interval.getHighBound();

        maxValue = integral.evaluate(vars);
    }


    @Override
    public double eval(double varValue) {
        if (varValue < interval.getLowBound()) return 0d;
        if (varValue > interval.getHighBound()) return maxValue;
        vars[varIndex] = varValue;
        return integral.evaluate(vars);
    }

    public double getNormalizationFactor() {
        return maxValue;
    }

     */
}
