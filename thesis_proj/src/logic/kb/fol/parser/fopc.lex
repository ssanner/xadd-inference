package logic.kb.fol.parser;

import java_cup.runtime.Symbol;

%%
%unicode
%char
%line
%cup
%type Symbol
%implements java_cup.runtime.Scanner
%function next_token
%eofval{  
	return new Symbol(sym.EOF); 
%eofval}
%{
public int yyline() { return yyline; } 
%}
ALPHA=[A-Za-z:]
DIGIT=[0-9]
WHITE_SPACE_CHAR=[\r\n\ \t\b\012]
%%
"true" { return new Symbol(sym.TRUE); }
"false" { return new Symbol(sym.FALSE); }
":" { return new Symbol(sym.COLON); }
";" { return new Symbol(sym.SEMI); }
"!E" { return new Symbol(sym.EXISTS); }
"!A" { return new Symbol(sym.FORALL); }
"!" { return new Symbol(sym.BANG); }
"?" { return new Symbol(sym.QST); }
"+" { return new Symbol(sym.PLUS); }
"*" { return new Symbol(sym.TIMES); }
"(" { return new Symbol(sym.LPAREN); }
")" { return new Symbol(sym.RPAREN); }
"." { return new Symbol(sym.DOT); }
"%" { return new Symbol(sym.MOD); }
"," { return new Symbol(sym.COMMA); }
"[" { return new Symbol(sym.LBRACK); }
"]" { return new Symbol(sym.RBRACK); }
"<=" { return new Symbol(sym.LESSEQ); }
"<" { return new Symbol(sym.LESS); }
">=" { return new Symbol(sym.GREATEREQ); }
">" { return new Symbol(sym.GREATER); }
"^" { return new Symbol(sym.AND); }
"~" { return new Symbol(sym.NOT); }
"|" { return new Symbol(sym.OR); }
"=>" { return new Symbol(sym.IMPLY); }
"<=>" { return new Symbol(sym.EQUIV); }
"~=" { return new Symbol(sym.NEQUAL); }
"=" { return new Symbol(sym.EQUAL); }
"/" { return new Symbol(sym.DIV); }
"-" { return new Symbol(sym.MINUS); }

{ALPHA}("#"|{ALPHA}|{DIGIT}|_|-)*("@"|"'")*"(" { return new Symbol(sym.PFNAME, yytext().substring(0,yytext().length()-1)); }
{ALPHA}("#"|{ALPHA}|{DIGIT}|_|-)*("@"|"'")* { return new Symbol(sym.IDENT, yytext()); }
{DIGIT}+"."{DIGIT}+ { return new Symbol(sym.DOUBLE, new Double(yytext())); }
{DIGIT}+ { return new Symbol(sym.INTEGER, new Integer(yytext())); }
{WHITE_SPACE_CHAR}+ { /* ignore white space. */ }
"#" { return new Symbol(sym.COUNT); }
. { System.err.println("Illegal character: "+yytext()); }
