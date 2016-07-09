#!/usr/bin/env bash
CASSANDRA_IP=$(nslookup cassandra-dcos-node.cassandra.dcos.mesos | grep Address | awk '{print $2}' | tail -n 1)
KAFKA_IP=$(nslookup broker-0.kafka.mesos | grep Address | awk '{print $2}' | tail -n 1)
ZOOKEEPER_IP=$(nslookup leader.mesos | grep Address | awk '{print $2}' | tail -n 1)
docker run -it zutherb/mesos-killrweather-app ./client_app.sh -Dcassandra.connection.host=$CASSANDRA_IP -Dkafka.hosts.0=$KAFKA_IP:1025
