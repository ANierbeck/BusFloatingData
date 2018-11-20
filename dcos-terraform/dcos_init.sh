#!/usr/bin/env bash
function init {
    echo "initializing setup"
    mkdir -p ./mesosphere/dcos-cli/bin/
    mkdir -p ./smack/state
    mkdir -p ./smack/conf
    mkdir -p ./vamp/conf/

    export MASTER_LB_DNS_NAME=$(grep master_external_loadbalancer genconf/config.yaml | sed 's/\master_external_loadbalancer://' | sed 's/^[ \t]*//' )

    echo "Master LB: $MASTER_LB_DNS_NAME"
}

function install_dcos_cli {
    echo "installing cli"
#    curl -s --output /tmp/get-pip.py https://bootstrap.pypa.io/get-pip.py
#    python /tmp/get-pip.py
#    pip install virtualenv
    wget https://downloads.dcos.io/binaries/cli/linux/x86-64/dcos-1.11/dcos -O ./mesosphere/dcos-cli/bin/dcos
    chmod +x ./mesosphere/dcos-cli/bin/dcos

    ./mesosphere/dcos-cli/bin/dcos cluster setup $MASTER_LB_DNS_NAME
}

#function update_dns_nameserver {
#    apt-get install -y jq
#    echo nameserver $(curl -s http://${internal_master_lb_dns_name}:8080/v2/info | jq '.leader' | \
#         sed 's/\"//' | sed 's/\:8080"//') &> /etc/resolvconf/resolv.conf.d/head
#    resolvconf -u
#}

function waited_until_marathon_is_running {
    echo "waiting for marathon"
    until $(curl --output /dev/null --silent --head --fail http://$MASTER_LB_DNS_NAME/); do
        echo "waiting for marathon"
        sleep 5
    done
}

#function waited_until_dns_is_ready {
#    until $(curl --output /dev/null --silent --head --fail http://master.mesos); do
#        echo "waiting for dns"
#        sleep 5
#        update_dns_nameserver
#    done
#}

function waited_until_kafka_is_running {
    until [ -n "$KAFKA_HOST" ] && [ -n "$KAFKA_PORT" ] ; do
        sleep 5
        echo "Kafka is not yet healthy"
        export_kafka_connection
    done
}

function export_kafka_connection {
    export KAFKA_CONNECTION=$(./mesosphere/dcos-cli/bin/dcos kafka --name=kafka endpoints broker | jq .dns[0] | sed -r 's/[\"]+//g' | tr ":" " ")
    export KAFKA_CON_ARR=($KAFKA_CONNECTION)
    export KAFKA_HOST=${KAFKA_CON_ARR[0]}
    echo "KAFKA_HOST: $KAFKA_HOST"
    export KAFKA_PORT=${KAFKA_CON_ARR[1]}
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
    export CASSANDRA_CONNECTION=$(./mesosphere/dcos-cli/bin/dcos cassandra endpoints native-client | jq .dns[0] | sed -r 's/[\"]+//g' | tr ":" " ")
    export CASSANDRA_CON_ARR=($CASSANDRA_CONNECTION)
    export CASSANDRA_HOST=${CASSANDRA_CON_ARR[0]}
    echo "CASSANDRA_HOST: $CASSANDRA_HOST"
    export CASSANDRA_PORT=${CASSANDRA_CON_ARR[1]}
    echo "CASSANDRA_PORT: $CASSANDRA_PORT"
}

function init_cassandra_schema {
    echo "init cassandra schema"
    cat &> ./smack/conf/init_cassandra_schema_job.json << EOF
{
  "id": "init-cassandra-schema-job",
  "description": "Initialize cassandra database",
  "run": {
    "cmd": "./bus-demo/import_data.sh $CASSANDRA_HOST",
    "cpus": 0.1,
    "mem": 256,
    "disk": 0,
    "docker": {
    "image": "anierbeck/bus-demo-schema:0.5.0-SNAPSHOT"
    }
  }
}
EOF
    ./mesosphere/dcos-cli/bin/dcos job add ./smack/conf/init_cassandra_schema_job.json
    ./mesosphere/dcos-cli/bin/dcos job run init-cassandra-schema-job
}

function init_cluster_spark_job {
    echo "init spark job"
    cat &> ./smack/conf/init_cluster_spark_job.json << EOF
{
  "id": "init-cluster-spark-job",
  "description": "Cluster Spark Job",
  "run": {
    "cpus": 0.1,
    "mem": 320,
    "disk": 0,
    "docker": {
      "image": "mesosphere/spark:1.0.9-2.1.0-1-hadoop-2.6"
    },
    "args": ["bin/spark-submit",
             "--conf", "spark.mesos.executor.docker.image=mesosphere/spark:1.0.9-2.1.0-1-hadoop-2.6",
             "--deploy-mode", "cluster",
             "--master", "mesos://leader.mesos/service/spark",
             "--driver-cores", "0.1",
             "--driver-memory", "1G",
             "--total-executor-cores", "4",
             "--class", "de.nierbeck.floating.data.stream.spark.CalcClusterSparkApp",
             "https://oss.sonatype.org/content/repositories/snapshots/de/nierbeck/floating/data/spark-digest_2.11/0.5.0-SNAPSHOT/spark-digest_2.11-0.5.0-SNAPSHOT-assembly.jar",
             "node.cassandra.l4lb.thisdcos.directory:9042"]
  }
}
EOF
    ./mesosphere/dcos-cli/bin/dcos job add ./smack/conf/init_cluster_spark_job.json
    #dcos job run init-cluster-spark-job
}

function init_ingest_app {
    echo "init ingest app"
    cat &> ./smack/conf/ingest.json << EOF
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
      "image": "anierbeck/akka-ingest:0.5.0-SNAPSHOT",
      "network": "HOST",
      "privileged": false,
      "parameters": [],
      "forcePullImage": true
    }
  },
  "env": {
    "CASSANDRA_CONNECT": "$CASSANDRA_HOST:$CASSANDRA_PORT",
    "KAFKA_CONNECT": "$KAFKA_HOST:$KAFKA_PORT"
  }
}
EOF
    ./mesosphere/dcos-cli/bin/dcos marathon app add ./smack/conf/ingest.json
}

function init_ingest_kube_app {
    echo "init ingest kube app"
    cat &> ./smack/conf/ingest_pod_deployment.yaml << EOF
apiVersion: apps/v1beta1
kind: Deployment
metadata:
  name: bus-demo-ingest-deployment
spec:
  replicas: 1
  selector:
    matchLabels:
      name: bus-demo-ingest
  template:
    metadata:
      labels:
        name: bus-demo-ingest
    spec:
     containers:
     - name: bus-demo-ingest
       image: anierbeck/akka-ingest:0.5.0-SNAPSHOT
       env:
       - name: CASSANDRA_CONNECT
         value: "$CASSANDRA_HOST:$CASSANDRA_PORT"
       - name: KAFKA_CONNECT
         value: "$KAFKA_HOST:$KAFKA_PORT"
EOF

    ./kubectl apply -f ./smack/conf/ingest_pod_deployment.yaml

    cat &> ./smack/conf/restart_pod << EOF
kubectl patch deployment $1 -p "{\"spec\":{\"template\":{\"metadata\":{\"labels\":{\"update-timestamp\":\"$(date +%s)\"}}}}}"
EOF

    chmod u+x ./smack/conf/restart_pod

}

function waited_until_spark_is_running {
    echo "waiting for spark"
    until ./mesosphere/dcos-cli/bin/dcos service | grep spark | awk '{print $3};' | grep True; do
        echo "waiting for spark"
        sleep 5
    done
    sleep 10
}


function init_spark_jobs {
    cat &> ./run-pi << EOF
./mesosphere/dcos-cli/bin/dcos spark run --submit-args='--driver-cores 0.1 --driver-memory 1024M --class org.apache.spark.examples.SparkPi https://downloads.mesosphere.com/spark/assets/spark-examples_2.10-1.4.0-SNAPSHOT.jar 10000000'
EOF
    cat &> ./run-digest << EOF
./mesosphere/dcos-cli/bin/dcos spark run --submit-args='--driver-cores 0.1 --driver-memory 1024M --total-executor-cores 4 --class de.nierbeck.floating.data.stream.spark.KafkaToCassandraSparkApp https://oss.sonatype.org/content/repositories/snapshots/de/nierbeck/floating/data/spark-digest_2.11/0.5.0-SNAPSHOT/spark-digest_2.11-0.5.0-SNAPSHOT-assembly.jar METRO-Vehicles $CASSANDRA_HOST:$CASSANDRA_PORT $KAFKA_HOST:$KAFKA_PORT'
EOF
    cat &> ./run-digest-hotspot << EOF
./mesosphere/dcos-cli/bin/dcos spark run --submit-args='--driver-cores 0.1 --driver-memory 1024M --class de.nierbeck.floating.data.stream.spark.CalcClusterSparkApp https://oss.sonatype.org/content/repositories/snapshots/de/nierbeck/floating/data/spark-digest_2.11/0.5.0-SNAPSHOT/spark-digest_2.11-0.5.0-SNAPSHOT-assembly.jar $CASSANDRA_HOST:$CASSANDRA_PORT'
EOF
    chmod 744 ./run-pi ./run-digest ./run-digest-hotspot
    ./run-digest
}

function init_flink_job {
    wget --no-check-certificate \
         --no-cookies \
         "https://oss.sonatype.org/content/repositories/snapshots/de/nierbeck/floating/data/flink-digest_2.11/0.5.0-SNAPSHOT/flink-digest_2.11-0.5.0-SNAPSHOT-assembly.jar" \
         -O /tmp/flink-digest-assembly-0.5.0-SNAPSHOT.jar

    ./mesosphere/dcos-cli/bin/dcos flink upload /tmp/flink-digest-assembly-0.5.0-SNAPSHOT.jar

#    export FILE_NAME=dcos flink jars | jq .files[0].id
#    dcos flink run $FILE_NAME
}

function init_dasboard {
    cat &> ./smack/conf/dashboard.json << EOF
{
    "id": "/bus-demo/dashboard",
    "container": {
        "type": "DOCKER",
        "docker": {
            "image": "anierbeck/akka-server:0.5.0-SNAPSHOT",
            "network": "HOST",
            "forcePullImage": false
        }
    },
    "env": {
    "CASSANDRA_CONNECT": "$CASSANDRA_HOST:$CASSANDRA_PORT",
    "KAFKA_CONNECT": "$KAFKA_HOST:$KAFKA_PORT"
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
    "mem": 2048.0,
    "ports": [
      8000, 8001
    ],
    "requirePorts" : true,
    "labels": {
      "HAPROXY_GROUP": "external",
      "HAPROXY_DEPLOYMENT_GROUP": "busdemo",
      "HAPROXY_0_BACKEND_HTTP_HEALTHCHECK_OPTIONS": "  option  httpchk GET {healthCheckPath} HTTP/1.1\\\\r\\\\nHost:\\\\ www\n  timeout check {healthCheckTimeoutSeconds}s\n",
      "HAPROXY_1_BACKEND_HTTP_HEALTHCHECK_OPTIONS": "  option  httpchk GET {healthCheckPath} HTTP/1.1\\\\r\\\\nHost:\\\\ www\n  timeout check {healthCheckTimeoutSeconds}s\n",
      "HAPROXY_HTTP_FRONTEND_ACL": "  acl host_{cleanedUpHostname} hdr(host) -i {hostname}\\r\\n  use_backend {backend} if host_{cleanedUpHostname}\\r\\n  acl is_websocket hdr(Upgrade) -i WebSocket\\r\\n  use_backend {backend} if is_websocket"
    }
}
EOF
    ./mesosphere/dcos-cli/bin/dcos marathon app add ./smack/conf/dashboard.json
}

function init_dasboard_kube_app {

    cat &> ./smack/conf/dashboard_pod.yaml << EOF
apiVersion: v1
kind: Pod
metadata:
  name: bus-demo-dasboard
  labels:
    app: bus-demo-dashboard-pod
spec:
  containers:
  - name: bus-demo-dashboard
    image: anierbeck/akka-server:0.5.0-SNAPSHOT
    ports:
      - containerPort: 8000
      - containerPort: 8001
    env:
    - name: CASSANDRA_CONNECT
      value: "$CASSANDRA_HOST:$CASSANDRA_PORT"
    - name: KAFKA_CONNECT
      value: "$KAFKA_HOST:$KAFKA_PORT"
    livenessProbe:
      httpGet:
        path: /
        port: 8000
        httpHeaders:
        - name: X-Custom-Header
          value: Awesome
      initialDelaySeconds: 3
      periodSeconds: 3
EOF

    cat &> ./smack/conf/dashboard_service.yaml << EOF
kind: Service
apiVersion: v1
metadata:
  name: bus-demo-service
  labels:
    HAPROXY_GROUP: external
spec:
  selector:
    app: bus-demo-dashboard-pod
  ports:
  - name: http
    protocol: TCP
    port: 8000
    targetPort: 8000
  - name: ws
    protocol: TCP
    port: 8001
    targetPort: 9001
EOF

    kubectl apply -f ./smack/conf/dashboard_pod.yaml
    kubectl apply -f ./smack/conf/dashboard_service.yaml

}


function install_smack {
    ./mesosphere/dcos-cli/bin/dcos package install --yes cassandra
    ./mesosphere/dcos-cli/bin/dcos package install --yes --cli cassandra
    ./mesosphere/dcos-cli/bin/dcos package install --yes kafka
    ./mesosphere/dcos-cli/bin/dcos package install --yes --cli kafka
    ./mesosphere/dcos-cli/bin/dcos package install --yes spark
    ./mesosphere/dcos-cli/bin/dcos package install --yes --cli spark
    #dcos package install --yes zeppelin --package-version=0.6.0
}

function install_marathonLB {
    ./mesosphere/dcos-cli/bin/dcos package install --yes marathon-lb
}


function install_flink {

    cat &> ./smack/conf/flink_options.json << EOF
{
  "service": {
    "name": "flink",
    "slots": 1,
    "parallelism-default": 2,
    "role": "*",
    "user": "root",
    "log-level": "INFO",
    "scala-2-11": true
  },
  "app-master": {
    "cpus": 1,
    "memory": 1024,
    "heap": 256
  },
  "task-managers": {
    "count": 3,
    "cpus": 2,
    "memory": 1024,
    "heap": 1024,
    "memory-preallocation": true
  },
  "security": {
    "kerberos": {
      "use-ticket-cache": true
    },
    "ssl": {
      "enabled": false,
      "enabledAlgorithms": "TLS_RSA_WITH_AES_128_CBC_SHA",
      "enableArtifactServerSSL": false
    }
  },
  "hdfs": {}
}
EOF

    ./mesosphere/dcos-cli/bin/dcos package install --yes --options=./smack/conf/flink_options.json flink

}

function install_kubernetes {

    ./mesosphere/dcos-cli/bin/dcos package install --yes kubernetes

    curl -LO https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/amd64/kubectl
    chmod +x ./kubectl
#    sudo mv ./kubectl /usr/local/bin/kubectl

}

function waited_until_kubernetes_is_running {

    until ./mesosphere/dcos-cli/bin/dcos kubernetes pod list --name="kubernetes" | grep kube-node; do
        echo "waiting for k8s"
        sleep 5
    done

}

function init_kubectl {

    ./kubectl config set-cluster dcos-k8s --server=http://kube-apiserver-0-instance.kubernetes.mesos:9000
    ./kubectl config set-context dcos-k8s --cluster=dcos-k8s --namespace=default
    ./kubectl config use-context dcos-k8s

}

function install_metering {
    ./mesosphere/dcos-cli/bin/dcos package install --yes elastic
}

function install_decanter_monitor {
    connect_string=($(./mesosphere/dcos-cli/bin/dcos cassandra connection | jq ".address[]" | sed 's/\"//g' | awk '{split($0,a,":"); print "("a[1]")"}' | sed -r 's/\./\\\\\./g' | tr '\r\n' '|'))
    export CASSANDRA_CONNCET_LIKE=$${connect_string::-1}

    cat &> ./smack/conf/decanter.json <<EOF
{
  "id": "/decanter",
  "cmd": "export PATH=\$(ls -d \$MESOS_SANDBOX/jdk1*/bin):\$PATH && export JAVA_HOME=\$(ls -d \$MESOS_SANDBOX/jdk1*) && ./Decanter-Runtime-0.1.0-SNAPSHOT/bin/karaf\n",
  "cpus": 0.2,
  "mem": 512,
  "disk": 0,
  "instances": 3,
  "executor": null,
  "fetch": [
    {
      "uri": "https://s3.eu-central-1.amazonaws.com/runtime-packages/jdk-8u102-linux-x64.tar.gz"
    },
    {
      "uri": "https://oss.sonatype.org/content/repositories/snapshots/de/nierbeck/karaf/decanter/Decanter-Runtime/0.1.0-SNAPSHOT/Decanter-Runtime-0.1.0-20161014.113802-11.tar.gz"
    }
  ],
  "constraints": [
    [
      "hostname",
      "UNIQUE",
      ""
    ],
    [
      "hostname",
      "LIKE",
      "$CASSANDRA_CONNCET_LIKE"
    ]
  ],
  "acceptedResourceRoles": null,
  "user": null,
  "container": null,
  "labels": null,
  "healthChecks": [
    {
      "protocol": "COMMAND",
      "command": {
        "value": "pgrep java && exit 0 || exit 1"
      },
      "gracePeriodSeconds": 300,
      "intervalSeconds": 60,
      "timeoutSeconds": 20,
      "maxConsecutiveFailures": 3
    }
  ],
  "env": null,
  "portDefinitions": [
    {
      "protocol": "tcp",
      "port": 0
    }
  ]
}
EOF

    ./mesosphere/dcos-cli/bin/dcos marathon app add ./smack/conf/decanter.json

}

init
#install_oracle_java                 #need for same commandline extension like kafka
waited_until_marathon_is_running
#waited_until_dns_is_ready
install_dcos_cli
install_smack
install_marathonLB
install_kubernetes
waited_until_kafka_is_running
export_kafka_connection
waited_until_cassandra_is_running
export_cassandra_connection
waited_until_spark_is_running
init_cassandra_schema
waited_until_kubernetes_is_running
init_kubectl
init_ingest_kube_app
init_spark_jobs
init_dasboard
init_cluster_spark_job
install_flink
init_flink_job
