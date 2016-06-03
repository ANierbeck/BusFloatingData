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

import java.io._
import java.util.Date
import java.nio.file.{ Files, Paths }

import de.nierbeck.floating.data.domain.Vehicle
import org.scalatest.{ FlatSpec, Matchers }

class TestKryoSerialization extends FlatSpec with Matchers {

  "Vehicle" should "be serialized via Kryo and back deserialized again" ignore {
    val vehicle = Vehicle("1", Some(new Date()), 45.0, 45.0, 90, Some("id"))

    val serializer = new VehicleKryoSerializer
    val deserializer = new VehicleKryoDeserializer

    val serialized = serializer.serialize("test", vehicle)

    val tmpFile = File.createTempFile("kryotest-", ".tmp")
    tmpFile.deleteOnExit()

    val bos = new BufferedOutputStream(new FileOutputStream(tmpFile))
    Stream.continually(bos.write(serialized))
    bos.close() // You may end up with 0 bytes file if not calling close.

    val byteArray = Files.readAllBytes(Paths.get(tmpFile.getPath))

    val deserializedVehicle = deserializer.deserialize("test", byteArray)

    deserializedVehicle should equal(vehicle)
  }

}
