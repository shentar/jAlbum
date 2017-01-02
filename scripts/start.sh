#!/bin/bash


DIR="$(cd "$(dirname "$0")" && pwd )"
cd "${DIR}"

(java -Xms512M -Xmx512M -Xdebug -Xrunjdwp:transport=dt_socket,address=4321,server=y,suspend=n -jar start.jar > log/jstdout.txt 2>&1 &)

