gnuplot << EOF
set terminal postscript eps color enhanced
set size 0.60,0.60
set output "SpaceVsHorizonRoverLinear3.eps"
set xlabel "Horizon" font "Times-Roman, 20"
set ylabel "Number of Nodes"  font "Times-Roman, 20"
set title 'Mars Rover Linear 3' font "Times-Roman, 20" 
set key top right
set key box
set yrange [0:3286] 
set xtic 1
plot "RoverLinear3Prunning.txt" u 2:6t 'Nodes with Pruning' with linespoints lt 1 pointtype 1,\
"RoverLinear3NonPrunning.txt" u 2:6 t 'Nodes without Pruning' with linespoints lt 5 pointtype 5

