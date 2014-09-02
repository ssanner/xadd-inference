#!/bin/bash

#Main script for compiling and running CAMDP solvers.
if [[ $# -lt 9 || $# -gt 10 ]]
then
	echo "Main.sh with wrong parameters. Usage: compile? run? plotResults? problemType problem nIter nTrial Display Verbose [PLOT]"
	exit
fi

#Boolean variables to determine which tasks to perform
COMPILE=$1;
RUN=$2;
PLOT=$3;
#String Variables codifing the instance to run
PROBLEMTYPE=$4
PROBLEM=$5
#Integer variables with run parameters
NITER=$6
NTRIAL=$7
DISPLAY=$8
VERBOSE=$9 
PLOTFLAG=${10}

#echo "Testing Main Parameters:"
#echo "ProbType = $PROBLEMTYPE"
#echo "Problem = $PROBLEM"
#echo "Niter = $NITER"
#echo "Ntrial = $NTRIAL"
#echo "Display = $DISPLAY"
#echo "Verb = $VERBOSE"
#echo "Plot debug = $PLOTFLAG"

#Main class to compile and run.
MAIN="camdp/solver/Main"
echo "Main Script Start - $MAIN"

ORI=$(pwd)
SCRIPTDIR="$ORI/scripts"
if [ $COMPILE == 1 ]
then
printf "Compile Start.\n"
$SCRIPTDIR/compile.sh $MAIN
printf "Compile Complete.\n\n"
fi

MAINCLASS=`echo $MAIN | tr '/' '.'`
if [ $RUN == 1 ]
then
	printf "Run Start.\n"
	INSTANCE="src/camdp/ex/$PROBLEMTYPE/$PROBLEM.cmdp"
	SOLVERS="1 3" 
	for i in $SOLVERS
		do
		$SCRIPTDIR/run.sh $MAINCLASS "$INSTANCE $i $NITER $DISPLAY $NTRIAL $VERBOSE $PLOTFLAG"
		done
    printf "Run Complete.\n\n"
fi

if [ $PLOT == 1 ]
then
    printf "Plot Start.\n"
$SCRIPTDIR/plot.sh $PROBLEMTYPE $PROBLEM $DISPLAY $NITER
    printf "Plot Complete.\n\n"
fi

echo "Main Script Finish"