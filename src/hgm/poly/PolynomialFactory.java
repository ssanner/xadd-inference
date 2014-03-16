package hgm.poly;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 20/02/14
 * Time: 10:54 AM
 */
public class PolynomialFactory {
    private String[] vars;
    // if a var does not evaluated, its place is filled with NULL
    private Double[] reusableAssignment;

    /**
     * This is a power list used to access constant in any polynomial using this factory
     */
    private final List<Double> zeroPowers;


    public PolynomialFactory(String... vars) { //NOTE: 'E' should not be a variable name since it may  be used in doubles i.e.: 5.0E-4
        this.vars = vars;
        Arrays.sort(this.vars); //so that they can be searched later on

        reusableAssignment = new Double[this.vars.length];

        Double[] zerosPowerArray = new Double[this.vars.length];
        Arrays.fill(zerosPowerArray, 0d);
        zeroPowers = Arrays.asList(zerosPowerArray);
    }

    public List<Double> getZeroPowers() {
        return zeroPowers;
    }

    public Polynomial makePolynomial(String str) {
        Polynomial poly = new Polynomial(this);

        String[] expressions = str.split("\\+");
        for (String expr : expressions) {
            expr = expr.trim();

            String[] subExpressions = expr.split("\\*");
            Double coef = 1d;
            List<Double> powers = Arrays.asList(new Double[vars.length]);
            Collections.fill(powers, 0d);

            for (String subExpr : subExpressions) {
                String includedVar = null;
                int includedVarIndex = -1;
                for (int i = 0; i < vars.length; i++) {
                    String var = vars[i];
                    if (subExpr.contains(var)) {
                        includedVar = var;
                        includedVarIndex = i;
                        break;
                    }
                }

                if (includedVar == null) {
                    //coefficient:
                    coef = coef * Double.valueOf(subExpr);
                } else {
                    if (!subExpr.startsWith(includedVar + '^'))
                        throw new PolynomialException("cannot parse: " + subExpr);
                    subExpr = subExpr.substring(includedVar.length() + "^".length());

                    if (!subExpr.startsWith("(")) throw new PolynomialException("'(' expected: " + subExpr);
                    if (!subExpr.endsWith(")")) throw new PolynomialException("')' expected: " + subExpr);
                    subExpr = subExpr.substring(1, subExpr.length() - 1); //p
                    powers.set(includedVarIndex, Double.valueOf(subExpr));
                }
            }

            poly.addTerm(powers, coef);
        }

        return poly;
    }

    //like 2 + 3.5*x^(3.9) + 6.66*x^(9.7)*y^(-3)
    public String toString(Polynomial poly) {
        StringBuilder sb = new StringBuilder(poly.size());

        for (List<Double> powers : poly.getAllPowers()) {

            sb.append(poly.getCoefficient(powers)).append("*");
            for (int i = 0; i < powers.size(); i++) {
                Double p = powers.get(i);
                if (p != 0d) {
                    sb.append(vars[i]).append("^(").append(p).append(")*");
                }
            }
            if (sb.charAt(sb.length() - 1) == '*') {
                sb.deleteCharAt(sb.length() - 1); //last "*"
            }

            sb.append("+");
        }
        if (sb.length()>0) sb.deleteCharAt(sb.length() - 1); //last "+"

        return sb.toString();
    }

    public Double[] getReusableVarValues(Map<String, Double> assign) {

        Set<String> instantiatedVars = assign.keySet();
        for (int i = 0; i < vars.length; i++) {
            String var = vars[i];
            reusableAssignment[i] = instantiatedVars.contains(var) ? assign.get(var) : null;
        }

        return reusableAssignment;
    }

    public int getVarIndex(String var) {
        return Arrays.binarySearch(vars, var);
    }

    //condition polynomial is always > 0
    public Polynomial makePositiveConstraint(String polyStr) {
        polyStr = polyStr.replaceAll(" ", "");
        if (!polyStr.endsWith(">0") && !polyStr.endsWith("<0")) throw new PolynomialException("cannot parse constraint: " + polyStr);

        Polynomial polynomial = makePolynomial(polyStr.substring(0, polyStr.length() - 2)); //for "<0" or ">0"

        if (polyStr.endsWith("<0")) {
            polynomial.multiplyScalarInThis(-1);
        }

        return polynomial;
    }

    public ConstrainedPolynomial makeConstrainedPolynomial(String polynomialStr, String... conditionsStr) {
        Polynomial poly = makePolynomial(polynomialStr);
        Polynomial[] posConstraints = new Polynomial[conditionsStr.length];
        for (int i = 0; i < conditionsStr.length; i++) {
            posConstraints[i] = makePositiveConstraint(conditionsStr[i]);
        }

        return new ConstrainedPolynomial(poly, Arrays.asList(posConstraints));
    }

    public String[] getAllVars() {
        return vars;
    }

    public int numberOfVars() {
        return vars.length;
    }
}
