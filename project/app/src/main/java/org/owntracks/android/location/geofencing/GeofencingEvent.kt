package org.owntracks.android.location.geofencing

import android.content.Intent
import android.location.Location

data class GeofencingEvent(val errorCode:String, val geofenceTransition:Int, val triggeringGeofences:List<Geofence>, val triggeringLocation:Location) {
    fun hasError(): Boolean {
        TODO("Not yet implemented")
    }

    companion object {
        @JvmStatic
        fun fromIntent(intent: Intent): GeofencingEvent {
            TODO("Not yet implemented")
        }
    }
}
