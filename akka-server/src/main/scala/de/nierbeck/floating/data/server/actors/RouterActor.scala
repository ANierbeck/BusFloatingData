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

import akka.actor.{Actor, ActorLogging}
import akka.routing.{AddRoutee, RemoveRoutee, Routee}
import de.nierbeck.floating.data.domain.TiledVehicle

/**
  * Simple router where we can add and remove routee. This actor is not
  * immutable.
  */
class RouterActor extends Actor with ActorLogging {
  var routees = Set[Routee]()

  def receive: Receive = {
    case ar: AddRoutee => {
      log.info(s"add routee ${ar.routee}")
      routees = routees + ar.routee
    }
    case rr: RemoveRoutee => {
      log.info(s"remove routee ${rr.routee}")
      routees = routees - rr.routee
    }
    case msg:Any => {
      routees.foreach(_.send(msg, sender))
    }
  }
}
