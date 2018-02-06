#!/bin/bash

ps auxw | grep FightLang | grep -v grep > /dev/null

if [ $? != 0 ]
then
	JAVA_PATH_SEP=":"

	cd ~/fight_club_bot
	mkdir -p "db/clients"
	mkdir -p "db/vars"

	java -cp "./build/classes/$JAVA_PATH_SEP./lib/gson-2.6.2.jar" FightLang.Main db PROD 2>&1 > ~/fight_club_bot_stdout.log

	echo "Respawning FightLang process..." | /usr/sbin/sendmail lennytmp@gmail.com borodin.vadim@gmail.com
fi
