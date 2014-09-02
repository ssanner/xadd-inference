
#gnuplot plot script
set autoscale                        # scale axes automatically
set xtic auto                          # set xtics automatically
set ytic auto                          # set ytics automatically

SOLVERS="SDP RTSDP"
std_width = 2
std_type = 1
pointtypes ="0 1 2 4 6 8 10 12" 

#set for [i=1:7] linetype i lt std_type lc i lw std_width pt word(pointtypes,i) ps 1.25
set style line 1 lc rgb '#1b1bFF' pt 8 ps 1 lt 1 lw 2 # --- blue
set style line 2 lc rgb '#8b1a0e' pt 1 ps 1 lt 1 lw 2 # --- red
set style line 3 lc rgb '#5e9c36' pt 6 ps 1 lt 1 lw 2 # --- green

set style line 11 lc rgb '#404040' lt 1
set border 3 back ls 11
set tics nomirror
set style line 12 lc rgb '#A0A0A0' lt 0 lw 1
set grid back ls 12

set term pdfcairo font "sans, 28" color
format="pdf"
set palette model RGB
set offsets graph 0.0, 0.0, 0.02, 0.02

#TotTime
set title sprintf("TotTime: %s",filename)
set xlabel "Iteration/Trials" textcolor ls 11
#set xtics auto
set ylabel "Cumulated Time (s)" textcolor ls 11
#set ytics auto

set output sprintf("%s-TotTime.%s",filename, format)
plot for [i=1:words(SOLVERS)] sprintf("%s.log",word(SOLVERS,i))\
	u 1:2 title sprintf("%s",word(SOLVERS,i)) ls i+1 w lp

#Nodes
set title sprintf("Nodes per Iter/Trial: %s",filename)
set xlabel "Iterations/Trials" textcolor ls 11
set ylabel "Memory (# of nodes)" textcolor ls 11
#set ytics 350
set output sprintf("%s-Nodes.%s",filename, format)
plot for [i=1:words(SOLVERS)] sprintf("%s.log",word(SOLVERS,i))\
	u 1:3 title sprintf("%s",word(SOLVERS,i)) ls i+1 w lp

#Val
set title sprintf("Initial State Value: %s",filename)
set xlabel "Iteration" textcolor ls 11
set ylabel "Value" textcolor ls 11
#set ytics 100
set output sprintf("%s-IniVal.%s",filename, format)
plot for [i=1:words(SOLVERS)] sprintf("%s.log",word(SOLVERS,i))\
	u 1:4 title sprintf("%s",word(SOLVERS,i)) ls i+1 w lp

#Val per Time
set title sprintf("Initial State Value per time: %s",filename)
set xlabel "Cumulated Time (s)" textcolor ls 11
set ylabel "Value" textcolor ls 11
set output sprintf("%s-ValTime.%s",filename, format)
plot for [i=1:words(SOLVERS)] sprintf("%s.log",word(SOLVERS,i))\
	u 2:4 title sprintf("%s",word(SOLVERS,i)) ls i+1 w lp

#Val per Nodes
set title sprintf("Initial State Value per Nodes: %s",filename)
set xlabel "Memory (# of nodes)" textcolor ls 11
#set xtics 4
set ylabel "Value" textcolor ls 11
#set ytics 200
set output sprintf("%s-ValNodes.%s",filename, format)
plot for [i=1:words(SOLVERS)] sprintf("%s.log",word(SOLVERS,i))\
	u 3:4 title sprintf("%s",word(SOLVERS,i)) ls i+1 w lp

#reset
if (plotdim == 3){
	set palette color	
	do for[h=1:nvalue]{
		do for [i=1:words(SOLVERS)]{
		  
    		set xlabel "X1" offset -8
#    		set xtics 30
    		set ylabel "X2" offset 0
#    		set ytics 30
    		set zlabel "Value" offset -3
#            set ztics h*50
			set title sprintf("%s-%s-Value%d.",filename,word(SOLVERS,i),h)
			set output sprintf("%s-%s-Value%d.%s",filename, word(SOLVERS,i),h, format)
			splot sprintf("%s-Value%d.txt",word(SOLVERS,i),h) notitle w pm3d
		}
	}
}

if (plotdim == 2){
	set palette color	
	do for[h=1:nvalue]{
		do for [i=1:words(SOLVERS)]{
		  
    		set xlabel "Var1" offset 0
#    		set xtics 30
    		set ylabel "Value" offset 0
#    		set ytics 30
			set title sprintf("%s-%s-Value%d",filename,word(SOLVERS,i),h)
			set output sprintf("%s-%s-Value%d.%s",filename, word(SOLVERS,i),h, format)
			plot sprintf("%s-Value%d.txt",word(SOLVERS,i),h) notitle w l
		}
	}
}