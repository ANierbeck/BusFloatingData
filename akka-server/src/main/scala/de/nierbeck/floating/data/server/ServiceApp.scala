package de.nierbeck.floating.data.server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives
import akka.pattern.after
import akka.stream.ActorMaterializer
import com.datastax.driver.core.{ Cluster, Session }

import scala.concurrent.duration.{ Duration, DurationInt }
import scala.concurrent.{ Await, ExecutionContext }
import scala.util.{ Failure, Success }
import scala.collection.JavaConversions._

object ServiceApp extends RestService {

  import ServiceConfig._

  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem("service-api-http")
    import system.dispatcher
    implicit val mat = ActorMaterializer()

    val session = connect()

    Http()
      .bindAndHandle(route(session), serviceInterface, servicePort)
      .onComplete {
        case Success(_) => system.log.info(s"Successfully bound to $serviceInterface:$servicePort")
        case Failure(e) => system.log.error(s"Failed !!!! ${e.getMessage}")
      }

    Await.ready(system.whenTerminated, Duration.Inf)
    close(session)
  }

  def connect(): Session = {
    val cluster = Cluster.builder().addContactPoint(cassandraNodeName).withPort(Integer.valueOf(cassandraNodePort)).build()
    val metadata = cluster.getMetadata
    printf(
      "Connected to cluster: %s\n",
      metadata.getClusterName
    )
    metadata.getAllHosts foreach {
      case host =>
        printf(
          "Datatacenter: %s; Host: %s; Rack: %s\n",
          host.getDatacenter, host.getAddress, host.getRack
        )
    }

    cluster.newSession()
  }

  def close(session: Session) {
    val cluster = session.getCluster
    session.close()
    cluster.close()
  }

}
