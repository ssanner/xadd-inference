package hgm.poly;

import hgm.sampling.VarAssignment;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 20/02/14
 * Time: 11:07 AM
 */
public class Polynomial implements Expression<Polynomial>, Cloneable{
    private PolynomialFactory factory;
    private Map<List<Double> /*powers*/, Double /*coefficient*/> powers2coefMap;

//    private List<Double> zeroPowers;

    public Polynomial(PolynomialFactory factory) {
//        this.factory = factory;
//        powers2coefMap = new HashMap<List<Double>, Double>();
        this(factory, new HashMap<List<Double>, Double>());
    }

    private Polynomial(PolynomialFactory factory, Map<List<Double>, Double> powers2coefMap) {
        this.factory = factory;
        this.powers2coefMap = powers2coefMap;

//        Double[] zeros = new Double[factory.getAllVars().length];
//        Arrays.fill(zeros, 0d);
//        zeroPowers = Arrays.asList(zeros);
//        System.out.println("zeroPowers = " + zeroPowers);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Polynomial that = (Polynomial) o;

        if (!factory.equals(that.factory)) return false;
        if (!powers2coefMap.equals(that.powers2coefMap)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = factory.hashCode();
        result = 31 * result + powers2coefMap.hashCode();
        return result;
    }

    public Double getCoefficient(List<Double> powers) {
        Double c = powers2coefMap.get(powers);
        return c == null ? 0d : c;
    }


    public void addTerm(List<Double> powers, Double coefficient) {
        if (coefficient == 0d) return;

        Double existingCoef = getCoefficient(powers);

        Double newCoef = existingCoef + coefficient;
        if (newCoef == 0d) {
            powers2coefMap.remove(powers);  // zero coefficients are not stored
        } else {
            powers2coefMap.put(powers, newCoef);
        }
    }

    public void addToThis(Polynomial other) {
        assetMatching(other);

        for (Map.Entry<List<Double>, Double> otherTermAndCoef : other.powers2coefMap.entrySet()) {
            List<Double> otherPowers = otherTermAndCoef.getKey();
            Double otherCoef = otherTermAndCoef.getValue();

            addTerm(otherPowers, otherCoef);
        }
    }

    public void addScalarToThis(double d) {
        addTerm(factory.getZeroPowers(), d);
    }

    public Set<List<Double>> getAllPowers() {
        return powers2coefMap.keySet();
    }

    public int size() {
        return powers2coefMap.size();
    }

    private void assetMatching(Polynomial other) {
        if (this.factory != other.factory) throw new RuntimeException("factory mismatch!");
    }

    public void multiplyScalarInThis(double c) {
        if (c==0) {
            powers2coefMap.clear();
            return;
        }

        for (Map.Entry<List<Double>, Double> term : powers2coefMap.entrySet()) {
            term.setValue(term.getValue() * c);
        }
    }

    public Polynomial scalarMultiplication(double c) {
        Polynomial result = this.clone();
        result.multiplyScalarInThis(c);
        return result;
    }

    @Override
    public Polynomial returnMultiplication(Polynomial other) {
        assetMatching(other);
        Polynomial prod = new Polynomial(factory);
        for (Map.Entry<List<Double>, Double> thisEntry : powers2coefMap.entrySet()) {
            List<Double> thisPow = thisEntry.getKey();
            Double thisCoef = thisEntry.getValue();

            for (Map.Entry<List<Double>, Double> thatEntry : other.powers2coefMap.entrySet()) {
                List<Double> thatPow = thatEntry.getKey();
                Double thatCoef = thatEntry.getValue();

                Double prodCoef = thisCoef * thatCoef;
                List<Double> prodPow = powersProduct(thisPow, thatPow);

                prod.addTerm(prodPow, prodCoef);
            }
        }

        return prod;
    }

    private List<Double> powersProduct(List<Double> thisPow, List<Double> thatPow) {
        Double[] prodPow = new Double[thatPow.size()];
        for (int i = 0; i < prodPow.length; i++) {
            prodPow[i] = thisPow.get(i) + thatPow.get(i);
        }

        return Arrays.asList(prodPow);
    }

    @Override
    public Polynomial substitute(Map<String, Double> assign) {
        Double[] varValues = factory.getReusableVarValues(assign);
        return substitute(varValues);
    }

    @Override
    public Polynomial substitute(Double[] varValues) {
        Polynomial result = new Polynomial(factory);
        for (Map.Entry<List<Double>, Double> term : powers2coefMap.entrySet()) {

            List<Double> pow = term.getKey();
            List<Double> newPow = new ArrayList<Double>(pow.size());

            double c = 1.0;
            for (int i = 0; i < varValues.length; i++) {
                if (varValues[i] != null) {
                    newPow.add(0d);
                    c *= Math.pow(varValues[i], pow.get(i));
                } else {
                    newPow.add(pow.get(i));
                }
            }

            result.addTerm(newPow, term.getValue() * c);
        }

        return result;

    }

    public void replaceThisWithDerivative(int derivativeVarIndex) {

        Map<List<Double>, Double> newEntries = new HashMap<List<Double>, Double>(powers2coefMap.size());

        for (Iterator<Map.Entry<List<Double>, Double>> iterator = powers2coefMap.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<List<Double>, Double> term = iterator.next();

//            iterator.remove();
            List<Double> pow = term.getKey();

            Double currentP = pow.get(derivativeVarIndex);
            if (currentP != 0) {
            pow.set(derivativeVarIndex, currentP - 1);
            newEntries.put(pow, term.getValue() * currentP);
            }
        }

        powers2coefMap = newEntries;
    }

    public Polynomial returnDerivative(int derivativeVarIndex) {
        Polynomial cloned = this.clone();
        cloned.replaceThisWithDerivative(derivativeVarIndex);
        return cloned;
    }

    @Deprecated
    public void replaceThisWithIndefiniteIntegral(String integrationVar) {
        int varIndex = factory.getVarIndex(integrationVar);
        replaceThisWithIndefiniteIntegral(varIndex);
    }

    public void replaceThisWithIndefiniteIntegral(int integrationVarIndex) {

        Map<List<Double>, Double> newEntries = new HashMap<List<Double>, Double>(powers2coefMap.size());

        for (Iterator<Map.Entry<List<Double>, Double>> iterator = powers2coefMap.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<List<Double>, Double> term = iterator.next();

            iterator.remove();
            List<Double> pow = term.getKey();

            Double newP = pow.get(integrationVarIndex) + 1;
            pow.set(integrationVarIndex, newP);
            newEntries.put(pow, term.getValue() / newP);
        }

        powers2coefMap = newEntries;
    }

    public Polynomial returnIndefiniteIntegral(int integrationVarIndex) {
        Polynomial cloned = this.clone();
        cloned.replaceThisWithIndefiniteIntegral(integrationVarIndex);
        return cloned;
    }

    //like 2 + 3.5*x^(3.9) + 6.66*x^(9.7)*y^(-3)
    @Override
    public String toString() {
        if (this.powers2coefMap.isEmpty()) return "0.0";

        StringBuilder sb = new StringBuilder(this.size());
        String[] allVars = factory.getAllVars();

        for (List<Double> powers : this.getAllPowers()) {

            sb.append(this.getCoefficient(powers)).append("*");
            for (int i = 0; i < powers.size(); i++) {
                Double p = powers.get(i);
                if (p != 0d) {
                    sb.append(allVars[i]).append("^(").append(p).append(")*");
                }
            }
            if (sb.charAt(sb.length() - 1) == '*') {
                sb.deleteCharAt(sb.length() - 1); //last "*"
            }

            sb.append("+");
        }
        if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1); //last "+"

        return sb.toString();
    }

    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone, CloneDoesntDeclareCloneNotSupportedException")
    public Polynomial clone() {
        Map<List<Double> /*powers*/, Double /*coefficient*/> newMap = new HashMap<List<Double>, Double>(powers2coefMap.size());

        for (Map.Entry<List<Double>, Double> powersAndCoef : powers2coefMap.entrySet()) {
            List<Double> ps = powersAndCoef.getKey();
            Double c = powersAndCoef.getValue();
            newMap.put(new ArrayList<Double>(ps), c);
        }

        return new Polynomial(factory, newMap);
    }

    @Deprecated
    public double evaluate(VarAssignment fullVarAssign) {  //the binary part is not used... just for compatibility...
        Double[] varValues = factory.getReusableVarValues(fullVarAssign.getContinuousVarAssign());
        return evaluate(varValues);
    }

    @Override
    public double evaluate(Double[] fullVarValues) {
        double eval = 0d;
        for (Map.Entry<List<Double>, Double> term : powers2coefMap.entrySet()) {
            List<Double> pow = term.getKey();

            double c = 1.0;
            for (int i = 0; i < fullVarValues.length; i++) {
                if (pow.get(i) != 0d) {
                    if (fullVarValues[i] == null) throw new PolynomialException("full assignment expected: " + Arrays.toString(fullVarValues));
                    c *= Math.pow(fullVarValues[i], pow.get(i));
                }
            }

            eval += term.getValue()*c;
        }

        return eval;
    }

    @Override
    public PolynomialFactory getFactory() {
        return factory;
    }

    public int degree() {
        int maxTermDegree = 0;
        for (List<Double> pow : powers2coefMap.keySet()) {
            int termDegree = 0;
            for (Double p : pow) {
                int pInt = p.intValue();
                if (pInt != p) throw new PolynomialException("degree cannot be computed for non-integer powers...");
                termDegree += pInt; //note: p should be an integer...
            }
            maxTermDegree = Math.max(maxTermDegree, termDegree);
        }
        return maxTermDegree;
    }

    //degree w.r.t. a particular var
    public int degree(int varIndex) {
        int maxDegree = 0;
        for (List<Double> pow : powers2coefMap.keySet()) {
            maxDegree = Math.max(maxDegree,  pow.get(varIndex).intValue());
        }
        return maxDegree;
    }

    /**
     * @return  this - m
     */
    public Polynomial returnSubtraction(Polynomial m) {
        Polynomial mNeg = m.clone();
        mNeg.multiplyScalarInThis(-1.0);
        return returnAddition(mNeg);
    }


    @Override
    public Polynomial returnAddition(Polynomial a) {
        Polynomial r = this.clone();
        r.addToThis(a);
        return r;
    }


    /**
     * @return The variables used in this expression rather than all factory variables.
     */
    @Override
    public Set<String> getScopeVars(){
        Set<String> scopeVars = new HashSet<String>();
        String[] allVars = factory.getAllVars();
            for (List<Double> powers : powers2coefMap.keySet()) {
                for (int vId = 0; vId < powers.size(); vId++) {
                    Double power = powers.get(vId);
                    if (power != 0) {
                        scopeVars.add(allVars[vId]);
                    }
                }
            }
        return scopeVars;
    }

    @Override
    public Polynomial substitute(String var, Polynomial value) {
        Polynomial result = new Polynomial(factory); //zero
        int varId = factory.getVarIndex(var);
        Polynomial substitution = (Polynomial)value; // not implemented for other expressions    //todo....
        Polynomial[] splits = this.split();
        Map<String, Double> map = new HashMap<String, Double>(1);
        map.put(var, 1d);
        for (Polynomial split : splits) {
            int degree = split.degree(varId);
            Polynomial powValue = substitution.toPower(degree);
            Polynomial varReduced = split.substitute(map);
            Polynomial r = varReduced.returnMultiplication(powValue);
            result.addToThis(r);
        }
        return result;
    }

    //e.g. split 'x*y + 5*x*z^2' to ['x*y', '5*x*z^2']
    public Polynomial[] split() {
        Polynomial[] results = new Polynomial[powers2coefMap.size()];
        int i=0;
        for (Map.Entry<List<Double>, Double> power2coef : powers2coefMap.entrySet()) {
            Polynomial split = new Polynomial(factory);
            split.addTerm(power2coef.getKey(), power2coef.getValue());
            results[i++] = split;
        }
        return results;
    }

    public Polynomial toPower(int power) {
        if (power<0) throw new RuntimeException("implemented only for non-negative integer powers");
        Polynomial result = factory.one();
        for (int i = 0; i < power; i++) {
            result = result.returnMultiplication(this);
        }
        return result;
    }

    public Polynomial[] sortWithRespectTo(String var){
        int varIndex = factory.getVarIndex(var);
        return sortWithRespectTo(varIndex);
    }

    //   x^2 + 3*x^2*y  +      2*x + -x*y +   y^3 w.r.t. 'x' is:
    //  [1  + 3*y],            [2    -y],     [y^3]
    //      2                      1           0        : <- note the index
    public Polynomial[] sortWithRespectTo(int varIndex){
        //calc degree w.r.t. the given var:
        Polynomial[] result = new Polynomial[degree(varIndex) + 1];
        for (int i = 0; i < result.length; i++) {
            result[i] = new Polynomial(factory);
        }

        for (List<Double> pow : getAllPowers()) {
            int varDegree = pow.get(varIndex).intValue(); //only works for int
            List<Double> clonedPow = new ArrayList<Double>(pow);
            clonedPow.set(varIndex, 0d);
            result[varDegree].addTerm(clonedPow, getCoefficient(pow));
        }
        return result;
    }

    public boolean isNumber() {
        if (powers2coefMap.size() >1) return false; //just to make it a bit faster.
        return degree() == 0;
    }

    public double getNumericalValue() {
        if (!isNumber()) throw new RuntimeException("this is a symbolic expression: " + this);
        if (powers2coefMap.size() == 0) return 0;
        List<Double> allZeroPow = powers2coefMap.keySet().iterator().next();
        return getCoefficient(allZeroPow);
    }

    public boolean isZero() {
        return powers2coefMap.isEmpty();
    }


    public Fraction cloneCastToFraction() {
        return new Fraction(this.clone(), factory.one());
    }

    //todo find more patterns...
    public boolean isAlwaysPositive() {
        if (this.isNumber() && this.getNumericalValue()>0) return true;

        for (List<Double> powers : powers2coefMap.keySet()) {
            for (Double power : powers) {
                if ((power % 2) != 0) return false; //at the moment only if all powers are even and coefficients are positive, 'true' is returned.... clearly more patterns can be found...
            }
            if (powers2coefMap.get(powers) < 0) return false;
        }
        return true;
    }

    //todo find more patterns...
    public boolean isAlwaysNegative() {
        if (this.isNumber() && this.getNumericalValue()<0) return true;

        for (List<Double> powers : powers2coefMap.keySet()) {
            for (Double power : powers) {
                if ((power % 2) != 0) return false; //at the moment only if all powers are even and coefficients are negative, 'true' is returned.... clearly more patterns can be found...
            }
            if (powers2coefMap.get(powers) > 0) return false;
        }
        return true;
    }
}

