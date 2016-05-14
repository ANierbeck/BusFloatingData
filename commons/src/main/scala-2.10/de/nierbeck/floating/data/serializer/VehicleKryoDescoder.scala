package de.nierbeck.floating.data.serializer

import java.io.ByteArrayInputStream
import java.util

import com.esotericsoftware.kryo.io.Input
import com.twitter.chill.ScalaKryoInstantiator
import de.nierbeck.floating.data.domain.Vehicle
import kafka.serializer.Decoder
import kafka.utils.VerifiableProperties
import org.apache.kafka.common.serialization.Deserializer

class VehicleKryoDecoder(props: VerifiableProperties) extends Decoder[Vehicle] {
  val kryo = new ScalaKryoInstantiator().newKryo()
  kryo.register(classOf[Vehicle])

  override def fromBytes(bytes: Array[Byte]): Vehicle = {
    withResource(new Input(new ByteArrayInputStream(bytes)))(input => kryo.readObject(input, classOf[Vehicle]))
  }

}
