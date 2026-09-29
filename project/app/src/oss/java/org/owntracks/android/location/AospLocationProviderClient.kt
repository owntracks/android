package org.owntracks.android.location

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import androidx.core.os.ExecutorCompat
import java.util.WeakHashMap
import timber.log.Timber

class AospLocationProviderClient(val context: Context) : LocationProviderClient() {
  enum class LocationSources {
    GPS,
    FUSED,
    NETWORK,
    PASSIVE,
  }

  private val locationManager =
      context.getSystemService(Context.LOCATION_SERVICE) as LocationManager?

  private val availableLocationProviders =
      (locationManager?.allProviders?.run {
        LocationSources.entries.filter { contains(it.name.lowercase()) }.toSet()
      } ?: emptySet())

  private val callbacks = WeakHashMap<LocationCallback, LocationListenerCompat>()

  private fun locationSourcesForPriority(priority: LocatorPriority): Set<LocationSources> =
      when (priority) {
        LocatorPriority.HighAccuracy -> setOf(LocationSources.GPS)
        else ->
            setOf(LocationSources.FUSED, LocationSources.NETWORK, LocationSources.PASSIVE)
                .intersect(availableLocationProviders)
      }

  @RequiresPermission(
      anyOf =
          ["android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION"]
  )
  override fun singleHighAccuracyLocation(clientCallBack: LocationCallback, looper: Looper) {
    Timber.d("Getting single high-accuracy location, posting to $clientCallBack")
    locationManager?.run {
      LocationManagerCompat.getCurrentLocation(
          this,
          LocationSources.GPS.name.lowercase(),
          android.os.CancellationSignal(),
          ExecutorCompat.create(Handler(looper)),
      ) { location: Location? ->
        location?.run { clientCallBack.onLocationResult(LocationResult(this)) }
            ?: Timber.w("Got null location from getCurrentLocation")
      }
    }
  }

  @RequiresPermission(
      anyOf =
          ["android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION"]
  )
  override fun actuallyRequestLocationUpdates(
      locationRequest: LocationRequest,
      clientCallBack: LocationCallback,
      looper: Looper,
  ) {
    locationManager?.run {
      // Explicit overrides, not a SAM lambda: these three methods are only default on API 31+,
      // so a lambda here throws AbstractMethodError on older devices when the OS calls them.
      val listener =
          object : LocationListenerCompat {
            override fun onLocationChanged(location: Location) {
              clientCallBack.onLocationResult(LocationResult(location))
            }

            override fun onProviderEnabled(provider: String) {}

            override fun onProviderDisabled(provider: String) {}

            @Suppress("DEPRECATION")
            override fun onStatusChanged(provider: String, status: Int, extras: Bundle?) {}
          }
      callbacks[clientCallBack] = listener
      locationSourcesForPriority(locationRequest.priority)
          .apply {
            Timber.v("Requested location updates for sources $this to callback $clientCallBack")
          }
          .forEach {
            LocationManagerCompat.requestLocationUpdates(
                this,
                it.name.lowercase(),
                LocationRequestCompat.Builder(locationRequest.interval.toMillis())
                    .setQuality(qualityForPriority(locationRequest.priority))
                    .setMinUpdateDistanceMeters(locationRequest.smallestDisplacement ?: 10f)
                    .build(),
                ExecutorCompat.create(Handler(looper)),
                listener,
            )
          }
    }
  }

  override fun removeLocationUpdates(clientCallBack: LocationCallback) {
    Timber.v("removeLocationUpdates for $clientCallBack")
    callbacks.getOrDefault(clientCallBack, null)?.apply {
      locationManager?.let { LocationManagerCompat.removeUpdates(it, this) }
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
                "Unable to flush locations for ${it.second} callback, as provider ${it.second} is not registered"
            )
          } else {
            Timber.e(e, "Unable to flush locations for ${it.second} callback")
          }
        }
      }
    } else {
      Timber.w(
          "Can't flush locations, Android device API needs to be 31, is actually ${Build.VERSION.SDK_INT}"
      )
    }
  }

  @Suppress("MissingPermission")
  override fun getLastLocation(): Location? = locationManager?.run {
    LocationSources.entries
        .map { getLastKnownLocation(it.name.lowercase()) }
        .maxByOrNull { it?.time ?: 0 }
  }

  init {
    Timber.i(
        "Using AOSP as a location provider. Available providers are ${locationManager?.allProviders}"
    )
  }
}

// Without an explicit quality, LocationManager requests BALANCED, which the fused provider can
// serve with GNSS. The priority must reach the provider for LowPower to mean network-only.
internal fun qualityForPriority(priority: LocatorPriority): Int =
    when (priority) {
      LocatorPriority.HighAccuracy -> LocationRequestCompat.QUALITY_HIGH_ACCURACY
      LocatorPriority.BalancedPowerAccuracy -> LocationRequestCompat.QUALITY_BALANCED_POWER_ACCURACY
      LocatorPriority.LowPower,
      LocatorPriority.NoPower -> LocationRequestCompat.QUALITY_LOW_POWER
    }
