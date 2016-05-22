package de.nierbeck.floating.data.stream

import java.util.Date

import akka.actor.Props
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse, Uri }
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.{ Flow, Sink, Source }
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import de.nierbeck.floating.data.domain.{ RouteInfo, Vehicle, Vehicles }
import org.joda.time.DateTime

import scala.concurrent.Future
import scala.util.{ Failure, Success }

object VehiclesActor {

  case class Tick()

  def props(routeInfo: RouteInfo, httpClient: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]]): Props = Props(new VehiclesActor(routeInfo, httpClient))

}

class VehiclesActor(routeInfo: RouteInfo, httpClient: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]]) extends ActorPublisher[Vehicle] {

  import VehiclesActor._

  import org.json4s._
  import org.json4s.jackson.JsonMethods._
  import Json4sSupport._
  import concurrent.duration._

  implicit val executionContext = context.dispatcher
  implicit val actorMaterializer = ActorMaterializer()
  implicit val serialization = jackson.Serialization
  implicit val formats = DefaultFormats

  private val log = Logging(context.system, getClass.getName)

  log.info(s"VehiclesActor for routeID ${routeInfo.id} created")

  val tick = context.system.scheduler.schedule(0 seconds, 30 seconds, self, Tick())

  var buffer = Vector.empty[Vehicle]

  override def receive: Receive = {
    case Tick() => {
      log.info(s"extracting vehicles Infor for routeID: ${routeInfo.id}")
      extractVehicles(routeInfo.id)
    }
  }

  override def postStop() = tick.cancel()

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
        log.debug("got vehicle entities")
        val vehicles = Unmarshal(entity).to[Vehicles].onComplete {
          case Success(vehicles) => {
            val currTime = DateTime.now
            log.debug(vehicles.toString)
            vehicles.items.foreach {
              vehicle =>
                {
                  log.debug(vehicle.toString)
                  log.debug("sending vehicle to stream sink")
                  val vehicleToPersist = Vehicle(vehicle.id, Some(currTime.minusSeconds(vehicle.seconds_since_report).withMillisOfSecond(0).toDate), vehicle.latitude, vehicle.longitude, vehicle.heading, Some(routeInfo.id), vehicle.run_id, vehicle.seconds_since_report)
                  log.debug(s"sending Vehicle ${vehicleToPersist}")
                  if (buffer.isEmpty && totalDemand > 0) {
                    log.info(s"Buffer Empty sending vehicle: ${vehicleToPersist}")
                    onNext(vehicleToPersist)
                  } else {
                    log.info(s"Buffering vehicle: ${vehicleToPersist}")
                    buffer :+= vehicleToPersist
                    if (totalDemand > 0) {
                      val (use, keep) = buffer.splitAt(totalDemand.toInt)
                      buffer = keep
                      log.info(s"Demand is greater 0 sending ${use}")
                      use foreach onNext
                    }
                  }
                }
            }
          }
          case Failure(ex) => log.error(ex, ex.getMessage)
        }
      }
      case None => { log.debug("und noch mal weinen") }
    }
  }
}