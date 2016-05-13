package de.nierbeck.floating.data.domain

case class RouteInfo(id: String, display_name: String)

case class RouteInfos(items: List[RouteInfo])
