#!/bin/bash
dir=`dirname $0`
if [[ "$dir" == "" ]]; then dir="." ; fi
prg=`basename $0`

$dir/prog main.LogRotation "$@"
exit $?

