package hgm.asve.cnsrv.approxator.fitting;

import hgm.asve.cnsrv.factor.IFactor;
import hgm.asve.cnsrv.factory.ElementaryFactorFactory;
import org.apache.commons.math3.linear.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by Hadi Afshar.
 * Date: 20/10/13
 * Time: 6:20 PM
 */
public class CurveFitting<F extends IFactor> {
    ElementaryFactorFactory<F> factory;

    public CurveFitting(ElementaryFactorFactory<F> factory) {
        this.factory = factory;
    }

    //todo: list of map for var assignments is expensive. Instead use a list<double[]> plus a List<String> for variable names
    public double[] solveWeights(List<F> basisFunctions,
                                 List<Map<String, Double>> variableAssignments,
                                 List<Double> targetVariablesForEachAssignment,
                                 double regularizationCoefficient
                                 //the last parameter seems useless since lambda compensates for it:
                                 /*, boolean constraintTheSizeOfBasisFunctionsToTheSizeOfSamplesMinusOne*/) {
        int n = targetVariablesForEachAssignment.size();
        if (variableAssignments.size() != n)
            throw new RuntimeException("size mismatch between variable assingments and targets");
        RealMatrix t = new Array2DRowRealMatrix(n, 1);
        for (int i = 0; i < n; i++) {
            t.setEntry(i, 0, targetVariablesForEachAssignment.get(i));
        }

//        System.out.println("n = " + n);
//        System.out.println("# basisFunctions = " + basisFunctions.size());
/*

        //NOTE: if n i.e. the number of samples is less than number of basis functions, only a subst of basis functions is chosen to prevent zero determinant:
        if (constraintTheSizeOfBasisFunctionsToTheSizeOfSamplesMinusOne && basisFunctions.size() >= n) {
            basisFunctions = basisFunctions.subList(0, n-1);
        }
        System.out.println("# basisFunctions = " + basisFunctions.size());
*/

//        System.out.println("variableAssignments = " + variableAssignments.size());
        RealMatrix designMatrix = designMatrix(basisFunctions, variableAssignments);
//        System.out.println("designMatrix dim:" + designMatrix.getRowDimension() + "x" + designMatrix.getColumnDimension());
        RealMatrix pseudoInverse = quadraticRegularizedMoorePenrosePseudoInverse(designMatrix, regularizationCoefficient);
        RealMatrix weightsMatrix = pseudoInverse.multiply(t);

        assert (weightsMatrix.getColumnDimension() == 1);
        int m = weightsMatrix.getRowDimension();
        if (basisFunctions.size() != m) throw new RuntimeException("unexpected result");

//        printMatrix("weightsMatrix", weightsMatrix);

        return weightsMatrix.getColumn(0);
    }


    /**
     * @param phi design matrix &phi;
     * @param regularizationCoefficient Regularization coefficient &lambda;
     * @return <p>(&lambda; . <b>I</b> + &phi;<sup>T</sup> . &phi;)<sup>-1</sup> . &phi;<sup>T</sup>
     */
    protected RealMatrix quadraticRegularizedMoorePenrosePseudoInverse(RealMatrix phi, double regularizationCoefficient) {
        RealMatrix phiT = phi.transpose();
        RealMatrix phiTphi = phiT.multiply(phi);

        RealMatrix identityMatrix = MatrixUtils.createRealIdentityMatrix(phiTphi.getColumnDimension());
        RealMatrix lambdaI = identityMatrix.scalarMultiply(regularizationCoefficient);
//        printMatrix("lambda I", lambdaI);


        LUDecomposition luDecomposition = new LUDecomposition(lambdaI.add(phiTphi));
        double determinant = luDecomposition.getDeterminant();
        if (determinant == 0) {
            System.out.println("luDecomposition.getDeterminant() = " + determinant);
            System.out.println("phiTphi = " + phiTphi);
            printMatrix("phiTphi", phiTphi);
        }

        DecompositionSolver solver = luDecomposition.getSolver();
        RealMatrix inverse = solver.getInverse();
        return inverse.multiply(phiT); //(lambda . I + phi^T . phi)^{-1} . phi^T
    }

    //todo move to a util class
    public static void printMatrix(String txt, RealMatrix m) {
        System.out.println("MATRIX " + txt + " = ");
        for (int i=0; i<m.getRowDimension(); i++) {
            double[] row = m.getRow(i);
            System.out.print("|");
            for (int j=0; j<row.length; j++) {
                System.out.format("%15f ", row[j]);
            }
            System.out.println("|");
        }
    }


    //see p. 142 Bishop
    protected RealMatrix designMatrix(List<F> basisFunctions, List<Map<String, Double>> variableAssignments) {
        int m = basisFunctions.size();
        int n = variableAssignments.size();

        RealMatrix designMatrix = new Array2DRowRealMatrix(n, m);

        for (int i = 0; i < m; i++) {
            F basisFunction = basisFunctions.get(i);
            for (int j = 0; j < n; j++) {
                Map<String, Double> varAssign = variableAssignments.get(j);
                double evalBasisFunc = factory.evaluate(basisFunction, varAssign);
                designMatrix.setEntry(j, i, evalBasisFunc);
            }
        }
        return designMatrix;
    }

    /**
     * @param maxPower  maximum number of variables multiplied in each factor
     * @param variables variables i.e. x_1, x_2, ... , x_n
     * @return polynomial basis functions: e.g. 1, x_1, x_2, x_n, x_1*x_1, x1_x_2, ..., x_1*x_n, x_2*x_2, x_2*x_3, ..., x_2*x_n, ... , x_n*x*n, x_1*x_1*x_1, ...
     */
    public List<F> calculateBasisFunctions(int maxPower, List<String> variables) {
        if (maxPower < 0) return null;
        List<F> factors = new ArrayList<F>();
        factors.add(factory.one()); // \phi_0 = 1

        if (maxPower == 0) return factors;

//        int counter = 1;

        int loopNum = 1;
        int d = variables.size();

        for (; ; ) {
            List<int[]> varIndexesList = nestedLoops(loopNum++, d);
            for (int[] varIndexes : varIndexesList) {
                F factor = makeFactorFromVarIndices(varIndexes, variables);
//                if (++counter == numberOfBasisFunctions) return factors;
                factors.add(factor);
            }
            if (loopNum > maxPower) return factors;
        }
    }

    private F makeFactorFromVarIndices(int[] varIndexes, List<String> allVariables) {
        String[] chosenVars = new String[varIndexes.length];
        for (int i = 0; i < varIndexes.length; i++) {
            chosenVars[i] = allVariables.get(varIndexes[i]);
        }
        return factory.getFactorForMultiplicationOfVars(chosenVars);
    }

    protected static List<int[]> nestedLoops(int loopNum, int d) {
        List<int[]> out = new ArrayList<int[]>();

        int[] o = new int[loopNum];
        out.add(o.clone());

        for (; ; ) {
            int i = loopNum - 1;

            o[i]++;

            if (o[i] < d) {
                out.add(o.clone());
                continue;
            }

            //overflow:
            do {
                i--;
                if (i < 0) return out;
            } while ((++o[i]) >= d);
            for (int j = i; j < loopNum; j++) {
                o[j] = o[i];
            }
            out.add(o.clone());
        }
    }

    protected static String toStringNestedLoop(List<int[]> nl) {
        StringBuilder sb = new StringBuilder("[");
        for (int[] n : nl) {
            sb.append(Arrays.toString(n)).append(", ");
        }
        return (sb.substring(0, sb.length() - 2)) + "]";

    }
}
