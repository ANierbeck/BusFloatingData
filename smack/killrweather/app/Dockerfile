FROM java:openjdk-8-jdk

ENV SBT_OPTS="-XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=2G -Xmx2048M"

RUN apt-get update && \
    apt-get install -y git && \
    apt-get install curl && \
    wget -O /tmp/sbt.deb https://bintray.com/artifact/download/sbt/debian/sbt-0.13.0.deb && \
    dpkg -i /tmp/sbt.deb && \
    git clone https://github.com/killrweather/killrweather.git /opt/killrweather && \
    cd /opt/killrweather && \
    sbt test package && \
    rm -rf /tmp/sbt.deb

ADD ingest.sh /opt/killrweather/ingest.sh
ADD digest.sh /opt/killrweather/digest.sh

WORKDIR /opt/killrweather
