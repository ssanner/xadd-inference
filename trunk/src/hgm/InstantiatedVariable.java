package hgm;

/**
 * Created by Hadi M Afshar
 * Date: 11/09/13
 * Time: 8:49 PM
 */
public class InstantiatedVariable {
    private Variable _variable;
    private String _value;

    public InstantiatedVariable(Variable variable, String value) {
        this._variable = variable;
        this._value = value;
    }

    public Variable getVariable() {
        return _variable;
    }

    public String getValue() {
        return _value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InstantiatedVariable that = (InstantiatedVariable) o;
        return _value.equals(that._value) && _variable.equals(that._variable);
    }

    @Override
    public int hashCode() {
        int result = _variable.hashCode();
        result = 31 * result + _value.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return _variable + "=" + _value;
    }
}
