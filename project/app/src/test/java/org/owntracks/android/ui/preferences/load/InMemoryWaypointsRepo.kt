package org.owntracks.android.ui.preferences.load

import androidx.lifecycle.LiveData
import io.objectbox.query.Query
import org.owntracks.android.data.WaypointModel
import org.owntracks.android.data.repos.WaypointsRepo

class InMemoryWaypointsRepo() : WaypointsRepo() {
    private val waypoints = mutableListOf<WaypointModel>()
    override fun get(tst: Long): WaypointModel? = waypoints.firstOrNull { it.tst == tst }

    public override fun getAll() = waypoints

    override fun getAllWithGeofences(): MutableList<WaypointModel> {
        TODO("Not yet implemented")
    }

    override fun getAllLive(): LiveData<List<WaypointModel>> {
        TODO("Not yet implemented")
    }

    override fun getAllQuery(): Query<WaypointModel> {
        TODO("Not yet implemented")
    }

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
