package de.nierbeck.floating.data.serializer

import java.io.{ByteArrayInputStream, ObjectInputStream}
import java.util

import de.nierbeck.floating.data.domain.Vehicle
import org.apache.kafka.common.serialization.Deserializer

/**
 * Created by anierbeck on 09.05.16.
 */
class VehicleDeserializer extends Deserializer[Vehicle] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def close(): Unit = {}

  override def deserialize(topic: String, data: Array[Byte]): Vehicle = {
    val ois = new ObjectInputStream(new ByteArrayInputStream(data))
    ois.readObject().asInstanceOf[Vehicle]
  }
}
