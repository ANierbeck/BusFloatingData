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

    mkdir -p /opt/mesosphere/dcos-cli/bin/
    mkdir -p /opt/smack/state
    mkdir -p /opt/smack/conf
    mkdir -p /opt/vamp/conf/
}

function install_dcos_cli {
    curl -s --output /tmp/get-pip.py https://bootstrap.pypa.io/get-pip.py
    python /tmp/get-pip.py
    pip install virtualenv
    wget https://downloads.dcos.io/binaries/cli/linux/x86-64/dcos-1.8/dcos -O /opt/mesosphere/dcos-cli/bin/dcos
    chmod +x /opt/mesosphere/dcos-cli/bin/dcos
    ln -s /opt/mesosphere/dcos-cli/bin/dcos /usr/sbin/dcos
    dcos config set core.dcos_url http://leader.mesos
    dcos config set core.email johndoe@mesosphere.com
    dcos config set core.dcos_acs_token abc
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
  "id": "init-cassandra-schema-job",
  "description": "Initialize cassandra database",
  "run": {
    "cmd": "/opt/bus-demo/import_data.sh $CASSANDRA_HOST",
    "cpus": 0.1,
    "mem": 256,
    "disk": 0,
    "docker": {
      "image": "codecentric/bus-demo-schema:3.0.7"
    }
  }
}
EOF
    dcos job add /opt/smack/conf/init_cassandra_schema_job.json
    dcos job run init-cassandra-schema-job
}

function init_ingest_app {
    cat &> /opt/smack/conf/ingest.json << EOF
{
  "id": "/bus-demo/ingest",
  "cpus": 0.1,
  "mem": 2048,
  "disk": 0,
  "instances": 1,
  "container": {
    "type": "DOCKER",
    "volumes": [],
    "docker": {
      "image": "codecentric/bus-demo-ingest:0.2.0",
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
  },
  "upgradeStrategy": {
    "minimumHealthCapacity": 0
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
dcos spark run --submit-args='--driver-cores 0.1 --driver-memory 1024M --class org.apache.spark.examples.SparkPi https://downloads.mesosphere.com/spark/assets/spark-examples_2.10-1.4.0-SNAPSHOT.jar 10000000'
EOF
    cat &> /usr/sbin/run-digest << EOF
dcos spark run --submit-args='--driver-cores 0.1 --driver-memory 1024M --class de.nierbeck.floating.data.stream.spark.KafkaToCassandraSparkApp https://s3.eu-central-1.amazonaws.com/big-data-muc/bus-demo-digest-assembly-0.2.0.jar METRO-Vehicles $CASSANDRA_HOST $CASSANDRA_PORT $KAFKA_HOST $KAFKA_PORT'
EOF
    cat &> /usr/sbin/run-digest-hotspot << EOF
dcos spark run --submit-args='--driver-cores 0.1 --driver-memory 1024M --class de.nierbeck.floating.data.stream.spark.CalcClusterSparkApp https://s3.eu-central-1.amazonaws.com/big-data-muc/bus-demo-digest-assembly-0.2.0.jar METRO-Vehicles $CASSANDRA_HOST $CASSANDRA_PORT $KAFKA_HOST $KAFKA_PORT @$'
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
            "image": "codecentric/bus-demo-dashboard:0.2.0",
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
    "upgradeStrategy": {
       "minimumHealthCapacity": 0
    },
    "dependencies": ["/bus-demo/ingest"],
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
    "cpus": 0.1,
    "mem": 2048.0
}
EOF
    dcos marathon app add /opt/smack/conf/dashboard.json
}

function install_smack {
    dcos package install --yes cassandra --package-version=1.0.16-3.0.8
    dcos package install --cli cassandra
    dcos package install --yes kafka --package-version=1.1.9-0.10.0.0
    dcos package install --cli kafka
    dcos package install --yes spark --package-version=1.0.2-2.0.0
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
init_cassandra_schema
init_ingest_app
waited_until_spark_is_running
init_spark_jobs
init_dasboard

