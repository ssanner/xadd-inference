#plots.sh plot graph with multiple curver using plotter.p
if [ $# -lt 2 ]
then
	echo "missing args: PROBLEMTYPE PROBLEM"
	exit
fi

PROBLEMTYPE=$1
PROBLEM=$2
#echo "Problem is $PROBLEMTYPE/$PROBLEM."

ORI=$(pwd)
plot="$ORI/scripts/plot3.p"

#PLOT DATA
cd $ORI/results/$PROBLEMTYPE/$PROBLEM
gnuplot -e "filename='$PROBLEM'" $plot
cd $ORI

#CREATE VIZ LaTeX FILE
tex="$ORI/scripts/template.tex"
end="/g"
comm1="s/FILENAME/$PROBLEM$end"
temptex="$ORI/results/$PROBLEMTYPE/$PROBLEM/$PROBLEM.tex"
sed -e $comm1 <$tex >$temptex
cd $ORI/results/$PROBLEMTYPE/$PROBLEM
pdflatex $PROBLEM.tex >/dev/null
cd $ORI