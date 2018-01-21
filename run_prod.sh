#!/bin/bash

ps auxw | grep ChatBot | grep -v grep > /dev/null

if [ $? != 0 ]
then
	JAVA_PATH_SEP=":"

	cd ~/fight_club_bot
	mkdir -p "db/clients"
	mkdir -p "db/vars"

	java -cp "./build/classes/$JAVA_PATH_SEP./lib/gson-2.6.2.jar" ChatBot.Main db PROD 2>&1 > ~/fight_club_bot_stdout.log

	echo "Respawning ChatBot process..." | /usr/sbin/sendmail lennytmp@gmail.com borodin.vadim@gmail.com
fi
