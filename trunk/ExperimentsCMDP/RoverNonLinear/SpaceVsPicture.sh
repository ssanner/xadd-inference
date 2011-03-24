gnuplot << EOF
set terminal postscript eps color enhanced
set size 0.60,0.60
set output "SpaceVsPictures.eps"
set xlabel "Number of Pictures" font "Times-Roman, 20"
set ylabel "Number of nodes"  font "Times-Roman, 20"
set title 'Mars Rover Nonlinear' font "Times-Roman, 20" 
set key top right
set key box
set yrange [0:5060] 
set xtic 1
plot "Iteration1Non.txt" u 1:6 t 'Horizon 1' with linespoints lt 1 pointtype 5,\
"Iteration2Non.txt" u 1:6 t 'Horizon 2' with linespoints lt 3 pointtype 8,\
"Iteration3Non.txt" u 1:6 t 'Horizon 3' with linespoints lt 5 pointtype 3
set 
EOF
