package org.owntracks.android.location.geofencing

import android.content.Intent
import android.location.Location
import timber.log.Timber

data class GeofencingEvent(
    val errorCode: Int?,
    val geofenceTransition: Int?,
    val triggeringGeofences: List<Geofence>?,
    val triggeringLocation: Location?
) {
    fun hasError(): Boolean = errorCode != null && errorCode >= 0

    companion object {
        @JvmStatic
        @Suppress("UNUSED_PARAMETER")
        fun fromIntent(intent: Intent): GeofencingEvent? {
            Timber.w("Decoding a geofencing event from an intent on non-GMS currently not supported")
            return null
        }
    }
}
