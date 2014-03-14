#!/bin/bash

COMPILE=$1;
RUN=$2;
PLOT=$3;
#this could be modified to compile and run diferent files
MAIN="camdp/solver/Main"
echo "Script Start - $MAIN"

PROBLEMTYPE=$4
PROBLEM=$5
NITER=$6
NTRIAL=$7
VERBOSE=$8 
if [ $# -lt 8 ]
then
	echo "missing parameters, usage: compile? run? plotResults? problemType problem nIter nTrial Verbose"
	exit
fi


ORI=$(pwd)
SCRIPTDIR="$ORI/scripts"
if [ $COMPILE == 1 ]
then
echo "compile start"
$SCRIPTDIR/compile.sh $MAIN
echo "Compile Done!"
fi


MAINCLASS=`echo $MAIN | tr '/' '.'`
if [ $RUN == 1 ]
then
	echo "run start"
	INSTANCE="src/camdp/ex/$PROBLEMTYPE/$PROBLEM.cmdp"
	SOLVERS="1 3" 
	DISPLAY=0
	for i in $SOLVERS
		do
		$SCRIPTDIR/run.sh $MAINCLASS "$INSTANCE $i $NITER $DISPLAY $NTRIAL 0 $VERBOSE"
		done
echo "Run Ok"
fi


if [ $PLOT == 1 ]
then
echo "plot start"
$SCRIPTDIR/plot.sh $PROBLEMTYPE $PROBLEM
echo "plot finish"
fi

echo "Script Finish"
