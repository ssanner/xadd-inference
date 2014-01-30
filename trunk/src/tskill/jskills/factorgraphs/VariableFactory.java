package tskill.jskills.factorgraphs;

public class VariableFactory<TValue> {

    // using a Func<TValue> to encourage fresh copies in case it's overwritten
    protected final Func<TValue> variablePriorInitializer;

    public VariableFactory(Func<TValue> variablePriorInitializer) {
        this.variablePriorInitializer = variablePriorInitializer;
    }

    /*//todo...
    public Variable<TValue> createBasicVariable(String nameFormat, Object... args) {
        return new Variable<TValue>(String.format(nameFormat, args), 
                                    variablePriorInitializer.eval());
    }*/
}