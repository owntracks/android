package org.owntracks.android.location

data class LocationRequest(
    var fastestInterval: Long? = null,
    var smallestDisplacement: Float? = null,
    var numUpdates: Int? = null,
    var expirationDuration: Long? = null,
    var priority: Int = PRIORITY_BALANCED_POWER_ACCURACY,
    var interval: Long? = null,
    var waitForAccurateLocation: Boolean? = null,
) {
    companion object {
        const val PRIORITY_HIGH_ACCURACY: Int = 0
        const val PRIORITY_BALANCED_POWER_ACCURACY: Int = 1
        const val PRIORITY_LOW_POWER: Int = 2
        const val PRIORITY_NO_POWER: Int = 3
    }
}