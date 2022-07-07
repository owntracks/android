package org.owntracks.android.ui.preferences.load

import io.objectbox.android.ObjectBoxLiveData
import io.objectbox.query.Query
import org.greenrobot.eventbus.EventBus
import org.owntracks.android.data.WaypointModel
import org.owntracks.android.data.repos.WaypointsRepo

class InMemoryWaypointsRepo(eventBus: EventBus?) : WaypointsRepo(eventBus) {
    override fun get(tst: Long): WaypointModel {
        TODO("Not yet implemented")
    }

    override fun getAll(): MutableList<WaypointModel> {
        TODO("Not yet implemented")
    }

    override fun getAllWithGeofences(): MutableList<WaypointModel> {
        TODO("Not yet implemented")
    }

    override fun getAllLive(): ObjectBoxLiveData<WaypointModel> {
        TODO("Not yet implemented")
    }

    override fun getAllQuery(): Query<WaypointModel> {
        TODO("Not yet implemented")
    }

    override fun insert_impl(w: WaypointModel?) {
        TODO("Not yet implemented")
    }

    override fun update_impl(w: WaypointModel?) {
        TODO("Not yet implemented")
    }

    override fun delete_impl(w: WaypointModel?) {
        TODO("Not yet implemented")
    }
}
