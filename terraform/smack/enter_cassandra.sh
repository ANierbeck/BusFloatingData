#!/usr/bin/env bash
CASSANDRA_HOST=node-0.cassandra.mesos
CASSANDRA_IP=$(nslookup $CASSANDRA_HOST | grep Address | awk '{print $2}' | tail -n 1)
docker run -it cassandra:2.2.5 cqlsh $CASSANDRA_IP
