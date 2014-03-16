package hgm.poly.integral;

import hgm.poly.Function;

/**
 * Created by Hadi Afshar.
 * Date: 24/02/14
 * Time: 11:43 PM
 */
public interface OneDimFunction {
    double eval(double var);

    public static final OneDimFunction ZERO_1D_FUNCTION = new OneDimFunction() {
        @Override
        public double eval(double var) {
            return 0;
        }
    };
}
