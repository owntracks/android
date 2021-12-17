package org.owntracks.android.ui.preferences.load

import io.objectbox.android.ObjectBoxLiveData
import io.objectbox.query.Query
import org.owntracks.android.data.WaypointModel
import org.owntracks.android.data.repos.WaypointsRepo

class InMemoryWaypointsRepo : WaypointsRepo() {
    override fun get(tst: Long): WaypointModel {
        TODO("Not yet implemented")
    }

    override val all: List<WaypointModel>
        get() = TODO("Not yet implemented")
    override val allWithGeofences: List<WaypointModel>
        get() = TODO("Not yet implemented")
    override val allLive: ObjectBoxLiveData<WaypointModel>
        get() = TODO("Not yet implemented")
    override val allQuery: Query<WaypointModel>
        get() = TODO("Not yet implemented")

    override fun insertImpl(waypoint: WaypointModel) {
        TODO("Not yet implemented")
    }

    override fun updateImpl(waypoint: WaypointModel) {
        TODO("Not yet implemented")
    }

    override fun deleteImpl(waypoint: WaypointModel) {
        TODO("Not yet implemented")
    }

    override fun reset() {
        TODO("Not yet implemented")
    }

}