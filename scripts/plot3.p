#gnuplot plot script
set autoscale                        # scale axes automatically
set xtic auto                          # set xtics automatically
set ytic auto                          # set ytics automatically

SOLVERS="VI CRTDPFH"
std_width = 2
std_type = 1
pointtypes ="0 1 2 4 6 8 10 12" 

set for [i=1:7] linetype i lt std_type lc i lw std_width pt word(pointtypes,i) ps 2

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

#Nodes
set title sprintf("Nodes per Iter/Trial: %s",filename)
set xlabel "Iterations/Trials"
set ylabel "Nodes"
set output sprintf("%s-Nodes.eps",filename)
plot for [i=1:words(SOLVERS)] sprintf("%s.log",word(SOLVERS,i))\
	u 1:3 title sprintf("%s",word(SOLVERS,i)) ls i+1 w lp

#Val
set title sprintf("Initial State Value: %s",filename)
set xlabel "Iteration"
set ylabel "Value"
set output sprintf("%s-IniVal.eps",filename)
plot for [i=1:words(SOLVERS)] sprintf("%s.log",word(SOLVERS,i))\
	u 1:4 title sprintf("%s",word(SOLVERS,i)) ls i+1 w lp

#Val per Time
set title sprintf("Initial State Value per time: %s",filename)
set xlabel "Cumulated Time(ms)"
set xtics auto
set ylabel "Value"
set output sprintf("%s-ValTime.eps",filename)
plot for [i=1:words(SOLVERS)] sprintf("%s.log",word(SOLVERS,i))\
	u 2:4 title sprintf("%s",word(SOLVERS,i)) ls i+1 w lp

#Val per Nodes
set title sprintf("Initial State Value per Nodes: %s",filename)
set xlabel "Nodes"
set xtics auto
set ylabel "Value"
set output sprintf("%s-ValNodes.eps",filename)
plot for [i=1:words(SOLVERS)] sprintf("%s.log",word(SOLVERS,i))\
	u 3:4 title sprintf("%s",word(SOLVERS,i)) ls i+1 w lp

if (plotdim == 3){
	set palette color	
	do for[h=1:nvalue]{
		do for [i=1:words(SOLVERS)]{
			set title sprintf("%s-%s-Value%d.",filename,word(SOLVERS,i),h)
			set output sprintf("%s-%s-Value%d.eps",filename, word(SOLVERS,i),h)
			splot sprintf("%s-Value%d.txt",word(SOLVERS,i),h) notitle w pm3d
		}
	}
}