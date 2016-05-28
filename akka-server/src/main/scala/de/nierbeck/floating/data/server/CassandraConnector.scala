/*
 * Copyright 2016 Achim Nierbeck
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.nierbeck.floating.data.server

import com.datastax.driver.core.{Cluster, Host, Metadata, Session}
import scala.collection.JavaConversions._

/**
  * Created by anierbeck on 26.05.16.
  */
object CassandraConnector {

  import ServiceConfig._

  def connect(): Session = {
    val cluster = Cluster.builder().addContactPoint(cassandraNodeName).withPort(Integer.valueOf(cassandraNodePort)).build()
    val metadata:Metadata = cluster.getMetadata
//    logger.info("Connected to cluster: {}", metadata.getClusterName)
    println(s"Connected to cluster: ${metadata.getClusterName}")
    metadata.getAllHosts foreach {
      case host: Host =>
        println(s"Datatacenter: ${host.getDatacenter}; Host: ${host.getAddress}; Rack: ${host.getRack}")
//        logger.info("Datatacenter: {}; Host: {}; Rack: {}", host.getDatacenter, host.getAddress, host.getRack)
    }

    cluster.newSession()
  }

  def close(session: Session) {
    val cluster = session.getCluster
    session.close()
    cluster.close()
  }

}
