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

package de.nierbeck.floating.data.server.actors.rest

import akka.actor.Props
import com.datastax.driver.core.ResultSet
import de.nierbeck.floating.data.domain.VehicleClusterDetails
import de.nierbeck.floating.data.server.actors.CassandraQuery

import scala.collection.JavaConversions._

object HotSpotDetailsActor {
  def props():Props = Props(new HotSpotDetailsActor())
}

class HotSpotDetailsActor extends CassandraQuery {

  val selectHotSpotDetails = session.prepare("SELECT * FROM streaming.vehicleclusterdetails WHERE id = ?")

  override def receive: Receive = {
    case hotSpotId:Long => {
      sender() ! retrieveHotSpotDetails(hotSpotId)
    }
    case _ => log.error("Wrong request")
  }

  def retrieveHotSpotDetails(hotSpotId: Long): List[VehicleClusterDetails] = {
    val futureResult:ResultSet = session.execute(selectHotSpotDetails.bind(hotSpotId.asInstanceOf[java.lang.Long]))

    val hotSpotDetailsFuture:List[VehicleClusterDetails] = futureResult.map{ row =>
          VehicleClusterDetails(row.getLong("id"), row.getInt("pos_id"), row.getLong("time_stamp"), row.getDouble("latitude"), row.getDouble("longitude"))
    }.toList
    hotSpotDetailsFuture
  }

}
