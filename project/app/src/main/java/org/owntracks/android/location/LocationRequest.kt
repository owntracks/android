package org.owntracks.android.location

data class LocationRequest(
        var fastestInterval: Long = 20000L,
        var smallestDisplacement: Float = 0F,
        var numUpdates: Int = Int.MAX_VALUE,
        var expirationDuration: Long = Long.MAX_VALUE,
        var priority: Int = PRIORITY_BALANCED_POWER_ACCURACY,
        var interval: Long = 60_000,
) {
    companion object {
        const val PRIORITY_HIGH_ACCURACY: Int = 0
        const val PRIORITY_BALANCED_POWER_ACCURACY: Int = 1
        const val PRIORITY_LOW_POWER: Int = 2
        const val PRIORITY_NO_POWER: Int = 3
    }
}


