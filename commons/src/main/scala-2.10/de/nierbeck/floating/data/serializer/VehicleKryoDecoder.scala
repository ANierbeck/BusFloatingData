/*
 * Copyright 2016 Achim Nierbeck
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.nierbeck.floating.data.serializer

import java.io.ByteArrayInputStream
import java.util

import com.esotericsoftware.kryo.io.Input
import com.twitter.chill.ScalaKryoInstantiator
import de.nierbeck.floating.data.domain.Vehicle
import kafka.serializer.Decoder
import kafka.utils.VerifiableProperties
import org.apache.kafka.common.serialization.Deserializer

object VehicleKryoDecoder {
  val kryo = new ScalaKryoInstantiator().newKryo()
}

class VehicleKryoDecoder(props: VerifiableProperties) extends Decoder[Vehicle] {
  import VehicleKryoDecoder._

  kryo.register(classOf[Vehicle], 1)

  override def fromBytes(bytes: Array[Byte]): Vehicle = {
    withResource(new Input(new ByteArrayInputStream(bytes)))(input => kryo.readObject(input, classOf[Vehicle]))
  }

}
