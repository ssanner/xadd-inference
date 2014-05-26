package hgm.poly.market;

import hgm.poly.Function;
import hgm.poly.bayesian.GatedGibbsGeneralBayesianSampler;
import hgm.poly.bayesian.GeneralBayesianPosteriorHandler;
import hgm.poly.bayesian.PriorHandler;
import hgm.poly.bayesian.RejectionBasedGeneralBayesianSampler;
import hgm.poly.sampling.SamplerInterface;
import hgm.poly.sampling.SamplingUtils;
import hgm.poly.vis.FunctionVisualizer;
import hgm.sampling.VarAssignment;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Hadi Afshar.
 * Date: 28/04/14
 * Time: 7:44 AM
 */
public class BayesianMarketMakingModelTest {
    public static String SAMPLES_FILE_PATH = "./test/hgm/poly/market/";//scatter2D.txt";

    public static void main(String[] args) throws FileNotFoundException {
        BayesianMarketMakingModelTest instance = new BayesianMarketMakingModelTest();
        instance.basicTest();
//        instance.testPriorMaking();
//        instance.testOnDummyDb();
    }

    MarketMakingDatabase testDB1 = new MarketMakingDatabase(
//            PriorHandler.uniformInEllipse("v", 30, 10), 8.001)
            PriorHandler.serialDependent("v", 2, 15, 10)
            , 8.001) {
        List<TradingResponse> deals = new ArrayList<TradingResponse>();

        {
//                deals.add(new TradingResponse(0, -8, 5, TradersChoice.SELL));
            deals.add(new TradingResponse(1, -5, 4, TradersChoice.SELL));
            deals.add(new TradingResponse(1, 2, 3, TradersChoice.BUY));
            deals.add(new TradingResponse(0, 1, 5, TradersChoice.NO_DEAL));
        }

        @Override
        public int getNumberOfParameters() {
            return 2;  //Two kind of instruments to deal...
        }

        @Override
        public List<TradingResponse> getObservedDataPoints() {
            return deals;
        }
    };

    @Test
    public void basicTest() throws FileNotFoundException {

        BayesianMarketMakingModel learning = new BayesianMarketMakingModel(testDB1);
//                BayesianMarketMakingModel.uniformInEllipse(30, 10),
        ////.uniformInHypercube(20)
//                        testDB1, 8.001, "v");

        // Pr(W | R^{n+1})
        GeneralBayesianPosteriorHandler posterior = learning.computeBayesianPosterior();
//        fixVarLimits(context, utilityWeights, -30d, 60d);

        FunctionVisualizer.visualize(posterior, -25d, 25d, 0.5, "Poly");
        FunctionVisualizer.save3DSurf(posterior, -20, 20, -20, 20, 0.5, SAMPLES_FILE_PATH + "synthetic");


        //now I sample from it:
        SamplerInterface sampler =
                new GatedGibbsGeneralBayesianSampler(posterior, /*-40, 40,*/ null);
//                new RejectionBasedGeneralBayesianSampler(posterior, 1);
//                GatedGibbsPolytopesSampler.makeGibbsSampler(posterior, -GPolyPreferenceLearning.C - 10, GPolyPreferenceLearning.C + 10, new Double[]{-5.771840329479172, 7.1312683349054});
//                SymbolicGibbsPolytopesSampler.makeSampler(posterior, -GPolyPreferenceLearning.C - 10, GPolyPreferenceLearning.C + 10, new Double[]{-5.771840329479172, 7.1312683349054});


//        GatedGibbsPolytopesSampler.DEBUG = false;
        for (int i = 0; i < 4; i++) {
            Double[] assign = sampler.sample();
            System.out.println(">>>>>>>>> t  = " + Arrays.toString(assign));
            System.out.println("posterior(t) = " + posterior.evaluate(assign));
        }

        long t1 = System.currentTimeMillis();
//        GatedGibbsPolytopesSampler.DEBUG = false;
        SamplingUtils.save2DSamples(sampler, 10000, SAMPLES_FILE_PATH + "scatterGibbs");
        long t2 = System.currentTimeMillis();
        System.out.println("Time: " + (t2 - t1) + " \t That was all the folk!");


    }

    @Test
    public void testOnDummyDb() {
        double c = 20;
        double epsilon = 1.5d;
        MarketMakingDatabase db = new DummyMarketMakingDatabase(20, PriorHandler.uniformInHypercube("v", 2, epsilon), epsilon);
        BayesianMarketMakingModel learning = new BayesianMarketMakingModel(db);
//                BayesianMarketMakingModel.uniformInHypercube(c),
//                db, epsilon, "v");

        // Pr(W | R^{n+1})
        GeneralBayesianPosteriorHandler posterior = learning.computeBayesianPosterior();
//        fixVarLimits(context, utilityWeights, -30d, 60d);

        double PresentationBound = 25d;
        FunctionVisualizer.visualize(posterior, -PresentationBound, PresentationBound, 0.5, "Poly");
    }

    void testPriorMakingUniEllipse() {
        final double c = 10.0;
        Function func = new Function() {
            double a = 6.0;
            double b = 2.0;
            //            double r = 3;
            double x0 = 0;
            double y0 = 5;
            double theta = -Math.PI / 4.0; //static
            double consTheta = Math.cos(theta); //static
            double sinTheta = Math.sin(theta); //static

            String[] vars = new String[]{"x", "y"};

            @Override
            public double evaluate(VarAssignment fullVarAssign) {
                double x = fullVarAssign.getContinuousVar("x");
                double y = fullVarAssign.getContinuousVar("y");
                if (f(x, y) < 0) return 1.0;
                else return 0.0;

            }

            private double f(double x, double y) {
//                double x = xx-x0;
//                double y = yy-y0;

                x = x - x0;
                y = y - y0;

                double xx = Math.cos(theta) * x - Math.sin(theta) * y;
                double yy = Math.sin(theta) * x + Math.cos(theta) * y;

//                xx = xx - x0; yy =yy - y0;
                return (xx * xx) / (a * a) + (yy * yy) / (b * b) - 1;//r*r;
            }

            @Override
            public String[] collectContinuousVars() {
                return vars;
            }
        };
        FunctionVisualizer.visualize(func, -c, c, 0.1, "");
    }

    void testPriorMaking() {
        final double c = 10.0;
        Function func = new Function() {
            double a = 6.0;
            double b = 2.0;
            double c = 0.1;

            double x0 = 0;
            double y0 = 5;
            double theta = -Math.PI / 4.0; //static
            double consTheta = Math.cos(theta); //static
            double sinTheta = Math.sin(theta); //static

            String[] vars = new String[]{"x", "y"};

            @Override
            public double evaluate(VarAssignment fullVarAssign) {
                double x = fullVarAssign.getContinuousVar("x");
                double y = fullVarAssign.getContinuousVar("y");
                double f = ellipseFunc(x, y);
                return f < 0 ? -f * (c * c) : 0.0;

            }

            private double ellipseFunc(double x, double y) {
//                double x = xx-x0;
//                double y = yy-y0;

                x = x - x0;
                y = y - y0;

                double xx = Math.cos(theta) * x - Math.sin(theta) * y;
                double yy = Math.sin(theta) * x + Math.cos(theta) * y;

//                xx = xx - x0; yy =yy - y0;
                return (xx * xx) / (a * a) + (yy * yy) / (b * b) - 1;
            }

            /////////////////////////////////////////////////////

//            private double g(double x, double y) {
//
//            }

            @Override
            public String[] collectContinuousVars() {
                return vars;
            }
        };
        FunctionVisualizer.visualize(func, -c, c, 0.1, "");
    }


}
