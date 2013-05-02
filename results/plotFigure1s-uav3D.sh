#gnuplot plot script
gnuplot << EOF
cd 's-uav'
filename1="uavV4"
set autoscale                          # scale axes automatically
set xtic auto                          # set xtics automatically
set ytic auto                          # set ytics automatically
set xlabel "y"
set ylabel "x"
set zlabel "V" rotate by 90
set ztics 20 
set xtics 30
set ytics 30
set term pdf font "Times-Roman,15"
# mono
set output sprintf("%s.pdf",filename1)
splot sprintf("%s.txt",filename1) t '' w pm3d

EOF
