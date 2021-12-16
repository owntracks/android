package org.owntracks.android.ui.region

import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.scopes.ActivityScoped
import org.owntracks.android.data.WaypointModel
import org.owntracks.android.data.repos.LocationRepo
import org.owntracks.android.data.repos.WaypointsRepo
import org.owntracks.android.ui.base.view.MvvmView
import org.owntracks.android.ui.base.viewmodel.BaseViewModel
import timber.log.Timber
import javax.inject.Inject

@ActivityScoped
class RegionViewModel @Inject constructor(
        private val waypointsRepo: WaypointsRepo,
        private val locationRepo: LocationRepo
) : BaseViewModel<MvvmView>() {
    val waypoint = MutableLiveData(getEmptyWaypoint())

    private fun getEmptyWaypoint(): WaypointModel =
            WaypointModel().apply {
                val currentLocation = locationRepo.currentMapLocation
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
        waypointsRepo[id]?.also {
            waypoint.postValue(it)
        } ?: run {
            Timber.w("Waypoint $id not found in the repo")
        }
    }

    fun canSaveWaypoint(): Boolean {
        return waypoint.value?.description?.isNotEmpty() ?: false
    }

    fun saveWaypoint() {
        if (canSaveWaypoint()) {
            waypointsRepo.insert(waypoint.value)
        }
    }
}