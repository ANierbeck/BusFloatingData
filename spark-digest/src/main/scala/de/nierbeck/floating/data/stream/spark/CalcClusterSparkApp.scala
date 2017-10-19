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
import com.vividsolutions.jts.geom.{Coordinate, Envelope}
import de.nierbeck.floating.data.domain._
import de.nierbeck.floating.data.tiler.TileCalc
import nak.cluster.{DBSCAN, GDBSCAN, Kmeans}
import org.apache.spark.mllib.clustering.BisectingKMeans
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import org.slf4j.{Logger, LoggerFactory}

//noinspection ScalaStyle
object CalcClusterSparkApp {

  val log:Logger = LoggerFactory.getLogger(getClass.getName)

  import scala.language.implicitConversions

  /**
    * Executable main method of CalcClusterSparkApp.
    *
    * @param args - args(0): cassandra-host:port, arg(1): start_time
    *             for example: "2016-06-05 18:00:00"
    */
  def main(args: Array[String]) {

    assert(args.size >= 1, "Please provide the following params: cassandrahost:cassandraport start_time[optional]")
    val cassandraHost = args(0).split(":").head
    val cassandraPort = args(0).split(":").reverse.head

    val fmt:DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")

    var startTime = DateTime.now().minusHours(1).toString(fmt)

    if (args.length > 1)
      startTime = args(1)

    val sparkConf = new SparkConf()
      .setAppName(getClass.getName)
      .set("spark.cassandra.connection.host", cassandraHost )
      .set("spark.cassandra.connection.port", cassandraPort )
      .set("spark.cassandra.connection.keep_alive_ms", "30000")

    val sc = new SparkContext(sparkConf)

    val timeStartLimit:DateTime = fmt.parseDateTime(startTime)

    val timeStopLimit = timeStartLimit.plusHours(1)

    val vehiclesRdd:RDD[Vehicle] = sc.cassandraTable[Vehicle]("streaming", "vehicles").where("time > ? and time < ?", fmt.print(timeStartLimit), fmt.print(timeStopLimit))

    val vehiclesPosRdd: RDD[(String, List[(Double,Double)])] = vehiclesRdd
      .flatMap(vehicle => Seq[(String, (Double,Double))]((s"${vehicle.id}_${vehicle.longitude}_${vehicle.latitude}",(vehicle.longitude, vehicle.latitude))))
      .distinct
      .map(tuple => (TileCalc.convertLatLongToQuadKey(tuple._2._1, tuple._2._2).dropRight(3), tuple._2))
      .groupBy(_._1)
      .map(x => (x._1, x._2.map(tuple => tuple._2).toList ))
      .reduceByKey( (x,y) => x ++ y )


    val denseMatrixRdd: RDD[(String, DenseMatrix[Double])] =
      vehiclesPosRdd
        .map{
          tileIdPointsArray =>
            //println(s"TileID ${tileIdPointsArray._1} contains ${tileIdPointsArray._2}")
            val tileId = tileIdPointsArray._1
            val pointsTuple = tileIdPointsArray._2.map(tuple => List(tuple._1, tuple._2).toArray).toArray
            (tileId, DenseMatrix.create[Double](pointsTuple.length, 2, pointsTuple.flatten))
        }.filter( dm => dm._2.size > 3)

    val clusterRdd:RDD[(String, Seq[GDBSCAN.Cluster[Double]])] = denseMatrixRdd.mapValues(dbscan)

    val clusterRddFiltered = clusterRdd
      .values
      .flatMap(x => x)
      .filter(cluster => cluster.points.map(_.value.toArray).length >= 4)
      .map{
        cluster =>
          val points = cluster.points.map(_.value.toArray)
          val coordinates = points.map(p => (p(0),p(1))).toList
          val correctedCoordinates = coordinates.map(tuple => correctLonLat(tuple._1,tuple._2))
          println(s"points: $points")
          println(s"coords: $correctedCoordinates")
          (cluster.id, correctedCoordinates)
      }
      .filter{
        tuples =>
          val coordinates = convertToCoordinates(tuples._2)
          val valid = coordinates.forall(validCoordinate)
          println(s"Cluster ${tuples._1} is valid? ${valid}")
          valid
      }
        .map{
          tuples =>
            println(s"Cluster: ${tuples._1} contains ${tuples._2.length} points")
          tuples
        }
      .cache()

    clusterRddFiltered.map{ clusterTuple =>
      val timeStamp:Long = new Date().getTime
      val id:Long = clusterTuple._1
      val points = clusterTuple._2
      points.zipWithIndex.map{tuple =>
        val points = (tuple._1._1, tuple._1._2)
        val index = tuple._2
        VehicleClusterDetails(id, index, timeStamp, points._1, points._2)
      }
    }.flatMap(identity)
      .saveToCassandra("streaming", "vehicleclusterdetails")

    val clusterdByKey = clusterRddFiltered.map(cluster => (cluster._1, cluster))

    val clustered:RDD[VehicleCluster] = clusterRddFiltered.map{ cluster =>
      val coordTuples = cluster._2
      log.info(s"cluster: ${cluster}")

      val envelope = new Envelope()

      convertToCoordinates(coordTuples).foreach(coord => envelope.expandToInclude(coord))
      val centre = envelope.centre

      VehicleCluster(cluster._1.toInt, timeStartLimit.toDate.getTime, centre.x, centre.y, coordTuples.size )
    }

    clustered.saveToCassandra("streaming", "vehiclecluster")

    clustered.map(vehiclCluster => TiledVehicleCluster(TileCalc.convertLatLongToQuadKey(vehiclCluster.latitude, vehiclCluster.longitude), vehiclCluster.id, vehiclCluster.timeStamp, vehiclCluster.latitude, vehiclCluster.longitude, vehiclCluster.amount)).saveToCassandra("streaming", "vehiclecluster_by_tileid")

    //sc.stop()

  }

  def convertToCoordinates(tuples: List[(Double, Double)]): List[Coordinate] = {
    tuples.map{
      tuple => {
        val correctedTuple = correctLonLat(tuple._1, tuple. _2)
        new Coordinate(correctedTuple._1, correctedTuple._2)
      }
    }
  }

  def dbscan(v : breeze.linalg.DenseMatrix[Double]):Seq[GDBSCAN.Cluster[Double]] = {
    println(s"calculating cluster for denseMatrix")
    val gdbscan = new GDBSCAN(
      DBSCAN.getNeighbours(epsilon = 0.001, distance = Kmeans.euclideanDistance),
      DBSCAN.isCorePoint(minPoints = 3)
    )
    gdbscan cluster v
  }

  //this only works for LosAngeles
  private def correctLonLat(lon: Double, lat: Double) = {
    val MinLongitude = 33.0
    val MaxLongitude = 35.0
    val MinLatitude = -120.0
    val MaxLatitude = -100.0

    if (MinLongitude < lat && lat < MaxLongitude) {
      //obviously the cluster did switch the coordinates
      (lat, lon)
    } else {
     (lon, lat)
    }
  }

  private def validCoordinate(coordinate: Coordinate): Boolean = {
    val MinLongitude = 33.0
    val MaxLongitude = 35.0
    val MinLatitude = -120.0
    val MaxLatitude = -100.0

    MinLongitude < coordinate.x && coordinate.x < MaxLongitude && MinLatitude < coordinate.y && coordinate.y < MaxLatitude

  }

}
