package hgm.poly;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 26/02/14
 * Time: 8:07 PM
 */
public class PiecewisePolynomial {
    /**
     * It is assumed that (the constraints of) different cases are mutually exclusive and jointly exhaustive:
     */
    private List<ConstrainedPolynomial> cases;

    public PiecewisePolynomial(List<ConstrainedPolynomial> cases) {
        this.cases = cases;
    }

    public Integer numCases() {
        return cases.size();
    }

    public List<ConstrainedPolynomial> getCases() {
        return cases;
    }

    public double evaluate(Double[] assign) {
        ConstrainedPolynomial activeCase;
        try{
            activeCase = cases.get(getActivatedCaseId(assign));
        }catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
        return activeCase.getPolynomial().evaluate(assign);

        /*
        for (ConstrainedPolynomial aCase : cases){
            List<Polynomial> constraints = aCase.getConstraints();
            boolean allConstraintsOfThisCaseHold = true;
            for (Polynomial constraint : constraints) {
                if (constraint.evaluate(assign) <=0) {
                    allConstraintsOfThisCaseHold = false;
                    break; //another case should be tested...
                }
            }
            if (allConstraintsOfThisCaseHold) {
                return aCase.getPolynomial().evaluate(assign);
            }
        }

        throw new RuntimeException("no case holds!");*/
    }

    public Integer getActivatedCaseId(Double[] assignment) {
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

        throw new RuntimeException("For assignment: " + Arrays.toString(assignment) + " no case holds!\n" + this.toString());

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (ConstrainedPolynomial aCase : cases) {
            sb.append("\t").append(aCase).append("\n");
        }
        return sb.toString();
    }
}

/*

    */
/**
     * A single entry map to set the value of the var for the purpose of expression evaluation
     *//*

    protected Map<List<Polynomial> */
/*constraints*//*
, Polynomial> constraints2valuesMap = new HashMap<List<Polynomial>, Polynomial>();

    // the lists should be mutually exclusive otherwise things might not work well...
    public void put(List<Polynomial> constrains, Polynomial value) {
        constraints2valuesMap.put(constrains, value);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<List<Polynomial>, Polynomial> aCase : constraints2valuesMap.entrySet()) {
            sb.append("\t").append(aCase).append("\n");
        }
        return sb.toString();
    }

    public double evaluate(Double[] assign) {
        for (Map.Entry<List<Polynomial>, Polynomial> aCase : constraints2valuesMap.entrySet()) {

            boolean allConstraintsSatisfied = true;
            for (Polynomial constraint : aCase.getKey()) {
                if (constraint.evaluate(assign) <= 0) {
                    allConstraintsSatisfied = false;
                    break;
                }
            }

            if (allConstraintsSatisfied) {
                return aCase.getValue().evaluate(assign);
            }

        }

        return 0d; //if no case sentence is satisfied then 0 is returned by default
    }

}
*/
