package hgm.poly.gm;

import hgm.poly.FactorizedPiecewiseStructure;
import hgm.poly.Fraction;
import hgm.poly.PiecewiseExpression;
import hgm.poly.PolynomialFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Hadi Afshar.
 * Date: 8/09/14
 * Time: 11:28 PM
 * Includes more information
 */
public class RichJointWrapper extends JointWrapper{
    private List<DeterministicFactor> eliminatedStochasticVarFactors;
    private List<String> queryVars;

    private GraphicalModel graphicalModel;
    private Map<String, Double> evidence;

    private int[] queryVarIndexes;
    private Double[] reusableQueryVarValues;
    private int qSize;


    public RichJointWrapper(
//            PiecewiseExpression<Fraction> joint,
            FactorizedPiecewiseStructure<Fraction> joint,
                            List<DeterministicFactor> eliminatedStochasticVarFactors,
                            List<String> queryVars, double minVarLimit, double maxVarLimit,
                            GraphicalModel graphicalModel, //model is only given so that in the generated samples, the deterministic variables can be instantiated, given the stochastic ones
                            Map<String, Double> evidence) {
        super(joint, minVarLimit, maxVarLimit);
        this.eliminatedStochasticVarFactors = eliminatedStochasticVarFactors;
        this.queryVars = queryVars;
        qSize = queryVars.size();

        this.graphicalModel = graphicalModel;
        this.evidence = evidence;

        this.queryVarIndexes = new int[qSize];
        PolynomialFactory factory = joint.getFactory();
        for (int i=0; i< qSize; i++){
            queryVarIndexes[i] = factory.getVarIndex(queryVars.get(i));
        }

        reusableQueryVarValues = new Double[qSize];
    }

    public List<DeterministicFactor> getEliminatedStochasticVarFactors() {
        return eliminatedStochasticVarFactors;
    }

    public List<String> getQueryVars() {
        return queryVars;
    }

//    @Override
    public int getAppropriateSampleVectorSize() {
        return qSize; //queryVars.size();
    }

    public GraphicalModel getGraphicalModel() {
        return graphicalModel;
    }

    public Map<String, Double> getEvidence() {
        return evidence;
    }


    //converts full vector sample (possibly stuffed with nulls) with vars in the factory order to query sample with query vector order
    public Double[] reusableQueriedVarValues(Double[] fullVectorSample) {
        for (int i=0; i< qSize; i++) {
            reusableQueryVarValues[i] = fullVectorSample[queryVarIndexes[i]];
        }
        return reusableQueryVarValues;
    }

    private Map<String, String> extraInfoMap = new HashMap<String, String>();
    public void addExtraInfo(String infoKey, String infoValue) {
        if (extraInfoMap.put(infoKey, infoValue) != null) throw new RuntimeException("already exists! ");
    }

    public String extraInfo(String infoKey) {
        return extraInfoMap.get(infoKey);
    }

    public int[] getQueryVarIndexes() {
        return queryVarIndexes;
    }

}
