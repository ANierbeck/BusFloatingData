#!/usr/bin/env bash
function init {
    export LC_ALL=C.UTF-8
    export LANG=C.UTF-8

    echo "export LC_ALL=C.UTF-8" >> ~/.bashrc
    echo "export LANG=C.UTF-8" >> ~/.bashrc

    apt-get install -y language-pack-UTF-8 language-pack-en python3
    ln -s /usr/bin/python3 /usr/bin/python

    curl "https://bootstrap.pypa.io/get-pip.py" -o "/tmp/get-pip.py"
    python /tmp/get-pip.py

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
    yes | /tmp/dcos_cli_install.sh /opt/mesosphere/dcos-cli http://master.mesos
    source /opt/mesosphere/dcos-cli/bin/env-setup
    ln -s /opt/mesosphere/dcos-cli/bin/dcos /usr/sbin/dcos
    dcos config set core.email johndoe@mesosphere.com
    dcos config set core.token john_doe_token
    dcos config set core.dcos_url http://master.mesos
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
    until $(curl --output /dev/null --silent --head --fail http://${internal_master_lb_dns_name}:8080/v2/info); do
        echo "waiting for marathon"
        sleep 5
    done
    rm /opt/smack/state/waited_for_marathon_is_running
}

function waited_until_chronos_is_running {
    touch /opt/smack/state/waited_for_chronos_is_running
    until $(curl --output /dev/null --silent --head --fail http://leader.mesos/service/chronos/scheduler/jobs); do
        echo "waiting for chronos"
        sleep 5
    done
    rm /opt/smack/state/waited_for_chronos_is_running
}

function waited_until_kafka_is_running {
    touch /opt/smack/state/waited_for_kafka_is_running
    until dcos service | grep kafka | awk '{print $3};' | grep True; do
        echo "waiting for kafka"
        sleep 5
    done
    rm /opt/smack/state/waited_for_kafka_is_running
}

function export_kafka_connection {
    export KAFKA_CONNECTION=$(dcos kafka connection | jq .dns[0] | sed -r 's/[\"]+//g' | tr ":" " ")
    IFS=" "
    set -- $KAFKA_CONNECTION
    export KAFKA_HOST=$1
    echo "KAFKA_HOST: $KAFKA_HOST"
    export KAFKA_PORT=$2
    echo "KAFKA_PORT: $KAFKA_PORT"
}

function waited_until_cassandra_is_running {
    touch /opt/smack/state/waited_for_cassandra_is_running
    until dcos service | grep cassandra | awk '{print $3};' | grep True; do
        echo "waiting for cassandra"
        sleep 5
    done
    rm /opt/smack/state/waited_for_cassandra_is_running
}

function export_cassandra_connection {
    export CASSANDRA_CONNECTION=$(dcos cassandra connection | jq .dns[0] | sed -r 's/[\"]+//g' | tr ":" " ")
    IFS=" "
    set -- $CASSANDRA_CONNECTION
    export CASSANDRA_HOST=$1
    echo "CASSANDRA_HOST: $CASSANDRA_HOST"
    export CASSANDRA_PORT=$2
    echo "CASSANDRA_PORT: $CASSANDRA_PORT"
}

function init_cassandra_schema {
    cat > /opt/smack/conf/init_cassandra_schema_job.json << EOF
{
    "schedule": "R0/2014-03-08T20:00:00.000Z/PT1M",
    "name": "init_cassandra_schema_job",
    "container": {
        "type": "DOCKER",
        "image": "zutherb/bus-demo-schema",
        "network": "BRIDGE",
        "forcePullImage": true
    },
    "cpus": "0.5",
    "mem": "512",
    "command": "/opt/bus-demo/import_data.sh $CASSANDRA_HOST",
    "uris": []
}
EOF
    curl -L -H 'Content-Type: application/json' \
            -X POST -d @/opt/smack/conf/init_cassandra_schema_job.json \
            http://leader.mesos/service/chronos/scheduler/iso8601
}

function init_ingest_app {
    cat > /opt/smack/conf/ingest.json << EOF
{
  "id": "/ingest",
  "cpus": 1,
  "mem": 2048,
  "disk": 0,
  "instances": 1,
  "container": {
    "type": "DOCKER",
    "volumes": [],
    "docker": {
      "image": "codecentric/bus-demo-ingest",
      "network": "HOST",
      "privileged": false,
      "parameters": [],
      "forcePullImage": true
    }
  },
  "env": {
    "CASSANDRA_HOST": "$CASSANDRA_HOST",
    "KAFKA_HOST": "$KAFKA_HOST",
    "KAFKA_PORT": "$KAFKA_PORT"
  }
}
EOF
    dcos marathon app add /opt/smack/conf/ingest.json
}

function waited_until_spark_is_running {
    touch /opt/smack/state/waited_for_spark_is_running
    until dcos service | grep spark | awk '{print $3};' | grep True; do
        echo "waiting for spark"
        sleep 5
    done
    rm /opt/smack/state/waited_for_spark_is_running
}


function init_spark_jobs {
    cat > /usr/sbin/run-pi << EOF
dcos spark run --submit-args='-Dspark.mesos.coarse=true --driver-cores 1 --driver-memory 1024M --class org.apache.spark.examples.SparkPi https://downloads.mesosphere.com/spark/assets/spark-examples_2.10-1.4.0-SNAPSHOT.jar 30'
EOF
    cat &> /usr/sbin/run-digest << EOF
dcos spark run --submit-args='--supervise -Dspark.mesos.coarse=true --driver-cores 1 --driver-memory 1024M --class de.nierbeck.floating.data.stream.spark.KafkaToCassandraSparkApp https://s3.eu-central-1.amazonaws.com/codecentric/big-data/bus-demo/bus-demo-digest-assembly-0.1.0.jar METRO-Vehicles $CASSANDRA_HOST $CASSANDRA_PORT $KAFKA_HOST $KAFKA_PORT'
EOF
    chmod 744 /usr/sbin/run-pi /usr/sbin/run-digest
    /usr/sbin/run-digest
}

function init_dasboard {
    cat > /opt/smack/conf/dashboard.json << EOF
{
    "id": "/dashboard",
    "container": {
        "type": "DOCKER",
        "docker": {
            "image": "codecentric/bus-demo-dashboard",
            "network": "HOST",
            "forcePullImage": true
        }
    },
    "acceptedResourceRoles": [
        "slave_public"
    ],
    "env": {
        "CASSANDRA_HOST": "$CASSANDRA_HOST",
        "CASSANDRA_PORT": "9042",
        "KAFKA_HOST": "$KAFKA_HOST",
        "KAFA_PORT" : "9092"
    },
    "healthChecks": [
        {
          "path": "/",
          "protocol": "HTTP",
          "gracePeriodSeconds": 300,
          "intervalSeconds": 60,
          "timeoutSeconds": 20,
          "maxConsecutiveFailures": 3,
          "ignoreHttp1xx": false,
          "port": 8000
        }
    ],
    "cpus": 1,
    "mem": 2048.0,
    "ports": [
        8000
    ]
}
EOF
    dcos marathon app add /opt/smack/conf/dashboard.json
}

function install_smack {
    touch /opt/smack/state/waited_for_install_smack
    dcos package install --yes chronos
    dcos package install --yes cassandra
    dcos package install --yes kafka
    dcos package install --cli kafka
    dcos package install --yes spark
    dcos package install --cli spark
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
waited_until_kafka_is_running
export_kafka_connection
waited_until_cassandra_is_running
export_cassandra_connection
waited_until_chronos_is_running
init_cassandra_schema
init_ingest_app
waited_until_spark_is_running
init_spark_jobs
init_dasboard
init_complete
