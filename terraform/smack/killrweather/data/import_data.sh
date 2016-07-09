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
cd /opt/killrweather/data
/opt/cassandra/bin/cqlsh $1 < create-timeseries.cql
/opt/cassandra/bin/cqlsh $1 < load-timeseries.cql