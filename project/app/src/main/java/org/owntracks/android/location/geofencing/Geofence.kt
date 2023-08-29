package org.owntracks.android.location.geofencing

class Latitude(private val latitude: Double) {
    val value: Double
        get() = when {
            latitude % 360 <= 90 -> {
                latitude % 360
            }
            latitude % 360 <= 270 -> {
                180 + (-1 * (latitude % 360))
            }
            else -> -360 + (latitude % 360)
        }
}

data class Longitude(private val longitude: Double) {
    val value: Double
        get() = ((longitude + 180) % 360) - 180
}

data class Geofence(
    val requestId: String? = null,
    val transitionTypes: Int? = null,
    val notificationResponsiveness: Int? = null,
    val circularLatitude: Latitude? = null,
    val circularLongitude: Longitude? = null,
    val circularRadius: Float? = null,
    val expirationDuration: Long? = null,
    val loiteringDelay: Int? = null
) {
    companion object {
        const val GEOFENCE_TRANSITION_UNKNOWN = 0
        const val GEOFENCE_TRANSITION_ENTER = 1
        const val GEOFENCE_TRANSITION_EXIT = 2
        const val GEOFENCE_TRANSITION_DWELL = 4
        const val NEVER_EXPIRE: Long = Long.MAX_VALUE
    }
}
