#gnuplot plot script
gnuplot << EOF
cd 's-reservcerto'
filename3="reserV7"
set xlabel "l1"
set ylabel "V"
set xtics 1000
set ytics 2000

set term pdf font "Times-Roman,16" 
set output sprintf("%s.pdf",filename3)
plot sprintf("%s.txt",filename3) t '' with lines


EOF
