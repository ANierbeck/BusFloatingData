package de.nierbeck.floating.data.stream.digest

import java.util.Date

import akka.actor.ActorSystem
import akka.event.Logging
import akka.kafka.ConsumerSettings
import akka.kafka.scaladsl._
import akka.stream.ActorMaterializer
import com.datastax.driver.core.{ Cluster, PreparedStatement, Session }
import de.nierbeck.floating.data.domain.Vehicle
import de.nierbeck.floating.data.serializer.VehicleKryoDeserializer
import de.nierbeck.floating.data.tiler.TileCalc
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer

/**
 * Created by anierbeck on 09.05.16.
 */
object KafkaToCassandraApp {

  implicit val system = ActorSystem("stream-system")
  implicit val actorMaterializer = ActorMaterializer()

  private val log = Logging(system, getClass.getName)

  val cluster: Cluster = Cluster.builder().addContactPoint("localhost").withPort(9042).build()
  val cassandraSession: Session = cluster.connect()

  val vehiclesStatement: PreparedStatement = cassandraSession.prepare("INSERT INTO streaming.vehicles(id, time, longitude, latitude, heading, route_id, run_id, seconds_since_report) VALUES(?, ?, ?, ?, ?, ?, ?, ?);")
  val vehiclesTiledStatement: PreparedStatement = cassandraSession.prepare("INSERT INTO streaming.vehicles_by_tileid(tileid, timeid, vehicle_id, time, longitude, latitude, heading, route_id, run_id, seconds_since_report) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")

  //Kafka
  val consumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new VehicleKryoDeserializer,
    Set("METRO-Vehicles"))
    .withBootstrapServers("localhost:9092")
    .withGroupId("group1")
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  def main(args: Array[String]): Unit = {
    new KafkaToCassandraApp(system).run()
  }
}

class KafkaToCassandraApp(system: ActorSystem) {

  import KafkaToCassandraApp._
  import TileCalc._

  def run(): Unit = {
    val source = Consumer.atMostOnceSource(consumerSettings.withClientId("Akka-Client"))
    source.map(message => message.value).runForeach(vehicle => {
      store(vehicle.asInstanceOf[Vehicle])
      calcTileAndStore(vehicle.asInstanceOf[Vehicle])
    })
  }

  private def calcTimeId(date: Date): Date = {
    val dt = new org.joda.time.DateTime(date)
    dt.withMinuteOfHour(0)
    dt.withSecondOfMinute(0)
    dt.toDate
  }

  def calcTileAndStore(vehicle: Vehicle): Unit = {
    val tileId = convertLatLongToQuadKey(vehicle.longitude, vehicle.latitude)
    val timeId = calcTimeId(vehicle.time.getOrElse(new java.util.Date()))
    val statement = vehiclesTiledStatement.bind(
      tileId,
      timeId,
      vehicle.id,
      vehicle.time.getOrElse(new java.util.Date()),
      vehicle.longitude.asInstanceOf[Object],
      vehicle.latitude.asInstanceOf[Object],
      vehicle.heading,
      vehicle.route_id.getOrElse(null),
      vehicle.run_id,
      vehicle.seconds_since_report
    )
  }

  def store(vehicle: Vehicle): Unit = {
    log.info(s"storing vehicle in cassandra: ${vehicle}")

    val statement = vehiclesStatement.bind(
      vehicle.id,
      vehicle.time.getOrElse(new java.util.Date()),
      vehicle.longitude.asInstanceOf[Object],
      vehicle.latitude.asInstanceOf[Object],
      vehicle.heading,
      vehicle.route_id.getOrElse(null),
      vehicle.run_id,
      vehicle.seconds_since_report
    )
    log.info(s"Statement: ${statement}")

    if (cassandraSession.isClosed) {
      log.error("Session is already closed")
      throw new RuntimeException("session already closed ...")
    }
    try {
      log.info(s"executing statement with session: $cassandraSession")
      cassandraSession.execute(statement)
    } catch {
      case e: Exception => log.error(s"Exception: ${e.getMessage}", e)
    }
  }
}