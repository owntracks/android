package org.owntracks.android.ui.waypoint

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.owntracks.android.data.repos.LocationRepo
import org.owntracks.android.data.waypoints.WaypointModel
import org.owntracks.android.data.waypoints.WaypointsRepo
import org.owntracks.android.location.LatLng
import org.owntracks.android.location.geofencing.Latitude
import org.owntracks.android.location.geofencing.Longitude
import timber.log.Timber

@HiltViewModel
class WaypointViewModel
@Inject
constructor(private val waypointsRepo: WaypointsRepo, locationRepo: LocationRepo) : ViewModel() {

  private val initialLocation =
      locationRepo.currentBlueDotOnMapLocation?.run { LatLng(latitude, longitude) }
          ?: LatLng(0.0, 0.0)

  val waypoint: LiveData<WaypointModel>
    get() = mutableWaypoint

  private val mutableWaypoint =
      MutableLiveData(
          WaypointModel(
              geofenceLatitude = initialLocation.latitude,
              geofenceLongitude = initialLocation.longitude,
              geofenceRadius = 20))

  fun loadWaypoint(id: Long) {
    viewModelScope.launch {
      Timber.d("Loading waypoint $id")
      waypointsRepo.get(id)?.apply { mutableWaypoint.postValue(this) }
          ?: run { Timber.w("Waypoint $id not found in the repo") }
    }
  }

  fun delete() {
    viewModelScope.launch { waypoint.value?.run { waypointsRepo.delete(this) } }
  }

  fun canDeleteWaypoint(): Boolean {
    return waypoint.value?.id?.run { this != 0L } ?: false
  }

  fun saveWaypoint(description: String, latitude: Latitude, longitude: Longitude, radius: Int) {
    viewModelScope.launch {
      waypointsRepo.insert(
          (waypoint.value ?: WaypointModel()).apply {
            this.description = description
            this.geofenceLatitude = latitude
            this.geofenceLongitude = longitude
            this.geofenceRadius = radius
          })
    }
  }
}
