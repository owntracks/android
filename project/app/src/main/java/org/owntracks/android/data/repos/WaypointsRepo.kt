package org.owntracks.android.data.repos

import io.objectbox.android.ObjectBoxLiveData
import io.objectbox.query.Query
import org.owntracks.android.data.WaypointModel
import org.owntracks.android.support.MessageWaypointCollection

abstract class WaypointsRepo protected constructor() {
    abstract operator fun get(tst: Long): WaypointModel?
    protected abstract val all: List<WaypointModel>
    abstract val allWithGeofences: List<WaypointModel>
    abstract val allLive: ObjectBoxLiveData<WaypointModel>
    abstract val allQuery: Query<WaypointModel>

    fun insert(waypoint: WaypointModel) {
        insertImpl(waypoint)
    }

    fun update(waypoint: WaypointModel) {
        updateImpl(waypoint)
    }

    fun delete(waypoint: WaypointModel) {
        deleteImpl(waypoint)
    }

    fun importFromMessage(waypoints: MessageWaypointCollection?) {
        if (waypoints == null) return
        waypoints.forEach {
            // Delete existing waypoint if one with the same tst already exists
            get(it.timestamp)?.run(this::delete)
            insert(it.toWaypoint())
        }
    }

    fun exportToMessage(): MessageWaypointCollection = MessageWaypointCollection().apply {
        all.forEach { this.add(it.toMessageWaypoint()) }
    }

    protected abstract fun insertImpl(waypoint: WaypointModel)
    protected abstract fun updateImpl(waypoint: WaypointModel)
    protected abstract fun deleteImpl(waypoint: WaypointModel)
    abstract fun reset()
}