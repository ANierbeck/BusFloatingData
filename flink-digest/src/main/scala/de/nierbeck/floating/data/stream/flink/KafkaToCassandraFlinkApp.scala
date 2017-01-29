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

import com.datastax.driver.core.Cluster.Builder
import de.nierbeck.floating.data.domain.{TiledVehicle, Vehicle}
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.connectors.cassandra.{CassandraSink, ClusterBuilder}
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010
import org.apache.flink.streaming.util.serialization.DeserializationSchema
import org.apache.flink.api.scala._

object KafkaToCassandraFlinkApp {

  def main(args: Array[String]) {

    val env = StreamExecutionEnvironment.getExecutionEnvironment

    val properties:Properties = new Properties()

    properties.setProperty("bootstrap.servers", "localhost:9092")
    properties.setProperty("group.id", "test")

    val stream: DataStream[Vehicle] = env.addSource(new FlinkKafkaConsumer010[Vehicle]("METRO-Vehicles", new VehicleFstDeserializationSchema(), properties))

    val pojoStream: DataStream[VehiclePojo] = stream.map(scalaVehicle => {
      println(s"received a vehicle with id ${scalaVehicle.id}")
      val pojo = new VehiclePojo()
      pojo.setId(scalaVehicle.id)
      pojo.setHeading(scalaVehicle.heading)
      pojo.setLatitude(scalaVehicle.latitude)
      pojo.setLongitude(scalaVehicle.longitude)
      if (scalaVehicle.route_id.isDefined) {
        pojo.setRoute_id(Optional.of(scalaVehicle.route_id.get))
      }
      pojo.setRun_id(scalaVehicle.run_id)
      pojo.setSeconds_since_report(scalaVehicle.seconds_since_report)
      if (scalaVehicle.time.isDefined)
        pojo.setTime(Optional.of(scalaVehicle.time.get))
      pojo
    })

    CassandraSink.addSink(pojoStream.javaStream)
//      .setQuery("INSERT INTO streaming.vehicle (id, time, latitude, longitude, heading, route_id, run_id, seconds_since_report) values (?,?,?,?,?,?,?,?);")
      .setClusterBuilder(new ClusterBuilder {
        override def buildCluster(builder: Builder) = builder.addContactPoint("127.0.0.1").build()
      }).build()


//    val result = env.execute()
  }
}
