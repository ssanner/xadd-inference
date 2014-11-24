package hgm.poly.gm;

import hgm.poly.sampling.SamplerInterface;

/**
 * Created by Hadi Afshar.
 * Date: 18/08/14
 * Time: 10:34 PM
 */
public interface JointToSampler {
//    SamplerInterface makeSampler(PiecewiseExpression<Fraction> joint, double minLimitForAllVars, double maxLimitForAllVars);
    SamplerInterface makeSampler(JointWrapper jointWrapper);

    String getName();
}


