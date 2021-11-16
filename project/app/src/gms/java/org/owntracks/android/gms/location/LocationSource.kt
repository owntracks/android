package org.owntracks.android.gms.location

import android.location.Location
import com.google.android.gms.maps.LocationSource
import org.owntracks.android.location.OnLocationChangedListener
import org.owntracks.android.ui.map.MapLocationSource

/**
 * Converts a generic [org.owntracks.android.location.LocationSource] to a GMS-specific [LocationSource]
 * that can then be passed to specific GMS components that need it.
 */
fun MapLocationSource.toGMSLocationSource(): LocationSource {
    return object : LocationSource {
        override fun activate(gmsLocationChangedListener: LocationSource.OnLocationChangedListener?) {
            this@toGMSLocationSource.activate(object :
                OnLocationChangedListener {
                override fun onLocationChanged(location: Location) {
                    gmsLocationChangedListener?.onLocationChanged(location)
                }
            })
        }

        override fun deactivate() {
            this@toGMSLocationSource.deactivate()
        }
    }
}