package org.owntracks.android.ui.regions

import dagger.hilt.android.scopes.ActivityScoped
import io.objectbox.query.Query
import org.owntracks.android.data.WaypointModel
import org.owntracks.android.data.repos.WaypointsRepo
import org.owntracks.android.services.LocationProcessor
import org.owntracks.android.ui.base.view.MvvmView
import org.owntracks.android.ui.base.viewmodel.BaseViewModel
import javax.inject.Inject

@ActivityScoped
class RegionsViewModel @Inject constructor(
        private val waypointsRepo: WaypointsRepo,
        private val locationProcessor: LocationProcessor
) : BaseViewModel<MvvmView>() {

    val waypointsList: Query<WaypointModel>
        get() = waypointsRepo.allQuery

    fun delete(model: WaypointModel?) {
        waypointsRepo.delete(model)
    }

    fun exportWaypoints() {
        locationProcessor.publishWaypointsMessage()
    }

}