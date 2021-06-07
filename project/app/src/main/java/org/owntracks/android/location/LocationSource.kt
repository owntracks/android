package org.owntracks.android.location

import android.location.Location

interface LocationSource {
    fun activate(onLocationChangedListener: OnLocationChangedListener)
    fun reactivate()
    fun deactivate()
    interface OnLocationChangedListener {
        fun onLocationChanged(location: Location)
    }
}