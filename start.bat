@rem cd /d %~dp0

cd dedup

"%JAVA_HOME%/bin/java" -Dinputdir=/ -Dusesqlite=true -jar start.jar >result.txt

pause
