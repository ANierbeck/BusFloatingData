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

import akka.event.{ Logging, LoggingAdapter }
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{ FlatSpec, Matchers }

/**
 * Created by anierbeck on 18.05.16.
 */
class TestRestService extends FlatSpec with ScalatestRouteTest with Matchers with RestService {

  override val logger = Logging(system, getClass.getName)
  "Endpoing" should "respond to requests" ignore {
    val session = ServiceApp.connect()

    Get("vehicles/boundingBox?bbox=34.94656360293794,-120.20947906250001,33.295920016396764,-116.25440093750001") ~> route(session) ~> check {
      status shouldEqual 200
      //      assertResult()
    }

    ServiceApp.close(session)
  }
}
