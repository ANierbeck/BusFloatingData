package de.nierbeck.test.akka.stream


import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util

import akka.actor.ActorSystem
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.{Input, Output}
import com.twitter.chill.ScalaKryoInstantiator
import kafka.serializer.Encoder
import kafka.utils.VerifiableProperties
import org.apache.kafka.common.serialization.{Deserializer, Serializer}

import scala.reflect.ClassTag

/**
 * Serializes a {@link RawEvent} to a byte-array.
 */

class VehicleSerializer() extends Serializer[Vehicle] /*with Deserializer[T]*/ {
  val kryo = new ScalaKryoInstantiator().newKryo()
  kryo.register(Vehicle.getClass)
  override def serialize(topic: String, r: Vehicle): Array[Byte] = {
    withResource(new Output(new ByteArrayOutputStream()))(output => {
      kryo.writeObject(output, r)
      output.getBuffer
    })

  }

  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def close(): Unit = {}

//  override def deserialize(topic: String, data: Array[Byte]): T = {
//    withResource(new Input(new ByteArrayInputStream(data)))(input => kryo.readObject(input, classOf[T]))
//  }
}
