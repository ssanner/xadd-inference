package util;

public class IntPair {
	public int _i1;
	public int _i2;
	public IntPair(int i1, int i2) {
		_i1 = i1;
		_i2 = i2;
	}
	public void set(int i1, int i2) {
		_i1 = i1;
		_i2 = i2;
	}
	public int hashCode() {
		return (_i1) + (_i2 << 10) - (_i2 >>> 10);
	}
	public boolean equals(Object o) {
		if (o instanceof IntPair) {
			IntPair i = (IntPair)o;
			return this._i1 == i._i1 
				&& this._i2 == i._i2;
		} else
			return false;
	}
}
