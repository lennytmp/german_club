#!/bin/bash
mkdir -p build/classes
javac *.java -d "build/classes" -classpath "lib/gson-2.8.6.jar"

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

java -cp "./build/classes/$JAVA_PATH_SEP./lib/gson-2.8.6.jar" -ea FightLang.ClientTest
java -cp "./build/classes/$JAVA_PATH_SEP./lib/gson-2.8.6.jar" -ea FightLang.CoreGameTest
java -cp "./build/classes/$JAVA_PATH_SEP./lib/gson-2.8.6.jar" -ea FightLang.BattleFlowTest
java -cp "./build/classes/$JAVA_PATH_SEP./lib/gson-2.8.6.jar" -ea FightLang.VictoryMessageTest
java -cp "./build/classes/$JAVA_PATH_SEP./lib/gson-2.8.6.jar" -ea FightLang.GeminiJsonTest
java -cp "./build/classes/$JAVA_PATH_SEP./lib/gson-2.8.6.jar" -ea FightLang.PotionEffectsTest
echo
