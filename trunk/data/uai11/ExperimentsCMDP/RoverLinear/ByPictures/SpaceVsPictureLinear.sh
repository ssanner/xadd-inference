gnuplot << EOF
set terminal postscript eps color enhanced
set size 0.60,0.60
set output "SpaceVsPictureLinear.eps"
set xlabel "Number of Pictures" font "Times-Roman, 20"
set ylabel "Number of Nodes"  font "Times-Roman, 20"
set title 'Mars Rover Linear' font "Times-Roman, 20" 
set key top left
set key box
set yrange [0:2000] 
set xtic 1
plot "Iteration1Lin.txt" u 1:6 t 'Horizon 1' with linespoints lt 1 pointtype 1,\
"Iteration2Lin.txt" u 1:6 t 'Horizon 2' with linespoints lt 2 pointtype 2,\
"Iteration3Lin.txt" u 1:6 t 'Horizon 3' with linespoints lt 3 pointtype 3,\
"Iteration4Lin.txt" u 1:6 t 'Horizon 4' with linespoints lt 4 pointtype 4,\
"Iteration5Lin.txt" u 1:6 t 'Horizon 5' with linespoints lt 5 pointtype 5,\
"Iteration6Lin.txt" u 1:6 t 'Horizon 6' with linespoints lt 6 pointtype 6,\
"Iteration7Lin.txt" u 1:6 t 'Horizon 7' with linespoints lt 7 pointtype 7,\
"Iteration8Lin.txt" u 1:6 t 'Horizon 8' with linespoints lt 8 pointtype 8
set 
EOF
