package de.nierbeck.floating.data.domain

case class LatLon(lat: Float, lon: Float)

case class BoundingBox(leftTop: LatLon, rightBotom: LatLon)
