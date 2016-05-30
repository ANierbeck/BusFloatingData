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
import akka.actor.Props
import akka.stream.actor.ActorPublisher
import de.nierbeck.floating.data.domain.Vehicle

object VehiclePublisher {
  def props: Props =  Props(new VehiclePublisher())
}

class VehiclePublisher extends ActorPublisher[Vehicle]{

  override def preStart:Unit = {
    context.system.eventStream.subscribe(self, classOf[Vehicle])
  }

  override def receive: Receive = {
    case vehicle: Vehicle => if (isActive && totalDemand > 0) onNext(vehicle)
  }
}
