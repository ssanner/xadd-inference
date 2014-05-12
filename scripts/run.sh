#!/bin/bash
#this runs a java projects with source on src, jars on lib to a bin
#assumes main project folder is .
ORI=$(pwd)

CLASSPATH=$ORI/src
JAR_DIR=$ORI/lib
BIN_DIR=$ORI/bin

cd $JAR_DIR
jars=($(ls *.jar))
JAR_PATH=""
for i in "${jars[@]}"; do
   	JAR_PATH="$JAR_DIR/$i:${JAR_PATH}"
done

cd $CLASSPATH
CLASSPATH=$JAR_PATH$CLASSPATH

cd $ORI
MAINCLASS=$1
PARAMS=$2

echo "Parameters are $PARAMS"
JLP=""
# for USE on LINUX: JLP="-Djava.library.path=/usr/local/lib"
#echo "java -Xmx3000M -cp $BIN_DIR:$CLASSPATH $MAINCLASS $PARAMS"
java -Xmx3500M -cp $BIN_DIR:$CLASSPATH $JLP $MAINCLASS $PARAMS
