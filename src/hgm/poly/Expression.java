package hgm.poly;

import java.util.Collection;
import java.util.Map;

/**
 * Created by Hadi Afshar.
 * Date: 26/07/14
 * Time: 7:31 PM
 */
public interface Expression<E extends Expression> extends Cloneable, WeakExpression {
    PolynomialFactory getFactory();

    Collection<String> getScopeVars();

    E substitute(Map<String, Double> assign);
    E substitute(Double[] varValues);
    E substitute(String var, E value); //maybe this should be generic as well but then PiecewisePolynomial should become generic....

    E returnMultiplication(E otherExpression);

    E returnAddition(E otherExpression);

//    @Override
//    @SuppressWarnings("CloneDoesntCallSuperClone, CloneDoesntDeclareCloneNotSupportedException")
//    E clone();

//    void replaceThisWithIndefiniteIntegral(int integrationVarIndex);
}
