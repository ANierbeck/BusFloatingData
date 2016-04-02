FROM java:openjdk-7u95-jre

ENV DOWNLOAD_FILENAME cassandra-mesos-0.2.0-1.tar.gz

RUN apt-get update && \
    apt-get install -y python && \
    mkdir -p /opt/cassandra && \
    wget -O /tmp/$DOWNLOAD_FILENAME https://downloads.mesosphere.com/cassandra-mesos/artifacts/0.2.0-1/$DOWNLOAD_FILENAME && \
    tar xvzf /tmp/$DOWNLOAD_FILENAME -C "/tmp" && \
    tar xvzf /tmp/apache-cassandra-2.1.4-bin.tar.gz --strip-components=1 -C "/opt/cassandra" && \
    rm -vf /tmp/$DOWNLOAD_FILENAME && \
    rm -vf /tmp/apache-cassandra-2.1.4-bin.tar.gz && \
    rm -vf /tmp/cassandra-mesos-executor.jar && \
    rm -vf /tmp/cassandra-mesos-framework.jar

# Expose ports
EXPOSE 7199 7000 7001 9160 9042

WORKDIR /opt/cassandra

CMD ["/opt/cassandra/bin/cassandra", "-f"]