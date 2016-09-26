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
    curl -s --output /tmp/get-pip.py https://bootstrap.pypa.io/get-pip.py
    python /tmp/get-pip.py
    pip install virtualenv
    curl -s --output /tmp/dcos_cli_install.sh https://downloads.mesosphere.com/dcos-cli/install.sh
    chmod +x /tmp/dcos_cli_install.sh
    yes | /tmp/dcos_cli_install.sh /opt/mesosphere/dcos-cli http://master.mesos
    source /opt/mesosphere/dcos-cli/bin/env-setup
    echo "source /opt/mesosphere/dcos-cli/bin/env-setup" >> ~/.bashrc
    ln -s /opt/mesosphere/dcos-cli/bin/dcos /usr/sbin/dcos
    dcos config set core.email johndoe@mesosphere.com
    dcos config core.dcos_acs_token 42
}

function install_oracle_java {
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
    export JAVA_HOME=/usr/local/jdk1.8.0_25/
    echo "export JAVA_HOME=$JAVA_HOME" >> ~/.bashrc
}

function update_dns_nameserver {
    apt-get install -y jq
    echo nameserver $(curl -s http://${internal_master_lb_dns_name}:8080/v2/info | jq '.leader' | \
         sed 's/\"//' | sed 's/\:8080"//') &> /etc/resolvconf/resolv.conf.d/head
    resolvconf -u
}

function waited_until_marathon_is_running {
    until $(curl --output /dev/null --silent --head --fail http://${internal_master_lb_dns_name}:8080/v2/info); do
        echo "waiting for marathon"
        sleep 5
    done
}

function waited_until_dns_is_ready {
    until $(curl --output /dev/null --silent --head --fail http://master.mesos); do
        echo "waiting for dns"
        sleep 5
        update_dns_nameserver
    done
}

function waited_until_chronos_is_running {
    until $(curl --output /dev/null --silent --head --fail http://leader.mesos/service/chronos/scheduler/jobs); do
        echo "waiting for chronos"
        sleep 5
    done
}

function waited_until_kafka_is_running {
    until [ -n "$KAFKA_HOST" ] && [ -n "$KAFKA_PORT" ] ; do
        sleep 5
        echo "Kafka is not yet healthy"
        export_kafka_connection
    done
}

function export_kafka_connection {
    export KAFKA_CONNECTION=($(dcos kafka connection | jq .dns[0] | sed -r 's/[\"]+//g' | tr ":" " "))
    export KAFKA_HOST=$${KAFKA_CONNECTION[0]}
    echo "KAFKA_HOST: $KAFKA_HOST"
    export KAFKA_PORT=$${KAFKA_CONNECTION[1]}
    echo "KAFKA_PORT: $KAFKA_PORT"
}

function waited_until_cassandra_is_running {
    until [ -n "$CASSANDRA_HOST" ] && [ -n "$CASSANDRA_PORT" ] ; do
        sleep 5
        echo "Cassandra is not yet healthy"
        export_cassandra_connection
    done
}

function export_cassandra_connection {
    export CASSANDRA_CONNECTION=($(dcos cassandra connection | jq .dns[0] | sed -r 's/[\"]+//g' | tr ":" " "))
    export CASSANDRA_HOST=$${CASSANDRA_CONNECTION[0]}
    echo "CASSANDRA_HOST: $CASSANDRA_HOST"
    export CASSANDRA_PORT=$${CASSANDRA_CONNECTION[1]}
    echo "CASSANDRA_PORT: $CASSANDRA_PORT"
}

function init_cassandra_schema {
    cat &> /opt/smack/conf/init_cassandra_schema_job.json << EOF
{
    "schedule": "R/2014-03-08T20:00:00Z/PT1M",
    "name": "init_cassandra_schema_job",
    "container": {
        "type": "DOCKER",
        "image": "codecentric/bus-demo-schema:2.2.5",
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

    curl -L -H 'Content-Type: application/json' \
            -X PUT http://leader.mesos/service/chronos/scheduler/job/init_cassandra_schema_job
}

function init_ingest_app {
    cat &> /opt/smack/conf/ingest.json << EOF
{
  "id": "/bus-demo/ingest",
  "cpus": 0.5,
  "mem": 2048,
  "disk": 0,
  "instances": 1,
  "container": {
    "type": "DOCKER",
    "volumes": [],
    "docker": {
      "image": "anierbeck/akka-ingest:0.2.0-SNAPSHOT",
      "network": "HOST",
      "privileged": false,
      "parameters": [],
      "forcePullImage": true
    }
  },
  "env": {
    "CASSANDRA_HOST": "$CASSANDRA_HOST",
    "CASSANDRA_PORT": "$CASSANDRA_PORT",
    "KAFKA_HOST": "$KAFKA_HOST",
    "KAFKA_PORT": "$KAFKA_PORT"
  }
}
EOF
    dcos marathon app add /opt/smack/conf/ingest.json
}

function waited_until_spark_is_running {
    until dcos service | grep spark | awk '{print $3};' | grep True; do
        echo "waiting for spark"
        sleep 5
    done
    update_dns_nameserver
    sleep 10
}


function init_spark_jobs {
    cat &> /usr/sbin/run-pi << EOF
dcos spark run --submit-args='-Dspark.mesos.coarse=true --driver-cores 1 --driver-memory 1024M --class org.apache.spark.examples.SparkPi https://downloads.mesosphere.com/spark/assets/spark-examples_2.10-1.4.0-SNAPSHOT.jar 30'
EOF
    cat &> /usr/sbin/run-digest << EOF
dcos spark run --submit-args='--supervise -Dspark.mesos.coarse=true --driver-cores 1 --driver-memory 1024M --class de.nierbeck.floating.data.stream.spark.KafkaToCassandraSparkApp https://oss.sonatype.org/content/repositories/snapshots/de/nierbeck/floating/data/spark-digest_2.11/0.2.0-SNAPSHOT/spark-digest_2.11-0.2.0-SNAPSHOT-assembly.jar METRO-Vehicles $CASSANDRA_HOST $CASSANDRA_PORT $KAFKA_HOST $KAFKA_PORT'
EOF
    cat &> /usr/sbin/run-digest-hotspot << EOF
dcos spark run --submit-args='-Dspark.mesos.coarse=true --driver-cores 1 --driver-memory 1024M --class de.nierbeck.floating.data.stream.spark.CalcClusterSparkApp https://oss.sonatype.org/content/repositories/snapshots/de/nierbeck/floating/data/spark-digest_2.11/0.2.0-SNAPSHOT/spark-digest_2.11-0.2.0-SNAPSHOT-assembly.jar METRO-Vehicles $CASSANDRA_HOST $CASSANDRA_PORT $KAFKA_HOST $KAFKA_PORT @$'
EOF
    chmod 744 /usr/sbin/run-pi /usr/sbin/run-digest /usr/sbin/run-digest-hotspot
    /usr/sbin/run-digest
}

function init_dasboard {
    cat &> /opt/smack/conf/dashboard.json << EOF
{
    "id": "/bus-demo/dashboard",
    "container": {
        "type": "DOCKER",
        "docker": {
            "image": "anierbeck/akka-server:0.2.0-SNAPSHOT",
            "network": "HOST",
            "forcePullImage": true
        }
    },
    "acceptedResourceRoles": [
        "slave_public"
    ],
    "env": {
        "CASSANDRA_HOST": "$CASSANDRA_HOST",
        "CASSANDRA_PORT": "$CASSANDRA_PORT",
        "KAFKA_HOST": "$KAFKA_HOST",
        "KAFKA_PORT": "$KAFKA_PORT"
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
    dcos package install --yes chronos
    dcos package install --yes cassandra --package-version=1.0.15-3.0.7
    dcos package install --cli cassandra
    dcos package install --yes kafka --package-version=1.1.11-0.10.0.0
    dcos package install --cli kafka
    dcos package install --yes spark --package-version=1.0.1-2.0.0
    dcos package install --cli spark
    dcos package install --yes zeppelin --package-version=0.6.0
}

init
install_oracle_java                 #need for same commandline extension like kafka
waited_until_marathon_is_running
waited_until_dns_is_ready
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
