package hgm.sampling.gibbs.integral;

import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import xadd.ExprLib;
import xadd.ExprLib.*;

/**
 * Created by Hadi Afshar.
 * Date: 21/01/14
 * Time: 2:56 AM
 */
public class Piecewise1DPolynomial {
    private String var;

    /**
     * A single entry map to set the value of the var for the purpose of expression evaluation
     */
    private HashMap<String, Double> assign;
    /**
     * mapping from the start point of each interval to the expression associated to that interval:
     */
    private SortedMap<Double, ArithExpr> intervalStartToExpressionMap;
    /**
     * Start point of each interval
     */
    private TreeSet<Double> startPoints;


    public Piecewise1DPolynomial(String var) {
        this.var = var;
        assign = new HashMap<String, Double>(1);

        intervalStartToExpressionMap = new TreeMap<Double, ArithExpr>();
        startPoints = new TreeSet<Double>();
    }

    public void put(Double startPoint, ArithExpr expr) {
        startPoints.add(startPoint);
        intervalStartToExpressionMap.put(startPoint, expr);
    }

    public Double value(Double x) {
        ArithExpr associatedExpr = intervalStartToExpressionMap.get(startPoints.floor(x));
        assign.put(var, x);
        return associatedExpr.evaluate(assign);
    }

    @Override
    public String toString() {
        return intervalStartToExpressionMap.toString();
    }
}
