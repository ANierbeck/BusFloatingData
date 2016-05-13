package de.nierbeck.floating.data.stream.simple

import java.util.Date

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.datastax.driver.core.{Cluster, PreparedStatement, Session}
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import de.nierbeck.floating.data.domain.{RouteInfos, Routes, Vehicles}
import org.apache.kafka.common

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
 * Created by anierbeck on 11.01.16.
 */

object StreamApp {

  implicit val system = ActorSystem("stream-system")
  implicit val actorMaterializer = ActorMaterializer()

  val cluster: Cluster = Cluster.builder().addContactPoint("localhost").withPort(9042).build()
  val session: Session = cluster.connect()

  val routeStatement: PreparedStatement = session.prepare("INSERT INTO streaming.routes(id, route_id, longitude, latitude, display_name) VALUES(?, ?, ?, ?, ?);")
  val vehiclesStatement: PreparedStatement = session.prepare("INSERT INTO streaming.vehicles(id, time, longitude, latitude, heading, route_id, run_id, seconds_since_report) VALUES(?, ?, ?, ?, ?, ?, ?, ?);")
  val routeInfoStatement: PreparedStatement = session.prepare("INSERT INTO streaming.routeInfos(id, display_name) VALUES(?,?);")

  def main(args: Array[String]): Unit = {
    val httpClient = Http(system).outgoingConnection("api.metro.net")

    val streamApp = new StreamApp(system, httpClient)

    streamApp.run()
  }

}

class StreamApp(system: ActorSystem, httpClient: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]]) {

  import Json4sSupport._
  import StreamApp.{actorMaterializer, _}
  import org.json4s._

  import concurrent.duration._
  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val serialization = jackson.Serialization
  implicit val formats = DefaultFormats

  private val log = Logging(system, getClass.getName)

  def run(): Unit = {
    log.info("Running \\o/")

    extractRouteNames()

  }

  def extractRouteNames(): Unit = {

    val materializer = Source.single(HttpRequest(uri = Uri("/agencies/lametro/routes/"))).via(httpClient).runWith(Sink.head)
    val future = materializer.map { x =>
      x.status match {
        case status if status.isSuccess() => { Some(x.entity) }
        case status if status.isFailure() => { None }
      }
    }

    future.onSuccess {
      case Some(entity) => {
        log.info(entity.toString)
        Unmarshal(entity).to[RouteInfos].onComplete {
          case Success(routeInfos) => {
            log.info(routeInfos.toString)
            routeInfos.items.foreach { routeInfo =>
              {
                log.info(routeInfo.toString)
                session.executeAsync(routeInfoStatement.bind(routeInfo.id, routeInfo.display_name))
                extractRoutes(routeInfo.id)
                system.scheduler.schedule(0 seconds, 30 seconds)(extractVehicles(routeInfo.id))
              }
            }
          }
          case Failure(ex) => log.error(ex, ex.getMessage)

        }
      }
      case None => {
        log.info("geh doch weinen")
      }
    }

  }

  def extractVehicles(routeId: String) = {
    val materializerVehicles = Source.single(HttpRequest(uri = Uri(s"/agencies/lametro/routes/$routeId/vehicles/"))).via(httpClient).runWith(Sink.head)
    val vehiclesFuture = materializerVehicles.map { x =>
      x.status match {
        case status if status.isSuccess() => { Some(x.entity) }
        case status if status.isFailure() => { None }
      }
    }
    vehiclesFuture.onSuccess {
      case Some(entity) => {
        log.info("got vehicle entities")
        Unmarshal(entity).to[Vehicles].onComplete {
          case Success(vehicles) => {
            val currTime = new Date()
            log.info(vehicles.toString)
            vehicles.items.foreach {
              vehicle =>
                {
                  log.info(vehicle.toString)
                  //id, time, longitude, latitude, heading, route_id, run_id, seconds_since_report
                  session.executeAsync(vehiclesStatement.bind(
                    vehicle.id,
                    currTime.asInstanceOf[Object],
                    vehicle.longitude.asInstanceOf[Object],
                    vehicle.latitude.asInstanceOf[Object],
                    vehicle.heading,
                    vehicle.route_id,
                    vehicle.run_id,
                    vehicle.seconds_since_report))

                }
            }
          }
          case Failure(ex) => log.error(ex, ex.getMessage)
        }
      }
      case None => { log.info("und noch mal weinen") }
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
        log.info(entity.toString)
        val routes = Unmarshal(entity).to[Routes].onComplete {
          case Success(routes) => {
            log.info(routes.toString)
            routes.items.foreach { route =>
              {
                log.info(route.toString)
                session.executeAsync(routeStatement.bind(route.id, routeId, route.longitude.asInstanceOf[Object], route.latitude.asInstanceOf[Object], route.display_name))
              }
            }
          }
          case Failure(ex) => log.error(ex, ex.getMessage)

        }
      }
      case None => { log.info("geh doch weinen") }
    }
  }
}
