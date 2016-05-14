package de.nierbeck.floating.data.serializer

import java.io.{ ByteArrayInputStream, ObjectInputStream }

import de.nierbeck.floating.data.domain.Vehicle
import kafka.serializer.Decoder
import kafka.utils.VerifiableProperties
import org.nustaq.serialization.FSTConfiguration

/**
 * Created by anierbeck on 09.05.16.
 */
class VehicleFstDecoder(props: VerifiableProperties) extends Decoder[Vehicle] {
  val fst = FSTConfiguration.createDefaultConfiguration()

  override def fromBytes(bytes: Array[Byte]): Vehicle = {
    fst.asObject(bytes).asInstanceOf[Vehicle]
  }
}