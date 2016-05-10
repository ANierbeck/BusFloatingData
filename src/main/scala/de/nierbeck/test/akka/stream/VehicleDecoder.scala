package de.nierbeck.test.akka.stream

import java.io.{ByteArrayInputStream, ObjectInputStream}
import java.util
import kafka.serializer.Decoder

import org.apache.kafka.common.serialization.Deserializer

/**
  * Created by anierbeck on 09.05.16.
  */
class VehicleDecoder extends Decoder[Vehicle]{
  override def fromBytes(bytes: Array[Byte]): Vehicle = {
    val ois = new ObjectInputStream(new ByteArrayInputStream(bytes))
    ois.readObject().asInstanceOf[Vehicle]
  }
}
