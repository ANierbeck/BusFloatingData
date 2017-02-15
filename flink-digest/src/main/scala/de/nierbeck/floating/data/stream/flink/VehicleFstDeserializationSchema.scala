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

package de.nierbeck.floating.data.stream.flink

import de.nierbeck.floating.data.domain.Vehicle
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.java.typeutils.TypeExtractor
import org.apache.flink.streaming.util.serialization.DeserializationSchema
import org.nustaq.serialization.FSTConfiguration

object VehicleFstDeserializationSchema {
  val fst = FSTConfiguration.createDefaultConfiguration()
}

class VehicleFstDeserializationSchema extends DeserializationSchema[Vehicle] {

  import VehicleFstDeserializationSchema._

  override def deserialize(message: Array[Byte]): Vehicle = {
    fst.asObject(message).asInstanceOf[Vehicle]
  }

  override def isEndOfStream(nextElement: Vehicle) = true

  override def getProducedType:TypeInformation[Vehicle] = {
    TypeExtractor.createTypeInfo(classOf[Vehicle])
  }
}
