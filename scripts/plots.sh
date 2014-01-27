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
tex="plottest.tex"
end="/g"

dirname=`echo $name | sed "s/\/.*//g"`
echo "Directory is $dirname."
shortname=`echo $name | sed 's/.*\///'`
echo "File is $shortname."

comm1="s/FILENAME/$shortname$end"
comm2="s/NCURVES/$ncur$end"
comm3="s/APROX_STEP/$step$end"
comm4="s/MAXITER/$niter$end"
comm5="s/VALUEPLOT/$valueplot$end"

sed -e $comm1 -e $comm2 -e $comm3 -e $comm4 -e $comm5 <$plot >../results/$name/$shortname$plot 

temptex="../results/$name/$shortname$tex"
sed -e $comm1  <$tex > tempfile
awkcomm="/%/{if(M<"$niter"){sub("'"%",""'");M=M+1}}{print}"
awk $awkcomm < tempfile > $temptex
rm tempfile

cd ..
home=$(pwd)
#resultsdir="./src/camdp/ex/$dirname/results"
#echo $home $resultsdir
#cd $resultsdir
#Vtext=($(ls $shortname*.txt))
#for file in "${Vtext[@]}"; do
#	$home/scripts/change3d.sh $file $home/results/$name/$file
#done  
mv $home/src/camdp/ex/$dirname/results/$shortname*.txt $home/results/$name
cd $home/results/$name
gnuplot $shortname$plot
