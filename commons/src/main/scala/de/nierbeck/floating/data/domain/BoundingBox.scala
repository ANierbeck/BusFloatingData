package de.nierbeck.floating.data.domain

case class LonLat(lon: Float, lat: Float)

case class BoundingBox(leftTop: LonLat, rightBotom: LonLat)
