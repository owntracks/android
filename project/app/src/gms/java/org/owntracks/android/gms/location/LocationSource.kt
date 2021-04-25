package org.owntracks.android.gms.location

import android.location.Location
import com.google.android.gms.maps.LocationSource


fun org.owntracks.android.location.LocationSource.toGMSLocationSource(): LocationSource {
    return object : LocationSource {
        override fun activate(gmsLocaniotChangedListener: LocationSource.OnLocationChangedListener?) {
            this@toGMSLocationSource.activate(object : org.owntracks.android.location.LocationSource.OnLocationChangedListener {
                override fun onLocationChanged(location: Location?) {
                    gmsLocaniotChangedListener?.onLocationChanged(location)
                }
            })
        }

        override fun deactivate() {
            this@toGMSLocationSource.deactivate()
        }
    }
}