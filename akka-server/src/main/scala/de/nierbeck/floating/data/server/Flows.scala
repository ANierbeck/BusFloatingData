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

import akka.actor.{ActorRef, Props}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Source}
import de.nierbeck.floating.data.domain.Vehicle
import de.nierbeck.floating.data.server.actors.VehiclePublisher
import GraphDSL.Implicits._
import de.nierbeck.floating.data.server._

/**
  */
object Flows {

  def graphFlowWithStats(router: ActorRef): Flow[Message, Message, _] = {
    Flow.fromGraph(GraphDSL.create() { implicit builder =>


      // create an actor source
      val source = Source.actorPublisher[String](VehiclePublisher.props(router))

      // Graph elements we'll use
      val merge = builder.add(Merge[String](2))
      val filter = builder.add(Flow[String].filter(_ => false))

      // get BBox from request and send it to route, return nothing ...
      val mapMsgToString = builder.add(Flow[Message].map[String] {
        case TextMessage.Strict(msg) => {
          println(s"received message: $msg")
          if (msg.contains("close")) {
            router ! msg
          } else {
            val bbox = toBoundingBox(msg)
            println(s"transformedt to bbox: $bbox")
            router ! bbox
          }
          ""
        }
      })
      //outgoing message ...
      val mapStringToMsg = builder.add(Flow[String].map[Message](x => TextMessage.Strict(x)))

      //add source to flow
      val vehiclesSource = builder.add(source)

      // connect the graph
      mapMsgToString ~> filter ~> merge // this part of the merge will never provide msgs
      vehiclesSource ~> merge ~> mapStringToMsg

      // expose ports
      FlowShape(mapMsgToString.in, mapStringToMsg.out)
    })
  }

}
