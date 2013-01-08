gnuplot << EOF
set terminal postscript eps color enhanced
set size 0.60,0.60
set output "SpaceVsItSnap.eps"
set xlabel "Horizon" font "Times-Roman, 20"
set ylabel "Number of nodes"  font "Times-Roman, 20"
set title 'Knapsack' font "Times-Roman, 20" 
set key top right
set key box
set xtic 1
plot "Knapsack2Res.txt" u 2:6 t '' with linespoints lt 1 pointtype 1


