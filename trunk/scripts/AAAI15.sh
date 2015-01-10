#!/bin/bash

#Script for compiling RTSDP and VI solvers and running on HMDP problems.
if [[ $# != 9 ]]
then
	echo "AAAI15.sh call with wrong parameters. Usage: compile? run? plotResults? problemType problem nIter nTrial DisplayDimension Verbose"
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
SCRIPT_DIR=$ORI/scripts
SRC_DIR=$ORI/src
JAR_DIR=$ORI/lib
BIN_DIR=$ORI/bin

cd $JAR_DIR
jars=($(ls *.jar))
JAR_PATH=""
for i in "${jars[@]}"; do
   	JAR_PATH="$JAR_DIR/$i:${JAR_PATH}"
done
CLASSPATH=$JAR_PATH$SRC_DIR
cd $ORI

if [ $COMPILE == 1 ]
then
printf "Compile Start.\n"
cd $SRC_DIR
MAINJAVA=$MAIN".java"
javac -source 1.6 -target 1.6 -d $BIN_DIR -cp $CLASSPATH $MAINJAVA 2>/dev/null #comment error redirecting to debug compilation
cd $ORI
printf "Compile Complete.\n\n"
fi


if [ $RUN == 1 ]
then
printf "Run Start.\n"

    MAINCLASS=`echo $MAIN | tr '/' '.'`	
	INSTANCE="src/camdp/ex/$PROBLEMTYPE/$PROBLEM.cmdp"
	SOLVERS="1 3" 
	for i in $SOLVERS
		do
        PARAMS="$INSTANCE $i $NITER $DISPLAY $NTRIAL $VERBOSE"
        JLP=""
        MEMLIMIT="-Xmx3000M"
        # for LINUX: JLP="-Djava.library.path=/usr/local/lib"
        printf "Running: java $MEMLIMIT $MAINCLASS $PARAMS \n"
        java $MEMLIMIT -cp $BIN_DIR:$CLASSPATH $JLP $MAINCLASS $PARAMS
        
        printf "\n\n"
		done
    printf "Run Complete.\n\n"
fi

if [ $PLOT == 1 ]
then
    printf "Plot Start.\n"
    #PLOTTER GNUPLOT SCRIPT
    plot="$ORI/scripts/AAAI15.p"
    
    cd $ORI/results/$PROBLEMTYPE/$PROBLEM
    gnuplot -e "filename='$PROBLEM'; plotdim='$DISPLAY';nvalue='$NITER'" $plot

    if [ $DISPLAY -gt 1 ]
    then
        #Change .dot to .pdf
    	SOLVERS="SDP RTSDP" 
    	for i in $(seq 1 $NITER);
        do	
		    for solve in $SOLVERS
    		do
    			#echo "Solver $solve at iter $i."
	       		dot -Tpdf "$solve-Value$i.dot" -o "$PROBLEM-$solve-Value$i-DD.pdf"
            done
    	done
    fi

    DISPLAYWORD=""
    if [ $DISPLAY == 3 ] 
    then
        DISPLAYWORD="three"
    fi

    if [ $DISPLAY == 2 ]
    then
	   DISPLAYWORD="two"
    fi

    #CREATE LaTeX RESULTS FILE
    tex="$ORI/scripts/AAAI15.tex"
    end="/g"
    comm1="s/FILENAME/$PROBLEM$end"
    comm2="s/NITER/$NITER$end"
    comm3="s/DISPLAY/$DISPLAYWORD$end"
    temptex="$ORI/results/$PROBLEMTYPE/$PROBLEM/$PROBLEM.tex"
    sed -e $comm1 -e $comm2 -e $comm3 <$tex >$temptex

    pdflatex $PROBLEM.tex 1>/dev/null #Comment out to debug the latex run.
    printf "Plot Complete.\n\n"
fi

echo "AAAI15 Script Finish"