package hgm.poly.integral.frac;

import hgm.poly.*;
import hgm.poly.integral.OneDimFunction;
import hgm.poly.integral.SymbolicOneDimFunctionGenerator;
import hgm.poly.pref.SymbolicCdfArrayHandler;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 1/08/14
 * Time: 2:10 AM
 */
public class Digester {
    private Set<IntermediateConstraintExpression> inters = new HashSet<IntermediateConstraintExpression>();
    private int varIndex;

    private List<PiecewiseExpression<Fraction>> factorsNotInvolvingIntegrand;

    public boolean normalizationRequired = false;


    public Digester(
//            PiecewiseExpression<Fraction> pf,
            FactorizedPiecewiseStructure<Fraction> pf,
            String var) {
        this(pf, pf.getFactory().getVarIndex(var));
    }

    public Digester(
//            PiecewiseExpression<Fraction> pf,
            FactorizedPiecewiseStructure<Fraction> fpf,
            int varIndex) {
        this.varIndex = varIndex;
        String var = fpf.getFactory().getAllVars()[varIndex]; //stupid move between varIndex and var names

        //0.1 keep the factors not involving var:
        if (normalizationRequired) {
            factorsNotInvolvingIntegrand = fpf.getFactorsNotInvolving(var);
        } else {
            factorsNotInvolvingIntegrand = new ArrayList<PiecewiseExpression<Fraction>>();//if normalization is not required, then I do not need to take care of other factors so pass an empty list
        }

        //0.2 make a single piecewise expression for the factors involving the var:
        List<PiecewiseExpression<Fraction>> factorsWithVar = fpf.getFactorsInvolving(var);
        PiecewiseExpression<Fraction> pf;
        if (factorsWithVar.size()==0){
            PolynomialFactory factory = fpf.getFactory();
            ConstrainedExpression<Fraction> case1 = new ConstrainedExpression<Fraction>(factory.makeFraction("1.0"), new ArrayList<Fraction>());
            pf = new PiecewiseExpression<Fraction>(true, case1); //one
        } else if (factorsWithVar.size()==1){
            pf = factorsWithVar.get(0);
        } else {
            pf = factorsWithVar.get(0);
//            System.out.println("\nfactorsWithVar " + var +" .size() = " + factorsWithVar.size());
//            System.out.println("pf.numCases() = " + pf.numCases());
            for (int i=1; i<factorsWithVar.size(); i++) {
                pf = pf.multiply(factorsWithVar.get(i));
//                System.out.println("pf.numCases() = " + pf.numCases());
            }
        }

        //1.
        for (ConstrainedExpression<Fraction> fractionConstrainedExpression : pf.getCases()) {
            inters.add(new IntermediateConstraintExpression(fractionConstrainedExpression));
        }

        //2.
        IntermediateConstraintExpression currentInter;
        for (; ; ) {
            currentInter = chooseAnInterWithUndigestedFraction(inters);
            if (currentInter == null) break; //nothing left
            digestFraction(currentInter, inters);
        }

        //3.
        for (; ; ) {
            currentInter = chooseAnInterWithUnsolvedPolynomial(inters, varIndex);
            if (currentInter == null) break; //nothing left
            solvePolynomial(currentInter, inters, varIndex);
        }

        //4.
        simplify(inters);

        //5.
        Map<Fraction, Expression> fruitsToTheirIndefIntegrals = new HashMap<Fraction, Expression>(); //since many fruits are repeated, we cache integrals.
        for (IntermediateConstraintExpression inter : inters) {
            Fraction fruit = inter.fruit;
            WeakExpression indefIntegral = fruitsToTheirIndefIntegrals.get(fruit);
            if (indefIntegral == null) {
                indefIntegral = fruit.calcIndefIntegral(varIndex);
            }
            inter.fruitIndefIntegral = indefIntegral;
        }
    }

    public SymbolicCdfArrayHandler calcSymbolicIntegral(){
        SymbolicOneDimFunctionGenerator[] varCdfGenerators = new SymbolicOneDimFunctionGenerator[inters.size()];
        int i=0;
        for (IntermediateConstraintExpression inter : inters) {
             varCdfGenerators[i++] = inter.integrate(varIndex);
        }
        return new SymbolicCdfArrayHandler(varCdfGenerators, factorsNotInvolvingIntegrand);
    }

    private void simplify(Set<IntermediateConstraintExpression> inters) {
        for (Iterator<IntermediateConstraintExpression> interIterator = inters.iterator(); interIterator.hasNext(); ) {
            IntermediateConstraintExpression inter = interIterator.next();
            for (Iterator<Polynomial> constraintIterator = inter.independentConstraints.iterator(); constraintIterator.hasNext(); ) {
                Polynomial c = constraintIterator.next();
                if (c.isNumber()) {
                    double cNum = c.getNumericalValue();
                    if (cNum == 0) throw new RuntimeException("where has 0 come from?");
                    if (cNum > 0) {
                        constraintIterator.remove(); //tautology
                    }
                    if (cNum < 0) {
                        interIterator.remove(); //this constraint is unsatisfiable
                        break;//this line added 27/10/2015
                    }
                }


            }

        }
    }

    public Set<IntermediateConstraintExpression> getInters() {
        return inters;
    }


    private void solvePolynomial(IntermediateConstraintExpression c1 /*current inter; it is a member of inters and not completely solved w.r.t. var yet*/,
                                 Set<IntermediateConstraintExpression> inters, int varIndex) {
        Polynomial unsolvedPolyConstraint = c1.unsolvedPolynomials.iterator().next();

        c1.unsolvedPolynomials.remove(unsolvedPolyConstraint);


        int varDegree = unsolvedPolyConstraint.degree(varIndex);
        Polynomial[] sortedWrtVar;

        if (varDegree == 0) {
            c1.independentConstraints.add(unsolvedPolyConstraint);
            return;
        }

        if (varDegree == 1) {
            //ax + b >0
            sortedWrtVar = unsolvedPolyConstraint.sortWithRespectTo(varIndex);
            if (sortedWrtVar.length != 2) throw new RuntimeException("how can it be possible!");
            final Polynomial bPoly = sortedWrtVar[0];
            Polynomial aPoly = sortedWrtVar[1];
            if (aPoly.isNumber()) { //todo if cases has to be simplified, separate treatment of numerical A is unnecessary
                double a = aPoly.getNumericalValue();
                //making -b/a out of b:
                bPoly.multiplyScalarInThis(-1.0 / a);
                //now bPoly is -b/a.
                if (a > 0) { //x > -b/a
                    c1.lowerBounds.add(bPoly);
                } else { //x < -b/a
                    c1.upperBounds.add(bPoly);
                }
            } else {
                //a is symbolic

                // (c1) | (A*x+B)>0, W>0 :   Y           | A>0, x>(-B/A), W>0:  Y         (c1')
                //      |  .                   ==>       |-A>0, x<(-B/A), W>0:  Y         (c2)
                //      |  .                             |  .
                //      |  .
                IntermediateConstraintExpression c2 = c1.clone(); //fork
                inters.add(c2);

                c1.independentConstraints.add(aPoly); // a>0
                c2.independentConstraints.add(aPoly.scalarMultiplication(-1)); //-a >0

                Fraction z = new Fraction(bPoly.scalarMultiplication(-1), aPoly); //-b/a
                c1.lowerBounds.add(z); //x > -b/a
                c2.upperBounds.add(z); //x < -b/a
            }
            return;
        }

        if (varDegree == 2) {
            //a*x^2 + b*x + c >0
            sortedWrtVar = unsolvedPolyConstraint.sortWithRespectTo(varIndex);
            if (sortedWrtVar.length != 3) throw new RuntimeException("how can it be possible!");
            final Polynomial cPoly = sortedWrtVar[0];
            final Polynomial bPoly = sortedWrtVar[1];
            final Polynomial aPoly = sortedWrtVar[2];

            //why should I distinguish between symbolic and numerical d when cases HAVE TO be simplified anyway.
            final Polynomial delta = bPoly.returnMultiplication(bPoly).returnSubtraction(aPoly.returnMultiplication(cPoly).scalarMultiplication(4)); //b^2 - 4*a*c

            WeakExpression root1 = new WeakExpression() {
                @Override
                public double evaluate(Double[] fullVarAssign) {
                    //[-b + delta^(0.5)]/[2a]
                    return ((-bPoly.evaluate(fullVarAssign)) + Math.sqrt(delta.evaluate(fullVarAssign))) / (2 * aPoly.evaluate(fullVarAssign));
                }

                @Override
                public String toString() {
                    return "-1 * " + bPoly + " + SQRT{" + delta + "} / [2 * " + aPoly + "]";
                }
            };

            WeakExpression root2 = new WeakExpression() {
                @Override
                public double evaluate(Double[] fullVarAssign) {
                    //[-b - delta^(0.5)]/[2a]
                    return ((-bPoly.evaluate(fullVarAssign)) - Math.sqrt(delta.evaluate(fullVarAssign))) / (2 * aPoly.evaluate(fullVarAssign));
                }

                @Override
                public String toString() {
                    return "-1 * " + bPoly + " - SQRT{" + delta + "} / [2 * " + aPoly + "]";
                }
            };

            // (c1) | (A*x^2 + B*x + C)>0, W>0 :   Y               | A>0, x > r_1, W>0:  Y              (c1)
            //      |  .                                 ==>       | A>0, x < r_2, W>0:  Y              (c2)
            //      |  .                                           | A<0, r_1 < x, x < r_2, W>0:  Y     (c3)
            //      |  .                                           | .
            // where r_1 := (-b + delta^(0.5))/(2a) and r_2 := (-b - delta^(0.5))/(2a)


            IntermediateConstraintExpression c3 = c1.clone();
            c3.independentConstraints.add(aPoly.scalarMultiplication(-1)); //A<0
            c3.lowerBounds.add(root1); //r_1 < x
            c3.upperBounds.add(root2); //x < r_2

            c1.independentConstraints.add(aPoly); // A>0
            IntermediateConstraintExpression c2 = c1.clone();
            c1.lowerBounds.add(root1); //r_1 < x
            c2.upperBounds.add(root2); //x < r_2

            inters.add(c2);
            inters.add(c3);

            return;
        }

        throw new RuntimeException("cannot handle polynomial " + unsolvedPolyConstraint + " with rel.degree " + varDegree);

    }

    private void digestFraction(IntermediateConstraintExpression c1 /*current constraint expression; it is a member of inters and is not completely digested yet*/,
                                Set<IntermediateConstraintExpression> inters) {
        Fraction aConstraintFraction = c1.unDigestedFractionConstraints.iterator().next();

        c1.unDigestedFractionConstraints.remove(aConstraintFraction); // no (F/G)>0

        Polynomial f = aConstraintFraction.getNumerator();    //F
        Polynomial g = aConstraintFraction.getDenominator();   //G
        if (g.isNumber()) {
            double d = g.getNumericalValue();
            if (d > 0) {   //F > 0 should be added
                if (c1.unsolvedPolynomials.contains(f.scalarMultiplication(-1)))  {
                    //inconsistency! F<0 is already a constraint
                    inters.remove(c1);
                    return;
                }
                c1.unsolvedPolynomials.add(f);   // F > 0
            } else if (d < 0) { //F<0 should be added:
                if (c1.unsolvedPolynomials.contains(f)) {
                    //inconsistency! F>0 is already a constraint
                    inters.remove(c1);
                    return;
                }
                c1.unsolvedPolynomials.add(f.scalarMultiplication(-1)); // F < 0
            } else {
                throw new RuntimeException("What should I do with: " + aConstraintFraction);
            }
            return;
        }

        //current constraint fraction has a symbolic denominator:
        // (c1) | (F/G)>0, X>0 :   Y           | G>0, F>0, X>0:  Y         (c1')
        //      |  .                   ==>     |-G>0,-F>0, X>0:  Y         (c2)
        //      |  .                           |  .
        //      |  .                           |  .

        //NEW: Added 6/01/2015. some simple checks to reduce invalid cases:
        if (c1.unsolvedPolynomials.contains(g)) {
            c1.unsolvedPolynomials.add(f);  //since G>0 is an existing condition, F has to be >0 also.
            return;
        }
        if (c1.unsolvedPolynomials.contains(f)) {
            c1.unsolvedPolynomials.add(g);  //since F>0 is an existing condition, G has to be >0 also.
            return;
        }
        if (c1.unsolvedPolynomials.contains(g.scalarMultiplication(-1))) {
            c1.unsolvedPolynomials.add(f.scalarMultiplication(-1)); //-F>0
            return;
        }
        if (c1.unsolvedPolynomials.contains(f.scalarMultiplication(-1))) {
            c1.unsolvedPolynomials.add(g.scalarMultiplication(-1)); //-G>0
            return;
        }
        //end new part.

        //forking:
        IntermediateConstraintExpression c2 = c1.clone();
        inters.add(c2);

        c1.unsolvedPolynomials.add(g); //G>0
        c1.unsolvedPolynomials.add(f); //F>0
        c2.unsolvedPolynomials.add(g.scalarMultiplication(-1)); //-G>0
        c2.unsolvedPolynomials.add(f.scalarMultiplication(-1)); //-F>0
    }

    private IntermediateConstraintExpression chooseAnInterWithUndigestedFraction(Set<IntermediateConstraintExpression> inters) {
        for (IntermediateConstraintExpression inter : inters) {
            if (!inter.fractionDigestionCompleted()) return inter;
        }
        return null;
    }

    private IntermediateConstraintExpression chooseAnInterWithUnsolvedPolynomial(Set<IntermediateConstraintExpression> inters, int varId) {
        for (IntermediateConstraintExpression inter : inters) {
            if (!inter.polynomialSolutionCompleted(varId)) return inter;
        }
        return null;
    }

    //*****************************************************************************************************************
    //                         fraction digestion                           polynomial solution
    // undigested fractions -----------------------> unsolved polynomials -----------------------> bounds
    class IntermediateConstraintExpression {
        Fraction fruit;
        Set<Fraction> unDigestedFractionConstraints;
        Set<Polynomial> unsolvedPolynomials; // polynomials such as 'x>c' will be solved (augmenting bound lists) and removed from this. polynomials such as 'c>0' (which is not a function of x) will be sent to the set of independent constraints

        Set<Polynomial> independentConstraints;
        Set<WeakExpression> lowerBounds; //x>l for each lower bound
        Set<WeakExpression> upperBounds; //x<u for each upper bound
        WeakExpression fruitIndefIntegral;

        private IntermediateConstraintExpression(Fraction fruit,
                                                 Set<Fraction> unDigestedFractionConstraints,
                                                 Set<Polynomial> unsolvedPolynomials,
                                                 Set<Polynomial> independentConstraints,
                                                 Set<WeakExpression> lowerBounds,
                                                 Set<WeakExpression> upperBounds,
                                                 WeakExpression fruitIndefIntegral) {
            this.fruit = fruit;
            this.unDigestedFractionConstraints = unDigestedFractionConstraints;
            this.unsolvedPolynomials = unsolvedPolynomials;
            this.independentConstraints = independentConstraints;
            this.lowerBounds = lowerBounds;
            this.upperBounds = upperBounds;
            this.fruitIndefIntegral = fruitIndefIntegral;
        }

        public IntermediateConstraintExpression(ConstrainedExpression<Fraction> fractionConstrainedExpression) {
            fruit = fractionConstrainedExpression.getFruit();
            unDigestedFractionConstraints = new HashSet<Fraction>(fractionConstrainedExpression.getConstraints());
            unsolvedPolynomials = new HashSet<Polynomial>(unDigestedFractionConstraints.size());

            independentConstraints = new HashSet<Polynomial>();
            lowerBounds = new HashSet<WeakExpression>();
            upperBounds = new HashSet<WeakExpression>();

            fruitIndefIntegral = null;
        }

        boolean fractionDigestionCompleted() {
            return unDigestedFractionConstraints.isEmpty();
        }

        public boolean polynomialSolutionCompleted(int varId) { //remained polynomials should not be function of var with this id
            return unsolvedPolynomials.isEmpty();
        }

        @Override
        protected IntermediateConstraintExpression clone() {
            return new IntermediateConstraintExpression(fruit /*note that the fruit is the same...*/,
                    new HashSet<Fraction>(unDigestedFractionConstraints),
                    new HashSet<Polynomial>(unsolvedPolynomials),

                    new HashSet<Polynomial>(independentConstraints),
                    new HashSet<WeakExpression>(lowerBounds),
                    new HashSet<WeakExpression>(upperBounds),
                    fruitIndefIntegral);
        }

        @Override
        public String toString() {
            return "\n IntermediateConstraintExpression{" +
                    "\n fruit=" + fruit +
                    (unDigestedFractionConstraints.isEmpty()? "" : ", \n unDigestedConstraints=" + unDigestedFractionConstraints) +
                    (unsolvedPolynomials.isEmpty() ? "" : ", \n digestedButUnsolvedConstraints=" + unsolvedPolynomials +
                    ", \n--------------------------------") +
                    "  \n solved independent constraints =" + independentConstraints +
                    ", \n lower bounds=" + lowerBounds +
                    ", \n upper bounds=" + upperBounds +
                    ", \n fruit indef. integral=" + fruitIndefIntegral +
                    "}\n*****************\n";
        }

        public SymbolicOneDimFunctionGenerator integrate(final int integrationVarIndex) {
            return new SymbolicOneDimFunctionGenerator() {

                @Override
                public String toString() {
                    return "value:\t" + fruitIndefIntegral +
                            "\nIndependents:\t" + independentConstraints +
                            "\nUB (min of):\t\t\t" + upperBounds +
                            "\nLB (max of):\t\t\t" + lowerBounds + "\n";
                }

                @Override
                public OneDimFunction makeFunction(final Double[] reusableVarAssign) { //NOTE: be very careful about the reusable array
                    //independent constraints and lower/higher bounds do not contain the integration var so we do not need to 'null' it.
                    for (Polynomial independent : independentConstraints) {
                        double v = independent.evaluate(reusableVarAssign);
                        if (v <= 0) {
                            return OneDimFunction.ZERO_1D_FUNCTION; //what should I do with '='?
                        }
                        //since independent "positive" constraints are not satisfied.
                    }

                    final double lb = max(lowerBounds, reusableVarAssign);
                    final double ub = min(upperBounds, reusableVarAssign);

                    if (lb >= ub) return OneDimFunction.ZERO_1D_FUNCTION; //critical!

                    reusableVarAssign[integrationVarIndex] = lb;
                    final double offset1 = fruitIndefIntegral.evaluate(reusableVarAssign);
                    reusableVarAssign[integrationVarIndex] = ub;
                    final double u = fruitIndefIntegral.evaluate(reusableVarAssign) - offset1;

                    return new OneDimFunction() {
                        @Override
                        public double eval(double var) {
                            if (var < lb) return 0.0;
                            if (var > ub) return u;

                            reusableVarAssign[integrationVarIndex] = var;
                            double f = fruitIndefIntegral.evaluate(reusableVarAssign);
                            return f - offset1;
                        }
                    };
                }

                private double max(Collection<WeakExpression> expressions, Double[] reusableVarAssign) {
                    double max = Double.NEGATIVE_INFINITY;
                    for (WeakExpression expr : expressions) {
                        max = Math.max(max, expr.evaluate(reusableVarAssign));
                    }
                    return max;
                }

                private double min(Collection<WeakExpression> expressions, Double[] reusableVarAssign) {
                    double min = Double.POSITIVE_INFINITY;
                    for (WeakExpression expr : expressions) {
                        min = Math.min(min, expr.evaluate(reusableVarAssign));
                    }
                    return min;
                }
            };
        }

    }  //end inner class

}
