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
