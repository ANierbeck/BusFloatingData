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
