package org.owntracks.android.ui.waypoints

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.owntracks.android.data.waypoints.WaypointModel
import org.owntracks.android.data.waypoints.WaypointsRepo
import org.owntracks.android.services.LocationProcessor

@HiltViewModel
class WaypointsViewModel @Inject constructor(
    waypointsRepo: WaypointsRepo,
    private val locationProcessor: LocationProcessor
) : ViewModel() {
    val waypointsList: LiveData<List<WaypointModel>> = waypointsRepo.allLive

    fun exportWaypoints() {
        viewModelScope.launch {
            locationProcessor.publishWaypointsMessage()
        }
    }
}
