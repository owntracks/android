package org.owntracks.android.ui.map

import android.content.Context
import android.location.Location
import androidx.lifecycle.LiveData
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider

class LocationLiveData(
    private val locationProviderClient: GpsMyLocationProvider,
    private val coroutineScope: CoroutineScope
) :
    LiveData<Location>() {
    constructor(
        context: Context,
        coroutineScope: CoroutineScope
    ) : this(GpsMyLocationProvider(context), coroutineScope)

    private val locationCallback = ThisLocationCallback()

    fun requestLocationUpdates() {
        locationProviderClient.apply {
            clearLocationSources()
            addLocationSource("gps")
            addLocationSource("network")
            addLocationSource("passive")
            locationUpdateMinTime = TimeUnit.SECONDS.toMillis(2)
            locationUpdateMinDistance = 1f
            startLocationProvider(locationCallback)
        }
    }

    private fun removeLocationUpdates() {
        locationProviderClient.stopLocationProvider()
    }

    override fun onActive() {
        super.onActive()
        coroutineScope.launch { requestLocationUpdates() }
    }

    override fun onInactive() {
        coroutineScope.launch { removeLocationUpdates() }
        super.onInactive()
    }

    inner class ThisLocationCallback : IMyLocationConsumer {
        override fun onLocationChanged(location: Location?, source: IMyLocationProvider?) {
            location?.run { value = this }
        }
    }
}
