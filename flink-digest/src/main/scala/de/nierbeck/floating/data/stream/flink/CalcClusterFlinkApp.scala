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
import com.vividsolutions.jts.geom._
import de.nierbeck.floating.data.domain.{TiledVehicleCluster, VehicleCluster}
import de.nierbeck.floating.data.tiler.TileCalc
import nak.cluster.{DBSCAN, GDBSCAN, Kmeans}
import org.apache.flink.api.common.functions.RichMapFunction
import org.apache.flink.api.java.functions.FunctionAnnotation.ForwardedFields
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

    println(s"retrieved $amountCollected from Cassandra")

    val pointsPerTileID  = cassandraDS.map(x => (x.f0, x.f1, x.f2))
      .map(vehicleTuple => (s"${vehicleTuple._1}_${vehicleTuple._2}_${vehicleTuple._3}", (vehicleTuple._2, vehicleTuple._3)))
      .distinct
      .map(map => (map._2._1, map._2._2))
      .map(coords => (TileCalc.convertLatLongToQuadKey(coords._1, coords._2).dropRight(3), List(coords)))
      .groupBy(_._1)
        .reduce {
          (x1, x2)  => (x1._1, x1._2 ++ x2._2)
        }
      .map(reducedTileIDCoords => {
        println(s"ID: ${reducedTileIDCoords._1} contains amount of coords: ${reducedTileIDCoords._2.size}")
        reducedTileIDCoords
      })

    val posPerTileId = pointsPerTileID.map {
      coordsPerTileId =>
        val key: String = coordsPerTileId._1
        val coordList = coordsPerTileId._2.map( x => List(x._1, x._2).toArray).toArray
        (key, coordList)
      }

    val denseMatrixDataSet:DataSet[(String,DenseMatrix[Double])] = posPerTileId.map {
      tileIdPointsArray =>
        val tileId = tileIdPointsArray._1
        val pointsTupple = tileIdPointsArray._2
        println(s"TileID: ${tileId} contains ${pointsTupple.length} points")
        //(tileId, DenseMatrix())
        (tileId, DenseMatrix.create(pointsTupple.length, 2, pointsTupple.flatten))
    }.map{
      denseMatrix =>
        println(s"Densitiy Matrix: ${denseMatrix._1}, ${denseMatrix._2}")
        denseMatrix
    }

    val clusterDataSet:DataSet[GDBSCAN.Cluster[Double]] = denseMatrixDataSet
      .map(tiledDM => dbscan(tiledDM._2))
        .map{
          clusters =>
            println(s"calculated ${clusters.size} clusters")
          clusters
        }
      .flatMap(x => x)
      .map{
        cluster =>
          println(s"Cluster - ID: ${cluster.id}")
          println(s"Cluster - Points: ${cluster.points.size}")
        cluster
      }.filter(cluster => cluster.points.size > 3)

    val clusterByKeyDS:DataSet[(Long,GDBSCAN.Cluster[Double])] = clusterDataSet.map(cluster => (cluster.id, cluster))

    val clustered = clusterByKeyDS.map{
      clusterTuple => {
        val points = clusterTuple._2.points.map(_.value.toArray).toList.flatMap(x => x.toList)
        val coordTuples = convertListToTuple(points, List.empty)
        val envelope = new Envelope()
        convertToCoordinates(coordTuples).foreach(coord => envelope.expandToInclude(coord))
        val centre = envelope.centre
        VehicleCluster(clusterTuple._1.toInt, timeStartLimit.toDate.getTime, centre.x, centre.y, clusterTuple._2.points.size)
      }
    }

    val clusteredPojo = clustered
      .map(vehicleCluster => {
        val pojo = new VehicleClusterPojo()
        pojo.setId(vehicleCluster.id)
        pojo.setTimeStamp(vehicleCluster.timeStamp)
        pojo.setLatitude(vehicleCluster.latitude)
        pojo.setLongitude(vehicleCluster.longitude)
        pojo.setAmount(vehicleCluster.amount)
        pojo
      })

    val collectedCluster = clusteredPojo.map(clusterPojo => {
      val cluster = new Cluster.Builder().addContactPoint(cassandraHost).build()
      val session = cluster.connect()
      val manager = new MappingManager(session)
      val vehicleClusterMapper = manager.mapper(classOf[VehicleClusterPojo])
      vehicleClusterMapper.save(clusterPojo)
      cluster.close()
    }).collect()

    println(s"got ${collectedCluster.size} cluster points")

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
      val pojo = new TiledVehicleClusterPojo();
      pojo.setTileId(tiledVehicleCluster.tileId)
      pojo.setId(tiledVehicleCluster.id)
      pojo.setTimeStamp(tiledVehicleCluster.timeStamp)
      pojo.setLatitude(tiledVehicleCluster.latitude)
      pojo.setLongitude(tiledVehicleCluster.longitude)
      pojo.setAmount(tiledVehicleCluster.amount)
      pojo
    })

    tiledVehicleClusterPojos.map(tiledVehicleClusterPojo => {
      val cluster = new Cluster.Builder().addContactPoint(cassandraHost).build()
      val session = cluster.connect()
      val manager = new MappingManager(session)

      val tiledVehicleClusterMapper = manager.mapper(classOf[TiledVehicleClusterPojo])
      tiledVehicleClusterMapper.save(tiledVehicleClusterPojo)
      cluster.close
    }).collect()

  }

  def dbscan(input : breeze.linalg.DenseMatrix[Double]):Seq[GDBSCAN.Cluster[Double]] = {
    val gdbscan = new GDBSCAN(
      DBSCAN.getNeighbours(epsilon = 0.0005, distance = Kmeans.euclideanDistance),
      DBSCAN.isCorePoint(minPoints = 3)
    )
    println("calculating clusters ... ")
    val clusters = gdbscan cluster input
    println("... calculating done")
    clusters
  }

  def convertListToTuple(incomingList: List[Double], tupleList: List[(Double, Double)]): List[(Double, Double)] = {

    if (incomingList.size >= 2) {
      val newTuples: List[(Double, Double)] = tupleList :+ correctLatLon(incomingList.head, incomingList.tail.head)
      if (incomingList.size > 2)
        convertListToTuple(incomingList.tail.tail, newTuples)
      else
        newTuples
    } else {
      tupleList
    }
  }

  def convertToCoordinates(tuples: List[(Double, Double)]): List[Coordinate] = {
    tuples.map{
      tuple => {
        new Coordinate(tuple._1, tuple._2)
      }
    }
  }

  //this only works for LosAngeles
  private def correctLatLon(lat: Double, lon: Double) = {
    val MinLatitude = 33
    val MaxLatitude = 35
    val MinLongitude = -120
    val MaxLongitude = -100

    if (lon < MinLatitude || lon > MaxLatitude) {
      //obviously the cluster did switch the coordinates
      (lon, lat)
    } else {
      (lat, lon)
    }
  }


}
