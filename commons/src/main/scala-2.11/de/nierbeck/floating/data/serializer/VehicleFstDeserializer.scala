package de.nierbeck.floating.data.serializer

import java.io.ByteArrayInputStream
import java.util

import com.esotericsoftware.kryo.io.Input
import com.twitter.chill.ScalaKryoInstantiator
import de.nierbeck.floating.data.domain.Vehicle
import org.apache.kafka.common.serialization.Deserializer
import org.nustaq.serialization.FSTConfiguration

class VehicleFstDeserializer() extends Deserializer[Vehicle] {
  val fst = FSTConfiguration.createDefaultConfiguration()

  override def deserialize(topic: String, data: Array[Byte]): Vehicle = {
    fst.asObject(data).asInstanceOf[Vehicle]
  }

  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def close(): Unit = {}

}
