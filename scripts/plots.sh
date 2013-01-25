#plots.sh plot graph with multiple curver using plotter.p
if [ $# -lt 3 ]
then
	echo "missing args: exp_name, Ncurves, step, Niter"
	exit
fi

name=$1
ncur=$2
step=$3
niter=$4
valueplot=$5

plot="plotter.p"
end="/g"

comm1="s/FILENAME/"$name$end
comm2="s/NCURVES/"$ncur$end
comm3="s/APROX_STEP/"$step$end
comm4="s/MAXITER/"$niter$end
comm5="s/VALUEPLOT/"$valueplot$end

sed -e $comm1 -e $comm2 -e $comm3 -e $comm4 -e $comm5 <$plot >../results/$name/$name$plot 

mv ../src/camdp/ex/results/$name*.txt ../results/$name
cd ../results/$name
gnuplot $name$plot
