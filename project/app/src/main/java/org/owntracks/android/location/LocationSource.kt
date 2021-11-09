package org.owntracks.android.location

import android.location.Location
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider

/**
 * An abstracted Map Location Source that can be used across OSM and Google Maps
 */
interface LocationSource {
    fun activate(onLocationChangedListener: OnLocationChangedListener)
    fun reactivate()
    fun deactivate()
    interface OnLocationChangedListener {
        fun onLocationChanged(location: Location)
    }

    fun getLastKnownLocation(): Location?

}

/**
 * Wraps an OSMDroid location consumer object in a more generic [LocationSource.OnLocationChangedListener]
 * This then gives a way for a fragment that's coerced a generic [LocationSource] into an
 * OSMDroid-specific [IMyLocationProvider] to be able to invoke the callback inside the [LocationSource]
 * by turning it into an [IMyLocationConsumer]
 *
 * @return
 */
fun IMyLocationConsumer.toOnLocationChangedListener(): LocationSource.OnLocationChangedListener {
    return object : LocationSource.OnLocationChangedListener {
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
fun LocationSource.toOSMLocationSource(): IMyLocationProvider {
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