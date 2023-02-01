package org.owntracks.android.ui.regions

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.owntracks.android.data.WaypointModel
import org.owntracks.android.data.repos.WaypointsRepo
import org.owntracks.android.services.LocationProcessor
import javax.inject.Inject

@HiltViewModel
class RegionsViewModel @Inject constructor(
    private val waypointsRepo: WaypointsRepo,
    private val locationProcessor: LocationProcessor
) : ViewModel() {
    val waypointsList: LiveData<List<WaypointModel>> = waypointsRepo.allLive

    fun delete(model: WaypointModel) {
        waypointsRepo.delete(model)
    }

    fun exportWaypoints() {
        locationProcessor.publishWaypointsMessage()
    }
}
