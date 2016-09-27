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

import java.io.ByteArrayOutputStream
import java.util

import com.esotericsoftware.kryo.io.Output
import com.twitter.chill.ScalaKryoInstantiator
import de.nierbeck.floating.data.domain.Vehicle
import org.apache.kafka.common.serialization.Serializer

object VehicleKryoSerializer {
  val kryo = new ScalaKryoInstantiator().newKryo()
}

class VehicleKryoSerializer() extends Serializer[Vehicle] {
  import VehicleKryoSerializer._

  kryo.register(classOf[Vehicle], 1)

  override def serialize(topic: String, r: Vehicle): Array[Byte] = {
    withResource(new Output(new ByteArrayOutputStream()))(output => {
      kryo.writeObject(output, r)
      output.getBuffer
    })

  }

  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def close(): Unit = {}

}
