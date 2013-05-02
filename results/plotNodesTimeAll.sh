gnuplot << EOF
cd 'Results'
std_width = 2
std_type = 1

set term pdf font "Times-Roman,14" 

#set terminal postscript eps color enhanced
set linestyle  1 lt std_type lc 1 lw std_width pt 1
set linestyle  2 lt std_type lc 2 lw std_width pt 2
set linestyle  3 lt std_type lc 3 lw std_width pt 3
set key top right
set key box
set output "Nodes.pdf"

#Nodes

set xlabel "Horizon (h)"
set ylabel "Size of V (Nodes)"

#plot "uni_ring_IP_SimulationP.tex" u 5:2 t 'pairwise basis' with linespoints lt 1 pointtype 5,\


 
plot 's-uav\s-2d-uav-bounded-region-noise_000.log' using 1:2 title 'UAV (6 Cont + 1 Disc)' 	ls 1 with linespoints,\
      's-teles\s-telescopeAction5Linear_000.log' using 1:2 title 'Telescope (3 Cont + 2 Disc)' 	ls 2 with linespoints,\
      's-reservcerto\s-reservoir-SeasonChange8New-1D_000.log' using 1:2 title 'Reservoir (2 Cont + 3 Disc)' 	ls 3 with linespoints

#Time
set xlabel "Horizon (h)"
set ylabel "Log scale Time (ms)"

set output "Time.pdf"

#set yrange [0.000:400000]

set logscale y

plot 's-uav\s-2d-uav-bounded-region-noise_000.log' using 1:6 title 'UAV (6 Cont + 1 Disc)' ls 1 with linespoints,\
      's-teles\s-telescopeAction5Linear_000.log' using 1:6 title 'Telescope (3 Cont + 2 Disc)' 	ls 2 with linespoints,\
      's-reservcerto\s-reservoir-SeasonChange8New-1D_000.log' using 1:6 title 'Reservoir (2 Cont + 3 Disc)' 	ls 3 with linespoints




EOF
