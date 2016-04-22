#!/usr/bin/env bash
CASSANDRA_HOST=cassandra-dcos-node.cassandra.dcos.mesos
CASSANDRA_IP=$(nslookup $CASSANDRA_HOST | grep Address | awk '{print $2}' | tail -n 1)
docker run -it zutherb/cassandra-mesos-0.2.0-1 bin/cqlsh $CASSANDRA_IP