/*
 * Copyright 2016 Achim Nierbeck
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.nierbeck.floating.data.stream.flink

import java.util.{Optional, Properties}

import com.datastax.driver.core.Cluster
import com.datastax.driver.core.Cluster.Builder
import de.nierbeck.floating.data.domain.{TiledVehicle, Vehicle}
import de.nierbeck.floating.data.tiler.TileCalc
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.connectors.cassandra.{CassandraSink, ClusterBuilder}
import org.apache.flink.streaming.connectors.kafka.partitioner.FixedPartitioner
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer010, FlinkKafkaProducer010}
import org.apache.flink.streaming.util.serialization.{KeyedSerializationSchemaWrapper, SerializationSchema}

object KafkaToCassandraFlinkApp {


  def main(args: Array[String]) {

    assert(args.size == 3, "Please provide the following params: topicname cassandrahost:cassandraport kafkahost:kafkaport")

    val kafkaConnect = args(2)
    val cassandraHost = args(1).split(":").head
    val cassandraPort = args(1).split(":").reverse.head
    val consumerTopic = args(0)

    import org.apache.flink.streaming.api.scala._

    implicit val typeInfo = createTypeInformation[Vehicle]
    implicit val typeInfoPojo = createTypeInformation[VehiclePojo]
    implicit val typeInfoTiledPojo = createTypeInformation[TiledVehiclePojo]
    implicit val typeInfoTiled = createTypeInformation[TiledVehicle]

    val env = StreamExecutionEnvironment.getExecutionEnvironment

    val properties:Properties = new Properties()
    properties.setProperty("bootstrap.servers", kafkaConnect)
    properties.setProperty("group.id", "flink")

    val kafkaConsumer = new FlinkKafkaConsumer010[Vehicle](consumerTopic, new VehicleFstDeserializationSchema(), properties)

    println("Yeah wir leben noch")

    val vehicleStream = env.addSource(kafkaConsumer)

    val pojoStream: DataStream[VehiclePojo] = vehicleStream.map(scalaVehicle => {
      println(s"received a vehicle with id ${scalaVehicle.id}")
      val pojo = new VehiclePojo()
      pojo.setId(scalaVehicle.id)
      pojo.setHeading(scalaVehicle.heading)
      pojo.setLatitude(scalaVehicle.latitude)
      pojo.setLongitude(scalaVehicle.longitude)
      if (scalaVehicle.route_id.isDefined) {
        pojo.setRoute_id(scalaVehicle.route_id.get)
      }
      pojo.setRun_id(scalaVehicle.run_id)
      pojo.setSeconds_since_report(scalaVehicle.seconds_since_report)
      if (scalaVehicle.time.isDefined) {
        pojo.setTime(scalaVehicle.time.get)
      }
      pojo
    })

    CassandraSink.addSink(pojoStream.javaStream)
      .setClusterBuilder(new ClusterBuilder {
        override def buildCluster(builder: Cluster.Builder) = builder.addContactPoint(cassandraHost).build()
      }).build()

    val tiledVehicleStream:DataStream[TiledVehicle] = vehicleStream.filter(x => x.time.isDefined).map(vehicle => TiledVehicle(
      TileCalc.convertLatLongToQuadKey(vehicle.latitude, vehicle.longitude),
      TileCalc.transformTime(vehicle.time.get),
      vehicle.id,
      vehicle.time,
      vehicle.latitude,
      vehicle.longitude,
      vehicle.heading,
      vehicle.route_id,
      vehicle.run_id,
      vehicle.seconds_since_report
    ))


    val tiledVehiclePojoStream:DataStream[TiledVehiclePojo] = tiledVehicleStream.map(tiledVehicle => {
      val pojo = new TiledVehiclePojo()
      pojo.setTileId(tiledVehicle.tileId)
      pojo.setTimeId(tiledVehicle.timeID.getTime)

      pojo.setId(tiledVehicle.id)
      pojo.setHeading(tiledVehicle.heading)
      pojo.setLatitude(tiledVehicle.latitude)
      pojo.setLongitude(tiledVehicle.longitude)
      if (tiledVehicle.route_id.isDefined) {
        pojo.setRoute_id(tiledVehicle.route_id.get)
      }
      pojo.setRun_id(tiledVehicle.run_id)
      pojo.setSeconds_since_report(tiledVehicle.seconds_since_report)
      if (tiledVehicle.time.isDefined) {
        pojo.setTime(tiledVehicle.time.get)
      }
      pojo
    })

    CassandraSink.addSink(tiledVehiclePojoStream.javaStream)
      .setClusterBuilder(new ClusterBuilder {
        override def buildCluster(builder: Cluster.Builder) = builder.addContactPoint(cassandraHost).build()
      }).build()

    val producerConfig = FlinkKafkaProducer010.writeToKafkaWithTimestamps(
      tiledVehicleStream.javaStream,          // input stream
      "tiledVehicles",                        // target topic
      new TiledVehicleSerializationSchema,    // serialization schema
      properties                              // custom configuration for KafkaProducer (including broker list)
    )

    // the following is necessary for at-least-once delivery guarantee
    producerConfig.setLogFailuresOnly(false)   // "false" by default
    producerConfig.setFlushOnCheckpoint(true)  // "false" by default

    env.execute("KafkaToCassandraFlink")

  }
}
