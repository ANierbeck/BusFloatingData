#!/usr/bin/env bash
/usr/local/Cellar/kafka/0.10.0.0/bin/kafka-console-consumer --zookeeper leader.mesos:2181/kafka \
                                                            --topic METRO-Vehicles
