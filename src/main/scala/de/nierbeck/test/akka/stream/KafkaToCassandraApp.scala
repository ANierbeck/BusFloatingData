package de.nierbeck.test.akka.stream

import akka.actor.ActorSystem
import akka.event.Logging
import akka.kafka.ConsumerSettings
import akka.stream.ActorMaterializer
import com.datastax.driver.core.{Cluster, PreparedStatement, ResultSetFuture, Session}
import akka.kafka.scaladsl._
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.clients.consumer.ConsumerConfig

import scala.concurrent.Promise
import scala.concurrent.Future

/**
  * Created by anierbeck on 09.05.16.
  */
object KafkaToCassandraApp {

  implicit val system = ActorSystem("stream-system")
  implicit val actorMaterializer = ActorMaterializer()

  private val log = Logging(system, getClass.getName)

  val cluster: Cluster = Cluster.builder().addContactPoint("localhost").withPort(9042).build()
  val cassandraSession: Session = cluster.connect()

  val routeStatement: PreparedStatement = cassandraSession.prepare("INSERT INTO streaming.routes(id, route_id, longitude, latitude, display_name) VALUES(?, ?, ?, ?, ?);")
  val vehiclesStatement: PreparedStatement = cassandraSession.prepare("INSERT INTO streaming.vehicles(id, time, longitude, latitude, heading, route_id, run_id, seconds_since_report) VALUES(?, ?, ?, ?, ?, ?, ?, ?);")
  val routeInfoStatement: PreparedStatement = cassandraSession.prepare("INSERT INTO streaming.routeInfos(id, display_name) VALUES(?,?);")

  //Kafka
  val consumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new VehicleDeserializer,
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

  def run(): Unit = {

//    Consumer.plainSource(consumerSettings.withClientId("Akka-Client")).runForeach(cr => {
//      val vehicle: Vehicle = cr.value()
//      log.info(s"storing vehicle in cassandra: ${vehicle}")
//      cassandraSession.executeAsync(vehiclesStatement.bind(
//        vehicle.id,
//        vehicle.time,
//        vehicle.longitude.asInstanceOf[Object],
//        vehicle.latitude.asInstanceOf[Object],
//        vehicle.heading,
//        vehicle.route_id,
//        vehicle.run_id,
//        vehicle.seconds_since_report))
//    })

//    val x = Consumer.atMostOnceSource(consumerSettings.withClientId("Akka-Client")).mapAsync(1){ record => Future(record.value)}
//    x.runForeach(vehicle => store(vehicle))

    val source = Consumer.atMostOnceSource(consumerSettings.withClientId("Akka-Client"))
    source.map(message => message.value).runForeach(vehicle => store(vehicle))
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
      vehicle.seconds_since_report)
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