package util;

public class IntQuad {
    public int _i1;
    public int _i2;
    public int _i3;
    public int _i4;

    public IntQuad(int i1, int i2, int i3, int i4) {
        _i1 = i1;
        _i2 = i2;
        _i3 = i3;
        _i4 = i4;
    }

    public void set(int i1, int i2, int i3, int i4) {
        _i1 = i1;
        _i2 = i2;
        _i3 = i3;
        _i4 = i4;
    }

    public int hashCode() {
        return (_i1) + (_i2 << 10) - (_i3 << 20)
                + (_i3 >>> 20) - (_i2 >>> 10) + (_i4);
    }

    public boolean equals(Object o) {
        if (o instanceof IntQuad) {
            IntQuad t = (IntQuad) o;
            return this._i1 == t._i1
                    && this._i2 == t._i2
                    && this._i3 == t._i3
                    && this._i4 == t._i4;
        } else
            return false;
    }
}

