#!/bin/bash

cmd="${1}"
DIR="$(cd "$(dirname "$0")" && pwd )"
cd "${DIR}"
sf=$(basename $0)
pidfile="/var/run/jalbum.pid"
pid=$(cat ${pidfile} 2>/dev/null)
JAVA_OPTS="${2}"
isDebug="true"

[ ! -z $isDebug ] && JAVA_OPTS="${JAVA_OPTS} -Xdebug -Xrunjdwp:transport=dt_socket,address=4321,server=y,suspend=n"

usage()
{
    echo "Usage: ${sf} [start|stop|status|restart]"
    exit 1
}

running()
{
    [ -z "${pid}" ] && return 1
    $(kill -0 "${pid}" 2>/dev/null)
}

startup()
{
    running
    if [ $? -eq 0 ]
    then 
        echo "already running"
    else
        java -Xms512M -Xmx512M ${JAVA_OPTS} -jar start.jar > log/jstdout.txt 2>&1 &
        ret=$?
        disown $! >/dev/null 2>&1
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
        $(rm -f "${pidfile}" >/dev/null 2>&1)
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
    *)
        usage
        ;;
esac

exit 0
