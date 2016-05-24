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

import com.typesafe.config.ConfigFactory

object ServiceConfig {

  val config = ConfigFactory.load();

  val serviceInterface = config.getString("service.interface")
  val servicePort = config.getInt("service.port")
  val cassandraNodeName = config.getString("cassandra.host")
  val cassandraNodePort = config.getString("cassandra.port")

}
