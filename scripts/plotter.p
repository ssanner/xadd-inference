#gnuplot plot script
set autoscale                        # scale axes automatically
set xtic auto                          # set xtics automatically
set ytic auto                          # set ytics automatically

filename="FILENAME"
Ncur=NCURVES
step=int("APROX_STEP")
Niter=MAXITER
valplot=VALUEPLOT

std_width = 2
std_type = 1

set linestyle  1 lt std_type lc 1 lw std_width pt 1
set linestyle  2 lt std_type lc 2 lw std_width pt 2
set linestyle  3 lt std_type lc 3 lw std_width pt 3
set linestyle  4 lt std_type lc 4 lw std_width pt 4
set linestyle  5 lt std_type lc 5 lw std_width pt 5
set linestyle  6 lt std_type lc 6 lw std_width pt 6
set linestyle  7 lt std_type lc 7 lw std_width pt 7
set linestyle  8 lt std_type lc 8 lw std_width pt 8
set linestyle  9 lt std_type lc 9 lw std_width pt 9
set linestyle  10 lt std_type lc rgb "#33AA33" lw std_width pt 10
set linestyle  11 lt std_type lc rgb "purple" lw std_width pt 11

set term postscript eps font "Times-Roman,12" color
set palette model RGB

f(x) = step*x
if ( 0>1){
	Ncur=3
	f(x)=(x==0?0:x==1?3:x==2?6:9)
}


#Value
if (valplot > 0) {
	do for [V=1:Niter] {
		set title sprintf("V%d : %s",V,filename)
		set xlabel "x"
		set ylabel "Value"

		set output sprintf("%s-V%d.eps",filename,V)

		FileI = sprintf("%s.cmdp.V_%d-%03d.txt", filename,V,000)
		isexist= system (sprintf("file %s | grep 'cannot open'",FileI)) 
 		#print (FileI)
		#print(isexist)
		if ( strlen(isexist) == 0 ) {
		
			if ( valplot == 2) {
				plot for [eps=0:Ncur]\
					 (ap = f(eps), \
					 sprintf("%s.cmdp.V_%d-%03d.txt",filename,V,ap))\
				using 1:2 title sprintf("eps = 0.%03d",ap) \
				ls eps+1 w l
			} else { if (valplot == 3) {
				set palette color
				splot for [eps=0:Ncur]\
					(ap = f(eps), \
					sprintf("%s.cmdp.V_%d-%03d.txt",filename,V,ap))\
					using 1:2:3 title sprintf("eps = 0.%03d",ap)\
				w pm3d
			} else {print("Wrong display val number")}
			}	
		}
	}
}

#Nodes
set title sprintf("Nodes: %s",filename)
set xlabel "Iteration"
set ylabel "Nodes"

set output sprintf("%s-Nodes.eps",filename)

plot for [eps=0:Ncur] (ap = f(eps), sprintf("%s_%03d.log",filename,ap))\
	using 1:2 title sprintf("eps = 0.%03d",ap) \
	ls eps+1 with linespoints

#Leaves
set title sprintf("Leaves: %s",filename)
set xlabel "Iteration"
set ylabel "Leaves"

set output sprintf("%s-Leaves.eps",filename)

plot for [eps=0:Ncur] (ap = f(eps), sprintf("%s_%03d.log",filename,ap))\
	using 1:3 title sprintf("eps = 0.%03d",ap) \
	ls eps+1 with linespoints

#Branches
set title sprintf("Branches: %s",filename)
set xlabel "Iteration"
set ylabel "Branches"

set output sprintf("%s-Branches.eps",filename)

plot for [eps=0:Ncur] (ap = f(eps), sprintf("%s_%03d.log",filename,ap))\
	using 1:4 title sprintf("eps = 0.%03d",ap) \
	ls eps+1 with linespoints

#Memory
set title sprintf("Memory: %s",filename)
set xlabel "Iteration"
set ylabel "Memory Used(MB)"

set output sprintf("%s-Memory.eps",filename)

plot for [eps=0:Ncur] (ap = f(eps), sprintf("%s_%03d.log",filename,ap))\
	using 1:5 title sprintf("eps = 0.%03d",ap) \
	ls eps+1 with linespoints

#Time
set title sprintf("Time: %s",filename)
set xlabel "Iteration"
set ylabel "Time Per Iteration"

set output sprintf("%s-Time.eps",filename)

plot for [eps=0:Ncur] (ap = f(eps), sprintf("%s_%03d.log",filename,ap))\
	using 1:6 title sprintf("eps = 0.%03d",ap) \
	ls eps+1 with linespoints

#TotTime
set title sprintf("TotTime: %s",filename)
set xlabel "Iteration"
set ylabel "Cumulated Time"

set output sprintf("%s-TotTime.eps",filename)

plot for [eps=0:Ncur] (ap = f(eps), sprintf("%s_%03d.log",filename,ap))\
	using 1:7 title sprintf("eps = 0.%03d",ap) \
	ls eps+1 with linespoints

#Mav Val
set title sprintf("Max Val: %s",filename)
set xlabel "Iteration"
set ylabel "Maximum of Value Function"

set output sprintf("%s-MaxVal.eps",filename)

plot for [eps=0:Ncur] (ap = f(eps), sprintf("%s_%03d.log",filename,ap))\
	using 1:8 title sprintf("eps = 0.%03d",ap) \
	ls eps+1 with linespoints

#MaxErr
set title sprintf("MaxErr: %s",filename)
set xlabel "Iteration"
set ylabel "Maximum Relative Error"

set output sprintf("%s-MaxErr.eps",filename)

plot for [eps=1:Ncur] (ap = f(eps), sprintf("%s_%03d.log",filename,ap))\
	using 1:9 title sprintf("eps = 0.%03d",ap) \
	ls eps+1 with linespoints
