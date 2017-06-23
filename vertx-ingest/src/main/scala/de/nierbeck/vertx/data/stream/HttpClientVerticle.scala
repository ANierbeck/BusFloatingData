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

package de.nierbeck.vertx.data.stream

import io.vertx.lang.scala.ScalaVerticle
import io.vertx.scala.ext.web.client.WebClient
import io.vertx.scala.ext.web.codec.BodyCodec
import de.nierbeck.floating.data.domain.{RouteInfos, Routes, Vehicle}
import com.englishtown.vertx.cassandra.CassandraSession
import com.englishtown.vertx.cassandra.impl.DefaultCassandraSession
import io.vertx.core.{AbstractVerticle, Context, Vertx}

import scala.util.{Failure, Success}

class HttpClientVerticle extends ScalaVerticle{

  val cassandraSession:CassandraSession;

  override def init(vertx: Vertx, context: Context): Unit = {
    super.init(vertx,context);
    cassandraSession = new DefaultCassandraSession(new Cluster.Builder(), new JsonCassandraConfigurator(vertx), vertx)
  }

  override def start(): Unit = {
    cassandraSession.onReady((v) => {
      def foo(v) = {
        System.out.printf("==> CASSANDRA SESSION INITIALIZED\n\t[%b]\n", cassandraSession.initialized)
        startFuture.complete
      }

      foo(v)
    })

    var client = WebClient.create(vertx)
    client.get(80, "api.metro.net", "/agencies/lametro/routes/").as(BodyCodec.json(classOf[RouteInfos])).sendFuture().onComplete{
      case Success(result) => {
        var response = result
        var routeInfos = response.body()
        println(s"Received route infos")
        routeInfos.items.foreach{ routeInfo => {
          println(s"RouteInfo: ${routeInfo}")

        }}
      }
      case Failure(cause) => {
        println(s"$cause")
      }
    }

    println("It's the end ...")
  }

}
