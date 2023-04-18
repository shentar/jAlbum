#!/bin/bash

cmd="${1}"
DIR="$(cd "$(dirname "$0")" && pwd)"
cd "${DIR}" || exit

mkdir /app/config/ >/dev/null 2>&1
if [ ! -f /app/config/jalbum.xml ]; then
  cp jalbum.xml /app/config/jalbum.xml
fi

sh jalbum.sh start
tail -F log/jstdout.txt
