#!/bin/bash

DIR="$(cd "$(dirname "$0")" && pwd )"
cd "${DIR}"

cd photoweb
sh build.sh
cd -

cd dedup
ant 
cd -

cd jdbcsqlitenative
make
cd -




