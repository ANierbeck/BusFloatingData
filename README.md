# Bus Floating Data #

this project is a simple show case which shows how to create a streaming akka actor to ingest data from the http source to Kafka
And consume those messages either via akka or spark to push them to a cassandra table. 

The project is a multi project with cross compilation due to incompatabilities with kafka spark and akka when using the latest scala akka versions. 

### for a clean build from commandline:

`sbt ";clean ;test; publishLocal"`

or simply call 

`sbt create`

### To run the applications 

**ingest:** 

`sbt ingest/run`

select the 1st entry as the simple does all steps at once
or better 

`sbt runIngest`

to prepare the ingest container call: 

`sbt createIngestContainer`

**akka frontend:**
`so service/run`
`sbt runService`

to prepare the service container: 

`sbt createServerContainer`

**spark digest:**
The spark digest starts a local spark master. 
As a spark job requires a fat jar first create that one: 

`sbt createDigestUberJar`

to run the kafka->cassandra spark job:
`sbt submitKafkaCassandra`

to run the cluster spark job: 
`sbt submitClusterSpark`

## Pre-conditions to run those, have 

a) a cassandra running
b) a zookeeper running
c) a kafka server running

for b) and c) you can use brew to install those. 
e.g. brew cask install zookeeper and brew install kafka

to run zookeper (not as a service)

zkServer start


to run kafka (not as a service)

kafka-server-start /usr/local/etc/kafka/server.properties

### Operating Kafka

to clean old data from kafka turn down the retention time in kafka for the topic

`kafka-configs --zookeeper localhost:2181 --entity-type topics --alter --add-config retention.ms=1000 --entity-name METRO-Vehicles`
 
this will result in a clean up afte about a minute

after that you can configure back the time to about 2 days (more isn't worth when testing)

`kafka-configs --zookeeper localhost:2181 --entity-type topics --alter --add-config retention.ms=172800000 --entity-name METRO-Vehicles`

to check the current setting do the following: 

`kafka-configs --zookeeper localhost:2181 --entity-type topics --describe --entity-name METRO-Vehicles`


### Preparations for running on AWS

See also the terraform folder on how to run this on AWS. 
But prior to using it on AWS with a DC/OS cluster, make sure to 
create all required Docker images and deploy them to a place you have access to. 
To create those artefacts just call

`sbt createAWS`

this will create all uber-jars and runnable Docker images. 


# how to kill a lunatic running framework 

curl -d'frameworkId=frameworkId' master.mesos:5050/master/teardown


# which params to use for flink jobs

## Run the Kafka to Cassandra app

Entry Class: 
de.nierbeck.floating.data.stream.flink.KafkaToCassandraFlinkApp

Programm Arguments:  
METRO-Vehicles node.cassandra.l4lb.thisdcos.directory:9042 broker.kafka.l4lb.thisdcos.directory:9092

## Run the cluster hotspot calculation

Entry Class: 
de.nierbeck.floating.data.stream.flink.CalcClusterFlinkApp

Programm Arguments: 
--connection node.cassandra.l4lb.thisdcos.directory:9042

# Kubernetes

To browse the K8s cluster browse to: 
kube-apiserver-0-instance.kubernetes.mesos:9000/api/v1/namespaces/kube-system/services/kubernetes-dashboard/proxy/
