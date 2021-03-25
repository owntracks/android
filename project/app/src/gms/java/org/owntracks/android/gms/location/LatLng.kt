package org.owntracks.android.gms.location

import org.owntracks.android.location.LatLng

fun LatLng.toGMSLatLngConversion(): com.google.android.gms.maps.model.LatLng {
    return com.google.android.gms.maps.model.LatLng(latitude, longitude)
}
