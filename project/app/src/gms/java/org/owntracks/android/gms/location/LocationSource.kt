package org.owntracks.android.gms.location

import android.location.Location
import com.google.android.gms.maps.LocationSource

/**
 * Converts a generic [org.owntracks.android.location.LocationSource] to a GMS-specific [LocationSource]
 * that can then be passed to specific GMS components that need it.
 */
fun org.owntracks.android.location.LocationSource.toGMSLocationSource(): LocationSource {
    return object : LocationSource {
        override fun activate(gmsLocationChangedListener: LocationSource.OnLocationChangedListener?) {
            this@toGMSLocationSource.activate(object :
                org.owntracks.android.location.LocationSource.OnLocationChangedListener {
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