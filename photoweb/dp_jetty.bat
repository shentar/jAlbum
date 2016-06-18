

:start
cd /d %~dp0

call "F:\java_web\apache-ant-1.8.2\bin\ant.bat"


copy "%~dp0\dist\root.war" "..\dedup\"


pause 

goto start