FROM zutherb/cassandra-mesos-0.2.0-1

RUN apt-get update && \
    apt-get install -y git && \
    git clone https://github.com/ANierbeck/BusFloatingData /opt/bus-demo

ADD import_data.sh /opt/bus-demo/import_data.sh
ADD import_data.sh /opt/bus-demo/import_data.sh

ENTRYPOINT ["/opt/bus-demo/import_data.sh"]
