#!/usr/bin/env bash
if [ -z "$1" ];
then
    echo "**************************************************";
    echo "please specify hostname:";
    echo "";
    echo "cassandra-dcos-node.cassandra.dcos.mesos";
    echo "";
    echo "**************************************************";
    exit 1
fi
cd /opt/bus-demo/commons/src/main/resources
HOSTNAME=$1
HOSTPORT=$2
/opt/cassandra/bin/cqlsh $HOSTNAME $HOSTPORT < create_tables.cql
