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

import java.io.File

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.kafka.ConsumerSettings
import akka.kafka.scaladsl.Consumer
import akka.stream.{ActorMaterializer, Materializer}
import de.nierbeck.floating.data.serializer.TiledVehicleFstDeserializer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer

import scala.concurrent.duration.FiniteDuration


/**
  * Just a simple router, which collects some VM stats and sends them to the provided
  * actorRef each interval.
  */

object TiledVehiclesFromKafkaActor {

  def props(router:ActorRef):Props = Props(new TiledVehiclesFromKafkaActor(router))

}

class TiledVehiclesFromKafkaActor(router: ActorRef) extends Actor with ActorLogging {

  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val materializer = ActorMaterializer()


  //Kafka
  val consumerSettings = ConsumerSettings(context.system, new ByteArrayDeserializer, new TiledVehicleFstDeserializer,
    Set("tiledVehicles"))
    .withBootstrapServers("localhost:9092")
    .withGroupId("group1")

  val source = Consumer.atMostOnceSource(consumerSettings.withClientId("Akka-Client"))
  source.map(message => message.value).runForeach(vehicle => router ! vehicle)

  override def receive: Actor.Receive = {
    case _ => // just ignore any messages
  }
}
