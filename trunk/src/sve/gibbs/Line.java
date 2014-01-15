package sve.gibbs;

import java.text.DecimalFormat;

class Line {
    private static DecimalFormat decimal_formatter = new DecimalFormat("#.##");
    private double[] _coefs;

    // private final int _dim;

    public Line(double... c) {
        // _dim = c.length - 1;
        _coefs = c;
    }

    public Line() {
        this(1);
    }

    public Line(int d) {
        // this._dim = d;
        _coefs = new double[d + 1];
    }

    public double[] getCoefs() {
        return _coefs;
    }

    public void setCoefs(int idx, double val) {
        _coefs[idx] = val;
    }

    // public int getDegree() {
    // return _dim;
    // }

    /*
     * The number of xs should be equal to the degree
     */
    public double eval(double[] x) {
        if (x.length != _coefs.length - 1) {
            System.err.println("The number of xs should be equal to the degree of the polynomial");
            return -1.;
        }

        double s = _coefs[0];
        for (int i = 0; i < _coefs.length - 1; i++) {
            s += _coefs[i + 1] * x[i];
        }

        return s;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(decimal_formatter.format(_coefs[0]));
        String[] vars = getVars(_coefs.length);

        for (int i = 1; i < _coefs.length; i++) {
            sb.append(" + (" + decimal_formatter.format(_coefs[i]) + "*" + vars[i - 1]);
            // if (i > 1) {
            // sb.append("^" + i);
            // }
            sb.append(")");
        }

        return sb.toString();
    }

    public static String[] getVars(int dim) {
        String[] s = new String[dim - 1];
        for (int i = 0; i < s.length; i++) {
            s[i] = "x" + (i > 0 ? i : "");
        }

        return s;
    }
}