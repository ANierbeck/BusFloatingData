FROM cassandra:3.0.7

RUN apt-get update && \
    apt-get install -y git && \
    mkdir -p /opt/bus-demo && \
    git clone https://github.com/ANierbeck/BusFloatingData /opt/bus-demo

RUN git -C /opt/bus-demo pull

ADD import_data.sh /opt/bus-demo/import_data.sh

ENTRYPOINT ["/opt/bus-demo/import_data.sh"]
