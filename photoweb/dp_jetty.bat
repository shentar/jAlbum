:start
cd /d %~dp0

ant.bat
copy "%~dp0\dist\root.war" "..\dedup\"

pause 

goto start
