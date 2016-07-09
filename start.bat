@ cd /d %~dp0

java -Xms512M -Xmx512M -Xdebug -Xrunjdwp:transport=dt_socket,address=4321,server=y,suspend=n  -jar start.jar

pause
