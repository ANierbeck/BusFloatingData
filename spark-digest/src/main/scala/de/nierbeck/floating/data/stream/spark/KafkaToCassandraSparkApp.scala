package de.nierbeck.floating.data.stream.spark

import java.util.Date

import de.nierbeck.floating.data.domain.{ TiledVehicle, Vehicle }
import de.nierbeck.floating.data.serializer.{ VehicleDecoder, VehicleFstDecoder }
import kafka.serializer.StringDecoder
import org.apache.spark.SparkConf
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{ Seconds, StreamingContext }
import com.datastax.spark.connector.streaming._
import de.nierbeck.floating.data.tiler.TileCalc

/**
 * Created by anierbeck on 09.05.16.
 */
object KafkaToCassandraSparkApp {

  import scala.language.implicitConversions

  def main(args: Array[String]) {

    val consumerTopic = "METRO-Vehicles" //args(0)
    val sparkConf = new SparkConf()
      .setMaster("local[2]")
      .setAppName(getClass.getName)
      .set("spark.cassandra.connection.host", "localhost" /*s"${args(1)}"*/ )
      .set("spark.cassandra.connection.port", "9042" /*"${args(2)}"*/ )
    val consumerProperties = Map("group.id" -> "group1", "bootstrap.servers" -> "localhost:9092" /*args(3)*/ , "auto.offset.reset" -> "smallest")
    val ssc = new StreamingContext(sparkConf, Seconds(10))

    val kafkaStream = KafkaUtils.createDirectStream[String, Vehicle, StringDecoder, VehicleFstDecoder](
      ssc,
      consumerProperties,
      Set(consumerTopic)
    )

    val vehicle = kafkaStream.map { tuple => tuple._2 }.cache()

    vehicle.saveToCassandra("streaming", "vehicles")

    vehicle.filter(x => x.time.isDefined)

    vehicle.map(vehicle => TiledVehicle(
      TileCalc.convertLatLongToQuadKey(vehicle.latitude, vehicle.longitude),
      TileCalc.transformTime(vehicle.time.get),
      vehicle.id,
      vehicle.time,
      vehicle.latitude,
      vehicle.longitude,
      vehicle.heading,
      vehicle.route_id,
      vehicle.run_id,
      vehicle.seconds_since_report
    )).saveToCassandra("streaming", "vehicles_by_tileid")

    ssc.start()
    ssc.awaitTermination()
    ssc.stop()
  }

}
