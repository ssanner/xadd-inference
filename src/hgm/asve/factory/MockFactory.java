package hgm.asve.factory;

import hgm.Variable;
import hgm.asve.factor.MockFactor;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 20/09/13
 * Time: 9:14 PM
 */
public class MockFactory implements FactorFactory<MockFactor> {
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
                if (f1.getText().length() < f2.getText().length()) return -1;
                if (f1.getText().length() > f2.getText().length()) return 1;

                if (f1.getText().hashCode() < f2.getText().hashCode()) return -1;
                if (f1.getText().hashCode() > f2.getText().hashCode()) return 1;

                return 0;
            }
        });
        String productFactorValue = "[";
        for (MockFactor f : sortedFactors) {
            productFactorValue += "(" + f.getText() + ")" + TIMES;
        }
        productFactorValue = productFactorValue.substring(0, productFactorValue.length() - TIMES.length()) + "]";
        return new MockFactor(productFactorValue);

    }

    @Override
    public MockFactor marginalize(MockFactor factor, Variable v) {
        String factorText = "[" + DEFINITE_INTEGRAL + "_{" + v + "}<" + factor.getText() + ">]";
        //the marginalized variable should not be a variable of this factor. To do so, We omit its brackets:
        String nakedVar = v.getName().substring(1, v.getName().length() - 1);
        factorText = factorText.replaceAll("\\[" + nakedVar + "\\]", nakedVar);
        return new MockFactor(factorText);
    }

    @Override
    public MockFactor approximate(MockFactor factor) {
        return new MockFactor(APPROXIMATE + "(\"" + factor.getText() + "\")");
    }
}

