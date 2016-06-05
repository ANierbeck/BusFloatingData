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

import java.util.{Date, Properties}

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

    val vehiclesRdd:RDD[Vehicle] = sc.cassandraTable[Vehicle]("streaming", "vehicles").where("time > '2016-06-05 18:00:00' and time < '2016-06-05 18:15:00'")

    val vehiclesPos:Array[Double] = vehiclesRdd.flatMap(vehicle => Seq[Double](vehicle.latitude, vehicle.longitude)).collect()

    log.info(s"got ${vehiclesPos.length} positions, first: ${vehiclesPos.head},${vehiclesPos.tail.head}")

    val dm = DenseMatrix.create[Double](vehiclesPos.length / 2, 2, vehiclesPos)

    log.info("calculate cluster")
    val cluster = dbscan(dm)
    log.info(s"cluster calculated: ${cluster.seq.size}")

    log.info(s"clusters: ${cluster}")

    val clusterRdd = sc.parallelize(cluster)

    val clusterRddFiltered = clusterRdd/*.filter(cluster => cluster.points.size > 4)*/.cache()

    val clusterdByKey = clusterRddFiltered.map(cluster => (cluster.id, cluster))

    val coordPointList:RDD[(Long,(Double,Double))] = clusterRddFiltered.map{ cluster =>
      //cluster.map(_.points.map(_.value.toArray))
      val points: Seq[Array[Double]] = cluster.points.map(_.value.toArray)
      log.info(s"cluster: ${cluster}")

      val coords: List[Double] = points.toList.flatMap(x => x.toList)

      val coordTuples = convertListToTuple(coords, List.empty)

      (cluster.id, coordTuples.map(coordTuple => (points.size, convertToPoint(coordTuple))))
    }.map{pointListTuple =>
      (pointListTuple._1, pointListTuple._2.foldLeft((0, (0.0,0.0,0.0))) {
        case ((count,(accA,accB,accC)), (z,(a,b,c))) => ( z,  (accA + a, accB + b, accC + c))
      })
    }.map{ case ((id, (count, (a, b, c)))) =>
      (id, (a/count, b/count, c/count))
    }.map{ case ((id,(a,b,c))) =>
      import Math._
      val lon = atan2(b,a)
      val hyp = sqrt(a * a + b * b)
      val lat = atan2(c, hyp)
      (id, (lat * 180 / PI, lon * 180 / PI))
    }

    val clustered:RDD[VehicleCluster] = clusterdByKey.join(coordPointList).map{ case ( (clusterId, (cluster, (centerLat, centerLon))) )  =>
      VehicleCluster(clusterId.toInt, new Date().getTime, centerLat, centerLon, cluster.points.size)
    }


/*
    val clustered:RDD[VehicleCluster] = clusterRddFiltered.map{cluster =>
      val points:Seq[GDBSCAN.Point[Double]] = cluster.points

      val coords: List[Double] = points.map(point => point.value.data).toList.flatMap(x => x.toList)

//      val coordTuples = convertListToTuple(coords, List.empty)
//      coordTuples.map(coordTuple => convertToPoint(coordTuple))

      VehicleCluster(cluster.id.toInt, points(0).value(0), points(0).value(1), cluster.points.size, coords)
    }
*/

    clustered.saveToCassandra("streaming", "vehiclecluster")

    clustered.map(vehiclCluster => TiledVehicleCluster(TileCalc.convertLatLongToQuadKey(vehiclCluster.latitude, vehiclCluster.longitude), vehiclCluster.id, vehiclCluster.timeStamp, vehiclCluster.latitude, vehiclCluster.longitude, vehiclCluster.amount)).saveToCassandra("streaming", "vehiclecluster_by_tileid")

  }

  def convertToPoint(coordTuple:(Double,Double)):(Double,Double,Double) = {
    import Math._

    val lat = coordTuple._1 * PI / 180
    val lon = coordTuple._2 * PI / 180

    val a = cos(lat) * cos(lon)
    val b = cos(lat) * sin(lon)
    val c = sin(lat)

    (a,b,c)
  }

  def dbscan(v : breeze.linalg.DenseMatrix[Double]):Seq[GDBSCAN.Cluster[Double]] = {
    log.info(s"calculating cluster for denseMatrix: ${v.data.head}, ${v.data.tail.head}")
    val gdbscan = new GDBSCAN(
      DBSCAN.getNeighbours(epsilon = 0.001, distance = Kmeans.euclideanDistance),
      DBSCAN.isCorePoint(minPoints = 4)
    )
    val clusters = gdbscan.cluster(v)
    clusters
  }

  def convertListToTuple(incomingList: List[Double], tupleList: List[(Double, Double)]): List[(Double, Double)] = {

    if (incomingList.size >= 2) {
      val newTuples: List[(Double, Double)] = tupleList :+ (incomingList.head, incomingList.tail.head)
      if (incomingList.size > 2)
        convertListToTuple(incomingList.tail.tail, newTuples)
      else
        newTuples
    } else {
      tupleList
    }
  }

}
