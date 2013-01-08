gnuplot << EOF
set terminal postscript eps color enhanced
set size 0.60,0.60
set output "TimeVsHorizonRoverLinear3.eps"
set xlabel "Horizon" font "Times-Roman, 20"
set ylabel "Time (ms)"  font "Times-Roman, 20"
set title 'Mars Rover Linear 3' font "Times-Roman, 20" 
set key top left
set key box
set yrange [0:108000] 
set xtic 1
plot "RoverLinear3Prunning.txt" u 2:15 t 'Time with Pruning' with linespoints lt 1 pointtype 5,\
"RoverLinear3NonPrunning.txt" u 2:15 t 'Time without Pruning' with linespoints lt 5 pointtype 3
set 
EOF
