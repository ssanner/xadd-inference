package sve.gibbs;

import java.text.DecimalFormat;

class Constraint {

    private Line _line;
    private boolean _greaterThan;

    public Constraint(Line line, boolean greaterThan) {
        _line = line;
        _greaterThan = greaterThan;
    }

    public String toString() {
        return _line.toString() + " " + (_greaterThan ? ">=" : "<") + " 0";
    }

}