package org.owntracks.android.location

import org.owntracks.android.location.gms.toGMSLatLngConversion

data class LatLng(val latitude: Double, val longitude: Double) {
    fun toGMSLatLng(): com.google.android.gms.maps.model.LatLng {
        return this.toGMSLatLngConversion()
    }
}


