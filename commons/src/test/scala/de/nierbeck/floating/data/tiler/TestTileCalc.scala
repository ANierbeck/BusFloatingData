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
