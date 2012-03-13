package logic.kb.fol.kif;

/** CUP generated class containing symbol constants. */
public class Symbol {

    /* Local vas */
    public int _nID;
    public int _nLine;
    public Object _value;

    /* Constructors and methods */
    public Symbol(int id, int line) {
	_nID = id;
	_value = null;	
	_nLine = line;
    }

    public Symbol(int id, String s, int line) {
	_nID = id;
	_value = s;
	_nLine = line;
    }

    public Symbol(Integer i, int line) {
	_nID = INTEGER;
	_value = i;
	_nLine = line;
    }

    public Symbol(Double d, int line) {
	_nID = DOUBLE;
	_value = d;
	_nLine = line;
    }

    public String toString() {
	String name;
	switch (_nID) {
	case LPAREN:  { name = "'('"; } break; 
	case GREATER: { name = "'>'"; } break; 
	case SEMI:    { name = "';'"; } break; 
	case COLON:   { name = "':'"; } break; 
	case EXISTS:  { name = "EXISTS"; } break; 
	case FORALL:  { name = "FORALL"; } break; 
	case LESS:    { name = "'<'"; } break; 
	case RPAREN:  { name = "')'"; } break; 
	case NOT:     { name = "'~'"; } break; 
	case AND:     { name = "'^'"; } break; 
	case OR:      { name = "'|'"; } break; 
	case BANG:    { name = "'!'"; } break; 
	case COMMA:   { name = "','"; } break; 
	case DIV:     { name = "'/'"; } break; 
	case PLUS:    { name = "'+'"; } break; 
	case MINUS:   { name = "'-'"; } break; 
	case LESSEQ:  { name = "'<='"; } break; 
	case DOT:     { name = "'.'"; } break; 
	case EOF:     { name = "[EOF]"; } break; 
	case EQUAL:   { name = "'='"; } break; 
	case TRUE:    { name = "'true'"; } break; 
	case error:   { name = "[ERROR]"; } break; 
	case MOD:     { name = "'%'"; } break; 
	case NEWLINE: { name = "[NEWLINE]"; } break; 
	case IMPLY:   { name = "'=>'"; } break;
	case QST:     { name = "'?'"; } break; 
	case LBRACK:  { name = "'['"; } break; 
	case TIMES:   { name = "'*'"; } break; 
	case RBRACK:  { name = "']'"; } break; 
	case NEQUAL:  { name = "'~='"; } break; 
	case EQUIV:   { name = "'<=>'"; } break; 
	case GREATEREQ: { name = "'>='"; } break; 
	case COUNT:   { name = "'#'"; } break; 
	case FALSE:   { name = "'false'"; } break; 
	case LCBRACE: { name = "'{'"; } break; 
	case RCBRACE: { name = "'}'"; } break; 
	case VARIABLE: {name = "VAR:'?" + _value.toString() + "'"; } break;
	case COMMENT: { name = "COMMENT:'" + _value.toString() + "'"; } break;
	case IDENT:   { name = "IDENT:'"   + _value.toString() + "'"; } break;
	case INTEGER: { name = "INTEGER:'" + _value.toString() + "'"; } break;
	case DOUBLE:  { name = "DOUBLE:'"  + _value.toString() + "'"; } break;
	default: { name = "[UNKNOWN AT LINE " + _nLine + "]"; } break;
	}
	return name;
    }

    /* IDs */
    public static final int HIGHEST = 31;
    public static final int DOUBLE = 33;
    public static final int INTEGER = 32;
    public static final int LPAREN = 6;
    public static final int GREATER = 15;
    public static final int SEMI = 2;
    public static final int LESS = 13;
    public static final int RPAREN = 7;
    public static final int NOT = 19;
    public static final int AND = 18;
    public static final int OR = 20;
    public static final int BANG = 3;
    public static final int COMMA = 9;
    public static final int DIV = 21;
    public static final int PLUS = 4;
    public static final int MINUS = 40;
    public static final int LESSEQ = 14;
    public static final int DOT = 8;
    public static final int EOF = 0;
    public static final int EQUAL = 24;
    public static final int TRUE = 26;
    public static final int error = 1;
    public static final int MOD = 25;
    public static final int IDENT = 34;
    public static final int NEWLINE = 35;
    public static final int IMPLY = 22;
    public static final int QST = 11;
    public static final int LBRACK = 10;
    public static final int TIMES = 5;
    public static final int RBRACK = 12;
    public static final int NEQUAL = 28;
    public static final int EQUIV = 23;
    public static final int GREATEREQ = 16;
    public static final int COUNT = 17;
    public static final int FALSE = 27;
    public static final int LCBRACE = 38;
    public static final int RCBRACE = 39;
    public static final int COMMENT = 36;
    public static final int COLON = 41;
    public static final int EXISTS = 42;
    public static final int FORALL = 43;
    public static final int VARIABLE = 44;
}

