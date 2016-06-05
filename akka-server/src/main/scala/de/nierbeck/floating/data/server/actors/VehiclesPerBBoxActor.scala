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

package de.nierbeck.floating.data.server.actors

import akka.actor.{Actor, ActorLogging, Props}
import akka.stream.ActorMaterializer
import com.datastax.driver.core.{ResultSet, Session}
import de.nierbeck.floating.data.domain.{BoundingBox, Vehicle}
import de.nierbeck.floating.data.server._
import de.nierbeck.floating.data.tiler.TileCalc

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

object VehiclesPerBBoxActor {

  def props():Props = Props(new VehiclesPerBBoxActor())

}

class VehiclesPerBBoxActor extends CassandraQuery {

  implicit val executionContext = context.dispatcher
  implicit val actorMaterializer = ActorMaterializer()

  val selectTrajectoriesByBBox = session.prepare("SELECT * FROM streaming.vehicles_by_tileid WHERE tile_id = ? AND time_id IN ? AND time > ? ")

  override def receive(): Receive = {
    case boundingBox:BoundingBox => {
      log.info("received a BBox query")
      val x = getVehiclesByBBox(boundingBox)
      log.info(s"X: ${x}")
      sender() ! x
      context.system.eventStream.publish(boundingBox)
    }
    case _ => log.error("Wrong request")
  }

  def getVehiclesByBBox(boundingBox: BoundingBox)(implicit executionContext: ExecutionContext): Future[List[Vehicle]] = {

    log.info(s"Querrying with bounding Box: ${boundingBox}")

    val tileIds: Set[String] = TileCalc.convertBBoxToTileIDs(boundingBox)

    log.info(s"extracted ${tileIds.size} tileIds")

    val timeStamp = new java.util.Date(System.currentTimeMillis() - (10 * 60 * 1000))
    val timeIdminusOne = TileCalc.transformTime(timeStamp).getTime
    val timeId = TileCalc.transformTime(new java.util.Date(System.currentTimeMillis())).getTime

    val timeList = new java.util.ArrayList(List(timeIdminusOne, timeId).asJavaCollection)

    log.info(s"timeId: ${timeIdminusOne},${timeId}")

    val futureResults: Set[Future[ResultSet]] = tileIds.map(tileId => session.executeAsync(selectTrajectoriesByBBox.bind(tileId, timeList, timeStamp)).toFuture)

    val futures: Set[Future[List[Vehicle]]] =
      futureResults.map(
        resultFuture => resultFuture.map(
          resultSet => resultSet.iterator().asScala.map(row => {
            Vehicle(
              row.getString("id"),
              Some(row.getTimestamp("time")),
              row.getDouble("latitude"),
              row.getDouble("longitude"),
              row.getInt("heading"),
              Some(row.getString("route_id")))
          }).toList))

    val futureVehicles: Future[List[Vehicle]] =
      Future.sequence(
        futures.map(
          futureToFutureTry(_))).map(_.collect {
        case Success(x) => x
      }).map(set => set.toList.flatten)

    futureVehicles
  }

}
