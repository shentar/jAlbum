#!/bin/bash


DIR="$(cd "$(dirname "$0")" && pwd )"
cd "${DIR}"

(java -jar start.jar > log/jstdout.txt 2>&1 &)

