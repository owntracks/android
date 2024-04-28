package org.owntracks.android.location

import android.content.Context
import org.owntracks.android.location.geofencing.GeofencingClient
import org.owntracks.android.location.geofencing.GeofencingRequest

class NoopGeofencingClient : GeofencingClient {
  override fun removeGeofences(context: Context) {}

  override fun addGeofences(request: GeofencingRequest, context: Context) {}
}
