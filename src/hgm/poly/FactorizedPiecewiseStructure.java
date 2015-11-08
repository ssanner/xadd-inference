package hgm.poly;

import java.util.List;
import java.util.SortedSet;

/**
 * Created by Hadi M Afshar.
 * Date: 11/4/15
 * Time: 3:21 AM
 */
public interface FactorizedPiecewiseStructure<E extends Expression> {
    PolynomialFactory getFactory();

    double evaluate(Double[] assign);

    SortedSet<String> getScopeVars();

    FactorizedPiecewiseStructure<E> substitute(Double[] assign);

    List<PiecewiseExpression<E>> getFactorsNotInvolving(String var);
    List<PiecewiseExpression<E>> getFactorsInvolving(String var);
}
