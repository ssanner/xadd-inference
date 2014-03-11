#!/bin/bash
#assumes main project folder is .
ORI=$(pwd)

#this could be modified to compile and run diferent files
MAIN="camdp/ApproxTest"

echo "Start - $MAIN"

COMPILE=1
RUN=1
PLOT=1

if [ $# -lt 7 ]
then
	echo "missing parameters, usage: problem Niter Ncurves Step compile? run? plotResults? PlotValue(0 for no plot, 2 for '2D' or 3 for '3D')?"
	exit
fi

EXNAME=$1
ITER=$2
NCURVE=$3
STEP=$4

COMPILE=$5
RUN=$6
PLOT=$7
VALPLOT=$8

mkdir -p results/$EXNAME
PARAMS="src/camdp/ex/$EXNAME.cmdp results/$EXNAME $ITER $NCURVE 0.$STEP $VALPLOT"

CLASSPATH=$ORI/src
JAR_DIR=$ORI/lib
BIN_DIR=$ORI/bin

cd $JAR_DIR
jars=($(ls *.jar))
JAR_PATH=""
for i in "${jars[@]}"; do
   	JAR_PATH="$JAR_DIR/$i:${JAR_PATH}"
done

cd $CLASSPATH
CLASSPATH=$JAR_PATH$CLASSPATH

if [ $COMPILE == 1 ]
then
	echo "Compile Start"
	MAINJAVA=$MAIN".java"

#	find . -name *.class -delete
	#echo "javac -cp "$CLASSPATH" xadd/TestXADD.java"
	javac -d $BIN_DIR -cp $CLASSPATH $MAINJAVA
	echo "Compile Ok"
fi

if [ $RUN == 1 ]
then
	cd $ORI
	MAINCLASS=`echo $MAIN | tr '/' '.'`
	echo "RUNNING: $MAINCLASS $PARAMS"	
	java -Xmx2000M -cp $BIN_DIR:$CLASSPATH $MAINCLASS $PARAMS
	echo "Run Ok"
fi

if [ $PLOT == 1 ]
then
	echo "Plot Start"
	cd $ORI/scripts
	./plots.sh $EXNAME $NCURVE $STEP $ITER $VALPLOT
	echo "plot End"
fi
echo "compilejava Done!"
