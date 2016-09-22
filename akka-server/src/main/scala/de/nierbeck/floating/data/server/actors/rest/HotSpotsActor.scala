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

package de.nierbeck.floating.data.server.actors.rest

import akka.actor.Props
import akka.stream.ActorMaterializer
import com.datastax.driver.core.ResultSet
import de.nierbeck.floating.data.domain.{BoundingBox, VehicleCluster}
import de.nierbeck.floating.data.server._
import de.nierbeck.floating.data.server.actors.CassandraQuery
import de.nierbeck.floating.data.tiler.TileCalc

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

object HotSpotsActor {

  def props():Props = Props(new HotSpotsActor())

}

class HotSpotsActor extends CassandraQuery{

  implicit val executionContext = context.dispatcher
  implicit val actorMaterializer = ActorMaterializer()

  val selectHotSpotsByBoundingBox = session.prepare("SELECT * FROM streaming.vehiclecluster_by_tileid WHERE tile_id = ?")

  override def receive: Receive = {
    case boundingBox:BoundingBox => {
      log.info("received a BBox query")
      sender() ! getHotSpotsByBBox(boundingBox)
    }
    case _ => log.error("Wrong request")
  }

  def getHotSpotsByBBox(boundingBox: BoundingBox)(implicit executionContext: ExecutionContext): Future[List[VehicleCluster]] = {

    log.info(s"Querrying with bounding Box: ${boundingBox}")

    val tileIds: Set[String] = TileCalc.convertBBoxToTileIDs(boundingBox)

    log.info(s"extracted ${tileIds.size} tileIds")

    val futureResults: Set[Future[ResultSet]] = tileIds.map(tileId => session.executeAsync(selectHotSpotsByBoundingBox.bind(tileId)).toFuture)

    val futures: Set[Future[List[VehicleCluster]]] =
      futureResults.map(
        resultFuture => resultFuture.map{ resultSet =>
            resultSet.iterator().asScala.map{ row =>
//            log.info("found vehicleCluster in db")
            val vehicle = VehicleCluster(
              row.getInt("id"),
              row.getLong("time_stamp"),
              row.getDouble("latitude"),
              row.getDouble("longitude"),
              row.getInt("amount"))
//            log.info(s"vehicleCluster: ${vehicle}")
            vehicle
          }.toList})

    val futureVehicleClusters: Future[List[VehicleCluster]] =
      Future.sequence(
        futures.map(
          futureToFutureTry(_))).map(_.collect {
        case Success(x) => x
      }).map(set => set.toList.flatten)

    futureVehicleClusters

  }

}
