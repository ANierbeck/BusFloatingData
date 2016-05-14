package de.nierbeck.floating.data.serializer

import java.io._
import java.util.Date
import java.nio.file.{ Files, Paths }

import de.nierbeck.floating.data.domain.Vehicle
import org.scalatest.{ FlatSpec, Matchers }

/**
 * Created by anierbeck on 14.05.16.
 */
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
