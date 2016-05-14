package de.nierbeck.floating.data.stream.spark

import de.nierbeck.floating.data.domain.Vehicle
import de.nierbeck.floating.data.serializer.VehicleDecoder
import kafka.serializer.StringDecoder
import org.apache.spark.SparkConf
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{ Seconds, StreamingContext }
import com.datastax.spark.connector.streaming._

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

    //    val stream = KafkaUtils.createStream[String, String, StringDecoder, StringDecoder](
    //      ssc, kafka.kafkaParams, Map(topic -> 1), StorageLevel.MEMORY_ONLY)

    val kafkaStream = KafkaUtils.createDirectStream[String, Vehicle, StringDecoder, VehicleDecoder](
      ssc,
      consumerProperties,
      Set(consumerTopic)
    )

    kafkaStream.map { case (topic: String, vehicle: Vehicle) => vehicle }.saveToCassandra("streaming", "vehicles")

    ssc.start()
    ssc.awaitTermination()
    ssc.stop()
  }

}
