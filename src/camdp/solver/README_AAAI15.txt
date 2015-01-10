This is a help file to explain how to run experiments for the AAAI-15.

The main source code is in the xadd-inference/src/camdp/solver pacekage and uses the camdp and xadd packages to represent the Hybrid MDPs.

The main file is Main.java in the solver package. It reads a .cmdp file from src/camdp/ex and creates a CAMDP object, then calls one or more CAMDPSolver (implemented by VI.java or CRTDPFH.java).

The script to run AAAI15 code is the file AAAI15.sh in the xadd-inference/scripts directory.

AAAI15.sh takes 9 command line parameters:
Boolean Compile? (if just compiled use 0 to avoid repeateded compilation)
Boolean Run? (If already ran and just want to plot use 0)
Boolean PlotResults? (If testing compile and run only use 0)
String ProblemType (initialstateContact or initialstateNonlinear)
String InstanceName (inventoryN, trafficN and reservoirN)
Integer NumberOfSDPIterations
Integer NumberOfRTSDPTrials
Integer PlotDimension (2 or 3)
Integer VerboseLevel(Increases amount of debug info displayed, 0 is no debug)

Examples: Compiling the code and then running SDP and RTSDP on inventory2, traffic1 and reservoir1 problems is done with the commands:

./scripts/AAAI15.sh 1 1 1 initialstateContact inventory2 2 20 3 0
./scripts/AAAI15.sh 1 1 1 initialstateNonlinear traffic1 3 5 3 0
./scripts/AAAI15.sh 0 1 1 initialstateNonlinear reservoir2 2 3 3 0

The results are stored in the xadd-inference/results folder, with a subfolder for problem type and instance. There are log files from both solvers as well as numerous comparison pdf images and larger pdf file summarizing all the data from this run.

A better description of the problems used in the experiments is available in the wiki page: https://code.google.com/p/xadd-inference/wiki/RTSDPAAAI2015Problems