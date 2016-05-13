package de.nierbeck.floating.data.tiler

object TileCalc {

  val levelOfDetail = 15

  private val MinLatitude = -85.05112878
  private val MaxLatitude = 85.05112878
  private val MinLongitude = -180
  private val MaxLongitude = 180

  private def clip(n: Double, minValue: Double, maxValue: Double): Double = Math.min(Math.max(n, minValue), maxValue)

  private def latLongToTileCoordinate(latitude: Double, longitude: Double): (Int, Int) = {
    val clippedLatitude = clip(latitude, MinLatitude, MaxLatitude)
    val clippedLongitude = clip(longitude, MinLongitude, MaxLongitude)

    val latRad = clippedLatitude * Math.PI / 180
    val n = Math.pow(2, levelOfDetail)
    val xTile = n * ((clippedLongitude + 180) / 360)
    val yTile = n * (1 - (Math.log(Math.tan(latRad) + 1 / Math.cos(latRad)) / Math.PI)) / 2

    (xTile.asInstanceOf[Int], yTile.asInstanceOf[Int])
  }

  private def tileCoordinateToQuadKey(tileX: Int, tileY: Int): String = {
    val quadKey = new StringBuilder()
    var i: Int = levelOfDetail
    while (i > 0) {
      var digit = 0
      val mask = 1 << (i - 1)
      if ((tileX & mask) != 0) {
        digit = digit + 1
      }
      if ((tileY & mask) != 0) {
        digit = digit + 2
      }
      quadKey.append(digit)
      i = i - 1
    }
    quadKey.toString
  }

  def convertLatLongToQuadKey(latitude: Double, longitude: Double): String = {
    val tileXY = latLongToTileCoordinate(latitude, longitude)
    tileCoordinateToQuadKey(tileXY._1, tileXY._2)
  }

  private def keyCharTranslate(keyChar: Char, direction: Direction): Char = {
    keyChar match {
      case '0' =>
        if (horizontal(direction)) '1' else '2'
      case '1' =>
        if (horizontal(direction)) '0' else '3'
      case '2' =>
        if (horizontal(direction)) '3' else '0'
      case '3' =>
        if (horizontal(direction)) '2' else '1'
      case _ => throw new IllegalArgumentException("Unknown direction")
    }
  }

  private def horizontal(direction: Direction) = direction == Left || direction == Right

  def keyTranslate(quadKey: String, index: Int, direction: Direction): String = {

    val savedChar = quadKey.charAt(index)

    val prefix = quadKey.substring(0, index)
    var postfix = ""
    if (index < quadKey.length - 1)
      postfix = quadKey.substring(index + 1)

    var key = prefix + keyCharTranslate(quadKey.charAt(index), direction) + postfix

    if (index > 0) {
      if (((savedChar == '0') && (direction == Left || direction == Up)) ||
        ((savedChar == '1') && (direction == Right || direction == Up)) ||
        ((savedChar == '2') && (direction == Left || direction == Down)) ||
        ((savedChar == '3') && (direction == Right || direction == Down))) {
        key = keyTranslate(key, index - 1, direction);
      }
    }
    key
  }
}
