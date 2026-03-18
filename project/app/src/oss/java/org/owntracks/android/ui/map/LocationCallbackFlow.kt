package org.owntracks.android.ui.map

import android.content.Context
import android.location.Location
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider

fun locationCallbackFlow(context: Context): Flow<Location> = callbackFlow {
  val provider =
      GpsMyLocationProvider(context).apply {
        clearLocationSources()
        addLocationSource("gps")
        addLocationSource("network")
        addLocationSource("passive")
        locationUpdateMinTime = TimeUnit.SECONDS.toMillis(2)
        locationUpdateMinDistance = 1f
      }
  val consumer = IMyLocationConsumer { location: Location?, _: IMyLocationProvider? ->
    location?.let { trySend(it) }
  }
  provider.startLocationProvider(consumer)
  awaitClose { provider.stopLocationProvider() }
}
