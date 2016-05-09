package de.nierbeck.test.akka.stream

import java.io.ByteArrayInputStream
import java.util

import com.esotericsoftware.kryo.io.Input
import com.twitter.chill.ScalaKryoInstantiator
import org.apache.kafka.common.serialization.Deserializer

class VehicleDeserializer() extends Deserializer[Vehicle] {
  val kryo = new ScalaKryoInstantiator().newKryo()
  kryo.register(Vehicle.getClass)

  override def deserialize(topic: String, data: Array[Byte]): Vehicle = {
    withResource(new Input(new ByteArrayInputStream(data)))(input => kryo.readObject(input, classOf[Vehicle]))
  }

  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def close(): Unit = {}


}
