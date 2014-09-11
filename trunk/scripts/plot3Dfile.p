#gnuplot plot script
INPUT="src/camdp/ex/initialstateContact/results/i2v1.txt"
OUTPUT="results/initialstateContact/inventory2Value1.pdf"
TITLE="Inventory2 Value 1"
set autoscale                        # scale axes automatically

set term pdfcairo font "sans, 26" color
set output OUTPUT
set title TITLE
set view 45,300,1,1

set xtics 150
set xtics offset -1.5, 0.05
set ytics 150
set ytics offset -0.5,-0.5
set ztics 100
set cbtics 100
#set border 127

set xlabel "x1"
set xlabel offset 0.0,-0.5
set ylabel "x2"
set ylabel offset -0.5,-1
set zlabel "value"
set zlabel offset 0,4

set colorbox vertical user origin 0.82, .5 size .04,.3

#set colorbox origin 0,5 size 0.2,0.5
splot INPUT w pm3d notitle

#Wait for input 
#pause -1