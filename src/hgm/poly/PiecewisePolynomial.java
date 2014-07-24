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
    private List<ConstrainedPolynomial> cases;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PiecewisePolynomial that = (PiecewisePolynomial) o;

        if (isJointlyExhaustive != that.isJointlyExhaustive) return false;
        if (!cases.equals(that.cases)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (isJointlyExhaustive ? 1 : 0);
        result = 31 * result + cases.hashCode();
        return result;
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
            Collection<Polynomial> constraints = aCase.getConstraints();
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
     * @return adds the new constraints to all statements and multiplies all sub-functions in the new polynomial
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

    public PiecewisePolynomial add(PiecewisePolynomial other) {
        return addCrossProdOp.run(this, other);
    }

    SegmentCrossProdOperation addCrossProdOp = new SegmentCrossProdOperation() {
        @Override
        Polynomial operate(Polynomial poly1, Polynomial poly2) {
            Polynomial clonedPoly1 = poly1.clone();
            clonedPoly1.addToThis(poly2);
            return clonedPoly1;
        }
    };

    /**
     * this: e.g.  <br>
     *      A1.A2:  f1    <br>
     *      A1.A3:  f2   <br>
     *      B1:     f3    <br>
     *      C:      f4
     * @param other e.g.    <br>
     *              A1:     g1      <br>
     *              B1.B2:  g2      <br>
     *              D:      g3      <br>
     * @return
     *      A1.A2:  f1.g1   <br>
     *      A1.A3:  f2.g1   <br>
     *      B1.A1:  f3.g1   <br>
     *      B1.B2:  f3:g2
     *      B1.D:   f3.g3   <br>
     *      C.A1:   f4.g1   <br>
     *      C.D:    f4.g3   <br>
     *
     *     i.e. 7 cases instead of 12 ones.
     */
    public PiecewisePolynomial multiply(PiecewisePolynomial other) {
        return multCrossProdOp.run(this, other);
        }

    SegmentCrossProdOperation multCrossProdOp = new SegmentCrossProdOperation() {
        @Override
        Polynomial operate(Polynomial poly1, Polynomial poly2) {
            return poly1.multiply(poly2);
        }
    };

    abstract class SegmentCrossProdOperation{
        PiecewisePolynomial run(PiecewisePolynomial thisPP, PiecewisePolynomial otherPP){
            boolean prodIsJointlyExclusive = thisPP.isJointlyExhaustive && otherPP.isJointlyExhaustive;
            List<ConstrainedPolynomial> prodCases = new ArrayList<ConstrainedPolynomial>();

            //determining sub-regions...
            Map<CaseStatementConstraints, CaseStatementConstraints> subset2superSet = computeSub2SuperSetMap(thisPP, otherPP);

            for (ConstrainedPolynomial cp1 : thisPP.getCases()) {
                CaseStatementConstraints cn1 = cp1.getConstraints();

                CaseStatementConstraints superSetOfCn1 = subset2superSet.get(cn1);
                if (superSetOfCn1 != null) { //cn1 is a sub-set of a set...
                    //so only consider the super-set:
                    prodCases.add(new ConstrainedPolynomial(
                            operate(cp1.getPolynomial(), otherPP.getCorrespondingPolynomial(superSetOfCn1)), //cp1.getPolynomial().multiply(otherPP.getCorrespondingPolynomial(superSetOfCn1)),
                            cn1));
                } else {
                    for (ConstrainedPolynomial cp2: otherPP.getCases()) {
                        CaseStatementConstraints cn2 = cp2.getConstraints();
                        CaseStatementConstraints superSetOfCn2 = subset2superSet.get(cn2);
                        if (superSetOfCn2 == null) {
                            Set<Polynomial> bothConstraints = new HashSet<Polynomial>(cn1);
                            bothConstraints.addAll(cn2);
                            prodCases.add(new ConstrainedPolynomial(
                                    operate(cp1.getPolynomial(), cp2.getPolynomial()), //cp1.getPolynomial().multiply(cp2.getPolynomial()),
                                    bothConstraints));

                        } else if (superSetOfCn2.equals(cn1)) {
                            prodCases.add(new ConstrainedPolynomial(
                                    operate(cp1.getPolynomial(), cp2.getPolynomial()), //cp1.getPolynomial().multiply(cp2.getPolynomial()),
                                    cn2));
                        } //else do nothing since cn2 is a subset of another partition in the first piecewise function...
                    }
                }
            }

            return new PiecewisePolynomial(prodIsJointlyExclusive, prodCases);

        }

        abstract Polynomial operate(Polynomial poly1, Polynomial poly2);

        Map<CaseStatementConstraints, CaseStatementConstraints> computeSub2SuperSetMap(PiecewisePolynomial thisPP, PiecewisePolynomial otherPP) {
            Map<CaseStatementConstraints /*subset*/, CaseStatementConstraints /*super-set*/> subset2superSet = new HashMap<CaseStatementConstraints, CaseStatementConstraints>();
//        Set<CaseStatementConstraints> remainedCnsThis = new HashSet<CaseStatementConstraints>();
            for (ConstrainedPolynomial cp1 : thisPP.getCases()) {
                CaseStatementConstraints cn1 = cp1.getConstraints();
                for (ConstrainedPolynomial cp2: otherPP.getCases()) {
                    CaseStatementConstraints cn2 = cp2.getConstraints();
                    if (cn1.isEntailedBy(cn2)) {
                        if (subset2superSet.put(cn1, cn2) != null) { //subset in the sense that it is associated with a smaller partition which is a subset of the other one, i.e. (A1.A2) and (A1) are two subsets of (A1).
                            throw new RuntimeException("already a subset!");
                        }
                    } else if (cn2.isEntailedBy(cn1)) {
                        if (subset2superSet.put(cn2, cn1) != null) {
                            throw new RuntimeException("already a subset!");
                        }
                    }
                }
            }
            return subset2superSet;
        }
    }

    /*public PiecewisePolynomial multiply(PiecewisePolynomial other) {
        boolean prodIsJointlyExclusive = this.isJointlyExhaustive && other.isJointlyExhaustive;
        List<ConstrainedPolynomial> prodCases = new ArrayList<ConstrainedPolynomial>();

        //determining sub-regions...
        Map<CaseStatementConstraints, CaseStatementConstraints> subset2superSet = computeSub2SuperSetMap(other);

        for (ConstrainedPolynomial cp1 : this.getCases()) {
            CaseStatementConstraints cn1 = cp1.getConstraints();

            CaseStatementConstraints superSetOfCn1 = subset2superSet.get(cn1);
            if (superSetOfCn1 != null) { //cn1 is a sub-set of a set...
                //so only consider the super-set:
                prodCases.add(new ConstrainedPolynomial(cp1.getPolynomial().multiply(other.getCorrespondingPolynomial(superSetOfCn1)), cn1));
            } else {
                for (ConstrainedPolynomial cp2: other.getCases()) {
                    CaseStatementConstraints cn2 = cp2.getConstraints();
                    CaseStatementConstraints superSetOfCn2 = subset2superSet.get(cn2);
                    if (superSetOfCn2 == null) {
                        Set<Polynomial> bothConstraints = new HashSet<Polynomial>(cn1);
                        bothConstraints.addAll(cn2);
                        prodCases.add(new ConstrainedPolynomial(cp1.getPolynomial().multiply(cp2.getPolynomial()), bothConstraints));

                    } else if (superSetOfCn2.equals(cn1)) {
                        prodCases.add(new ConstrainedPolynomial(cp1.getPolynomial().multiply(cp2.getPolynomial()), cn2));
                    } //else do nothing since cn2 is a subset of another partition in the first piecewise function...
                }
            }
        }

        return new PiecewisePolynomial(prodIsJointlyExclusive, prodCases);
    }*/



    private Polynomial getCorrespondingPolynomial(CaseStatementConstraints caseConstraints) {
        for (ConstrainedPolynomial cp : cases) {
            if (cp.getConstraints().equals(caseConstraints)) {
                return cp.getPolynomial();
            }
        }
        throw new RuntimeException("not found");
    }

    public Set<String> getScopeVars() {
        Set<String> scopeVars = new HashSet<String>();
        for (ConstrainedPolynomial constrainedPolynomial : cases) {
            scopeVars.addAll(constrainedPolynomial.getScopeVars());
        }
        return scopeVars;
    }

    public PiecewisePolynomial substitute(Map<String, Double> assign) {
        List<ConstrainedPolynomial> newCases = new ArrayList<ConstrainedPolynomial>(this.numCases());
        for (ConstrainedPolynomial c : cases) {
            newCases.add(c.substitute(assign));
        }
        return new PiecewisePolynomial(this.isJointlyExhaustive, newCases);
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

