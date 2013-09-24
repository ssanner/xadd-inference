package hgm;

/**
 * Created by Hadi Afshar.
 * Date: 19/09/13
 * Time: 5:03 PM
 */

/*
 A wrapper for variable name and properties...
 */
public class Variable {
    private String _name;

    public Variable(String name) {
        _name = name;
    }

    // equality and hashCode only depend on the variable name:
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Variable variable = (Variable) o;

        return _name.equals(variable.getName());

    }

    @Override
    public int hashCode() {
        return _name.hashCode();
    }

    public String getName() {
        return _name;
    }

    @Override
    public String toString() {
        return _name;
    }

}
