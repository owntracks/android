package org.owntracks.android.location

class LocationRequest {

    var fastestInterval: Long = 20000L
        private set

    fun setFastestInterval(value: Long): LocationRequest {
        this.fastestInterval = value
        return this
    }

    var smallestDisplacement: Float = 0F
        private set

    fun setSmallestDisplacement(value: Float): LocationRequest {
        this.smallestDisplacement = value
        return this
    }

    var numUpdates: Int = Int.MAX_VALUE
        private set

    fun setNumUpdates(value: Int): LocationRequest {
        this.numUpdates = value
        return this
    }

    var expirationDuration: Long = Long.MAX_VALUE
        private set

    fun setExpirationDuration(value: Long): LocationRequest {
        this.expirationDuration = value
        return this
    }

    var priority: Int = 1
        private set

    fun setPriority(value: Int): LocationRequest {
        this.priority = value
        return this
    }

    var interval: Long = 60000
        private set

    fun setInterval(value: Long): LocationRequest {
        this.interval = value
        return this
    }

    companion object {
        const val PRIORITY_HIGH_ACCURACY: Int = 0
        const val PRIORITY_BALANCED_POWER_ACCURACY: Int = 1
        const val PRIORITY_LOW_POWER: Int = 2
        const val PRIORITY_NO_POWER: Int = 3
    }
}


