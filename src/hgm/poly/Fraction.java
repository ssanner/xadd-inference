package hgm.poly;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 20/02/14
 * Time: 11:07 AM
 */
public class Fraction implements Expression<Fraction>, Cloneable {
    private PolynomialFactory factory;
    private Polynomial numerator;
    private Polynomial denominator;

    public Fraction(Polynomial numerator, Polynomial denominator) {
        this.factory = numerator.getFactory();
        if (denominator.getFactory() != this.factory) throw new RuntimeException("factory mismatch");
        this.numerator = numerator;
        this.denominator = numerator.isZero() ? factory.one() : denominator;      // simplify 0/X to 0/1
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Fraction fraction = (Fraction) o;

        return denominator.equals(fraction.denominator) && numerator.equals(fraction.numerator);

    }

    @Override
    public int hashCode() {
        int result = numerator.hashCode();
        result = 31 * result + denominator.hashCode();
        return result;
    }

    private void assetMatching(Fraction other) {
        if (this.factory != other.factory) throw new RuntimeException("factory mismatch!");
    }

    @Override
    public Fraction returnAddition(Fraction other) {
        assetMatching(other);
        if (this.denominator.equals(other.denominator)) {
            Polynomial n = numerator.returnAddition(other.numerator);
            return new Fraction(n, denominator.clone());
        } else {
            Polynomial d = this.denominator.returnMultiplication(other.denominator);//How can I be more clever using least common multiple?
            Polynomial n = this.numerator.returnMultiplication(other.denominator).returnAddition(other.numerator.returnMultiplication(this.denominator));
            return new Fraction(n, d);
        }

    }

    public void multiplyScalarInThis(double c) {
        numerator.multiplyScalarInThis(c);
    }

    @Override
    public Fraction returnMultiplication(Fraction other) {
        assetMatching(other);
        if (this.numerator.equals(other.denominator))
            return new Fraction(other.numerator.clone(), this.denominator.clone()); //how to optimize using least common multiples?
        if (other.numerator.equals(this.denominator))
            return new Fraction(this.numerator.clone(), other.denominator.clone()); //how to optimize using least common multiples?

        Polynomial n = this.numerator.returnMultiplication(other.numerator);
        Polynomial d = this.denominator.returnMultiplication(other.denominator);
        return new Fraction(n, d);
    }

    public Fraction divide(Fraction other) {
        assetMatching(other);

        if (this.numerator.equals(other.numerator))
            return new Fraction(other.denominator.clone(), this.denominator.clone()); //how to optimize using least common multiples?
        if (this.denominator.equals(other.denominator))
            return new Fraction(this.numerator.clone(), other.numerator.clone()); //how to optimize using least common multiples?

        Polynomial n = this.numerator.returnMultiplication(other.denominator);
        Polynomial d = this.denominator.returnMultiplication(other.numerator);
        return new Fraction(n, d);
    }

    @Override
    public Fraction substitute(Map<String, Double> assign) {
        return new Fraction(numerator.substitute(assign), denominator.substitute(assign));
    }

    @Override
    public Fraction substitute(Double[] varValues) {
        return new Fraction(numerator.substitute(varValues), denominator.substitute(varValues));
    }

    @Override
    public Fraction substitute(String var, Fraction value) {
        if (!(value instanceof Fraction)) throw new RuntimeException("not implemented for " + value.getClass());
        Fraction substitutionFraction = (Fraction) value;  //todo
        Polynomial n = nonNormalizedSubstitution(numerator, substitutionFraction, var);
        Polynomial d = nonNormalizedSubstitution(denominator, substitutionFraction, var);

        int varIndex = factory.getVarIndex(var);
        int nMaxVarDeg = numerator.degree(varIndex);
        int dMaxVarDeg = denominator.degree(varIndex);

        if (nMaxVarDeg > dMaxVarDeg) {
            d = d.returnMultiplication(substitutionFraction.denominator.toPower(nMaxVarDeg - dMaxVarDeg));
        } else if (nMaxVarDeg < dMaxVarDeg) {
            n = n.returnMultiplication(substitutionFraction.denominator.toPower(dMaxVarDeg - nMaxVarDeg));
        }
        return new Fraction(n, d);
    }

    //e.g. 5*x^2*y + 6*x*y with substitution A/B for x returns 5*A^2*y + 6*A*y*B
    private Polynomial nonNormalizedSubstitution(Polynomial polynomial, Fraction substitution, String var) {
        int varIndex = factory.getVarIndex(var);
        int nMaxVarDeg = polynomial.degree(varIndex);
        Polynomial[] numeratorAtoms = polynomial.split();
        Polynomial nResult = new Polynomial(factory);
        for (Polynomial nAtom : numeratorAtoms) {
            int deg = nAtom.degree(varIndex);
            Polynomial numeratorHandled = nAtom.substitute(var, substitution.numerator);
            nResult.addToThis(numeratorHandled.returnMultiplication(substitution.denominator.toPower(nMaxVarDeg - deg)));
        }
        return nResult;
    }

    @Override
    public double evaluate(Double[] fullVarAssign) {
        double nv = numerator.evaluate(fullVarAssign);
        double dv = denominator.evaluate(fullVarAssign);

        return nv / dv;
    }

    public double evaluate(Map<String, Double> fullContinuousVarAssign) {
        Double[] varValues = factory.getReusableVarValues(fullContinuousVarAssign);
        return evaluate(varValues);
    }

    @Override
    public String toString() {
        String ns = numerator.toString();
        String ds = denominator.toString();
//        int fractionLength = Math.max(ns.length(), ds.length());
//        StringBuilder sb = new StringBuilder(fractionLength);
//        for (int i = 0; i < fractionLength; i++) {
//            sb.append("-");
//        }
//        return ns + "\n" + sb.toString() + "\n" + ds;
        return "[" + ns + "]" + "/" + "[" + ds + "]";
    }

    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone, CloneDoesntDeclareCloneNotSupportedException")
    public Fraction clone() {
        return new Fraction(numerator.clone(), denominator.clone());
    }

    @Override
    public PolynomialFactory getFactory() {
        return factory;
    }

    public Fraction subtract(Fraction m) {
        Fraction mNeg = m.clone();
        mNeg.multiplyScalarInThis(-1.0);
        return returnAddition(mNeg);
    }

    /**
     * @return The variables used in this expression rather than all factory variables.
     */
    @Override
    public Set<String> getScopeVars() {
        Set<String> scopeVars = numerator.getScopeVars();
        scopeVars.addAll(denominator.getScopeVars());
        return scopeVars;
    }

    //should be read-only
    public Polynomial getNumerator() {
        return numerator;
    }

    //should be read-only
    public Polynomial getDenominator() {
        return denominator;
    }

    public WeakExpression calcIndefIntegral(final int varIndex) {
        int denominatorRelDegree = denominator.degree(varIndex);

        //1. if a polynomial:
        if (denominatorRelDegree == 0) {
            Polynomial numeratorIntegral = numerator.returnIndefiniteIntegral(varIndex);
            return new Fraction(numeratorIntegral, denominator.clone());
        }

        int numeratorRelDegree = numerator.degree(varIndex);

        //2. form: (b1)/(a2*x + b2)
        if (numeratorRelDegree == 0 && denominatorRelDegree == 1) {
            //integral([b_1]/[a_2 * x + b_2])dx = [b_1 * log(a_2 * x + b_2)] / [a_2]

            final Polynomial b1 = numerator.clone();
            Polynomial[] a2b2 = denominator.sortWithRespectTo(varIndex);
            if (a2b2.length != 2) throw new RuntimeException("how possible!");
            final Polynomial a2 = a2b2[1];
            final Polynomial b2 = a2b2[0];
            return new WeakExpression() {
                String xStr = b1.getFactory().getAllVars()[varIndex];

                /**
                 * @return [b_1 * log(a_2 * x + b_2)] / [a_2]
                 */
                @Override
                public double evaluate(Double[] fullVarAssign) {
                    double x = fullVarAssign[varIndex];
                    double a2Val = a2.evaluate(fullVarAssign);
                    if (a2Val == 0) throw new RuntimeException("division by zero");
                    return b1.evaluate(fullVarAssign) * Math.log(a2Val * x + b2.evaluate(fullVarAssign)) / a2Val;
                }

                @Override
                public String toString() {
                    return "[" + b1 + " * log{" + a2 + " * " + xStr + " + " + b2 + "}] / [" + a2 + "]";
                }
            };
        }

        //3. for: (c1)/(a2*x^2 + b2*x + c2)
        if (numeratorRelDegree == 0 && denominatorRelDegree == 2) {
            final Polynomial c1 = numerator.clone();
            Polynomial[] a2b2c2 = denominator.sortWithRespectTo(varIndex);
            if (a2b2c2.length != 3) throw new RuntimeException("how possible!");
            final Polynomial a2 = a2b2c2[2];
            final Polynomial b2 = a2b2c2[1];
            final Polynomial c2 = a2b2c2[0];
            return new WeakExpression() {
                String xStr = c1.getFactory().getAllVars()[varIndex];

                // see http://en.wikipedia.org/wiki/List_of_integrals_of_rational_functions
                @Override
                public double evaluate(Double[] fullVarAssign) {
                    double g = c1.evaluate(fullVarAssign);

                    double x = fullVarAssign[varIndex];
                    double a = a2.evaluate(fullVarAssign);
                    double b = b2.evaluate(fullVarAssign);
                    double c = c2.evaluate(fullVarAssign);
                    double del = 4 * a * c - b * b;
                    double two_ax_plus_b = 2 * a * x + b;

                    if (del > 0) {
                        double delRoot = Math.sqrt(del);
                        double result = (2 * c1.evaluate(fullVarAssign) * Math.atan(two_ax_plus_b / delRoot)) / delRoot;
                        if (Double.isNaN(result)) throw new RuntimeException();
                        return result;
                    } else if (del < 0) {
                        double delRoot = Math.sqrt(-del);
                        double result = (1/delRoot) * Math.log(Math.abs((two_ax_plus_b - delRoot)/(two_ax_plus_b + delRoot)));
                        if (Double.isNaN(result)) throw new RuntimeException();
                        return result;
                    } else { //del == 0
                        return -2/two_ax_plus_b;
                    }
                }

                @Override
                public String toString() {
                    return "INTEGRAL[" + c1 + "]/[(" + a2 + ")X^2 + (" + b2 + ")X + (" + c2 + ")] dX";
                }
            };
        }

        throw new RuntimeException("Integral of this not implemented yet: " + this + "\n\t w.r.t. var " + numerator.getFactory().getAllVars()[varIndex]);
    }
}

