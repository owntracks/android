package org.owntracks.android.ui.status

import android.app.Application
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.PowerManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import org.owntracks.android.data.EndpointState
import org.owntracks.android.data.repos.EndpointStateRepo
import org.owntracks.android.data.repos.LocationRepo
import org.owntracks.android.ui.status.logs.LogViewerActivity
import java.util.*
import javax.inject.Inject

@HiltViewModel
class StatusViewModel @Inject constructor(
    application: Application,
    endpointStateRepo: EndpointStateRepo,
    locationRepo: LocationRepo
) :
    AndroidViewModel(application) {
    val endpointState: LiveData<EndpointState> = endpointStateRepo.endpointState
    val endpointQueueLength: LiveData<Int> = endpointStateRepo.endpointQueueLength
    val serviceStarted: LiveData<Date> = endpointStateRepo.serviceStartedDate
    val currentLocation: LiveData<Location> = locationRepo.currentPublishedLocation
    internal val dozeWhitelisted: MutableLiveData<Boolean> = MutableLiveData()

    fun getDozeWhitelisted(): LiveData<Boolean> = dozeWhitelisted

    fun refreshDozeModeWhitelisted() {
        dozeWhitelisted.postValue(isIgnoringBatteryOptimizations())
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            (getApplication<Application>().applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(
                getApplication<Application>().applicationContext.packageName
            )
    }

    fun viewLogs() {
        val intent =
            Intent(
                getApplication<Application>().applicationContext,
                LogViewerActivity::class.java
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().applicationContext.startActivity(intent)
    }
}
