package de.nierbeck.floating.data.serializer

import java.util.Date

import de.nierbeck.floating.data.domain.Vehicle
import org.scalatest.{ FlatSpec, Matchers }

/**
 * Created by anierbeck on 14.05.16.
 */
class TestKryoSerialization extends FlatSpec with Matchers {

  "Vehicle" should "be serialized via Kryo and back deserialized again" in {
    val vehicle = Vehicle("1", Some(new Date()), 45.0, 45.0, 90, Some("id"))

    val serializer = new VehicleKryoSerializer
    val deserializer = new VehicleKryoDeserializer

    val serialized = serializer.serialize("test", vehicle)
    val deserializedVehicle = deserializer.deserialize("test", serialized)

    deserializedVehicle should equal(vehicle)
  }

}
