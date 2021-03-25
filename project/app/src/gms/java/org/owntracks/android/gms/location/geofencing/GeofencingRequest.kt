package org.owntracks.android.gms.location.geofencing

import org.owntracks.android.location.geofencing.Geofence
import org.owntracks.android.location.geofencing.GeofencingRequest


fun GeofencingRequest.toGMSGeofencingRequest(): com.google.android.gms.location.GeofencingRequest {
    return com.google.android.gms.location.GeofencingRequest.Builder()
            .addGeofences(geofences!!.map { it.toGMSGeofence() }.toMutableList())
            .setInitialTrigger(initialTrigger!!)
            .build()
}

fun Geofence.toGMSGeofence(): com.google.android.gms.location.Geofence {
    return com.google.android.gms.location.Geofence.Builder()
            .setRequestId(this.requestId!!)
            .setCircularRegion(this.circularLatitude!!, this.circularLongitude!!, this.circularRadius!!)
            .setExpirationDuration(this.expirationDuration!!)
            .setTransitionTypes(this.transitionTypes!!)
            .setNotificationResponsiveness(this.notificationResponsiveness!!)
            .build()
}