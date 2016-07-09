#!/usr/bin/env bash
echo "Parameters are: $@"
echo 2 | sbt -mem 2048 app/run $@
