package hgm.poly.market;

import hgm.poly.ConstrainedPolynomial;
import hgm.poly.PolynomialFactory;

/**
 * Created by Hadi Afshar.
 * Date: 28/04/14
 * Time: 11:03 AM
 */
public interface PriorGenerator {
    //todo: prior should be Piecewise polynomial not a single constraint polynomial...
    ConstrainedPolynomial computePrior(PolynomialFactory factory);
}
