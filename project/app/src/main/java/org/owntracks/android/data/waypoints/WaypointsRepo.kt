package org.owntracks.android.data.waypoints

import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import org.owntracks.android.location.geofencing.Latitude
import org.owntracks.android.location.geofencing.Longitude
import org.owntracks.android.model.messages.MessageWaypoint
import org.owntracks.android.support.MessageWaypointCollection
import timber.log.Timber

abstract class WaypointsRepo protected constructor() {
  sealed class WaypointOperation {
    data class Insert(val waypoint: WaypointModel) : WaypointOperation()

    data class Update(val waypoint: WaypointModel) : WaypointOperation()

    data class Delete(val waypoint: WaypointModel) : WaypointOperation()

    data object Clear : WaypointOperation()
  }

  abstract suspend fun get(id: Long): WaypointModel?

  abstract suspend fun getByTst(instant: Instant): WaypointModel?

  abstract val all: List<WaypointModel>
  abstract val allLive: Flow<List<WaypointModel>>

  private val mutableOperations = MutableSharedFlow<WaypointOperation>()
  val operations: SharedFlow<WaypointOperation> = mutableOperations

  suspend fun insert(waypointModel: WaypointModel) {
    waypointModel.run {
      insertImpl(this@run)
      mutableOperations.emit(WaypointOperation.Insert(this@run))
    }
  }

  suspend fun update(waypointModel: WaypointModel, notify: Boolean) {
    waypointModel.run {
      updateImpl(this@run)
      if (notify) {
        mutableOperations.emit(WaypointOperation.Update(this@run))
      }
    }
  }

  suspend fun delete(waypointModel: WaypointModel) {
    waypointModel.run {
      deleteImpl(this@run)
      mutableOperations.emit(WaypointOperation.Delete(this@run))
    }
  }

  suspend fun clearAll() {
    clearImpl()
    mutableOperations.emit(WaypointOperation.Clear)
  }

  /**
   * Imports all the waypoints in a given message, replacing
   *
   * @param waypoints
   */
  suspend fun importFromMessage(waypoints: MessageWaypointCollection?) {
    waypoints
        ?.map { Instant.ofEpochSecond(it.timestamp) to it }
        ?.forEach {
          getByTst(it.first)?.run { delete(this) }
          // check if the latitude and longitude are valid, otherwise do not replace the waypoint
          if ((it.second.latitude >= -90.0) &&
              (it.second.latitude <= 90.0) &&
              (it.second.longitude >= -180.0) &&
              (it.second.longitude <= 180.0)) {
            insert(toDaoObject(it.second))
          } else {
            // log a warning for the waypoint that isn't being imported
            Timber.w("Ignoring waypoint with invalid coordinates: %s", it.second.description)
          }
        }
  }

  private fun toDaoObject(messageWaypoint: MessageWaypoint): WaypointModel {
    return WaypointModel(
        0,
        messageWaypoint.description ?: "",
        Latitude(messageWaypoint.latitude),
        Longitude(messageWaypoint.longitude),
        messageWaypoint.radius ?: 0,
        Instant.MIN,
        0,
        Instant.ofEpochSecond(messageWaypoint.timestamp))
  }

  fun fromDaoObject(w: WaypointModel): MessageWaypoint {
    return MessageWaypoint().apply {
      description = w.description
      latitude = w.geofenceLatitude.value
      longitude = w.geofenceLongitude.value
      radius = w.geofenceRadius
      timestamp = w.tst.epochSecond
    }
  }

  protected abstract suspend fun clearImpl()

  protected abstract suspend fun insertImpl(waypointModel: WaypointModel)

  protected abstract suspend fun updateImpl(waypointModel: WaypointModel)

  protected abstract suspend fun deleteImpl(waypointModel: WaypointModel)

  abstract val migrationCompleteFlow: StateFlow<Boolean>
}
