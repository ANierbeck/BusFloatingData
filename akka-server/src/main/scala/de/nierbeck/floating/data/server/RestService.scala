/*
 * Copyright Audi Electronics Venture GmbH 2016
 */
package de.nierbeck.floating.data.server

import com.datastax.driver.core.{ResultSet, Row, Session}
import de.nierbeck.floating.data.domain.{BoundingBox, LatLon, Vehicle}
import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.server.StandardRoute
import akka.stream.Materializer
import com.fasterxml.jackson.databind.SerializationFeature
import com.google.common.base.Strings
import com.google.common.util.concurrent.{FutureCallback, Futures, ListenableFuture}
import com.lambdaworks.jacks.JacksMapper
import de.nierbeck.floating.data.tiler.TileCalc

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

trait RestService extends CorsSupport {

  val logger: LoggingAdapter

  def route(session: Session)(implicit system: ActorSystem, ec: ExecutionContext) = {

    import org.json4s._
    import org.json4s.jackson.JsonMethods._

    import akka.http.scaladsl.server.Directives
    import akka.http.scaladsl.server.Directives._

    //prepared statements for cassandra calls
    val selectTrajectoriesByBBox = session.prepare("SELECT * FROM streaming.vehicles_by_tileid WHERE tile_id = ? AND time_id IN ? ")

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
              val boundingBox: BoundingBox = new BoundingBox(LatLon(bboxCoords(0).toFloat, bboxCoords(1).toFloat), LatLon(bboxCoords(2).toFloat, bboxCoords(3).toFloat))

              logger.debug(s"Querrying with bounding Box: ${boundingBox}")

              val tileIds: Set[String] = TileCalc.convertBBoxToTileIDs(boundingBox)

              logger.debug(s"extracted ${tileIds.size} tileIds")

              val timeStamp = new java.util.Date(System.currentTimeMillis() - (5*60*1000))
              val timeIdminusOne = TileCalc.transformTime(timeStamp).getTime
              val timeId = TileCalc.transformTime(new java.util.Date(System.currentTimeMillis())).getTime

              val timeList = new java.util.ArrayList(List(timeIdminusOne, timeId).asJavaCollection)

              logger.debug(s"timeId: ${timeIdminusOne},${timeId}")

              val futureResults: Set[Future[ResultSet]] = tileIds.map(tileId => session.executeAsync(selectTrajectoriesByBBox.bind(tileId, timeList)).toFuture)

              val futures: Set[Future[List[Vehicle]]] = futureResults.map(resultFuture => resultFuture.map(resultSet => resultSet.iterator().asScala.map(row => {
                Vehicle(row.getString("id"), Some(row.getTimestamp("time")), row.getDouble("latitude"), row.getDouble("longitude"), row.getInt("heading"))
              }).toList))

              val futureVehicles: Future[List[Vehicle]] = Future.sequence(futures.map(futureToFutureTry(_))).map(_.collect { case Success(x) => x }).map(set => set.toList.flatten)

              futureVehicles
            }
          }
        }
      }
    }

    service ~ vehiclesOnBBox
  }

  def futureToFutureTry[T](f: Future[T])(implicit ec: ExecutionContext): Future[Try[T]] =
    f.map(Success(_)).recover { case exception: Exception => Failure(exception) }

  implicit class RichListenableFuture[T](lf: ListenableFuture[T]) {
    def toFuture: Future[T] = {
      val p = Promise[T]()
      Futures.addCallback(lf, new FutureCallback[T] {
        def onFailure(t: Throwable): Unit = p failure t

        def onSuccess(result: T): Unit = p success result
      })
      p.future
    }
  }

  def marshal(m: => Future[Any])(implicit ec: ExecutionContext): StandardRoute =
    StandardRoute(ctx => {
      ctx.complete({
        JacksMapper.mapper.enable(SerializationFeature.INDENT_OUTPUT)
        m.map(JacksMapper.writeValueAsString(_))
      })
    })

}
