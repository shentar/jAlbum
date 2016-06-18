@rem cd /d %~dp0

"%JAVA_HOME%/bin/java" -Dinputdir=\\10.10.10.101\root\Ent\ -Dusesqlite=true -jar scan.jar >result.txt

pause
