package de.nierbeck.test.akka.stream

case class RouteInfo(id: String, display_name: String)

case class RouteInfos(items: List[RouteInfo])
