package de.nierbeck.test.akka.stream

import java.io.{ByteArrayOutputStream, ObjectOutputStream}
import java.util

import org.apache.kafka.common.serialization.Serializer

/**
  * Created by anierbeck on 09.05.16.
  */
class VehicleSerializer extends Serializer[Vehicle] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def serialize(topic: String, data: Vehicle): Array[Byte] = {
    val baos = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(baos)
    oos.writeObject(data)
    oos.close

    baos.toByteArray
  }

  override def close(): Unit = {}
}
