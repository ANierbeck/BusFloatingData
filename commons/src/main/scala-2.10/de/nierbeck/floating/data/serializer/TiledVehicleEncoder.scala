package de.nierbeck.floating.data.serializer

import java.io.ByteArrayOutputStream

import de.nierbeck.floating.data.domain.TiledVehicle
import org.nustaq.serialization.FSTConfiguration
import kafka.serializer.Encoder
import kafka.utils.VerifiableProperties

class TiledVehicleEncoder(props: VerifiableProperties = null) extends Encoder[TiledVehicle] {
  val fst = FSTConfiguration.createDefaultConfiguration()

  override def toBytes(tiledVehicle: TiledVehicle): Array[Byte] = {
    fst.asByteArray(tiledVehicle)
  }

}