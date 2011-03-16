xadd-inference -- Implementation of Extended ADDs (XADDs) and 
                  XADD-based inference algorithms.

Copyright (C) 2010, Scott Sanner (ssanner [@] gmail.com)

License: MPL 1.1 (with exceptions for 3rd party software & data)

		 The Java interface to LPSolve
		 
		   http://lpsolve.sourceforge.net/5.5/
		   
		 is included for the user's convenience.  LPSolve uses
		 an LGPL license:
		  
		   http://lpsolve.sourceforge.net/5.5/LGPL.htm
		   
		 LPSolve requires binaries appropriate for the user's
		 system, e.g., Windows requires lpsolve55j.dll.  To
		 obtain these binaries, please consult:
		 
		   http://lpsolve.sourceforge.net/5.5/distribution.htm
		   http://sourceforge.net/projects/lpsolve/files/lpsolve/5.5.2.0/
		   

Basic Installation and Invocation
=================================

dd-inference/ provides the following subdirectories:

    src   All source code (.java files)
    bin   All binaries (.class files)
    lib   All 3rd party libraries (.jar files)
    files All supplementary files 

Always ensure that all .jar files in lib/ are included in your
CLASSPATH for both Java compilation and at Java runtime.  We highly
recommend that you use Eclipse for Java development:

    http://www.eclipse.org/downloads/

In Eclipse the CLASSPATH libraries can be set via 

    Project -> Properties -> Java Build Path -> Libraries Tab

For running this code from a terminal, we provide two scripts

    run     For Windows/Cygwin and UNIX/Linux systems
    run.bat For the Windows CMD prompt

You can pass up to 10 arguments to these scripts as required.


Instructions for using Decision Diagrams and Prob. Inference
============================================================

To start with, try to understand XADD.main() by running

  logic.xadd.XADD

and tracing the code.

WARNING: the code is complex, so please feel free to ask questions.
The main thing is to understand the high level structure.

If you think you've got that down, then go onto CMDP.main() by running

  cmdp.CMDP src/cmdp/ex/knapsack.cmdp
  
This is only currently setup for deterministic problems... you'll
probably recognize the code structure and modified file format in

  src/cmdp/ex/knapsack.cmdp


GraphViz Visualization
======================

To enable Java Graphviz visualization:

- Download and install GraphViz on your system:
 
  http://www.graphviz.org/

- Make sure "dot" and "neato" (including ".exe" if running on Windows)
  are in your PATH, i.e., you can execute them from any home directory

Run graph.Graph.main() and verify that a cleanly formatted Java window
displaying a graph appears.  If so then other code which uses the
Graph class should run properly.
