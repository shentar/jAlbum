#!/bin/bash

DIR="$(cd "$(dirname "$0")" && pwd )"
cd "${DIR}../"

git pull

ant -f build_for_Raspberry3.xml

cd distribute
sh jalbum_for_Raspberry3.sh restart


