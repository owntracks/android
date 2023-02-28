package org.owntracks.android.data.repos

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.objectbox.query.Query
import org.owntracks.android.data.WaypointModel
import org.owntracks.android.model.messages.MessageWaypoint
import org.owntracks.android.support.MessageWaypointCollection

abstract class WaypointsRepo protected constructor() {
    enum class Operation {
        INSERT,
        UPDATE,
        DELETE
    }

    data class WaypointAndOperation(val operation: Operation, val waypoint: WaypointModel)

    abstract operator fun get(tst: Long): WaypointModel?
    protected abstract val all: List<WaypointModel>
    abstract val allWithGeofences: List<WaypointModel>
    abstract val allLive: LiveData<List<WaypointModel>>
    abstract val allQuery: Query<WaypointModel?>?

    private val mutableOperations = MutableLiveData<WaypointAndOperation>()
    val operations: LiveData<WaypointAndOperation> = mutableOperations

    fun insert(w: WaypointModel) {
        w.run {
            insert_impl(this)
            mutableOperations.postValue(WaypointAndOperation(Operation.INSERT, this))
        }
    }

    fun update(w: WaypointModel, notify: Boolean) {
        w.run {
            update_impl(this)
            if (notify) {
                mutableOperations.postValue(WaypointAndOperation(Operation.UPDATE, this))
            }
        }
        update_impl(w)
    }

    fun delete(w: WaypointModel) {
        w.run {
            delete_impl(this)
            mutableOperations.postValue(WaypointAndOperation(Operation.DELETE, this))
        }
        delete_impl(w)
    }

    fun importFromMessage(waypoints: MessageWaypointCollection?) {
        if (waypoints == null) return
        for (m in waypoints) {
            // Delete existing waypoint if one with the same tst already exists
            val exisiting = get(m.timestamp)
            exisiting?.let { delete(it) }
            insert(toDaoObject(m))
        }
    }

    fun exportToMessage(): MessageWaypointCollection {
        val messages = MessageWaypointCollection()
        for (waypoint in all) {
            messages.add(fromDaoObject(waypoint))
        }
        return messages
    }

    private fun toDaoObject(messageWaypoint: MessageWaypoint): WaypointModel {
        return WaypointModel(
            0,
            messageWaypoint.timestamp,
            messageWaypoint.description!!,
            messageWaypoint.latitude,
            messageWaypoint.longitude,
            (if (messageWaypoint.radius != null) messageWaypoint.radius else 0)!!,
            0,
            0
        )
    }

    fun fromDaoObject(w: WaypointModel): MessageWaypoint {
        val message = MessageWaypoint()
        message.description = w.description
        message.latitude = w.geofenceLatitude
        message.longitude = w.geofenceLongitude
        message.radius = w.geofenceRadius
        message.timestamp = w.tst
        return message
    }

    protected abstract fun insert_impl(w: WaypointModel?)
    protected abstract fun update_impl(w: WaypointModel?)
    protected abstract fun delete_impl(w: WaypointModel?)
}
