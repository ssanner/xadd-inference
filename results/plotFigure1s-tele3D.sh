#gnuplot plot script
gnuplot << EOF
cd 's-teles'
filename2="telesV4"
set autoscale                          # scale axes automatically
set xtic auto                          # set xtics automatically
set ytic auto                          # set ytics automatically
set zrange [0:]
set xlabel "v"
set ylabel "k" 
set zlabel "V" rotate by 90
set xtics 0.03
set ytics 1
set xtics rotate by 45
set term pdf font "Times-Roman,15" 
#mono
set output sprintf("%s.pdf",filename2)
splot sprintf("%s.txt",filename2) t '' w pm3d

EOF
