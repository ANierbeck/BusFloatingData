package de.nierbeck.test.akka.stream

/**
  * Created by anierbeck on 11.01.16.
  */
case class Route(longitude: Double, latitude: Double, display_name: String, id: String = "none")

case class Routes(items: List[Route])