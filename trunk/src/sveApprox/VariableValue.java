package sveApprox;

/**
 * Created by Hadi M Afshar
 * Date: 11/09/13
 * Time: 8:49 PM
 */
public class VariableValue {
    private String variable;
    private String value;

    public VariableValue(String variable, String value) {
        this.variable = variable;
        this.value = value;
    }

    public String getVariable() {
        return variable;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VariableValue that = (VariableValue) o;
        return value.equals(that.value) && variable.equals(that.variable);
    }

    @Override
    public int hashCode() {
        int result = variable.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return variable + "=" + value;
    }
}
