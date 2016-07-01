#!/bin/bash

DIR="$(cd "$(dirname "$0")" && pwd )"
cd "${DIR}"
(java -Xms128M -Xmx512M -Xdebug -Xrunjdwp:transport=dt_socket,address=4321,server=y,suspend=n -Dorg.sqlite.lib.path=./ -Dorg.sqlite.lib.name=libsqlite.so -jar start.jar >./log/jstdout.log 2>&1  &)

