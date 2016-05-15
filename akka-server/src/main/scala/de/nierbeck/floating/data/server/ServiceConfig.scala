package de.nierbeck.floating.data.server

import com.typesafe.config.ConfigFactory

object ServiceConfig {

  val config = ConfigFactory.load();

  val serviceInterface = config.getString("service.interface")
  val servicePort = config.getInt("service.port")
  val cassandraNodeName = config.getString("cassandra.host")
  val cassandraNodePort = config.getString("cassandra.port")

}
