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

import akka.actor.{ActorRef, ActorSystem}
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.{Route, StandardRoute}
import akka.pattern.ask
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.scaladsl._
import akka.util.Timeout
import com.datastax.driver.core.{ResultSet, Session}
import com.fasterxml.jackson.databind.SerializationFeature
import com.lambdaworks.jacks.JacksMapper
import de.nierbeck.floating.data.domain._
import de.nierbeck.floating.data.server.actors._
import de.nierbeck.floating.data.server.actors.rest._

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

trait RestService extends CorsSupport {

  val logger: LoggingAdapter

  val session: Session

  implicit val timeout = Timeout(3 seconds)

  //noinspection ScalaStyle
  def route()(implicit system: ActorSystem, ec: ExecutionContext): Route = {

    import akka.http.scaladsl.server.Directives._

    val vehiclesPerBBox = system.actorOf(VehiclesPerBBoxActor.props(), "vehicles-per-bbox")
    val routeDetailsPerId = system.actorOf(RouteDetailActor.props(), "route-details-id")
    val routeInfosPerId = system.actorOf(RouteInfoActor.props(), "route-info-id")
    val hotspots = system.actorOf(HotSpotsActor.props(), "hotspots")
    val hotSpotDetailsPerId = system.actorOf(HotSpotDetailsActor.props(), "hotspotDetails")

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
        parameter('bbox.as[String], 'time.as[String] ? "5") { (bbox, time) =>
          get {
            marshal {
              val boundingBox: BoundingBox = toBoundingBox(bbox)

              val askedVehicles: Future[Future[List[Vehicle]]] = (vehiclesPerBBox ? (boundingBox, time)).mapTo[Future[List[Vehicle]]]
              askedVehicles.flatMap(future => future)

            }
          }
        }
      }
    }

    def routeInfo = path("routeInfo" / IntNumber) { routeId =>
      corsHandler {
        get {
          marshal {
            (routeInfosPerId ? routeId).mapTo[Future[List[RouteInfo]]].flatMap(future => future)
          }
        }
      }
    }

    def routes = path("route" / IntNumber) { routeId =>
      corsHandler {
        get {
          marshal {
            (routeDetailsPerId ? routeId).mapTo[Future[List[RouteDetail]]].flatMap(future => future)
          }
        }
      }
    }

    def hotSpots = path("hotspots" / "boundingBox") {
      corsHandler {
        parameter('bbox.as[String]) { bbox =>
          get {
            marshal {
              val boundingBox: BoundingBox = toBoundingBox(bbox)
              (hotspots ? boundingBox).mapTo[Future[List[VehicleCluster]]].flatMap(future => future)
            }
          }
        }
      }
    }

    def hotSpotDetails = path("hotspots" / LongNumber) { hotSpotId =>
      corsHandler{
        get{
          marshal{
            (hotSpotDetailsPerId ? hotSpotId).mapTo[List[VehicleClusterDetails]]
          }
        }
      }
    }

    val vehiclesPerBBoxService = Flow[Message].map {
      case TextMessage.Strict(bbox) => {
        val boundingBox: BoundingBox = toBoundingBox(bbox)

        val vehicles = (vehiclesPerBBox ? boundingBox).mapTo[Future[List[Vehicle]]].flatMap(future => future)

        JacksMapper.mapper.enable(SerializationFeature.INDENT_OUTPUT)
        val result: Future[String] = vehicles.map(JacksMapper.writeValueAsString(_))
        TextMessage(Source.fromFuture(result))
      }
      case _ => TextMessage("Message type unsupported")
    }



    // Websocket endpoints
    def webSocketVehicles =
      path("ws" / "vehicles" / "boundingBox") {
        parameter('bbox.as[String]) { bbox =>
          get {
            logger.info("WebSocket request ...")
            handleWebSocketMessages(vehiclesPerBBoxService)
          }
        }
      }

    // Frontend
    def index = (path("") | pathPrefix("index.htm")) {
      getFromResource("index.html")
    }
    def img = (pathPrefix("data") & path(Segment)) { resource => getFromResource(s"data/$resource") }

    def js = (pathPrefix("js") & path(Segment)) { resource => getFromResource(s"js/${resource}") }

    get {
      index ~ img ~ js
    } ~ service ~ vehiclesOnBBox ~ routeInfo ~ routes ~ webSocketVehicles ~ hotSpots ~ hotSpotDetails
  }

  def marshal(m: => Future[Any])(implicit ec: ExecutionContext): StandardRoute =
    StandardRoute(ctx => {
      ctx.complete({
        JacksMapper.mapper.enable(SerializationFeature.INDENT_OUTPUT)
        m.map(JacksMapper.writeValueAsString(_))
      })
    })

}
