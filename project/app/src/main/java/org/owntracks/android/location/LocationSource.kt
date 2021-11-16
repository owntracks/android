package org.owntracks.android.location

import android.location.Location
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider
import org.owntracks.android.ui.map.MapLocationSource

interface OnLocationChangedListener {
    fun onLocationChanged(location: Location)
}

/**
 * Wraps an OSMDroid location consumer object in a more generic [OnLocationChangedListener]
 * This then gives a way for a fragment that's coerced a generic [MapLocationSource] into an
 * OSMDroid-specific [IMyLocationProvider] to be able to invoke the callback inside the [MapLocationSource]
 * by turning it into an [IMyLocationConsumer]
 *
 * @return
 */
fun IMyLocationConsumer.toOnLocationChangedListener(): OnLocationChangedListener {
    return object : OnLocationChangedListener {
        override fun onLocationChanged(location: Location) {
            return this@toOnLocationChangedListener.onLocationChanged(location, null)
        }

    }
}

/**
 * Converts a generic [LocationSource] into an [IMyLocationProvider] for use with an OSMDroid map
 *
 * @return
 */
fun MapLocationSource.toOSMLocationSource(): IMyLocationProvider {
    return object : IMyLocationProvider {
        override fun startLocationProvider(myLocationConsumer: IMyLocationConsumer?): Boolean {
            myLocationConsumer?.also { this@toOSMLocationSource.activate(it.toOnLocationChangedListener()); return true }
            return false
        }

        override fun stopLocationProvider() {
            this@toOSMLocationSource.deactivate()
        }

        override fun getLastKnownLocation(): Location? =
            this@toOSMLocationSource.getLastKnownLocation()

        override fun destroy() {
            this@toOSMLocationSource.deactivate()
        }
    }
}