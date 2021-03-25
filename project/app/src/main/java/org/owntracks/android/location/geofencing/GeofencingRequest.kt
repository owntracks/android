package org.owntracks.android.location.geofencing

class GeofencingRequest private constructor(val initialTrigger: Int?, val geofences: List<Geofence>?) {
    data class Builder(var initialTrigger: Int? = null, var geofences: List<Geofence>? = null) {
        fun addGeofences(geofences: List<Geofence>) = apply { this.geofences = geofences }
        fun setInitialTrigger(initialTrigger: Int) = apply { this.initialTrigger = initialTrigger }
        fun build() = GeofencingRequest(initialTrigger, geofences)
    }
}
