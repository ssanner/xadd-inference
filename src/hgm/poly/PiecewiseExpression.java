package hgm.poly;

import hgm.poly.pref.FatalSamplingException;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 26/02/14
 * Time: 8:07 PM
 */
public class PiecewiseExpression<E extends Expression> {
    /**
     * It is assumed that (the constraints of) different cases are mutually exclusive and jointly exhaustive.
     * However, if this parameter is unset, it is assumed that a case exists with constraint = 'negation of constraints of other cases' and value = 0.
     */
    boolean isJointlyExhaustive = true;
    private List<ConstrainedExpression<E>> cases;

    public PiecewiseExpression(ConstrainedExpression<E>... cases) {
        this(true, cases);
    }

    //note: all cases should share a same factory
    public PiecewiseExpression(boolean isJointlyExhaustive, ConstrainedExpression<E>... cases) {
        this(isJointlyExhaustive, Arrays.asList(cases));
    }

    private PiecewiseExpression(boolean isJointlyExhaustive, List<ConstrainedExpression<E>> cases) {
        this.cases = new ArrayList<ConstrainedExpression<E>>(cases);
        this.isJointlyExhaustive = isJointlyExhaustive;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PiecewiseExpression that = (PiecewiseExpression) o;

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

    public List<ConstrainedExpression<E>> getCases() {
        return cases;
    }

    public double evaluate(Double[] assign) {
        ConstrainedExpression activeCase;
        try {
            Integer activatedCaseId = getActivatedCaseId(assign);
            if (activatedCaseId==-1) return 0.0;

            activeCase = cases.get(activatedCaseId);
            return activeCase.getFruit().evaluate(assign);

        } catch (Exception e) {
            e.printStackTrace();
            return 0.0; //this happens if there is a bug (exhaustiveness is necessary but does not hold!)
        }

    }

    public Integer getActivatedCaseId(Double[] assignment) throws FatalSamplingException {
        for (int caseId = 0; caseId < cases.size(); caseId++) {
            ConstrainedExpression aCase = cases.get(caseId);
            Collection<Expression> constraints = aCase.getConstraints();
            boolean allConstraintsOfThisCaseHold = true;
            for (Expression constraint : constraints) {
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
    public PiecewiseExpression multiply(List<? extends Expression> newConstraints, Expression newPolynomial){
        ConstrainedExpression[] augmentedCases = new ConstrainedExpression[cases.size()];
        for (int i = 0; i < cases.size(); i++) {
            ConstrainedExpression aCase = cases.get(i);
            List<Expression> augmentedConstraints = new ArrayList<Expression>(aCase.getConstraints().size() + newConstraints.size());
            augmentedConstraints.addAll(aCase.getConstraints());
            augmentedConstraints.addAll(newConstraints);

            ConstrainedExpression augmentedCase = new ConstrainedExpression(aCase.getFruit().returnMultiplication(newPolynomial), augmentedConstraints);
            augmentedCases[i] = augmentedCase;
        }

        return new PiecewiseExpression(false, augmentedCases);
    }

    public PiecewiseExpression add(PiecewiseExpression other) {
        if (!this.isJointlyExhaustive || !other.isJointlyExhaustive) throw new RuntimeException("ADDITION is not implemented for nonExhaustive piecewise expressions");
        return addCrossProdOp.run(this, other);
    }

    SegmentCrossProdOperation addCrossProdOp = new SegmentCrossProdOperation() {
        @Override
        Expression operate(Expression poly1, Expression poly2) {
            return poly1.returnAddition(poly2);
//            Polynomial clonedPoly1 = poly1.clone();
//            clonedPoly1.addToThis(poly2);
//            return clonedPoly1;
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
    public PiecewiseExpression multiply(PiecewiseExpression other) {
        return multCrossProdOp.run(this, other);
        }

    SegmentCrossProdOperation multCrossProdOp = new SegmentCrossProdOperation() {
        @Override
        Expression operate(Expression poly1, Expression poly2) {
            return poly1.returnMultiplication(poly2);
        }
    };

    abstract class SegmentCrossProdOperation{
        PiecewiseExpression run(PiecewiseExpression<E> thisPP, PiecewiseExpression<E> otherPP){
            boolean prodIsJointlyExclusive = thisPP.isJointlyExhaustive && otherPP.isJointlyExhaustive;
            List<ConstrainedExpression> prodCases = new ArrayList<ConstrainedExpression>();

            //determining sub-regions...
            Map<CaseStatementConstraints, CaseStatementConstraints> subset2superSet = computeSub2SuperSetMap(thisPP, otherPP);

            for (ConstrainedExpression<E> cp1 : thisPP.getCases()) {
                CaseStatementConstraints cn1 = cp1.getConstraints();

                CaseStatementConstraints superSetOfCn1 = subset2superSet.get(cn1);
                if (superSetOfCn1 != null) { //cn1 is a sub-set of a set...
                    //so only consider the super-set:
                    prodCases.add(new ConstrainedExpression(
                            operate(cp1.getFruit(), otherPP.getCorrespondingPolynomial(superSetOfCn1)), //cp1.getPolynomial().multiply(otherPP.getCorrespondingPolynomial(superSetOfCn1)),
                            cn1));
                } else {
                    for (ConstrainedExpression<E> cp2: otherPP.getCases()) {
                        CaseStatementConstraints cn2 = cp2.getConstraints();
                        CaseStatementConstraints superSetOfCn2 = subset2superSet.get(cn2);
                        if (superSetOfCn2 == null) {
                            Set<Expression> bothConstraints = new HashSet<Expression>(cn1);
                            bothConstraints.addAll(cn2);
                            prodCases.add(new ConstrainedExpression(
                                    operate(cp1.getFruit(), cp2.getFruit()), //cp1.getPolynomial().multiply(cp2.getPolynomial()),
                                    bothConstraints));

                        } else if (superSetOfCn2.equals(cn1)) {
                            prodCases.add(new ConstrainedExpression(
                                    operate(cp1.getFruit(), cp2.getFruit()), //cp1.getPolynomial().multiply(cp2.getPolynomial()),
                                    cn2));
                        } //else do nothing since cn2 is a subset of another partition in the first piecewise function...
                    }
                }
            }

            return new PiecewiseExpression(prodIsJointlyExclusive, prodCases);

        }

        abstract Expression operate(Expression poly1, Expression poly2);

        Map<CaseStatementConstraints, CaseStatementConstraints> computeSub2SuperSetMap(PiecewiseExpression<E> thisPP, PiecewiseExpression<E> otherPP) {
            Map<CaseStatementConstraints /*subset*/, CaseStatementConstraints /*super-set*/> subset2superSet = new HashMap<CaseStatementConstraints, CaseStatementConstraints>();
//        Set<CaseStatementConstraints> remainedCnsThis = new HashSet<CaseStatementConstraints>();
            for (ConstrainedExpression cp1 : thisPP.getCases()) {
                CaseStatementConstraints cn1 = cp1.getConstraints();
                for (ConstrainedExpression cp2: otherPP.getCases()) {
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



    private Expression getCorrespondingPolynomial(CaseStatementConstraints caseConstraints) {
        for (ConstrainedExpression cp : cases) {
            if (cp.getConstraints().equals(caseConstraints)) {
                return cp.getFruit();
            }
        }
        throw new RuntimeException("not found");
    }

    public Set<String> getScopeVars() {
        Set<String> scopeVars = new HashSet<String>();
        for (ConstrainedExpression constrainedExpression : cases) {
            scopeVars.addAll(constrainedExpression.getScopeVars());
        }
        return scopeVars;
    }

    public PiecewiseExpression<E> substitute(Map<String, Double> assign) {
        List<ConstrainedExpression<E>> newCases = new ArrayList<ConstrainedExpression<E>>(this.numCases());
        for (ConstrainedExpression<E> c : cases) {
            newCases.add(c.substitute(assign));
        }
        return new PiecewiseExpression<E>(this.isJointlyExhaustive, newCases);
    }

    public PiecewiseExpression<E> substitute(String var, Expression value) {

        List<ConstrainedExpression> newCases = new ArrayList<ConstrainedExpression>(this.numCases());
        for (ConstrainedExpression c : cases) {
            newCases.add(c.substitute(var, value));
        }
        return new PiecewiseExpression(this.isJointlyExhaustive, newCases);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (ConstrainedExpression aCase : cases) {
            sb.append("\t").append(aCase).append("\n");
        }
        if (!isJointlyExhaustive) {
            sb.append("0 \t\t\t OTHERWISE:\n");
        }

        return sb.toString();
    }
}

