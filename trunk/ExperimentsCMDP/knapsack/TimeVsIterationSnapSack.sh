gnuplot << EOF
set terminal postscript eps color enhanced
set size 0.60,0.60
set output "TimeVsHorizonKnap.eps"
set xlabel "Horizon" font "Times-Roman, 20"
set ylabel "Time (ms)"  font "Times-Roman, 20"
set title 'Knapsack' font "Times-Roman, 20" 
set key top right
set yrange [0:100] 
set key box
set xtic 1
plot "Knapsack2Res.txt" u 2:15 t '' with linespoints lt 3 pointtype 7
