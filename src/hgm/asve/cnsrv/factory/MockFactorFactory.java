package hgm.asve.cnsrv.factory;

import hgm.asve.cnsrv.factor.MockFactor;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 20/09/13
 * Time: 9:14 PM
 */
public class MockFactorFactory implements FactorFactory<MockFactor> {
    public static final String TIMES = ".";
    public static final String DEFINITE_INTEGRAL = "DEF_INT";
    public static final String APPROXIMATE = "APPROX";

    @Override
    public MockFactor multiply(Collection<MockFactor> factors) {
        //to guarantee that always same result is produced the factor set is sorted first:
        List<MockFactor> sortedFactors = new ArrayList<MockFactor>(factors);
        Collections.sort(sortedFactors, new Comparator<MockFactor>() {
            @Override
            public int compare(MockFactor f1, MockFactor f2) {
                if (f1.getHelpingText().length() <   f2.getHelpingText().length()) return -1;
                if (f1.getHelpingText().length() >   f2.getHelpingText().length()) return 1;
                if (f1.getHelpingText().hashCode() < f2.getHelpingText().hashCode()) return -1;
                if (f1.getHelpingText().hashCode() > f2.getHelpingText().hashCode()) return 1;

                return 0;
            }
        });
        String productFactorValue = "[";
        for (MockFactor f : sortedFactors) {
            productFactorValue += "(" + f.getHelpingText() + ")" + TIMES;
        }
        productFactorValue = productFactorValue.substring(0, productFactorValue.length() - TIMES.length()) + "]";
        return new MockFactor(productFactorValue);

    }

    @Override
    public MockFactor marginalize(MockFactor factor, String v) {
        String factorText = "[" + DEFINITE_INTEGRAL + "_{" + v + "}<" + factor.getHelpingText() + ">]";
        //the marginalized variable should not be a variable of this factor. To do so, We omit its brackets:
        String nakedVar = v.substring(1, v.length() - 1);
        factorText = factorText.replaceAll("\\[" + nakedVar + "\\]", nakedVar);
        return new MockFactor(factorText);
    }

    @Override
    public MockFactor approximate(MockFactor factor, double param1, double param2) {
        return new MockFactor(APPROXIMATE + "(\"" + factor.getHelpingText() + "\")");
    }

    class MultOfVarsFactor extends MockFactor{
        /**
         * a list of (possibly many repeated) variables to be multiplied together
         */
        private List<String> elements;
        public MultOfVarsFactor(String factorText, List<String> elements) {
            super(factorText);
            this.elements = elements;
        }

        List<String> getElements() {
            return elements;
        }
    }

    MockFactor one = new MultOfVarsFactor("ONE", new ArrayList<String>());

    @Override
    public MockFactor one() {
        return one;
    }

    @Override
    public MockFactor getFactorForMultiplicationOfVars(String[] vars) {
        StringBuilder sb = new StringBuilder();
        for (String var : vars) {
            sb.append(".").append(var);
        }

        MockFactor mult = new MultOfVarsFactor(sb.substring(1), Arrays.asList(vars));
        return mult;
    }

    @Override
    public double evaluate(MockFactor factor, Map<String, Double> completeVariableAssignment) {
        if (!(factor instanceof MultOfVarsFactor)) throw new RuntimeException("only implemented for multiplication of factors");
        double d = 1;
        for (String var : ((MultOfVarsFactor)factor).getElements()) {
            Double value = completeVariableAssignment.get(var);
            if (value == null) throw new RuntimeException("no assignment for variable: " + var + " found");
            d*= value;
        }

        return d;
    }
}

