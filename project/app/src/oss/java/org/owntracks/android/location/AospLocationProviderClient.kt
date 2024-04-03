package org.owntracks.android.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.location.LocationManagerCompat
import androidx.core.os.ExecutorCompat
import java.util.WeakHashMap
import org.owntracks.android.location.LocationRequest.Companion.PRIORITY_HIGH_ACCURACY
import timber.log.Timber

class AospLocationProviderClient(val context: Context) : LocationProviderClient() {
  enum class LocationSources {
    GPS,
    FUSED,
    NETWORK,
    PASSIVE
  }

  private val locationManager =
      context.getSystemService(Context.LOCATION_SERVICE) as LocationManager?

  private val availableLocationProviders =
      (locationManager?.allProviders?.run {
        LocationSources.entries.filter { contains(it.name.lowercase()) }.toSet()
      } ?: emptySet())

  private val callbacks = WeakHashMap<LocationCallback, LocationListener>()

  private fun locationSourcesForPriority(priority: Int): Set<LocationSources> =
      when (priority) {
        PRIORITY_HIGH_ACCURACY -> setOf(LocationSources.GPS)
        else ->
            setOf(LocationSources.FUSED, LocationSources.NETWORK, LocationSources.PASSIVE)
                .intersect(availableLocationProviders)
      }

  @SuppressLint("MissingPermission")
  override fun singleHighAccuracyLocation(clientCallBack: LocationCallback, looper: Looper) {
    Timber.d("Getting single high-accuracy location, posting to $clientCallBack")
    locationManager?.run {
      LocationManagerCompat.getCurrentLocation(
          this,
          LocationSources.GPS.name.lowercase(),
          null,
          ExecutorCompat.create(Handler(looper))) { location ->
            clientCallBack.onLocationResult(LocationResult(location))
          }
    }
  }

  @SuppressLint("MissingPermission")
  override fun actuallyRequestLocationUpdates(
      locationRequest: LocationRequest,
      clientCallBack: LocationCallback,
      looper: Looper
  ) {
    locationManager?.run {
      val listener = LocationListener { location ->
        clientCallBack.onLocationResult(LocationResult(location))
      }
      callbacks[clientCallBack] = listener
      locationSourcesForPriority(locationRequest.priority)
          .apply {
            Timber.v("Requested location updates for sources $this to callback $clientCallBack")
          }
          .forEach {
            requestLocationUpdates(
                it.name.lowercase(),
                locationRequest.interval.toMillis(),
                locationRequest.smallestDisplacement ?: 10f,
                listener,
                looper)
          }
    }
  }

  override fun removeLocationUpdates(clientCallBack: LocationCallback) {
    Timber.v("removeLocationUpdates for $clientCallBack")
    callbacks.getOrDefault(clientCallBack, null)?.apply {
      locationManager?.removeUpdates(this)
      callbacks.remove(clientCallBack)
    } ?: run { Timber.w("No current location updates found for $clientCallBack") }
  }

  override fun flushLocations() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      callbacks.values.zip(availableLocationProviders).forEach {
        try {
          Timber.v("Flushing locations for ${it.first} callback on ${it.second} provider")
          locationManager?.requestFlush(it.second.name.lowercase(), it.first, 0)
        } catch (e: IllegalArgumentException) {
          if (e.message == "unregistered listener cannot be flushed") {
            Timber.d(
                "Unable to flush locations for ${it.second} callback, as provider {it.second} is not registered")
          } else {
            Timber.e(e, "Unable to flush locations for ${it.second} callback")
          }
        }
      }
    } else {
      Timber.w(
          "Can't flush locations, Android device API needs to be 31, is actually ${Build.VERSION.SDK_INT}")
    }
  }

  @Suppress("MissingPermission")
  override fun getLastLocation(): Location? =
      locationManager?.run {
        LocationSources.entries
            .map { getLastKnownLocation(it.name.lowercase()) }
            .maxByOrNull { it?.time ?: 0 }
      }

  init {
    Timber.i(
        "Using AOSP as a location provider. Available providers are ${locationManager?.allProviders}")
  }
}
