#!/bin/bash

DIR="$(cd "$(dirname "$0")" && pwd )"
cd "${DIR}"

bash jalbum.sh $@ " -Dorg.sqlite.lib.path=./ -Dorg.sqlite.lib.name=libsqlite.so "
