package org.owntracks.android.ui.waypoints

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.owntracks.android.data.WaypointModel
import org.owntracks.android.data.repos.WaypointsRepo
import org.owntracks.android.services.LocationProcessor

@HiltViewModel
class WaypointsViewModel @Inject constructor(
    waypointsRepo: WaypointsRepo,
    private val locationProcessor: LocationProcessor
) : ViewModel() {
    val waypointsList: LiveData<List<WaypointModel>> = waypointsRepo.allLive
    fun exportWaypoints() {
        locationProcessor.publishWaypointsMessage()
    }
}
