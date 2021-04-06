package org.owntracks.android.location.geofencing

data class Geofence(
        val requestId: String? = null,
        val transitionTypes: Int? = null,
        val notificationResponsiveness: Int? = null,
        val circularLatitude: Double? = null,
        val circularLongitude: Double? = null,
        val circularRadius: Float? = null,
        val expirationDuration: Long? = null,
        val loiteringDelay: Int? = null,
) {
    companion object {
        const val GEOFENCE_TRANSITION_ENTER: Int = 1
        const val GEOFENCE_TRANSITION_EXIT: Int = 2
        const val GEOFENCE_TRANSITION_DWELL: Int = 4
        const val NEVER_EXPIRE: Long = Long.MAX_VALUE
    }
}
