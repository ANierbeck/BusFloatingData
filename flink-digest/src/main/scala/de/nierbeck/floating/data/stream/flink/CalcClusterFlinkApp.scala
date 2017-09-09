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

import java.util.Date

import breeze.linalg.DenseMatrix
import com.datastax.driver.core.Cluster
import com.datastax.driver.mapping.MappingManager
import de.nierbeck.floating.data.domain.{TiledVehicleCluster, VehicleCluster}
import de.nierbeck.floating.data.tiler.TileCalc
import nak.cluster.{DBSCAN, GDBSCAN, Kmeans}
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.api.scala.{DataSet, ExecutionEnvironment}
import org.apache.flink.batch.connectors.cassandra.{CassandraInputFormat, CassandraOutputFormat}
import org.apache.flink.streaming.api.scala.createTypeInformation
import org.apache.flink.streaming.connectors.cassandra.ClusterBuilder
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import org.apache.flink.api.java.tuple.{Tuple3 => JTuple3, Tuple5 => JTuple5, Tuple6 => JTuple6}
import org.apache.flink.util.Collector

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object CalcClusterFlinkApp {

  val LOG = LoggerFactory.getLogger(CalcClusterFlinkApp.getClass)

  def main(args: Array[String]): Unit = {
    // checking input parameters
    val params: ParameterTool = ParameterTool.fromArgs(args)

    // set up execution environment
    val env: ExecutionEnvironment = ExecutionEnvironment.getExecutionEnvironment

    val connection = params.getRequired("connection")
    val cassandraHost = connection.split(":").head
    val cassandraPort = Integer.parseInt(connection.split(":").reverse.head)

    val fmt: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")

    val startTime = params.get("startTime", DateTime.now().minusHours(1).toString(fmt))

    LOG.info(s"start time $startTime")

    val timeStartLimit: DateTime = fmt.parseDateTime(startTime)
    val timeStopLimit = timeStartLimit.plusHours(1)

    LOG.info(s"start time Limit: $timeStartLimit")
    LOG.info(s"stop time limit: $timeStopLimit")

    val query = "SELECT id, longitude, latitude FROM streaming.vehicles_flink WHERE time > '" + timeStartLimit.toString(fmt) + "' and time < '" + timeStopLimit.toString(fmt) + "' ALLOW FILTERING;"

    val vehicleClusterQuery = "INSERT INTO streaming.vehiclecluster_flink (id, time_stamp, longitude, latitude, amount) VALUES(?,?,?,?,?);"
    val tiledVehicleClusterQuery = "INSERT INTO streaming.vehiclecluster_by_tileid_flink (tile_id, id, time_stamp, longitude, latitude, amount) VALUES(?,?,?,?,?,?);"

    val cb = new ClusterBuilder() {
      override def buildCluster(builder: Cluster.Builder): Cluster = builder.addContactPoint(cassandraHost).withPort(cassandraPort).build
    }

    implicit val typeInfoArrayDouble = createTypeInformation[Array[Double]]
    implicit val typeInfoJTuple = createTypeInformation[JTuple3[String, Double, Double]]

    val cassandraInputFormat = new CassandraInputFormat[JTuple3[String, Double, Double]](query, cb)

    cassandraInputFormat.configure(null)
    cassandraInputFormat.open(null)

    val cassandraDS: DataSet[JTuple3[String, Double, Double]] = env.createInput(cassandraInputFormat)

    val amountCollected = cassandraDS.collect().toList.length

    LOG.info(s"retrieved $amountCollected from Cassandra")

    val pointsPerTileID  = cassandraDS.map(x => (x.f0, x.f1, x.f2))
      .map(vehicleTuple => (s"${vehicleTuple._1}_${vehicleTuple._2}_${vehicleTuple._3}", (vehicleTuple._2, vehicleTuple._3)))
      .distinct
      .map(map => (map._2._1, map._2._2))
      .map(coords => (TileCalc.convertLatLongToQuadKey(coords._1, coords._2).dropRight(3), (coords)))
      .groupBy(_._1)
      .reduceGroup {
        (tiledCoords, out:Collector[(String, List[(Double,Double)])]) =>
          var key: String = null
          val coordinates:List[(Double,Double)] = List.empty[(Double, Double)]

          for ((tileId, coords) <- tiledCoords) {
            key = tileId
            coordinates :+ coords
          }
          out.collect((key, coordinates))
      }

//    val seqOfVehiclePos:Seq[Array[Double]] = Seq(points)
//    val vehiclePosDataSet:DataSet[Array[Double]] = env.fromCollection(seqOfVehiclePos)

    val posPerTileId = pointsPerTileID.map {
      coordsPerTileId =>
        val key: String = coordsPerTileId._1
        val coordList = coordsPerTileId._2.flatMap( x => List(x._1, x._2)).toArray
        (key, coordList)
      }


/*
    val denseMatrixDataSet:DataSet[(String,DenseMatrix[Double])] = posPerTileId.map {
      tileIdPointsArray =>
        val tileId = tileIdPointsArray._1
        val pointsArray = tileIdPointsArray._2
        (tileId, DenseMatrix.create[Double](pointsArray.length / 2, 2, pointsArray))
    }

    val clusterDataSet:DataSet[GDBSCAN.Cluster[Double]] = denseMatrixDataSet.map(tiledDM => dbscan(tiledDM._2)).flatMap(x => x)
*/

    val posArray:Array[Double] = posPerTileId.map{(tileIdPositions) =>
      tileIdPositions._2
    }.collect().flatMap(identity(_)).toArray

    val dm = DenseMatrix.create(posArray.length / 2, 2, posArray)

    val cluster = dbscan(dm)

    val clusterDataSet = env.fromCollection(cluster)

    val clusterByKeyDS:DataSet[(Long,GDBSCAN.Cluster[Double])] = clusterDataSet.map(cluster => (cluster.id, cluster))

    val clusterWithCenter = clusterByKeyDS
      .map(clusterTuple => {
        val points = clusterTuple._2.points.map(_.value.toArray).toList.flatMap(x => x.toList)
        val coordTuples = convertListToTuple(points, List.empty)

        (clusterTuple._1, coordTuples.map(coordTuple => (points.size, convertToPoint(coordTuple))))
      })
      .map { pointListTuple =>
        (pointListTuple._1, pointListTuple._2.foldLeft((0, (0.0, 0.0, 0.0))) {
          case ((count, (accA, accB, accC)), (z, (a, b, c))) => (z, (accA + a, accB + b, accC + c))
        })
      }.map { idCountABC =>
      val id    = idCountABC._1
      val count = idCountABC._2._1
      val a     = idCountABC._2._2._1
      val b     = idCountABC._2._2._2
      val c     = idCountABC._2._2._3
      (id, (a / count, b / count, c / count))
    }.map { idABC =>
      val id = idABC._1
      val a  = idABC._2._1
      val b  = idABC._2._2
      val c  = idABC._2._3
      import Math._
      val lon = atan2(b, a)
      val hyp = sqrt(a * a + b * b)
      val lat = atan2(c, hyp)
      (id, (lat * 180 / PI, lon * 180 / PI))
    }

    val clustered = clusterByKeyDS
      .join(clusterWithCenter)
      .where(0).equalTo(0)
      .map{ x  =>
        val clusterId = x._1._1
        val centerLat = x._2._2._1
        val centerLon = x._2._2._2
        val cluster   = x._1._2
      VehicleCluster(clusterId.toInt, new Date().getTime, centerLat, centerLon, cluster.points.size)
    }

    val clusteredPojo = clustered
      .map(vehicleCluster => {
        //new JTuple5[Int, Long, Double, Double, Int](vehicleCluster.id, vehicleCluster.timeStamp, vehicleCluster.latitude, vehicleCluster.longitude, vehicleCluster.amount)
        val pojo = new VehicleClusterPojo()
        pojo.setId(vehicleCluster.id)
        pojo.setTimeStamp(vehicleCluster.timeStamp)
        pojo.setLatitude(vehicleCluster.latitude)
        pojo.setLongitude(vehicleCluster.longitude)
        pojo.setAmount(vehicleCluster.amount)
        pojo
      })

//    val clusterOutPut = new CassandraOutputFormat[JTuple5[Int, Long, Double, Double, Int]](vehicleClusterQuery, cb)
//    clusterOutPut.configure(null)
//    clusterOutPut.open(3,3)
    clusteredPojo.map(clusterPojo => {
      //clusterOutPut.writeRecord(clusterPojo)
      val cluster = new Cluster.Builder().addContactPoint(cassandraHost).build()
      val session = cluster.connect()
      val manager = new MappingManager(session)
      val vehicleClusterMapper = manager.mapper(classOf[VehicleClusterPojo])
      vehicleClusterMapper.save(clusterPojo)
      cluster.close()
    }).collect()
    //clusterOutPut.close()

    val tiledVehicleCluster = clustered
      .map { vehiclCluster =>
        TiledVehicleCluster(
          TileCalc.convertLatLongToQuadKey(vehiclCluster.latitude, vehiclCluster.longitude),
          vehiclCluster.id,
          vehiclCluster.timeStamp,
          vehiclCluster.latitude,
          vehiclCluster.longitude,
          vehiclCluster.amount)
      }

    val tiledVehicleClusterPojos = tiledVehicleCluster.map(tiledVehicleCluster => {
      //new JTuple6[String, Int, Long, Double, Double, Int](tiledVehicleCluster.tileId, tiledVehicleCluster.id, tiledVehicleCluster.timeStamp, tiledVehicleCluster.latitude, tiledVehicleCluster.longitude, tiledVehicleCluster.amount)
      val pojo = new TiledVehicleClusterPojo();
      pojo.setTileId(tiledVehicleCluster.tileId)
      pojo.setId(tiledVehicleCluster.id)
      pojo.setTimeStamp(tiledVehicleCluster.timeStamp)
      pojo.setLatitude(tiledVehicleCluster.latitude)
      pojo.setLongitude(tiledVehicleCluster.longitude)
      pojo.setAmount(tiledVehicleCluster.amount)
      pojo
    })

    //val tiledOutPut = new CassandraOutputFormat[JTuple6[String, Int, Long, Double, Double, Int]](tiledVehicleClusterQuery, cb)
    //tiledOutPut.configure(null)
    //tiledOutPut.open(3, 3)

    tiledVehicleClusterPojos.map(tiledVehicleClusterPojo => {
      val cluster = new Cluster.Builder().addContactPoint(cassandraHost).build()
      val session = cluster.connect()
      val manager = new MappingManager(session)

      val tiledVehicleClusterMapper = manager.mapper(classOf[TiledVehicleClusterPojo])
      tiledVehicleClusterMapper.save(tiledVehicleClusterPojo)
      cluster.close
      //tiledOutPut.writeRecord(tiledVehicleClusterPojo)
    }).collect()

    //tiledOutPut.close()

  }

  def dbscan(v : breeze.linalg.DenseMatrix[Double]):Seq[GDBSCAN.Cluster[Double]] = {
    val gdbscan = new GDBSCAN(
      DBSCAN.getNeighbours(epsilon = 0.0005, distance = Kmeans.euclideanDistance),
      DBSCAN.isCorePoint(minPoints = 3)
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

  def convertToPoint(coordTuple: (Double, Double)): (Double, Double, Double) = {
    import Math._

    val lat = coordTuple._1 * PI / 180
    val lon = coordTuple._2 * PI / 180

    val a = cos(lat) * cos(lon)
    val b = cos(lat) * sin(lon)
    val c = sin(lat)

    (a, b, c)
  }

  /**
    * Common trait for operations supported by both points and centroids
    * Note: case class inheritance is not allowed in Scala
    */
  trait Coordinate extends Serializable {

    var x: Double
    var y: Double

    def add(other: Coordinate): this.type = {
      x += other.x
      y += other.y
      this
    }

    def div(other: Long): this.type = {
      x /= other
      y /= other
      this
    }

    def euclideanDistance(other: Coordinate): Double =
      Math.sqrt((x - other.x) * (x - other.x) + (y - other.y) * (y - other.y))

    def clear(): Unit = {
      x = 0
      y = 0
    }

    override def toString: String =
      s"$x $y"

  }

  /**
    * A simple two-dimensional point.
    */
  case class Point(var x: Double = 0, var y: Double = 0) extends Coordinate

  /**
    * A simple two-dimensional centroid, basically a point with an ID.
    */
  case class Centroid(var id: Int = 0, var x: Double = 0, var y: Double = 0) extends Coordinate {

    def this(id: Int, p: Point) {
      this(id, p.x, p.y)
    }

    override def toString: String =
      s"$id ${super.toString}"

  }

}
