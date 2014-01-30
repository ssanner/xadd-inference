package tskill.jskills.factorgraphs;

import lombok.Getter;

public class KeyedVariable<TKey, TValue> extends Variable<TValue> {

    @Getter private final TKey key;

    public TKey getKey() {
        return key;
    }

    public KeyedVariable(TKey key, TValue prior, String name, Object... args) {
        super(prior, name, args);
        this.key = key;
    }
}