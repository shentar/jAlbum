#!/bin/bash

sf=`filename $0`
param=$1
if [ -z "${param}" ]
then
        echo "Usage: sf [start|stop|status|restart]"
        exit 1
fi

pid=0
status=1
pidfile="/var/run/jalbum.pid"
if [ -f ${pidfile} ]
then
    pid=$(cat ${pidfile})
    if [ ! -z "${pid}" ]
    then
        $(kill -0 "${pid}")
        status=$?
    fi
fi

startjAlbum ()
{
    DIR="$(cd "$(dirname "$0")" && pwd )"
    cd "${DIR}"
    java -Xms128M -Xmx512M -Xdebug -Xrunjdwp:transport=dt_socket,address=4321,server=y,suspend=n -Dorg.sqlite.lib.path=./ -Dorg.sqlite.lib.name=libsqlite.so -jar start.jar >./log/jstdout.log 2>&1 &
    echo "$!" >"${pidfile}"
}

stopjAlbum ()
{
    local jpid="$1"
    if [ ! -z "${jpid}" ]
    then
        kill -9 "${jpid}"
    fi
    rm -rf "${pidfile}" >/dev/null 2>&1
}

case "${param}" in
    status)
        if [ ${status} -eq 0 ]
        then    
            echo "running"
        else
            echo "not running"
        fi
    ;;

    start)
        if [ ${status} -eq 0 ]
        then
            echo "is already running"
        else
            (startjAlbum &)
        fi
    ;;
    
    stop)
        if [ ${status} -eq 0 ]
        then
            stopjAlbum "$pid"
        else
            echo "not running"
            rm -rf "${pidfile}" >/dev/null 2>&1
        fi
    ;;
    
    restart)
        if [ ${status} -eq 0 ]
        then
            stopjAlbum "$pid"
        fi    
        (startjAlbum &)
    ;;
    
    *)
        echo "Usage: sf [start|stop|status|restart]"
        exit 1
    ;;    
    
esac
    
exit 0
