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

import java.util

import de.nierbeck.floating.data.domain.TiledVehicle
import org.apache.kafka.common.serialization.Serializer
import org.nustaq.serialization.FSTConfiguration

object TiledVehicleFstSerializer {
  val fst = FSTConfiguration.createDefaultConfiguration()
}

class TiledVehicleFstSerializer() extends Serializer[TiledVehicle] {
  import TiledVehicleFstSerializer._

  override def serialize(topic: String, r: TiledVehicle): Array[Byte] = {
    fst.asByteArray(r)
  }

  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def close(): Unit = {}

}
