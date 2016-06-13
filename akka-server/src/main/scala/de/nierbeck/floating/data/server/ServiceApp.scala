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

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.ws.UpgradeToWebSocket
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.stream.ActorMaterializer
import de.nierbeck.floating.data.server.actors.{RouterActor, TiledVehiclesFromKafkaActor}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

object ServiceApp extends RestService {

  import ServiceConfig._
  import system.dispatcher

  implicit val system = ActorSystem("service-api-http")
  implicit val mat = ActorMaterializer()

  override val logger = Logging(system, getClass.getName)
  override val session = CassandraConnector.connect()

  def main(args: Array[String]): Unit = {

    val router: ActorRef = system.actorOf(Props[RouterActor], "router")
    val vmactor: ActorRef = system.actorOf(TiledVehiclesFromKafkaActor.props(router), "Kafka-Consumer")


    val requestHandler: HttpRequest => HttpResponse = {
      case req@HttpRequest(GET, Uri.Path("/ws/vehicles"), _, _, _) =>
        req.header[UpgradeToWebSocket] match {
          case Some(upgrade) => upgrade.handleMessages(Flows.graphFlowWithStats(router))
          case None => HttpResponse(400, entity = "Not a valid websocket request!")
        }
      case _: HttpRequest => HttpResponse(404, entity = "Unknown resource!")
    }

    Http()
      .bindAndHandle(route(), serviceInterface, servicePort)
      .onComplete {
        case Success(_) => logger.info(s"Successfully bound to $serviceInterface:$servicePort")
        case Failure(e) => logger.error(s"Failed !!!! ${e.getMessage}")
      }

    Http()
      .bindAndHandleSync(requestHandler, "localhost", 8001)
      .onComplete {
        case Success(_) => logger.info(s"Successfully started Server")
        case Failure(e) => logger.error(s"Failed !!!! ${e.getMessage}")
      }

    Await.ready(system.whenTerminated, Duration.Inf)
    CassandraConnector.close(session)
  }

}
