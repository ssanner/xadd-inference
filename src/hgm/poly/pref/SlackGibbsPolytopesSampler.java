package hgm.poly.pref;

/**
 * Created by Hadi Afshar.
 * Date: 11/03/14
 * Time: 9:05 PM
 */
//todo never finished.....
    @Deprecated
public class SlackGibbsPolytopesSampler{}/* extends AbstractPolytopesSampler {

    private int sightExtent; //how many neighbouring regions to look through.... //todo change name while not myopic anymore

    //todo while posterior directly works with GPolyPreferenceLearning.C, the idea of min/max for all vars can be bug prone....
    public static SlackGibbsPolytopesSampler makeGibbsSampler(PolytopesHandler gph, double minForAllVars, double maxForAllVars, Double[] reusableInitialSample, int sightExtent) {
        int varNum = gph.getPolynomialFactory().getAllVars().length;
        double[] cVarMins = new double[varNum];
        double[] cVarMaxes = new double[varNum];
        Arrays.fill(cVarMins, minForAllVars);
        Arrays.fill(cVarMaxes, maxForAllVars);
        return new SlackGibbsPolytopesSampler(gph, cVarMins, cVarMaxes, reusableInitialSample, sightExtent);
    }

    public SlackGibbsPolytopesSampler(PolytopesHandler gph, double[] cVarMins, double[] cVarMaxes, Double[] reusableInitialSample, int sightExtent) {
        super(gph, cVarMins, cVarMaxes, reusableInitialSample);
        this.sightExtent = sightExtent;
    }

    @Override
    protected void sampleSingleContinuousVar(String varToBeSampled, int varIndexToBeSampled, Double[] reusableVarAssign) {

        //todo: important: it is wrong to sample from all variables in the factory.... One should only sample from the variables in the cp

        //todo VERY IMPORTANT: THIS ONLY WORKS FOR CUBE-LIKE PRIORS, FIX FOR GENERAL CASE.....WHERE MIN, MAX are CHOSEN BY INSTANTIATION OF THE PRIOR....
//        double minVarValue = -GPolyPreferenceLearning.C;
//        double maxVarValue = GPolyPreferenceLearning.C;
        double minVarValue = cVarMins[varIndexToBeSampled];
        double maxVarValue = cVarMaxes[varIndexToBeSampled];


        Set<List<Boolean>> chosenMasks = new HashSet<List<Boolean>>(sightExtent +1); //'1' for the current region

        List<Boolean> currentGateMask = gph.adjustedReusableGateActivationMask(reusableVarAssign); //current mask should be in the list of chosen masks.
        chosenMasks.add((ArrayList<Boolean>) (((ArrayList<Boolean>) (currentGateMask)).clone()));  //code smell!

        for (int i=0; i<sightExtent; i++) {
            double sx = randomDoubleUniformBetween(minVarValue, maxVarValue); //todo maybe other distributions such as Gaussian can be used...
            reusableVarAssign[varIndexToBeSampled] = sx;
            double sy = gph.evaluate(reusableVarAssign);
            if (sy>0) {
                //if its region is not currently chosen then choose it:
                List<Boolean> mask = gph.adjustedReusableGateActivationMask(reusableVarAssign);
                chosenMasks.add((ArrayList<Boolean>)(((ArrayList<Boolean>)(mask)).clone()));
            }
            System.out.println("chosenMasks = " + chosenMasks);
            //now I have a set of regions by which I can do Gibbs....
            //NOTE:integration in constant zones is tricky... it should be shifted to left or right to reach non constant....
            //todo I was here.....*********************************************************************************

        }

        ConstrainedPolynomial polytope = gph.makePolytope(currentGateMask);
        //it is assumed that samples are taken from a non zero function i.e. not OneDimFunction.ZERO_1D_FUNCTION, therefore should be a PiecewiseOffset1DPolynomial:
        final PiecewiseOffset1DPolynomial varCDF = (PiecewiseOffset1DPolynomial)makeCumulativeDistributionFunction(polytope, varToBeSampled, reusableVarAssign);
        System.out.println("varCDF.getIntervals() = " + varCDF.getIntervals());
        varCDF.getIntervals().get(0).getLowBound()


        //I choose 2 (or later on at least 2) constraint polytopes by choosing a single gate variable
//        int chosenGateVarIndex = random.nextInt(gateMask.size()*//*exclusive*//*);

//        if (DEBUG) System.out.println("gateMask = " + gateMask + "\t chosen gate index: " + chosenGateVarIndex);

        currentGateMask.set(chosenGateVarIndex, true);
        ConstrainedPolynomial posKeyPolytope = gph.makePolytope(currentGateMask);     //todo at least one of the polytopes can be cached...
        if (DEBUG) {
            FunctionVisualizer.visualize(posKeyPolytope, -11, 11, 0.1, "pos key");
        }

        currentGateMask.set(chosenGateVarIndex, false);
        ConstrainedPolynomial negKeyPolytope = gph.makePolytope(currentGateMask);
        if (DEBUG) {
            FunctionVisualizer.visualize(negKeyPolytope, -11, 11, 0.1, "neg key");
        }

//        System.out.println("(before +- CDF) reusableVarAssign = " + Arrays.toString(reusableVarAssign) + "\t var to be sampled: " + varToBeSampled);
        final OneDimFunction posVarCDF = makeCumulativeDistributionFunction(posKeyPolytope, varToBeSampled, reusableVarAssign);
        if (DEBUG) {
            FunctionVisualizer.visualize(posVarCDF, -11, 11, 0.1, "posCDF");
        }

        final OneDimFunction negVarCDF = makeCumulativeDistributionFunction(negKeyPolytope, varToBeSampled, reusableVarAssign);
        if (DEBUG) {
            FunctionVisualizer.visualize(negVarCDF, -11, 11, 0.1, "negCDF");
        }

        OneDimFunction varCDF = new OneDimFunction() {
            @Override
            public double eval(double var) {

                double posV = posVarCDF.eval(var);
//                System.out.println("posV("+var+") = " + posV);
                double negV = negVarCDF.eval(var);
//                System.out.println("negV("+var+") = " + negV);
                return posV + negV;
            }
        };

        if (DEBUG) {
            FunctionVisualizer.visualize(varCDF, -50, 50, 0.1, "+-CDF");
        }

        double maxVarValue = cVarMaxes[varIndexToBeSampled];
        double minVarValue = cVarMins[varIndexToBeSampled];

        double s = takeSampleFrom1DFunc(varCDF, minVarValue, maxVarValue);

        // here the sample is stored....
        reusableVarAssign[varIndexToBeSampled] = s;
    }
}*/