package tskill.jskills.factorgraphs;

import lombok.Getter;
import lombok.Setter;

public class Message<T> {

    private final String nameFormat;
    private final Object[] nameFormatArgs;

    @Getter @Setter private T value;

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public Message() { this(null, null, (Object[])null); }

    public Message(T value, String nameFormat, Object... args) {
        this.nameFormat = nameFormat;
        nameFormatArgs = args;
        this.value = value;
    }

    @Override
    public String toString() {
        return (nameFormat == null) ? 
                super.toString() : 
                    String.format(nameFormat, nameFormatArgs);
    }
}