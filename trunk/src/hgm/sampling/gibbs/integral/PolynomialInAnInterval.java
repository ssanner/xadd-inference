package hgm.sampling.gibbs.integral;

import xadd.ExprLib;

/**
* Created by Hadi Afshar.
* Date: 22/01/14
* Time: 2:38 PM
*/
public class PolynomialInAnInterval extends Interval {

    private ExprLib.ArithExpr polynomial;

    public PolynomialInAnInterval(Double lowBound, Double highBound, ExprLib.ArithExpr polynomial) {
        super(lowBound, highBound);
        this.polynomial = polynomial;
    }

    void setPolynomial(ExprLib.ArithExpr polynomial) {
        this.polynomial = polynomial;
    }

    ExprLib.ArithExpr getPolynomial() {
        return polynomial;
    }

    @Override
    public String toString() {
        return polynomial + " : [" + lowBound + ", " + highBound + "]";
    }

}
