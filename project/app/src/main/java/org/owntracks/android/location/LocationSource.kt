package org.owntracks.android.location

import android.location.Location
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider

interface LocationSource {
    fun activate(onLocationChangedListener: OnLocationChangedListener)
    fun reactivate()
    fun deactivate()
    interface OnLocationChangedListener {
        fun onLocationChanged(location: Location)
    }

    fun getLastKnownLocation(): Location?

}

fun IMyLocationConsumer.toOnLocationChangedListener(): LocationSource.OnLocationChangedListener {
    return object : LocationSource.OnLocationChangedListener {
        override fun onLocationChanged(location: Location) {
            return this@toOnLocationChangedListener.onLocationChanged(location, null)
        }

    }
}

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