if [ $# -lt 2 ]
then
	echo "wrong number of files, give input and output"
else
	#echo "ok, here we go"
	awk 'BEGIN{FS="\t";old2="x"} {if( old2 != $2) printf("\n");old2=$2;tmp=$1;$1=$2;$2=tmp;print}' $1 > $2
fi
