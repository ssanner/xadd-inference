package hgm.poly.gm;

import hgm.poly.FactorizedPiecewiseStructure;
import hgm.poly.Fraction;

/**
 * Created by Hadi Afshar.
 * Date: 8/09/14
 * Time: 10:04 PM
 */
@Deprecated // I just want to use rich joint wrapper //todo replace this with RichJointWrapper everywhere...
public class JointWrapper {
//    private PiecewiseExpression<Fraction> joint;
    private FactorizedPiecewiseStructure<Fraction> joint;
    private double minVarLimit;
    private double maxVarLimit;

    public JointWrapper(
//            PiecewiseExpression<Fraction> joint,
            FactorizedPiecewiseStructure<Fraction> joint,
            double minVarLimit, double maxVarLimit) {
        this.joint = joint;
        this.minVarLimit = minVarLimit;
        this.maxVarLimit = maxVarLimit;
    }

//    public PiecewiseExpression<Fraction> getJoint() {
    public FactorizedPiecewiseStructure<Fraction> getJoint() {
        return joint;
    }

    public double getMinLimitForAllVars(){
        return minVarLimit;
    }
    public double getMaxLimitForAllVars(){
        return maxVarLimit;
    }

//    @Deprecated
//    public int getAppropriateSampleVectorSize() {
//        return joint.getScopeVars().size();
//    }
}
