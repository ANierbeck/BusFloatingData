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

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging, Props}
import akka.stream.ActorMaterializer
import com.datastax.driver.core.{ResultSet, Session}
import de.nierbeck.floating.data.domain.RouteDetail
import de.nierbeck.floating.data.server._
import de.nierbeck.floating.data.server.CassandraConnector

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.JavaConverters._

object RouteDetailActor {

  def props():Props = Props(new RouteDetailActor())

}

class RouteDetailActor extends CassandraQuery {

  implicit val executionContext = context.dispatcher
  implicit val actorMaterializer = ActorMaterializer()

  val selectRoute = session.prepare("SELECT * FROM streaming.routes WHERE route_id = ?")
  val selectRouteByStop = session.prepare("SELECT * FROM streaming.routes WHERE route_id = ? and ")

  override def receive: Receive = {
    case routeId: Int => sender() ! retrieveRouteDetail(routeId)
    case _ => log.error("Wrong request")
  }

  private def retrieveRouteDetail(routeId: Int)(implicit executionContext: ExecutionContext): Future[List[RouteDetail]] = {
    log.info(s"route detaisl for route id: ${routeId}")

    val futureResult: Future[ResultSet] = session.executeAsync(selectRoute.bind(routeId.toString)).toFuture

    val futures: Future[List[RouteDetail]] = futureResult.map(resultSet => resultSet.iterator().asScala.map(row => {
      RouteDetail(row.getString("route_id"), row.getString("id"), row.getDouble("longitude"), row.getDouble("latitude"), row.getString("display_name"))
    }).toList)
    futures
  }

}
