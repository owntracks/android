package org.owntracks.android.location.geofencing

class Geofence private constructor(
        val requestId: String?,
        val transitionTypes: Int?,
        val notificationResponsiveness: Int?,
        val circularLatitude: Double?,
        val circularLongitude: Double?,
        val circularRadius: Float?,
        val expirationDuration: Long?
) {
    class Builder(
            var requestId: String? = null,
            var transitionTypes: Int? = null,
            var notificationResponsiveness: Int? = null,
            var circularLatitude: Double? = null,
            var circularLongitude: Double? = null,
            var circularRadius: Float? = null,
            var expirationDuration: Long? = null
    ) {
        fun build() = Geofence(requestId, transitionTypes, notificationResponsiveness, circularLatitude, circularLongitude, circularRadius, expirationDuration)
        fun requestId(requestId: String) = apply { this.requestId = requestId }
        fun transitionTypes(transitionTypes: Int) = apply { this.transitionTypes = transitionTypes }
        fun notificationResponsiveness(notificationResponsiveness: Int) = apply { this.notificationResponsiveness = notificationResponsiveness }
        fun circularRegion(latitude: Double, longitude: Double, radius: Float) = apply {
            this.circularLatitude = latitude
            this.circularLongitude = longitude
            this.circularRadius = radius
        }

        fun expirationDuration(expirationDuration: Long) = apply { this.expirationDuration = expirationDuration }
    }

    companion object {
        const val GEOFENCE_TRANSITION_ENTER: Int = 0
        const val GEOFENCE_TRANSITION_EXIT: Int = 1
        const val NEVER_EXPIRE: Int = Int.MAX_VALUE
    }
}
