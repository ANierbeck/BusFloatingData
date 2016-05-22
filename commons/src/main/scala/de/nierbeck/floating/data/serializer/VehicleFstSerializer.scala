package de.nierbeck.floating.data.serializer

import java.util

import de.nierbeck.floating.data.domain.Vehicle
import org.apache.kafka.common.serialization.Serializer
import org.nustaq.serialization.FSTConfiguration

class VehicleFstSerializer() extends Serializer[Vehicle] {
  val fst = FSTConfiguration.createDefaultConfiguration()

  override def serialize(topic: String, r: Vehicle): Array[Byte] = {
    fst.asByteArray(r)
  }

  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def close(): Unit = {}

}
