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