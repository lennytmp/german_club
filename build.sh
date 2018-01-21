#!/bin/bash
mkdir -p build/classes
javac *.java -d "build/classes" -classpath "lib/gson-2.6.2.jar"
