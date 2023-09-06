package org.owntracks.android.geocoding

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_LOW
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import org.owntracks.android.R
import org.owntracks.android.di.ApplicationScope
import org.owntracks.android.di.CoroutineScopes
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.ReverseGeocodeProvider
import org.owntracks.android.services.BackgroundService
import org.owntracks.android.ui.map.MapActivity
import org.threeten.bp.Instant
import org.threeten.bp.ZoneOffset.UTC
import org.threeten.bp.format.DateTimeFormatter
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

    private suspend fun geocoderResolve(messageLocation: MessageLocation): GeocodeResult {
        return withContext(ioDispatcher) {
            job?.run { join() }
            return@withContext geocoder.reverse(messageLocation.latitude, messageLocation.longitude)
        }
    }

    suspend fun resolve(messageLocation: MessageLocation) {
        if (messageLocation.hasGeocode) {
            return
        }
        Timber.d("Resolving geocode for $messageLocation")
        val result = geocoderResolve(messageLocation)
        messageLocation.geocode = geocodeResultToText(result)
        Timber.v("Geocoded $messageLocation")
        maybeCreateErrorNotification(result)
    }

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
                    .withZone(UTC)
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

    private fun geocodeResultToText(result: GeocodeResult) =
        when (result) {
            is GeocodeResult.Formatted -> result.text
            else -> null
        }

    fun resolve(messageLocation: MessageLocation, backgroundService: BackgroundService) {
        if (messageLocation.hasGeocode) {
            backgroundService.onGeocodingProviderResult(messageLocation)
            return
        }
        scope.launch {
            val result = geocoderResolve(messageLocation)
            messageLocation.geocode = geocodeResultToText(result)
            backgroundService.onGeocodingProviderResult(messageLocation)
            maybeCreateErrorNotification(result)
        }
    }

    private val preferenceChangeListener = object : Preferences.OnPreferenceChangeListener {
        override fun onPreferenceChanged(properties: Set<String>) {
            if (properties.intersect(setOf("reverseGeocodeProvider", "opencageApiKey"))
                    .isNotEmpty()
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
