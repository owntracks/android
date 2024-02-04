package org.owntracks.android.geocoding

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_LOW
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.owntracks.android.R
import org.owntracks.android.di.ApplicationScope
import org.owntracks.android.di.CoroutineScopes
import org.owntracks.android.location.LatLng
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.ReverseGeocodeProvider
import org.owntracks.android.services.BackgroundService
import org.owntracks.android.ui.map.MapActivity
import timber.log.Timber

@Singleton
class GeocoderProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: Preferences,
    private val notificationManager: NotificationManagerCompat,
    @ApplicationScope private val scope: CoroutineScope,
    @CoroutineScopes.IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val httpClient: OkHttpClient
) {
    private var lastRateLimitedNotificationTime: Instant? = null
    private var geocoder: Geocoder = GeocoderNone()

    private var job: Job? = null

    private fun setGeocoderProvider(context: Context, preferences: Preferences) {
        Timber.i("Setting geocoding provider to ${preferences.reverseGeocodeProvider}")
        job = scope.launch {
            withContext(ioDispatcher) {
                geocoder = when (preferences.reverseGeocodeProvider) {
                    ReverseGeocodeProvider.OPENCAGE -> OpenCageGeocoder(
                        preferences.opencageApiKey,
                        httpClient
                    )
                    ReverseGeocodeProvider.DEVICE -> DeviceGeocoder(context)
                    ReverseGeocodeProvider.NONE -> GeocoderNone()
                }
            }
        }
    }

    private suspend fun geocoderResolve(latLng: LatLng): GeocodeResult {
        return withContext(ioDispatcher) {
            job?.run { join() }
            return@withContext geocoder.reverse(latLng.latitude,latLng.longitude)
        }
    }

    suspend fun resolve(latLng: LatLng): String {
        Timber.d("Resolving geocode for $latLng")
        val result = geocoderResolve(latLng)
        maybeCreateErrorNotification(result)
        Timber.d("Resolved $latLng to $result with ${geocoder.javaClass.name}")
        return geocodeResultToText(result) ?: latLng.toDisplayString()
    }

    suspend fun resolve(latLng: LatLng, backgroundService: BackgroundService) {
        val result = geocoderResolve(latLng)
        backgroundService.onGeocodingProviderResult(latLng,geocodeResultToText(result)?: latLng.toDisplayString())
        maybeCreateErrorNotification(result)
    }

    private fun geocodeResultToText(result: GeocodeResult) =
        when (result) {
            is GeocodeResult.Formatted -> result.text
            else -> null
        }

    @SuppressLint("MissingPermission")
    private fun maybeCreateErrorNotification(result: GeocodeResult) {
        if (result is GeocodeResult.Formatted ||
            result is GeocodeResult.Empty ||
            !preferences.notificationGeocoderErrors
        ) {
            notificationManager.cancel(GEOCODE_ERROR_NOTIFICATION_TAG, 0)
            return
        }
        val errorNotificationText = when (result) {
            is GeocodeResult.Fault.Error -> context.getString(
                R.string.geocoderError,
                result.message
            )
            is GeocodeResult.Fault.ExceptionError -> context.getString(R.string.geocoderExceptionError)
            is GeocodeResult.Fault.Disabled -> context.getString(R.string.geocoderDisabled)
            is GeocodeResult.Fault.IPAddressRejected -> context.getString(R.string.geocoderIPAddressRejected)
            is GeocodeResult.Fault.RateLimited -> context.getString(
                R.string.geocoderRateLimited,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    .withZone(ZoneId.systemDefault())
                    .format(result.until)
            )
            is GeocodeResult.Fault.Unavailable -> context.getString(R.string.geocoderUnavailable)
            else -> ""
        }
        val until = when (result) {
            is GeocodeResult.Fault -> result.until
            else -> Instant.MIN
        }

        if (until == lastRateLimitedNotificationTime) {
            return
        } else {
            lastRateLimitedNotificationTime = until
        }

        val activityLaunchIntent = Intent(this.context, MapActivity::class.java)
        activityLaunchIntent.action = "android.intent.action.MAIN"
        activityLaunchIntent.addCategory("android.intent.category.LAUNCHER")
        activityLaunchIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        val notification = NotificationCompat.Builder(context, ERROR_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.geocoderProblemNotificationTitle))
            .setContentText(errorNotificationText)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_owntracks_80)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(errorNotificationText)
            )
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    activityLaunchIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .setPriority(PRIORITY_LOW)
            .setSilent(true)
            .build()

        notificationManager.notify(GEOCODE_ERROR_NOTIFICATION_TAG, 0, notification)
    }

    private val preferenceChangeListener = object : Preferences.OnPreferenceChangeListener {
        override fun onPreferenceChanged(properties: Set<String>) {
            if (properties.intersect(setOf("reverseGeocodeProvider", "opencageApiKey")).isNotEmpty()
            ) {
                setGeocoderProvider(context, preferences)
            }
        }
    }

    init {
        setGeocoderProvider(context, preferences)
        preferences.registerOnPreferenceChangedListener(preferenceChangeListener)
    }

    companion object {
        const val ERROR_NOTIFICATION_CHANNEL_ID = "Errors"
        const val GEOCODE_ERROR_NOTIFICATION_TAG = "GeocoderError"
    }
}
