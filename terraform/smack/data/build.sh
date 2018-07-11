#!/usr/bin/env bash
docker build -t anierbeck/bus-demo-schema .
docker build -t anierbeck/bus-demo-schema:0.5.0-SNAPSHOT .
docker push anierbeck/bus-demo-schema:0.5.0-SNAPSHOT
