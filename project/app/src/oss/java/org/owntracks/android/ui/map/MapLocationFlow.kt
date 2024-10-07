package org.owntracks.android.ui.map

import android.content.Context
import android.location.Location
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer
import timber.log.Timber

class MapLocationFlow(
    private val locationProviderClient: GpsMyLocationProvider,
) {
  constructor(
      context: Context,
  ) : this(GpsMyLocationProvider(context))

  init {
    Timber.tag("LocationFlow").w("Init locationFlow")
  }

  fun getLocationFlow(coroutineScope: CoroutineScope): Flow<Location> =
      callbackFlow {
            val locationCallback = IMyLocationConsumer { location, source ->
              location?.let { trySend(it) }
            }
            locationProviderClient.apply {
              clearLocationSources()
              addLocationSource("gps")
              addLocationSource("network")
              addLocationSource("passive")
              locationUpdateMinTime = TimeUnit.SECONDS.toMillis(2)
              locationUpdateMinDistance = 1f
              Timber.tag("LocationFlow").w("Starting locationFlow")
              startLocationProvider(locationCallback)
            }
            awaitClose {
              locationProviderClient.stopLocationProvider()
              Timber.tag("LocationFlow").w("Stopping locationFlow")
            }
          }
          .shareIn(
              coroutineScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 2000), replay = 0)
}
