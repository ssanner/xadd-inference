package hgm.poly;

import hgm.poly.pref.FatalSamplingException;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 26/02/14
 * Time: 8:07 PM
 */
public class PiecewisePolynomial {
    /**
     * It is assumed that (the constraints of) different cases are mutually exclusive and jointly exhaustive.
     * However, if this parameter is unset, it is assumed that a case exists with constraint = 'negation of constraints of other cases' and value = 0.
     */
    boolean isJointlyExhaustive = true;
    private ArrayList<ConstrainedPolynomial> cases;

    public PiecewisePolynomial(ConstrainedPolynomial... cases) {
        this(true, cases);
    }

    //note: all cases should share a same factory
    public PiecewisePolynomial(boolean isJointlyExhaustive, ConstrainedPolynomial... cases) {
        this(isJointlyExhaustive, Arrays.asList(cases));
    }

    private PiecewisePolynomial(boolean isJointlyExhaustive, List<ConstrainedPolynomial> cases) {
        this.cases = new ArrayList<ConstrainedPolynomial>(cases);
        this.isJointlyExhaustive = isJointlyExhaustive;
    }

    /*@Override
    @SuppressWarnings("CloneDoesntCallSuperClone, CloneDoesntDeclareCloneNotSupportedException")
    public PiecewisePolynomial clone() {
    WRONG SINCE CLONING IS NOT DEEP....
        return new PiecewisePolynomial(this.isJointlyExhaustive, this.cases);
    }
*/
    public PolynomialFactory getFactory() {
        return cases.get(0).getPolynomialFactory();
    }

    public Integer numCases() {
        return cases.size(); //todo for non-exhaustive case, it does not make a bug???
    }

    public List<ConstrainedPolynomial> getCases() {
        return cases;
    }

    public double evaluate(Double[] assign) {
        ConstrainedPolynomial activeCase;
        try {
            Integer activatedCaseId = getActivatedCaseId(assign);
            if (activatedCaseId==-1) return 0.0;

            activeCase = cases.get(activatedCaseId);
            return activeCase.getPolynomial().evaluate(assign);

        } catch (Exception e) {
            e.printStackTrace();
            return 0.0; //this happens if there is a bug (exhaustiveness is necessary but does not hold!)
        }

    }

    public Integer getActivatedCaseId(Double[] assignment) throws FatalSamplingException {
        for (int caseId = 0; caseId < cases.size(); caseId++) {
            ConstrainedPolynomial aCase = cases.get(caseId);
            List<Polynomial> constraints = aCase.getConstraints();
            boolean allConstraintsOfThisCaseHold = true;
            for (Polynomial constraint : constraints) {
                if (constraint.evaluate(assignment) <= 0) {
                    allConstraintsOfThisCaseHold = false;
                    break; //another case should be tested...
                }
            }
            if (allConstraintsOfThisCaseHold) {
                return caseId;
            }
        }

        if (isJointlyExhaustive) {
            throw new FatalSamplingException("For assignment: " + Arrays.toString(assignment) + " no case (in a jointly exhaustive piecewise polynomial) holds!\n" + this.toString());
        } else return -1; //in a none exhaustive piecewise polynomial no case Id holds...

    }

//    is not tested and can be wrong.... do to deep cloning issues
//    public void multiplyInThis(ConstrainedPolynomial constrainedPolynomial){
//        List<Polynomial> newConstraints = constrainedPolynomial.getConstraints();
//        Polynomial newPolynomial = constrainedPolynomial.getPolynomial();
//        multiplyInThis(newConstraints, newPolynomial);
//    }

    /**
     * @return adds the new constraints to all statements and multiplies all sub-functions is the new polynomial
     */
    public PiecewisePolynomial multiply(List<Polynomial> newConstraints, Polynomial newPolynomial){
        ConstrainedPolynomial[] augmentedCases = new ConstrainedPolynomial[cases.size()];
        for (int i = 0; i < cases.size(); i++) {
            ConstrainedPolynomial aCase = cases.get(i);
            List<Polynomial> augmentedConstraints = new ArrayList<Polynomial>(aCase.getConstraints().size() + newConstraints.size());
            augmentedConstraints.addAll(aCase.getConstraints());
            augmentedConstraints.addAll(newConstraints);

            ConstrainedPolynomial augmentedCase = new ConstrainedPolynomial(aCase.getPolynomial().multiply(newPolynomial), augmentedConstraints);
            augmentedCases[i] = augmentedCase;
        }

        return new PiecewisePolynomial(false, augmentedCases);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (ConstrainedPolynomial aCase : cases) {
            sb.append("\t").append(aCase).append("\n");
        }
        if (!isJointlyExhaustive) {
            sb.append("0 \t\t\t OTHERWISE:\n");
        }

        return sb.toString();
    }
}

