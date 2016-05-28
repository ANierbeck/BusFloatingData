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
import de.nierbeck.floating.data.domain.RouteInfo
import de.nierbeck.floating.data.server._
import de.nierbeck.floating.data.server.CassandraConnector

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.JavaConverters._

object RouteInfoActor {

  def props():Props = Props(new RouteInfoActor())

}

class RouteInfoActor extends CassandraQuery {

  implicit val executionContext = context.dispatcher
  implicit val actorMaterializer = ActorMaterializer()

  val selectRouteInfo = session.prepare("SELECT * FROM streaming.routeinfos WHERE ID = ?")

  override def receive: Receive = {
    case routeId:Int => sender() ! retrieveRouteInfo(routeId)
    case _ => log.error("Wrong request")
  }


  private def retrieveRouteInfo(routeId: Int)(implicit executionContext: ExecutionContext): Future[List[RouteInfo]] = {
    log.info(s"routeinfo requested for route id ${routeId}")

    val futureResult: Future[ResultSet] = session.executeAsync(selectRouteInfo.bind(routeId.toString)).toFuture

    val futures: Future[List[RouteInfo]] = futureResult.map(resultSet => resultSet.iterator().asScala.map(row => {
      RouteInfo(row.getString("id"), row.getString("display_name"))
    }).toList)

    futures
  }

}
