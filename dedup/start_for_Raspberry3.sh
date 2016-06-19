#!/bin/bash

DIR="$(cd "$(dirname "$0")" && pwd )"
cd "${DIR}"
(java -Xdebug -Xrunjdwp:transport=dt_socket,address=4321,server=y,suspend=n -Dorg.sqlite.lib.path=../jdbcsqlitenative/ -Dorg.sqlite.lib.name=libsqlite.so -Dinputdir=/media/Ent -Dthreadcount=4 -Dhashalog=MD5 -jar start.jar >./log/jstdout.log 2>&1  &)
