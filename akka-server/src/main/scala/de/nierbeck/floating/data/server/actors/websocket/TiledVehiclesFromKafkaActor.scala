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

package de.nierbeck.floating.data.server.actors.websocket

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.kafka.scaladsl.Consumer
import akka.stream.ActorMaterializer
import de.nierbeck.floating.data.serializer.TiledVehicleFstDeserializer
import org.apache.kafka.common.serialization.ByteArrayDeserializer

import scala.reflect.ClassTag


/**
  * Just a simple router, which collects some VM stats and sends them to the provided
  * actorRef each interval.
  */

object TiledVehiclesFromKafkaActor {

  def props(router:ActorRef, topic:String, specializer: Stream ):Props = Props(new TiledVehiclesFromKafkaActor(router, topic, specializer))

}

class TiledVehiclesFromKafkaActor(router: ActorRef, topic: String, specializer: Stream) extends Actor with ActorLogging {

  import de.nierbeck.floating.data.server.ServiceConfig._
  implicit val materializer = ActorMaterializer()


  //Kafka
  val consumerSettings = ConsumerSettings(context.system, new ByteArrayDeserializer, new TiledVehicleFstDeserializer)
    .withBootstrapServers(kafkaConnect)
    .withGroupId("group1")


  val source = Consumer.atMostOnceSource(consumerSettings.withClientId("Akka-Client"), Subscriptions.topics(topic))
  source
    .map(message => message.value)
    .runForeach(vehicle => {
      val forwardVehicle = specializer match {
        case SPARK => SparkVehicles(vehicle)
        case FLINK => FlinkVehicles(vehicle)
      }
      router ! forwardVehicle
    })

  override def receive: Actor.Receive = {
    case _ => // just ignore any messages
  }
}
