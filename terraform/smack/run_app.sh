#!/usr/bin/env bash
CASSANDRA_IP=$(nslookup cassandra-dcos-node.cassandra.dcos.mesos | grep Address | awk '{print $2}' | tail -n 1)
ZOOKEEPER_IP=$(nslookup leader.mesos | grep Address | awk '{print $2}' | tail -n 1)
docker run -it zutherb/mesos-killrweather-app ./app.sh -Dspark.cassandra.connection.host=$CASSANDRA_IP -Dkafka.zookeeper.connection=$ZOOKEEPER_IP
