package org.owntracks.android.location

import java.time.Duration

data class LocationRequest(
    var fastestInterval: Duration? = null,
    var smallestDisplacement: Float? = null,
    var numUpdates: Int? = null,
    var expirationDuration: Duration? = null,
    var priority: LocatorPriority = LocatorPriority.BalancedPowerAccuracy,
    val interval: Duration,
    var waitForAccurateLocation: Boolean? = null
)
