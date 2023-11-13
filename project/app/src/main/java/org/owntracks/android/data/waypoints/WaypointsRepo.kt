package org.owntracks.android.data.waypoints

import androidx.lifecycle.LiveData
import java.time.Instant
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.owntracks.android.model.messages.MessageWaypoint
import org.owntracks.android.support.MessageWaypointCollection

abstract class WaypointsRepo protected constructor() {
    sealed class WaypointOperation {
        data class Insert(val waypoint: WaypointModel) : WaypointOperation()
        data class Update(val waypoint: WaypointModel) : WaypointOperation()
        data class Delete(val waypoint: WaypointModel) : WaypointOperation()
        object Clear : WaypointOperation()
    }

    abstract suspend fun get(id: Long): WaypointModel?
    abstract suspend fun getByTst(instant: Instant): WaypointModel?
    abstract val all: List<WaypointModel>
    abstract val allLive: LiveData<List<WaypointModel>>

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
                getByTst(it.first)?.run {
                    delete(this)
                }
                insert(toDaoObject(it.second))
            }
    }

    fun exportToMessage(): MessageWaypointCollection =
        MessageWaypointCollection().apply { addAll(all.map(::fromDaoObject)) }

    private fun toDaoObject(messageWaypoint: MessageWaypoint): WaypointModel {
        return WaypointModel(
            0,
            messageWaypoint.description ?: "",
            messageWaypoint.latitude,
            messageWaypoint.longitude,
            messageWaypoint.radius ?: 0,
            Instant.MIN,
            0,
            Instant.ofEpochSecond(messageWaypoint.timestamp)
        )
    }

    fun fromDaoObject(w: WaypointModel): MessageWaypoint {
        val message = MessageWaypoint()
        message.description = w.description
        message.latitude = w.geofenceLatitude
        message.longitude = w.geofenceLongitude
        message.radius = w.geofenceRadius
        message.timestamp = w.tst.epochSecond
        return message
    }

    protected abstract suspend fun clearImpl()
    protected abstract suspend fun insertImpl(waypointModel: WaypointModel)
    protected abstract suspend fun updateImpl(waypointModel: WaypointModel)
    protected abstract suspend fun deleteImpl(waypointModel: WaypointModel)
    abstract val migrationCompleteFlow: StateFlow<Boolean>
}
