package logic.kb.fol.kif;

%%
%unicode
%char
%line
%type Symbol
%function nextToken
%eofval{  
	return new Symbol(Symbol.EOF, yyline()); 
%eofval}
%{
public int yyline() { return yyline; } 
%}
ALPHA=[A-Za-z:_-.%]
DIGIT=[0-9]
WHITE_SPACE_CHAR=[\r\n\ \t\b\012]
QUOTED_STRING=\"[^\"]*\"
SLASH_COMMENT_TEXT=.*[\r\n]
%%
"//"{SLASH_COMMENT_TEXT} { return new Symbol(Symbol.COMMENT, yytext(), yyline()); }
"true" { return new Symbol(Symbol.TRUE, yyline()); }
"TRUE" { return new Symbol(Symbol.TRUE, yyline()); }
"false" { return new Symbol(Symbol.FALSE, yyline()); }
"FALSE" { return new Symbol(Symbol.FALSE, yyline()); }
":" { return new Symbol(Symbol.COLON, yyline()); }
";" { return new Symbol(Symbol.SEMI, yyline()); }
"exists" { return new Symbol(Symbol.EXISTS, yyline()); }
"EXISTS" { return new Symbol(Symbol.EXISTS, yyline()); }
"forall" { return new Symbol(Symbol.FORALL, yyline()); }
"FORALL" { return new Symbol(Symbol.FORALL, yyline()); }
"count" { return new Symbol(Symbol.COUNT, yyline()); }
"COUNT" { return new Symbol(Symbol.COUNT, yyline()); }
"!" { return new Symbol(Symbol.BANG, yyline()); }
"+" { return new Symbol(Symbol.PLUS, yyline()); }
"*" { return new Symbol(Symbol.TIMES, yyline()); }
"(" { return new Symbol(Symbol.LPAREN, yyline()); }
")" { return new Symbol(Symbol.RPAREN, yyline()); }
"." { return new Symbol(Symbol.DOT, yyline()); }
"%" { return new Symbol(Symbol.MOD, yyline()); }
"," { return new Symbol(Symbol.COMMA, yyline()); }
"[" { return new Symbol(Symbol.LBRACK, yyline()); }
"]" { return new Symbol(Symbol.RBRACK, yyline()); }
"<=" { return new Symbol(Symbol.LESSEQ, yyline()); }
"<" { return new Symbol(Symbol.LESS, yyline()); }
">=" { return new Symbol(Symbol.GREATEREQ, yyline()); }
">" { return new Symbol(Symbol.GREATER, yyline()); }
"and" { return new Symbol(Symbol.AND, yyline()); }
"AND" { return new Symbol(Symbol.AND, yyline()); }
"not" { return new Symbol(Symbol.NOT, yyline()); }
"NOT" { return new Symbol(Symbol.NOT, yyline()); }
"or" { return new Symbol(Symbol.OR, yyline()); }
"OR" { return new Symbol(Symbol.OR, yyline()); }
"=>" { return new Symbol(Symbol.IMPLY, yyline()); }
"if" { return new Symbol(Symbol.IMPLY, yyline()); }
"IF" { return new Symbol(Symbol.IMPLY, yyline()); }
"implies" { return new Symbol(Symbol.IMPLY, yyline()); }
"IMPLIES" { return new Symbol(Symbol.IMPLY, yyline()); }
"iff" { return new Symbol(Symbol.EQUIV, yyline()); }
"IFF" { return new Symbol(Symbol.EQUIV, yyline()); }
"<=>" { return new Symbol(Symbol.EQUIV, yyline()); }
"~=" { return new Symbol(Symbol.NEQUAL, yyline()); }
"/=" { return new Symbol(Symbol.NEQUAL, yyline()); }
"=" { return new Symbol(Symbol.EQUAL, yyline()); }
"/" { return new Symbol(Symbol.DIV, yyline()); }
"~" { return new Symbol(Symbol.NOT, yyline()); }

{DIGIT}+"."{DIGIT}+ { return new Symbol(new Double(yytext()), yyline()); }
{DIGIT}+"."{DIGIT}+("e"|"E")("+"|"-")?{DIGIT}+ { return new Symbol(new Double(yytext()), yyline()); }
{DIGIT}+("e"|"E")("+"|"-")?{DIGIT}+ { return new Symbol(new Double(yytext()), yyline()); }
{DIGIT}+ { return new Symbol(new Integer(yytext()), yyline()); }
{QUOTED_STRING} { return new Symbol(Symbol.IDENT, yytext(), yyline()); }
"?"({ALPHA}|{DIGIT}|"."|"_"|"%")+({ALPHA}|{DIGIT}|"#"|"."|"-"|"_"|"%")*("@")* { return new Symbol(Symbol.VARIABLE, yytext().substring(1), yyline()); }
({ALPHA}|{DIGIT}|"."|"_"|"%")+({ALPHA}|{DIGIT}|"#"|"."|"-"|"_"|"%")*("@")* { return new Symbol(Symbol.IDENT, yytext(), yyline()); }
{WHITE_SPACE_CHAR}+ { /* ignore white space. */ }
"#" { return new Symbol(Symbol.COUNT, yyline()); }
"-" { return new Symbol(Symbol.MINUS, yyline()); }
"?" { return new Symbol(Symbol.QST, yyline()); }
. { System.err.println("Illegal character: "+yytext()+":ln:" + yyline()); }
