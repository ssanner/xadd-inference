package hgm.poly.pref;

import hgm.poly.bayesian.PriorHandler;
import hgm.poly.integral.OneDimFunction;
import hgm.poly.sampling.SamplerInterface;
import hgm.poly.sampling.SamplingUtils;
import hgm.poly.vis.FunctionVisualizer;
import hgm.preference.Choice;
import hgm.preference.Preference;
import hgm.preference.db.PreferenceDatabase;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Hadi Afshar.
 * Date: 12/03/14
 * Time: 5:53 AM
 */
public class SymbolicGibbsPolytopesSamplerOnBpplTest {
    public static String SAMPLES_FILE_PATH = "./test/hgm/poly/sampling/";//scatter2D.txt";
    public static void main(String[] args) throws FileNotFoundException {
        SymbolicGibbsPolytopesSamplerOnBpplTest instance = new SymbolicGibbsPolytopesSamplerOnBpplTest();
//        instance.basicTest();
        instance.basicTestSymbolicGibbs();
    }


    PreferenceDatabase testDB1 = new PreferenceDatabase(
            PriorHandler.uniformInHypercube("w", 2, BayesianPairwisePreferenceLearningModel.C)
//            PriorHandler.uniformInEllipse("w", 10, 10)
//            PriorHandler.quadraticInEllipse("w", 10, 10)
    ) {
        Preference[] prefs = new Preference[]{
                new Preference(1, 2, Choice.FIRST),
                new Preference(1, 3, Choice.FIRST),
                new Preference(2, 3, Choice.FIRST),
        };

        List<Double[]> items = new ArrayList<Double[]>(5);

        {
            items.add(new Double[]{10.0, 5.0});
            items.add(new Double[]{5.0, 3.0});
            items.add(new Double[]{6.0, 2.0});
            items.add(new Double[]{6.0, 3.0});
            items.add(new Double[]{40.0, 25.0});
        }

        @Override
        public int getNumberOfParameters() {
            return items.get(0).length;
        }

        @Override
        public int getNumberOfItems() {
            return items.size();
        }

        @Override
        public List<Preference> getObservedDataPoints() {
            return Arrays.asList(prefs);
        }

        @Override
        public Double[] getItemAttributeValues(int itemId) {
            return items.get(itemId);
        }

        @Override
        public double[] getAuxiliaryWeightVector() {
            return null;
        }
    }; //end inner class.

   /* @Test
    public void basicTest() throws FileNotFoundException {

        BayesianPairwisePreferenceLearningModel learning = new BayesianPairwisePreferenceLearningModel(testDB1, 0.3);

        // Pr(W | R^{n+1})
        ConstantBayesianPosteriorHandler posterior = learning.computePosteriorWeightVector(Integer.MAX_VALUE);
//        fixVarLimits(context, utilityWeights, -30d, 60d);

        FunctionVisualizer.visualize(posterior, -15d, 15d, 0.5, "Poly");
        FunctionVisualizer.save3DSurf(posterior, -20, 20, -20, 20, 0.5, SAMPLES_FILE_PATH + "synthetic");


        //now I sample from it:
        SamplerInterface sampler =
                GatedGibbsPolytopesSampler.makeSampler(posterior, -BayesianPairwisePreferenceLearningModel.C - 10, BayesianPairwisePreferenceLearningModel.C + 10, new Double[]{-5.771840329479172, 7.1312683349054});
//                SymbolicGibbsPolytopesSampler.makeSampler(posterior, -BayesianPairwisePreferenceLearningModel.C - 10, BayesianPairwisePreferenceLearningModel.C + 10, new Double[]{-5.771840329479172, 7.1312683349054});


//        GatedGibbsPolytopesSampler.DEBUG = false;
        for (int i = 0; i < 2; i++) {
            Double[] assign = sampler.reusableSample();
            System.out.println("t = " + Arrays.toString(assign));
        }

        long t1 = System.currentTimeMillis();
//        GatedGibbsPolytopesSampler.DEBUG = false;
        SamplingUtils.save2DSamples(sampler, 10000, SAMPLES_FILE_PATH + "scatter2D");//);"scatterGibbs");
        long t2 = System.currentTimeMillis();
        System.out.println("Time: "+ (t2-t1) + " \t That was all the folk!");
    }*/

    @Test
    public void basicTestSymbolicGibbs() throws FileNotFoundException {

        BayesianPairwisePreferenceLearningModel learning = new BayesianPairwisePreferenceLearningModel(testDB1, 0.2);//0.3);

        // Pr(W | R^{n+1})
        final ConstantBayesianPosteriorHandler posterior = learning.computePosteriorWeightVector(Integer.MAX_VALUE);
//        fixVarLimits(context, utilityWeights, -30d, 60d);

        FunctionVisualizer.visualize(posterior, -15d, 15d, 0.5, "Poly");
        FunctionVisualizer.save3DSurf(posterior, -20, 20, -20, 20, 0.5, SAMPLES_FILE_PATH + "synthetic");

        /*FunctionVisualizer.visualize(new OneDimFunction() {
            @Override
            public double eval(double var) {
                return posterior.evaluate(new Double[]{-8d, var});
            }
        }, -15d, 15d, 0.01, "w+0 fixed to -8...");
*/

        //now I sample from it:
        SamplerInterface sampler =
//                GatedGibbsPolytopesSampler.makeSampler(posterior, -BayesianPairwisePreferenceLearningModel.C - 10, BayesianPairwisePreferenceLearningModel.C + 10, new Double[]{-5.771840329479172, 7.1312683349054});
                SymbolicGibbsPolytopesSampler.makeSampler(posterior, -BayesianPairwisePreferenceLearningModel.C - 10, BayesianPairwisePreferenceLearningModel.C + 10, null);


//        GatedGibbsPolytopesSampler.DEBUG = false;
        for (int i = 0; i < 10; i++) {
            Double[] assign = sampler.reusableSample();
            System.out.println("t = " + Arrays.toString(assign));
        }

        long t1 = System.currentTimeMillis();
//        GatedGibbsPolytopesSampler.DEBUG = false;
        SamplingUtils.save2DSamples(sampler, 10000, SAMPLES_FILE_PATH + "scatter2D");//);"scatterGibbs");
        long t2 = System.currentTimeMillis();
        System.out.println("Time: "+ (t2-t1) + " \t That was all the folk!");
    }

    //***********************************************************************

  /*  @Test
    public void basicTestClever() throws FileNotFoundException {

        BayesianPairwisePreferenceLearningModel learning = new BayesianPairwisePreferenceLearningModel(testDB1, 0.1);

        // Pr(W | R^{n+1})
        ConstantBayesianPosteriorHandler posterior = learning.computePosteriorWeightVector(Integer.MAX_VALUE);
//        fixVarLimits(context, utilityWeights, -30d, 60d);

        FunctionVisualizer.visualize(posterior, -30d, 60d, 0.5, "Poly");


        //now I sample from it:
        TargetedGatedGibbsPolytopesSampler sampler = TargetedGatedGibbsPolytopesSampler.makeCleverGibbsSampler(posterior, -BayesianPairwisePreferenceLearningModel.C - 10, BayesianPairwisePreferenceLearningModel.C + 10, new Double[]{-5.771840329479172, 7.1312683349054});
        TargetedGatedGibbsPolytopesSampler.DEBUG = false;
        for (int i = 0; i < 2; i++) {
            Double[] assign = sampler.reusableSample();
            System.out.println("t = " + Arrays.toString(assign));
        }

        long t1 = System.currentTimeMillis();
        GatedGibbsPolytopesSampler.DEBUG = false;
        SamplingUtils.save2DSamples(sampler, 10000, SAMPLES_FILE_PATH + "scatterGibbs");
        long t2 = System.currentTimeMillis();
        System.out.println("Time: "+ (t2-t1) + " \t That was all the folk!");
    }
*/
}
