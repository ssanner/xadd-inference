package hgm.poly.reports.sg;

import hgm.poly.Fraction;
import hgm.poly.PiecewiseExpression;

public class JointWrapper {
    PiecewiseExpression<Fraction> joint;
    double minVarLimit;
    double maxVarLimit;

    public JointWrapper(PiecewiseExpression<Fraction> joint, double minVarLimit, double maxVarLimit) {
        this.joint = joint;
        this.minVarLimit = minVarLimit;
        this.maxVarLimit = maxVarLimit;
    }
}
