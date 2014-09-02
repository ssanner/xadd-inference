#plot.sh plot graph with multiple curver using plotter.p
if [[ $# -lt 4 || $# -gt 4 ]]
then
	echo "plot.sh script wrong arguments number. Usage: PROBLEMTYPE PROBLEM PLOTDIM=$3 NVALUE=$4"
	exit
fi

PROBLEMTYPE=$1
PROBLEM=$2
PLOTDIM=$3
NVALUE=$4

#echo "Problem is $PROBLEMTYPE/$PROBLEM, plotDim is $PLOTDIM, nValue is $NVALUE."

ORI=$(pwd)
plot="$ORI/scripts/plot3.p"

#PLOT DATA
cd $ORI/results/$PROBLEMTYPE/$PROBLEM
gnuplot -e "filename='$PROBLEM'; plotdim='$PLOTDIM';nvalue='$NVALUE'" $plot

if [ $PLOTDIM -gt 1 ]
then
	#Change .dot to .pdf
	SOLVERS="SDP RTSDP" 
	for i in $(seq 1 $NVALUE);
	do	
		for solve in $SOLVERS
		do
			#echo "Solver $solve at iter $i."
			dot -Tpdf "$solve-Value$i.dot" -o "$PROBLEM-$solve-Value$i-DD.pdf"
		done
	done
fi

cd $ORI

PLOTDIMWORD=""
if [ $PLOTDIM == 3 ] 
then
PLOTDIMWORD="three"
fi

if [ $PLOTDIM == 2 ]
then
	PLOTDIMWORD="two"
fi

#CREATE VIZ LaTeX FILE
tex="$ORI/scripts/template.tex"
end="/g"
comm1="s/FILENAME/$PROBLEM$end"
comm2="s/NVALUE/$NVALUE$end"
comm3="s/PLOTDIM/$PLOTDIMWORD$end"
temptex="$ORI/results/$PROBLEMTYPE/$PROBLEM/$PROBLEM.tex"
sed -e $comm1 -e $comm2 -e $comm3 <$tex >$temptex
cd $ORI/results/$PROBLEMTYPE/$PROBLEM
pdflatex $PROBLEM.tex 1>/dev/null #Comment out to debug the latex run.
cd $ORI