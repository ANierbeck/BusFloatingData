#!/usr/bin/env bash
function init {
    export LANGUAGE=en_US.UTF-8
    export LANG=en_US.UTF-8
    export LC_ALL=en_US.UTF-8
    locale-gen en_US.UTF-8
    dpkg-reconfigure locales

    mkdir -p /opt/mesosphere/dcos-cli
    mkdir -p /opt/smack/state
    mkdir -p /opt/smack/conf
}

function install_dcos_cli {
    touch /opt/smack/state/install_dcos_cli
    curl -s --output /tmp/get-pip.py https://bootstrap.pypa.io/get-pip.py
    python /tmp/get-pip.py
    pip install virtualenv
    curl -s --output /tmp/dcos_cli_install.sh https://downloads.mesosphere.com/dcos-cli/install.sh
    chmod +x /tmp/dcos_cli_install.sh
    yes | /tmp/dcos_cli_install.sh /opt/mesosphere/dcos-cli http://${internal_master_lb_dns_name}
    ln -s /opt/mesosphere/dcos-cli/bin/dcos /usr/local/bin/dcos
    dcos config set core.email johndoe@mesosphere.com
    dcos config set core.token john_doe_token
    dcos config set core.dcos_url http://leader.mesos
    rm /opt/smack/state/install_dcos_cli
}

function install_oracle_java {
    touch /opt/smack/state/install_oracle_java
    wget --no-cookies \
         --no-check-certificate \
         --header "Cookie: gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; oraclelicense=accept-securebackup-cookie" \
         "http://download.oracle.com/otn-pub/java/jdk/8u25-b17/jdk-8u25-linux-x64.tar.gz" \
         -O /tmp/jdk-8u25-linux-x64.tar.gz
    tar xzf /tmp/jdk-8u25-linux-x64.tar.gz --directory=/usr/local/
    update-alternatives --install "/usr/bin/java" "java" "/usr/local/jdk1.8.0_25/bin/java" 1
    update-alternatives --install "/usr/bin/javac" "javac" "/usr/local/jdk1.8.0_25/bin/javac" 1
    update-alternatives --install "/usr/bin/javaws" "javaws" "/usr/local/jdk1.8.0_25/bin/javaws" 1
    update-alternatives --set "java" "/usr/local/jdk1.8.0_25/bin/java"
    update-alternatives --set "javac" "/usr/local/jdk1.8.0_25/bin/javac"
    update-alternatives --set "javaws" "/usr/local/jdk1.8.0_25/bin/javaws"
    rm /opt/smack/state/install_oracle_java
}

function set_dns_nameserver {
    touch /opt/smack/state/set_dns_nameserver
    apt-get install -y jq
    echo nameserver $(curl -s http://${internal_master_lb_dns_name}:8080/v2/info | jq '.leader' | \
         sed 's/\"//' | sed 's/\:8080"//') &> /etc/resolvconf/resolv.conf.d/head
    resolvconf -u
    rm /opt/smack/state/set_dns_nameserver
}

function waited_until_marathon_is_running {
    touch /opt/smack/state/waited_for_marathon_is_running
    until $(curl --output /dev/null --silent --head --fail http://${internal_master_lb_dns_name}:8080/ping); do
        sleep 5
    done
    rm /opt/smack/state/waited_for_marathon_is_running
}

function waited_until_chronos_is_running {
    touch /opt/smack/state/waited_for_chronos_is_running
    until $(curl --output /dev/null --silent --head --fail http://leader.mesos/service/chronos/scheduler/jobs); do
        sleep 5
    done
    rm /opt/smack/state/waited_for_chronos_is_running
}

function waited_until_kafka_is_running {
    touch /opt/smack/state/waited_for_kafka_is_running
    until dcos service | grep kafka | awk '{print $3};' | grep True; do
        sleep 5
    done
    rm /opt/smack/state/waited_for_kafka_is_running
}

function init_cassandra_schema {
    cat > /opt/smack/conf/init_killrweather_cassandra_job.json << EOF
{
    "schedule": "R0/2014-03-08T20:00:00.000Z/PT1M",
    "name": "init_killrweather_cassandra_job",
    "container": {
        "type": "DOCKER",
        "image": "zutherb/mesos-killrweather-data",
        "network": "BRIDGE",
        "forcePullImage": true
    },
    "cpus": "0.5",
    "mem": "512",
    "command": "/opt/killrweather/import_data.sh cassandra-dcos-node.cassandra.dcos.mesos",
    "uris": []
}
EOF
    curl -L -H 'Content-Type: application/json' \
            -X POST -d @/opt/smack/conf/init_killrweather_cassandra_job.json \
            http://leader.mesos/service/chronos/scheduler/iso8601
}

function init_ingest_app {
    cat > /opt/smack/conf/killrweather_ingest.json << EOF
{
    "id": "/ingest",
    "container": {
        "type": "DOCKER",
        "docker": {
            "image": "zutherb/mesos-killrweather-app",
            "network": "HOST",
            "forcePullImage": true
        }
    },
    "cmd": "./ingest.sh -Dcassandra.connection.host=cassandra-dcos-node.cassandra.dcos.mesos -Dkafka.hosts.0=broker-0.kafka.mesos:1025 -Dkafka.zookeeper.connection=leader.mesos",
    "cpus": 1,
    "mem": 2048.0
}
EOF
    dcos marathon app add /opt/smack/conf/killrweather_ingest.json
}

function init_client_app {
    cat > /opt/smack/conf/killrweather_client_app.json << EOF
{
    "id": "/client-app",
    "container": {
        "type": "DOCKER",
        "docker": {
            "image": "zutherb/mesos-killrweather-app",
            "network": "HOST",
            "forcePullImage": true
        }
    },
    "cmd": "./client_app.sh -Dcassandra.connection.host=cassandra-dcos-node.cassandra.dcos.mesos -Dkafka.hosts.0=broker-0.kafka.mesos:1025 -Dkafka.zookeeper.connection=leader.mesos",
    "cpus": 1,
    "mem": 4096.0
}
EOF
    dcos marathon app add /opt/smack/conf/killrweather_client_app.json
}

function init_app {
    cat > /opt/smack/conf/killrweather_app.json << EOF
{
    "id": "/app",
    "container": {
        "type": "DOCKER",
        "docker": {
            "image": "zutherb/mesos-killrweather-app",
            "network": "HOST",
            "forcePullImage": true
        }
    },
    "cmd": "./app.sh -Dcassandra.connection.host=cassandra-dcos-node.cassandra.dcos.mesos -Dkafka.hosts.0=broker-0.kafka.mesos:1025 -Dkafka.zookeeper.connection=leader.mesos",
    "cpus": 1,
    "mem": 4096.0
}
EOF
    dcos marathon app add /opt/smack/conf/killrweather_app.json
}

function install_smack {
    touch /opt/smack/state/waited_for_install_smack
    dcos package install --yes chronos
    dcos package install --yes cassandra
    dcos package install --yes kafka
    dcos package install --yes spark
    waited_until_kafka_is_running
    dcos kafka broker add 0
    dcos kafka broker start 0
    dcos kafka topic add killrweather.raw
    dcos package install --yes zeppelin
    rm /opt/smack/state/waited_for_install_smack
}

function init_complete {
    touch /opt/smack/state/init_complete
}

init
install_oracle_java                 #need for same commandline extension like kafka
waited_until_marathon_is_running
set_dns_nameserver
install_dcos_cli
install_smack
waited_until_chronos_is_running
init_cassandra_schema
init_ingest_app
init_client_app
init_app
init_complete
