#!/bin/bash

zkServer start

#kafka-server-start /usr/local/etc/kafka/server.properties
kafka-server-start conf/kafka-server.properties

zkServer stop
