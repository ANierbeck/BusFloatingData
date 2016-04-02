FROM zutherb/cassandra-mesos-0.2.0-1

RUN apt-get update && \
    apt-get install -y git && \
    git clone https://github.com/killrweather/killrweather.git /opt/killrweather

ADD import_data.sh /opt/killrweather/import_data.sh

ENTRYPOINT ["/opt/killrweather/import_data.sh"]