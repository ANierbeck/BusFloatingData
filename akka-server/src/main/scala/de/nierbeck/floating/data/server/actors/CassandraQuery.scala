package de.nierbeck.floating.data.server.actors
import akka.actor.{Actor, ActorLogging}
import com.datastax.driver.core.Session
import de.nierbeck.floating.data.server.CassandraConnector


sealed trait CassandraQuery extends Actor with ActorLogging {

  val session: Session = CassandraConnector.connect()

  override def postStop(): Unit = {
    CassandraConnector.close(session)
  }
}
