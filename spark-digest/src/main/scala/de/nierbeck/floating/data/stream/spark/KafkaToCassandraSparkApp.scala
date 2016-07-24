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

package de.nierbeck.floating.data.stream.spark

import java.util.Properties

import breeze.linalg.DenseMatrix
import com.datastax.spark.connector.streaming._
import de.nierbeck.floating.data.domain.{TiledVehicle, Vehicle, VehicleCluster}
import de.nierbeck.floating.data.serializer.{TiledVehicleEncoder, VehicleFstDecoder}
import de.nierbeck.floating.data.tiler.TileCalc
import kafka.serializer.StringDecoder
import nak.cluster.{DBSCAN, GDBSCAN, Kmeans}
import org.apache.kafka.clients.producer.{KafkaProducer, Producer, ProducerRecord}
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.{SparkConf, streaming}
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Duration, Seconds, StreamingContext}

object KafkaToCassandraSparkApp {

  import scala.language.implicitConversions

  /**
    * Executable main method of KafkaToCassandraSparkApp.
    *
    * @param args - args(0): topicName, args(1): cassandra host name, args(2): cassandra port, arg(3): kafka host, args(4): kafka port
    */
  def main(args: Array[String]) {

    assert(args.size == 5, "Please provide the following params: topicname cassandrahost cassandraport kafkahost kafkaport")

    val kafkaHost = args(3)
    val kafkaPort = args(4)
    val cassandraHost = args(1)
    val cassandraPort = args(2)
    val consumerTopic = args(0)

    val sparkConf = new SparkConf()
      .setMaster("local[2]")
      .setAppName(getClass.getName)
      .set("spark.cassandra.connection.host", cassandraHost )
      .set("spark.cassandra.connection.port", cassandraPort )
      .set("spark.cassandra.connection.keep_alive_ms", "30000")
    val consumerProperties = Map("group.id" -> "group1", "bootstrap.servers" -> s"""$kafkaHost:$kafkaPort""", "auto.offset.reset" -> "smallest")

    val producerConf = new Properties()
    producerConf.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")
    producerConf.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    producerConf.put("bootstrap.servers", s"""$kafkaHost:$kafkaPort""")

    //noinspection ScalaStyle
    val ssc = new StreamingContext(sparkConf, Seconds(10))

    val kafkaStream = KafkaUtils.createDirectStream[String, Vehicle, StringDecoder, VehicleFstDecoder](
      ssc,
      consumerProperties,
      Set(consumerTopic)
    )

    val vehicle = kafkaStream.map { tuple => tuple._2 }.cache()

    vehicle.saveToCassandra("streaming", "vehicles")

    vehicle.filter(x => x.time.isDefined)

    val tiledVehicle = vehicle.map(vehicle => TiledVehicle(
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

    tiledVehicle.cache()

    tiledVehicle.saveToCassandra("streaming", "vehicles_by_tileid")

    tiledVehicle.foreachRDD(rdd => rdd.foreachPartition(f = tiledVehicles => {

      val producer: Producer[String, Array[Byte]] = new KafkaProducer[String, Array[Byte]](producerConf)

      tiledVehicles.foreach { tiledVehicle =>
        val message = new ProducerRecord[String, Array[Byte]]("tiledVehicles", new TiledVehicleEncoder().toBytes(tiledVehicle))
        producer.send(message)
      }

      producer.close()

    }))

    ssc.start()
    ssc.awaitTermination()
    ssc.stop()
  }

}
