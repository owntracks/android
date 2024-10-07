package org.owntracks.android.ui.map

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import java.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import org.owntracks.android.gms.location.toGMSLocationRequest
import org.owntracks.android.location.LocationRequest
import org.owntracks.android.location.LocatorPriority
import timber.log.Timber

class MapLocationFlow(private val locationProviderClient: FusedLocationProviderClient) {
  constructor(
      context: Context,
  ) : this(LocationServices.getFusedLocationProviderClient(context))

  init {
    Timber.tag("LocationFlow").w("Init locationFlow")
  }

  @SuppressLint("MissingPermission")
  fun getLocationFlow(coroutineScope: CoroutineScope): Flow<Location> =
      callbackFlow {
            val locationCallback =
                object : LocationCallback() {
                  override fun onLocationResult(location: LocationResult) {
                    location.lastLocation?.run(::trySend)
                  }
                }
            locationProviderClient.apply {
              requestLocationUpdates(
                      LocationRequest(
                              smallestDisplacement = 1f,
                              priority = LocatorPriority.HighAccuracy,
                              interval = Duration.ofSeconds(2),
                              waitForAccurateLocation = false)
                          .toGMSLocationRequest(),
                      locationCallback,
                      Looper.getMainLooper())
                  .addOnCompleteListener {
                    Timber.d(
                        "LocationLiveData location update request completed: " +
                            "Success=${it.isSuccessful} Cancelled=${it.isCanceled}")
                    Timber.tag("LocationFlow").w("Starting locationFlow")
                  }
              awaitClose {
                locationProviderClient
                    .removeLocationUpdates(locationCallback)
                    .addOnCompleteListener {
                      Timber.d(
                          "LocationLiveData removing location updates completed: " +
                              "Success=${it.isSuccessful} Cancelled=${it.isCanceled}")
                      Timber.tag("LocationFlow").w("Stopping locationFlow")
                    }
              }
            }
          }
          .shareIn(
              coroutineScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 2000), replay = 0)
}
