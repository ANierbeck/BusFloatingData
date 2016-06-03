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
import com.datastax.spark.connector._
import de.nierbeck.floating.data.domain.{TiledVehicle, TiledVehicleCluster, Vehicle, VehicleCluster}
import de.nierbeck.floating.data.serializer.{TiledVehicleEncoder, VehicleFstDecoder}
import de.nierbeck.floating.data.tiler.TileCalc
import kafka.serializer.StringDecoder
import nak.cluster
import nak.cluster.{DBSCAN, GDBSCAN, Kmeans}
import org.apache.kafka.clients.producer.{KafkaProducer, Producer, ProducerRecord}
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.slf4j.{Logger, LoggerFactory}

//noinspection ScalaStyle
object CalcClusterSparkApp {

  val log:Logger = LoggerFactory.getLogger(getClass.getName)

  import scala.language.implicitConversions

  def main(args: Array[String]) {

    val consumerTopic = "METRO-Vehicles" //args(0)
    val sparkConf = new SparkConf()
      .setMaster("local[4]")
      .setAppName(getClass.getName)
      .set("spark.cassandra.connection.host", "localhost" /*s"${args(1)}"*/ )
      .set("spark.cassandra.connection.port", "9042" /*"${args(2)}"*/ )
      .set("spark.cassandra.connection.keep_alive_ms", "30000")
    val consumerProperties = Map("group.id" -> "group1", "bootstrap.servers" -> "localhost:9092" /*args(3)*/ , "auto.offset.reset" -> "smallest")

    val producerConf = new Properties()
    producerConf.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")
    producerConf.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    producerConf.put("bootstrap.servers", "localhost:9092")

    val sc = new SparkContext(sparkConf)

    val vehiclesRdd:RDD[Vehicle] = sc.cassandraTable[Vehicle]("streaming", "vehicles").where("time > '2016-06-04 00:00:00'")

    val vehiclesPos:Array[Double] = vehiclesRdd.flatMap(vehicle => Seq[Double](vehicle.latitude, vehicle.longitude)).collect()

    val dm = DenseMatrix.create[Double](vehiclesPos.length / 2, 2, vehiclesPos)

    log.info("calculate cluster")
    val cluster = dbscan(dm)
    log.info("cluster calculated")

    val clusterRdd = sc.parallelize(cluster)

    val clustered:RDD[VehicleCluster] = clusterRdd.filter(cluster => cluster.points.size > 4).map{cluster =>
      val points = cluster.points
      VehicleCluster(cluster.id.toInt, points(0).value(0), points(0).value(1), cluster.points.size)
    }

    clustered.saveToCassandra("streaming", "vehiclecluster")

    clustered.map(vehiclCluster => TiledVehicleCluster(TileCalc.convertLatLongToQuadKey(vehiclCluster.latitude, vehiclCluster.longitude), vehiclCluster.id, vehiclCluster.latitude, vehiclCluster.longitude, vehiclCluster.amount)).saveToCassandra("streaming", "vehiclecluster_by_tileid")

  }


  def dbscan(v : breeze.linalg.DenseMatrix[Double]):Seq[GDBSCAN.Cluster[Double]] = {
    val gdbscan = new GDBSCAN(
      DBSCAN.getNeighbours(epsilon = 0.001, distance = Kmeans.euclideanDistance),
      DBSCAN.isCorePoint(minPoints = 3)
    )
    val clusters = gdbscan.cluster(v)
    clusters
  }
}
