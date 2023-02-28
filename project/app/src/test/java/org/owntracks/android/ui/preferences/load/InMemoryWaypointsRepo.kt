package org.owntracks.android.ui.preferences.load

import androidx.lifecycle.LiveData
import io.objectbox.query.Query
import org.owntracks.android.data.WaypointModel
import org.owntracks.android.data.repos.WaypointsRepo

class InMemoryWaypointsRepo : WaypointsRepo() {
    private val waypoints = mutableListOf<WaypointModel>()
    override fun get(tst: Long): WaypointModel? = waypoints.firstOrNull { it.tst == tst }
    override val all: List<WaypointModel>
        get() = TODO("Not yet implemented")
    override val allWithGeofences: List<WaypointModel>
        get() = TODO("Not yet implemented")
    override val allLive: LiveData<List<WaypointModel>>
        get() = TODO("Not yet implemented")
    override val allQuery: Query<WaypointModel?>?
        get() = TODO("Not yet implemented")

    fun getAll() = waypoints

    override fun insert_impl(w: WaypointModel?) {
        waypoints.add(w!!)
    }

    override fun update_impl(w: WaypointModel?) {
        TODO("Not yet implemented")
    }

    override fun delete_impl(w: WaypointModel?) {
        TODO("Not yet implemented")
    }
}
