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

package de.nierbeck.floating.data.tiler

import java.util.Date

import de.nierbeck.floating.data.domain.BoundingBox
import org.slf4j.{Logger, LoggerFactory}

import scala.annotation.tailrec

object TileCalc {

  val logger = LoggerFactory.getLogger(this.getClass)

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
    calcQuadKey(tileX, tileY, levelOfDetail, "")
  }

  @tailrec
  private def calcQuadKey(tileX:Int, tileY:Int, index: Int, partKey: String): String = {
    index match {
      case 0 => partKey
      case x:Int => {
        val mask = 1 << (index - 1)
        val tileXmask = tileX & mask
        val tileYmask = tileY & mask

        val digit = if (tileXmask != 0 && tileYmask != 0) {
          3
        } else if (tileXmask != 0) {
          1
        } else if (tileYmask != 0) {
          2
        } else {
          0
        }

        calcQuadKey(tileX, tileY, index - 1, partKey + digit)
      }
    }
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

  def convertLatLongToQuadKey(latitude: Double, longitude: Double): String = {
    val tileXY = latLongToTileCoordinate(latitude, longitude)
    tileCoordinateToQuadKey(tileXY._1, tileXY._2)
  }

  //noinspection ScalaStyle
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
        key = keyTranslate(key, index - 1, direction)
      }
    }
    key
  }

  /**
    * calculates the contained tiles within one bounding box. To do so the algorithm crawls from the left top bbox coordinate
    * to the top right coordinate corresponding tile. This is repeated till the right bottom bbox coordinate is reached.
    *
    * @param bBox - enclosing bounding box to calculate the tiles for.
    * @return - a list of tileIDs, as String.
    */
  def convertBBoxToTileIDs(bBox: BoundingBox): Set[String] = {
    logger.debug("calculating tiles for bounding box")
    val tileIDLeftTop = TileCalc.convertLatLongToQuadKey(bBox.leftTop.lat, bBox.leftTop.lon)
    val tileIDRightBottom = TileCalc.convertLatLongToQuadKey(bBox.rightBottom.lat, bBox.rightBottom.lon)

    if (tileIDLeftTop != tileIDRightBottom) {
      val tileIDRightTop = TileCalc.convertLatLongToQuadKey(bBox.leftTop.lat, bBox.rightBottom.lon)
      val tileIDLeftBottom = TileCalc.convertLatLongToQuadKey(bBox.rightBottom.lat, bBox.leftTop.lon)

      if (tileIDLeftTop != tileIDRightTop && tileIDLeftBottom != tileIDRightBottom) {
        var cursor = tileIDLeftTop
        logger.debug(s"cursor: ${cursor}")
        var countRight = 0
        var tiles: Set[String] = Set()
        while (cursor != tileIDRightTop) {
          tiles = tiles + cursor
          cursor = TileCalc.keyTranslate(cursor, cursor.length - 1, Right)
          countRight = countRight + 1;
          logger.debug(s"new cursor: ${cursor}")
        }

        cursor = TileCalc.keyTranslate(tileIDLeftTop, tileIDLeftTop.length - 1, Down)

        logger.debug(s"new cursor: ${cursor}")
        while (cursor != tileIDRightBottom) {
          val startCursor = cursor
          var increment = 0
          while (increment < countRight) {

            tiles = tiles + cursor
            cursor = TileCalc.keyTranslate(cursor, cursor.length - 1, Right)
            increment = increment + 1
            logger.debug(s"new cursor: ${cursor}")
          }
          if (cursor != tileIDRightBottom) {
            cursor = TileCalc.keyTranslate(startCursor, startCursor.length - 1, Down)
          }
        }
        logger.debug(s"Done with tiles: ${tiles}")
        tiles
      } else {
        Set(tileIDLeftTop, tileIDRightBottom)
      }
    } else {
      Set(tileIDLeftTop)
    }
  }

  def transformTime(date: Date): Date = {
    val dt = new org.joda.time.DateTime(date)
    dt.withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0).toDate
  }
}