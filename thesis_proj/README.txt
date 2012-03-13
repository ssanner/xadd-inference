xadd-inference -- Implementation of Extended ADDs (XADDs) and 
                  XADD-based inference algorithms.

Copyright (C) 2010, Scott Sanner (ssanner [@] gmail.com)
                    Karina Valdivia Delgado (karinaval [@] gmail.com)

License: MPL 1.1 (with exceptions for 3rd party software & data)

		 The Java interface to LPSolve
		 
		   http://lpsolve.sourceforge.net/5.5/
		   
		 is included for the user's convenience.  LPSolve 
		 uses an LGPL license:
		  
		   http://lpsolve.sourceforge.net/5.5/LGPL.htm

		 Also includes a 2D Java plotting package in PlotPackage.jar available here:
		 
		   http://homepage.mac.com/jhuwaldt/java/Packages/Plot/PlotPackage.html
		   

Basic Installation and Invocation
=================================

xadd-inference/ provides the following subdirectories:

    src   All source code (.java files)
    bin   All binaries (.class files)
    lib   All 3rd party libraries (.jar files)
    files All supplementary files 

Always ensure that all .jar files in lib/ are included in your
CLASSPATH for both Java compilation and at Java runtime.  

We highly recommend that you use Eclipse for Java development:

    http://www.eclipse.org/downloads/

In Eclipse the CLASSPATH libraries can be set via 

    Project -> Properties -> Java Build Path -> Libraries Tab

For running this code from a terminal, we provide two scripts

    run     For Windows/Cygwin and UNIX/Linux systems
    run.bat For the Windows CMD prompt

You can pass up to 10 arguments to these scripts as required.


Instructions for using Decision Diagrams and Prob. Inference
============================================================

**LPSolve and GraphViz should be installed on your system.  See 
below for instructions on installing each.

To start with, try to understand XADD.main() by running (using
the launch scripts ./run or run.bat)

  xadd.XADD

and tracing the code.

The main thing is to understand the high level structure.

If you think you've got that down, then go onto CMDP.main() by 
running

  cmdp.CMDP src/cmdp/ex/knapsackM.cmdp 2
  
This is only currently setup for deterministic problems... note 
the code structure and modified file format in

  src/cmdp/ex/knapsackM.cmdp


Installing LPSolve
==================

LPSolve requires binaries appropriate for the user's system, 
e.g., Windows requires lpsolve55j.dll.  To obtain these binaries, 
please consult:
		 
  http://lpsolve.sourceforge.net/5.5/distribution.htm
  http://sourceforge.net/projects/lpsolve/files/lpsolve/5.5.2.0/

To verify it is installed correctly, run

  lpsolve.LP

and ensure that there are no errors (LPSolve provides verbose
output about it's solution, but these are not errors).


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
