package de.nierbeck.test.akka.stream


import java.io.ByteArrayOutputStream
import java.util

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Output
import com.twitter.chill.ScalaKryoInstantiator
import org.apache.kafka.common.serialization.Serializer

class VehicleSerializer() extends Serializer[Vehicle] {
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

}
