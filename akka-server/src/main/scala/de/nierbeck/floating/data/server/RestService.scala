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

package de.nierbeck.floating.data.server

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.server.{Route, StandardRoute}
import akka.pattern.ask
import akka.util.Timeout
import com.datastax.driver.core.{ResultSet, Session}
import com.fasterxml.jackson.databind.SerializationFeature
import com.lambdaworks.jacks.JacksMapper
import de.nierbeck.floating.data.domain._
import de.nierbeck.floating.data.tiler.TileCalc

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

trait RestService extends CorsSupport {

  val logger: LoggingAdapter

  val session:Session

  implicit val timeout = Timeout(15 seconds)

  def route()(implicit system: ActorSystem, ec: ExecutionContext): Route = {

    import akka.http.scaladsl.server.Directives._

    def service = pathSingleSlash {
      corsHandler {
        encodeResponse {
          // serve up static content from a JAR resource
          getFromResourceDirectory("")
        }
      }
    }

    def vehiclesOnBBox = path("vehicles" / "boundingBox") {
      corsHandler {
        parameter('bbox.as[String]) { bbox =>
          get {
            marshal {
              val bboxCoords: Array[String] = bbox.split(",")
              val boundingBox: BoundingBox =
                new BoundingBox(LatLon(bboxCoords(0).toFloat, bboxCoords(1).toFloat), LatLon(bboxCoords(2).toFloat, bboxCoords(3).toFloat))

              getVehiclesByBBox(boundingBox)
            }
          }
        }
      }
    }

    def routeInfo = path("routeInfo" / IntNumber) { routeId =>
      corsHandler {
        get {
          marshal {
            retrieveRouteInfo(routeId)
          }
        }
      }
    }

    def routes = path("route" / IntNumber) { routeId =>
      corsHandler {
        get {
          marshal {
            retrieveRouteDetail(routeId)
          }
        }
      }
    }


    // Frontend
    def index = (path("") | pathPrefix("index.htm")) {
      getFromResource("index.html")
    }
    def img = (pathPrefix("data") & path(Segment)) { resource => getFromResource(s"data/$resource") }

    def js = (pathPrefix("js") & path(Segment)) { resource => getFromResource(s"js/${resource}")}

    get {
      index ~ img ~ js
    } ~ service ~ vehiclesOnBBox ~ routeInfo ~ routes
  }

  def getVehiclesByBBox(boundingBox: BoundingBox)(implicit executionContext: ExecutionContext): Future[List[Vehicle]] = {

    val selectTrajectoriesByBBox = session.prepare("SELECT * FROM streaming.vehicles_by_tileid WHERE tile_id = ? AND time_id IN ? ")

    logger.debug(s"Querrying with bounding Box: ${boundingBox}")

    val tileIds: Set[String] = TileCalc.convertBBoxToTileIDs(boundingBox)

    logger.debug(s"extracted ${tileIds.size} tileIds")

    val timeStamp = new java.util.Date(System.currentTimeMillis() - (5 * 60 * 1000))
    val timeIdminusOne = TileCalc.transformTime(timeStamp).getTime
    val timeId = TileCalc.transformTime(new java.util.Date(System.currentTimeMillis())).getTime

    val timeList = new java.util.ArrayList(List(timeIdminusOne, timeId).asJavaCollection)

    logger.debug(s"timeId: ${timeIdminusOne},${timeId}")

    val futureResults: Set[Future[ResultSet]] = tileIds.map(tileId => session.executeAsync(selectTrajectoriesByBBox.bind(tileId, timeList)).toFuture)

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

  private def retrieveRouteDetail(routeId: Int)(implicit executionContext: ExecutionContext): Future[List[RouteDetail]] = {
    logger.info(s"route detaisl for route id: ${routeId}")

    val selectRoute = session.prepare("SELECT * FROM streaming.routes WHERE route_id = ?")

    val futureResult: Future[ResultSet] = session.executeAsync(selectRoute.bind(routeId.toString)).toFuture

    val futures: Future[List[RouteDetail]] = futureResult.map(resultSet => resultSet.iterator().asScala.map(row => {
      RouteDetail(row.getString("route_id"), row.getString("id"), row.getDouble("longitude"), row.getDouble("latitude"), row.getString("display_name"))
    }).toList)
    futures
  }

  private def retrieveRouteInfo(routeId: Int)(implicit executionContext: ExecutionContext): Future[List[RouteInfo]] = {
    logger.info(s"routeinfo requested for route id ${routeId}")

    val selectRouteInfo = session.prepare("SELECT * FROM streaming.routeinfos WHERE ID = ?")

    val futureResult: Future[ResultSet] = session.executeAsync(selectRouteInfo.bind(routeId.toString)).toFuture

    val futures: Future[List[RouteInfo]] = futureResult.map(resultSet => resultSet.iterator().asScala.map(row => {
      RouteInfo(row.getString("id"), row.getString("display_name"))
    }).toList)

    futures
  }

  def marshal(m: => Future[Any])(implicit ec: ExecutionContext): StandardRoute =
    StandardRoute(ctx => {
      ctx.complete({
        JacksMapper.mapper.enable(SerializationFeature.INDENT_OUTPUT)
        m.map(JacksMapper.writeValueAsString(_))
      })
    })

}
