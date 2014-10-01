/**
 * 
 */
package cpomdp.market.utils;

/**
 * XADD operator constants. Taken from xadd.XADD.
 * 
 * @author skinathil
 *
 *	TODO: 
 *	1. Implement toString() method
 */
public enum XADDOperator {
    UND(0), 
    SUM(1), 
    MINUS(2),
    PROD(3),
    DIV(4),
    MAX(5),
    MIN(6),
    RESTRICT_LOW(7),
    RESTRICT_HIGH(8),
    EQ(9),
    NEQ(10),
    GT(11),
    GT_EQ(12),
    LT(13),
    LT_EQ(14),
    LINEARIZE(15),
    ROUND(16),
    ERROR(17); // This should always be max index -- anything index >= is ERROR
    
//    public final static String[] _aOpNames({/* 0 */"UND",
//        /* 1 */"+", "-", "*", "/", "max", "min", "|l", "|h",
//        /* 9 */"=", "!=", ">", ">=", "<", "<=", "LIN", "ROUND", "ERROR"})
	
    private Integer value;

    /**
     * 
     * @param value
     */
    private XADDOperator(Integer value) {
            this.setValue(value);
    }

	/**
	 * @return the value
	 */
	public Integer getValue() {
		return value;
	}

	/**
	 * @param value the value to set
	 */
	private void setValue(Integer value) {
		this.value = value;
	}
}
