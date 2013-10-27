package hgm.asve.cnsrv.factory;

import hgm.asve.cnsrv.factor.IFactor;

import java.util.Map;

/**
 * Created by Hadi Afshar.
 * Date: 23/10/13
 * Time: 3:02 PM
 */
public interface ElementaryFactorFactory<F extends IFactor> {
    /**
     * @return IFactor that corresponds one
     */
    F one();

    /**
     *
     * @param vars array of variables v_1, v_2, ...
     * @return a variable that corresponds the expression v_1.v_2....
     */
    F getFactorForMultiplicationOfVars(String[] vars);

    double evaluate(F factor, Map<String, Double> completeVariableAssignment);
}
