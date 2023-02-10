package org.owntracks.android.ui.region

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.owntracks.android.data.WaypointModel
import org.owntracks.android.data.repos.LocationRepo
import org.owntracks.android.data.repos.WaypointsRepo
import timber.log.Timber

@HiltViewModel
class RegionViewModel @Inject constructor(
    private val waypointsRepo: WaypointsRepo,
    private val locationRepo: LocationRepo
) : ViewModel() {
    val waypoint = MutableLiveData(getEmptyWaypoint())

    private fun getEmptyWaypoint(): WaypointModel = WaypointModel().apply {
        val currentLocation = locationRepo.currentBlueDotOnMapLocation
        if (currentLocation != null) {
            geofenceLatitude = currentLocation.latitude
            geofenceLongitude = currentLocation.longitude
        } else {
            geofenceLatitude = 0.0
            geofenceLongitude = 0.0
        }
    }

    fun loadWaypoint(id: Long) {
        Timber.d("Loading waypoint $id")
        waypointsRepo[id]?.apply {
            waypoint.postValue(this)
        } ?: run {
            Timber.w("Waypoint $id not found in the repo")
        }
    }

    fun delete() {
        waypointsRepo.delete(waypoint.value)
    }

    fun canSaveWaypoint(): Boolean {
        return waypoint.value?.description?.isNotEmpty() ?: false
    }

    fun canDeleteWaypoint(): Boolean {
        return waypoint.value?.id?.run { this != 0L } ?: false
    }

    fun saveWaypoint() {
        if (canSaveWaypoint()) {
            waypoint.value?.run(waypointsRepo::insert)
        }
    }
}
