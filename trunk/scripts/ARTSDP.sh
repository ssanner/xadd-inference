#!/bin/bash

#Deafult values of parameters
COMPILE=""
RUN=""
PLOT=""
PROBLEMTYPE=""
PROBLEM=""
NITER="3"
NTRIAL="3"
DISPLAY="1"
VERBOSE="0"
PLOTFLAG="false"
APPROX="0.0"

#Script for compiling RTSDP and ARTSDP solvers and running on HMDP problems.
while getopts ":crpk:i:h:t:v:a:32" opt; do
    case $opt in
        c) echo "compile ON"; COMPILE="1";;
        r) echo "run ON"; RUN="1";;
        p) echo "plot ON"; PLOT="1";;
        k) echo "problem kind = $OPTARG"; PROBLEMTYPE=$OPTARG;;
        i) echo "problem = $OPTARG"; PROBLEM=$OPTARG;;
        h) echo "horizon = $OPTARG"; NITER=$OPTARG;;
        t) echo "ntrials = $OPTARG"; NTRIAL=$OPTARG;;
        3) echo "3D plot ON"; DISPLAY="3";PLOTFLAG="true";;
        2) echo "2D plot ON"; DISPLAY="2";PLOTFLAG="true";;            
        v) echo "verbose = $OPTARG";VERBOSE=$OPTARG;;
        a) echo "approx = $OPTARG"; APPROX=$OPTARG;;

        \?) echo "Invalid Option: -$OPTARG";
        	echo "ARTSDP.sh call with wrong parameters. Usage: -k PROBLEMKIND -i instance [-c (compile) -r (run) -p (plotResults) -h horizon=3) -t trials(=3) -N dimension (-3 or -2) -v Verbose(=0)  -a ApproximationError(=0.0)"

        exit 1
    esac
done

if [ -z $PROBLEMTYPE ] || [ -z $PROBLEM ] ; then
	echo "ARTSDP.sh call with wrong parameters. Usage: -k PROBLEMKIND -i instance [-c (compile) -r (run) -p (plotResults) -h horizon=3) -t trials(=3) -g Dimension(=2) -v Verbose(=0) -g graph=false -a ApproximationError(=0.0)"
	exit
fi

# Old non optget parameters

#Boolean variables to determine which tasks to perform
#COMPILE=$1;
#RUN=$2;
#PLOT=$3;
#String Variables codifing the instance to run
#DOMAIN=$4
#PROBLEM=$5
#Integer variables with run parameters
#NITER=$6
#NTRIAL=$7
#DISPLAY=$8
#VERBOSE=$9
#Approximation value
#PLOTFLAG = $10
#APPROX = $11

#if [[ $# != 11 ]]
#then
#	echo "ARTSDP.sh call with wrong parameters. Usage: compile? run? plotResults? problemType problem nIter nTrial DisplayDimension Verbose PlotFlag(true or false) ApproximationError"
#	exit
#fi

echo "Testing Main Parameters:"
echo "COMPILE = $COMPILE"
echo "RUN = $RUN"
echo "PLOT = $PLOT"
echo "ProbType = $PROBLEMTYPE"
echo "Problem = $PROBLEM"
echo "Niter = $NITER"
echo "Ntrial = $NTRIAL"
echo "Display = $DISPLAY"
echo "Verb = $VERBOSE"
echo "Plot debug = $PLOTFLAG"
echo "APPROX = $APPROX"

#Main class to compile and run.
MAIN="camdp/solver/Main"
printf "\nMain Script Start - $MAIN\n\n"

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

if [[ -n $COMPILE ]]; then
    printf "Compile Start. COMPILE = $COMPILE.\n"
    cd $SRC_DIR
    MAINJAVA=$MAIN".java"

    #comment error redirecting to debug compilation
    #if [[ $VERBOSE -gt 0 ]] ; then
    #    echo "Compile command: \"javac -source 1.6 -target 1.6 -d $BIN_DIR -cp $CLASSPATH $MAINJAVA 2>/#dev/null\""
    #fi
    javac -source 1.6 -target 1.6 -d $BIN_DIR -cp $CLASSPATH $MAINJAVA 2>/dev/null 
    cd $ORI
    printf "Compile Complete.\n\n"
fi

if [[ -n $RUN ]]; then
    printf "Run Start. RUN = $RUN.\n"

    MAINCLASS=`echo $MAIN | tr '/' '.'`	
	INSTANCE="src/camdp/ex/$PROBLEMTYPE/$PROBLEM.cmdp"
	SOLVERS="1 4 3 5" 
	for i in $SOLVERS
		do
        PARAMS="$INSTANCE $i $NITER $DISPLAY $NTRIAL $VERBOSE $PLOTFLAG $APPROX"
        JLP=""
        MEMLIMIT="-Xmx3000M"
        # for LINUX: JLP="-Djava.library.path=/usr/local/lib"
        #if [[ $VERBOSE -gt 0 ]] ; then
        #    echo "Run command: java $MEMLIMIT -cp $BIN_DIR:$CLASSPATH $JLP $MAINCLASS $PARAMS"
        #fi
        java $MEMLIMIT -cp $BIN_DIR:$CLASSPATH $JLP $MAINCLASS $PARAMS
        printf "\n\n"
		done
    printf "Run Complete.\n\n"
fi

if [[ -n $PLOT ]] ; then
    printf "Plot Start. PLOT = $PLOT.\n"
    #PLOTTER GNUPLOT SCRIPT
    plot="$ORI/scripts/ARTSDP.p"
    
    cd $ORI/results/$PROBLEMTYPE/$PROBLEM
    gnuplot -e "filename='$PROBLEM'; plotdim='$DISPLAY';nvalue='$NITER'" $plot
    
    if [ $DISPLAY -gt 1 ]
    then
        #Change .dot to .pdf
    	SOLVERS="SDP BASDP RTSDP ARTSDP" 
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
    tex="$ORI/scripts/ARTSDP.tex"
    end="/g"
    comm1="s/FILENAME/$PROBLEM$end"
    comm2="s/NITER/$NITER$end"
    comm3="s/DISPLAY/$DISPLAYWORD$end"
    temptex="$ORI/results/$PROBLEMTYPE/$PROBLEM/$PROBLEM.tex"
    sed -e $comm1 -e $comm2 -e $comm3 <$tex >$temptex

    pdflatex $PROBLEM.tex 1>/dev/null #Comment out to debug the latex run.
    printf "Plot Complete.\n\n"
fi

echo "ARTSDP Script Finish"