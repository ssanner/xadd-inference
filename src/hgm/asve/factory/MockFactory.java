package hgm.asve.factory;

import hgm.Variable;
import hgm.asve.factor.OLD_MockFactor;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 20/09/13
 * Time: 9:14 PM
 */
@Deprecated
public class MockFactory implements OLD_FactorFactory<OLD_MockFactor> {
    public static final String TIMES = ".";
    public static final String DEFINITE_INTEGRAL = "DEF_INT";
    public static final String APPROXIMATE = "APPROX";

    @Override
    public OLD_MockFactor multiply(Collection<OLD_MockFactor> factors) {
        //to guarantee that always same result is produced the factor set is sorted first:
        List<OLD_MockFactor> sortedFactors = new ArrayList<OLD_MockFactor>(factors);
        Collections.sort(sortedFactors, new Comparator<OLD_MockFactor>() {
            @Override
            public int compare(OLD_MockFactor f1, OLD_MockFactor f2) {
                if (f1.getText().length() < f2.getText().length()) return -1;
                if (f1.getText().length() > f2.getText().length()) return 1;

                if (f1.getText().hashCode() < f2.getText().hashCode()) return -1;
                if (f1.getText().hashCode() > f2.getText().hashCode()) return 1;

                return 0;
            }
        });
        String productFactorValue = "[";
        for (OLD_MockFactor f : sortedFactors) {
            productFactorValue += "(" + f.getText() + ")" + TIMES;
        }
        productFactorValue = productFactorValue.substring(0, productFactorValue.length() - TIMES.length()) + "]";
        return new OLD_MockFactor(productFactorValue);

    }

    @Override
    public OLD_MockFactor marginalize(OLD_MockFactor factor, Variable v) {
        String factorText = "[" + DEFINITE_INTEGRAL + "_{" + v + "}<" + factor.getText() + ">]";
        //the marginalized variable should not be a variable of this factor. To do so, We omit its brackets:
        String nakedVar = v.getName().substring(1, v.getName().length() - 1);
        factorText = factorText.replaceAll("\\[" + nakedVar + "\\]", nakedVar);
        return new OLD_MockFactor(factorText);
    }

    @Override
    public OLD_MockFactor approximate(OLD_MockFactor factor) {
        return new OLD_MockFactor(APPROXIMATE + "(\"" + factor.getText() + "\")");
    }
}

