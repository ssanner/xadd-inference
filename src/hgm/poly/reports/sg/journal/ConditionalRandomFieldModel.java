package hgm.poly.reports.sg.journal;

import hgm.asve.Pair;
import hgm.poly.Fraction;
import hgm.poly.PiecewiseExpression;
import hgm.poly.PolynomialFactory;
import hgm.poly.gm.*;

import java.util.*;

/**
 * Created by Hadi M Afshar.
 * Date: 10/19/15
 * Time: 7:56 PM
 * <p/>
 * cvariables (d x_i)
 * min-values (-10 -10)
 * max-values (20 20)
 * bvariables ( )
 * d ([U(d,0,0,10)])
 * x_i ([0.05*U(x_i,0,0,10) + 0.85*N(x_i,d,2,2.5) + 0.1*T(x_i,10,1,0)])
 * <p/>
 * <p/>
 * ___________________
 * NOTES:
 * -------------------
 * <p/>
 * XADD -> Convert2XADD(.)
 * <p/>
 * N(expr, mu, var, width) where truncated outside +-with
 * U(expr, mu, width1, width2) that is from (mu-width1) to (mu+width2) I think
 * T(expr, mu, width_l, width_r) that is from (mu-width1) to (mu+width2) peaked at mu I think
 * ____________________
 */
public class ConditionalRandomFieldModel implements GraphicalModel {
    PolynomialFactory factory;
    Distributions dBank;

    double normalVariance = 2.0;
    Double normalStdDev = Math.sqrt(normalVariance);
    int[][] varIdArray;

    int nX;
    int nY;

    public String varName(int varId) {
        return "x_" + varId;
    }

    public ConditionalRandomFieldModel(int nX, int nY /*num data*/) {
        this.nX = nX;
        this.nY = nY;

        varIdArray = new int[nX][nY];
        String[] varList = new String[nX * nY];

        int c = 0;
        for (int i = 0; i < nX; i++) {
            for (int j = 0; j < nY; j++) {
                varIdArray[i][j] = c;
                varList[c] = varName(c);
                c++;
            }
        }

        factory = new PolynomialFactory(varList);
        dBank = new Distributions(factory);


    }

    List<String> fetchSouthEastNeighbors(String var) {
        Pair<Integer, Integer> pos = ij(var);
        int i = pos.getFirstEntry();
        int j = pos.getSecondEntry();

        List<String> neighbors = new ArrayList<String>();

//        if (i > 0) neighbors.add(varName(varIdArray[i - 1][j]));
        if (i < nX - 1) neighbors.add(varName(varIdArray[i + 1][j]));
//        if (j > 0) neighbors.add(varName(varIdArray[i][j - 1]));
        if (j < nY - 1) neighbors.add(varName(varIdArray[i][j + 1]));

        return neighbors;
    }

    private Pair<Integer, Integer> ij(String var) {
        int c = Integer.parseInt(var.substring("x_".length()));
        return ij(c);
    }

    private Pair<Integer, Integer> ij(int varId) {
        int i = varId / nY;
        int j = varId % nY;
        if (varIdArray[i][j] != varId) throw new RuntimeException();  //for debug... todo remove
        return new Pair<Integer, Integer>(i, j);
    }

    PiecewiseExpression<Fraction> neighbourFactor(String variable, String neighbourVar) {
        //x_i ([0.05*U(x_i,0,0,10) + 0.85*N(x_i,d,2,2.5) + 0.1*T(x_i,10,1,0)])
//        PiecewiseExpression<Fraction> uniform = dBank.createExhaustiveUniformDistributionFraction(variable, "0", "10");
//        uniform.multiplyScalarInThis(0.05);

        double w = 0.5;
//        PiecewiseExpression<Fraction> normal = dBank.createNormalDistributionIrwinHallApprox(variable, neighbourVar +"^(1)", normalStdDev.toString());
        //what I want is a normal N(x-y; mean = 0; variance = whatever...)
        PiecewiseExpression<Fraction> normal = dBank.createNormalDistributionViaIrwinHallApprox(variable, "0", normalStdDev.toString());
        normal = normal.substitute(variable, factory.makeFraction(variable + "^(1) + -1*" +neighbourVar + "^(1)"));

        normal.multiplyScalarInThis(w);
        Double u = (1 - w) * 0.1;
        PiecewiseExpression<Fraction> out = normal.returnAdd(factory.makeFraction(u.toString()));
        Fraction lowSupport1 = factory.makePolynomial(variable + "^(1)").cloneCastToFraction();// 0 < v
        Fraction highSupport1 = factory.makePolynomial("-1.0 * " + variable + "^(1) +" + 10).cloneCastToFraction();// 10 > v
        Fraction lowSupport2 = factory.makePolynomial(neighbourVar + "^(1)").cloneCastToFraction();// 0 < v
        Fraction highSupport2 = factory.makePolynomial("-1.0 * " + neighbourVar + "^(1) +" + 10).cloneCastToFraction();// 10 > v
        out.addConstraintsToAllCasesInThis(Arrays.asList(lowSupport1, highSupport1, lowSupport2, highSupport2), true);

//        PiecewiseExpression<Fraction> triangular = dBank.createExhaustiveTriangular(variable, "10", "1", "0");
//        triangular.multiplyScalarInThis(0.1);

//        PiecewiseExpression<Fraction> total = triangular.add(normal).add(uniform);//triangular;//uniform.add(normal).add(triangular);
//        total.makeNonExclusive();

        return out;
    }

//    PiecewiseExpression<Fraction> neighbourFactor(String variable, String neighbourVar) {
//        double w = 0.5;
//        PiecewiseExpression<Fraction> normal = dBank.createNormalDistributionIrwinHallApprox(variable, neighbourVar + "^(1)", normalStdDev.toString());
//        normal.multiplyScalarInThis(w);
//        Double u = (1 - w) * 0.1;
//        PiecewiseExpression<Fraction> out = normal.returnAdd(factory.makeFraction(u.toString()));
//        Fraction lowSupport = factory.makePolynomial(variable + "^(1)").cloneCastToFraction();// 0 < v
//        Fraction highSupport = factory.makePolynomial("-1.0 * " + variable + "^(1) +" + 10).cloneCastToFraction();// 10 > v
//        out.addConstraintsToAllCasesInThis(Arrays.asList(lowSupport, highSupport), true);
//        return out;
//    }

    PiecewiseExpression<Fraction> selfFactor(String variable) {
        return null;
    }

    /////////////////////////////////////


    @Override
    public List<Factor> allInferenceRelevantFactors(Collection<String> vars) {
        throw new RuntimeException("not implemented.... use makeJoint directely...");
    }

    @Override
    public List<String> allDeterministicVars() {
        return new ArrayList<String>();  //no deterministic var.
    }

    public int getNumCRFieldNodes() {
        return nX * nY;
    }

    public int oppositeNodeId(int aRandomNodeId) {
        Pair<Integer, Integer> ij = ij(aRandomNodeId);
        int i = ij.getFirstEntry();
        int j = ij.getSecondEntry();
        return varIdArray[nX - 1 - i][nY - 1 - j];
    }

    public MultiFactorJoint makeJoint(List<String> query, Map<String, Double> evidence) {
        //I return all X-factors and all XiXj factors and all observed XY factors //todo is this a right strategy?

        //1. X-factors:
        int numVars = nX * nY;
        // a list of factors associated with each var is assigned to it
//        Map<String, List<StochasticVAFactor>> varFactorsMap = new HashMap<String, List<StochasticVAFactor>>(varList.length);
        List<PiecewiseExpression<Fraction>> factors = new ArrayList<PiecewiseExpression<Fraction>>();

        for (int i = 0; i < numVars; i++) {
            String var = varName(i);

            //self factors:
            PiecewiseExpression<Fraction> selfFactor = selfFactor(var);
            if (selfFactor != null) {
                factors.add(selfFactor);
            }

            //neighbor XiXj factors:
            List<String> neighbours = fetchSouthEastNeighbors(var);
            for (String neighbour : neighbours) {
                factors.add(neighbourFactor(var, neighbour));
            }
        }

        //observed factors:
        for (String e : evidence.keySet()) {
            if (!e.startsWith("y_")) throw new RuntimeException();
            String x = "x_" + e.substring(2);  //x associated with the observed y
            factors.add(observedXYFactror(x, evidence.get(e)));
        }

        return new MultiFactorJoint(factors);

    }

    private PiecewiseExpression<Fraction> observedXYFactror(String x, Double value) {
//        System.err.println("TRIANGUKAR here TODO...");
//        return dBank.createUniformDistributionFraction(x, "" + (value - 0.3), "" + (value + 0.3));//todo TRIANGUKAR HERE
        return dBank.createExhaustiveTriangular(x, value.toString(), "2.0", "4.0").returnAdd(factory.makeFraction("1.0"));
    }

    @Override
    public List<String> allBinaryVars() {
        return Collections.emptyList();
    }
}

