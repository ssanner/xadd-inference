package logic.kb.fol.kif;


class Yylex {
	private final int YY_BUFFER_SIZE = 512;
	private final int YY_F = -1;
	private final int YY_NO_STATE = -1;
	private final int YY_NOT_ACCEPT = 0;
	private final int YY_START = 1;
	private final int YY_END = 2;
	private final int YY_NO_ANCHOR = 4;
	private final int YY_BOL = 65536;
	private final int YY_EOF = 65537;

public int yyline() { return yyline; } 
	private java.io.BufferedReader yy_reader;
	private int yy_buffer_index;
	private int yy_buffer_read;
	private int yy_buffer_start;
	private int yy_buffer_end;
	private char yy_buffer[];
	private int yychar;
	private int yyline;
	private boolean yy_at_bol;
	private int yy_lexical_state;

	Yylex (java.io.Reader reader) {
		this ();
		if (null == reader) {
			throw (new Error("Error: Bad input stream initializer."));
		}
		yy_reader = new java.io.BufferedReader(reader);
	}

	Yylex (java.io.InputStream instream) {
		this ();
		if (null == instream) {
			throw (new Error("Error: Bad input stream initializer."));
		}
		yy_reader = new java.io.BufferedReader(new java.io.InputStreamReader(instream));
	}

	private Yylex () {
		yy_buffer = new char[YY_BUFFER_SIZE];
		yy_buffer_read = 0;
		yy_buffer_index = 0;
		yy_buffer_start = 0;
		yy_buffer_end = 0;
		yychar = 0;
		yyline = 0;
		yy_at_bol = true;
		yy_lexical_state = YYINITIAL;
	}

	private boolean yy_eof_done = false;
	private final int YYINITIAL = 0;
	private final int yy_state_dtrans[] = {
		0
	};
	private void yybegin (int state) {
		yy_lexical_state = state;
	}
	private int yy_advance ()
		throws java.io.IOException {
		int next_read;
		int i;
		int j;

		if (yy_buffer_index < yy_buffer_read) {
			return yy_buffer[yy_buffer_index++];
		}

		if (0 != yy_buffer_start) {
			i = yy_buffer_start;
			j = 0;
			while (i < yy_buffer_read) {
				yy_buffer[j] = yy_buffer[i];
				++i;
				++j;
			}
			yy_buffer_end = yy_buffer_end - yy_buffer_start;
			yy_buffer_start = 0;
			yy_buffer_read = j;
			yy_buffer_index = j;
			next_read = yy_reader.read(yy_buffer,
					yy_buffer_read,
					yy_buffer.length - yy_buffer_read);
			if (-1 == next_read) {
				return YY_EOF;
			}
			yy_buffer_read = yy_buffer_read + next_read;
		}

		while (yy_buffer_index >= yy_buffer_read) {
			if (yy_buffer_index >= yy_buffer.length) {
				yy_buffer = yy_double(yy_buffer);
			}
			next_read = yy_reader.read(yy_buffer,
					yy_buffer_read,
					yy_buffer.length - yy_buffer_read);
			if (-1 == next_read) {
				return YY_EOF;
			}
			yy_buffer_read = yy_buffer_read + next_read;
		}
		return yy_buffer[yy_buffer_index++];
	}
	private void yy_move_end () {
		if (yy_buffer_end > yy_buffer_start &&
		    '\n' == yy_buffer[yy_buffer_end-1])
			yy_buffer_end--;
		if (yy_buffer_end > yy_buffer_start &&
		    '\r' == yy_buffer[yy_buffer_end-1])
			yy_buffer_end--;
	}
	private boolean yy_last_was_cr=false;
	private void yy_mark_start () {
		int i;
		for (i = yy_buffer_start; i < yy_buffer_index; ++i) {
			if ('\n' == yy_buffer[i] && !yy_last_was_cr) {
				++yyline;
			}
			if ('\r' == yy_buffer[i]) {
				++yyline;
				yy_last_was_cr=true;
			} else yy_last_was_cr=false;
		}
		yychar = yychar
			+ yy_buffer_index - yy_buffer_start;
		yy_buffer_start = yy_buffer_index;
	}
	private void yy_mark_end () {
		yy_buffer_end = yy_buffer_index;
	}
	private void yy_to_mark () {
		yy_buffer_index = yy_buffer_end;
		yy_at_bol = (yy_buffer_end > yy_buffer_start) &&
		            ('\r' == yy_buffer[yy_buffer_end-1] ||
		             '\n' == yy_buffer[yy_buffer_end-1] ||
		             2028/*LS*/ == yy_buffer[yy_buffer_end-1] ||
		             2029/*PS*/ == yy_buffer[yy_buffer_end-1]);
	}
	private java.lang.String yytext () {
		return (new java.lang.String(yy_buffer,
			yy_buffer_start,
			yy_buffer_end - yy_buffer_start));
	}
	private int yylength () {
		return yy_buffer_end - yy_buffer_start;
	}
	private char[] yy_double (char buf[]) {
		int i;
		char newbuf[];
		newbuf = new char[2*buf.length];
		for (i = 0; i < buf.length; ++i) {
			newbuf[i] = buf[i];
		}
		return newbuf;
	}
	private final int YY_E_INTERNAL = 0;
	private final int YY_E_MATCH = 1;
	private java.lang.String yy_error_string[] = {
		"Error: Internal error.\n",
		"Error: Unmatched input.\n"
	};
	private void yy_error (int code,boolean fatal) {
		java.lang.System.out.print(yy_error_string[code]);
		java.lang.System.out.flush();
		if (fatal) {
			throw new Error("Fatal Error.\n");
		}
	}
	private int[][] unpackFromString(int size1, int size2, String st) {
		int colonIndex = -1;
		String lengthString;
		int sequenceLength = 0;
		int sequenceInteger = 0;

		int commaIndex;
		String workString;

		int res[][] = new int[size1][size2];
		for (int i= 0; i < size1; i++) {
			for (int j= 0; j < size2; j++) {
				if (sequenceLength != 0) {
					res[i][j] = sequenceInteger;
					sequenceLength--;
					continue;
				}
				commaIndex = st.indexOf(',');
				workString = (commaIndex==-1) ? st :
					st.substring(0, commaIndex);
				st = st.substring(commaIndex+1);
				colonIndex = workString.indexOf(':');
				if (colonIndex == -1) {
					res[i][j]=Integer.parseInt(workString);
					continue;
				}
				lengthString =
					workString.substring(colonIndex+1);
				sequenceLength=Integer.parseInt(lengthString);
				workString=workString.substring(0,colonIndex);
				sequenceInteger=Integer.parseInt(workString);
				res[i][j] = sequenceInteger;
				sequenceLength--;
			}
		}
		return res;
	}
	private int yy_acpt[] = {
		/* 0 */ YY_NOT_ACCEPT,
		/* 1 */ YY_NO_ANCHOR,
		/* 2 */ YY_NO_ANCHOR,
		/* 3 */ YY_NO_ANCHOR,
		/* 4 */ YY_NO_ANCHOR,
		/* 5 */ YY_NO_ANCHOR,
		/* 6 */ YY_NO_ANCHOR,
		/* 7 */ YY_NO_ANCHOR,
		/* 8 */ YY_NO_ANCHOR,
		/* 9 */ YY_NO_ANCHOR,
		/* 10 */ YY_NO_ANCHOR,
		/* 11 */ YY_NO_ANCHOR,
		/* 12 */ YY_NO_ANCHOR,
		/* 13 */ YY_NO_ANCHOR,
		/* 14 */ YY_NO_ANCHOR,
		/* 15 */ YY_NO_ANCHOR,
		/* 16 */ YY_NO_ANCHOR,
		/* 17 */ YY_NO_ANCHOR,
		/* 18 */ YY_NO_ANCHOR,
		/* 19 */ YY_NO_ANCHOR,
		/* 20 */ YY_NO_ANCHOR,
		/* 21 */ YY_NO_ANCHOR,
		/* 22 */ YY_NO_ANCHOR,
		/* 23 */ YY_NO_ANCHOR,
		/* 24 */ YY_NO_ANCHOR,
		/* 25 */ YY_NO_ANCHOR,
		/* 26 */ YY_NO_ANCHOR,
		/* 27 */ YY_NO_ANCHOR,
		/* 28 */ YY_NO_ANCHOR,
		/* 29 */ YY_NO_ANCHOR,
		/* 30 */ YY_NO_ANCHOR,
		/* 31 */ YY_NO_ANCHOR,
		/* 32 */ YY_NO_ANCHOR,
		/* 33 */ YY_NO_ANCHOR,
		/* 34 */ YY_NO_ANCHOR,
		/* 35 */ YY_NO_ANCHOR,
		/* 36 */ YY_NO_ANCHOR,
		/* 37 */ YY_NO_ANCHOR,
		/* 38 */ YY_NO_ANCHOR,
		/* 39 */ YY_NO_ANCHOR,
		/* 40 */ YY_NO_ANCHOR,
		/* 41 */ YY_NO_ANCHOR,
		/* 42 */ YY_NO_ANCHOR,
		/* 43 */ YY_NO_ANCHOR,
		/* 44 */ YY_NO_ANCHOR,
		/* 45 */ YY_NO_ANCHOR,
		/* 46 */ YY_NO_ANCHOR,
		/* 47 */ YY_NO_ANCHOR,
		/* 48 */ YY_NO_ANCHOR,
		/* 49 */ YY_NO_ANCHOR,
		/* 50 */ YY_NO_ANCHOR,
		/* 51 */ YY_NO_ANCHOR,
		/* 52 */ YY_NO_ANCHOR,
		/* 53 */ YY_NO_ANCHOR,
		/* 54 */ YY_NO_ANCHOR,
		/* 55 */ YY_NO_ANCHOR,
		/* 56 */ YY_NO_ANCHOR,
		/* 57 */ YY_NO_ANCHOR,
		/* 58 */ YY_NO_ANCHOR,
		/* 59 */ YY_NO_ANCHOR,
		/* 60 */ YY_NOT_ACCEPT,
		/* 61 */ YY_NO_ANCHOR,
		/* 62 */ YY_NO_ANCHOR,
		/* 63 */ YY_NO_ANCHOR,
		/* 64 */ YY_NO_ANCHOR,
		/* 65 */ YY_NO_ANCHOR,
		/* 66 */ YY_NOT_ACCEPT,
		/* 67 */ YY_NO_ANCHOR,
		/* 68 */ YY_NOT_ACCEPT,
		/* 69 */ YY_NO_ANCHOR,
		/* 70 */ YY_NOT_ACCEPT,
		/* 71 */ YY_NO_ANCHOR,
		/* 72 */ YY_NO_ANCHOR,
		/* 73 */ YY_NO_ANCHOR,
		/* 74 */ YY_NO_ANCHOR,
		/* 75 */ YY_NO_ANCHOR,
		/* 76 */ YY_NO_ANCHOR,
		/* 77 */ YY_NO_ANCHOR,
		/* 78 */ YY_NO_ANCHOR,
		/* 79 */ YY_NO_ANCHOR,
		/* 80 */ YY_NO_ANCHOR,
		/* 81 */ YY_NO_ANCHOR,
		/* 82 */ YY_NO_ANCHOR,
		/* 83 */ YY_NO_ANCHOR,
		/* 84 */ YY_NO_ANCHOR,
		/* 85 */ YY_NO_ANCHOR,
		/* 86 */ YY_NO_ANCHOR,
		/* 87 */ YY_NO_ANCHOR,
		/* 88 */ YY_NO_ANCHOR,
		/* 89 */ YY_NO_ANCHOR,
		/* 90 */ YY_NO_ANCHOR,
		/* 91 */ YY_NO_ANCHOR,
		/* 92 */ YY_NO_ANCHOR,
		/* 93 */ YY_NO_ANCHOR,
		/* 94 */ YY_NO_ANCHOR,
		/* 95 */ YY_NO_ANCHOR,
		/* 96 */ YY_NO_ANCHOR,
		/* 97 */ YY_NO_ANCHOR,
		/* 98 */ YY_NO_ANCHOR,
		/* 99 */ YY_NO_ANCHOR,
		/* 100 */ YY_NO_ANCHOR,
		/* 101 */ YY_NO_ANCHOR,
		/* 102 */ YY_NO_ANCHOR,
		/* 103 */ YY_NO_ANCHOR,
		/* 104 */ YY_NO_ANCHOR,
		/* 105 */ YY_NO_ANCHOR,
		/* 106 */ YY_NO_ANCHOR,
		/* 107 */ YY_NO_ANCHOR,
		/* 108 */ YY_NO_ANCHOR,
		/* 109 */ YY_NO_ANCHOR,
		/* 110 */ YY_NO_ANCHOR,
		/* 111 */ YY_NO_ANCHOR,
		/* 112 */ YY_NO_ANCHOR,
		/* 113 */ YY_NO_ANCHOR,
		/* 114 */ YY_NO_ANCHOR,
		/* 115 */ YY_NO_ANCHOR,
		/* 116 */ YY_NO_ANCHOR,
		/* 117 */ YY_NO_ANCHOR,
		/* 118 */ YY_NO_ANCHOR,
		/* 119 */ YY_NO_ANCHOR,
		/* 120 */ YY_NO_ANCHOR,
		/* 121 */ YY_NO_ANCHOR,
		/* 122 */ YY_NO_ANCHOR,
		/* 123 */ YY_NO_ANCHOR,
		/* 124 */ YY_NO_ANCHOR,
		/* 125 */ YY_NO_ANCHOR,
		/* 126 */ YY_NO_ANCHOR,
		/* 127 */ YY_NO_ANCHOR,
		/* 128 */ YY_NO_ANCHOR,
		/* 129 */ YY_NO_ANCHOR,
		/* 130 */ YY_NO_ANCHOR,
		/* 131 */ YY_NO_ANCHOR,
		/* 132 */ YY_NO_ANCHOR,
		/* 133 */ YY_NO_ANCHOR,
		/* 134 */ YY_NO_ANCHOR,
		/* 135 */ YY_NO_ANCHOR,
		/* 136 */ YY_NO_ANCHOR,
		/* 137 */ YY_NO_ANCHOR,
		/* 138 */ YY_NO_ANCHOR,
		/* 139 */ YY_NO_ANCHOR
	};
	private int yy_cmap[] = unpackFromString(1,65538,
"2:8,60:2,3,2:2,3,2:18,60,32,54,58,2,38,2:2,35,36,34,33,39,53,37,1,52:10,20," +
"21,42,43,44,55,59,17,56,30,46,11,16,56:2,25,56:2,18,49,31,27,50,56,9,19,8,1" +
"0,56:2,24,56:2,40,2,41,2,57,2,13,56,28,45,7,12,56:2,23,56:2,14,47,29,26,48," +
"56,5,15,4,6,56:2,22,56:2,2:3,51,2:65409,0:2")[0];

	private int yy_rmap[] = unpackFromString(1,140,
"0,1,2,1,3,4,5,1:6,5:2,1:3,6,7,8,9,10,1,11,1:2,12,13,5:2,14,1:4,15,1,5:6,1,1" +
"6,17,5:6,18,5:6,19,20,21,22,23,24,20,25,23,26,24,27,28,29,30,31,32,33,34,35" +
",36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,37,47,51,52,53,54,5,55,56,57," +
"58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82," +
"83,84,5,85,86,87,88,89,90,91")[0];

	private int yy_nxt[][] = unpackFromString(92,61,
"1,2,3,4,5,132:2,133,134,132:2,135,136,94,132:2,137,97,132:2,6,7,132,62,132," +
"67,69,71,138,98,139,99,8,9,10,11,12,13,14,15,16,17,18,19,20,132:6,21,22,23," +
"61,24,132:2,25,3,4,-1:62,60,-1:41,26,-1:20,4,-1:56,4,-1:4,132,100,132:15,-1" +
",132:10,-1:5,101:2,-1:6,132:6,-1,101,102,-1:2,132,101,102,72,-1:5,132:17,-1" +
",132:10,-1:5,101:2,-1:6,132:6,-1,101,102,-1:2,132,101,102,72,-1:44,31,-1:61" +
",32,-1:59,33,-1:60,34,-1:21,132:3,77,132:3,77,132:9,-1,132:10,-1:5,78,101,-" +
"1:6,132:6,-1,22,102,-1:2,132,101,102,72,-1:5,36:17,-1,36:10,-1:5,36:2,-1:6," +
"36:6,-1,36,-1:3,36:2,-1:7,132:8,40,132:8,-1,132:10,-1:5,101:2,-1:6,132:6,-1" +
",101,102,-1:2,132,101,102,72,-1:5,132:12,41,132:4,-1,132:10,-1:5,101:2,-1:6" +
",132:6,-1,101,102,-1:2,132,101,102,72,-1:45,44,-1:20,36:17,-1,36:10,-1:5,36" +
":2,-1:6,36:6,-1,36:2,-1:2,36:3,63,-1:5,132:17,-1,132:10,-1:5,101:2,-1:6,132" +
":6,-1,45,102,-1:2,132,101,102,72,-1:5,132:3,86,132:3,86,132:9,-1,132:10,-1:" +
"5,101:2,-1:6,132:6,-1,46,102,-1:2,132,101,102,72,-1:5,132:17,-1,132:10,-1:5" +
",101:2,-1:6,132:6,-1,53,102,-1:2,132,101,102,72,-1:2,60:2,37,60:57,-1,66:53" +
",35,66:6,-1:4,132:8,27,132:8,-1,132:10,-1:5,101:2,-1:6,132:2,110,132:3,-1,1" +
"01,102,-1:2,132,101,102,72,-1:60,63,-1:53,64,-1:60,65,-1:12,132:12,28,132:4" +
",-1,132:10,-1:5,101:2,-1:6,132:4,111,132,-1,101,102,-1:2,132,101,102,72,-1:" +
"5,132,29,132:15,-1,132:10,-1:5,101:2,-1:6,132:6,-1,101,102,-1:2,132,101,102" +
",72,-1:5,132:5,30,132:11,-1,132:10,-1:5,101:2,-1:6,132:6,-1,101,102,-1:2,13" +
"2,101,102,72,-1:60,72,-1:5,132:17,-1,132:10,-1:5,101:2,-1:6,38,132:5,-1,101" +
",102,-1:2,132,101,102,72,-1:5,132:17,-1,132:10,-1:5,101:2,-1:6,132,39,132:4" +
",-1,101,102,-1:2,132,101,102,72,-1:5,42,132:16,-1,132:10,-1:5,101:2,-1:6,13" +
"2:6,-1,101,102,-1:2,132,101,102,72,-1:5,132:4,43,132:12,-1,132:10,-1:5,101:" +
"2,-1:6,132:6,-1,101,102,-1:2,132,101,102,72,-1:5,132:17,-1,132:10,-1,68,-1:" +
"3,101:2,-1:6,132:6,-1,45,81,-1:2,132,101,102,72,-1:5,132:17,-1,132:10,-1:5," +
"101:2,-1:6,132:6,-1,46,102,-1:2,132,101,102,72,-1:5,132:3,47,132:13,-1,132:" +
"10,-1:5,101:2,-1:6,132:6,-1,101,102,-1:2,132,101,102,72,-1:5,132:7,48,132:9" +
",-1,132:10,-1:5,101:2,-1:6,132:6,-1,101,102,-1:2,132,101,102,72,-1:5,102:17" +
",-1,102:10,-1:5,102:2,-1:6,102:6,-1,95,102,-1:2,102:3,72,-1:5,132:3,49,132:" +
"13,-1,132:10,-1:5,101:2,-1:6,132:6,-1,101,102,-1:2,132,101,102,72,-1:5,132:" +
"7,50,132:9,-1,132:10,-1:5,101:2,-1:6,132:6,-1,101,102,-1:2,132,101,102,72,-" +
"1:5,51,132:16,-1,132:10,-1:5,101:2,-1:6,132:6,-1,101,102,-1:2,132,101,102,7" +
"2,-1:5,132:4,52,132:12,-1,132:10,-1:5,101:2,-1:6,132:6,-1,101,102,-1:2,132," +
"101,102,72,-1:5,132:17,-1,132:10,-1,70,-1:3,101:2,-1:6,132:6,-1,53,91,-1:2," +
"132,101,102,72,-1:5,132:11,54,132:5,-1,132:10,-1:5,101:2,-1:6,132:6,-1,101," +
"102,-1:2,132,101,102,72,-1:5,132:15,55,132,-1,132:10,-1:5,101:2,-1:6,132:6," +
"-1,101,102,-1:2,132,101,102,72,-1:5,132:10,56,132:6,-1,132:10,-1:5,101:2,-1" +
":6,132:6,-1,101,102,-1:2,132,101,102,72,-1:5,132:14,57,132:2,-1,132:10,-1:5" +
",101:2,-1:6,132:6,-1,101,102,-1:2,132,101,102,72,-1:5,102:17,-1,102:10,-1:5" +
",102:2,-1:6,102:6,-1,96,102,-1:2,102:3,72,-1:5,132:11,58,132:5,-1,132:10,-1" +
":5,101:2,-1:6,132:6,-1,101,102,-1:2,132,101,102,72,-1:5,132:15,59,132,-1,13" +
"2:10,-1:5,101:2,-1:6,132:6,-1,101,102,-1:2,132,101,102,72,-1:5,132:17,-1,13" +
"2:7,73,132:2,-1:5,101:2,-1:6,132:6,-1,101,102,-1:2,132,101,102,72,-1:5,132:" +
"17,-1,132:9,74,-1:5,101:2,-1:6,132:6,-1,101,102,-1:2,132,101,102,72,-1:5,13" +
"2:17,-1,132:4,75,132:5,-1:5,101:2,-1:6,132:6,-1,101,102,-1:2,132,101,102,72" +
",-1:5,132:17,-1,132:5,76,132:4,-1:5,101:2,-1:6,132:6,-1,101,102,-1:2,132,10" +
"1,102,72,-1:5,132:2,79,132:14,-1,132:10,-1:5,101:2,-1:6,132:6,-1,101,102,-1" +
":2,132,101,102,72,-1:5,102:17,-1,102:10,-1:5,102:2,-1:6,102:6,-1,102:2,-1:2" +
",102:3,72,-1:5,132:17,-1,132,114,132:8,-1:5,101:2,-1:6,132:6,-1,101,102,-1:" +
"2,132,101,102,72,-1:5,132:6,80,132:10,-1,132:10,-1:5,101:2,-1:6,132:6,-1,10" +
"1,102,-1:2,132,101,102,72,-1:5,132:17,-1,132:3,115,132:6,-1:5,101:2,-1:6,13" +
"2:6,-1,101,102,-1:2,132,101,102,72,-1:5,132:10,116,132:6,-1,132:10,-1:5,101" +
":2,-1:6,132:6,-1,101,102,-1:2,132,101,102,72,-1:5,132,117,132:15,-1,132:10," +
"-1:5,101:2,-1:6,132:6,-1,101,102,-1:2,132,101,102,72,-1:5,132:14,118,132:2," +
"-1,132:10,-1:5,101:2,-1:6,132:6,-1,101,102,-1:2,132,101,102,72,-1:5,132:5,1" +
"19,132:11,-1,132:10,-1:5,101:2,-1:6,132:6,-1,101,102,-1:2,132,101,102,72,-1" +
":5,132:17,-1,132:10,-1:5,101:2,-1:6,132:3,120,132:2,-1,101,102,-1:2,132,101" +
",102,72,-1:5,132:17,-1,132:10,-1:5,101:2,-1:6,132:5,121,-1,101,102,-1:2,132" +
",101,102,72,-1:5,132:2,122,132:14,-1,132:10,-1:5,101:2,-1:6,132:6,-1,101,10" +
"2,-1:2,132,101,102,72,-1:5,132:6,123,132:10,-1,132:10,-1:5,101:2,-1:6,132:6" +
",-1,101,102,-1:2,132,101,102,72,-1:5,132:11,124,132:5,-1,132:10,-1:5,101:2," +
"-1:6,132:6,-1,101,102,-1:2,132,101,102,72,-1:5,132:15,125,132,-1,132:10,-1:" +
"5,101:2,-1:6,132:6,-1,101,102,-1:2,132,101,102,72,-1:5,132:11,82,132:5,-1,1" +
"32:10,-1:5,101:2,-1:6,132:6,-1,101,102,-1:2,132,101,102,72,-1:5,132:9,126,1" +
"32:7,-1,132:10,-1:5,101:2,-1:6,132:6,-1,101,102,-1:2,132,101,102,72,-1:5,13" +
"2:15,83,132,-1,132:10,-1:5,101:2,-1:6,132:6,-1,101,102,-1:2,132,101,102,72," +
"-1:5,132:13,127,132:3,-1,132:10,-1:5,101:2,-1:6,132:6,-1,101,102,-1:2,132,1" +
"01,102,72,-1:5,132:10,128,132:6,-1,132:10,-1:5,101:2,-1:6,132:6,-1,101,102," +
"-1:2,132,101,102,72,-1:5,132:14,129,132:2,-1,132:10,-1:5,101:2,-1:6,132:6,-" +
"1,101,102,-1:2,132,101,102,72,-1:5,132:17,-1,132:7,84,132:2,-1:5,101:2,-1:6" +
",132:6,-1,101,102,-1:2,132,101,102,72,-1:5,132:17,-1,132:9,85,-1:5,101:2,-1" +
":6,132:6,-1,101,102,-1:2,132,101,102,72,-1:5,87,132:16,-1,132:10,-1:5,101:2" +
",-1:6,132:6,-1,101,102,-1:2,132,101,102,72,-1:5,132:4,88,132:12,-1,132:10,-" +
"1:5,101:2,-1:6,132:6,-1,101,102,-1:2,132,101,102,72,-1:5,132:10,89,132:6,-1" +
",132:10,-1:5,101:2,-1:6,132:6,-1,101,102,-1:2,132,101,102,72,-1:5,132:14,90" +
",132:2,-1,132:10,-1:5,101:2,-1:6,132:6,-1,101,102,-1:2,132,101,102,72,-1:5," +
"132:17,-1,132,130,132:8,-1:5,101:2,-1:6,132:6,-1,101,102,-1:2,132,101,102,7" +
"2,-1:5,132:17,-1,132:3,131,132:6,-1:5,101:2,-1:6,132:6,-1,101,102,-1:2,132," +
"101,102,72,-1:5,132:3,92,132:13,-1,132:10,-1:5,101:2,-1:6,132:6,-1,101,102," +
"-1:2,132,101,102,72,-1:5,132:7,93,132:9,-1,132:10,-1:5,101:2,-1:6,132:6,-1," +
"101,102,-1:2,132,101,102,72,-1:5,132:17,-1,103,132:9,-1:5,101:2,-1:6,132:6," +
"-1,101,102,-1:2,132,101,102,72,-1:5,132:5,104,132:11,-1,132:10,-1:5,101:2,-" +
"1:6,132:6,-1,101,102,-1:2,132,101,102,72,-1:5,132:17,-1,132:2,105,132:7,-1:" +
"5,101:2,-1:6,132:6,-1,101,102,-1:2,132,101,102,72,-1:5,132:9,106,132:7,-1,1" +
"32:4,107,132:5,-1:5,101:2,-1:6,132:6,-1,101,102,-1:2,132,101,102,72,-1:5,13" +
"2:13,108,132:3,-1,132:5,109,132:4,-1:5,101:2,-1:6,132:6,-1,101,102,-1:2,132" +
",101,102,72,-1:5,132:17,-1,132:4,112,132:5,-1:5,101:2,-1:6,132:6,-1,101,102" +
",-1:2,132,101,102,72,-1:5,132:17,-1,132:5,113,132:4,-1:5,101:2,-1:6,132:6,-" +
"1,101,102,-1:2,132,101,102,72,-1");

	public Symbol nextToken ()
		throws java.io.IOException {
		int yy_lookahead;
		int yy_anchor = YY_NO_ANCHOR;
		int yy_state = yy_state_dtrans[yy_lexical_state];
		int yy_next_state = YY_NO_STATE;
		int yy_last_accept_state = YY_NO_STATE;
		boolean yy_initial = true;
		int yy_this_accept;

		yy_mark_start();
		yy_this_accept = yy_acpt[yy_state];
		if (YY_NOT_ACCEPT != yy_this_accept) {
			yy_last_accept_state = yy_state;
			yy_mark_end();
		}
		while (true) {
			if (yy_initial && yy_at_bol) yy_lookahead = YY_BOL;
			else yy_lookahead = yy_advance();
			yy_next_state = YY_F;
			yy_next_state = yy_nxt[yy_rmap[yy_state]][yy_cmap[yy_lookahead]];
			if (YY_EOF == yy_lookahead && true == yy_initial) {
  
	return new Symbol(Symbol.EOF, yyline()); 
			}
			if (YY_F != yy_next_state) {
				yy_state = yy_next_state;
				yy_initial = false;
				yy_this_accept = yy_acpt[yy_state];
				if (YY_NOT_ACCEPT != yy_this_accept) {
					yy_last_accept_state = yy_state;
					yy_mark_end();
				}
			}
			else {
				if (YY_NO_STATE == yy_last_accept_state) {
					throw (new Error("Lexical Error: Unmatched Input."));
				}
				else {
					yy_anchor = yy_acpt[yy_last_accept_state];
					if (0 != (YY_END & yy_anchor)) {
						yy_move_end();
					}
					yy_to_mark();
					switch (yy_last_accept_state) {
					case 1:
						
					case -2:
						break;
					case 2:
						{ return new Symbol(Symbol.DIV, yyline()); }
					case -3:
						break;
					case 3:
						{ System.err.println("Illegal character: "+yytext()+":ln:" + yyline()); }
					case -4:
						break;
					case 4:
						{ /* ignore white space. */ }
					case -5:
						break;
					case 5:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -6:
						break;
					case 6:
						{ return new Symbol(Symbol.COLON, yyline()); }
					case -7:
						break;
					case 7:
						{ return new Symbol(Symbol.SEMI, yyline()); }
					case -8:
						break;
					case 8:
						{ return new Symbol(Symbol.BANG, yyline()); }
					case -9:
						break;
					case 9:
						{ return new Symbol(Symbol.PLUS, yyline()); }
					case -10:
						break;
					case 10:
						{ return new Symbol(Symbol.TIMES, yyline()); }
					case -11:
						break;
					case 11:
						{ return new Symbol(Symbol.LPAREN, yyline()); }
					case -12:
						break;
					case 12:
						{ return new Symbol(Symbol.RPAREN, yyline()); }
					case -13:
						break;
					case 13:
						{ return new Symbol(Symbol.DOT, yyline()); }
					case -14:
						break;
					case 14:
						{ return new Symbol(Symbol.MOD, yyline()); }
					case -15:
						break;
					case 15:
						{ return new Symbol(Symbol.COMMA, yyline()); }
					case -16:
						break;
					case 16:
						{ return new Symbol(Symbol.LBRACK, yyline()); }
					case -17:
						break;
					case 17:
						{ return new Symbol(Symbol.RBRACK, yyline()); }
					case -18:
						break;
					case 18:
						{ return new Symbol(Symbol.LESS, yyline()); }
					case -19:
						break;
					case 19:
						{ return new Symbol(Symbol.EQUAL, yyline()); }
					case -20:
						break;
					case 20:
						{ return new Symbol(Symbol.GREATER, yyline()); }
					case -21:
						break;
					case 21:
						{ return new Symbol(Symbol.NOT, yyline()); }
					case -22:
						break;
					case 22:
						{ return new Symbol(new Integer(yytext()), yyline()); }
					case -23:
						break;
					case 23:
						{ return new Symbol(Symbol.MINUS, yyline()); }
					case -24:
						break;
					case 24:
						{ return new Symbol(Symbol.QST, yyline()); }
					case -25:
						break;
					case 25:
						{ return new Symbol(Symbol.COUNT, yyline()); }
					case -26:
						break;
					case 26:
						{ return new Symbol(Symbol.NEQUAL, yyline()); }
					case -27:
						break;
					case 27:
						{ return new Symbol(Symbol.IMPLY, yyline()); }
					case -28:
						break;
					case 28:
						{ return new Symbol(Symbol.IMPLY, yyline()); }
					case -29:
						break;
					case 29:
						{ return new Symbol(Symbol.OR, yyline()); }
					case -30:
						break;
					case 30:
						{ return new Symbol(Symbol.OR, yyline()); }
					case -31:
						break;
					case 31:
						{ return new Symbol(Symbol.LESSEQ, yyline()); }
					case -32:
						break;
					case 32:
						{ return new Symbol(Symbol.IMPLY, yyline()); }
					case -33:
						break;
					case 33:
						{ return new Symbol(Symbol.GREATEREQ, yyline()); }
					case -34:
						break;
					case 34:
						{ return new Symbol(Symbol.NEQUAL, yyline()); }
					case -35:
						break;
					case 35:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -36:
						break;
					case 36:
						{ return new Symbol(Symbol.VARIABLE, yytext().substring(1), yyline()); }
					case -37:
						break;
					case 37:
						{ return new Symbol(Symbol.COMMENT, yytext(), yyline()); }
					case -38:
						break;
					case 38:
						{ return new Symbol(Symbol.AND, yyline()); }
					case -39:
						break;
					case 39:
						{ return new Symbol(Symbol.AND, yyline()); }
					case -40:
						break;
					case 40:
						{ return new Symbol(Symbol.EQUIV, yyline()); }
					case -41:
						break;
					case 41:
						{ return new Symbol(Symbol.EQUIV, yyline()); }
					case -42:
						break;
					case 42:
						{ return new Symbol(Symbol.NOT, yyline()); }
					case -43:
						break;
					case 43:
						{ return new Symbol(Symbol.NOT, yyline()); }
					case -44:
						break;
					case 44:
						{ return new Symbol(Symbol.EQUIV, yyline()); }
					case -45:
						break;
					case 45:
						{ return new Symbol(new Double(yytext()), yyline()); }
					case -46:
						break;
					case 46:
						{ return new Symbol(new Double(yytext()), yyline()); }
					case -47:
						break;
					case 47:
						{ return new Symbol(Symbol.TRUE, yyline()); }
					case -48:
						break;
					case 48:
						{ return new Symbol(Symbol.TRUE, yyline()); }
					case -49:
						break;
					case 49:
						{ return new Symbol(Symbol.FALSE, yyline()); }
					case -50:
						break;
					case 50:
						{ return new Symbol(Symbol.FALSE, yyline()); }
					case -51:
						break;
					case 51:
						{ return new Symbol(Symbol.COUNT, yyline()); }
					case -52:
						break;
					case 52:
						{ return new Symbol(Symbol.COUNT, yyline()); }
					case -53:
						break;
					case 53:
						{ return new Symbol(new Double(yytext()), yyline()); }
					case -54:
						break;
					case 54:
						{ return new Symbol(Symbol.EXISTS, yyline()); }
					case -55:
						break;
					case 55:
						{ return new Symbol(Symbol.EXISTS, yyline()); }
					case -56:
						break;
					case 56:
						{ return new Symbol(Symbol.FORALL, yyline()); }
					case -57:
						break;
					case 57:
						{ return new Symbol(Symbol.FORALL, yyline()); }
					case -58:
						break;
					case 58:
						{ return new Symbol(Symbol.IMPLY, yyline()); }
					case -59:
						break;
					case 59:
						{ return new Symbol(Symbol.IMPLY, yyline()); }
					case -60:
						break;
					case 61:
						{ System.err.println("Illegal character: "+yytext()+":ln:" + yyline()); }
					case -61:
						break;
					case 62:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -62:
						break;
					case 63:
						{ return new Symbol(Symbol.VARIABLE, yytext().substring(1), yyline()); }
					case -63:
						break;
					case 64:
						{ return new Symbol(new Double(yytext()), yyline()); }
					case -64:
						break;
					case 65:
						{ return new Symbol(new Double(yytext()), yyline()); }
					case -65:
						break;
					case 67:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -66:
						break;
					case 69:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -67:
						break;
					case 71:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -68:
						break;
					case 72:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -69:
						break;
					case 73:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -70:
						break;
					case 74:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -71:
						break;
					case 75:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -72:
						break;
					case 76:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -73:
						break;
					case 77:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -74:
						break;
					case 78:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -75:
						break;
					case 79:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -76:
						break;
					case 80:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -77:
						break;
					case 81:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -78:
						break;
					case 82:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -79:
						break;
					case 83:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -80:
						break;
					case 84:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -81:
						break;
					case 85:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -82:
						break;
					case 86:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -83:
						break;
					case 87:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -84:
						break;
					case 88:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -85:
						break;
					case 89:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -86:
						break;
					case 90:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -87:
						break;
					case 91:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -88:
						break;
					case 92:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -89:
						break;
					case 93:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -90:
						break;
					case 94:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -91:
						break;
					case 95:
						{ return new Symbol(new Double(yytext()), yyline()); }
					case -92:
						break;
					case 96:
						{ return new Symbol(new Double(yytext()), yyline()); }
					case -93:
						break;
					case 97:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -94:
						break;
					case 98:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -95:
						break;
					case 99:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -96:
						break;
					case 100:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -97:
						break;
					case 101:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -98:
						break;
					case 102:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -99:
						break;
					case 103:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -100:
						break;
					case 104:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -101:
						break;
					case 105:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -102:
						break;
					case 106:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -103:
						break;
					case 107:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -104:
						break;
					case 108:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -105:
						break;
					case 109:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -106:
						break;
					case 110:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -107:
						break;
					case 111:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -108:
						break;
					case 112:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -109:
						break;
					case 113:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -110:
						break;
					case 114:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -111:
						break;
					case 115:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -112:
						break;
					case 116:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -113:
						break;
					case 117:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -114:
						break;
					case 118:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -115:
						break;
					case 119:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -116:
						break;
					case 120:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -117:
						break;
					case 121:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -118:
						break;
					case 122:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -119:
						break;
					case 123:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -120:
						break;
					case 124:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -121:
						break;
					case 125:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -122:
						break;
					case 126:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -123:
						break;
					case 127:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -124:
						break;
					case 128:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -125:
						break;
					case 129:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -126:
						break;
					case 130:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -127:
						break;
					case 131:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -128:
						break;
					case 132:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -129:
						break;
					case 133:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -130:
						break;
					case 134:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -131:
						break;
					case 135:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -132:
						break;
					case 136:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -133:
						break;
					case 137:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -134:
						break;
					case 138:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -135:
						break;
					case 139:
						{ return new Symbol(Symbol.IDENT, yytext(), yyline()); }
					case -136:
						break;
					default:
						yy_error(YY_E_INTERNAL,false);
					case -1:
					}
					yy_initial = true;
					yy_state = yy_state_dtrans[yy_lexical_state];
					yy_next_state = YY_NO_STATE;
					yy_last_accept_state = YY_NO_STATE;
					yy_mark_start();
					yy_this_accept = yy_acpt[yy_state];
					if (YY_NOT_ACCEPT != yy_this_accept) {
						yy_last_accept_state = yy_state;
						yy_mark_end();
					}
				}
			}
		}
	}
}
