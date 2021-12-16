package org.owntracks.android.data.repos

import android.location.Location
import androidx.lifecycle.MutableLiveData
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepo @Inject constructor() {
    var currentPublishedLocation: MutableLiveData<Location> = MutableLiveData()

    val currentLocationTime: Long
        get() = currentPublishedLocation.value?.time ?: 0

    fun setCurrentPublishedLocation(l: Location) {
        Timber.d("Setting current location to $l on ${Thread.currentThread()}")
        try {
            // If we're on the right thread, let's just update this value synchronously
            currentPublishedLocation.value = l
        } catch (e: IllegalStateException) {
            currentPublishedLocation.postValue(l)
        }
    }

    var currentMapLocation: Location? = null

    fun setMapLocation(location: Location) {
        currentMapLocation = location
    }
}