package org.owntracks.android.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import org.owntracks.android.location.LocationRequest.Companion.PRIORITY_HIGH_ACCURACY
import timber.log.Timber

class AospLocationProviderClient(val context: Context) : LocationProviderClient() {
    enum class LocationSources {
        GPS,
        FUSED,
        NETWORK,
        PASSIVE
    }

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager?

    private val availableLocationProviders =
        (
            locationManager?.allProviders?.run {
                LocationSources.values()
                    .filter { contains(it.name.lowercase()) }
                    .toSet()
            }
                ?: emptySet()
            )

    private val callbacks = mutableMapOf<LocationCallback, LocationListener>()

    private fun locationSourcesForPriority(priority: Int): Set<LocationSources> =
        when (priority) {
            PRIORITY_HIGH_ACCURACY -> setOf(LocationSources.GPS)
            else -> setOf(LocationSources.FUSED, LocationSources.NETWORK, LocationSources.PASSIVE).intersect(
                availableLocationProviders
            )
        }

    /**
     * A location listener that discards locations that are older than [maxUpdateAge], posting the first location
     * to the given [clientCallBack] and then stopping updates.
     *
     *
     * @property maxUpdateAge
     * @property clientCallBack
     * @property locationManager
     * @constructor Create empty Location listener with max update age
     */
    @Suppress("MissingPermission")
    class LocationListenerWithMaxUpdateAge(
        private val maxUpdateAge: Duration,
        private val clientCallBack: LocationCallback,
        private val locationManager: LocationManager
    ) : LocationListenerCompat {
        override fun onLocationChanged(location: Location) {
            val age = (System.currentTimeMillis() - location.time).milliseconds
            if (age <= maxUpdateAge) {
                clientCallBack.onLocationResult(LocationResult(location))
                LocationManagerCompat.removeUpdates(locationManager, this@LocationListenerWithMaxUpdateAge)
            } else {
                Timber.v("Received location that's too old: $age. Discarding")
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun singleHighAccuracyLocation(clientCallBack: LocationCallback, looper: Looper) {
        Timber.d("Getting single high-accuracy location, posting to $clientCallBack")

        locationManager?.run {
            val listener = LocationListenerWithMaxUpdateAge(5.seconds, clientCallBack, this)
            LocationManagerCompat.requestLocationUpdates(
                this,
                LocationSources.GPS.name.lowercase(),
                LocationRequestCompat.Builder(1.seconds.inWholeMilliseconds).setQuality(
                    LocationRequestCompat.QUALITY_HIGH_ACCURACY
                ).build(),

                listener,
                looper
            )
        }
    }

    @SuppressLint("MissingPermission")
    override fun actuallyRequestLocationUpdates(
        locationRequest: LocationRequest,
        clientCallBack: LocationCallback,
        looper: Looper
    ) {
        locationManager?.run {
            val listener =
                LocationListener { location -> clientCallBack.onLocationResult(LocationResult(location)) }
            callbacks[clientCallBack] = listener
            locationSourcesForPriority(locationRequest.priority).apply {
                Timber.v("Requested location updates for sources $this to callback $clientCallBack")
            }.forEach {
                requestLocationUpdates(
                    it.name.lowercase(),
                    locationRequest.interval.toMillis(),
                    locationRequest.smallestDisplacement ?: 10f,
                    listener,
                    looper
                )
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
            Timber.v("Flushing locations")
            callbacks.values.zip(availableLocationProviders)
                .forEach {
                    try {
                        locationManager?.requestFlush(it.second.name.lowercase(), it.first, 0)
                    } catch (e: IllegalArgumentException) {
                        Timber.d("Unable to flush locations for ${it.second} callback")
                    }
                }
        } else {
            Timber.w("Can't flush locations, Android device API needs to be 31, is actually ${Build.VERSION.SDK_INT}")
        }
    }

    @Suppress("MissingPermission")
    override fun getLastLocation(): Location? =
        locationManager?.run {
            LocationSources.values().map { getLastKnownLocation(it.name.lowercase()) }
                .maxByOrNull { it?.time ?: 0 }
        }

    init {
        Timber.i("Using AOSP as a location provider. Available providers are ${locationManager?.allProviders}")
    }
}
