package hgm.poly.gm;

import hgm.poly.Expression;
import hgm.poly.PolynomialFactory;

import java.util.Map;

/**
 * Created by Hadi Afshar.
 * Date: 3/07/14
 * Time: 4:07 AM
 */
public interface Factor {

    Factor substitution(Map<String, Double> assign);
    Factor substitution(String var, Expression value);

    PolynomialFactory getFactory();

//    Set<String> getScopeVars();

}
