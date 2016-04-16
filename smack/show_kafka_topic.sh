#!/usr/bin/env bash
/usr/local/Cellar/kafka/0.9.0.0/bin/kafka-console-consumer --zookeeper leader.mesos:2181 \
                                                           --topic killrweather.raw \
                                                           --from-beginning