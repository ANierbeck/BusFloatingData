Bus Floating Data

this project is a simple show case which shows how to create a streaming akka actor to ingest data from the http source to Kafka
And consume those messages either via akka or spark to push them to a cassandra table. 

The project is a multi project with cross compilation due to incompatabilities with kafka spark and akka when using the latest scala akka versions. 

To compile it run: 

`;so test; very publishLocal`
 
To run the applications 

ingest: 

`so ingest/run`

select the 1st entry as the simple does all steps at once


digest akka: 

`so akkaDigest/run`

digest spark: 

`so sparkDigest/run`



Pre-conditions to run those, have 

a) a cassandra running
b) a zookeeper running
c) a kafka server running

for b) and c) you can use brew to install those. 
e.g. brew cask install zookeeper and brew install kafka

to run zookeper (not as a service)

zkServer start


to run kafka (not as a service)

kafka-server-start /usr/local/etc/kafka/server.properties

Operating Kafka

to clean old data from kafka turn down the retention time in kafka for the topic

`kafka-configs --zookeeper localhost:2181 --entity-type topics --alter --add-config retention.ms=1000 --entity-name METRO-Vehicles`
 
this will result in a clean up afte about a minute

after that you can configure back the time to about 2 days (more isn't worth when testing)

`kafka-configs --zookeeper localhost:2181 --entity-type topics --alter --add-config retention.ms=172800000 --entity-name METRO-Vehicles`

to check the current setting do the following: 

`kafka-configs --zookeeper localhost:2181 --entity-type topics --describe --entity-name METRO-Vehicles`

