#!/bin/bash
#this compiles java projects with source on src, jars on lib to a bin
#assumes main project folder is .
ORI=$(pwd)
MAIN=$1

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

MAINJAVA=$MAIN".java"
javac -source 1.6 -target 1.6 -d $BIN_DIR -cp $CLASSPATH $MAINJAVA 2>/dev/null #comment to debug compilation
cd $ORI