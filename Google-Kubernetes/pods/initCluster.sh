#!/usr/bin/env bash

function init {
    gcloud container clusters get-credentials busfloatingdata --zone europe-west1-b --project linen-age-193411
    #kubectl proxy
}
function install_cassandra {
    kubectl create -f cassandra-service.yaml
    kubectl create -f cassandra-statefulset.yaml
}

function wait_for_cassandra {
    until kubectl get statefulset cassandra | grep cassandra | awk '{print $3};' | grep 1 ; do
        echo "waiting for cassandra"
        sleep 10
    done
}

function init_cassandra {
    kubectl create -f cassandraInitJob.yaml
}

function install_zookeeper {
    kubectl create -f zookeeper.yaml
    #kubectl apply -f https://raw.githubusercontent.com/kubernetes/website/master/docs/tutorials/stateful-application/zookeeper.yaml
}

function install_kafka {
    kubectl create -f kafka.yaml
}

function wait_for_kafka {
    until kubectl get statefulset kafka | grep kafka | awk '{print $3};' | grep 1 ; do
        echo "waiting for kafka"
        sleep 10
    done
}

function install_flink {
    kubectl create -f jobmanager-deployment.yaml
    kubectl create -f taskmanager-deployment.yaml
    kubectl create -f jobmanager-service.yaml
}

function wait_for_flink {
    until [ "kubectl get deployment | grep flink-jobmanager | awk '{print $3};' | grep 1" ] && [ "kubectl get deployment | grep flink-taskmanager | awk '{print $3};' | grep 1" ] ; do
        echo "waiting for flink"
        sleep 10
    done
    echo "Navigate to localhost:8001/api/v1/proxy/namespaces/default/services/flink-jobmanager:8081 for flink Dashboard"
}

function install_ingest {
    kubectl apply -f ingest_pod_deployment.yaml
}

function install_dashboard {
    kubectl apply -f dashboard_deployment.yaml
    kubectl apply -f dashboard_service.yaml
}

function print_end {
    echo "The following needs to be issued to the flink interface: "
    echo "localhost:8001/api/v1/proxy/namespaces/default/services/flink-jobmanager:8081"
    echo "upload the flink digest assembly jar"
    echo "The main class is: de.nierbeck.floating.data.stream.flink.KafkaToCassandraFlinkApp"
    echo "The input is: METRO-Vehicles cassandra:9094 kafka-svc:9092"
}
init
install_cassandra
wait_for_cassandra
init_cassandra
install_zookeeper
install_kafka
wait_for_kafka
install_flink
wait_for_flink
install_ingest
install_dashboard
print_end