package org.owntracks.android.ui.status

import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Application
import android.content.Context
import android.location.Location
import android.os.Build
import android.os.PowerManager
import androidx.core.content.PermissionChecker
import androidx.core.content.PermissionChecker.PERMISSION_DENIED
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.owntracks.android.R
import org.owntracks.android.data.EndpointState
import org.owntracks.android.data.repos.EndpointStateRepo
import org.owntracks.android.data.repos.LocationRepo

@HiltViewModel
class StatusViewModel
@Inject
constructor(
    application: Application,
    endpointStateRepo: EndpointStateRepo,
    locationRepo: LocationRepo
) : AndroidViewModel(application) {
  val endpointState: StateFlow<EndpointState> = endpointStateRepo.endpointState
  val endpointQueueLength: StateFlow<Int> = endpointStateRepo.endpointQueueLength
  val serviceStarted: StateFlow<Instant> = endpointStateRepo.serviceStartedDate
  val currentLocation: StateFlow<Location?> = locationRepo.currentPublishedLocation
  private val powerManager =
      (getApplication<Application>().applicationContext.getSystemService(Context.POWER_SERVICE)
          as PowerManager)
  internal val dozeWhitelisted = MutableStateFlow(false)

  private val mutableLocationPermissions =
      MutableStateFlow(R.string.statusLocationPermissionsUnknown)
  val locationPermissions: StateFlow<Int> = mutableLocationPermissions

  fun refreshLocationPermissions() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          val permissions =
              listOf(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION, ACCESS_BACKGROUND_LOCATION).map {
                PermissionChecker.checkSelfPermission(getApplication(), it)
              }
          when (permissions) {
            listOf(PERMISSION_DENIED, PERMISSION_DENIED, PERMISSION_DENIED) ->
                R.string.statusLocationPermissionsNone
            listOf(PERMISSION_GRANTED, PERMISSION_DENIED, PERMISSION_DENIED) ->
                R.string.statusLocationPermissionsCoarseForeground
            listOf(PERMISSION_GRANTED, PERMISSION_GRANTED, PERMISSION_DENIED) ->
                R.string.statusLocationPermissionsFineForeground
            listOf(PERMISSION_GRANTED, PERMISSION_DENIED, PERMISSION_GRANTED) ->
                R.string.statusLocationPermissionsCoarseBackground
            listOf(PERMISSION_GRANTED, PERMISSION_GRANTED, PERMISSION_GRANTED) ->
                R.string.statusLocationPermissionsFineBackground
            else -> R.string.statusLocationPermissionsUnknown
          }
        } else {
          val permissions =
              listOf(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION).map {
                PermissionChecker.checkSelfPermission(getApplication(), it)
              }
          when (permissions) {
            listOf(PERMISSION_DENIED, PERMISSION_DENIED) -> R.string.statusLocationPermissionsNone
            listOf(PERMISSION_GRANTED, PERMISSION_DENIED) ->
                R.string.statusLocationPermissionsCoarseForeground
            listOf(PERMISSION_GRANTED, PERMISSION_GRANTED) ->
                R.string.statusLocationPermissionsFineForeground
            else -> R.string.statusLocationPermissionsUnknown
          }
        }
        .run { mutableLocationPermissions.value = this }
  }

  fun getDozeWhitelisted(): StateFlow<Boolean> = dozeWhitelisted

  fun refreshDozeModeWhitelisted() {
    dozeWhitelisted.value = isIgnoringBatteryOptimizations()
  }

  private fun isIgnoringBatteryOptimizations(): Boolean {
    return powerManager.isIgnoringBatteryOptimizations(
        getApplication<Application>().applicationContext.packageName)
  }
}
