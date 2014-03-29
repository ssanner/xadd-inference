package hgm.poly;

import hgm.sampling.VarAssignment;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 20/02/14
 * Time: 11:07 AM
 */
public class Polynomial implements Cloneable{
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

    public Polynomial multiply(Polynomial other) {
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

    public Polynomial substitute(Map<String, Double> assign) {
        Double[] varValues = factory.getReusableVarValues(assign);
        return substitute(varValues);
    }

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

    public void replaceThisWithIndefiniteIntegral(String integrationVar) {
        int varIndex = factory.getVarIndex(integrationVar);

        Map<List<Double>, Double> newEntries = new HashMap<List<Double>, Double>(powers2coefMap.size());

        for (Iterator<Map.Entry<List<Double>, Double>> iterator = powers2coefMap.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<List<Double>, Double> term = iterator.next();

            iterator.remove();
            List<Double> pow = term.getKey();

            Double newP = pow.get(varIndex) + 1;
            pow.set(varIndex, newP);
            newEntries.put(pow, term.getValue() / newP);
        }

        powers2coefMap = newEntries;
    }

    @Override
    public String toString() {
        return factory.toString(this);
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
}

