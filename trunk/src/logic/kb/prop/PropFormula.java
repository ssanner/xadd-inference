package logic.kb.prop;

import java.util.*;

//import logic.add.*;

public class PropFormula {

	public abstract static class Term {
		// Not efficient, but String representation should 
		// contain relevant information
		public int hashCode() {
			return toString().hashCode();
		}
		public boolean equals(Object o) {
			return toString().equals(o.toString());
		}
	}

	public abstract static class BinConn // BinaryConnective
			extends Term {

		public final static int INVALID = 0;
		public final static int AND = 1;
		public final static int OR = 2;
		public final static int IMPLIES = 3;
		public final static int EQUIV = 4;

		public abstract Term getLTerm();

		public abstract Term getRTerm();

		public abstract int getType();

	}

	public abstract static class UnConn // UnaryConnective
			extends Term {

		public final static int INVALID = 0;
		public final static int NEG = 1;

		public abstract Term getTerm();

		public abstract int getType();

	}

	public abstract static class Prop extends Term {

		public abstract int getID(); // Unique id for a literal
	}

	public abstract static class TruthConstant extends Term {

		public abstract boolean getTruthValue();

	}

}
