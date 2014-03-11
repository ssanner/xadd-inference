#gnuplot plot script
set autoscale                        # scale axes automatically
set xtic auto                          # set xtics automatically
set ytic auto                          # set ytics automatically

SOLVERS="VI CRTDPFH"
std_width = 2
std_type = 1

set for [i=1:9] linetype i lt std_type lc i lw std_width pt i

set term postscript eps font "Times-Roman,24" color
set palette model RGB
set offsets graph 0.0, 0.0, 0.02, 0.02

#TotTime
set title sprintf("TotTime: %s",filename)
set xlabel "Iteration/Trials"
set xtics 1
set ylabel "Cumulated Time(ms)"

set output sprintf("%s-TotTime.eps",filename)
plot for [i=1:words(SOLVERS)] sprintf("%s.log",word(SOLVERS,i))\
	u 1:2 title sprintf("%s",word(SOLVERS,i)) ls i+1 w lp
#gap=0.1*(GPVAL_DATA_Y_MAX-GPVAL_DATA_Y_MIN)
#set yr [GPVAL_DATA_Y_MIN-gap:GPVAL_DATA_Y_MAX+gap]; replot; set yr restore

#Nodes
set title sprintf("Nodes per Iter/Trial: %s",filename)
set xlabel "Iterations/Trials"
set ylabel "Nodes"
set output sprintf("%s-Nodes.eps",filename)
plot for [i=1:words(SOLVERS)] sprintf("%s.log",word(SOLVERS,i))\
	u 1:3 title sprintf("%s",word(SOLVERS,i)) ls i+1 w lp
#gap=0.1*(GPVAL_DATA_Y_MAX-GPVAL_DATA_Y_MIN)
#set yr [GPVAL_DATA_Y_MIN-gap:GPVAL_DATA_Y_MAX+gap]; replot; set yr restore;

#Val
set title sprintf("Initial State Value: %s",filename)
set xlabel "Iteration"
set ylabel "Value"
set output sprintf("%s-IniVal.eps",filename)
plot for [i=1:words(SOLVERS)] sprintf("%s.log",word(SOLVERS,i))\
	u 1:4 title sprintf("%s",word(SOLVERS,i)) ls i+1 w lp
#gap=0.1*(GPVAL_DATA_Y_MAX-GPVAL_DATA_Y_MIN)
#set yr [GPVAL_DATA_Y_MIN-gap:GPVAL_DATA_Y_MAX+gap]; replot; set yr restore

#Val per Time
set title sprintf("Initial State Value per time: %s",filename)
set xlabel "Cumulated Time(ms)"
set xtics auto
set ylabel "Value"
set output sprintf("%s-ValTime.eps",filename)
plot for [i=1:words(SOLVERS)] sprintf("%s.log",word(SOLVERS,i))\
	u 2:4 title sprintf("%s",word(SOLVERS,i)) ls i+1 w lp
#gap=0.1*(GPVAL_DATA_Y_MAX-GPVAL_DATA_Y_MIN)
#set yr [GPVAL_DATA_Y_MIN-gap:GPVAL_DATA_Y_MAX+gap]; replot; set yr restore