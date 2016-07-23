FROM cassandra:2.2.5

RUN apt-get update && \
    apt-get install -y git && \
    mkdir -p /opt/bus-demo && \
    git clone https://github.com/ANierbeck/BusFloatingData /opt/bus-demo

ADD import_data.sh /opt/bus-demo/import_data.sh

ENTRYPOINT ["/opt/bus-demo/import_data.sh"]
