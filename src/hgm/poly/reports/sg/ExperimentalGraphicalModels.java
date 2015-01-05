package hgm.poly.reports.sg;

import hgm.poly.Fraction;
import hgm.poly.PiecewiseExpression;
import hgm.poly.PolynomialFactory;
import hgm.poly.gm.*;

import java.util.List;
import java.util.Map;

/**
 * Created by Hadi Afshar.
 * Date: 8/09/14
 * Time: 10:59 PM
 */
public class ExperimentalGraphicalModels {

    public static GraphicalModel makeCircuitModel(int n /*num. Resistances*/,
                                                  Double lowBound, Double highBound) {
        // 1/R_t = 1/R_1 + 1/R_2 + ... + 1/R_n

        String[] vars = new String[n + 1];
        for (int i = 0; i < n; i++) {
            vars[i] = "R_" + (i + 1);     //R_1, ..
        }
        vars[n] = "R_t"; //total R

        PolynomialFactory factory = new PolynomialFactory(vars);
        Distributions dBank = new Distributions(factory);

        BayesNetGraphicalModel bn = new BayesNetGraphicalModel();

        PiecewiseExpression<Fraction>[] rsF = new PiecewiseExpression[n];
        for (int i = 0; i < n; i++) {

            rsF[i] =
//                    (i==0) ?
                    dBank.createUniformDistributionFraction("R_" + (i + 1), lowBound.toString(), highBound.toString());
//                   :
//                    dBank.createNonNormalizedBellShapedDistribution("R_" + (i + 1), lowBound.toString(), highBound.toString());//createUniformDistributionFraction("R_" + (i + 1), lowBound.toString(), highBound.toString());
            bn.addFactor(new StochasticVAFactor("R_" + (i + 1), rsF[i])); //mass

        }

        Fraction rtF = factory.makeFraction(reciprocalSumOfReciprocals("R_", n)); // 1/R_t = 1/R_1 + ... + 1/R_n

        bn.addFactor(new DeterministicFactor("R_t", rtF)); //total momentum
        return bn;
    }

    static String reciprocalSumOfReciprocals(String varPrefix, int n) {
        StringBuilder[] ss = new StringBuilder[n + 1];
        for (int i = 0; i < ss.length; i++) {
            ss[i] = new StringBuilder();
        }

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < ss.length; j++) {
                if (j != i) {
                    ss[j].append(varPrefix + (i + 1) + "^(1) *");
                }
            }
        }

        StringBuilder numerator = new StringBuilder();
        for (int i = 0; i < n; i++) {
            numerator.append(ss[i].substring(0, ss[i].length() - 1)).append(" +"); //to remove last '*'
        }

        return "[" + ss[n].substring(0, ss[n].length() - 1) + "] / [" + numerator.substring(0, numerator.length() - 1) + "]";
    }

    /*///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static GraphicalModel makeSimplifiedDoubleFermentationModel(int n *//*num colliding objects*//*,
                                                                 Double lowerBound, Double upperBound) {
//                                                 double minVarLimit, double maxVarLimit,
//                                                 JointToSampler jointToSampler) {
        // l_1 --> l_2 --> ... --> l_n--         // m_1 --> m_2 --> ... --> m_n---
        //  |       |            |      \        //  |       |            |       \
        //  \_______\____________\____q  \       //  \_______\____________\____r   \
        //                                \_________________________________________\_____s = m_n - l_n

        String[] vars = new String[2*n + 3];
        for (int i = 0; i < n; i++) {
            vars[2*i] = "l_" + (i + 1);     // lactose at time step i
            vars[2*i + 1] = "m_" + (i + 1);     // lactose at time step i
//            vars[3 * i + 1] = "k_" + (i + 1); // not used...
//            vars[3 * i + 2] = "p_" + (i + 1); // not used...
        }
        vars[2 * n] = "q";
        vars[2 * n+1] = "r";
        vars[2 * n+2] = "s";

        PolynomialFactory factory = new PolynomialFactory(vars);
        Distributions dBank = new Distributions(factory);

        BayesNetGraphicalModel bn = new BayesNetGraphicalModel();

        PiecewiseExpression<Fraction>[] lFs = new PiecewiseExpression[n];
        PiecewiseExpression<Fraction>[] mFs = new PiecewiseExpression[n];

        String averageQ = "";
        String averageR = "";
        for (int i = 0; i < n; i++) {
            lFs[i] = i == 0 ?
                    dBank.createUniformDistributionFraction("l_1", lowerBound.toString(), upperBound.toString())
                    : dBank.createUniformDistributionFraction("l_" + (i + 1), lowerBound.toString(), "l_" + i + "^(1)");
            mFs[i] = i == 0 ?
                    dBank.createUniformDistributionFraction("m_1", lowerBound.toString(), upperBound.toString())
                    : dBank.createUniformDistributionFraction("m_" + (i + 1), lowerBound.toString(), "m_" + i + "^(1)");
            averageQ += ("l_" + (i + 1) + "^(1) +");
            averageR += ("m_" + (i + 1) + "^(1) +");
        }
        averageQ = averageQ.substring(0, averageQ.length() - 1); //removing last "+"
        averageR = averageR.substring(0, averageR.length() - 1); //removing last "+"

        Fraction averageQF = factory.makeFraction(averageQ, "" + n); // [l_1^(1) + ... + l_n^(1)]/[n]
        Fraction averageRF = factory.makeFraction(averageR, "" + n); // [m_1^(1) + ... + m_n^(1)]/[n]

        for (int i = 0; i < n; i++) {
            bn.addFactor(new StochasticVAFactor("l_" + (i + 1), lFs[i]));
            bn.addFactor(new StochasticVAFactor("m_" + (i + 1), mFs[i]));
        }

        bn.addFactor(new DeterministicFactor("q", averageQF));
        bn.addFactor(new DeterministicFactor("r", averageRF));
        bn.addFactor(new DeterministicFactor("s", factory.makeFraction("q^(1) + -1*r^(1)", "" + n)));
        return bn;
    }



*/
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static GraphicalModel makeSimplifiedFermentationModel(int n /*num colliding objects*/,
                                                                 Double minLactoseAlpha, Double maxInitialLactoseBeta) {
//                                                 double minVarLimit, double maxVarLimit,
//                                                 JointToSampler jointToSampler) {
        // l_1 --> l_2 --> ... --> l_n
        //  |       |            |
        //  \_______\____________\____q //average ph

        String[] vars = new String[n + 1];
        for (int i = 0; i < n; i++) {
            vars[i] = "l_" + (i + 1);     // lactose at time step i
//            vars[3 * i + 1] = "k_" + (i + 1); // not used...
//            vars[3 * i + 2] = "p_" + (i + 1); // not used...
        }
        vars[n] = "q";

        PolynomialFactory factory = new PolynomialFactory(vars);
        Distributions dBank = new Distributions(factory);

        BayesNetGraphicalModel bn = new BayesNetGraphicalModel();

        PiecewiseExpression<Fraction>[] lactoseFs = new PiecewiseExpression[n];
//        PiecewiseExpression<Fraction>[] pHFs = new PiecewiseExpression[n];

        String averagePH = "";
        for (int i = 0; i < n; i++) {
            lactoseFs[i] = i == 0 ?
                    dBank.createUniformDistributionFraction("l_1", minLactoseAlpha.toString(), maxInitialLactoseBeta.toString())
                    : dBank.createUniformDistributionFraction("l_" + (i + 1), minLactoseAlpha.toString(), "l_" + i + "^(1)");
            averagePH += ("l_" + (i + 1) + "^(1) +");
        }
        averagePH = averagePH.substring(0, averagePH.length() - 1); //removing last "+"

        Fraction averagePhF = factory.makeFraction(averagePH, "" + n); // [l_1^(1) + ... + l_n^(1)]/[n]

        for (int i = 0; i < n; i++) {
            bn.addFactor(new StochasticVAFactor("l_" + (i + 1), lactoseFs[i])); //mass
        }

        bn.addFactor(new DeterministicFactor("q", averagePhF)); //total momentum
        return bn;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static GraphicalModel makeFermentationModel(int n /*num colliding objects*/,
                                                       Double pDistributionParam,
                                                       Double minLactoseAlpha, Double maxInitialLactoseBeta) {
//                                                 double minVarLimit, double maxVarLimit,
//                                                 JointToSampler jointToSampler) {
        // a_1 --> a_2 --> ... --> a_n
        //  |       |            |
        //  p_1     p_2          p_n
        //   \______\____________\____q //average ph

        String[] vars = new String[3 * n + 1];
        for (int i = 0; i < n; i++) {
            vars[3 * i] = "l_" + (i + 1);     //lactose at time step i
            vars[3 * i + 1] = "k_" + (i + 1); //"K. Marxianus" at time step i      //todo not used...
            vars[3 * i + 2] = "p_" + (i + 1); //observation (of pH) at time step i
        }
        vars[3 * n] = "q";

        PolynomialFactory factory = new PolynomialFactory(vars);
        Distributions dBank = new Distributions(factory);

        BayesNetGraphicalModel bn = new BayesNetGraphicalModel();

        PiecewiseExpression<Fraction>[] lactoseFs = new PiecewiseExpression[n];
        PiecewiseExpression<Fraction>[] pHFs = new PiecewiseExpression[n];
//        PiecewiseExpression<Fraction>[] kMarxianusFs = new PiecewiseExpression[n];

//        Fraction[] momentaF = new Fraction[n];
        String averagePH = "";
//        String totalMassFormula = "";
//        double c = 1; //just a bound
        for (int i = 0; i < n; i++) {
            lactoseFs[i] = i == 0 ?
                    dBank.createUniformDistributionFraction("l_1", minLactoseAlpha.toString(), maxInitialLactoseBeta.toString())
                    : dBank.createUniformDistributionFraction("l_" + (i + 1), minLactoseAlpha.toString(), "l_" + i + "^(1)");
            pHFs[i] = dBank.createUniformDistributionFraction("p_" + (i + 1), "l_" + (i + 1) + "^(1) + " + (-pDistributionParam), "l_" + (i + 1) + "^(1) + " + pDistributionParam);//TruncatedNormalDistributionFraction("p_"  + (i + 1), )//todo test other things... E.g., TRIANGULAR
//            momentaF[i] = factory.makeFraction("m_" + (i + 1) + "^(1) * v_" + (i + 1) + "^(1)");
            averagePH += ("p_" + (i + 1) + "^(1) +");
//            totalMassFormula += ("m_" + (i + 1) + "^(1) +");
        }
        averagePH = averagePH.substring(0, averagePH.length() - 1); //removing last "+"
//        totalMassFormula = totalMassFormula.substring(0, totalMassFormula.length() - 1); //removing last "+"

        Fraction averagePhF = factory.makeFraction(averagePH, "" + n); // m_1^(1)*v_1^(1) + m_2^(1)*v_2^(1) + ...
//        Fraction mtF = factory.makeFraction(totalMassFormula);
//        Fraction vtF = factory.makeFraction("[p_t^(1)]/[m_t^(1)]");

        for (int i = 0; i < n; i++) {
            bn.addFactor(new StochasticVAFactor("l_" + (i + 1), lactoseFs[i])); //mass
            bn.addFactor(new StochasticVAFactor("p_" + (i + 1), pHFs[i]));
//            bn.addFactor(new DeterministicFactor("p_" + (i + 1), momentaF[i])); //momentum
        }

        bn.addFactor(new DeterministicFactor("q", averagePhF)); //total momentum
//        bn.addFactor(new DeterministicFactor("m_t", mtF)); //total mass (after collision)
//        bn.addFactor(new DeterministicFactor("v_t", vtF)); //total velocity (after collision)
        return bn;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static GraphicalModel makeCollisionModel(int n /*num colliding objects*/,
                                                    Double muAlpha, Double muBeta,
                                                    Double nuAlpha, Double nuBeta,
                                                    boolean symmetric) {
//                                                 double minVarLimit, double maxVarLimit,
//                       (?)                          JointToSampler jointToSampler) {
        // m_1      v_1 ---->  v_n     m_2
        //   \__p_1__/        \__p_n__/
        //       \______p_t______/


        String[] vars = new String[3 * n + 3];
        for (int i = 0; i < n; i++) {
            vars[3 * i] = "m_" + (i + 1);     //mass of i-th object
            vars[3 * i + 1] = "v_" + (i + 1); //velocity of i-th object
            vars[3 * i + 2] = "p_" + (i + 1); //momentum of i-th object
        }
        vars[3 * n] = "m_t"; //total mass
        vars[3 * n + 1] = "v_t"; //total velocity
        vars[3 * n + 2] = "p_t"; //total momentum


        PolynomialFactory factory = new PolynomialFactory(vars);
        Distributions dBank = new Distributions(factory);

        BayesNetGraphicalModel bn = new BayesNetGraphicalModel();

        PiecewiseExpression<Fraction>[] massesF = new PiecewiseExpression[n];
        PiecewiseExpression<Fraction>[] velocitiesF = new PiecewiseExpression[n];
        Fraction[] momentaF = new Fraction[n];
        String totalMomentumFormula = "";
        String totalMassFormula = "";
        for (int i = 0; i < n; i++) {
            massesF[i] = dBank.createUniformDistributionFraction("m_" + (i + 1), muAlpha.toString(), muBeta.toString());
            if (symmetric) {
                velocitiesF[i] = dBank.createUniformDistributionFraction("v_" + (i+1), nuAlpha.toString(), nuBeta.toString());
            } else {
                velocitiesF[i] = i == 0 ? dBank.createUniformDistributionFraction("v_1", nuAlpha.toString(), nuBeta.toString())
                        : dBank.createUniformDistributionFraction("v_" + (i + 1), nuAlpha.toString(), "v_" + i + "^(1)");
            }
            momentaF[i] = factory.makeFraction("m_" + (i + 1) + "^(1) * v_" + (i + 1) + "^(1)");
            totalMomentumFormula += ("p_" + (i + 1) + "^(1) +");
            totalMassFormula += ("m_" + (i + 1) + "^(1) +");
        }
        totalMomentumFormula = totalMomentumFormula.substring(0, totalMomentumFormula.length() - 1); //removing last "+"
        totalMassFormula = totalMassFormula.substring(0, totalMassFormula.length() - 1); //removing last "+"

        Fraction ptF = factory.makeFraction(totalMomentumFormula); // m_1^(1)*v_1^(1) + m_2^(1)*v_2^(1) + ...
        Fraction mtF = factory.makeFraction(totalMassFormula);
        Fraction vtF = factory.makeFraction("[p_t^(1)]/[m_t^(1)]");

        for (int i = 0; i < n; i++) {
            bn.addFactor(new StochasticVAFactor("m_" + (i + 1), massesF[i])); //mass
            bn.addFactor(new StochasticVAFactor("v_" + (i + 1), velocitiesF[i])); //mass 2
            bn.addFactor(new DeterministicFactor("p_" + (i + 1), momentaF[i])); //momentum
        }

        bn.addFactor(new DeterministicFactor("p_t", ptF)); //total momentum
        bn.addFactor(new DeterministicFactor("m_t", mtF)); //total mass (after collision)
        bn.addFactor(new DeterministicFactor("v_t", vtF)); //total velocity (after collision)
        return bn;


//        SamplerInterface sampler = handler.makeSampler(bn, ("v_1 v_" + (n-1)).split(" "), //todo what about this?
//                evidence, minVarLimit, maxVarLimit, jointToSampler
//                FractionalJointBaselineGibbsSampler.makeJointToSampler()
//                FractionalJointRejectionSampler.makeJointToSampler(1)
//                SelfTunedFractionalJointMetropolisHastingSampler.makeJointToSampler(10, 30, 100)
//                FractionalJointMetropolisHastingSampler.makeJointToSampler(10)
//                SymbolicFractionalJointGibbsSampler.makeJointToSampler()
//        );

    }

    //////////////////////////////////////

    public static GraphicalModel makeExponentialRelationshipModel(double n /*exponent*/,
                                                                  Double xL, Double xH,
                                                                  Double yL, Double yH){


        String[] vars = new String[]{"x", "y", "z"};

        PolynomialFactory factory = new PolynomialFactory(vars);
        Distributions dBank = new Distributions(factory);

        BayesNetGraphicalModel bn = new BayesNetGraphicalModel();

//        PiecewiseExpression<Fraction>[] massesF = new PiecewiseExpression[n];
//        PiecewiseExpression<Fraction>[] velocitiesF = new PiecewiseExpression[n];
//        Fraction[] momentaF = new Fraction[n];
//        String totalMomentumFormula = "";
//        String totalMassFormula = "";

        PiecewiseExpression<Fraction> fX = dBank.createUniformDistributionFraction("x", xL.toString(), xH.toString());
        PiecewiseExpression<Fraction> fY = dBank.createUniformDistributionFraction("y", yL.toString(), yH.toString());
        Fraction zF = factory.makeFraction("y^(1) + -1*x^(" + n + ")");  //y = x^n [potential constant]
        bn.addFactor(new StochasticVAFactor("x", fX));
        bn.addFactor(new StochasticVAFactor("y", fY));
        bn.addFactor(new DeterministicFactor("z", zF));

        return bn;

    }



}
