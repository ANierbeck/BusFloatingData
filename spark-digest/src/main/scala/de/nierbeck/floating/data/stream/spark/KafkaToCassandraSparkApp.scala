package de.nierbeck.floating.data.stream.spark

import java.util.{ Date, Properties }

import de.nierbeck.floating.data.domain.{ TiledVehicle, Vehicle }
import de.nierbeck.floating.data.serializer.{ TiledVehicleEncoder, VehicleDecoder, VehicleFstDecoder }
import kafka.serializer.StringDecoder
import org.apache.spark.SparkConf
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{ Seconds, StreamingContext }
import com.datastax.spark.connector.streaming._
import de.nierbeck.floating.data.tiler.TileCalc
import org.apache.kafka.clients.producer.{ KafkaProducer, Producer, ProducerRecord }
import org.apache.kafka.clients.producer._
import org.apache.spark.streaming.dstream.DStream

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

    val producerConf = new Properties()
    producerConf.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")
    producerConf.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    producerConf.put("bootstrap.servers", "localhost:9092")

    val ssc = new StreamingContext(sparkConf, Seconds(10))

    val kafkaStream = KafkaUtils.createDirectStream[String, Vehicle, StringDecoder, VehicleFstDecoder](
      ssc,
      consumerProperties,
      Set(consumerTopic)
    )

    val vehicle = kafkaStream.map { tuple => tuple._2 }.cache()

    vehicle.saveToCassandra("streaming", "vehicles")

    vehicle.filter(x => x.time.isDefined)

    val tiledVehicle = vehicle.map(vehicle => TiledVehicle(
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
    ))

    tiledVehicle.cache()

    tiledVehicle.saveToCassandra("streaming", "vehicles_by_tileid")

    tiledVehicle.foreachRDD(rdd => rdd.foreachPartition(f = tiledVehicles => {

      val producer: Producer[String, Array[Byte]] = new KafkaProducer[String, Array[Byte]](producerConf)

      tiledVehicles.foreach { tiledVehicle =>
        val message = new ProducerRecord[String, Array[Byte]]("tiledVehicles", null, new TiledVehicleEncoder().toBytes(tiledVehicle))
        producer.send(message)
      }

      producer.close()

    }))

    ssc.start()
    ssc.awaitTermination()
    ssc.stop()
  }

}
