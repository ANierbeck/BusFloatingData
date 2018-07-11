#!/bin/bash

zkServer start

sleep 15

#kafka-server-start /usr/local/etc/kafka/server.properties
kafka-server-start conf/kafka-server.properties

zkServer stop
