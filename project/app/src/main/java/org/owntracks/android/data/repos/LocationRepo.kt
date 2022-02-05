package org.owntracks.android.data.repos

import android.location.Location
import androidx.lifecycle.MutableLiveData
import org.greenrobot.eventbus.EventBus
import org.owntracks.android.location.LatLng
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepo @Inject constructor(private val eventBus: EventBus) {
    var currentPublishedLocation: MutableLiveData<Location> = MutableLiveData()

    val currentLocationTime: Long
        get() = currentPublishedLocation.value?.time ?: 0

    fun setCurrentPublishedLocation(l: Location) {
        currentPublishedLocation.postValue(l)
        eventBus.postSticky(l)
    }

    var currentMapLocation: LatLng? = null

    fun setMapLocation(location: LatLng) {
        currentMapLocation = location
    }
}