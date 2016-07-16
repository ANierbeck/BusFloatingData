#!/usr/bin/env bash
/usr/local/Cellar/kafka/0.10.0.0/bin/kafka-console-consumer --zookeeper master.mesos:2181/kafka \
                                                            --topic killrweather.raw
