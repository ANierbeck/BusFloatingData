/*
 * Copyright Audi Electronics Venture GmbH 2016
 */
package de.nierbeck.floating.data.server

import com.datastax.driver.core.{ResultSet, Row, Session}
import de.nierbeck.floating.data.domain.{BoundingBox, LonLat, Vehicle}
import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import com.google.common.base.Strings
import com.google.common.util.concurrent.{FutureCallback, Futures, ListenableFuture}
import de.heikoseeberger.akkahttpcirce.CirceSupport
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import de.nierbeck.floating.data.tiler.TileCalc

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

trait RestService {

  private val format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

  def route(session: Session)(implicit system: ActorSystem, ec: ExecutionContext) = {

    import org.json4s._
    import org.json4s.JsonDSL._
    import org.json4s.jackson.JsonMethods._

    //prepared statements for cassandra calls
    val selectTrajectoriesByBBox = session.prepare("SELECT * FROM streaming.vehicles_by_tileid WHERE tile_id = ? AND time_id = ? ")

    def service = pathSingleSlash {
      encodeResponse {
        // serve up static content from a JAR resource
        getFromResourceDirectory("")
      }
    }

    def vehiclesOnBBox = path("vehicles" / "boundingBox") {
      parameter('bbox.as[String]) { bbox =>
        get {
          complete {
            val bboxCoords: Array[String] = bbox.split(",")
            val boundingBox: BoundingBox = new BoundingBox(LonLat(bboxCoords(0).toFloat, bboxCoords(1).toFloat), LonLat(bboxCoords(2).toFloat, bboxCoords(3).toFloat))

            val tileIds: Set[String] = TileCalc.convertBBoxToTileIDs(boundingBox)

            val timeStamp = new java.util.Date(System.currentTimeMillis() - 3600000)

            val futureResults : Set[Future[ResultSet]] = tileIds.map(tileId => session.executeAsync(selectTrajectoriesByBBox.bind(tileId, timeStamp)).toFuture)

            val futures: Set[Future[List[Vehicle]]] = futureResults.map(resultFuture => resultFuture.map(resultSet => resultSet.iterator().asScala.map( row => {
              Vehicle(row.getString("id"), Some(row.getTimestamp("time")), row.getDouble("latitude"), row.getDouble("longitude"), row.getInt("heading"))
            }).toList))

            val futureVehicles: Future[List[Vehicle]] = Future.sequence(futures.map(futureToFutureTry(_))).map(_.collect{ case Success(x) => x}).map(set => set.toList.flatten)

            val result:Future[String] = futureVehicles.map( vehicles => {
              val vehicleJson =
                ("vehicles" ->
                  vehicles.map { v =>
                    (("id" -> v.id))})
              val json:String = compact(render(vehicleJson))
              json
            })

            result
          }
        }
      }
    }

    service ~ vehiclesOnBBox
  }

  def futureToFutureTry[T](f: Future[T])(implicit ec: ExecutionContext): Future[Try[T]] =
    f.map(Success(_)).recover{ case exception: Exception  => Failure(exception)}

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
}
