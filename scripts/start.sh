#!/bin/bash

cmd="${1}"
[ -z "${cmd}" ] && exit 1

DIR="$(cd "$(dirname "$0")" && pwd )"
cd "${DIR}"

pidfile="/var/run/jalbum.pid"

pid=`cat ${pidfile}` 2>/dev/null

running()
{
    [ -z "${pid}" ] && return 1
    kill -0 "${pid}" 2>/dev/null
}

startup()
{
    running
    if [ $? -eq 0 ]
    then 
        echo "already running"
    else
        java -Xms512M -Xmx512M -Xdebug -Xrunjdwp:transport=dt_socket,address=4321,server=y,suspend=n -jar start.jar > log/jstdout.txt 2>&1 &
        ret=$?
        disown $!
        echo $! > "${pidfile}"
        if [ ${ret} -ne 0 ]
        then
            echo "start failed"
        else
            echo "started"        
        fi         
    fi     
}

stopnow()
{
    running
    if [ $? -eq 0 ]
    then
        kill -9 ${pid}
        rm -f "${pidfile}" >/dev/null 2>&1
        echo "stopped"
    else
        echo "not running"
    fi
}

case ${cmd} in
    status)
        running
        if [ $? -eq 0 ]
        then 
            echo "running"
        else
            echo "not running"
            rm -f "${pidfile}" >/dev/null 2>&1
        fi
        ;;
    start)
        startup
        ;;    
    stop)
        stopnow
        ;;
    restart)
        stopnow
        startup
        ;;
esac

exit 0
