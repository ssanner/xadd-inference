#================================================================
# A Makefile for the fodt (first-order decision theory) package
# and subpackages.
#
# Author: Scott Sanner
# $Revision: 1.1 $
#================================================================

# environment settings:
JAVAC   = javac -classpath ../../../..
JAVADOC = javadoc
JAVA    = java

# project specifics:
CLASSES      = \
	KIFParser.class HierarchicalParser.class \
	Yylex.class Symbol.class

# list of subpackages:
PACKAGES     = lexer

# Explain to make how compiling Java code works...
.SUFFIXES :     .class .java
.java.class :
	$(JAVAC) $<

# some other useful macros we can derive...
SRCS = $(CLASSES:.class=.java)
DOCS = $(CLASSES:.class=.html)

# Typical make targets:
all: $(PACKAGES) $(CLASSES)

docs: ALWAYS_BUILD
	-mkdir docs; \
	$(JAVADOC) -author -d docs $(SRCS) $(PACKAGES)

lexer: ALWAYS_BUILD
	-java -classpath ../../../.. jlex.Main kif.lex; \
	mv kif.lex.java Yylex.java; 

clean:
	-rm *.class

test: ALWAYS_BUILD
	echo PARSER TEST RESULTS;
	java  -classpath ../../../..  logic.kb.fol.kif.KIFParser test.kif;

# Packages:


# This is a dummy target that can be used to ensure that a target's
# dependencies are never satisfied.  This is useful because by default, make
# treats the presence of a directory with the same name as the target as an
# up-to-date target.
ALWAYS_BUILD:
