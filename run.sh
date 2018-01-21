#!/bin/bash
if [ "$(uname)" == "Darwin" ]; then
    JAVA_PATH_SEP=":"
elif [ "$(expr substr $(uname -s) 1 5)" == "Linux" ]; then
    JAVA_PATH_SEP=":"
elif [ "$(expr substr $(uname -s) 1 10)" == "MINGW32_NT" ]; then
    JAVA_PATH_SEP=";"
elif [ "$(expr substr $(uname -s) 1 9)" == "CYGWIN_NT" ]; then
    JAVA_PATH_SEP=";"
fi

java -cp "./build/classes/$JAVA_PATH_SEP./lib/gson-2.6.2.jar" -ea ChatBot.Main db
