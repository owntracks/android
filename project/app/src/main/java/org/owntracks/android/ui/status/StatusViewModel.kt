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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Date
import javax.inject.Inject
import org.owntracks.android.R
import org.owntracks.android.data.EndpointState
import org.owntracks.android.data.repos.EndpointStateRepo
import org.owntracks.android.data.repos.LocationRepo

@HiltViewModel
class StatusViewModel @Inject constructor(
    application: Application,
    endpointStateRepo: EndpointStateRepo,
    locationRepo: LocationRepo
) :
    AndroidViewModel(application) {
    val endpointState: LiveData<EndpointState> = endpointStateRepo.endpointStateLiveData
    val endpointQueueLength: LiveData<Int> = endpointStateRepo.endpointQueueLength
    val serviceStarted: LiveData<Date> = endpointStateRepo.serviceStartedDate
    val currentLocation: LiveData<Location> = locationRepo.currentPublishedLocation
    private val powerManager =
        (getApplication<Application>().applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager)
    internal val dozeWhitelisted = MutableLiveData<Boolean>()

    private val mutableLocationPermissions = MutableLiveData(R.string.statusLocationPermissionsUnknown)
    val locationPermissions: LiveData<Int> = mutableLocationPermissions

    fun refreshLocationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val permissions = listOf(
                ACCESS_COARSE_LOCATION,
                ACCESS_FINE_LOCATION,
                ACCESS_BACKGROUND_LOCATION
            ).map { PermissionChecker.checkSelfPermission(getApplication(), it) }
            when (permissions) {
                listOf(
                    PERMISSION_DENIED,
                    PERMISSION_DENIED,
                    PERMISSION_DENIED
                ) -> R.string.statusLocationPermissionsNone
                listOf(
                    PERMISSION_GRANTED,
                    PERMISSION_DENIED,
                    PERMISSION_DENIED
                ) -> R.string.statusLocationPermissionsCoarseForeground
                listOf(
                    PERMISSION_GRANTED,
                    PERMISSION_GRANTED,
                    PERMISSION_DENIED
                ) -> R.string.statusLocationPermissionsFineForeground
                listOf(
                    PERMISSION_GRANTED,
                    PERMISSION_DENIED,
                    PERMISSION_GRANTED
                ) -> R.string.statusLocationPermissionsCoarseBackground
                listOf(
                    PERMISSION_GRANTED,
                    PERMISSION_GRANTED,
                    PERMISSION_GRANTED
                ) -> R.string.statusLocationPermissionsFineBackground
                else -> R.string.statusLocationPermissionsUnknown
            }
        } else {
            val permissions = listOf(
                ACCESS_COARSE_LOCATION,
                ACCESS_FINE_LOCATION
            ).map { PermissionChecker.checkSelfPermission(getApplication(), it) }
            when (permissions) {
                listOf(PERMISSION_DENIED, PERMISSION_DENIED) -> R.string.statusLocationPermissionsNone
                listOf(PERMISSION_GRANTED, PERMISSION_DENIED) -> R.string.statusLocationPermissionsCoarseForeground
                listOf(PERMISSION_GRANTED, PERMISSION_GRANTED) -> R.string.statusLocationPermissionsFineForeground
                else -> R.string.statusLocationPermissionsUnknown
            }
        }.run { mutableLocationPermissions.postValue(this) }
    }

    fun getDozeWhitelisted(): LiveData<Boolean> = dozeWhitelisted
    fun refreshDozeModeWhitelisted() {
        dozeWhitelisted.postValue(isIgnoringBatteryOptimizations())
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        return powerManager.isIgnoringBatteryOptimizations(
            getApplication<Application>().applicationContext.packageName
        )
    }
}
