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

package de.nierbeck.floating.data.stream

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse, Uri }
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Flow, Sink, Source }
import com.datastax.driver.core.{ Cluster, PreparedStatement, Session }
import de.heikoseeberger.akkahttpjson4s.Json4sSupport

import scala.concurrent.Future
import scala.util.{ Failure, Success }
import org.json4s.{ DefaultFormats, Formats, Serialization, jackson }

import concurrent.duration._
import akka.kafka.ProducerSettings
import org.reactivestreams.Subscriber

import scala.concurrent.Promise
import akka.kafka.scaladsl.{ Producer, _ }
import de.nierbeck.floating.data.domain.{ RouteInfos, Routes, Vehicle }
import de.nierbeck.floating.data.serializer.{ VehicleFstSerializer }
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ ByteArraySerializer, StringSerializer }

object StreamToKafkaApp {

  implicit val system = ActorSystem("stream-system")
  implicit val actorMaterializer = ActorMaterializer()

  val cluster: Cluster = Cluster.builder().addContactPoint("localhost").withPort(9042).build()
  val cassandraSession: Session = cluster.connect()

  val routeStatement: PreparedStatement = cassandraSession.prepare("INSERT INTO streaming.routes(id, order_id, route_id, longitude, latitude, display_name) VALUES(?, ?, ?, ?, ?, ?);")
  val vehiclesStatement: PreparedStatement = cassandraSession.prepare("INSERT INTO streaming.vehicles(id, time, longitude, latitude, heading, route_id, run_id, seconds_since_report) VALUES(?, ?, ?, ?, ?, ?, ?, ?);")
  val routeInfoStatement: PreparedStatement = cassandraSession.prepare("INSERT INTO streaming.routeInfos(id, display_name) VALUES(?,?);")

  //Kafka stuff
  val producerSettings = ProducerSettings(system, new ByteArraySerializer, new VehicleFstSerializer)
    .withBootstrapServers("localhost:9092")

  def main(args: Array[String]): Unit = {
    val httpClient: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] = Http(system).outgoingConnection("api.metro.net")

    val streamToKafka = new StreamToKafkaApp(system, httpClient)
    streamToKafka.run()
  }

}

class StreamToKafkaApp(system: ActorSystem, httpClient: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]]) {

  import org.json4s._
  import org.json4s.jackson.JsonMethods._
  import Json4sSupport._
  import StreamToKafkaApp._
  import StreamToKafkaApp.actorMaterializer
  import scala.concurrent.ExecutionContext.Implicits.global
  import concurrent.duration._

  implicit val serialization = jackson.Serialization
  implicit val formats = DefaultFormats

  private val log = Logging(system, getClass.getName)

  private val producer = Producer.plainSink(producerSettings)

  def run(): Unit = {
    log.info("starting StreamToKafkaApp")

    consume()
  }

  private def consume(): Unit = {

    log.debug("Consuming routes")
    val materializer = Source.single(HttpRequest(uri = Uri("/agencies/lametro/routes/"))).via(httpClient).runWith(Sink.head)
    val future = materializer.map { x =>
      x.status match {
        case status if status.isSuccess() => { log.info("success"); Some(x.entity) }
        case status if status.isFailure() => { None }
      }
    }

    future.onSuccess {
      case Some(entity) => {
        log.debug(entity.toString)
        val routeInfos = Unmarshal(entity).to[RouteInfos].onComplete {
          case Success(routeInfos) => {
            log.debug(routeInfos.toString)
            routeInfos.items.foreach { routeInfo =>
              {
                log.debug(routeInfo.toString)
                cassandraSession.executeAsync(routeInfoStatement.bind(routeInfo.id, routeInfo.display_name))
                extractRoutes(routeInfo.id)
                log.info("adding new actor route for routeInfo:" + routeInfo)

                //                Source.actorPublisher(VehiclesActor.props(routeInfo, httpClient)).map(elem => {
                //                    log.info(s"publishing element: ${elem}")
                //                    new ProducerRecord[Array[Byte], Vehicle]("vehicles", elem)
                //                }).to(producer).run()

                Flow[Vehicle].map(elem => {
                  log.info(s"publishing element: ${elem}")
                  new ProducerRecord[Array[Byte], Vehicle]("METRO-Vehicles", elem)
                }).to(producer).runWith(Source.actorPublisher(VehiclesActor.props(routeInfo, httpClient)))

              }
            }
          }
          case Failure(ex) => log.error(ex, ex.getMessage)

        }
      }
      case None => {
        log.debug("geh doch weinen")
      }
    }

  }

  def extractRoutes(routeId: String) = {

    val materializer = Source.single(HttpRequest(uri = Uri(s"/agencies/lametro/routes/$routeId/sequence/"))).via(httpClient).runWith(Sink.head)
    val future = materializer.map { x =>
      x.status match {
        case status if status.isSuccess() => { Some(x.entity) }
        case status if status.isFailure() => { None }
      }
    }

    future.onSuccess {
      case Some(entity) => {
        log.debug(entity.toString)
        val routes = Unmarshal(entity).to[Routes].onComplete {
          case Success(routes) => {
            log.debug(routes.toString)
            routes.items.zipWithIndex.foreach {
              case (route, index) =>
                {
                  log.debug(route.toString)
                  cassandraSession.executeAsync(routeStatement.bind(route.id, index.asInstanceOf[Object], routeId, route.longitude.asInstanceOf[Object], route.latitude.asInstanceOf[Object], route.display_name))
                }
            }
          }
          case Failure(ex) => log.error(ex, ex.getMessage)

        }
      }
      case None => { log.debug("geh doch weinen") }
    }
  }

}
