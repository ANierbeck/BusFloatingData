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

import de.nierbeck.floating.data.domain.{ BoundingBox, LatLon }
import org.scalatest.{ FlatSpec, Matchers }

/**
 * Created by anierbeck on 17.05.16.
 */
class TestTileCalc extends FlatSpec with Matchers {

  "TileCalc" should "create a tile for a given coordinate" in {

    val coordinate1 = (34.12527, -118.2319)
    val coordinate2 = (34.94656360293794, -120.20947906250001)

    //0230123111303100
    val tileId1 = TileCalc.convertLatLongToQuadKey(coordinate1._1, coordinate1._2)
    val tileId2 = TileCalc.convertLatLongToQuadKey(coordinate2._1, coordinate2._2)

    tileId1 should equal("023012311130310")
    tileId2 should equal("023012121222012")

  }

  it should "produce multiple tiles for a boundingBox" in {
    //bbox: 34.94656360293794,-120.20947906250001,33.295920016396764,-116.25440093750001

    val bbox = BoundingBox(LatLon(34.94656360293794f, -120.20947906250001f), LatLon(33.295920016396764f, -116.25440093750001f))

    val tiles: Set[String] = TileCalc.convertBBoxToTileIDs(bbox)

    //    println(tiles)

    tiles should contain("023012311130310")

  }
}
