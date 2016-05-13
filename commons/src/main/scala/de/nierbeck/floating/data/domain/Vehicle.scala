package de.nierbeck.floating.data.domain

import java.util.Date

case class Vehicle(id: String, time: Option[Date] = None, latitude: Double, longitude: Double, heading: Integer, route_id: Option[String] = None, run_id: String = "none", seconds_since_report: Integer = 0)