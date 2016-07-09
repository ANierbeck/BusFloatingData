#!/usr/bin/env bash
echo "Parameters are: $@"
echo 1 | sbt -mem 2048 clients/run $@
